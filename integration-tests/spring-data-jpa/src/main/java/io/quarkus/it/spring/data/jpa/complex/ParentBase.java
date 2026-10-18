package io.quarkus.it.spring.data.jpa.complex;

import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public class ParentBase {

    private String name;
    private String detail;
    private int age;
    private float test;
    private TestEnum testEnum;

    public ParentBase(String name, String detail, int age, float test, TestEnum testEnum) {
        this.name = name;
        this.detail = detail;
        this.age = age;
        this.test = test;
        this.testEnum = testEnum;
    }

    public ParentBase() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public float getTest() {
        return test;
    }

    public void setTest(float test) {
        this.test = test;
    }

    public TestEnum getTestEnum() {
        return testEnum;
    }

    public void setTestEnum(TestEnum testEnum) {
        this.testEnum = testEnum;
    }
}
