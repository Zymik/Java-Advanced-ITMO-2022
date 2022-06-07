package info.kgeorgiy.ja.kosolapov.hello;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class HelloUDPServer extends AbstractHelloUDPServer {

    private DatagramSocket datagramSocket;
    private int bufferSize;


    /**
     * Start server on {@code port = Integer.parseInt(args[0])} and {@code threads = Integer.parseInt(args[1])}.
     *
     * @param args - port and threads. Must be parsed to int and follow the contract of port and threads of {@link #start}
     */
    public static void main(final String[] args) {
        AbstractHelloUDPServer.run(new HelloUDPServer(), args);
    }

    private void handler() {
        final DatagramPacket datagram = getDatagramPacket();
        while (!Thread.interrupted()) {
            try {
                datagramSocket.receive(datagram);
            } catch (final IOException e) {
              //  System.err.println("IOException while receiving: " + e.getMessage());
            }
            try {
                setDatagram(datagram);
                datagramSocket.send(datagram);
            } catch (final IOException e) {
                //System.err.println("IOException while sending: " + e.getMessage());
            }
            resetDatagram(datagram);
        }
    }

    private void setDatagram(final DatagramPacket datagram) throws SocketException {
        datagram.setData(datagram.getData(), 0, datagram.getLength() + HELLO.length);
    }


    private void resetDatagram(final DatagramPacket datagram) {
        datagram.setData(datagram.getData(), HELLO.length, bufferSize - HELLO.length);
    }

    private DatagramPacket getDatagramPacket() {
        final byte[] bytes = Arrays.copyOf(HELLO, bufferSize);
        return new DatagramPacket(bytes, HELLO.length, bufferSize - HELLO.length);
    }


    /**
     * Starts a new Hello server.
     * This method should return immediately.
     * if {@code threads == 1} will make one thread for receiving and handling packets (one will do both actions).
     * if {@code threads > 1} will make {@code Math.min(threads / 3, 1)} for receiving data and remaining threads for handling.
     *
     * @param port    server port.
     * @param threads number of working threads.
     * @throws IllegalArgumentException if {@code threads <= 0} or {@code port < 0}
     */

    @Override
    public void start(final int port, final int threads) {
        try {
            datagramSocket = new DatagramSocket(port);
            bufferSize = datagramSocket.getReceiveBufferSize();
            final ExecutorService handlers = Executors.newFixedThreadPool(threads);
            setCloseables(datagramSocket);
            setExecutorServices(handlers);
            for (int i = 0; i < threads; i++) {
                handlers.submit(this::handler);
            }
        } catch (final SocketException e) {
           // System.err.println("HelloUDPServer was not started cause of SocketException: " + e.getMessage());
        }
    }

}
