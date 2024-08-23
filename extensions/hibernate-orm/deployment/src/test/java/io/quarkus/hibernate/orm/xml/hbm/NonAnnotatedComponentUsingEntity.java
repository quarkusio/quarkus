package io.quarkus.hibernate.orm.xml.hbm;

public class NonAnnotatedComponentUsingEntity {

    private long id;

    private NonAnnotatedComponent value = new NonAnnotatedComponent();

    public NonAnnotatedComponentUsingEntity() {
    }

    public NonAnnotatedComponentUsingEntity(String name) {
        this.value.setName(name);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public NonAnnotatedComponent getValue() {
        return value;
    }

    public void setValue(NonAnnotatedComponent value) {
        this.value = value;
    }
}
