package client;

import commons.types.FileMessage;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.spi.FileTypeDetector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

import static java.lang.String.format;

/**
 * Created with IntelliJ IDEA.
 * User: sbenner
 * Date: 12/9/16
 * Time: 5:44 AM
 */
public class FileMessageSender extends MessageSender implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(FileMessageSender.class);
    private static DataOutputStream out;
    private File file;
    private Semaphore semaphore;
    private long id;
    private CountDownLatch countDownLatch;
    private String md5;
    private String fileType;


    public FileMessageSender(DataOutputStream out, File file, String md5, Semaphore semaphore, long id, CountDownLatch countDownLatch) {
        setOut(out);
        setFile(file);
        setSemaphore(semaphore);
        setId(id);
        setMd5(md5);
        setCountDownLatch(countDownLatch);
    }

    public static DataOutputStream getOut() {
        return out;
    }

    public static void setOut(DataOutputStream out) {
        FileMessageSender.out = out;
    }

    public void readFileAndSendMessage(DataOutputStream out, File file, long id, String md5) {
        try {
            setFileType(new Tika().detect(file));

            logger.info("sending file " + file.getName());
            logger.info("sending contentType " + getFileType());
            RandomAccessFile aFile = new RandomAccessFile(
                    file, "r");
            FileChannel inChannel = aFile.getChannel();
            ByteBuffer buf = ByteBuffer.allocate(1024);
            long threadId = Thread.currentThread().getId();

            int read;

            while ((read = inChannel.read(buf)) != -1) {

                buf.flip();
                if (read > 0) {
                    byte[] b = new byte[read];
                    buf.get(b);
                    sendFileMessage(out, createMessage(b));
                }
            }

        } catch (IOException e) {
            logger.error(e.getMessage(),e);
        }

    }

    private FileMessage createMessage(byte[] fileContents){
        FileMessage fileMessage = new FileMessage();
        fileMessage.setPayload(ByteBuffer.allocate(fileContents.length).put(fileContents).array());
        fileMessage.setMesageId(getId());
        fileMessage.setFileSize(getFile().length());
        fileMessage.setFileType(getFileType());
        fileMessage.setFileName(getFile().getName());
        fileMessage.setMd5(getMd5());
        return  fileMessage;

    }

    private void sendFileMessage(DataOutputStream outToServer,
                              FileMessage fileMessage) throws IOException {


        byte[] msg = buildMessage(fileMessage, id);
            logger.error(format("sent %s bytes msg #id %s",msg.length,id));
        outToServer.write(msg);
        outToServer.flush();

    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public Semaphore getSemaphore() {
        return semaphore;
    }

    public void setSemaphore(Semaphore semaphore) {
        this.semaphore = semaphore;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public CountDownLatch getCountDownLatch() {
        return countDownLatch;
    }

    public void setCountDownLatch(CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public void run() {
        readFileAndSendMessage(getOut(), getFile(), getId(), getMd5());
        getSemaphore().release();
        getCountDownLatch().countDown();
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
}
