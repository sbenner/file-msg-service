package client;

import commons.Utils;
import commons.types.Message;
import org.apache.commons.lang3.SerializationUtils;

import java.nio.ByteBuffer;


public class MessageSender {


    public MessageSender(){}


    protected byte[] buildMessage(Message m, long messageId) {

        byte[] serializedMsg = SerializationUtils.serialize(m);
        int size = serializedMsg.length;

        byte[] header = ByteBuffer.allocate(13).putLong(messageId).putInt(size).put(m.getType()).array();
        int crc = Utils.getCRC(header);

        header = ByteBuffer.wrap(new byte[17]).put(header).putInt(crc).array();
        return ByteBuffer.wrap(new byte[17 + size]).put(header).put(serializedMsg).array();
    }

}
