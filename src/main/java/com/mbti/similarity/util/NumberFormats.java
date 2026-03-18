package com.mbti.similarity.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public final class NumberFormats {
    private NumberFormats() {
    }

    public static String formatFixed(double value, int decimals) {
        return String.format(Locale.ROOT, "%." + decimals + "f", value);
    }

    public static String jsonNumber(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("JSON numbers must be finite.");
        }
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    public static String jsonRounded(double value, int scale) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("JSON numbers must be finite.");
        }

        BigDecimal decimal = BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros();
        return decimal.toPlainString();
    }
}
