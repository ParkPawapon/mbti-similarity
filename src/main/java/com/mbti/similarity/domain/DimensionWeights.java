package com.mbti.similarity.domain;

import com.mbti.similarity.util.NumberFormats;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class DimensionWeights {
    private static final double EPSILON = 1e-12;

    public static final DimensionWeights EQUAL = create(1, 1, 1, 1, 1, 1, 1, 1);

    private final double ne;
    private final double ni;
    private final double te;
    private final double ti;
    private final double se;
    private final double si;
    private final double fe;
    private final double fi;

    private DimensionWeights(
        double ne,
        double ni,
        double te,
        double ti,
        double se,
        double si,
        double fe,
        double fi
    ) {
        this.ne = ne;
        this.ni = ni;
        this.te = te;
        this.ti = ti;
        this.se = se;
        this.si = si;
        this.fe = fe;
        this.fi = fi;
    }

    public static DimensionWeights parse(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("--weights must not be empty.");
        }

        Map<String, Double> values = new LinkedHashMap<>();
        for (String dimension : CognitiveScores.DIMENSIONS) {
            values.put(dimension, 1.0d);
        }

        String[] tokens = rawValue.split(",");
        boolean hasToken = false;
        for (String token : tokens) {
            String trimmedToken = token.trim();
            if (trimmedToken.isEmpty()) {
                continue;
            }

            hasToken = true;
            String[] pair = trimmedToken.split("=", 2);
            if (pair.length != 2) {
                throw new IllegalArgumentException(
                    "Invalid weight token '" + trimmedToken + "'. Use format like Ne=1.2."
                );
            }

            String dimension = normalizeDimension(pair[0]);
            double parsedValue;
            try {
                parsedValue = Double.parseDouble(pair[1].trim());
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException(
                    "Weight for '" + dimension + "' is not numeric: '" + pair[1].trim() + "'.",
                    ex
                );
            }

            values.put(dimension, parsedValue);
        }

        if (!hasToken) {
            throw new IllegalArgumentException("--weights format is invalid.");
        }

        return create(
            values.get("Ne"),
            values.get("Ni"),
            values.get("Te"),
            values.get("Ti"),
            values.get("Se"),
            values.get("Si"),
            values.get("Fe"),
            values.get("Fi")
        );
    }

    public static DimensionWeights create(
        double ne,
        double ni,
        double te,
        double ti,
        double se,
        double si,
        double fe,
        double fi
    ) {
        validateWeight(ne, "Ne");
        validateWeight(ni, "Ni");
        validateWeight(te, "Te");
        validateWeight(ti, "Ti");
        validateWeight(se, "Se");
        validateWeight(si, "Si");
        validateWeight(fe, "Fe");
        validateWeight(fi, "Fi");

        double total = ne + ni + te + ti + se + si + fe + fi;
        if (total <= EPSILON) {
            throw new IllegalArgumentException("At least one weight must be greater than 0.");
        }

        return new DimensionWeights(
            ne / total,
            ni / total,
            te / total,
            ti / total,
            se / total,
            si / total,
            fe / total,
            fi / total
        );
    }

    public double getNe() {
        return ne;
    }

    public double getNi() {
        return ni;
    }

    public double getTe() {
        return te;
    }

    public double getTi() {
        return ti;
    }

    public double getSe() {
        return se;
    }

    public double getSi() {
        return si;
    }

    public double getFe() {
        return fe;
    }

    public double getFi() {
        return fi;
    }

    public boolean isEqualProfile() {
        return almostEqual(ne, EQUAL.ne)
            && almostEqual(ni, EQUAL.ni)
            && almostEqual(te, EQUAL.te)
            && almostEqual(ti, EQUAL.ti)
            && almostEqual(se, EQUAL.se)
            && almostEqual(si, EQUAL.si)
            && almostEqual(fe, EQUAL.fe)
            && almostEqual(fi, EQUAL.fi);
    }

    public Map<String, Double> toMap() {
        Map<String, Double> values = new LinkedHashMap<>();
        values.put("Ne", ne);
        values.put("Ni", ni);
        values.put("Te", te);
        values.put("Ti", ti);
        values.put("Se", se);
        values.put("Si", si);
        values.put("Fe", fe);
        values.put("Fi", fi);
        return values;
    }

    public String toCompactDisplay() {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Double> entry : toMap().entrySet()) {
            if (!first) {
                builder.append(", ");
            }
            builder.append(entry.getKey())
                .append('=')
                .append(NumberFormats.formatFixed(entry.getValue(), 4));
            first = false;
        }
        return builder.toString();
    }

    private static String normalizeDimension(String rawDimension) {
        Objects.requireNonNull(rawDimension, "rawDimension");
        String candidate = rawDimension.trim();
        if (candidate.isEmpty()) {
            throw new IllegalArgumentException("Weight dimension key is empty.");
        }

        for (String dimension : CognitiveScores.DIMENSIONS) {
            if (dimension.equalsIgnoreCase(candidate)) {
                return dimension;
            }
        }

        throw new IllegalArgumentException(
            "Unknown dimension '" + candidate + "'. Valid keys: " + String.join(", ", CognitiveScores.DIMENSIONS)
        );
    }

    private static void validateWeight(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Weight for '" + name + "' must be a finite number.");
        }

        if (value < 0d) {
            throw new IllegalArgumentException("Weight for '" + name + "' must be >= 0.");
        }
    }

    private static boolean almostEqual(double left, double right) {
        return Math.abs(left - right) < EPSILON;
    }
}
