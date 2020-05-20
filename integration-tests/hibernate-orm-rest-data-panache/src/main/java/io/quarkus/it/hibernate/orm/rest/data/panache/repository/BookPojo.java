package io.quarkus.it.hibernate.orm.rest.data.panache.repository;

import java.util.LinkedList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "book")
public class BookPojo {

    @Id
    @GeneratedValue
    private Long id;

    private String title;

    @ManyToOne
    private AuthorPojo author;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "book")
    private List<ReviewPojo> reviews = new LinkedList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public AuthorPojo getAuthor() {
        return author;
    }

    public void setAuthor(AuthorPojo author) {
        this.author = author;
    }

    public List<ReviewPojo> getReviews() {
        return reviews;
    }

    public void setReviews(List<ReviewPojo> reviews) {
        this.reviews = reviews;
    }
}
