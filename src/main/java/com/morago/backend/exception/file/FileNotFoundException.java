package com.morago.backend.exception.file;

import com.morago.backend.exception.common.NotFoundException;

public class FileNotFoundException extends NotFoundException {
  public FileNotFoundException(Long id) { super("File", id); }
  public FileNotFoundException(String key) { super("File", key); }
}