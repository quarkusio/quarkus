package io.quarkus.example.jpah2;

import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;

import org.hibernate.annotations.GenericGenerator;

@Entity
@NamedQuery(name = "get_person_by_name", query = "select p from Person p where name = :name")
public class Person {

    private UUID id;
    private String name;
    private SequencedAddress address;

    public Person() {
    }

    public Person(UUID id, String name, SequencedAddress address) {
        this.id = id;
        this.name = name;
        this.address = address;
    }

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    public SequencedAddress getAddress() {
        return address;
    }

    public void setAddress(SequencedAddress address) {
        this.address = address;
    }

    public void describeFully(StringBuilder sb) {
        sb.append("Person with id=").append(id).append(", name='").append(name).append("', address { ");
        getAddress().describeFully(sb);
        sb.append(" }");
    }
}
