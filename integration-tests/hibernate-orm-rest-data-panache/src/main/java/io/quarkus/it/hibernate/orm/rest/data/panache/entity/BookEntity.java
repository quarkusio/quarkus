package io.quarkus.it.hibernate.orm.rest.data.panache.entity;

import java.util.LinkedList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@Table(name = "book")
public class BookEntity extends PanacheEntity {

    public String title;

    @ManyToOne
    public AuthorEntity author;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "book")
    public List<ReviewEntity> reviews = new LinkedList<>();

    public static BookEntity create(Long id, String title, AuthorEntity author) {
        BookEntity book = new BookEntity();
        book.id = id;
        book.title = title;
        book.author = author;

        return book;
    }

    public static BookEntity create(Long id, String title, AuthorEntity author, List<ReviewEntity> reviews) {
        BookEntity book = new BookEntity();
        book.id = id;
        book.title = title;
        book.author = author;
        book.reviews = reviews;

        return book;
    }
}
