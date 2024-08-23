package io.quarkus.it.panache.reactive;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class Bug7721Entity extends Bug7721EntitySuperClass {
    @Column(nullable = false)
    public String foo = "default";

    public Bug7721Entity() {
        foo = "default"; // same as init
        this.foo = "default"; // qualify
        superField = "default";
        this.superField = "default";
        super.superField = "default";

        Bug7721OtherEntity otherEntity = new Bug7721OtherEntity();
        otherEntity.foo = "bar"; // we want to make sure the setter gets called because it's not our hierarchy
        if (!otherEntity.foo.equals("BAR"))
            throw new AssertionError("setter was not called", null);
    }

    public void setFoo(String foo) {
        Objects.requireNonNull(foo);
        // should never be null
        Objects.requireNonNull(this.foo);
        this.foo = foo;
    }
}
