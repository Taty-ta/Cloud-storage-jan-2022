package com.geekbrains.cloud.nio;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class BuffersExample {
    public static void main(String[] args) {
        ByteBuffer buf =  ByteBuffer.allocate(12); //выделили буфер размером 5 байтов
        buf.putChar('H');
        buf.putChar('e');
        buf.flip();
        System.out.println(buf.getChar());
        System.out.println(buf.getChar());
        buf.rewind();
        System.out.println(buf.getChar());
        System.out.println(buf.getChar());
        buf.clear();
        buf.put("Hello world".getBytes(StandardCharsets.UTF_8));
        buf.flip();
        while (buf.hasRemaining()){
            byte b = buf.get();
            System.out.print((char) b);
        }
        System.out.println();
    }
}
