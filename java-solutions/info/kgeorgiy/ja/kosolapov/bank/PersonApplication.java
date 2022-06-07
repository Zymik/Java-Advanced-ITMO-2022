package info.kgeorgiy.ja.kosolapov.bank;

import info.kgeorgiy.ja.kosolapov.bank.bank.Bank;
import info.kgeorgiy.ja.kosolapov.bank.person.Person;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;


public class PersonApplication {

    private static boolean validatePerson(Person person, String passport, String firstName,
                                          String secondName) throws RemoteException {
        return person.getPassport().equals(passport) &&
                person.getFirstName().equals(firstName) &&
                person.getSecondName().equals(secondName);
    }

    private static String getErrorString(Person person, String passport, String firstName,
                                         String secondName) throws RemoteException {
        return person.getPassport() + ":" + passport + ";" + person.getFirstName() + ":" +
                firstName + ";" + person.getSecondName() + ":" + secondName;
    }

    private static boolean validateArgs(String[] args) {
        return args != null && args.length == 6;
    }

    /**
     * Command line arguments: bank url, first name, second name, passport, account subId, change of account amount.
     * If information about the {@link Person} is missing, then it should be added. His data should be checked.
     * If Person does not have an account with a subId, then it creates with a zero balance.
     * After updating the amounts, the new balance should be displayed on the console.
     * @param args args in specified format
     */
    public static void main(String... args)  {
        if (!validateArgs(args)) {
            System.err.println("Invalid args format, size must be 6");
        }
        Bank bank = null;
        try {
            bank = (Bank) Naming.lookup(args[0]);
        } catch (NotBoundException e) {
            System.err.println("Bank isn't bound: " + e.getMessage());
        } catch (RemoteException e) {
            System.err.println("Remote exception: " + e.getMessage());
        } catch (MalformedURLException e) {
            System.err.println("URL isn't valid: " + e.getMessage());
        }
        if (bank == null) {
            return;
        }
        String passport = args[1];
        String firstName = args[2];
        String secondName = args[3];
        String id = args[4];
        int value;
        try {
            value = Integer.parseInt(args[5]);
        } catch (NumberFormatException e) {
            System.err.println("Fifth arg have invalid number format: " + args[5]);
            return;
        }
        runOperation(bank, passport, firstName, secondName, id, value);
    }

    private static void runOperation(Bank bank, String passport, String firstName, String secondName, String id, int value) {
        try {
            Person person = bank.getRemotePerson(passport);
            if (person == null) {
                bank.createPerson(passport, firstName, secondName);
                person = bank.getRemotePerson(passport);
            }
            if (!validatePerson(person, passport, firstName, secondName)) {
                System.err.println("Invalid person: " +
                        getErrorString(person, passport, firstName, secondName));
                return;
            }
            if (!person.checkAccount(id)) {
                person.createAccount(id);
            }
            person.addAccountAmount(id, value);
            System.out.println("Account amount: " + person.getAccountAmount(id));

        } catch (RemoteException e) {
            System.err.println("Exception while executing remote method: " + e.getMessage());
        }
    }
}
