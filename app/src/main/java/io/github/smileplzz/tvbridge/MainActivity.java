package io.github.smileplzz.tvbridge;

import android.app.*;
import android.os.*;
import android.content.*;
import android.net.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import android.graphics.Typeface;
import java.io.*;
import java.util.*;

public final class MainActivity extends Activity implements BridgeServer.Listener {
    BridgeServer bridge;
    BridgeService connection;
    boolean bound;
    String filter="all";
    LinearLayout root,content;
    TextView state;
    boolean visible;
    final Handler ui=new Handler(Looper.getMainLooper());
    final ArrayDeque<Runnable> dialogs=new ArrayDeque<>();
    boolean dialogOpen;
    AlertDialog activeDialog;
    String page="home";
    String boundIp;
    final ServiceConnection serviceConnection=new ServiceConnection(){public void onServiceConnected(ComponentName n,IBinder binder){connection=((BridgeService.LocalBinder)binder).service();bridge=connection.bridge;connection.attach(MainActivity.this);home();}public void onServiceDisconnected(ComponentName n){connection=null;bridge=null;home();}};
    int dp(int n){return (int)(n*getResources().getDisplayMetrics().density);}
    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);}
    @Override public void onStart(){super.onStart();visible=true;startForegroundService(new Intent(this,BridgeService.class));bound=bindService(new Intent(this,BridgeService.class),serviceConnection,Context.BIND_AUTO_CREATE);}
    @Override public void onStop(){visible=false;dialogs.clear();if(activeDialog!=null)activeDialog.dismiss();dialogOpen=false;if(connection!=null)connection.detach(this);if(bound){unbindService(serviceConnection);bound=false;}connection=null;super.onStop();}
    String ip(){
        android.net.ConnectivityManager cm=(android.net.ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
        LinkProperties props=cm.getLinkProperties(cm.getActiveNetwork());
        if(props!=null)for(LinkAddress a:props.getLinkAddresses())if(a.getAddress() instanceof java.net.Inet4Address && a.getAddress().isSiteLocalAddress())return a.getAddress().getHostAddress();
        return null;
    }
    void startBridge(){
        if(connection!=null)connection.restart();else{startForegroundService(new Intent(this,BridgeService.class));if(!bound)bound=bindService(new Intent(this,BridgeService.class),serviceConnection,Context.BIND_AUTO_CREATE);}
    }
    void stopBridge(){if(connection!=null)connection.detach(this);if(bound){unbindService(serviceConnection);bound=false;}connection=null;bridge=null;stopService(new Intent(this,BridgeService.class));home();}
    void connectionChanged(){ui.post(()->{bridge=connection==null?null:connection.bridge;home();});}
    TextView label(String text,int size,int color){TextView t=new TextView(this);t.setText(text);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(5),0,dp(5));return t;}
    Button button(String text,Runnable action){Button b=new Button(this);b.setText(text);b.setAllCaps(false);b.setTextSize(17);b.setPadding(dp(20),dp(8),dp(20),dp(8));b.setOnClickListener(v->action.run());b.setOnFocusChangeListener((v,focus)->{v.setScaleX(focus?1.04f:1f);v.setScaleY(focus?1.04f:1f);});return b;}
    void base(String title){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(Color.rgb(12,20,33));
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(42),dp(24),dp(42),dp(24));scroll.addView(root);setContentView(scroll);
        TextView h=label(title,32,Color.WHITE);h.setTypeface(null,Typeface.BOLD);root.addView(h);
    }
    void home(){
        page="home";
        base("TV Bridge");root.addView(label("Your phone. Your TV. One local connection.",18,0xff9eb0c5));
        if(bridge==null){root.addView(label(connection==null?"Bridge stopped. Start it when you want to connect your phone.":connection.status,22,Color.WHITE));root.addView(button("Start / retry connection",this::startBridge));return;}
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);root.addView(row);
        String url="http://"+bridge.host;
        ImageView qr=new ImageView(this);qr.setImageBitmap(QrCode.bitmap(url));qr.setContentDescription("Scan to open TV Bridge on your phone");LinearLayout.LayoutParams qp=new LinearLayout.LayoutParams(dp(190),dp(190));qp.setMargins(0,dp(12),dp(28),dp(12));row.addView(qr,qp);
        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);row.addView(info,new LinearLayout.LayoutParams(0,-2,1));
        info.addView(label("1  Scan with your phone camera",22,Color.WHITE));
        TextView address=label(url,24,0xff70e4c3);address.setTextIsSelectable(true);info.addView(address);
        info.addView(label("2  Enter code:  "+bridge.code,28,Color.WHITE));
        info.addView(label("3  Approve the connection on this TV",20,Color.WHITE));
        info.addView(label("Same Wi-Fi · Pair within 10 minutes · Session lasts 1 hour",15,0xff9eb0c5));
        state=label("Ready to pair",17,0xff70e4c3);root.addView(state);
        LinearLayout actions=new LinearLayout(this);root.addView(actions);
        Button files=button("Files on TV",this::files);actions.addView(files);
        actions.addView(button("New code / Disconnect all",this::startBridge));
        LinearLayout more=new LinearLayout(this);root.addView(more);
        more.addView(button("Connected phones",this::phones));
        more.addView(button("About & help",this::about));
        more.addView(button("Stop bridge",this::stopBridge));
        root.addView(label("Connection stays active while using other apps. Stop it here or in its notification. Automatically stops after 2 hours.",15,0xff70e4c3));
        root.addView(label("Use only on a trusted home network. Files stay on this TV until you delete them or uninstall TV Bridge.",15,0xff9eb0c5));files.requestFocus();
    }
    void files(){
        page="files";
        base("Shared files");root.addView(label("Received files · Select to open or remove · Refresh after a transfer",17,0xff9eb0c5));
        LinearLayout actions=new LinearLayout(this);root.addView(actions);actions.addView(button("Back to pairing",this::home));actions.addView(button("Refresh",this::files));actions.addView(button("Add file from TV",()->{try{startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE),42);}catch(ActivityNotFoundException e){toast("No file picker is installed. Install a TV-compatible file manager first.");}}));
        LinearLayout categories=new LinearLayout(this);root.addView(categories);for(String kind:new String[]{"all","photos","videos","apps","documents"})categories.addView(button(kind.equals(filter)?"["+kind+"]":kind,()->{filter=kind;files();}));
        File folder=new File(getFilesDir(),"shared");File[] list=folder.listFiles((d,n)->!n.endsWith(".part"));
        if(list==null||list.length==0)root.addView(label("No files yet. Send one from your phone.",22,Color.WHITE));
        else{Arrays.sort(list,Comparator.comparingLong(File::lastModified).reversed());int shown=0;for(File f:list){if(!f.isFile()||(!filter.equals("all")&&bridge!=null&&!bridge.kind(f.getName()).equals(filter)))continue;shown++;root.addView(button(UploadStore.displayName(f.getName())+"  ·  "+android.text.format.Formatter.formatFileSize(this,f.length()),()->new AlertDialog.Builder(this).setTitle(UploadStore.displayName(f.getName())).setItems(new String[]{"Open on TV","Delete file"},(d,n)->{if(n==0)openFile(f);else new AlertDialog.Builder(this).setTitle("Delete this file?").setMessage(UploadStore.displayName(f.getName())).setNegativeButton("Keep",null).setPositiveButton("Delete",(x,y)->{if(!f.delete())toast("Could not delete file");files();}).show();}).show()));}if(shown==0)root.addView(label("No files in this category yet.",20,Color.WHITE));}
    }
    void phones(){page="phones";base("Connected phones");root.addView(button("Back",this::home));if(bridge==null||bridge.sessions.isEmpty()){root.addView(label("No paired phones yet.",22,Color.WHITE));return;}bridge.sessions.entrySet().removeIf(e->System.currentTimeMillis()-e.getValue().created()>60*60_000);for(Map.Entry<String,BridgeServer.Session> entry:bridge.sessions.entrySet()){root.addView(button(entry.getValue().name()+" - Disconnect",()->{bridge.revoke(entry.getKey(),null);phones();}));}root.addView(label("Sessions expire after 1 hour. New code disconnects every phone.",17,0xff9eb0c5));}
    void requestOpen(String name){enqueue(()->show(new AlertDialog.Builder(this).setTitle("Open file from your phone?").setMessage(UploadStore.displayName(name)).setNegativeButton("Cancel",null).setPositiveButton("Open on TV",(d,w)->{try{File f=bridge==null?null:bridge.sharedFile(name);if(f!=null)openFile(f);else toast("File is no longer available.");}catch(IOException e){toast("File is no longer available.");}}).create()));}
    void openFile(File file){
        Uri uri=new Uri.Builder().scheme("content").authority("io.github.smileplzz.tvbridge.files").appendPath(file.getName()).build();
        String type=getContentResolver().getType(uri);
        if("application/vnd.android.package-archive".equals(type)&&!getPackageManager().canRequestPackageInstalls()){
            new AlertDialog.Builder(this).setTitle("Allow APK installation?").setMessage("Android requires you to allow TV Bridge as an installation source. Enable it in Settings, return here, then open the APK again. Only install APKs you trust.").setNegativeButton("Cancel",null).setPositiveButton("Open Android settings",(d,w)->{try{startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName())));}catch(ActivityNotFoundException e){toast("Open your TV’s Security settings to allow installation from TV Bridge.");}}).show();return;
        }
        if(type!=null&&(type.startsWith("image/")||type.startsWith("video/"))){startActivity(new Intent(this,MediaActivity.class).setDataAndType(uri,type));return;}
        try{startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(uri,type).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));}
        catch(ActivityNotFoundException e){toast("No installed app can open this file type.");}
    }
    void about(){
        page="about";base("About TV Bridge");
        root.addView(label("Version 1.1.0 - Ismail Hossain",23,Color.WHITE));
        root.addView(label("A standalone local bridge between your phone browser and Android TV. No account, ads, analytics or cloud uploads.\n\nSend files, text and links. Download shared TV files. Use your phone for playback controls inside TV Bridge.\n\nThe connection stays active while opening media, using a file picker or switching TV apps. Stop it from the home screen or notification. It automatically stops after 2 hours. Pairing codes last 10 minutes and approved phone sessions last 1 hour.\n\nUploads use resumable 2 MiB chunks, up to 1 GiB per file, with a 32 MiB storage reserve. Choose the same file on the same phone browser and TV address to resume. Paused transfers expire after 24 hours. Keep the phone awake while uploading.\n\nLocal HTTP is unencrypted: use only on a trusted home network. Guest Wi-Fi, VPNs and network isolation can prevent connection.\n\nMedia codecs and document pickers depend on the TV. APK installation always needs Android approval. This app does not control other TV apps, mirror screens or read their private diagnostics.\n\nSource and support: github.com/smile-plzz/TV-Bridge\nCreator: github.com/smile-plzz",18,0xffb8c6d8));
        root.addView(button("Back",this::home));
    }
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=42||result!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();toast("Adding file. Keep TV Bridge open.");new Thread(()->{File part=null;try{
        String name="TV-file";try(android.database.Cursor cursor=getContentResolver().query(uri,new String[]{android.provider.OpenableColumns.DISPLAY_NAME},null,null,null)){if(cursor!=null&&cursor.moveToFirst())name=cursor.getString(0);}
        File folder=new File(getFilesDir(),"shared");if(!folder.isDirectory()&&!folder.mkdirs())throw new IOException("Cannot create shared folder");
        File target=new File(folder,System.currentTimeMillis()+"-"+UUID.randomUUID().toString().substring(0,8)+"-"+HttpWire.safeName(name==null?"TV-file":name));part=new File(folder,target.getName()+".part");long total=0;
        try(InputStream in=getContentResolver().openInputStream(uri);FileOutputStream out=new FileOutputStream(part)){if(in==null)throw new IOException("Cannot read file");byte[] buf=new byte[65536];int n;while((n=in.read(buf))!=-1){total+=n;if(total>HttpWire.MAX_UPLOAD||folder.getUsableSpace()<32L*1024*1024)throw new IOException("File too large or TV storage is full");out.write(buf,0,n);}out.getFD().sync();}
        if(!part.renameTo(target))throw new IOException("Cannot save file");ui.post(()->{if(visible){toast("File shared. Refresh the file list on your phone.");files();}});
    }catch(Exception e){ui.post(()->toast("Could not add file: "+e.getMessage()));}finally{if(part!=null)part.delete();}},"bridge-import").start();}
    void toast(String text){Toast.makeText(this,text,Toast.LENGTH_LONG).show();}
    void enqueue(Runnable r){ui.post(()->{if(!visible||dialogs.size()>=4)return;dialogs.add(r);nextDialog();});}
    void nextDialog(){if(!dialogOpen&&!dialogs.isEmpty()&&visible){dialogOpen=true;dialogs.remove().run();}}
    void show(AlertDialog d){activeDialog=d;d.setOnDismissListener(x->{activeDialog=null;dialogOpen=false;nextDialog();});d.show();}
    public void pair(String name,String id){BridgeServer owner=bridge;enqueue(()->{if(owner==null||owner!=bridge){dialogOpen=false;nextDialog();return;}show(new AlertDialog.Builder(this).setTitle("Connect "+name+"?").setMessage("This phone can send text and links, upload files, and download or delete files in TV Bridge’s shared folder. Approve only a request you started.").setNegativeButton("Decline",(d,w)->owner.approve(id,false)).setPositiveButton("Allow",(d,w)->owner.approve(id,true)).create());});}
    public void received(String text,boolean link){enqueue(()->{AlertDialog.Builder d=new AlertDialog.Builder(this).setTitle(link?"Open this link?":"Text from your phone").setMessage(text).setNegativeButton("Close",null);if(link)d.setPositiveButton("Open on TV",(x,y)->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(text)));}catch(ActivityNotFoundException e){toast("No installed app can open this link.");}});else d.setPositiveButton("Copy text",(x,y)->{((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("TV Bridge",text));toast("Text copied. Other apps must support paste.");});show(d.create());});}
    public void changed(String event){ui.post(()->{if(visible&&state!=null)state.setText(event);});}
    @Override public void onBackPressed(){if(!"home".equals(page)){home();return;}if(bridge==null){finish();return;}new AlertDialog.Builder(this).setTitle("Leave TV Bridge?").setMessage("Keep the connection running to transfer files while using other apps, or stop it now.").setNeutralButton("Stay here",null).setNegativeButton("Stop and exit",(d,w)->{stopBridge();finish();}).setPositiveButton("Keep running",(d,w)->finish()).show();}
}
