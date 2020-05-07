package io.quarkus.it.panache.rest.repository;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "review")
public class ReviewPojo {

    @Id
    private String id;

    private String text;

    @ManyToOne(optional = false)
    private BookPojo book;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @JsonIgnore
    public BookPojo getBook() {
        return book;
    }

    public void setBook(BookPojo book) {
        this.book = book;
    }
}
