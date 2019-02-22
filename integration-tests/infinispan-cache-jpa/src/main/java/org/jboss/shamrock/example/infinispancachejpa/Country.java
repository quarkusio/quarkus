package io.quarkus.example.infinispancachejpa;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;

@Entity
@Cacheable
@NaturalIdCache
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
public class Country {

    @Id
    @GeneratedValue
    private Integer id;
    private String name;
    @NaturalId
    private String callingCode;

    public Country() {
    }

    public Country(String name, String callingCode) {
        this.name = name;
        this.callingCode = callingCode;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getCallingCode() {
        return callingCode;
    }

}
