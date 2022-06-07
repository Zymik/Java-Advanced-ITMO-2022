package info.kgeorgiy.ja.kosolapov.bank;

import info.kgeorgiy.ja.kosolapov.bank.bank.Bank;
import info.kgeorgiy.ja.kosolapov.bank.bank.RemoteBank;

import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.*;

public final class Server {
    private final static int DEFAULT_PORT = 8889;

    /**
     * Starts {@link Bank} on {args[0]} port
     */
    public static void main(final String... args) throws RemoteException {
        final int port = args == null || args.length == 0 ? DEFAULT_PORT : Integer.parseInt(args[0]) ;

        final Bank bank = new RemoteBank(port);
        var registry = LocateRegistry.getRegistry(port);
        try {
            UnicastRemoteObject.exportObject(bank, port);
            registry.bind("bank", bank);
            System.out.println("Server started");
        } catch (final RemoteException | AlreadyBoundException e) {
            System.out.println("Cannot export object: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}