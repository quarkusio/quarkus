package io.quarkus.hibernate.orm.xml.hbm;

public class NonAnnotatedComplexEntity {

    private long id;

    private NonAnnotatedComponentEntity value = new NonAnnotatedComponentEntity();

    public NonAnnotatedComplexEntity() {
    }

    public NonAnnotatedComplexEntity(String name) {
        this.value.setName(name);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public NonAnnotatedComponentEntity getValue() {
        return value;
    }

    public void setValue(NonAnnotatedComponentEntity value) {
        this.value = value;
    }
}
