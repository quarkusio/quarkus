package io.quarkus.it.spring.data.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

@Entity
public class Country extends NamedEntity {

    @Id
    @SequenceGenerator(name = "countrySeqGen", sequenceName = "countrySeq", initialValue = 4, allocationSize = 1)
    @GeneratedValue(generator = "countrySeqGen")
    public Long id;

    private String iso3;

    public Country() {
    }

    public Country(String name, String iso3) {
        super(name);
        this.iso3 = iso3;
    }

    public Long getId() {
        return id;
    }

    public String getIso3() {
        return iso3;
    }

    public void setIso3(String iso3) {
        this.iso3 = iso3;
    }
}
