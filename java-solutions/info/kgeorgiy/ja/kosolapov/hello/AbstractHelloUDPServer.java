package info.kgeorgiy.ja.kosolapov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;

public abstract class AbstractHelloUDPServer implements HelloServer {
    private List<Closeable> closeables;
    private List<ExecutorService> executorServices;
    public static final byte[] HELLO = "Hello, ".getBytes(StandardCharsets.UTF_8);

    private static boolean validate(final String[] args) {
        return args != null && args.length == 2;
    }

    protected static void run(final HelloServer helloServer, final String[] args) {
        if (!validate(args)) {
            System.err.println("Size of args must be 2");
            return;
        }
        final int[] intArgs = new int[2];
        for (int i = 0; i < 2; i++) {
            try {
                intArgs[i] = Integer.parseInt(args[i]);
            } catch (final NumberFormatException e) {
                System.err.println("Invalid number format of " + i + " arg: " + args[i]);
                break;
            }
        }
        if (!(intArgs[0] >= 0 && intArgs[1] > 0)) {
            System.err.println("First argument must greater or equal then zero, first must be greater than zero");
        }
        helloServer.start(intArgs[0], intArgs[1]);
        while (Thread.currentThread().isInterrupted()) ;
        helloServer.close();
    }

    protected void setCloseables(final Closeable... closeables) {
        if (closeables != null) {
            this.closeables = List.of(closeables);
        }
    }

    protected void setExecutorServices(final ExecutorService... executorServices) {
        if (executorServices != null) {
            this.executorServices = List.of(executorServices);
        }
    }

    @Override
    public void close() {
        for (final var i : closeables) {
            if (i != null) {
                try {
                    i.close();
                } catch (final IOException e) {
                   // System.err.println("Exception while closing: " + e.getMessage());
                }
            }
        }
        for (final var i : executorServices) {
            i.shutdownNow();
        }
    }
}
