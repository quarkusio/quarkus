package io.quarkus.it.hibernate.orm.rest.data.panache.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

@Entity
@Table(name = "review")
public class ReviewEntity extends PanacheEntityBase {

    @Id
    public String id;

    public String text;

    @ManyToOne(optional = false)
    public BookEntity book;

    public static ReviewEntity create(String id, String text) {
        ReviewEntity review = new ReviewEntity();
        review.id = id;
        review.text = text;

        return review;
    }

    // Otherwise serialization will cause infinite loop.
    // There seems to be a bug that requires this annotation to be on a getter.
    @JsonIgnore
    public BookEntity getBook() {
        return book;
    }
}
