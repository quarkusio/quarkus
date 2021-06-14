package io.quarkus.it.jpa.postgresql;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class SequencedAddress {

    private long id;
    private String street;

    public SequencedAddress() {
    }

    public SequencedAddress(String street) {
        this.street = street;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "addressSeq")
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String name) {
        this.street = name;
    }

    public void describeFully(StringBuilder sb) {
        sb.append("Address with id=").append(id).append(", street='").append(street).append("'");
    }
}
