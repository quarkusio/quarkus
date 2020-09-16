package io.quarkus.it.panache;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.runtime.JpaOperations;

@XmlRootElement
@Entity(name = "Person2")
@NamedQuery(name = "Person.getByName", query = "from Person2 where name = :name")
@FilterDef(name = "Person.hasName", defaultCondition = "name = :name", parameters = @ParamDef(name = "name", type = "string"))
@FilterDef(name = "Person.isAlive", defaultCondition = "status = 'LIVING'")
@Filter(name = "Person.isAlive")
@Filter(name = "Person.hasName")
public class Person extends PanacheEntity {

    public String name;
    @Column(unique = true)
    public String uniqueName;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    public Address address;

    @Enumerated(EnumType.STRING)
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

    // For https://github.com/quarkusio/quarkus/issues/9635
    public static <T extends PanacheEntityBase> PanacheQuery<T> find(String query, Object... params) {
        return (PanacheQuery<T>) JpaOperations.find(Person.class, query, params);
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

    public static void voidMethod() {
        throw new RuntimeException("void");
    }
}
