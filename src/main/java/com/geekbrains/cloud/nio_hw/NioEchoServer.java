package com.geekbrains.cloud.nio_hw;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
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
            while (buf.hasRemaining()) {
                s.append((char) buf.get());
            }
            buf.clear(); // буфер в изначальную конфигурацию
        }
        // прочитали и иотправляем обратно
        System.out.println("Received: " + s);

        Path path = Paths.get("serverDir");

        if (s.toString().equals("ls\r\n".toString())) {
            outputFilesDir(channel, path); //список файлов в директории
        } else if (s.toString().equals("cd dir_name\r\n".toString())) {
            moveToDir(channel, path); //переместиться в директорию
        } else if (s.toString().equals("cat file_name\r\n".toString())) {
            printFile(channel);//распечатать содержание файла на экран
        } else if (s.toString().equals("mkdir dir_name\r\n".toString())) {
            createInDir(); // создать директорию в текущей
        } else if (s.toString().equals("touch file_name\r\n".toString())) {
            createFile();//  создать пустой файл в текущей директории
        }
        //channel.write(ByteBuffer.wrap(s.toString().getBytes(StandardCharsets.UTF_8)));
    }
 //  создать пустой файл в текущей директории
    private void createFile() {
        Path path = Paths.get("serverDir/example.txt");

        try {
            String str = "Some write file Example";
            byte[] bs = str.getBytes();
            Path writtenFilePath = Files.write(path, bs);
            System.out.println("Written content in file:\n" + new String(Files.readAllBytes(writtenFilePath)));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


// создать директорию в текущей
    private void createInDir() {
        Path path  = Paths.get("serverDir/directory");

        try {
            Path newDir = Files.createDirectory(path);
        } catch(FileAlreadyExistsException e){
            // Каталог уже существует
        } catch (IOException e) {
            // Что-то пошло не так при чтении/записи
            e.printStackTrace();
        }

}

    private void printFile(SocketChannel channel) {
        Path filePath = Paths.get("serverDir", "example.txt");

        if (Files.exists(filePath)) {
            try {
                byte[] bytes = Files.readAllBytes(filePath);
                String text = new String(bytes, StandardCharsets.UTF_8);

                channel.write(ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8)));
                System.out.println(text);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void moveToDir(SocketChannel channel, Path path) throws IOException {
        String  text = Files.exists(path)+ " размер: "+ Files.size(path) + " путь: "+ path.toAbsolutePath();
        channel.write(ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8)));
    }
//список файлов в директории
    private void outputFilesDir(SocketChannel channel, Path path) throws IOException {

        Files.walkFileTree(path, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                String str = "pre visit dir:" + dir + "\r\n";
                channel.write(ByteBuffer.wrap(str.getBytes(StandardCharsets.UTF_8)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String str = "visit file: " + file + "\r\n";
                channel.write(ByteBuffer.wrap(str.getBytes(StandardCharsets.UTF_8)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                String str = "visit file failed: " + file + "\r\n";
                channel.write(ByteBuffer.wrap(str.getBytes(StandardCharsets.UTF_8)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                String str = "post visit directory: " + dir + "\r\n";
                channel.write(ByteBuffer.wrap(str.getBytes(StandardCharsets.UTF_8)));
                return FileVisitResult.CONTINUE;
            }
        });

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
