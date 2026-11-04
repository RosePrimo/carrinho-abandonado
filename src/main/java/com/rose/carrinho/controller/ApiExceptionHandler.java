package com.rose.carrinho.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    // as regras do Carrinho (ex.: "Carrinho não está ativo") viram 409 Conflict
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail regraDeNegocio(IllegalStateException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }
}