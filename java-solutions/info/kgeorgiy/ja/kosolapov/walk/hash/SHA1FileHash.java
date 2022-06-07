package info.kgeorgiy.ja.kosolapov.walk.hash;

import info.kgeorgiy.ja.kosolapov.walk.exception.FileHashException;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHA1FileHash implements FileHash {

    private static final String ZERO_HASH = "0".repeat(40);
    private static final int BUFFER_SIZE = 8192;
    private final byte[] buffer = new byte[BUFFER_SIZE];

    private final MessageDigest messageDigest ;
    public SHA1FileHash() throws NoSuchAlgorithmException {
        messageDigest = MessageDigest.getInstance("SHA1");
    }

    @Override
    public String hashFile(final Path file) throws FileHashException {
        try (final var reader = new DigestInputStream(Files.newInputStream(file), messageDigest)) {
            // :NOTE: Переиспользовать

            while (true) {
                if (reader.read(buffer) < 0) {
                    break;
                }
            }
            return bytesToHexString(reader.getMessageDigest().digest());
        } catch (final IOException | SecurityException e) {
            messageDigest.reset();
            throw new FileHashException("Exception while hashing file: " + e.getMessage(), e);
        }
    }

    @Override
    public String defaultFileHash() {
        return ZERO_HASH;
    }

    private static String bytesToHexString(final byte[] bytes) {
        return String.format("%0" + (bytes.length << 1) + "x", new BigInteger(1, bytes));
    }
}
