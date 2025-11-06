package com.morago.backend.exception.translatorprofile;

public class TranslatorProfileAlreadyExistsException extends RuntimeException {
  public TranslatorProfileAlreadyExistsException(Long userId) {
    super("User " + userId + " already has a TranslatorProfile");
  }
}