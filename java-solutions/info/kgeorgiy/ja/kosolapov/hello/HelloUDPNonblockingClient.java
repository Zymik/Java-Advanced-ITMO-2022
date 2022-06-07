package info.kgeorgiy.ja.kosolapov.hello;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;

public class HelloUDPNonblockingClient extends AbstractHelloUDPClient {

    private final static int TIME_OUT = 100;

    /**
     * Run UDPClient where args is arguments
     *
     * @param args {@code prefix = args[0], port = args[1], prefix = args[2], threads = args[3], request = args[4]}.
     */
    public static void main(final String[] args) {
        AbstractHelloUDPClient.main(new HelloUDPNonblockingClient(), args);
    }

    private static class Context {

        private final int requests;
        private int number;
        private final int pid;
        private final String prefix;
        private final ByteBuffer response;
        private final ByteBuffer request;
        private String message;

        private Context(int pid, int requests, int bufferSize, String prefix) {
            this.pid = pid;
            this.requests = requests;
            this.prefix = prefix;
            response = ByteBuffer.allocate(bufferSize);
            request = ByteBuffer.allocate(bufferSize);
            calc();
        }

        private ByteBuffer getRequest() {
            request.rewind();
            return request;
        }

        private void calc() {
            message = createMessage();
            request.clear();
            request.put(message.getBytes(StandardCharsets.UTF_8));
            request.flip();
        }

        private ByteBuffer emptyResponse() {
            return response.clear();
        }

        private String responseString() {
            try {
                response.flip();
                return UDPUtil.decode(response);
            } catch (CharacterCodingException e) {
                return null;
            }
        }

        private String getMessage() {
            return message;
        }

        private String createMessage() {
            return AbstractHelloUDPClient.createMessage(prefix, number, pid);
        }

        private boolean finish() {
            number++;
            calc();
            return number == requests;
        }
    }

    protected void runClient(
            final SocketAddress socketAddress,
            final String prefix,
            final int threads,
            final int requests
    ) throws IOException {
        Selector selector = Selector.open();
        for (int i = 0; i < threads; i++) {
            DatagramChannel datagramChannel = DatagramChannel.open();
            datagramChannel.configureBlocking(false);
            datagramChannel.connect(socketAddress);
            int bufferSize = datagramChannel.socket().getSendBufferSize();
            datagramChannel.register(selector, SelectionKey.OP_WRITE, new Context(i, requests, bufferSize, prefix));
        }
        while (!Thread.interrupted() && hasKeys(selector)) {
            if (selector.select(this::selectKey, TIME_OUT) == 0) {
                keysInterestOrOpWrite(selector);
            }
        }
    }

    private void selectKey(SelectionKey key) {
        if (key.isValid()) {
            if (key.isReadable()) {
                read(key);
            } else if (key.isWritable()) {
                write(key);
            }

        }
    }

    private boolean hasKeys(Selector selector) {
        return !selector.keys().isEmpty();
    }

    private void keysInterestOrOpWrite(Selector selector) {
        selector.keys()
                .forEach(x -> x.interestOpsOr(SelectionKey.OP_WRITE));
    }

    private void read(SelectionKey key) {
        Context context = (Context) key.attachment();
        DatagramChannel channel = (DatagramChannel) key.channel();
        try {
            channel.receive(context.emptyResponse());
            String response = context.responseString();
            if (response != null && response.contains(context.getMessage())) {
                System.out.println(response);
                if (context.finish()) {
                    channel.close();
                    return;
                }
            }
        } catch (IOException e) {
            UDPUtil.logIO(e);
        }
        key.interestOps(SelectionKey.OP_WRITE);
    }

    private void write(SelectionKey key) {
        Context context = (Context) key.attachment();
        DatagramChannel channel = (DatagramChannel) key.channel();
        try {
            channel.write(context.getRequest());
        } catch (IOException e) {
            UDPUtil.logIO(e);
        }
        key.interestOps(SelectionKey.OP_READ);

    }
}
