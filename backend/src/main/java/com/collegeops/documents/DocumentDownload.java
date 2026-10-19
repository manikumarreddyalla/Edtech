package com.collegeops.documents;

public record DocumentDownload(String contentType, String originalFilename, byte[] content) {
}
