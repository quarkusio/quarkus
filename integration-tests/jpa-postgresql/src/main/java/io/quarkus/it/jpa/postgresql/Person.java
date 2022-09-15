package io.quarkus.it.jpa.postgresql;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity
@Table(schema = "myschema")
@NamedQuery(name = "get_person_by_name", query = "select p from Person p where name = :name")
public class Person {

    private long id;
    private String name;
    private SequencedAddress address;
    private Status status;

    public Person() {
    }

    public Person(long id, String name, SequencedAddress address) {
        this.id = id;
        this.name = name;
        this.address = address;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "personSeq")
    public long getId() {
        return id;
    }

    public void setId(long id) {
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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void describeFully(StringBuilder sb) {
        sb.append("Person with id=").append(id).append(", name='").append(name).append("', status='").append(status)
                .append("', address { ");
        getAddress().describeFully(sb);
        sb.append(" }");
    }
}
