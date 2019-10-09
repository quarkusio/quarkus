package io.quarkus.it.spring.data.jpa;

import javax.persistence.Entity;

@Entity
public class Movie extends MovieSuperclass {

    private String title;
    private String rating;
    private int duration;

    public Movie() {
    }

    public Movie(String title, String rating, int duration) {
        this.title = title;
        this.rating = rating;
        this.duration = duration;
    }

    public String getTitle() {
        return title;
    }

    public String getRating() {
        return rating;
    }

    public int getDuration() {
        return duration;
    }
}
