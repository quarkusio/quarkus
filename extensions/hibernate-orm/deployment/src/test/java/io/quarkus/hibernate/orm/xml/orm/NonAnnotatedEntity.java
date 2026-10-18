package io.quarkus.hibernate.orm.xml.orm;

public class NonAnnotatedEntity {

    private long id;

    private String name;

    public NonAnnotatedEntity() {
    }

    public NonAnnotatedEntity(String name) {
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
}
