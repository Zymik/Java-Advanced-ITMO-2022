package info.kgeorgiy.ja.kosolapov.bank.person;

import info.kgeorgiy.ja.kosolapov.bank.account.Account;
import info.kgeorgiy.ja.kosolapov.bank.bank.Bank;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;


public class RemotePerson extends AbstractPerson {

    /**
     * Bank where Person is registered
     */
    private final Bank bank;

    /**
     * Creates RemotePerson associated with {@code bank} on {@code port}
     */
    public static RemotePerson createRemotePerson(String passport, String firstName,
                                                  String secondName, Bank bank, int port) throws RemoteException {
        var answer = new RemotePerson(passport, firstName, secondName, bank);
        answer.export(port);
        return answer;
    }

    private RemotePerson(String passport, String firstName, String secondName, Bank bank) {
        super(passport, firstName, secondName);
        this.bank = bank;
    }

    private void export(int port) throws RemoteException {
        UnicastRemoteObject.exportObject(this, port);
    }


    @Override
    public void createAccount(String subId) throws RemoteException {
        bank.createAccount(getPassport(), subId);
    }

    private Account checkAccountAndThrowException(String id) throws RemoteException {
        var account = getAccount(id);
        if (account == null) {
            throw new IllegalArgumentException("Account: " + accountFullName(id) + " don't exist in bank");
        }
        return account;
    }

    private Account getAccount(String id) throws RemoteException {
        return bank.getAccount(getPassport(), id);
    }

    @Override
    public boolean checkAccount(String subId) throws RemoteException {
        return getAccount(subId) != null;
    }

    @Override
    public int getAccountAmount(String subId) throws RemoteException {
        return checkAccountAndThrowException(subId).getAmount();
    }

    @Override
    public void setAccountAmount(String subId, int value) throws RemoteException {
        checkAccountAndThrowException(subId).setAmount(value);
    }

    @Override
    public void addAccountAmount(String subId, int value) throws RemoteException {
        checkAccountAndThrowException(subId).addAmount(value);
    }
}
