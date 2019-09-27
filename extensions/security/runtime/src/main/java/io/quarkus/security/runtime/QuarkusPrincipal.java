package io.quarkus.security.runtime;

import java.security.Principal;
import java.util.Objects;

public class QuarkusPrincipal implements Principal {

    private final String name;

    public QuarkusPrincipal(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        QuarkusPrincipal that = (QuarkusPrincipal) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
