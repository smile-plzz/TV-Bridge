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
    LinearLayout root,content;
    TextView state;
    boolean visible;
    final Handler ui=new Handler(Looper.getMainLooper());
    final ArrayDeque<Runnable> dialogs=new ArrayDeque<>();
    boolean dialogOpen;
    AlertDialog activeDialog;
    String page="home";
    android.net.ConnectivityManager.NetworkCallback networkCallback;
    String boundIp;
    int dp(int n){return (int)(n*getResources().getDisplayMetrics().density);}
    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);}
    @Override public void onStart(){super.onStart();visible=true;startBridge();
        networkCallback=new android.net.ConnectivityManager.NetworkCallback(){public void onLinkPropertiesChanged(Network n,LinkProperties p){ui.post(()->{if(visible&&!Objects.equals(ip(),boundIp)){stopBridge();startBridge();}});}};
        ((android.net.ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE)).registerDefaultNetworkCallback(networkCallback);
    }
    @Override public void onStop(){visible=false;dialogs.clear();if(activeDialog!=null)activeDialog.dismiss();dialogOpen=false;stopBridge();if(networkCallback!=null)((android.net.ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE)).unregisterNetworkCallback(networkCallback);super.onStop();}
    String ip(){
        android.net.ConnectivityManager cm=(android.net.ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
        LinkProperties props=cm.getLinkProperties(cm.getActiveNetwork());
        if(props!=null)for(LinkAddress a:props.getLinkAddresses())if(a.getAddress() instanceof java.net.Inet4Address && a.getAddress().isSiteLocalAddress())return a.getAddress().getHostAddress();
        return null;
    }
    void startBridge(){
        boundIp=ip();
        if(boundIp!=null)try{bridge=new BridgeServer(this,boundIp,this);}catch(Exception e){bridge=null;}
        home();
    }
    void stopBridge(){if(bridge!=null){bridge.close();bridge=null;}}
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
        if(bridge==null){root.addView(label(boundIp==null?"Connect the TV to your home Wi-Fi or Ethernet, then retry.":"Could not start on port 8765. Close other bridge apps and retry.",22,Color.WHITE));root.addView(button("Retry connection",()->{stopBridge();startBridge();}));return;}
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
        actions.addView(button("Disconnect all / New code",()->{stopBridge();startBridge();}));
        actions.addView(button("About & help",this::about));
        root.addView(label("Keep this app open during transfers. Use only on a trusted home network.\nFiles stay on this TV until you delete them or uninstall TV Bridge.",15,0xff9eb0c5));files.requestFocus();
    }
    void files(){
        page="files";
        base("Shared files");root.addView(label("Received files · Select to open or remove · Refresh after a transfer",17,0xff9eb0c5));
        LinearLayout actions=new LinearLayout(this);root.addView(actions);actions.addView(button("Back to pairing",this::home));actions.addView(button("Refresh",this::files));actions.addView(button("Add file from TV",()->{try{startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE),42);}catch(ActivityNotFoundException e){toast("No file picker is installed. Install a TV-compatible file manager first.");}}));
        File folder=new File(getFilesDir(),"shared");File[] list=folder.listFiles((d,n)->!n.endsWith(".part"));
        if(list==null||list.length==0)root.addView(label("No files yet. Send one from your phone.",22,Color.WHITE));
        else{Arrays.sort(list,Comparator.comparingLong(File::lastModified).reversed());for(File f:list){if(!f.isFile())continue;root.addView(button(f.getName()+"  ·  "+android.text.format.Formatter.formatFileSize(this,f.length()),()->new AlertDialog.Builder(this).setTitle(f.getName()).setItems(new String[]{"Open with installed app","Delete file"},(d,n)->{if(n==0)openFile(f);else new AlertDialog.Builder(this).setTitle("Delete this file?").setMessage(f.getName()).setNegativeButton("Keep",null).setPositiveButton("Delete",(x,y)->{if(!f.delete())toast("Could not delete file");files();}).show();}).show()));}}
    }
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
    void about(){page="about";base("About TV Bridge");root.addView(label("Version 1.0.0 · Created for Ismail Hossain",23,Color.WHITE));root.addView(label("Standalone phone ↔ Android TV communication. No account, cloud upload, ads, or analytics.\n\nPhone: Safari, Chrome, or another modern browser. TV: Android 8.0 or newer.\n\nOpen http:// plus the exact TV address if your camera cannot open the QR code. Disable VPN or guest-network isolation if connection fails.\n\nTransfers: one file at a time, up to 1 GiB, with at least 32 MiB left free. Keep your phone awake and this app visible. Interrupted transfers can be retried from the start.\n\nMedia and APK opening depend on installed TV apps. Android handles installation confirmation. System-wide remote control and other apps’ diagnostics are not included.\n\nLocal HTTP is unencrypted. Pair only on your trusted network. Press Home or leave TV Bridge to stop the server and revoke sessions.\n\nSource: github.com/smile-plzz/TV-Bridge",18,0xffb8c6d8));root.addView(button("Back",this::home));}
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
    @Override public void onBackPressed(){if("home".equals(page))finish();else home();}
}
