package io.quarkus.it.jaxb;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlRootElement
@XmlType(propOrder = { "IBAN", "title" })
public class BookWithParent extends BookIBANField {
    @XmlElement
    private String title;

    public BookWithParent() {
    }

    public BookWithParent(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
