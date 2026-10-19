package io.github.smileplzz.tvbridge;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class UploadStoreTest {
    static int checks;
    interface F {void run()throws Exception;}
    static void check(boolean b,String message){if(!b)throw new AssertionError(message);checks++;}
    static void reject(F f)throws Exception{try{f.run();throw new AssertionError("Invalid operation accepted");}catch(IOException expected){checks++;}}
    public static void main(String[] args)throws Exception {
        Path root=Files.createTempDirectory("tv-bridge-test-");
        try{
            File staging=root.resolve("uploads").toFile(),shared=root.resolve("shared").toFile();UploadStore store=new UploadStore(staging,shared);
            reject(()->store.start("empty",0,"x"));reject(()->store.start("big",HttpWire.MAX_UPLOAD+1,"x"));reject(()->store.status("../outside"));
            UploadStore.Item item=store.start("photo.part",16,"fingerprint");String id=item.id();
            check(item.offset()==0&&!item.name().endsWith(".part"),"initial metadata");
            byte[] data=new byte[16];for(int i=0;i<data.length;i++)data[i]=(byte)i;
            check(store.chunk(id,0,new ByteArrayInputStream(data),8).offset()==8,"chunk commit");
            reject(()->store.chunk(id,0,new ByteArrayInputStream(data),8));
            reject(()->store.chunk(id,8,new ByteArrayInputStream(new byte[2]),8));
            check(store.status(id).offset()==8,"truncated chunk rollback");
            reject(()->store.finish(id));
            UploadStore restored=new UploadStore(staging,shared);check(restored.status(id).offset()==8,"process restart recovery");
            restored.chunk(id,8,new ByteArrayInputStream(Arrays.copyOfRange(data,8,16)),8);UploadStore.Item done=restored.finish(id);
            check(done.complete(),"finalize");check(Arrays.equals(Files.readAllBytes(shared.toPath().resolve(done.target())),data),"resumed byte integrity");
            check(UploadStore.sha256(new File(shared,done.target())).equals("be45cb2605bf36bebde684841a28f0fd43c69850a3dce5fedba69928ee3a8991"),"SHA-256");
            check(restored.finish(id).target().equals(done.target()),"idempotent finish");restored.forgetCompleted(id);reject(()->restored.status(id));
            UploadStore.Item discard=restored.start("discard",4,"x");restored.discard(discard.id());reject(()->restored.status(discard.id()));
            check(UploadStore.displayName("1770000000000-1234abcd-my photo.jpg").equals("my photo.jpg"),"friendly names");
            List<String> ids=new ArrayList<>();for(int i=0;i<8;i++)ids.add(restored.start("file",1,"x").id());reject(()->restored.start("ninth",1,"x"));for(String value:ids)restored.discard(value);
            System.out.println(checks+" resumable storage checks passed");
        }finally{try(var paths=Files.walk(root)){for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}}
    }
}
