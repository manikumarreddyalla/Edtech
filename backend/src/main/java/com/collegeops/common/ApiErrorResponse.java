package com.collegeops.common;

import java.util.List;

public record ApiErrorResponse(String code, String message, List<FieldErrorDetail> details, String traceId) {
}
