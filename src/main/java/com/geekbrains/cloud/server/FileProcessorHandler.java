package com.geekbrains.cloud.server;

import com.geekbrains.cloud.utills.SenderUtils;

import java.io.*;
import java.net.Socket;
import java.nio.file.Path;

public class FileProcessorHandler implements Runnable {
    private File currentDir;
    private DataOutputStream os;
    private DataInputStream is;
    private byte[] buf;
    private static final int SIZE = 256;

    public FileProcessorHandler(Socket socket) throws IOException {
        is = new DataInputStream(socket.getInputStream());
        os = new DataOutputStream(socket.getOutputStream());
        buf = new byte[SIZE];
        currentDir = new File("serverDir");
        SenderUtils.sendFilesListToOutputStream(os, currentDir);
    }

    @Override
    public void run() {
        try {
            while (true) {
                String command = is.readUTF();
                System.out.println("Received_получено с сервера: " + command);
                if (command.equals("#SEND#FILE#")) {
                    SenderUtils.getFileFromInputStream(is, currentDir);
            //обновилось состояние сервера, нужно послать команду списка
                    SenderUtils.sendFilesListToOutputStream(os, currentDir);
                }
                if (command.equals("#GET#FILE")) {
                    String fileName = is.readUTF();
                    File file = currentDir.toPath().resolve(fileName).toFile();
                    SenderUtils.loadFileToOutputStream(os, file);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
