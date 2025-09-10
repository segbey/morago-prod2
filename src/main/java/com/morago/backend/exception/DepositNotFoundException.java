package com.morago.backend.exception;

public class DepositNotFoundException extends RuntimeException {
  public DepositNotFoundException(String message) {
    super(message);
  }
}
