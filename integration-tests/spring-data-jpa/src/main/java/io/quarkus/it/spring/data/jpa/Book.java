package io.quarkus.it.spring.data.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Book extends NamedEntity {

    private int bid;

    private Integer publicationYear;

    public Book() {
    }

    public Book(Integer bid, String name, Integer publicationYear) {
        super(name);
        this.bid = bid;
        this.publicationYear = publicationYear;
    }

    @Id
    public int getBid() {
        return bid;
    }

    public void setBid(int bid) {
        this.bid = bid;
    }

    public Integer getPublicationYear() {
        return publicationYear;
    }

    public void setPublicationYear(Integer publicationYear) {
        this.publicationYear = publicationYear;
    }
}
