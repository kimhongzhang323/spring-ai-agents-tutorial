package com.masterclass.structured;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExtractionExceptionHandler {

    @ExceptionHandler(ExtractionService.ExtractionFailedException.class)
    public ProblemDetail handleExtractionFailure(ExtractionService.ExtractionFailedException ex) {
        var problem = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        problem.setTitle("Extraction Failed");
        problem.setDetail(ex.getMessage());
        problem.setProperty("errorCode", "EXTRACTION_PARSE_FAILURE");
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleValidation(IllegalArgumentException ex) {
        var problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Invalid Input");
        problem.setDetail(ex.getMessage());
        return problem;
    }
}
