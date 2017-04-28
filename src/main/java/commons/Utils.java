package commons;

/**
 * Created by sbenner on 28/04/2017.
 */
public class Utils {

    public static int getCRC(byte[] header){
        int crc = 0;
        for (int i = 0; i < 13; i++) {
            crc += header[i] & 0xFF;
        }
        return crc;
    }


    public static boolean checkCrc(byte[] header, int crc) {
        return getCRC(header) == crc;
    }

}
