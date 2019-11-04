import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


class ServiceMessages extends AbstractMessage {
    private String msg;
    private ArrayList fileList;


    String getMsg() {
        return msg;
    }

    ArrayList getFileList() {
        return fileList;
    }

    ServiceMessages(ArrayList fileList) {
        this.fileList = fileList;
    }

    ServiceMessages(String msg) {
        this.msg = msg;
    }
}
