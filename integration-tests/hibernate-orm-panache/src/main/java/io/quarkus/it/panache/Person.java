package io.quarkus.it.panache;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@XmlRootElement
@Entity(name = "Person2")
@NamedQuery(name = "Person.getByName", query = "from Person2 where name = :name")
public class Person extends PanacheEntity {

    public String name;
    @Column(unique = true)
    public String uniqueName;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    public Address address;

    public Status status;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    public List<Dog> dogs = new ArrayList<>();

    // note that this annotation is automatically added for mapped fields, which is not the case here
    // so we do it manually to emulate a mapped field situation
    @XmlTransient
    @Transient
    public int serialisationTrick;

    public static List<Person> findOrdered() {
        return find("ORDER BY name").list();
    }

    // For JAXB: both getter and setter are required
    // Here we make sure the field is not used by Hibernate, but the accessor is used by jaxb, jsonb and jackson
    @JsonProperty
    public int getSerialisationTrick() {
        return ++serialisationTrick;
    }

    public void setSerialisationTrick(int serialisationTrick) {
        this.serialisationTrick = serialisationTrick;
    }

    public static long methodWithPrimitiveParams(boolean b, byte bb, short s, int i, long l, float f, double d, char c) {
        return 0;
    }
}
