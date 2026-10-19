package io.github.smileplzz.tvbridge;

import android.graphics.Bitmap;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Fixed version 4-L, byte mode, mask 0. Local IPv4 URLs fit the 78-byte capacity. */
final class QrCode {
    static final int N=33;
    final boolean[][] m=new boolean[N][N],reserved=new boolean[N][N];
    void set(int x,int y,boolean bit){if(x>=0&&y>=0&&x<N&&y<N){m[y][x]=bit;reserved[y][x]=true;}}
    void finder(int x,int y){for(int dy=-1;dy<=7;dy++)for(int dx=-1;dx<=7;dx++){int d=Math.max(Math.abs(dx-3),Math.abs(dy-3));set(x+dx,y+dy,d!=2&&d!=4);}}
    void format(){int bits=0x77c4;for(int i=0;i<=5;i++)set(8,i,((bits>>>i)&1)!=0);set(8,7,((bits>>>6)&1)!=0);set(8,8,((bits>>>7)&1)!=0);set(7,8,((bits>>>8)&1)!=0);for(int i=9;i<15;i++)set(14-i,8,((bits>>>i)&1)!=0);for(int i=0;i<8;i++)set(N-1-i,8,((bits>>>i)&1)!=0);for(int i=8;i<15;i++)set(8,N-15+i,((bits>>>i)&1)!=0);set(8,N-8,true);}
    static int mul(int a,int b){int p=0;for(int i=7;i>=0;i--){p=(p<<1)^((p>>>7)*0x11d);p^=((b>>>i)&1)*a;}return p;}
    static byte[] encode(String text){
        byte[] raw=text.getBytes(StandardCharsets.UTF_8);if(raw.length>78)throw new IllegalArgumentException("URL too long");
        ArrayList<Integer> bits=new ArrayList<>();add(bits,4,4);add(bits,raw.length,8);for(byte b:raw)add(bits,b&255,8);add(bits,0,4);while(bits.size()%8!=0)bits.add(0);
        byte[] data=new byte[80];int count=bits.size()/8;for(int i=0;i<bits.size();i++)data[i/8]|=(byte)(bits.get(i)<<(7-i%8));for(int i=count;i<80;i++)data[i]=(byte)((i-count)%2==0?0xec:0x11);
        int[] divisor=new int[20];divisor[19]=1;int root=1;for(int i=0;i<20;i++){for(int j=0;j<20;j++){divisor[j]=mul(divisor[j],root);if(j+1<20)divisor[j]^=divisor[j+1];}root=mul(root,2);}
        int[] rem=new int[20];for(byte b:data){int factor=(b&255)^rem[0];System.arraycopy(rem,1,rem,0,19);rem[19]=0;for(int j=0;j<20;j++)rem[j]^=mul(divisor[j],factor);}
        byte[] all=Arrays.copyOf(data,100);for(int i=0;i<20;i++)all[80+i]=(byte)rem[i];return all;
    }
    static void add(List<Integer> bits,int value,int count){for(int i=count-1;i>=0;i--)bits.add((value>>>i)&1);}
    static boolean[][] matrix(String text){
        QrCode q=new QrCode();q.finder(0,0);q.finder(N-7,0);q.finder(0,N-7);
        for(int i=8;i<N-8;i++){q.set(i,6,i%2==0);q.set(6,i,i%2==0);}
        for(int dy=-2;dy<=2;dy++)for(int dx=-2;dx<=2;dx++)q.set(26+dx,26+dy,Math.max(Math.abs(dx),Math.abs(dy))!=1);
        q.format();byte[] bytes=encode(text);int index=0;
        for(int right=N-1;right>=1;right-=2){if(right==6)right=5;for(int vert=0;vert<N;vert++){int y=((right+1)&2)==0?N-1-vert:vert;for(int j=0;j<2;j++){int x=right-j;if(!q.reserved[y][x]){boolean b=index<800&&((bytes[index>>>3]>>>(7-(index&7)))&1)!=0;index++;q.m[y][x]=b^((x+y)%2==0);}}}}
        return q.m;
    }
    static Bitmap bitmap(String text){boolean[][] m=matrix(text);int scale=6,size=(N+8)*scale;Bitmap b=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);b.eraseColor(0xffffffff);for(int y=0;y<N;y++)for(int x=0;x<N;x++)if(m[y][x])for(int dy=0;dy<scale;dy++)for(int dx=0;dx<scale;dx++)b.setPixel((x+4)*scale+dx,(y+4)*scale+dy,0xff000000);return b;}
}
