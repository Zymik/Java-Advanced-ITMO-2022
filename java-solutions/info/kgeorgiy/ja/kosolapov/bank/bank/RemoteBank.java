package info.kgeorgiy.ja.kosolapov.bank.bank;

import info.kgeorgiy.ja.kosolapov.bank.account.Account;
import info.kgeorgiy.ja.kosolapov.bank.account.RemoteAccount;
import info.kgeorgiy.ja.kosolapov.bank.person.AbstractPerson;
import info.kgeorgiy.ja.kosolapov.bank.person.LocalPerson;
import info.kgeorgiy.ja.kosolapov.bank.person.Person;
import info.kgeorgiy.ja.kosolapov.bank.person.RemotePerson;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RemoteBank implements Bank {
    private final int port;
    private final ConcurrentMap<String, Set<RemoteAccount>> passportToAccount = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, RemoteAccount> accounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, RemotePerson> persons = new ConcurrentHashMap<>();

    public RemoteBank(final int port) {
        this.port = port;
    }

    @Override
    public Account createAccount(final String passport, final String id) throws RemoteException {
        if (persons.containsKey(passport)) {
            var set = passportToAccount.get(passport);
            var fullId = AbstractPerson.accountIdFormat(passport, id);
            System.out.println("Creating account " + fullId);
            final RemoteAccount account = new RemoteAccount(fullId);
            if (accounts.putIfAbsent(fullId, account) == null) {
                UnicastRemoteObject.exportObject(account, port);
                set.add(account);
                return account;
            } else {
                return getAccount(fullId);
            }
        }
        return null;
    }

    @Override
    public Account getAccount(final String passport, final String id) throws RemoteException {
        var fullId = AbstractPerson.accountIdFormat(passport, id);
        System.out.println("Retrieving account " + fullId);
        return accounts.get(fullId);
    }

    @Override
    public void createPerson(String passport, String firstName, String secondName) throws RemoteException {
        RemotePerson person = RemotePerson.createRemotePerson(passport, firstName, secondName, this, port);
        System.out.println("Creating Person " + person);
        persons.putIfAbsent(passport, person);
        passportToAccount.putIfAbsent(passport, ConcurrentHashMap.newKeySet());

    }

    @Override
    public Person getRemotePerson(String passport) throws RemoteException {
        System.out.println("Retrieving RemotePerson " + passport);
        return persons.get(passport);
    }

    @Override
    public Person getLocalPerson(String passport) throws RemoteException {
        System.out.println("Retrieving LocalPerson " + passport);

        var person = persons.get(passport);
        if (person == null) {
            return null;
        }
        ConcurrentMap<String, Integer> accounts = new ConcurrentHashMap<>();
        for (var i : passportToAccount.get(passport)) {
            accounts.put(i.getId(), i.getAmount());
        }
        return new LocalPerson(passport, person.getFirstName(), person.getSecondName(), accounts);
    }


    private Account getAccount(final String id) {
        System.out.println("Retrieving account " + id);
        return accounts.get(id);
    }
}
