package com.geekbrains.cloud.nio;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

import static java.nio.file.StandardWatchEventKinds.*;

public class PathExample {
    public static void main(String[] args) throws IOException {
        //Для описания пути к файлу в java.nio используется класс Path (аналог класса File в java.io).
        // Чтобы получить экземпляр Path, необходимо вызвать статический метод Paths.get(),
        // в который можно передать абсолютный или относительный путь.
        //Path path = Paths.get("dir/dir1/dir2/file.txt");
        Path path = Paths.get("dir", "dir1", "Книга.xlsx");
        Path dir = Paths.get("serverDir");
        Path fxml = dir.resolve("desktop.ini");
        System.out.println(Files.exists(path));//Files.exists() проверяет существование пути в системе
        System.out.println(Files.size(fxml));
        System.out.println(dir.toAbsolutePath());// полный путь к директории
        startListening(dir); //регистрируем
    }

    private static void startListening(Path path) throws IOException {
        //слушаем измения в файле. Если что то удалим в файле, сам файл удалим, Создадим новый файл в дир
        WatchService service = FileSystems.getDefault().newWatchService();
        new Thread(() -> {
            try {
                while (true) {
                    WatchKey key = service.take();
                    List<WatchEvent<?>> events = key.pollEvents();
                    for (WatchEvent<?> event:events){
                        System.out.println(event.context()+ " "+ event.kind());// на каком файле + тип события на котором произошло

                    }
                    key.reset();// сброс
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }).start();
        // зарегистрировали все виды событий
        path.register(service, ENTRY_CREATE,ENTRY_MODIFY, ENTRY_DELETE) ;
    }


}
