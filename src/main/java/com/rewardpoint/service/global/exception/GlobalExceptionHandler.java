package com.rewardpoint.service.global.exception;

import com.rewardpoint.service.pointledger.exception.PointOperationException;
import com.rewardpoint.service.pointledger.exception.PointPolicyNotFoundException;
import com.rewardpoint.service.pointledger.exception.PointTransactionNotFoundException;
import com.rewardpoint.service.pointaccount.exception.PointAccountAlreadyExistsException;
import com.rewardpoint.service.pointaccount.exception.PointAccountNotFoundException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PointAccountNotFoundException.class)
    public ProblemDetail handlePointAccountNotFound(PointAccountNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(PointAccountAlreadyExistsException.class)
    public ProblemDetail handlePointAccountAlreadyExists(PointAccountAlreadyExistsException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(PointTransactionNotFoundException.class)
    public ProblemDetail handlePointTransactionNotFound(PointTransactionNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(PointOperationException.class)
    public ProblemDetail handlePointOperation(PointOperationException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(PointPolicyNotFoundException.class)
    public ProblemDetail handlePolicyNotFound(PointPolicyNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException exception) {
        String detail = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }
}
