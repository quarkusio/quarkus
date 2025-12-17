package io.quarkus.it.jaxb;

import java.awt.Image;

import jakarta.xml.bind.annotation.XmlRootElement;

public class Book {

    private String title;
    private String cover;

    public Book() {
    }

    public Book(String title, String cover) {
        this.title = title;
        this.cover = cover;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    @XmlRootElement
    public static class Cover {
        private Image image;

        public Cover() {
        }

        public Cover(Image image) {
            this.image = image;
        }

        public Image getImage() {
            return image;
        }

        public void setImage(Image image) {
            this.image = image;
        }
    }
}
