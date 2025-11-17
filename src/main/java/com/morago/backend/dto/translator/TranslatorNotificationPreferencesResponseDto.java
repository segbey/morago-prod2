package com.morago.backend.dto.translator;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TranslatorNotificationPreferencesResponseDto {
    private Boolean emailNotifications;
    private Boolean pushNotifications;
    private Boolean smsNotifications;
    private Boolean callNotifications;
    private Boolean messageNotifications;
    private String message;
}

