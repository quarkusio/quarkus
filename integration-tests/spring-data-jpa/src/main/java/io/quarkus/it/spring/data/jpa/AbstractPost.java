package io.quarkus.it.spring.data.jpa;

import java.time.ZonedDateTime;

import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AbstractPost {
    private ZonedDateTime posted;

    public ZonedDateTime getPosted() {
        return posted;
    }

    public void setPosted(ZonedDateTime postedAt) {
        this.posted = postedAt;
    }
}
