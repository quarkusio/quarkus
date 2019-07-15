package io.quarkus.axon.deployment.aggregatetest;

import java.io.Serializable;

public class TestItem implements Serializable {
    private Long id;
    private String someText;

    TestItem() {
    }

    public TestItem(Long id, String someText) {
        this.id = id;
        this.someText = someText;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSomeText() {
        return someText;
    }

    public void setSomeText(String someText) {
        this.someText = someText;
    }

    @Override
    public String toString() {
        return "TestItem{" +
                "id=" + id +
                ", someText='" + someText + '\'' +
                '}';
    }
}
