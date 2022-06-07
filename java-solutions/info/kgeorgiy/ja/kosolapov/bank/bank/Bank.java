package info.kgeorgiy.ja.kosolapov.bank.bank;

import info.kgeorgiy.ja.kosolapov.bank.account.Account;
import info.kgeorgiy.ja.kosolapov.bank.person.Person;
import info.kgeorgiy.ja.kosolapov.bank.person.RemotePerson;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Bank extends Remote {

    /**
     * Creates a new account with specified identifier for {@link Person} with {@code passport} if it is not already exists.
     *
     * @param passport person passport
     * @param id account id
     * @return created or existing account.
     */
    Account createAccount(String passport, String id) throws RemoteException;

    /**
     * Returns account by identifier and {@link Person} owner passport
     *
     * @param passport owner passport
     * @param id account id
     * @return account with specified identifier or {@code null} if such account does not exists.
     */
    Account getAccount(String passport, String id) throws RemoteException;

    /**
     * Add new {@link Person} to bank by description.
     * If {@link #getRemotePerson(String)} return not null with {@code passport} do nothing.
     * @param passport {@link Person} passport
     * @param firstName {@link Person} first name
     * @param secondName {@link Person} second name
     */
    void createPerson(String passport, String firstName, String secondName) throws RemoteException;

    /**
     * Finds {@link Person} by passport in bank
     * @param passport passport of {@link Person} to find
     * @return {@link RemotePerson} which will be passed through RMI as Remote object.
     * if there is no such {@link Person} in bank return null
     */
    Person getRemotePerson(String passport) throws RemoteException;

    /**
     * Finds {@link Person} by passport in bank
     * @param passport passport of {@link Person} to find
     * @return {@link RemotePerson} which will be passed through RMI as Remote object.
     * if there is no such {@link Person} in bank return null
     */
    Person getLocalPerson(String passport) throws RemoteException;

}
