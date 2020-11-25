package io.quarkus.reactivemessaging.http.sink.app;

public class Dto {
    String field;

    public Dto(String value) {
        field = value;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    @Override
    public String toString() {
        return "Dto{" +
                "field='" + field + '\'' +
                '}';
    }
}