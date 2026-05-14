package org.example.meetingscheduler.exception;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(final NotFoundException notFoundException) {
        return new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            notFoundException.getMessage()
        );
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConflict(final ConflictException conflictException) {
        return new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            conflictException.getMessage()
        );
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleForbidden(final ForbiddenException forbiddenException) {
        return new ErrorResponse(
            HttpStatus.FORBIDDEN.value(),
            forbiddenException.getMessage()
        );
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequest(final BadRequestException badRequestException) {
        return new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            badRequestException.getMessage()
        );
    }

    @ExceptionHandler(UnprocessableEntityException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public ErrorResponse handleUnprocessable(final UnprocessableEntityException unprocessableEntityException) {
        return new ErrorResponse(
            HttpStatus.UNPROCESSABLE_CONTENT.value(),
            unprocessableEntityException.getMessage()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(final MethodArgumentNotValidException methodArgumentNotValidException) {
        return new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            methodArgumentNotValidException.getBindingResult().getAllErrors().stream()
                .findFirst()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .orElse("Validation failed")
        );
    }
}
