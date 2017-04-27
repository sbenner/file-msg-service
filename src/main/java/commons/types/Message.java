package commons.types;

import java.io.Serializable;

/**
 * Created by sergey on 12/3/16.
 */
public class Message implements Serializable {

    private long mesageId;

    private byte type;
    private byte[] payload;

    public Message() {
    }

    private boolean isFinal;

    public Message(byte type, byte[] msg) {
        setType(type);
        setPayload(msg);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("type: ").append(getType()).append(" message len: ").append(getPayload().length);

        return sb.toString();
    }


    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    public long getMesageId() {
        return mesageId;
    }

    public void setMesageId(long mesageId) {
        this.mesageId = mesageId;
    }


    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public void setFinal(boolean aFinal) {
        isFinal = aFinal;
    }
}
