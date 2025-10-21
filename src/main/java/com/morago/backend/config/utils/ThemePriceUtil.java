package com.morago.backend.config.utils;

import com.morago.backend.entity.Money;
import com.morago.backend.entity.Theme;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalTime;

public final class ThemePriceUtil {
    private static final LocalTime NIGHT_START = LocalTime.of(22, 0);
    private static final LocalTime NIGHT_END   = LocalTime.of(7, 0);

    private ThemePriceUtil() {}

    public static BigDecimal perMinute(Theme theme) {
        return perMinute(theme, Clock.systemDefaultZone());
    }

    public static BigDecimal perMinute(Theme theme, Clock clock) {
        LocalTime now = LocalTime.now(clock);
        return perMinuteAt(theme, now);
    }

    public static BigDecimal perMinuteAt(Theme theme, LocalTime time) {
        if (theme == null) {
            throw new IllegalArgumentException("Theme must not be null");
        }

        BigDecimal base = theme.getPrice();
        if (base == null) {
            throw new IllegalStateException("Theme price is not set");
        }

        boolean night = isNight(time);
        BigDecimal nightPrice = theme.getNightPrice();

        BigDecimal chosen = (night && nightPrice != null) ? nightPrice : base;

        BigDecimal perMinute = Money.s2(chosen);
        if (perMinute.signum() <= 0) {
             if (night && nightPrice != null) {
                 perMinute = Money.s2(base);
                 if (perMinute.signum() <= 0) {
                     throw new IllegalArgumentException("Theme per-minute price must be > 0");
                 }
             } else {
                 throw new IllegalArgumentException("Theme per-minute price must be > 0");
             }
        }

        return perMinute;
    }

    public static boolean isNight(LocalTime t) {
        return !t.isBefore(NIGHT_START) || t.isBefore(NIGHT_END);
    }
}