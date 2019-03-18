package io.quarkus.extest.runtime;

import java.util.Objects;

/**
 * A configuration type that has a static {@linkplain ObjectOfValue#of(String)} conversion method
 */
public class ObjectOfValue {
    private String part1;
    private String part2;

    public static ObjectOfValue of(String serial) {
        if (serial.isEmpty())
            return null;

        String[] parts = serial.split("\\+");
        return new ObjectOfValue(parts[0], parts[1]);
    }

    public ObjectOfValue() {

    }

    public ObjectOfValue(String part1, String part2) {
        this.part1 = part1;
        this.part2 = part2;
    }

    public String getPart1() {
        return part1;
    }

    public String getPart2() {
        return part2;
    }

    public boolean equals(Object obj) {
        ObjectOfValue oov = (ObjectOfValue) obj;
        return oov == this || oov != null && Objects.equals(part1, oov.part1) && Objects.equals(part2, oov.part2);
    }

    @Override
    public String toString() {
        return "ObjectOfValue{" +
                "part1='" + part1 + '\'' +
                ", part2='" + part2 + '\'' +
                '}';
    }
}
