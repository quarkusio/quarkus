package io.quarkus.it.hibernate.orm.rest.data.panache.common;

public class ReviewDto {

    public final String id;

    public final String text;

    public ReviewDto(String id, String text) {
        this.id = id;
        this.text = text;
    }
}
