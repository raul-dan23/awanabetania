package com.awanabetania.awanabetania.Exception;

import org.springframework.http.HttpStatus;

/**
 * A failure the caller can act on: an unknown id, a state that forbids the operation,
 * a wrong PIN. Services throw it instead of building HTTP responses themselves;
 * {@link GlobalExceptionHandler} turns it into an RFC 7807 problem response whose
 * {@code detail} is this exception's message, shown to the user as is.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String detail) {
        super(detail);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    /** 400: the request is well-formed but its values are not acceptable. */
    public static ApiException badRequest(String detail) {
        return new ApiException(HttpStatus.BAD_REQUEST, detail);
    }

    /**
     * 403: the caller is logged in but may not do this. Never 401 here: the frontend reads
     * 401 as an expired session and logs the user out.
     */
    public static ApiException forbidden(String detail) {
        return new ApiException(HttpStatus.FORBIDDEN, detail);
    }

    /** 404: the referenced record does not exist. */
    public static ApiException notFound(String detail) {
        return new ApiException(HttpStatus.NOT_FOUND, detail);
    }

    /** 409: the record's current state does not allow the operation (already approved, too few points). */
    public static ApiException conflict(String detail) {
        return new ApiException(HttpStatus.CONFLICT, detail);
    }

    /** 503: something the operation depends on (the database backup) failed; nothing was changed. */
    public static ApiException unavailable(String detail) {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, detail);
    }
}
