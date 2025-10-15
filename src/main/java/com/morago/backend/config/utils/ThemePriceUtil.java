package com.morago.backend.config.utils;

import com.morago.backend.entity.Money;
import com.morago.backend.entity.Theme;

import java.math.BigDecimal;
import java.time.LocalTime;

public final class ThemePriceUtil {
    private ThemePriceUtil() {}

    public static BigDecimal perMinute(Theme theme) {
        boolean night = LocalTime.now().isAfter(LocalTime.of(22,0))
                || LocalTime.now().isBefore(LocalTime.of(7,0));

        BigDecimal base = theme.getPrice();
        if (base == null) throw new IllegalStateException("Theme price is not set");

        BigDecimal nightPrice = theme.getNightPrice();
        BigDecimal perMinute = Money.s2(night && nightPrice != null ? nightPrice : base);

        if (perMinute.signum() <= 0) {
            throw new IllegalArgumentException("Theme per-minute price must be > 0");
        }
        return perMinute;
    }
}