package com.morago.backend.exception;

public class WithdrawalNotFoundException extends RuntimeException {
  public WithdrawalNotFoundException(String message) {
    super(message);
  }
}
