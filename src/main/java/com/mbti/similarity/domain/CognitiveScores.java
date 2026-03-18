package com.mbti.similarity.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class CognitiveScores {
    public static final List<String> DIMENSIONS = List.of("Ne", "Ni", "Te", "Ti", "Se", "Si", "Fe", "Fi");

    private final double ne;
    private final double ni;
    private final double te;
    private final double ti;
    private final double se;
    private final double si;
    private final double fe;
    private final double fi;

    public CognitiveScores(
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

    public double euclideanDistanceTo(CognitiveScores other) {
        Objects.requireNonNull(other, "other");

        double sumSquares =
            square(ne - other.ne) +
            square(ni - other.ni) +
            square(te - other.te) +
            square(ti - other.ti) +
            square(se - other.se) +
            square(si - other.si) +
            square(fe - other.fe) +
            square(fi - other.fi);

        return Math.sqrt(sumSquares);
    }

    public double weightedEuclideanDistanceTo(CognitiveScores other, DimensionWeights weights) {
        Objects.requireNonNull(other, "other");
        Objects.requireNonNull(weights, "weights");

        double sumSquares =
            weights.getNe() * square(ne - other.ne) +
            weights.getNi() * square(ni - other.ni) +
            weights.getTe() * square(te - other.te) +
            weights.getTi() * square(ti - other.ti) +
            weights.getSe() * square(se - other.se) +
            weights.getSi() * square(si - other.si) +
            weights.getFe() * square(fe - other.fe) +
            weights.getFi() * square(fi - other.fi);

        return Math.sqrt(sumSquares);
    }

    public Map<String, Double> absoluteDifferenceFrom(CognitiveScores other) {
        Objects.requireNonNull(other, "other");

        Map<String, Double> differences = new LinkedHashMap<>();
        differences.put("Ne", Math.abs(ne - other.ne));
        differences.put("Ni", Math.abs(ni - other.ni));
        differences.put("Te", Math.abs(te - other.te));
        differences.put("Ti", Math.abs(ti - other.ti));
        differences.put("Se", Math.abs(se - other.se));
        differences.put("Si", Math.abs(si - other.si));
        differences.put("Fe", Math.abs(fe - other.fe));
        differences.put("Fi", Math.abs(fi - other.fi));
        return differences;
    }

    public double[] toVector() {
        return new double[] {ne, ni, te, ti, se, si, fe, fi};
    }

    public static CognitiveScores fromVector(double[] values) {
        if (values == null || values.length != DIMENSIONS.size()) {
            throw new IllegalArgumentException("A score vector must contain exactly 8 values.");
        }

        return new CognitiveScores(
            values[0],
            values[1],
            values[2],
            values[3],
            values[4],
            values[5],
            values[6],
            values[7]
        );
    }

    private static double square(double value) {
        return value * value;
    }
}
