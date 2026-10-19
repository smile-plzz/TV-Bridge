package io.github.smileplzz.tvbridge;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.io.*;

/** Local media only. Uses platform codecs and samples photos to protect TV memory. */
public final class MediaActivity extends Activity {
    VideoView video;
    Bitmap bitmap;
    @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);String type=getIntent().getType();
        if(type!=null&&type.startsWith("video/")){
            FrameLayout layout=new FrameLayout(this);layout.setBackgroundColor(Color.BLACK);video=new VideoView(this);FrameLayout.LayoutParams vp=new FrameLayout.LayoutParams(-1,-1,Gravity.CENTER);layout.addView(video,vp);setContentView(layout);
            MediaController controls=new MediaController(this);controls.setAnchorView(video);video.setMediaController(controls);video.setVideoURI(getIntent().getData());video.setOnPreparedListener(p->{video.requestFocus();video.start();controls.show(4000);});video.setOnErrorListener((p,w,e)->{Toast.makeText(this,"This TV cannot play this video format.",Toast.LENGTH_LONG).show();finish();return true;});
        }else{
            ImageView image=new ImageView(this);image.setBackgroundColor(Color.BLACK);image.setScaleType(ImageView.ScaleType.FIT_CENTER);image.setContentDescription("Shared photo. Press Back to return.");setContentView(image);
            new Thread(()->{try{BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;try(InputStream in=getContentResolver().openInputStream(getIntent().getData())){BitmapFactory.decodeStream(in,null,options);}options.inSampleSize=1;while(options.outWidth/options.inSampleSize>1920||options.outHeight/options.inSampleSize>1080)options.inSampleSize*=2;options.inJustDecodeBounds=false;Bitmap decoded;try(InputStream in=getContentResolver().openInputStream(getIntent().getData())){decoded=BitmapFactory.decodeStream(in,null,options);}if(decoded==null)throw new IOException("Unsupported photo");runOnUiThread(()->{if(isDestroyed())decoded.recycle();else{bitmap=decoded;image.setImageBitmap(bitmap);}});}catch(Exception|OutOfMemoryError e){runOnUiThread(()->{Toast.makeText(this,"Unable to display this photo format.",Toast.LENGTH_LONG).show();finish();});}},"bridge-photo").start();
        }
    }
    @Override public boolean onKeyDown(int key,KeyEvent event){if(video!=null&&(key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)){if(video.isPlaying())video.pause();else video.start();return true;}return super.onKeyDown(key,event);}
    @Override public void onPause(){if(video!=null)video.pause();super.onPause();}
    @Override public void onDestroy(){if(video!=null)video.stopPlayback();if(bitmap!=null)bitmap.recycle();super.onDestroy();}
}
