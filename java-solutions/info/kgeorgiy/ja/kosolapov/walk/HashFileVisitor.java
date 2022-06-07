package info.kgeorgiy.ja.kosolapov.walk;


import info.kgeorgiy.ja.kosolapov.walk.exception.FileHashException;
import info.kgeorgiy.ja.kosolapov.walk.hash.FileHash;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;


public class HashFileVisitor extends SimpleFileVisitor<Path> {

    private final Writer writer;
    private final FileHash fileHash;

    public HashFileVisitor(final Writer writer, final FileHash fileHash) {
        this.writer = writer;
        this.fileHash = fileHash;
    }

    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
        try {
            printHashAndFileName(fileHash.hashFile(file.normalize()), file.toString());
        }  catch (final FileHashException e) {
            printDefaultHashAndFileName(file);
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {
        printHashAndFileName(fileHash.defaultFileHash(), file.toString());
        return FileVisitResult.CONTINUE;
    }

    public void printHashAndFileName(final String string, final String fileName) throws IOException {
        writer.write(String.format("%s %s%n", string, fileName));
    }

    public void printDefaultHashAndFileName(final Path file) throws IOException {
        printHashAndFileName(fileHash.defaultFileHash(), file.toString());
    }

    public void visit(final String fileName) throws IOException {
        try {
            Files.walkFileTree(Path.of(fileName), this);
        } catch (final InvalidPathException e) {
            printHashAndFileName(fileHash.defaultFileHash(), fileName);
        }
    }

}
