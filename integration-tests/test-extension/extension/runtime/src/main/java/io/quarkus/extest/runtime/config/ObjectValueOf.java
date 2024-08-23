package io.quarkus.extest.runtime.config;

import java.util.Objects;

/**
 * A configuration type that has a static {@linkplain ObjectValueOf#valueOf(String)} conversion method
 */
public class ObjectValueOf {
    private String part1;
    private String part2;

    public static ObjectValueOf valueOf(String serial) {
        if (serial.isEmpty())
            return null;

        String[] parts = serial.split("\\+");
        return new ObjectValueOf(parts[0], parts[1]);

    }

    public ObjectValueOf() {

    }

    public ObjectValueOf(String part1, String part2) {
        this.part1 = part1;
        this.part2 = part2;
    }

    public String getPart1() {
        return part1;
    }

    public String getPart2() {
        return part2;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ObjectValueOf) {
            ObjectValueOf other = (ObjectValueOf) obj;
            return Objects.equals(part1, other.part1) && Objects.equals(part2, other.part2);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(part1, part2);
    }

    @Override
    public String toString() {
        return "ObjectOfValue{" +
                "part1='" + part1 + '\'' +
                ", part2='" + part2 + '\'' +
                '}';
    }
}
