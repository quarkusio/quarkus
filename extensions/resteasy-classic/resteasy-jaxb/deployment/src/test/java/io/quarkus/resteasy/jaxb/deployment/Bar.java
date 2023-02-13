package io.quarkus.resteasy.jaxb.deployment;

import java.util.Objects;

import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Bar {

    public String name;
    public String description;

    public Bar() {
    }

    public Bar(String name, String description) {
        this.name = name;
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Bar bar = (Bar) o;
        return Objects.equals(name, bar.name) &&
                Objects.equals(description, bar.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description);
    }
}
