package io.github.smileplzz.tvbridge;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Small, single-request HTTP/1.1 transport. No request body is read before authorization. */
public final class HttpWire {
    public static final long MAX_UPLOAD = 1024L * 1024 * 1024;
    public record Request(String method, String path, Map<String,String> headers, long length) {}
    public static Request read(InputStream in) throws IOException {
        String first = line(in, 4096);
        String[] parts = first.split(" ");
        if (parts.length != 3 || !parts[2].equals("HTTP/1.1")) throw new IOException("Invalid HTTP request");
        Map<String,String> headers = new HashMap<>();
        int total = first.length();
        for (int i=0; i<64; i++) {
            String s = line(in, 4096); total += s.length();
            if (total > 16384) throw new IOException("Headers too large");
            if (s.isEmpty()) {
                if (headers.containsKey("transfer-encoding")) throw new IOException("Chunked requests unsupported");
                long length;
                try { length = Long.parseLong(headers.getOrDefault("content-length", "0")); }
                catch (NumberFormatException e) { throw new IOException("Invalid length"); }
                if (length < 0 || length > MAX_UPLOAD) throw new IOException("Body too large (1 GiB maximum)");
                if (!parts[1].startsWith("/") || parts[1].contains("#")) throw new IOException("Invalid target");
                return new Request(parts[0], parts[1], headers, length);
            }
            int p = s.indexOf(':');
            if (p < 1) throw new IOException("Invalid header");
            String key = s.substring(0,p).toLowerCase(Locale.ROOT);
            if (headers.put(key, s.substring(p+1).trim()) != null) throw new IOException("Duplicate header");
        }
        throw new IOException("Too many headers");
    }
    private static String line(InputStream in, int limit) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i=0; i<limit; i++) {
            int b = in.read(); if (b<0) throw new EOFException();
            if (b=='\n') {
                byte[] a=out.toByteArray();
                if (a.length==0 || a[a.length-1]!='\r') throw new IOException("CRLF required");
                return new String(a,0,a.length-1,StandardCharsets.US_ASCII);
            }
            out.write(b);
        }
        throw new IOException("Line too long");
    }
    public static void copy(InputStream in, OutputStream out, long length) throws IOException {
        byte[] buffer = new byte[65536];
        while (length>0) {
            int n=in.read(buffer,0,(int)Math.min(buffer.length,length));
            if (n<0) throw new EOFException("Incomplete transfer");
            out.write(buffer,0,n); length-=n;
        }
    }
    public static void headers(OutputStream out,int status,String type,long length,String extra) throws IOException {
        String reason = status==200?"OK":status==202?"Accepted":status==204?"No Content":status==401?"Unauthorized":status==403?"Forbidden":status==404?"Not Found":status==409?"Conflict":status==429?"Too Many Requests":"Error";
        String h="HTTP/1.1 "+status+" "+reason+"\r\nContent-Type: "+type+"\r\nContent-Length: "+length+"\r\nConnection: close\r\nCache-Control: no-store\r\nX-Content-Type-Options: nosniff\r\nReferrer-Policy: no-referrer\r\nContent-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'\r\n"+extra+"\r\n";
        out.write(h.getBytes(StandardCharsets.US_ASCII));
    }
    public static String safeName(String name) {
        String s=name.replaceAll("[^\\p{L}\\p{N} ._()-]", "_").replaceAll("^[. ]+", "").trim();
        if (s.length()>100) s=s.substring(s.length()-100);
        if (s.endsWith(".part")) s+="_shared";
        return s.isEmpty()?"file":s;
    }
}
