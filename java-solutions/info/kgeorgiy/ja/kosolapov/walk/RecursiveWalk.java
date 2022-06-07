package info.kgeorgiy.ja.kosolapov.walk;

import info.kgeorgiy.ja.kosolapov.walk.exception.RecursiveWalkException;
import info.kgeorgiy.ja.kosolapov.walk.hash.SHA1FileHash;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;


public class RecursiveWalk {


    private static boolean checkArgs(final String[] args) {
        return args != null && args.length >= 2 && args[0] != null && args[1] != null;
    }

    public static String exceptionFormat(final String message, final Exception e) {
        return String.format("%s: %s", message, e.getMessage());
    }

    public static void printExceptionWithMessage(final String message, final Exception e) {
        System.out.printf("%s: %s%n", message, e.getMessage());
    }

    public static void main(final String[] args) {
        if (!checkArgs(args)) {
            System.out.println("Arguments do not match format");
            return;
        }
        try {

            final var output = Path.of(args[1]);
            try {
                Files.createDirectories(output.getParent());
            } catch (final IOException | NullPointerException | SecurityException ignored) {
                //Probably we have rights to write into it
            }
            try {
                final var input = Path.of(args[0]);
                run(input, output);
                // :NOTE: CP
            } catch (final InvalidPathException e) {
                printExceptionWithMessage("Output file has invalid name", e);
            } catch (final RecursiveWalkException e) {
                System.out.println(e.getMessage());
            }
        } catch (final InvalidPathException e) {
            printExceptionWithMessage("Input file has invalid name", e);
        }
    }


    public static void run(final Path input, final Path output) throws RecursiveWalkException {
        try (final var reader = Files.newBufferedReader(input)) {
            try (final var writer = Files.newBufferedWriter(output)) {
                recursiveWalk(reader, writer);
            } catch (final IOException e) {
                throw new RecursiveWalkException(exceptionFormat("Can not open output file", e), e);
            } catch (final SecurityException e) {
                throw new RecursiveWalkException(exceptionFormat("Can not access to output file", e), e);
            }
        } catch (final IOException e) {
            throw new RecursiveWalkException(exceptionFormat("Can not open input file", e), e);
        } catch (final SecurityException e) {
            throw new RecursiveWalkException(exceptionFormat("Can not access to input file", e), e);
        }
    }

    public static void recursiveWalk(final BufferedReader reader, final BufferedWriter writer) throws RecursiveWalkException {
        final HashFileVisitor hashFileVisitor;
        try {
            hashFileVisitor = new HashFileVisitor(writer, new SHA1FileHash());
        } catch (final NoSuchAlgorithmException e) {
            throw new RecursiveWalkException(exceptionFormat("Unsupported SHA1 encoding", e), e);
        }

        while (true) {
            final String line;
            try {
                line = reader.readLine();
                if (line == null) {
                    break;
                }
            } catch (final IOException e) {
                throw new RecursiveWalkException(exceptionFormat("Exception while reading input", e), e);
            }

            try {
                hashFileVisitor.visit(line);
            } catch (final IOException e) {
                throw new RecursiveWalkException(exceptionFormat("Exception while writing to output", e), e);
            }
        }
    }
}
