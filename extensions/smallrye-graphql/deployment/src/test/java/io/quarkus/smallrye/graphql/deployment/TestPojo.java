package io.quarkus.smallrye.graphql.deployment;

import java.util.Arrays;
import java.util.List;

/**
 * Just a test pojo
 */
public class TestPojo {
    private String message;
    private List<String> list = Arrays.asList(new String[] { "a", "b", "c" });

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

    @Override
    public String toString() {
        return "TestPojo{" + "message=" + message + ", list=" + list + '}';
    }

}
