package com.collegeops.documents;

public interface DocumentStorage {
    String store(byte[] content, String extension);

    byte[] read(String storageKey);

    void delete(String storageKey);
}
