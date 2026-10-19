package io.github.smileplzz.tvbridge;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ProtocolTest {
    static int count;
    static void check(boolean b,String name){if(!b)throw new AssertionError(name);count++;}
    static HttpWire.Request parse(String s)throws Exception{return HttpWire.read(new ByteArrayInputStream(s.getBytes(StandardCharsets.US_ASCII)));}
    static void reject(String s)throws Exception{try{parse(s);throw new AssertionError("Accepted invalid request");}catch(IOException expected){count++;}}
    public static void main(String[] args)throws Exception {
        check(parse("GET / HTTP/1.1\r\nHost: 127.0.0.1:8765\r\n\r\n").length()==0,"bodyless GET");
        check(parse("POST /api/upload HTTP/1.1\r\nContent-Length: 1073741824\r\n\r\n").length()==HttpWire.MAX_UPLOAD,"boundary upload");
        reject("POST / HTTP/1.1\r\nContent-Length: -1\r\n\r\n");
        reject("POST / HTTP/1.1\r\nContent-Length: 1073741825\r\n\r\n");
        reject("POST / HTTP/1.1\r\nContent-Length: x\r\n\r\n");
        reject("POST / HTTP/1.1\r\nContent-Length: 1\r\ncontent-length: 2\r\n\r\n");
        reject("POST / HTTP/1.1\r\nTransfer-Encoding: chunked\r\n\r\n");
        reject("GET / HTTP/1.1\n\n");
        reject("GET http://evil.test/ HTTP/1.1\r\n\r\n");
        reject("GET / HTTP/1.0\r\n\r\n");
        reject("GET / HTTP/1.1\r\nX: "+"a".repeat(5000)+"\r\n\r\n");
        ByteArrayOutputStream out=new ByteArrayOutputStream();HttpWire.copy(new ByteArrayInputStream(new byte[]{1,2,3,4}),out,3);check(Arrays.equals(out.toByteArray(),new byte[]{1,2,3}),"bounded streaming");
        try{HttpWire.copy(new ByteArrayInputStream(new byte[2]),out,3);throw new AssertionError("Truncation accepted");}catch(EOFException expected){count++;}
        check(!HttpWire.safeName("../../a\r\n.txt").contains("/"),"filename traversal");
        check(HttpWire.safeName("...").equals("file"),"empty filename");
        check(HttpWire.safeName("my video.mp4").equals("my video.mp4"),"normal filename");
        check(!HttpWire.safeName("download.part").endsWith(".part"),"reserve partial-transfer suffix");
        System.out.println(count+" protocol checks passed");
    }
}
