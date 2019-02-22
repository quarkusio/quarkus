package io.quarkus.extest;

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

    public boolean equals(Object obj) {
        ObjectValueOf oov = (ObjectValueOf) obj;
        return part1.equals(oov.part1) && part2.equals(oov.part2);
    }

    @Override
    public String toString() {
        return "ObjectOfValue{" +
                "part1='" + part1 + '\'' +
                ", part2='" + part2 + '\'' +
                '}';
    }
}
