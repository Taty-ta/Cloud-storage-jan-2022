package com.geekbrains.cloud.client;

import com.geekbrains.cloud.utills.SenderUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.ResourceBundle;

public class ClientController implements Initializable {

    public ListView<String> clientView;
    public ListView<String> serverView;
    public TextField textField;
    public Label clientLabel;
    public Label serverLabel;

    private DataInputStream is;
    private DataOutputStream os;

    private File currentDir;
    private byte[] buf; // буфер для чтения ..

    private void read() {
        try {
            while (true) {
                String command = is.readUTF();
                System.out.println("Received command1: " + command);
                // от сервера будем ждать файлы,.
                // если команда лист, то дальше будем
                // вычитывать количество файлов
                if (command.equals("#LIST")) {
                    Platform.runLater(() -> serverView.getItems().clear());
                    int count = is.readInt();
                    for (int i = 0; i < count; i++) {
                        String fileName = is.readUTF();
                        Platform.runLater(() -> serverView.getItems().add(fileName));
                    }
                }
                //сервер может отдавать файл
                if (command.equals("#SEND#FILE")) {
                    SenderUtils.getFileFromInputStream(is, currentDir);
                    //файл скачался и сервер обновился
                    Platform.runLater(this::fillCurrentDirFiles);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // reconnect to server
        }
    }

    private void fillCurrentDirFiles() {
        // получить список файлов в текущей директории
        clientView.getItems().clear();
        // добавить навигацию
        clientView.getItems().add("..");
        clientView.getItems().addAll(currentDir.list()); // listFile - список файлов вернет
        clientLabel.setText(getClientFilesDetails());

    }

    // отправим данные в метку на клиенте
    private String getClientFilesDetails() {
        File[] files = currentDir.listFiles();
        long size = 0;
        String label;
        if (files != null) {
            label = files.length + " files in current dir. ";
            for (File file : files) {
                size += file.length();
            }
            label = "Summary size: " + size + " bytes.";

        } else {
            label = "Current dir is empty";
        }
        return label;
    }

    // обработчик событий на .. (двойные щелчки на папках)
    private void initClickListener() {
        // ! переделать не проваливаться в папку, а слать запрос - сервер провались в папку
        // что то как в процедуре Скачать
        clientView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String fileName = clientView.getSelectionModel().getSelectedItem();
                System.out.println("Выбран файл: " + fileName);
                Path path = currentDir.toPath().resolve(fileName);
                if (Files.isDirectory(path)) {
                    currentDir = path.toFile();
                    fillCurrentDirFiles();// обновим файлы в папке

                }
            }
        });
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            buf = new byte[256];
            currentDir = new File(System.getProperty("user.home"));
            fillCurrentDirFiles();
            initClickListener();
            Socket socket = new Socket("localhost", 8189);
            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());
            Thread readThread = new Thread(this::read);
            readThread.setDaemon(true);
            readThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // по кнопке скачать
    public void downLoad(ActionEvent actionEvent) throws IOException {
        String fileName = serverView.getSelectionModel().getSelectedItem();
        os.writeUTF("#GET#FILE");// отправляем команду сервер дай файл
        os.writeUTF(fileName); // с таким то именем
        os.flush();
    }

    // по кнопке Загрузить
    public void upLoad(ActionEvent actionEvent) throws IOException {
        //убрали ручной ввод в текстовом поле, при нажатии на ентер файлик должен улететь на сервер
        String fileName = clientView.getSelectionModel().getSelectedItem();
        File currentFile = currentDir.toPath().resolve(fileName).toFile();
        //отправляем на сервер команду сендфайл,имя файла, размер и данные из этого файла
        // вынесли в отдельный класс
        SenderUtils.loadFileToOutputStream(os, currentFile);

    }
}
