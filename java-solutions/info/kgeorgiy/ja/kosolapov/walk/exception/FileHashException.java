package info.kgeorgiy.ja.kosolapov.walk.exception;

public class FileHashException extends Exception{
    public FileHashException(Throwable e) {
        super(e);
    }

    public FileHashException(String message, Throwable e) {
        super(message, e);
    }
}
