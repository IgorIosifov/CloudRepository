

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileMessage extends AbstractMessage {
    private String filename;
    private byte[] data;
    private byte[] file;



    public String getFilename() {
        return filename;
    }

    public byte[] getData() {
        return data;
    }



    public FileMessage(Path path) throws IOException {
        filename = path.getFileName().toString();
        data = Files.readAllBytes(path);
    }


    public byte[] getFile() {
        return file;
    }

    public FileMessage(byte[] file) {
        this.file = file;
    }


}
