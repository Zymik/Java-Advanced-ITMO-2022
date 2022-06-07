package info.kgeorgiy.ja.kosolapov.concurrent;

import java.util.Collection;

public class ThreadFinisher {

    /**
     * Interrupt and join all threads from {@code threads} until all threads are not complete .
     *
     * @param threads threads to interrupt and join.
     */
    public static void finishThreads(Collection<? extends Thread> threads) {
        threads.forEach(Thread::interrupt);
        for (final var thread : threads) {
            while (true) {
                try {
                    thread.join();
                    break;
                } catch (InterruptedException ignored) {
                }
            }
        }
    }
}
