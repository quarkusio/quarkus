package io.quarkus.resteasy.reactive.jackson.deployment.test;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SensorUnit {

    /**
     * Note that for simplicity's sake, we're not supporting deca prefix since it would make the prefix identification logic
     * more complex and it's not a very used prefix. Might revisit as needed.
     */
    private final static Map<String, Double> prefixesToFactors = Map.of(
            "n", 1e-9,
            "µ", 1e-6,
            "m", 1e-3,
            "c", 1e-2,
            "d", 1e-1,
            "h", 100.0,
            "k", 1e3,
            "M", 1e6,
            "G", 1e9);
    private final static Map<String, SensorUnit> baseUnits = new HashMap<>();
    public static final SensorUnit W = baseUnitFor("W");
    public static final SensorUnit J = baseUnitFor("J");
    public static final SensorUnit decimalPercentage = baseUnitFor("decimal percentage");
    private final static Map<String, SensorUnit> knownUnits = new HashMap<>();
    public static final SensorUnit mW = SensorUnit.of("mW");
    public static final SensorUnit µJ = SensorUnit.of("µJ");
    private final String symbol;
    private final SensorUnit base;
    private final double factor;

    public SensorUnit(String symbol, SensorUnit base, double factor) {
        this.symbol = Objects.requireNonNull(symbol).trim();
        this.base = base != null ? base : this;
        this.factor = factor < 0 ? 1 : factor;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static SensorUnit of(String symbol) {
        symbol = Objects.requireNonNull(symbol, "Unit symbol cannot be null").trim();
        if (symbol.isBlank()) {
            throw new IllegalArgumentException("Unit symbol cannot be blank");
        }

        var unit = baseUnits.get(symbol);
        if (unit != null) {
            return unit;
        }

        unit = knownUnits.get(symbol);
        if (unit != null) {
            return unit;
        }

        if (symbol.length() == 1) {
            // assume base unit
            return baseUnitFor(symbol);
        }

        final var prefix = symbol.substring(0, 1);
        final var base = symbol.substring(1);
        var baseUnit = baseUnits.get(base);
        if (baseUnit == null) {
            baseUnit = baseUnitFor(base);
        }
        final var factor = prefixesToFactors.get(prefix);
        if (factor == null) {
            throw new IllegalArgumentException(
                    "Unknown unit prefix '" + prefix + "', known prefixes: " + prefixesToFactors.entrySet().stream()
                            .sorted(Comparator.comparingDouble(Map.Entry::getValue)).map(Map.Entry::getKey).toList());
        }

        unit = new SensorUnit(symbol, baseUnit, factor);
        knownUnits.put(symbol, unit);
        return unit;
    }

    private static SensorUnit baseUnitFor(String symbol) {
        return baseUnits.computeIfAbsent(symbol, s -> new SensorUnit(symbol, null, 1.0));
    }

    public boolean isCommensurableWith(SensorUnit other) {
        return other != null && base.equals(other.base);
    }

    public double conversionFactorTo(SensorUnit other) {
        if (other.isCommensurableWith(this)) {
            return factor / other.factor;
        } else {
            throw new IllegalArgumentException("Cannot convert " + this + " to " + other);
        }
    }

    @JsonProperty("symbol")
    public String symbol() {
        return symbol;
    }

    public SensorUnit base() {
        return base;
    }

    public double factor() {
        return factor;
    }

    @Override
    public String toString() {
        return symbol;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SensorUnit that = (SensorUnit) o;
        return Objects.equals(symbol, that.symbol);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(symbol);
    }
}
