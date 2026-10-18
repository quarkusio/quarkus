package io.quarkus.deployment.recording;

import java.util.Objects;

public class ValidationFails {

    private String name;
    private boolean nameValid;

    public ValidationFails() {
    }

    public ValidationFails(String name) {
        setName(name);
    }

    public String getName() {
        return name;
    }

    public ValidationFails setName(String name) {
        this.name = name;
        this.nameValid = name.contains(" ");
        return this;
    }

    public boolean isNameValid() {
        return nameValid;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ValidationFails that = (ValidationFails) o;
        return nameValid == that.nameValid &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, nameValid);
    }
}
