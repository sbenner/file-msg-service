package server;

import commons.Utils;
import commons.enums.MessageType;
import commons.types.DataChunk;
import commons.types.FileMessage;
import commons.types.Message;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.String.format;

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


    private void parseBufferForHeader(byte[] buff, Set<DataChunk> dataList, Map<String, Message> messageMap) {

        DataChunk dataChunkLocal = new DataChunk();
        long seqNum;
        int serializedMsgSize;
        int type;

        //int startHeaderOffset = sniffStartHeaderOffset(buff);


//        if (buff.length <= 17 && dataList.size() > 0) {
//
//            dataChunkLocal = dataList.iterator().next();
//
//           logger.info("REST IS SMALLL");
//                ByteBuffer
//                        data = ByteBuffer.wrap(new byte[buff.length]).put(buff, 0, buff.length);
//
//                data = ByteBuffer.wrap(new byte[dataChunkLocal.getTotal() + buff.length])
//                        .put(dataChunkLocal.getRawData()).put(data.array()); //we write 5 bytes of header
//
//                dataChunkLocal.setRawData(data.array());
//
//                dataChunkLocal.setSeqNum(seqNum);
//
//                if (dataChunkLocal.getMessageType().equals(MessageType.FILE) && dataChunkLocal.getTotal() == dataChunkLocal.getFullMessageSize()) { //we build message if it's full
//                    concatMessage(messageMap, dataChunkLocal);
//
//                }
//
//            printMessages();
//            return;
//
//        }

        byte[] header = new byte[17];
        new ByteArrayInputStream(buff).read(header, 0, 17);
        seqNum = ByteBuffer.wrap(header, 0, 8).getLong();
        serializedMsgSize = ByteBuffer.wrap(header, 8, 4).getInt();
        type = ByteBuffer.wrap(header, 12, 1).get() & 0xff;
        int crc = ByteBuffer.wrap(header, 13, 4).getInt();


        if (Utils.checkCrc(header, crc)) { //see if header is ok with crc

            logger.info("seq num "+seqNum);
            MessageType c = MessageType.getValue(type);

            byte[] msgData = ByteBuffer.wrap(new byte[buff.length - 17]).put(buff, 17, buff.length - 17).array();
            dataChunkLocal.setMessageType(c);
            dataChunkLocal.setFullMessageSize(serializedMsgSize);
            dataChunkLocal.setRawData(msgData);
            dataChunkLocal.setCrc(crc);
            dataChunkLocal.setSeqNum(seqNum);

            if (dataChunkLocal.getMessageType().equals(MessageType.FILE)
                    && dataChunkLocal.getTotal() == dataChunkLocal.getFullMessageSize()) { //we build message if it's full
                concatMessage(messageMap, dataChunkLocal);
            }

        } else {
//
            DataChunk dataChunk = null;
            if (dataList.size() > 0) {
                dataChunk = dataList.iterator().next();

            }
            //we attaching the rest of the buffers if needed and transform into correct messages
            if (dataChunk != null && dataChunk.getTotal() < dataChunk.getFullMessageSize()) {

                logger.info(format("dataChunk.total %s msg size: %s ",dataChunk.getTotal(),dataChunk.getFullMessageSize()));

                int alloc = 0;
                if ((dataChunk.getFullMessageSize() - dataChunk.getTotal()) > buff.length) {
                    alloc = buff.length;
                } else if (dataChunk.getFullMessageSize() > dataChunk.getTotal()) {
                    alloc = dataChunk.getFullMessageSize() - dataChunk.getTotal();
                }

                ByteBuffer
                        data = ByteBuffer.wrap(new byte[alloc]).put(buff, 0, alloc);
                dataChunkLocal = dataChunk;
                data = ByteBuffer.wrap(
                        new byte[dataChunk.getTotal() + alloc]).put(dataChunk.getRawData()).put(data.array()); //we write 5 bytes of header

                dataChunkLocal.setRawData(data.array());

                try {
                    if (dataChunk.getMessageType().equals(MessageType.FILE)
                            && dataChunkLocal.getTotal() == dataChunkLocal.getFullMessageSize()) { //we build message if it's full
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

                        }
                    }


                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }



        }

        if (dataChunkLocal != null && dataChunkLocal.getRawData() != null) {
            dataList.add(dataChunkLocal);
            logger.info("dataList size: "+dataList.size());
            logger.info(dataChunkLocal.toString());
        }

        printMessages();

    }

    private void printMessages(){

        Iterator iterator = messageMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String,Message> sm = (Map.Entry<String,Message> )iterator.next();
            Message m = sm.getValue();

            if (MessageType.getValue(m.getType()).equals(MessageType.FILE)) {
                FileMessage fileMessage = (FileMessage) m;
                if (fileMessage.getMessageSize() == fileMessage.getFileSize()) {
                    try {
                        Files.write(Paths.get(fileMessage.getFileName()),fileMessage.getPayload(), StandardOpenOption.CREATE);
                        iterator.remove();
                    } catch (IOException e) {
                       logger.error(e.getMessage(),e);
                    }
                    if(fileMessage.getFileType().contains("text"))
                    logger.info(new String(fileMessage.getPayload()));
                }
            }

        }
    }

    private void concatMessage(Map<String, Message> messageMap, DataChunk dataChunkLocal) {

        if (dataChunkLocal.getMessageType().equals(MessageType.FILE)) {
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
