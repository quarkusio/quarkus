package io.quarkus.smallrye.graphql.deployment;

import java.util.Arrays;
import java.util.List;

/**
 * Just a test pojo
 */
public class TestPojo {
    private String message;
    private List<String> list = Arrays.asList("a", "b", "c");

    private Number number;

    public TestPojo() {
        super();
    }

    public TestPojo(String message) {
        super();
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getList() {
        return list;
    }

    public void setList(List<String> list) {
        this.list = list;
    }

    public Number getNumber() {
        return number;
    }

    public void setNumber(Number number) {
        this.number = number;
    }

    // <placeholder>

    @Override
    public String toString() {
        return "TestPojo{" + "message=" + message + ", list=" + list + ", number=" + number + '}';
    }

    enum Number {
        ONE,
        TWO,
        THREE
    }
}
