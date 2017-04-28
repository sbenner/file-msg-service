package commons.types;

/**
 * Created by sergey on 12/3/16.
 */
public class FileMessage extends Message {

    private String fileName;

    private String fileType;

    private String md5;
    private long fileSize;


    private int messageSize;

    public FileMessage() {
        setType((byte)1);
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public int getMessageSize() {
        return getPayload().length;
    }

    public void setMessageSize(int len) {
        this.messageSize = len;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
}
