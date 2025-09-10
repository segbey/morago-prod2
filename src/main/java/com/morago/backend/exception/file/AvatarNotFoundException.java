package com.morago.backend.exception.file;

public class AvatarNotFoundException extends RuntimeException {
  public AvatarNotFoundException(String message) {
    super(message);
  }
}
