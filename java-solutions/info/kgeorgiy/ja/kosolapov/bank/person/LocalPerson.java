package info.kgeorgiy.ja.kosolapov.bank.person;


import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentMap;

public class LocalPerson extends AbstractPerson {
    private final ConcurrentMap<String, Integer> accounts;

    /**
     * Creates LocalPerson with {@code passport, firstName, secondName}.
     * @param accounts snapshot of Accounts at the moment of creation
     */
    public LocalPerson(String passport, String firstName, String secondName,
                       ConcurrentMap<String, Integer> accounts) {
        super(passport, firstName, secondName);
        this.accounts = accounts;
    }


    @Override
    public void createAccount(String subId) {
        accounts.putIfAbsent(accountFullName(subId), 0);
    }

    @Override
    public boolean checkAccount(String subId) {
        return accounts.containsKey(accountFullName(subId));
    }

    @Override
    public int getAccountAmount(String subId) {
        return accounts.get(accountFullName(subId));
    }

    @Override
    public void setAccountAmount(String subId, int value) {
        accounts.put(accountFullName(subId), value);
    }

    @Override
    public void addAccountAmount(String subId, int value) throws RemoteException {
        accounts.computeIfPresent(subId, (i, val) -> val + value);
    }
}
