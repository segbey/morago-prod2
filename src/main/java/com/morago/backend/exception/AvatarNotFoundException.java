package com.morago.backend.exception;

public class AvatarNotFoundException extends ResourceNotFoundException {
    public AvatarNotFoundException(Long userId) {
        super("Avatar not found for userId=" + userId);
    }
}