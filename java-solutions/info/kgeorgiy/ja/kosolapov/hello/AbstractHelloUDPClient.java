package info.kgeorgiy.ja.kosolapov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public abstract class AbstractHelloUDPClient implements HelloClient {
    /**
     * Run UDPClient where args is arguments
     *
     * @param args {@code prefix = args[0], port = args[1], prefix = args[2], threads = args[3], request = args[4]}.
     */
    protected static void main(AbstractHelloUDPClient abstractHelloUDPClient, String[] args) {

        if (args == null || args.length != 5) {
            System.err.println("Invalid args size must be 5");
            return;
        }
        final InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getByName(args[0]);
        } catch (final UnknownHostException e) {
            System.err.println(args[0] + " is invalid host name");
            return;
        }

        // :NOTE: CP
        final int[] argsPos = new int[]{1, 3, 4};
        final int[] uppers = new int[]{0, 1, 0};
        final int[] values = new int[3];
        final List<String> names = List.of("port", "threads", "request");
        for (int i = 0; i < 3; i++) {
            if (notValid(args[argsPos[i]], names.get(i), uppers[i])) {
                return;
            }
            values[i] = Integer.parseInt(args[argsPos[i]]);
        }
        final String prefix = args[2];
        try {
            abstractHelloUDPClient.runClient(new InetSocketAddress(inetAddress, values[0]),
                    prefix, values[1], values[2]);
        } catch (final ExecutionException | InterruptedException | IOException e) {
            System.err.println(e.getMessage());
        }

    }

    private static boolean notValid(final String value, final String name, final int upper) {
        try {
            final int threads = Integer.parseInt(value);
            if (threads < upper) {
                System.err.println(name + " must be greater than " + upper);
                return true;
            }
        } catch (final NumberFormatException e) {
            System.err.println(name + " must be correct int");
            return true;
        }
        return false;
    }

    protected abstract void runClient(
            final SocketAddress socketAddress,
            final String prefix,
            final int threads,
            final int requests
    ) throws InterruptedException, ExecutionException, IOException;

    @Override
    public void run(final String host, final int port, final String prefix, final int threads, final int requests) {
        try {
            runClient(new InetSocketAddress(InetAddress.getByName(host), port), prefix, threads, requests);
        } catch (final UnknownHostException e) {
            throw new IllegalArgumentException("Unknown host: " + e.getMessage(), e);
        } catch (final ExecutionException e) {
            if (e.getCause() instanceof RuntimeException cause) {
                throw cause;
            }
            System.err.println("Exception while tasks was executing: " + e.getMessage());
        } catch (final InterruptedException e) {
            System.err.println("Thread was interrupted: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("IOException while execution: " + e.getMessage());
        }
    }

    protected static String createMessage(final String prefix, final int number, final long pid) {
        return prefix +
                pid +
                "_" +
                number;
    }
}
