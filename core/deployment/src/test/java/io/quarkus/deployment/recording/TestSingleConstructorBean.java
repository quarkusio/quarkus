package io.quarkus.deployment.recording;

import java.util.Objects;

public class TestSingleConstructorBean {

    private final String first;
    private final String last;
    private final int age;

    public TestSingleConstructorBean(String first, String last, int age) {
        this.first = first;
        this.last = last;
        this.age = age;
    }

    public String getFirst() {
        return first;
    }

    public String getLast() {
        return last;
    }

    public int getAge() {
        return age;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TestSingleConstructorBean that = (TestSingleConstructorBean) o;
        return age == that.age && Objects.equals(first, that.first) && Objects.equals(last, that.last);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, last, age);
    }
}
