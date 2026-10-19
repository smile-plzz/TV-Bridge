package io.github.smileplzz.tvbridge;

import android.app.Instrumentation;
import android.os.Bundle;
import org.json.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Device-level protocol checks, independent of UI timing. Run with adb am instrument. */
public final class BridgeInstrumentation extends Instrumentation {
    BridgeServer bridge;
    String token="";
    int checks;
    String base="http://127.0.0.1:8765";
    record Reply(int code,byte[] body){JSONObject json()throws Exception{return new JSONObject(new String(body,StandardCharsets.UTF_8));}}
    public void onCreate(Bundle b){super.onCreate(b);start();}
    void check(boolean condition,String label){if(!condition)throw new AssertionError(label);checks++;}
    Reply request(String path,String method,byte[] body,String origin)throws Exception {
        return request(path,method,body,origin,Collections.emptyMap());
    }
    Reply request(String path,String method,byte[] body,String origin,Map<String,String> headers)throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(base+path).openConnection();c.setConnectTimeout(5000);c.setReadTimeout(5000);c.setRequestMethod(method);for(Map.Entry<String,String> h:headers.entrySet())c.setRequestProperty(h.getKey(),h.getValue());
        if(!token.isEmpty())c.setRequestProperty("Authorization","Bearer "+token);
        if(origin!=null)c.setRequestProperty("Origin",origin);
        if(body!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("X-File-Name","test.bin");c.setFixedLengthStreamingMode(body.length);try(OutputStream o=c.getOutputStream()){o.write(body);}}
        int status=c.getResponseCode();InputStream input=status>=400?c.getErrorStream():c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream();if(input!=null)try(InputStream in=input){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);}c.disconnect();return new Reply(status,out.toByteArray());
    }
    byte[] json(String s){return s.getBytes(StandardCharsets.UTF_8);}
    public void onStart(){Bundle result=new Bundle();try{
        bridge=new BridgeServer(getTargetContext(),"127.0.0.1",new BridgeServer.Listener(){public void pair(String n,String id){}public void received(String t,boolean l){}public void changed(String e){}});
        check(request("/","GET",null,null).code==200,"phone page");
        check(request("/api/files","GET",null,null).code==401,"authorization required");
        check(request("/api/pair","POST",json("{\"code\":\"bad\"}"),null).code==403,"wrong code");
        check(request("/api/pair","POST",json("{}"),"http://evil.test").code==403,"origin rejected");
        Reply pair=request("/api/pair","POST",json("{\"code\":\""+bridge.code+"\"}"),null);check(pair.code==202,"pair request");String id=pair.json().getString("request");
        byte[] poll=json("{\"request\":\""+id+"\"}");check(request("/api/pair/status","POST",poll,null).code==202,"TV approval required");
        bridge.approve(id,true);token=request("/api/pair/status","POST",poll,null).json().getString("token");check(token.length()>60,"strong token");
        check(request("/api/pair/status","POST",poll,null).code==403,"one-time pairing");
        byte[] data=new byte[262144];new Random(42).nextBytes(data);
        Reply uploaded=request("/api/upload","POST",data,null);check(uploaded.code==200,"upload");String name=uploaded.json().getString("name");byte[] file=json(new JSONObject().put("name",name).toString());
        check(Arrays.equals(request("/api/file","POST",file,null).body,data),"download byte integrity");
        check(request("/api/files","GET",null,null).json().getJSONArray("files").length()>0,"file listing");
        check(request("/api/file","POST",json("{\"name\":\"../private\"}"),null).code==404,"traversal rejected");
        check(request("/api/send","POST",json("{\"text\":\"javascript:alert(1)\",\"link\":true}"),null).code==400,"unsafe link rejected");
        check(request("/api/send","POST",json("{\"text\":\"hello TV\"}"),null).code==200,"text received");
        check(request("/api/diagnostics","GET",null,null).json().getString("app").equals("TV Bridge"),"diagnostics");
        check(request("/api/status","GET",null,null).json().getString("version").equals("1.1.0"),"connection status");
        String ticket=request("/api/download","POST",file,null).json().getString("url");
        check(Arrays.equals(request(ticket,"GET",null,null).body,data),"native download byte integrity");
        check(request(ticket,"GET",null,null).code==403,"download ticket single use");
        check(request("/api/open","POST",file,null).code==409,"opening needs visible TV confirmation UI");
        check(request("/api/media","POST",json("{\"action\":\"pause\"}"),null).code==409,"media command requires active player");
        String uploadId=request("/api/upload/start","POST",json("{\"name\":\"resume.bin\",\"size\":16,\"signature\":\"test\"}"),null).json().getString("id");
        byte[] first=Arrays.copyOfRange(data,0,8),second=Arrays.copyOfRange(data,8,16);
        check(request("/api/upload/chunk","POST",first,null,Map.of("X-Upload-Id",uploadId,"X-Upload-Offset","0")).json().getLong("offset")==8,"resumable first chunk");
        check(request("/api/upload/chunk","POST",second,null,Map.of("X-Upload-Id",uploadId,"X-Upload-Offset","0")).code==409,"wrong upload offset rejected");
        check(request("/api/upload/status","POST",json("{\"id\":\""+uploadId+"\"}"),null).json().getLong("offset")==8,"resumable status");
        check(request("/api/upload/chunk","POST",second,null,Map.of("X-Upload-Id",uploadId,"X-Upload-Offset","8")).json().getLong("offset")==16,"resumable second chunk");
        JSONObject completed=request("/api/upload/finish","POST",json("{\"id\":\""+uploadId+"\"}"),null).json();
        check(completed.getBoolean("complete")&&completed.getString("sha256").length()==64,"verified finalization");
        byte[] resumedFile=json(new JSONObject().put("name",completed.getString("target")).toString());
        check(Arrays.equals(request("/api/file","POST",resumedFile,null).body,Arrays.copyOfRange(data,0,16)),"resumed download integrity");
        request("/api/upload/ack","POST",json("{\"id\":\""+uploadId+"\"}"),null);request("/api/file","DELETE",resumedFile,null);
        String revokedDownload=request("/api/download","POST",file,null).json().getString("url");
        check(request("/api/file","DELETE",file,null).code==200,"delete");
        check(request("/api/file","POST",file,null).code==404,"deleted file unavailable");
        check(request("/api/logout","POST",json("{}"),null).code==200,"logout");
        check(request("/api/files","GET",null,null).code==401,"revoked token");
        check(request(revokedDownload,"GET",null,null).code==403,"logout revokes download tickets");
        bridge.close();bridge=null;token="";
        android.app.Activity activity=startActivitySync(new android.content.Intent(getTargetContext(),MainActivity.class).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
        for(int i=0;i<50&&(BridgeService.instance==null||BridgeService.instance.bridge==null);i++)Thread.sleep(100);
        check(BridgeService.instance!=null&&BridgeService.instance.bridge!=null,"visible activity starts foreground connection");
        BridgeServer persistent=BridgeService.instance.bridge;base="http://"+persistent.host;
        android.graphics.Bitmap shot=getUiAutomation().takeScreenshot();if(shot!=null){try(OutputStream o=new FileOutputStream(new File(getTargetContext().getExternalFilesDir(null),"tv-home.png"))){shot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,o);}shot.recycle();}
        runOnMainSync(activity::finish);waitForIdleSync();Thread.sleep(300);
        check(BridgeService.instance!=null&&BridgeService.instance.bridge==persistent,"connection survives activity close");
        check(request("/","GET",null,null).code==200,"phone page works while TV UI is closed");
        getTargetContext().stopService(new android.content.Intent(getTargetContext(),BridgeService.class));for(int i=0;i<30&&BridgeService.instance!=null;i++)Thread.sleep(100);
        check(BridgeService.instance==null,"explicit stop destroys connection");
        result.putString("stream","TV_BRIDGE_TESTS_OK: "+checks+" checks\n");finish(-1,result);
    }catch(Throwable e){result.putString("stream","TV_BRIDGE_TESTS_FAILED: "+e+"\n"+android.util.Log.getStackTraceString(e));finish(0,result);}finally{if(bridge!=null)bridge.close();}}
}
