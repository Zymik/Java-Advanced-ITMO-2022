package info.kgeorgiy.ja.kosolapov.hello;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

public class HelloUDPClient extends AbstractHelloUDPClient {
    private final static int TIME_OUT = 100;

    /**
     * Run UDPClient where args is arguments
     *
     * @param args {@code prefix = args[0], port = args[1], prefix = args[2], threads = args[3], request = args[4]}.
     */
    public static void main(final String[] args) {
        AbstractHelloUDPClient.main(new HelloUDPClient(), args);
    }

    protected void runClient(
            final SocketAddress socketAddress,
            final String prefix,
            final int threads,
            final int requests
    ) throws InterruptedException, ExecutionException {
        final List<Callable<Object>> list = IntStream.range(0, threads)
                .<Callable<Object>>mapToObj(pid -> () -> createCallable(prefix, pid, requests, socketAddress))
                .toList();
        final var executor = Executors.newFixedThreadPool(threads);
        try {
            final var result = executor.invokeAll(list);
            final var e = resolveExceptionFromCallables(result);
            if (e != null) {
                throw e;
            }
        } finally {
            executor.shutdown();
        }
    }


    private static ExecutionException resolveExceptionFromCallables(final List<? extends Future<?>> futures) throws InterruptedException {
        ExecutionException exception = null;
        for (final var future : futures) {
            try {
                future.get();
            } catch (final ExecutionException e) {
                if (exception != null) {
                    exception.addSuppressed(e);
                } else {
                    exception = e;
                }
            }
        }
        return exception;
    }



    private static Void createCallable(
            final String prefix, final int pid, final int requests,
            final SocketAddress socketAddress
    ) throws IOException {
        try (final var datagramSocket = new DatagramSocket()) {
            final int bufferSize = datagramSocket.getSendBufferSize();
            final var request = new DatagramPacket(new byte[0], 0, socketAddress);
            final var response = UDPUtil.emptyDataGram(bufferSize);
            datagramSocket.setSoTimeout(TIME_OUT);
            for (int i = 0; i < requests; i++) {
                final String message = createMessage(prefix, i, pid);
                final var bytes = message.getBytes(StandardCharsets.UTF_8);
                request.setData(bytes);
                request.setLength(bytes.length);
                receiveResponse(socketAddress, datagramSocket, response, request, message);
            }
        }
        return null;
    }

    private static void receiveResponse(final SocketAddress socketAddress, final DatagramSocket datagramSocket,
                                        final DatagramPacket response, final DatagramPacket request, final String message)
            throws IOException {
        while (true) {
            datagramSocket.send(request);
            try {
                datagramSocket.receive(response);
                if (validateResponseAddress(socketAddress, response)) {
                    final String responseAnswer = UDPUtil.dataGramMessageUTF8(response);
                    if (responseAnswer.contains(message)) {
                        System.out.println(message + " " + responseAnswer);
                        break;
                    }
                }
            } catch (final SocketTimeoutException ignored) {
            }
        }
    }

    private static boolean validateResponseAddress(final SocketAddress socketAddress, final DatagramPacket response) {
        return response.getSocketAddress().equals(socketAddress);
    }


}
