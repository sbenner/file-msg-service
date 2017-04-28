package commons.types;

import commons.enums.MessageType;

/**
 * Created with IntelliJ IDEA.
 * User: sbenner
 * Date: 12/9/16
 * Time: 5:50 AM
 */
public class DataChunk {
    private boolean isNew=false;
    private long seqNum=0;
    private int fullMessageSize=0;
    private int crc=0;
    private MessageType messageType;
    private byte[]  rawData;
    public DataChunk(){

    }

    @Override
    public String toString() {
        String sb = " MessageType: " + getMessageType().name() +
                " Message Size: " + getFullMessageSize() +
                " crc: " + getCrc() +
                " current data size: " + getTotal();
        return sb;    //To change body of overridden methods use File | Settings | File Templates.
    }

    public int getCrc() {
        return crc;
    }

    public void setCrc(int crc) {
        this.crc = crc;
    }



    public void setRawData(byte[] b) {
          this.rawData=b;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean aNew) {
        isNew = aNew;
    }

    public long getSeqNum() {
        return seqNum;
    }

    public void setSeqNum(long seqNum) {
        this.seqNum = seqNum;
    }

    public byte[] getRawData() {
        return rawData;
    }

    public int getTotal() {
        return getRawData().length;
    }



    public int getFullMessageSize() {
        return fullMessageSize;
    }

    public void setFullMessageSize(int fullMessageSize) {
        this.fullMessageSize = fullMessageSize;
    }
}