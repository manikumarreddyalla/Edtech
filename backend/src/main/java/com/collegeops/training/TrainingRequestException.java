package com.collegeops.training;

public class TrainingRequestException extends RuntimeException {
    private final int status;
    private final String code;

    public TrainingRequestException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public int getStatus() { return status; }
    public String getCode() { return code; }
}
