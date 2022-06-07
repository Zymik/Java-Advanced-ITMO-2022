package info.kgeorgiy.ja.kosolapov.walk.hash;

import info.kgeorgiy.ja.kosolapov.walk.exception.FileHashException;

import java.nio.file.Path;

public interface FileHash {
    String hashFile(Path file) throws FileHashException;

    String defaultFileHash();
}
