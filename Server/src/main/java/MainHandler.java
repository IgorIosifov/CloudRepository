
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.io.IOException;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.ArrayList;


public class MainHandler extends ChannelInboundHandlerAdapter {
    private String nick = null;
    private String tempFileName = null;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        try {
            if (nick == null) {
                if (msg instanceof ServiceMessages) {
                    String in = ((ServiceMessages) msg).getMsg();
                    if (in.startsWith("/auth")) {
                        String[] tokens = in.split(" ");
                        String nickFromDB = DBService.getNickByLoginAndPass(tokens[1], tokens[2]);
                        if (nickFromDB == null) {
                            ServiceMessages sm = new ServiceMessages("wrong login/pass");
                            ctx.writeAndFlush(sm);
                            return;
                        } else {
                            nick = nickFromDB;
                            if (isNickBusy(ctx)) return;
                            updateRemoteFileList(ctx);
                        }
                    }
                }
            }
            if (nick != null) {
                setBuffer(ctx);
                if (msg instanceof FileRequest) {
                    FileRequest fr = (FileRequest) msg;
                    if (Files.exists(Paths.get("server_storage/" + fr.getFilename()))) {
                        FileMessage fm = new FileMessage(Paths.get("server_storage/" + fr.getFilename()));
                        ctx.writeAndFlush(fm);
                    }
                }
                if (msg instanceof FileMessage) {
                    Files.write(Paths.get("server_storage/" + tempFileName), ((FileMessage) msg).getFile(), StandardOpenOption.APPEND);
                }
                if (msg instanceof ServiceMessages) {
                    String in = ((ServiceMessages) msg).getMsg();
                    if (in.equalsIgnoreCase("/updateRemoteFileList")) {
                        updateRemoteFileList(ctx);
                    }
                    if (in.startsWith("/delete")) {
                        String[] tokens = in.split(">");
                        Path path = Paths.get("server_storage/" + tokens[1]);
                        Files.deleteIfExists(path);
                        updateRemoteFileList(ctx);
                    }
                    if (in.startsWith("logOut")) {
                        DBService.logOut(nick);
                        nick = null;
                    }
                    if (in.startsWith("new file")) {
                        String[] tokens = in.split(">");
                        String fileName = tokens[1];
                        ctx.writeAndFlush(new ServiceMessages(fileName));
                        tempFileName = fileName;
                        Files.createFile(Paths.get("server_storage/" + tempFileName));
                    }
                    if (in.startsWith("file transfer")) {
                        updateRemoteFileList(ctx);
                    }
                }
            }

        } finally {
            ReferenceCountUtil.release(msg);
        }

    }

    private boolean isNickBusy(ChannelHandlerContext ctx) throws SQLException {
        if (!DBService.isNickBusy(nick)) {
            DBService.logIn(nick);
            ServiceMessages sm = new ServiceMessages("nick" + ">" + nick);
            ctx.writeAndFlush(sm);
        } else {
            ServiceMessages sm = new ServiceMessages("this nick is busy");
            ctx.writeAndFlush(sm);
            nick = null;
            return true;
        }
        return false;
    }

    private void setBuffer(ChannelHandlerContext ctx) {
        int bufferSizeForEachClient = 1024 * 1024;
        ServiceMessages bufMsg = new ServiceMessages("buffer size is:" + ">" + bufferSizeForEachClient);
        ctx.writeAndFlush(bufMsg);
    }


    private void updateRemoteFileList(ChannelHandlerContext ctx) {

        try {
            ArrayList<String> fileList = new ArrayList<>();
            Files.list(Paths.get("server_storage/")).map(p -> p.getFileName().toString()).forEach(fileList::add);
            ServiceMessages actualFileList = new ServiceMessages(fileList);
            ctx.writeAndFlush(actualFileList);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        DBService.logOut(nick);
        ctx.close();
    }
}
