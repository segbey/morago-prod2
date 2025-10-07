package com.morago.backend.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryDto {
    private Long id;
    private String name;
    private Boolean active;
//    private LocalDateTime createdAt;
//    private LocalDateTime updatedAt;
}
