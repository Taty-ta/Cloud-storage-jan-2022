package com.geekbrains.cloud.utills;

import java.io.*;
import java.nio.file.Path;

public class SenderUtils {
    private static final int SIZE = 256;

    public static void getFileFromInputStream(DataInputStream is, File currentDir) throws IOException {
        String fileName = is.readUTF();
        byte[] buf = new byte[SIZE];
        long size = is.readLong();
        System.out.println("Created file: " + fileName);
        System.out.println("File size: " + size);
        Path currentPath = currentDir.toPath().resolve(fileName);
        try (FileOutputStream fos = new FileOutputStream(currentPath.toFile())) {

            for (int i = 0; i < (size + SIZE - 1) / SIZE; i++) {
                int read = is.read(buf);
                fos.write(buf, 0, read);
            }
        }
    }

    public static void loadFileToOutputStream(DataOutputStream os, File file) throws IOException {
        String fileName = file.getName();
        byte[] buf = new byte[SIZE];
        //отправляем на сервер команду сендфайл,имя файла, размер и данные из этого файла
        os.writeUTF("#SEND#FILE");
        os.writeUTF(fileName);
        os.writeLong(file.length());
        try (FileInputStream is = new FileInputStream(file)) {
            // вычитываем весь инпутстрима и печатаем байтами в отпутстрим, при этом мы снаем что
            // длина файла = количеству байтов
            while (true) {
                int read = is.read(buf);// фиксируем сколько мы прочитали
                if (read == -1) {
                    break;
                }
                os.write(buf, 0, read);// читаем файликом по 256
            }
        }
        os.flush();
    }

    public static void sendFilesListToOutputStream(DataOutputStream os, File currentDir) throws IOException {
        String[] files = currentDir.list();
        if (files != null) {
            os.writeUTF("#LIST");
            os.writeInt(files.length);
            //каждый файлик полылаем
            for (String file : files) {
                os.writeUTF(file);
            }
        }
    }
}
