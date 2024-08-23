package io.quarkus.it.opentelemetry.model;

public interface Hit {

    Long getId();

    String getMessage();

    void setId(Long id);

    void setMessage(String message);

    void persist();

}
