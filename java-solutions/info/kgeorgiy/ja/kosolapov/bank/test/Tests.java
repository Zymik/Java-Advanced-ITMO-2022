package info.kgeorgiy.ja.kosolapov.bank.test;


import info.kgeorgiy.ja.kosolapov.bank.PersonApplication;
import info.kgeorgiy.ja.kosolapov.bank.account.Account;
import info.kgeorgiy.ja.kosolapov.bank.bank.Bank;
import info.kgeorgiy.ja.kosolapov.bank.bank.RemoteBank;
import info.kgeorgiy.ja.kosolapov.bank.person.AbstractPerson;
import info.kgeorgiy.ja.kosolapov.bank.person.LocalPerson;
import info.kgeorgiy.ja.kosolapov.bank.person.Person;
import info.kgeorgiy.ja.kosolapov.bank.person.RemotePerson;
import org.junit.jupiter.api.*;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;


@DisplayName("Bank tests")
@TestMethodOrder(MethodOrderer.MethodName.class)
public class Tests {

    private static final int BANK_PORT = 8888;
    private static final int REGISTRY_PORT = 8889;
    private static final int TEST_SIZE = 4;
    private static final String BANK_SUB_URL = "bank";
    private static final String BANK_FULL_URL = "//localhost:" + REGISTRY_PORT + "/" + BANK_SUB_URL;
    private static Registry registry;
    private Bank realBank;
    private Bank bank;
    private static final List<String> passports = IntStream.range(0, TEST_SIZE).mapToObj(Integer::toString).toList();
    private static final List<String> firstNames = List.of("Ivan", "Andrew", "Georgiy", "Mike");
    private static final List<String> secondNames = List.of("Kosolapov", "Stankevich", "Korneev", "Mirzayanov");


    @BeforeAll
    public static void configureRegistry() throws RemoteException {
        LocateRegistry.createRegistry(REGISTRY_PORT);
        registry = LocateRegistry.getRegistry(REGISTRY_PORT);
    }

    @BeforeEach
    public void configureBank() throws RemoteException, AlreadyBoundException, NotBoundException {

        realBank = new RemoteBank(BANK_PORT);
        UnicastRemoteObject.exportObject(realBank, BANK_PORT);
        registry.bind(BANK_SUB_URL, realBank);
        bank = (Bank) registry.lookup(BANK_SUB_URL);
        for (int i = 0; i < TEST_SIZE; i++) {
            bank.createPerson(passports.get(i), firstNames.get(i), secondNames.get(i));
        }
    }

    @AfterEach
    public void unbindBank() throws NotBoundException, RemoteException {
        registry.unbind(BANK_SUB_URL);
        UnicastRemoteObject.unexportObject(realBank, true);
    }

    private static String fullName(int i) {
        return passports.get(i) + ":" + firstNames.get(i) + ":" + secondNames.get(i);
    }

    private static void stringEquals(String heading, String s1, String s2) {
        Assertions.assertEquals(s1, s2, heading + ": " + s1 + " should be equals to " + s2);
    }

    @Test
    @DisplayName("Test that Persons was added")
    public void containsPersons() throws RemoteException, NotBoundException {
        bank = (Bank) registry.lookup(BANK_SUB_URL);
        for (int i = 0; i < TEST_SIZE; i++) {
            String firstName = firstNames.get(i);
            String secondName = secondNames.get(i);
            String passport = passports.get(i);
            String fullName = fullName(i);

            Person person = bank.getRemotePerson(passport);
            Assertions.assertNotNull(person, fullName + " is not contains in Bank");
            Assertions.assertAll("Testing fields ",
                    () -> stringEquals("Passport", passport, person.getPassport()),
                    () -> stringEquals("First name", firstName, person.getFirstName()),
                    () -> stringEquals("Second name", secondName, person.getSecondName()));

        }
    }

    @Test
    @DisplayName("Not contains Missing Persons")
    public void notContains() throws RemoteException {
        String first = "oirsdfjkas";
        String second = "oirsdfjkas121";
        String error = ": Person wasn't added";
        Assertions.assertNull(bank.getRemotePerson(first), first + error);
        Assertions.assertNull(bank.getRemotePerson(second), second + error);
    }

    @DisplayName("Test that RemotePerson is proxy and LocalPerson is not proxy")
    @Test
    public void testPersonsTypes() throws RemoteException {
        Assertions.assertNotEquals(bank.getRemotePerson(passports.get(0)).getClass(),
                RemotePerson.class, "RemotePerson should be proxy");
        Assertions.assertEquals(bank.getLocalPerson(passports.get(0)).getClass(),
                LocalPerson.class, "LocalPerson should not be proxy");
    }

    private static void checkAccountId(Account account, String id) throws RemoteException {
        Assertions.assertEquals(account.getId(), id, "Account id " + account.getId() +
                " should in \"passport:subid\" format and equal to :" + id);
    }

    @DisplayName("Test that Accounts make changes and have required id")
    @Test
    public void testAccount() throws RemoteException {
        Account acc1 = bank.createAccount(passports.get(0), "123");
        String fullName1 = AbstractPerson.accountIdFormat(passports.get(0), "123");
        acc1.setAmount(100);
        checkAccountId(acc1, fullName1);
        Assertions.assertEquals(100, acc1.getAmount(), "Account didn't change amount of money from 0 to 100");
        Account acc2 = bank.createAccount(passports.get(0), "1234");
        String fullName2 = AbstractPerson.accountIdFormat(passports.get(0), "1234");
        checkAccountId(acc2, fullName2);
        acc1.setAmount(102);
        Assertions.assertEquals(102, acc1.getAmount(), "Account didn't change amount of money from 0 to 102");
        Assertions.assertNotEquals(acc1.getAmount(), acc2.getAmount(), "Different accounts have same amount of money");
    }

    @DisplayName("Test Persons visibility of Accounts changing")
    @Test
    public void testPersonVisibility() throws RemoteException {
        String passport = passports.get(0);
        Person person = bank.getRemotePerson(passport);
        Person localPerson = bank.getLocalPerson(passport);
        String accountPartId = "123";
        person.createAccount(accountPartId);

        Assertions.assertFalse(localPerson.checkAccount(accountPartId), "Account was created remote," +
                " but visible in LocalPerson");

        Account acc = bank.getAccount(passport, accountPartId);
        Person local = bank.getLocalPerson(passport);

        acc.setAmount(100);
        Assertions.assertEquals(acc.getAmount(), person.getAccountAmount(accountPartId),
                "RemotePerson and Account should return same amount of money");
        Assertions.assertTrue(local.checkAccount(accountPartId), "LocalPerson created after Account creation, " +
                "but doesn't see it");

        local.setAccountAmount(accountPartId, 99);
        Assertions.assertEquals(local.getAccountAmount(accountPartId), 99, "LocalPerson account wasn't changed");
        Assertions.assertNotEquals(local.getAccountAmount(accountPartId), acc.getAmount(),
                "LocalPerson account value changing shouldn't be visible remote");
        local.createAccount("999");
        Assertions.assertFalse(person.checkAccount("999"), "Account was created in local person, but visible in remote");
    }

    @DisplayName("Parallel test")
    @Test
    public void parallelTest() throws InterruptedException, RemoteException {
        var accountId = new String[]{"123", "456", "7"};
        List<Callable<Object>> tasks = new ArrayList<>();
        List<Person> persons = passports.stream().map(x -> {
            try {
                return bank.getRemotePerson(x);
            } catch (RemoteException ignored) {
                return null;
            }
        }).toList();

        ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        for (var i : persons) {
            for (var j : accountId) {
                tasks.add(() -> {
                    i.createAccount(j);
                    return null;
                });
            }
        }
        Collections.shuffle(tasks);
        service.invokeAll(tasks);
        for (var i : persons) {
            for (var j : accountId) {
                Assertions.assertTrue(i.checkAccount(j), "Account " + j + " should contain in person: " + i);
            }
        }

        tasks = new ArrayList<>();
        int[] values = IntStream.range(0, 10).map(x -> x * x).toArray();
        int result = Arrays.stream(values).sum();
        for (var i : persons) {
            for (var j : accountId) {
                for (var val : values) {
                    tasks.add(() -> {
                        i.addAccountAmount(j, val);
                        return null;
                    });
                    tasks.add(() -> {
                        bank.getAccount(i.getPassport(), j).addAmount(val);
                        return null;
                    });
                }
            }
        }
        Collections.shuffle(tasks);
        service.invokeAll(tasks);
        for (var i : persons) {
            for (var j : accountId) {
                Assertions.assertEquals(2 * result, i.getAccountAmount(j), "Account " + j + " of " + i + " should have "
                        + 2 * result + "on balance");
            }
        }
        service.shutdown();
    }

    private static void executeApplication(Person person, String id, int value) throws RemoteException {
        executeApplication(person.getPassport(), person.getFirstName(), person.getSecondName(), id, value);
    }
    private static void executeApplication(String passport, String firstName, String secondName, String id, int value) {
        PersonApplication.main(BANK_FULL_URL, passport, firstName, secondName, id, Integer.toString(value));
    }
    @DisplayName("Application test")
    @Test
    public void applicationTest() throws RemoteException {
        var person = bank.getRemotePerson("0");
        var accountId = "1234";
        int amount = 100;
        executeApplication(person, accountId, amount);
        Assertions.assertTrue(person.checkAccount(accountId), "Account " + accountId + " wasn't created");
        Assertions.assertEquals(amount, person.getAccountAmount(accountId), "Account amount must be equal after creation to "
                + amount);
        executeApplication(person.getPassport(), "1234", "1234", accountId, amount);
        Assertions.assertEquals(amount, person.getAccountAmount(accountId), "Account amount mustn't change with invalid user "
                + amount);
        executeApplication(person, accountId, amount);
        Assertions.assertEquals(2 * amount, person.getAccountAmount(accountId), "Account amount must be equal to after two changes "
                + 2 * amount);
    }
}
