package client;

import commons.types.Message;
import org.apache.commons.lang3.SerializationUtils;

import java.nio.ByteBuffer;


public class MessageSender {


    public MessageSender(){}

    private int makeCRC(byte[] header){
        int crc = 0;
        for (int i = 0; i < 13; i++) {
            crc += header[i] & 0xFF;
        }
        return crc;
    }

    private boolean checkCrc(byte[] header,int crc){
        int total=makeCRC(header);
        return crc==total;
    }

    protected byte[] buildMessage(Message m, long messageId) {

        byte[] serializedMsg = SerializationUtils.serialize(m);
        int size = serializedMsg.length;

        byte[] header = ByteBuffer.allocate(13).putLong(messageId).putInt(size).put(m.getType()).array();
        int crc = makeCRC(header);

        header = ByteBuffer.wrap(new byte[17]).put(header).putInt(crc).array();
        return ByteBuffer.wrap(new byte[17 + size]).put(header).put(serializedMsg).array();
    }

}
