package io.quarkus.it.spring.data.jpa;

import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.SequenceGenerator;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
public class Song {

    @Id
    @SequenceGenerator(name = "songSeqGen", sequenceName = "songSeq", initialValue = 100, allocationSize = 1)
    @GeneratedValue(generator = "songSeqGen")
    private Long id;

    private String title;

    private String author;

    @JsonIgnore
    @ManyToMany(mappedBy = "likedSongs")
    private Set<Person> likes;

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

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Set<Person> getLikes() {
        return likes;
    }

    public void setLikes(Set<Person> likes) {
        this.likes = likes;
    }

    public void removePerson(Person person) {
        this.likes.remove(person);
        person.getLikedSongs().remove(this);

    }
}
