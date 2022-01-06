package io.quarkus.hibernate.orm.xml.hbm;

public class NonAnnotatedComplexEntity {

    private long id;

    private String name;

    private NonAnnotatedComponentEntity value;

    public NonAnnotatedComplexEntity() {
    }

    public NonAnnotatedComplexEntity(String name) {
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public NonAnnotatedComponentEntity getValue() {
        return value;
    }

    public void setValue(NonAnnotatedComponentEntity value) {
        this.value = value;
    }
}
