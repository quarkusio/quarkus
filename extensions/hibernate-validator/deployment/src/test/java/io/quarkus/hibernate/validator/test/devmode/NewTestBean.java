package io.quarkus.hibernate.validator.test.devmode;

import jakarta.validation.constraints.NotNull;

public class NewTestBean {
    @NotNull(message = "My new bean message")
    public String name;
}
