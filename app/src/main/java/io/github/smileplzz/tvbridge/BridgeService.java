package io.github.smileplzz.tvbridge;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.net.*;
import android.os.*;
import org.json.JSONObject;
import java.util.*;

/** User-started local bridge. Persists through media, file pickers and TV Home. */
public final class BridgeService extends Service implements BridgeServer.Listener {
    static volatile BridgeService instance;
    volatile BridgeServer bridge;
    volatile MainActivity screen;
    volatile String status="Starting connection";
    final Handler main=new Handler(Looper.getMainLooper());
    final ArrayDeque<Message> inbox=new ArrayDeque<>();
    final IBinder binder=new LocalBinder();
    android.net.ConnectivityManager.NetworkCallback network;
    PowerManager.WakeLock wake;
    int busyCount;
    record Message(String text,boolean link) {}
    final class LocalBinder extends Binder {BridgeService service(){return BridgeService.this;}}
    @Override public void onCreate(){super.onCreate();instance=this;
        NotificationManager manager=getSystemService(NotificationManager.class);manager.createNotificationChannel(new NotificationChannel("bridge","Phone connection",NotificationManager.IMPORTANCE_LOW));
        Intent stop=new Intent(this,BridgeService.class).setAction("STOP");
        Notification n=new Notification.Builder(this,"bridge").setContentTitle("TV Bridge is available").setContentText("Local phone connection active. Open TV Bridge to pair or stop.").setSmallIcon(io.github.smileplzz.tvbridge.R.drawable.icon).setOngoing(true).setContentIntent(PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT)).addAction(new Notification.Action.Builder(null,"Stop bridge",PendingIntent.getService(this,1,stop,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT)).build()).build();
        if(Build.VERSION.SDK_INT>=29)startForeground(1,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);else startForeground(1,n);
        wake=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"TVBridge:Transfer");wake.setReferenceCounted(false);
        restart();
        network=new android.net.ConnectivityManager.NetworkCallback(){public void onLinkPropertiesChanged(Network n,LinkProperties p){main.post(()->{String ip=ip();BridgeServer b=bridge;if(!Objects.equals(ip,b==null?null:b.host.split(":")[0]))restart();});}public void onLost(Network n){main.post(()->{if(ip()==null)restart();});}};
        ((android.net.ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE)).registerDefaultNetworkCallback(network);
        main.postDelayed(this::shutdown,2*60*60_000L);
    }
    String ip(){android.net.ConnectivityManager cm=(android.net.ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);LinkProperties p=cm.getLinkProperties(cm.getActiveNetwork());if(p!=null)for(LinkAddress a:p.getLinkAddresses())if(a.getAddress() instanceof java.net.Inet4Address&&a.getAddress().isSiteLocalAddress())return a.getAddress().getHostAddress();return null;}
    void restart(){if(bridge!=null)bridge.close();bridge=null;String ip=ip();status=ip==null?"Connect the TV to home Wi-Fi or Ethernet":"Starting connection";if(ip!=null)try{bridge=new BridgeServer(this,ip,this);status="Ready to pair";}catch(Exception e){status="Port 8765 is unavailable. Stop other bridge apps and retry.";}if(screen!=null)screen.connectionChanged();}
    void shutdown(){MainActivity a=screen;if(a!=null)main.post(a::stopBridge);else stopSelf();}
    @Override public int onStartCommand(Intent intent,int flags,int id){if(intent!=null&&"STOP".equals(intent.getAction())){shutdown();return START_NOT_STICKY;}return START_NOT_STICKY;}
    @Override public IBinder onBind(Intent intent){return binder;}
    void attach(MainActivity activity){screen=activity;if(bridge!=null)for(Map.Entry<String,BridgeServer.Pending> e:bridge.pending.entrySet())if(!e.getValue().approved())activity.pair(e.getValue().name(),e.getKey());synchronized(inbox){while(!inbox.isEmpty()){Message m=inbox.remove();activity.received(m.text,m.link);}}}
    void detach(MainActivity activity){if(screen==activity)screen=null;}
    public void pair(String name,String id){MainActivity a=screen;if(a!=null)a.pair(name,id);changed("Pairing request waiting. Open TV Bridge to approve.");}
    public void received(String text,boolean link){MainActivity a=screen;if(a!=null)a.received(text,link);else synchronized(inbox){if(inbox.size()==5)inbox.remove();inbox.add(new Message(text,link));}}
    public boolean openFile(String name){MainActivity a=screen;if(a==null)return false;a.requestOpen(name);return true;}
    public JSONObject media(){return MediaActivity.snapshot();}
    public boolean control(String action){return MediaActivity.control(action);}
    public void changed(String event){status=event;MainActivity a=screen;if(a!=null)a.changed(event);}
    public synchronized void busy(boolean active){if(active){if(busyCount++==0)wake.acquire(35*60_000L);}else{busyCount=Math.max(0,busyCount-1);if(busyCount==0&&wake.isHeld())wake.release();}}
    @Override public void onDestroy(){instance=null;main.removeCallbacksAndMessages(null);if(bridge!=null)bridge.close();bridge=null;if(network!=null)((android.net.ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE)).unregisterNetworkCallback(network);if(wake!=null&&wake.isHeld())wake.release();if(screen!=null)screen.connectionChanged();screen=null;super.onDestroy();}
}
