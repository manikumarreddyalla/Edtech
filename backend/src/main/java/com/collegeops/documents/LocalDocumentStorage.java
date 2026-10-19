package com.collegeops.documents;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Component
@Profile({"dev", "test"})
public class LocalDocumentStorage implements DocumentStorage {
    private final Path root;

    public LocalDocumentStorage(@Value("${app.documents.storage-root}") String storageRoot) {
        this.root = Path.of(storageRoot).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to initialize private document storage.", exception);
        }
    }

    @Override
    public String store(byte[] content, String extension) {
        String storageKey = UUID.randomUUID() + extension;
        Path target = resolve(storageKey);
        Path temporary = resolve("." + UUID.randomUUID() + ".tmp");
        try {
            Files.write(temporary, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, target);
            }
            return storageKey;
        } catch (IOException exception) {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw new UncheckedIOException("Unable to store document content.", exception);
        }
    }

    @Override
    public byte[] read(String storageKey) {
        try {
            return Files.readAllBytes(resolve(storageKey));
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read stored document content.", exception);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to remove stored document content.", exception);
        }
    }

    private Path resolve(String storageKey) {
        if (!storageKey.matches("^(?:[0-9a-f-]{36}\\.(?:pdf|jpg|png|txt)|\\.[0-9a-f-]{36}\\.tmp)$")) {
            throw new IllegalArgumentException("Invalid document storage key.");
        }
        Path path = root.resolve(storageKey).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Invalid document storage path.");
        }
        return path;
    }
}
