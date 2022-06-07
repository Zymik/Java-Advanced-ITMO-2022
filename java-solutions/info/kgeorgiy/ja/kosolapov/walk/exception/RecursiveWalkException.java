package info.kgeorgiy.ja.kosolapov.walk.exception;
public class RecursiveWalkException extends Exception{
    public RecursiveWalkException(Throwable e) {
        super(e);
    }

    public RecursiveWalkException(String message, Throwable e) {
        super(message, e);
    }
}

