package info.kgeorgiy.ja.kosolapov.bank.person;

import info.kgeorgiy.ja.kosolapov.bank.account.Account;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Person extends Remote {

    /**
     * Get passport of person
     */
    String getPassport() throws RemoteException;

    /**
     * Get first name of person
     */
    String getFirstName() throws RemoteException;

    /**
     * Get second name of person
     */
    String getSecondName() throws RemoteException;

    /**
     * Create {@link Account} for this Person with following {@code subId}
     */
    void createAccount(String subId) throws RemoteException;

    /**
     * Check that exist {@link Account} with {@code subId} of this Person
     */
    boolean checkAccount(String subId) throws RemoteException;

    /**
     * Get {@link Account} amount with {@code subId} of this Person
     * @throws IllegalArgumentException if such is not exist
     */
    int getAccountAmount(String subId) throws RemoteException;

    /**
     * Set {@link Account} amount with {@code subId} for this Person to {@code value}
     * @throws IllegalArgumentException if such is not exist
     */
    void setAccountAmount(String subId, int value) throws RemoteException;

    /**
     * Add {@code value} to {@link Account} amount with {@code subId} of this Person
     * @throws IllegalArgumentException if such is not exist
     */
    void addAccountAmount(String subId, int value) throws RemoteException;
}
