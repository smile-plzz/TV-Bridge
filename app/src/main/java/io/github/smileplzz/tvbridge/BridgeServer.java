package io.github.smileplzz.tvbridge;

import android.content.Context;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;

final class BridgeServer implements AutoCloseable {
    interface Listener {
        void pair(String name, String id);
        void received(String text, boolean link);
        void changed(String event);
    }
    final Context context;
    final Listener listener;
    final File folder;
    final String code = String.format(Locale.US,"%06d",new SecureRandom().nextInt(1000000));
    final String host;
    final long started=System.currentTimeMillis();
    final Map<String,Long> sessions=new ConcurrentHashMap<>();
    final Map<String,Pending> pending=new ConcurrentHashMap<>();
    final Map<String,Deque<Long>> attempts=new ConcurrentHashMap<>();
    final List<String> events=Collections.synchronizedList(new ArrayList<>());
    final Set<Socket> sockets=ConcurrentHashMap.newKeySet();
    final ThreadPoolExecutor pool=new ThreadPoolExecutor(4,4,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(8));
    final java.util.concurrent.atomic.AtomicBoolean uploading=new java.util.concurrent.atomic.AtomicBoolean();
    volatile boolean running=true;
    ServerSocket server;
    record Pending(String name, long created, boolean approved) {}
    BridgeServer(Context c, String ip, Listener l) throws IOException {
        context=c; listener=l; host=ip+":8765"; folder=new File(c.getFilesDir(),"shared");
        if (!folder.isDirectory() && !folder.mkdirs()) throw new IOException("Cannot create shared folder");
        File[] old=folder.listFiles((d,n)->n.endsWith(".part"));
        if(old!=null) for(File f:old) if(System.currentTimeMillis()-f.lastModified()>24*60*60_000L)f.delete();
        server=new ServerSocket(); server.setReuseAddress(true); server.bind(new InetSocketAddress(ip,8765));
        new Thread(()->{while(running) try {
            Socket s=server.accept(); s.setSoTimeout(30000); sockets.add(s);
            try { pool.execute(()->handle(s)); } catch(RejectedExecutionException e){sockets.remove(s);s.close();}
        } catch(IOException e) {if(running) log("Connection error");}},"bridge-accept").start();
        log("Bridge started");
    }
    void approve(String id, boolean allow) {
        Pending p=pending.get(id);
        if(p!=null) { if(allow) pending.put(id,new Pending(p.name,p.created,true)); else pending.remove(id); }
        log(allow?"Phone approved":"Pairing declined");
    }
    void log(String text){ synchronized(events){events.add(java.time.Instant.now()+" · "+text);if(events.size()>40)events.remove(0);} listener.changed(text); }
    void handle(Socket socket) {
        try(Socket s=socket) {
            InputStream in=new BufferedInputStream(s.getInputStream()); OutputStream out=s.getOutputStream();
            HttpWire.Request r;
            try {r=HttpWire.read(in);} catch(IOException e){reply(out,400,"Invalid request");return;}
            if(!host.equals(r.headers().get("host"))) {reply(out,403,"Host rejected");return;}
            String origin=r.headers().get("origin");
            if(origin!=null && !origin.equals("http://"+host)){reply(out,403,"Origin rejected");return;}
            String fetch=r.headers().get("sec-fetch-site");
            if("cross-site".equals(fetch)){reply(out,403,"Cross-site request rejected");return;}
            String path=r.path().split("\\?",2)[0];
            if(r.method().equals("GET") && (path.equals("/")||path.equals("/app.js")||path.equals("/style.css"))) {
                String file=path.equals("/")?"index.html":path.substring(1);
                byte[] data;try(InputStream a=context.getAssets().open(file);ByteArrayOutputStream b=new ByteArrayOutputStream()){byte[] buf=new byte[8192];int n;while((n=a.read(buf))!=-1)b.write(buf,0,n);data=b.toByteArray();}
                HttpWire.headers(out,200,file.endsWith("js")?"text/javascript":file.endsWith("css")?"text/css":"text/html; charset=utf-8",data.length,"");out.write(data);return;
            }
            if(r.method().equals("POST")&&path.equals("/api/pair")) {
                String address=s.getInetAddress().getHostAddress();
                if(!rate(address)){reply(out,429,"Wait one minute before pairing again");return;}
                JSONObject body=body(in,r,1024);
                if(System.currentTimeMillis()-started>10*60_000 || !code.equals(body.optString("code"))){reply(out,403,"Incorrect or expired code. Restart the bridge for a new code.");return;}
                pending.entrySet().removeIf(e->System.currentTimeMillis()-e.getValue().created>60_000);
                if(pending.size()>=3){reply(out,429,"Another pairing request is waiting");return;}
                String id=UUID.randomUUID().toString();String name=body.optString("name","Phone");
                name=name.substring(0,Math.min(name.length(),40)).replaceAll("[\\p{Cntrl}]", "");
                pending.put(id,new Pending(name,System.currentTimeMillis(),false)); listener.pair(name,id);
                json(out,202,new JSONObject().put("request",id));return;
            }
            if(r.method().equals("POST")&&path.equals("/api/pair/status")) {
                JSONObject b=body(in,r,1024);String id=b.optString("request");Pending p=pending.get(id);
                if(p==null||System.currentTimeMillis()-p.created>60_000){pending.remove(id);reply(out,403,"Pairing expired or declined");return;}
                if(!p.approved){json(out,202,new JSONObject().put("waiting",true));return;}
                pending.remove(id);String token=UUID.randomUUID()+"-"+UUID.randomUUID();sessions.put(token,System.currentTimeMillis());
                log("Phone connected");json(out,200,new JSONObject().put("token",token));return;
            }
            String auth=r.headers().getOrDefault("authorization","");String token=auth.startsWith("Bearer ")?auth.substring(7):"";
            Long time=sessions.get(token);
            if(time==null||System.currentTimeMillis()-time>60*60_000){sessions.remove(token);reply(out,401,"Session expired. Pair again.");return;}
            if(r.method().equals("GET")&&path.equals("/api/files")) {
                JSONArray files=new JSONArray();File[] list=folder.listFiles((d,n)->!n.endsWith(".part"));
                if(list!=null){Arrays.sort(list,Comparator.comparingLong(File::lastModified).reversed());for(File f:list)if(f.isFile())files.put(new JSONObject().put("name",f.getName()).put("size",f.length()).put("modified",f.lastModified()));}
                json(out,200,new JSONObject().put("files",files).put("free",folder.getUsableSpace()).put("maxUpload",HttpWire.MAX_UPLOAD));return;
            }
            if(r.method().equals("POST")&&path.equals("/api/upload")) {
                if(!uploading.compareAndSet(false,true)){reply(out,409,"Another transfer is active");return;}
                File part=null;
                try {
                    if(r.length()==0||r.length()>folder.getUsableSpace()-32*1024*1024){reply(out,400,"Empty file or insufficient TV storage");return;}
                    String name=HttpWire.safeName(URLDecoder.decode(r.headers().getOrDefault("x-file-name","file"),"UTF-8"));
                    String prefix=System.currentTimeMillis()+"-"+UUID.randomUUID().toString().substring(0,8)+"-";
                    File target=new File(folder,prefix+name);part=new File(folder,prefix+name+".part");
                    log("Receiving "+name);
                    try(FileOutputStream dest=new FileOutputStream(part)){HttpWire.copy(in,dest,r.length());dest.getFD().sync();}
                    if(!running)throw new IOException("Bridge stopped");
                    if(!part.renameTo(target))throw new IOException("Cannot finish transfer");
                    log("Received "+name);json(out,200,new JSONObject().put("name",target.getName()));
                }finally{if(part!=null)part.delete();uploading.set(false);}return;
            }
            if(path.equals("/api/file")&&(r.method().equals("POST")||r.method().equals("DELETE"))) {
                JSONObject b=body(in,r,2048);String name=b.optString("name");File f=new File(folder,name);
                if(!f.getCanonicalFile().getParentFile().equals(folder.getCanonicalFile())||name.endsWith(".part")||!f.isFile()){reply(out,404,"File unavailable");return;}
                if(r.method().equals("DELETE")){if(!f.delete()){reply(out,409,"Could not delete file");return;}log("File removed");reply(out,200,"Removed");return;}
                HttpWire.headers(out,200,"application/octet-stream",f.length(),"Content-Disposition: attachment; filename=\""+name.replaceAll("[^a-zA-Z0-9._-]","_")+"\"\r\n");
                try(InputStream file=new FileInputStream(f)){HttpWire.copy(file,out,f.length());}return;
            }
            if(path.equals("/api/send")&&r.method().equals("POST")) {
                JSONObject b=body(in,r,65536);String text=b.optString("text").trim();boolean link=b.optBoolean("link");
                if(text.isEmpty()||text.length()>8000){reply(out,400,"Text must contain 1–8000 characters");return;}
                if(link){URI u;try{u=new URI(text);}catch(Exception e){reply(out,400,"Invalid URL");return;}
                    if(!("https".equalsIgnoreCase(u.getScheme())||"http".equalsIgnoreCase(u.getScheme()))||u.getHost()==null||u.getUserInfo()!=null){reply(out,400,"Use an http or https URL");return;}}
                listener.received(text,link);log(link?"Link received; waiting for TV approval":"Text received");reply(out,200,"Sent to TV");return;
            }
            if(path.equals("/api/diagnostics")&&r.method().equals("GET")) {
                JSONObject d=new JSONObject().put("app","TV Bridge").put("version","1.0.0").put("captured",java.time.Instant.now().toString()).put("device",android.os.Build.MANUFACTURER+" "+android.os.Build.MODEL).put("android",android.os.Build.VERSION.RELEASE).put("freeBytes",folder.getUsableSpace()).put("activeSessions",sessions.size()).put("uptimeSeconds",(System.currentTimeMillis()-started)/1000);
                synchronized(events){d.put("events",new JSONArray(events));}
                byte[] data=d.toString(2).getBytes(StandardCharsets.UTF_8);HttpWire.headers(out,200,"application/json",data.length,"Content-Disposition: attachment; filename=\"tv-bridge-diagnostics.json\"\r\n");out.write(data);return;
            }
            if(path.equals("/api/logout")&&r.method().equals("POST")){sessions.remove(token);reply(out,200,"Disconnected");return;}
            reply(out,404,"Not found");
        }catch(Exception e){if(running)log("Request failed or transfer interrupted");}finally{sockets.remove(socket);}
    }
    boolean rate(String ip){if(attempts.size()>64&&!attempts.containsKey(ip))return false;Deque<Long> q=attempts.computeIfAbsent(ip,k->new ArrayDeque<>());synchronized(q){long now=System.currentTimeMillis();while(!q.isEmpty()&&now-q.peek()>60_000)q.remove();if(q.size()>=5)return false;q.add(now);return true;}}
    JSONObject body(InputStream in,HttpWire.Request r,int max)throws Exception {if(r.length()>max)throw new IOException("Body too large");ByteArrayOutputStream b=new ByteArrayOutputStream();HttpWire.copy(in,b,r.length());return new JSONObject(b.toString("UTF-8"));}
    void reply(OutputStream o,int status,String message)throws Exception{json(o,status,new JSONObject().put("message",message));}
    void json(OutputStream o,int status,JSONObject data)throws IOException{byte[] b=data.toString().getBytes(StandardCharsets.UTF_8);HttpWire.headers(o,status,"application/json; charset=utf-8",b.length,"");o.write(b);}
    public void close(){running=false;sessions.clear();pending.clear();try{server.close();}catch(Exception ignored){}for(Socket s:sockets)try{s.close();}catch(Exception ignored){}pool.shutdownNow();}
}
