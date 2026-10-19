package io.github.smileplzz.tvbridge;
public class QrDump {
    public static void main(String[] args){for(String text:args){boolean[][] m=QrCode.matrix(text);System.out.println(text);for(boolean[] row:m){for(boolean b:row)System.out.print(b?'1':'0');System.out.println();}}}
}
