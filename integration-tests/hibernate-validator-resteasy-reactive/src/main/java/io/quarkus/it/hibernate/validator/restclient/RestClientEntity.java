package io.quarkus.it.hibernate.validator.restclient;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;

public class RestClientEntity {
    @Max(value = 5, groups = ErrorGroup.class)
    public int number;
    @NotNull
    public String string;

    public RestClientEntity() {
    }

    public RestClientEntity(int number, String string) {
        this.number = number;
        this.string = string;
    }

    @Override
    public String toString() {
        return "RestClientEntity{" +
                "number=" + number +
                ", string='" + string + '\'' +
                '}';
    }

    public interface ErrorGroup {
    }
}
