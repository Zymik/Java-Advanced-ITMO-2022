package info.kgeorgiy.ja.kosolapov.bank.person;


import java.io.Serializable;

public abstract class AbstractPerson implements Person, Serializable {
    private final String passport;
    private final String firstName;
    private final String secondName;

    /**
     * Creates AbstractPerson with following {@code passport, firstName, secondName}
     */
    public AbstractPerson(String passport, String firstName, String secondName) {
        this.passport = passport;
        this.firstName = firstName;
        this.secondName = secondName;
    }


    /**
     * Make {@link String} in format {@code passport:id}
     */
    public static String accountIdFormat(String passport, String id) {
        return passport + ":" + id;
    }

    /**
     * Execute {@link #accountIdFormat} with {@code this.getPassport()} and {@code id}
     */
    public String accountFullName(String id) {
        return accountIdFormat(passport, id);
    }

    @Override
    public String getFirstName() {
        return firstName;
    }

    @Override
    public String getPassport() {
        return passport;
    }

    @Override
    public String getSecondName() {
        return secondName;
    }

    @Override
    public String toString() {
        return passport + ":" + firstName + ":" + secondName;
    }

}
