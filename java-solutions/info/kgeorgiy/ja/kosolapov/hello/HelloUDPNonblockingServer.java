package info.kgeorgiy.ja.kosolapov.hello;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloUDPNonblockingServer extends AbstractHelloUDPServer {

    private DatagramChannel datagramChannel;
    private SelectionKey key;
    private Selector selector;
    private ExecutorService handlers;
    private int bufferSize;
    private static final int QUEUE_SIZE = 10000;
    private final BlockingQueue<Request> requests = new ArrayBlockingQueue<>(QUEUE_SIZE);

    public static void main(final String[] args) {
        AbstractHelloUDPServer.run(new HelloUDPNonblockingServer(), args);
    }

    private void selectKey(final SelectionKey key) {
        if (key.isValid()) {
            if (key.isReadable() && hasCapacity()) {
                read();
            } else if (!hasCapacity()) {
                key.interestOps(SelectionKey.OP_WRITE);
            }
            if (key.isWritable()) {
                write(key);
            }
        }
    }

    private boolean hasCapacity() {
        return requests.remainingCapacity() > 0;
    }

    private record Request(SocketAddress socketAddress, ByteBuffer buffer) {
    }

    private void start() {
        while (!Thread.interrupted() && !selector.keys().isEmpty()) {
            try {
                selector.select(this::selectKey);
            } catch (final IOException e) {
                System.err.println("IOException while selecting: " + e.getMessage());
            }
            if (!requests.isEmpty()) {
                key.interestOpsOr(SelectionKey.OP_WRITE);
            }
        }
    }

    private void read() {
        try {
            final ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
            buffer.position(HELLO.length);
            final SocketAddress socketAddress = datagramChannel.receive(buffer);
            if (socketAddress != null) {
                handlers.submit(() -> handle(buffer, socketAddress));
            }
        } catch (final IOException e) {
            System.err.println("IOException while reading: " + e.getMessage());
        }
    }

    private void handle(final ByteBuffer buffer, final SocketAddress socketAddress) {
        putHello(buffer);
        requests.add(new Request(socketAddress, buffer));
        selector.wakeup();
    }

    private void putHello(final ByteBuffer buffer) {
        buffer.flip();
        buffer.position(0);
        buffer.put(HELLO);
        buffer.position(0);
    }

    private void write(final SelectionKey key) {
        if (requests.isEmpty()) {
            key.interestOps(SelectionKey.OP_READ);
            return;
        }
        final var client = requests.poll();
        try {
            datagramChannel.send(client.buffer, client.socketAddress);
        } catch (final IOException e) {
            System.err.println("IOException while sending: " + e.getMessage());
        } finally {
            key.interestOpsOr(SelectionKey.OP_READ);
        }
    }


    @Override
    public void start(final int port, final int threads) {
        try {
            datagramChannel = DatagramChannel.open();
            datagramChannel.configureBlocking(false);
            datagramChannel.bind(new InetSocketAddress(port));
            bufferSize = datagramChannel.socket().getReceiveBufferSize();
            selector = Selector.open();
            key = datagramChannel.register(selector, SelectionKey.OP_READ);
            final ExecutorService receiver = Executors.newSingleThreadExecutor();
            handlers = Executors.newFixedThreadPool(threads);
            receiver.submit((Runnable) this::start);
            setCloseables(selector, datagramChannel);
            setExecutorServices(handlers, receiver);
        } catch (final IOException e) {
            System.err.println("IOException while starting: " + e.getMessage());
        }
    }

}
