package io.quarkus.it.spring.data.jpa;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Book {

    private Integer bid;

    private String name;
    private Integer publicationYear;

    public Book() {
    }

    public Book(Integer bid, String name, Integer publicationYear) {
        this.bid = bid;
        this.name = name;
        this.publicationYear = publicationYear;
    }

    @Id
    public Integer getBid() {
        return bid;
    }

    public void setBid(Integer bid) {
        this.bid = bid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getPublicationYear() {
        return publicationYear;
    }

    public void setPublicationYear(Integer publicationYear) {
        this.publicationYear = publicationYear;
    }
}
