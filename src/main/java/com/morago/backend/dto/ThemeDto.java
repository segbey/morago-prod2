package com.morago.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThemeDto {
    private Long id;
    private String name;
    private String koreanTitle;
    private BigDecimal price;
    private BigDecimal nightPrice;
    private String description;
    private Boolean popular;
    private Boolean active;
    private Long categoryId;
    private String iconUrl;
}
