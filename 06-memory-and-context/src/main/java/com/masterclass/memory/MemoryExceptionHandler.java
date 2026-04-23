package com.masterclass.memory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class MemoryExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleValidationError(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid Input");
        problem.setType(URI.create("urn:problem:invalid-input"));
        return problem;
    }
}
