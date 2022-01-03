package com.geekbrains.cloud.nio;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

public class NioEchoServer {
    private ServerSocketChannel serverChannel;
    private Selector selector;
    private ByteBuffer buf;

    public NioEchoServer() throws IOException {
        buf = ByteBuffer.allocate(5); // небольшой буфер
        serverChannel = ServerSocketChannel.open();// открываем канал
        selector = Selector.open();
        serverChannel.configureBlocking(false);// в не блокирующем режиме работает
        serverChannel.bind(new InetSocketAddress(8189)); // добавляем порт
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Server started");
       while (serverChannel.isOpen()) {
            selector.select();// блокирующая операция, но собирает событыя со всех каналов
           System.out.println("Keys selected");
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = keys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                //обрабатываем если подключение, если чтение
                if (key.isAcceptable()) {
                    handleAccept();
                }
                if (key.isReadable()) {
                    handleRead(key);
                }
                iterator.remove();
            }

        }
    }

    private void handleRead(SelectionKey key) throws IOException {
        // если чтение то кпнпл должен быть сокетный
        SocketChannel channel = (SocketChannel) key.channel();
        // должны из этого канала все прочитать и отправить обратно
        StringBuilder s = new StringBuilder();
        int read = 0;
        while (true) {
            read = channel.read(buf);//смотрим сколько прочитали
            if (read == 0) {
                break;
            }
            if (read < 0) { // если прочитали меньше 0, то это разрыв канала
                channel.close();
                return;
            }
            // во свех иных ситуациях мы что то записали в буфер
            // и хотим прочитать
            buf.flip();
            while (buf.hasRemaining()){
                s.append((char) buf.get());
            }
            buf.clear(); // буфер в изначальную конфигурацию
        }
        // прочитали и иотправляем обратно
        System.out.println("Received: "+ s);
        channel.write(ByteBuffer.wrap(s.toString().getBytes(StandardCharsets.UTF_8)));
    }

    private void handleAccept() throws IOException {
        SocketChannel channel = serverChannel.accept();
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);// регистрируем чтоние на этом сокете
        // отправим приветствие // wrap - завернули массив байт в буфер
        channel.write(ByteBuffer.wrap(
                "hello user\n\r".getBytes(StandardCharsets.UTF_8)
        ));
        System.out.println("Client accepted");
    }

    public static void main(String[] args) throws IOException {
        new NioEchoServer();
    }
}
