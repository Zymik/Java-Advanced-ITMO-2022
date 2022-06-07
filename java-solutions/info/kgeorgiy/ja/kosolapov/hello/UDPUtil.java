package info.kgeorgiy.ja.kosolapov.hello;

import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;

public class UDPUtil {


    private static final CharsetDecoder DECODER = StandardCharsets.UTF_8.newDecoder();

    /**
     * Convert data from {@code datagramPocket.getData()} to {@link String}
     *
     * @param datagramPacket {@link DatagramPacket} to convert
     * @return converted {@code datagramPacket}
     */
    public static String dataGramMessageUTF8(DatagramPacket datagramPacket) throws CharacterCodingException {
        var buffer = ByteBuffer.wrap(datagramPacket.getData(), datagramPacket.getOffset(), datagramPacket.getLength());
        return DECODER.decode(buffer).toString();
    }

    public static void logIO(IOException e) {
        //System.err.println("IOException: " + e.getMessage());
    }

    public static String decode(ByteBuffer buffer) throws CharacterCodingException {
        return DECODER.decode(buffer).toString();
    }

    /**
     * Create empty {@link DatagramPacket} with {@code size} elements
     *
     * @param size size of buffer
     * @return empty {@link DatagramPacket}
     */
    public static DatagramPacket emptyDataGram(int size) {
        return new DatagramPacket(new byte[size], size);
    }

}


