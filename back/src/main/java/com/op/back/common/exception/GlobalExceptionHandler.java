package com.op.back.common.exception;

import com.op.back.petsitter.exception.ReservationException;
import com.op.back.petsitter.exception.ReviewException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ReservationException.class)
    public ResponseEntity<?> handleReservation(
            ReservationException e
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT) // 409
                .body(Map.of(
                        "success", false,
                        "message", e.getMessage()
                ));
    }

    @ExceptionHandler(ReviewException.class)
    public ResponseEntity<?> handleReview(
            ReviewException e
    ){
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "success", false,
                        "message", e.getMessage()
                ));
    }

}
