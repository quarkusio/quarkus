package io.quarkus.it.jaxb;

import java.awt.Image;

import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Book {

    private String title;

    private Image cover;

    public Book() {
    }

    public Book(String title, Image cover) {
        this.title = title;
        this.cover = cover;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Image getCover() {
        return cover;
    }

    public void setCover(Image cover) {
        this.cover = cover;
    }
}
