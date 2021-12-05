package io.quarkus.it.panache.reactive;

import static io.quarkus.hibernate.reactive.panache.runtime.JpaOperations.INSTANCE;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.quarkus.hibernate.reactive.panache.PanacheQuery;
import io.smallrye.mutiny.Uni;

@XmlRootElement
@Entity(name = "Person2")
@NamedQueries({
        @NamedQuery(name = "Person.getByName", query = "from Person2 where name = :name"),
        @NamedQuery(name = "Person.countAll", query = "select count(*) from Person2"),
        @NamedQuery(name = "Person.countByName", query = "select count(*) from Person2 where name = :name"),
        @NamedQuery(name = "Person.countByName.ordinal", query = "select count(*) from Person2 where name = ?1"),
        @NamedQuery(name = "Person.updateAllNames", query = "Update Person2 p set p.name = :name"),
        @NamedQuery(name = "Person.updateNameById", query = "Update Person2 p set p.name = :name where p.id = :id"),
        @NamedQuery(name = "Person.updateNameById.ordinal", query = "Update Person2 p set p.name = ?1 where p.id = ?2"),
        @NamedQuery(name = "Person.deleteAll", query = "delete from Person2"),
        @NamedQuery(name = "Person.deleteById", query = "delete from Person2 p where p.id = :id"),
        @NamedQuery(name = "Person.deleteById.ordinal", query = "delete from Person2 p where p.id = ?1"),
})
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

    public static Uni<List<Person>> findOrdered() {
        return find("ORDER BY name").list();
    }

    // For https://github.com/quarkusio/quarkus/issues/9635
    public static <T extends PanacheEntityBase> PanacheQuery<T> find(String query, Object... params) {
        return (PanacheQuery<T>) INSTANCE.find(Person.class, query, params);
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
