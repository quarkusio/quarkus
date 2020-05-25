package org.test;

import javax.persistence.*;

@Entity
public class SomeEntity {

    @Id
    private String id;

    private String foo;

    @Embedded
    private SomeEmbeddable someEmbeddable;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFoo() {
        return foo;
    }

    public void setFoo(String foo) {
        this.foo = foo;
    }

    public SomeEmbeddable getSomeEmbeddable() {
        return someEmbeddable;
    }

    public void setSomeEmbeddable(SomeEmbeddable someEmbeddable) {
        this.someEmbeddable = someEmbeddable;
    }
}
