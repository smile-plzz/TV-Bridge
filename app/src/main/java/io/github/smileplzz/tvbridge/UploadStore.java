package io.github.smileplzz.tvbridge;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Durable, capability-addressed uploads. Each chunk commits completely or rolls back. */
final class UploadStore {
    static final int CHUNK=2*1024*1024;
    final File staging, shared;
    record Item(String id,String name,String signature,long size,long offset,String target,boolean complete) {}
    UploadStore(File staging,File shared)throws IOException {
        this.staging=staging;this.shared=shared;
        if(!staging.isDirectory()&&!staging.mkdirs())throw new IOException("Cannot create transfer storage");
        if(!shared.isDirectory()&&!shared.mkdirs())throw new IOException("Cannot create shared storage");
        File[] files=staging.listFiles();if(files!=null)for(File f:files)if(System.currentTimeMillis()-f.lastModified()>24*60*60_000L)f.delete();
    }
    File meta(String id)throws IOException{if(!id.matches("[a-f0-9-]{36}"))throw new IOException("Invalid transfer ID");return new File(staging,id+".properties");}
    File part(String id)throws IOException{meta(id);return new File(staging,id+".part");}
    synchronized Item start(String name,long size,String signature)throws IOException {
        if(size<1||size>HttpWire.MAX_UPLOAD)throw new IOException("Choose a nonempty file up to 1 GiB");
        if(signature.length()>400)throw new IOException("Invalid file identity");
        File[] existing=staging.listFiles((d,n)->n.endsWith(".properties"));if(existing!=null&&existing.length>=8)throw new IOException("Too many paused transfers. Discard one before starting another.");
        if(size>shared.getUsableSpace()-32L*1024*1024)throw new IOException("Not enough TV storage");
        String id=UUID.randomUUID().toString();name=HttpWire.safeName(name);
        Properties p=new Properties();p.setProperty("name",name);p.setProperty("size",Long.toString(size));p.setProperty("signature",signature);p.setProperty("target",System.currentTimeMillis()+"-"+id.substring(0,8)+"-"+name);
        try(FileOutputStream o=new FileOutputStream(meta(id))){p.store(o,"TV Bridge transfer");o.getFD().sync();}
        try(FileOutputStream o=new FileOutputStream(part(id))){o.getFD().sync();}
        return status(id);
    }
    synchronized Item status(String id)throws IOException {
        Properties p=new Properties();try(InputStream in=new FileInputStream(meta(id))){p.load(in);}
        String target=p.getProperty("target");if(target==null||!new File(shared,target).getCanonicalFile().getParentFile().equals(shared.getCanonicalFile()))throw new IOException("Invalid transfer");
        long size;try{size=Long.parseLong(p.getProperty("size"));}catch(Exception e){throw new IOException("Invalid transfer size");}
        boolean complete=new File(shared,target).isFile();long offset=complete?size:part(id).length();
        return new Item(id,p.getProperty("name"),p.getProperty("signature"),size,offset,target,complete);
    }
    synchronized Item chunk(String id,long offset,InputStream in,long length)throws IOException {
        Item item=status(id);if(item.complete)return item;
        if(offset!=item.offset)throw new IOException("Transfer offset changed. Resume from the reported position.");
        if(length<1||length>CHUNK||length>item.size-offset)throw new IOException("Invalid transfer chunk");
        if(length>shared.getUsableSpace()-32L*1024*1024)throw new IOException("TV storage is full");
        try(RandomAccessFile out=new RandomAccessFile(part(id),"rw")){
            out.seek(offset);byte[] buf=new byte[65536];long remaining=length;
            try{while(remaining>0){int n=in.read(buf,0,(int)Math.min(remaining,buf.length));if(n<0)throw new EOFException("Chunk interrupted");out.write(buf,0,n);remaining-=n;}out.getFD().sync();}
            catch(IOException e){out.setLength(offset);throw e;}
        }
        meta(id).setLastModified(System.currentTimeMillis());return status(id);
    }
    synchronized Item finish(String id)throws IOException {
        Item item=status(id);if(item.complete)return item;
        if(item.offset!=item.size)throw new IOException("Transfer is not complete");
        java.nio.file.Files.move(part(id).toPath(),new File(shared,item.target).toPath(),java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        return status(id);
    }
    synchronized void discard(String id)throws IOException {Item item=status(id);part(id).delete();meta(id).delete();}
    synchronized void forgetCompleted(String id)throws IOException {if(status(id).complete)meta(id).delete();}
    static String sha256(File file)throws Exception {MessageDigest d=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(file)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)d.update(b,0,n);}StringBuilder s=new StringBuilder();for(byte b:d.digest())s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}
    static String displayName(String name){return name.replaceFirst("^[0-9]{13}-[a-f0-9]{8}-","");}
}
