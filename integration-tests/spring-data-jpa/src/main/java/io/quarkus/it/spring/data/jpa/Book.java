package io.quarkus.it.spring.data.jpa;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Book extends NamedEntity {

    private Integer bid;

    private Integer publicationYear;

    public Book() {
    }

    public Book(Integer bid, String name, Integer publicationYear) {
        super(name);
        this.bid = bid;
        this.publicationYear = publicationYear;
    }

    @Id
    public Integer getBid() {
        return bid;
    }

    public void setBid(Integer bid) {
        this.bid = bid;
    }

    public Integer getPublicationYear() {
        return publicationYear;
    }

    public void setPublicationYear(Integer publicationYear) {
        this.publicationYear = publicationYear;
    }
}
