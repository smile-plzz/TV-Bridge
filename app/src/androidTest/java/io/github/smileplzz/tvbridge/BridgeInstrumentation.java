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
    record Reply(int code,byte[] body){JSONObject json()throws Exception{return new JSONObject(new String(body,StandardCharsets.UTF_8));}}
    public void onCreate(Bundle b){super.onCreate(b);start();}
    void check(boolean condition,String label){if(!condition)throw new AssertionError(label);checks++;}
    Reply request(String path,String method,byte[] body,String origin)throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL("http://127.0.0.1:8765"+path).openConnection();c.setConnectTimeout(5000);c.setReadTimeout(5000);c.setRequestMethod(method);
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
        check(request("/api/file","DELETE",file,null).code==200,"delete");
        check(request("/api/file","POST",file,null).code==404,"deleted file unavailable");
        check(request("/api/logout","POST",json("{}"),null).code==200,"logout");
        check(request("/api/files","GET",null,null).code==401,"revoked token");
        result.putString("stream","TV_BRIDGE_TESTS_OK: "+checks+" checks\n");finish(-1,result);
    }catch(Throwable e){result.putString("stream","TV_BRIDGE_TESTS_FAILED: "+e+"\n"+android.util.Log.getStackTraceString(e));finish(0,result);}finally{if(bridge!=null)bridge.close();}}
}
