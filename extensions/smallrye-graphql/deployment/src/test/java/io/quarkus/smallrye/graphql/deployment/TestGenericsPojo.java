package io.quarkus.smallrye.graphql.deployment;

import java.util.Arrays;
import java.util.List;

/**
 * Just a test pojo with generics
 */
public class TestGenericsPojo<T> {
    private T message;
    private List<String> list = Arrays.asList("aa", "bb", "cc");

    public TestGenericsPojo() {
        super();
    }

    public TestGenericsPojo(T message) {
        super();
        this.message = message;
    }

    public T getMessage() {
        return message;
    }

    public void setMessage(T message) {
        this.message = message;
    }

    public List<String> getList() {
        return list;
    }

    public void setList(List<String> list) {
        this.list = list;
    }

    @Override
    public String toString() {
        return "TestPojo{" + "message=" + message + ", list=" + list + '}';
    }

}