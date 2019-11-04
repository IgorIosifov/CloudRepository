
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML
    ListView<String> localFileList;
    @FXML
    ListView<String> remoteFileList;
    @FXML
    TextField loginField;
    @FXML
    PasswordField passwordField;
    @FXML
    VBox remoteFiles;
    @FXML
    VBox localFiles;
    @FXML
    VBox authorize;
    @FXML
    Button download;
    @FXML
    Button upload;
    @FXML
    Button delete;
    @FXML
    TextField infoPanel;
    @FXML
    Button log_in;
    @FXML
    Button log_out;

    private boolean isAuthorized;
    private String nick;
    private int bufferSize;

    public void setAuthorized(boolean isAuthorized) {
        this.isAuthorized = isAuthorized;
        if (!isAuthorized) {
            download.setVisible(false);
            upload.setVisible(false);
            remoteFiles.setVisible(false);
            loginField.setVisible(true);
            passwordField.setVisible(true);
            log_in.setVisible(true);
            log_out.setVisible(false);
        } else {
            download.setVisible(true);
            upload.setVisible(true);
            localFiles.setVisible(true);
            remoteFiles.setVisible(true);
            loginField.setVisible(false);
            passwordField.setVisible(false);
            log_in.setVisible(false);
            log_out.setVisible(true);

        }

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Network.start();
        Thread t = new Thread(() -> {
            try {
                setAuthorized(false);
                while (true) {
                    AbstractMessage am = Network.readObject();
                    if (am instanceof FileMessage) {
                        FileMessage fm = (FileMessage) am;
                        Files.write(Paths.get("client_storage/" + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);
                        refreshLocalFilesList();
                        infoPanel.setText("file " + localFileList.getSelectionModel().getSelectedItem() + " successfully downloaded");
                    }
                    if (am instanceof ServiceMessages) {
                        ServiceMessages sm = (ServiceMessages) am;
                        if (sm.getFileList() != null) {
                            refreshRemoteFilesList(sm.getFileList());
                        }
                        if (sm.getMsg() != null) {
                            if (sm.getMsg().startsWith("nick")) {
                                setAuthorized(true);
                                infoPanel.clear();
                                Network.sendMsg(new ServiceMessages("/update"));
                                String[] tokens = sm.getMsg().split(">");
                                nick = tokens[1];
                            } else if (sm.getMsg().startsWith("this")) {
                                setAuthorized(false);
                                infoPanel.setText("this user has already logged in");
                            } else if (sm.getMsg().startsWith("buffer")) {
                                String[] tokens = sm.getMsg().split(">");
                                bufferSize = Integer.parseInt(tokens[1]);
                            } else if (sm.getMsg().startsWith("wrong")){
                                setAuthorized(false);
                                infoPanel.setText(sm.getMsg());
                            }else{
                                sendFile("client_storage/"+sm.getMsg());
                            }
                        }
                    }
                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            } finally {
                Network.stop();
            }
        });
        t.setDaemon(true);
        t.start();
        refreshLocalFilesList();
    }


    private void refreshLocalFilesList() {
        updateUI(() -> {
            try {
                localFileList.getItems().clear();
                Files.list(Paths.get("client_storage/")).map(p -> p.getFileName().toString()).forEach(o -> localFileList.getItems().add(o));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void refreshRemoteFilesList(ArrayList remoteList) {
        updateUI(() -> {
            remoteFileList.getItems().clear();
            remoteFileList.getItems().addAll(remoteList);
        });
    }

    private static void updateUI(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }

    public void pressOnDeleteBtn(ActionEvent actionEvent) {

        if (remoteFileList.isFocused()) {
            ServiceMessages am = new ServiceMessages("/delete" + ">" + remoteFileList.getSelectionModel().getSelectedItem());
            Network.sendMsg(am);
            infoPanel.setText("file " + remoteFileList.getSelectionModel().getSelectedItem() + " successfully deleted");
        }
        if (localFileList.isFocused()) {
            Path path = Paths.get("client_storage/" + localFileList.getSelectionModel().getSelectedItem());
            infoPanel.setText("file " + localFileList.getSelectionModel().getSelectedItem() + " successfully deleted");

            try {
                Files.deleteIfExists(path);
                refreshLocalFilesList();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendFile(String source) {

        RandomAccessFile srcFile = null;
        int buffSize = bufferSize;
        try {
            srcFile = new RandomAccessFile(source, "r");
            FileChannel srcChannel = srcFile.getChannel();
            ByteBuffer buff = ByteBuffer.allocate(buffSize);
            int bytesRead = srcChannel.read(buff);
            while (bytesRead != -1) {
                buff.flip();
                FileMessage fm = new FileMessage(buff.array());
                Network.sendMsg(fm);
                buff.clear();
                bytesRead = srcChannel.read(buff);
            }
            srcFile.close();
            ServiceMessages sm = new ServiceMessages("file transfer has been finished");
            Network.sendMsg(sm);
        } catch (IOException e) {
            e.printStackTrace();
        }
        infoPanel.setText("file " + localFileList.getSelectionModel().getSelectedItem() + " successfully uploaded");
    }

    public void pressOnUploadBtn(ActionEvent actionEvent) throws IOException {

        if (localFileList.getSelectionModel().getSelectedItem() != null) {
            FileRequest fr = new FileRequest(localFileList.getSelectionModel().getSelectedItem());
            ServiceMessages sm = new ServiceMessages("new file" + ">" + fr.getFilename());
            Network.sendMsg(sm);

        }
    }

    public void pressOnDownloadBtn(ActionEvent actionEvent) {

        if (remoteFileList.getSelectionModel().getSelectedItem() != null) {
            FileRequest fr = new FileRequest(remoteFileList.getSelectionModel().getSelectedItem());
            Network.sendMsg(new FileRequest(fr.getFilename()));
        }
    }

    public void tryToAuth(ActionEvent actionEvent) {
        AbstractMessage am = new ServiceMessages("/auth" + " " + loginField.getText() + " " + passwordField.getText());
        Network.sendMsg(am);
        loginField.clear();
        passwordField.clear();
    }

    public void logOut(ActionEvent actionEvent) {
        setAuthorized(false);
        AbstractMessage am = new ServiceMessages("logOut");
        Network.sendMsg(am);
    }
}
