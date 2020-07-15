package io.quarkus.deployment.recording;

import java.util.Objects;

import io.quarkus.runtime.annotations.RecordableConstructor;

public class TestConstructorBean {

    String first;
    final String last;
    int age;

    @RecordableConstructor
    public TestConstructorBean(String first, String last) {
        this.first = first;
        this.last = last;
    }

    public void setFirst(String first) {
        //should not be called, as it was initialized in the constructor
        this.first = "Mr " + first;
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

    public TestConstructorBean setAge(int age) {
        this.age = age;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TestConstructorBean that = (TestConstructorBean) o;
        return age == that.age &&
                Objects.equals(first, that.first) &&
                Objects.equals(last, that.last);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, last, age);
    }
}
