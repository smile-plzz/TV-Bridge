package io.github.smileplzz.tvbridge;

import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;
import java.io.*;

public final class SharedFileProvider extends ContentProvider {
    public boolean onCreate(){return true;}
    private File file(Uri uri)throws FileNotFoundException {
        try {
            File root=new File(getContext().getFilesDir(),"shared").getCanonicalFile();
            File f=new File(root,uri.getLastPathSegment()).getCanonicalFile();
            if(!root.equals(f.getParentFile())||!f.isFile()||f.getName().endsWith(".part"))throw new IOException();
            return f;
        }catch(Exception e){throw new FileNotFoundException();}
    }
    public String getType(Uri uri){String name=uri.getLastPathSegment();int p=name.lastIndexOf('.');String t=p<0?null:MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substring(p+1).toLowerCase(java.util.Locale.ROOT));return t==null?"application/octet-stream":t;}
    public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException {if(!"r".equals(mode))throw new FileNotFoundException("Read only");return ParcelFileDescriptor.open(file(uri),ParcelFileDescriptor.MODE_READ_ONLY);}
    public Cursor query(Uri uri,String[] projection,String selection,String[] args,String order){try{File f=file(uri);String[] cols=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;MatrixCursor c=new MatrixCursor(cols);Object[] row=new Object[cols.length];for(int i=0;i<cols.length;i++)row[i]=OpenableColumns.DISPLAY_NAME.equals(cols[i])?f.getName():OpenableColumns.SIZE.equals(cols[i])?f.length():null;c.addRow(row);return c;}catch(Exception e){return null;}}
    public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}
    public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
    public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
}
