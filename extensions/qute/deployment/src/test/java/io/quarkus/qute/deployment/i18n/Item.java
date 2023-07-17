package io.quarkus.qute.deployment.i18n;

import java.util.List;

public class Item {

    private String name;

    private Integer age;

    public Item(String name, Integer age) {
        this.name = name;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public Integer getAge() {
        return age;
    }

    public List<String> getNames() {
        return List.of(name);
    }

}
