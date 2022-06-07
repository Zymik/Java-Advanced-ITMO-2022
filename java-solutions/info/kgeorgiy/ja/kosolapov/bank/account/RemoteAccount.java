package info.kgeorgiy.ja.kosolapov.bank.account;


import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.concurrent.atomic.AtomicInteger;

public class RemoteAccount implements Account, Serializable {
    private final String id;
    private final AtomicInteger amount;

    /**
     * Creates account by id
     */
    public RemoteAccount(final String id) {
        this.id = id;
        amount = new AtomicInteger(0);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public int getAmount() {
        System.out.println("Getting amount of money for account " + id);
        return amount.get();
    }

    @Override
    public void setAmount(final int amount) {
        System.out.println("Setting amount of money for account " + id);
        this.amount.set(amount);
    }

    @Override
    public void addAmount(final int amount) throws RemoteException {
        System.out.println("Adding amount of money for account " + id);
        this.amount.addAndGet(amount);
    }


}
