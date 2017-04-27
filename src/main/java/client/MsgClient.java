package client;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class MsgClient {


    private static File[] readDirectory(String path) {
        return new File(path).listFiles(new FileFilter() {
            
            public boolean accept(File pathname) {
                return pathname.getName().startsWith("log4j.");
            }
        });
    }

    public static void main(String argv[]) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(5);

        Socket clientSocket = new Socket("localhost", 10523);
        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));


        String modifiedSentence = inFromServer.readLine();
        System.out.println(modifiedSentence);

        long i = 1;
        File[] files = readDirectory("/Users/sergey/projects/msgserver");
        CountDownLatch countDownLatch = new CountDownLatch(files.length);
        Semaphore semaphore = new Semaphore(files.length);

        for (File file : files) {
            //     File file = new File("/Users/sergey/projects/niochat/log4j.properties");
            if (file.exists()) {
                semaphore.acquire();
                String md5 = DigestUtils.md5Hex(FileUtils.readFileToByteArray(file));

                executor.execute(new FileMessageSender(outToServer, file, md5, semaphore, i, countDownLatch));

            }

            i++;
        }
        countDownLatch.await();
        clientSocket.close();
        System.exit(0);
    }


}