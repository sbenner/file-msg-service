package server;

import commons.enums.Command;
import commons.types.FileMessage;
import commons.types.Message;
import commons.types.DataChunk;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.ReadWriteLock;

public class MessageServer implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(MessageServer.class);
    private final int port;
    private final ByteBuffer welcomeBuf = ByteBuffer.wrap("Welcome to NioChat!\n".getBytes());
    Set<DataChunk> dataList = new HashSet<DataChunk>();
    Map<String, Message> messageMap = new ConcurrentHashMap();
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    

    public MessageServer(int port) throws IOException {
        this.port = port;
        this.serverSocketChannel = ServerSocketChannel.open();
        this.serverSocketChannel.socket().bind(new InetSocketAddress(port));
        this.serverSocketChannel.configureBlocking(false);
        this.selector = Selector.open();
        this.serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    public static void main(String[] args) throws IOException {
        MessageServer server = new MessageServer(10523);
        (new Thread(server)).start();
    }

    public void run() {
        try {
            logger.info("Server starting on port " + this.port);

            Iterator<SelectionKey> iter;
            SelectionKey key;
            while (this.serverSocketChannel.isOpen()) {
                selector.select();
                iter = this.selector.selectedKeys().iterator();
                while (iter.hasNext()) {
                    key = iter.next();
                    iter.remove();

                    if (key.isAcceptable()) this.handleAccept(key);
                    if (key.isReadable()) this.handleRead(key);
                }
            }
        } catch (IOException e) {
            logger.info("IOException, server of port " + this.port + " terminating. Stack trace:");
            e.printStackTrace();
        }
    }

    private void handleAccept(SelectionKey key) throws IOException {
        SocketChannel sc = ((ServerSocketChannel) key.channel()).accept();
        String address = (new StringBuilder(sc.socket().getInetAddress().toString())).append(":").append(sc.socket().getPort()).toString();
        sc.configureBlocking(false);
        sc.register(selector, SelectionKey.OP_READ, address);
        sc.write(welcomeBuf);
        welcomeBuf.rewind();
        logger.info("accepted connection from: " + address);
    }

    private boolean checkCrc(byte[] header, int crc) {
        int total = 0;
        for (int i = 0; i < header.length; i++) {
            total += header[i] & 0xFF;
        }
        return crc == total;
    }

    private void parseBufferForHeader(byte[] buff, Set<DataChunk> dataList, Map<String, Message> messageMap) {

        DataChunk dataChunkLocal = new DataChunk();

        ByteBuffer seqNumBuffer = ByteBuffer.allocate(8);
        ByteBuffer sizeBuffer = ByteBuffer.allocate(4);


        ByteBuffer typeBuffer = ByteBuffer.allocate(1);
        ByteBuffer crcBuffer = ByteBuffer.allocate(4);


        long seqNum = -1;
        int size = -1;
        int type = -1;

        //int startHeaderOffset = sniffStartHeaderOffset(buff);

        if (buff.length <= 17 && dataList.size() > 0) {

            dataChunkLocal = dataList.iterator().next();
            if (buff.length == (dataChunkLocal.getFullMessageSize() - dataChunkLocal.getTotal())) {

                ByteBuffer
                        data = ByteBuffer.wrap(new byte[buff.length]).put(buff, 0, buff.length);

                data = ByteBuffer.wrap(new byte[dataChunkLocal.getTotal() + buff.length])
                        .put(dataChunkLocal.getRawData()).put(data.array()); //we write 5 bytes of header

                dataChunkLocal.setRawData(data.array());

                dataChunkLocal.setSeqNum(seqNum);

                if (dataChunkLocal.getCommand().equals(Command.FILE) && dataChunkLocal.getTotal() == dataChunkLocal.getFullMessageSize()) { //we build message if it's full
                    concatMessage(messageMap, dataChunkLocal);

                }
            }
            logger.info("message map size: " + messageMap.size());
            return;
        }


        for (int i = 0; i < buff.length; i++) {
            byte b = buff[i];

            if (seqNumBuffer.position() < 8) {
                seqNumBuffer.put(b);
            }
            if (seqNumBuffer.position() == 8 && seqNum == -1) {
                seqNumBuffer.flip();
                long val = seqNumBuffer.getLong();
                seqNum = val < 0 ? 0 : val;
                continue;
            }

            if (seqNumBuffer.position() == 8 && sizeBuffer.position() < 4) {
                sizeBuffer.put(b);
            }
            if (sizeBuffer.position() == 4 && size == -1) {
                sizeBuffer.flip();
                int val = sizeBuffer.getInt();
                size = val < 0 ? 0 : val;
                continue;
            }

            if (seqNumBuffer.position() == 8 && sizeBuffer.position() == 4 && typeBuffer.position() == 0 && type == -1) {
                typeBuffer.put(b);
                typeBuffer.flip();
                int val = Byte.valueOf(b).intValue() & 0xFF;
                type = val < 0 ? 0 : val;
                continue;
            }
            if (seqNumBuffer.position() == 8
                    && sizeBuffer.position() == 4
                    && typeBuffer.limit() == 1
                    && crcBuffer.position() < 4) {     //we read a presumed header even in the middle of the buffer

                crcBuffer.put(b);

                if (crcBuffer.position() == 4) {   //buffer filled w 4 bytes to get crc

                    crcBuffer.flip(); //we want to read it from start

                    int crc = crcBuffer.getInt(); // we read the number written with 4 bytes

                    ByteBuffer header = ByteBuffer.wrap(new byte[13]).put(seqNumBuffer.array()).put(sizeBuffer.array()).put(typeBuffer.get(0)); //we write 5 bytes of header
                    header.flip();
                    seqNumBuffer.clear();
                    sizeBuffer.clear();
                    typeBuffer.clear();

                    if (checkCrc(header.array(), crc)) { //see if header is ok with crc

                        Command c = Command.getValue(type);

                        crcBuffer.clear();
                        header.clear();

                        //if(c!=null&&size>0&&size<=1024){   //if our message is less than 1024 we return the message data

                        // we just return the raw bytes of message to be de-serialized

                        byte[] msgData = ByteBuffer.wrap(new byte[buff.length - 17]).put(buff, 17, buff.length - 17).array();
                        dataChunkLocal.setCommand(c);
                        dataChunkLocal.setFullMessageSize(size);
                        dataChunkLocal.setRawData(msgData);
                        dataChunkLocal.setCrc(crc);
                        dataChunkLocal.setSeqNum(seqNum);

                        if (dataChunkLocal.getCommand().equals(Command.FILE) && dataChunkLocal.getTotal() == dataChunkLocal.getFullMessageSize()) { //we build message if it's full
                            concatMessage(messageMap, dataChunkLocal);
                        }
                        break;

                    } else {

                        DataChunk dataChunk = null;
                        if (dataList.size() > 0) {
                            dataChunk = dataList.iterator().next();
                            dataList.clear();
                        }
                        //we attaching the rest of the buffers if needed and transform into correct messages
                        if (dataChunk != null && dataChunk.getTotal() < dataChunk.getFullMessageSize()) {
                            int alloc = 0;
                            if ((dataChunk.getFullMessageSize() - dataChunk.getTotal()) > buff.length) {
                                alloc = buff.length;
                            } else if (dataChunk.getFullMessageSize() > dataChunk.getTotal()) {
                                alloc = dataChunk.getFullMessageSize() - dataChunk.getTotal();
                            }

                            ByteBuffer
                                    data = ByteBuffer.wrap(new byte[alloc]).put(buff, 0, alloc);
                            dataChunkLocal = dataChunk;
                            data = ByteBuffer.wrap(new byte[dataChunk.getTotal() + alloc]).put(dataChunk.getRawData()).put(data.array()); //we write 5 bytes of header

                            dataChunkLocal.setRawData(data.array());

                            try {
                                if (dataChunk.getCommand().equals(Command.FILE) && dataChunkLocal.getTotal() == dataChunkLocal.getFullMessageSize()) { //we build message if it's full
                                    concatMessage(messageMap, dataChunkLocal);
                                    dataList.clear();
                                    dataChunkLocal = null;
                                    if (buff.length > alloc) {
                                        //we need to create another head
                                        int startMessageSize = buff.length - alloc;
                                        ByteBuffer startNewMessage =
                                                ByteBuffer.wrap(new byte[startMessageSize])
                                                        .put(buff, alloc, startMessageSize);

                                        parseBufferForHeader(startNewMessage.array(), dataList, messageMap);
                                        break;
                                    }

                                }

                                logger.info("not found i 1: " + i);

//                                if (dataChunk.getTotal() == dataChunk.getFullMessageSize()) {
//
//                                    logger.info("not found i 3: " + i);
//                                    break;
//                                }

                                List<Message> messages = new ArrayList<Message>(messageMap.values());

                                for(Message m : messages){
                                     FileMessage fm = (FileMessage) m;
                                     if(fm.getMessageSize()==fm.getFileSize()){
                                         logger.info(new String(fm.getPayload()));
                                     }

                                }

                                break;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            logger.info("not found i 2: " + i);
                        }
                        if (dataChunk.getTotal() == dataChunk.getFullMessageSize()) {

                            logger.info("not found i 3: " + i);
                            break;
                        }
                    }                                             
                }
            }

            logger.info("I!!! " + i);
        }
        if (dataChunkLocal != null && dataChunkLocal.getRawData() != null) {
            dataList.add(dataChunkLocal);
            logger.info(dataChunkLocal.toString());
        }


        seqNumBuffer.clear();
        sizeBuffer.clear();
        typeBuffer.clear();
        crcBuffer.clear();


    }

    private void concatMessage(Map<String, Message> messageMap, DataChunk dataChunkLocal) {

        if(dataChunkLocal.getCommand().equals(Command.FILE)) {
            FileMessage m = (FileMessage) buildMessage(dataChunkLocal.getRawData());
            if (m != null) {
                FileMessage existingMessagePart = (FileMessage) messageMap.get(m.getMd5());
                if (existingMessagePart != null) {
                    byte[] mdata = ByteBuffer.
                            wrap(new byte[m.getPayload().length + existingMessagePart.getPayload().length])
                            .put(existingMessagePart.getPayload()).put(m.getPayload()).array();
                    m.setPayload(mdata);
                }

                messageMap.put(m.getMd5(), m);
            }
        }

    }

    private void readHeader(SocketChannel ch) throws IOException {
        int read;

        try {

            ByteBuffer body = ByteBuffer.allocate(512);

            while ((read = ch.read(body)) > 0) {
                logger.info("read bytes : " + read);
                body.flip();
                byte chunk[] = new byte[read];
                body.get(chunk);
                parseBufferForHeader(chunk, dataList, messageMap);

            }
            // logger.info(messageMap);

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

    private Message buildMessage(byte[] data) {
        Message msg = null;
        try {
                msg = SerializationUtils.deserialize(data);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        return msg;
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel ch = (SocketChannel) key.channel();
        readHeader(ch);

    }


}
