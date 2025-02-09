package io.quarkus.deployment.recording;

import java.util.Objects;

public class OtherTestConstructorBean {

    String first;
    final String last;
    int age;

    @TestRecordingAnnotationsProvider.TestRecordableConstructor
    public OtherTestConstructorBean(String first, String last) {
        this.first = first;
        this.last = last;
    }

    public void setFirst(String first) {
        //should not be called, as it was initialized in the constructor
        this.first = "Mrs " + first;
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

    public OtherTestConstructorBean setAge(int age) {
        this.age = age;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OtherTestConstructorBean that = (OtherTestConstructorBean) o;
        return age == that.age
                && Objects.equals(first, that.first)
                && Objects.equals(last, that.last);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, last, age);
    }
}
