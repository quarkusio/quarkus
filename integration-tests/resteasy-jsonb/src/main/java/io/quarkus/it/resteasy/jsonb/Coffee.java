package io.quarkus.it.resteasy.jsonb;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import javax.json.bind.annotation.JsonbNillable;
import javax.json.bind.annotation.JsonbNumberFormat;
import javax.json.bind.annotation.JsonbProperty;

@JsonbNillable // allow null values be default
public class Coffee {

    // used to test both the name and the formatting
    // also because of the upper-case name, this will be the first value in the json output
    @JsonbProperty("ID")
    @JsonbNumberFormat(value = "#,#00.0#;(#,#00.0#)", locale = "en_US")
    private Integer id;

    private String name;

    // used to show that nillable extends to empty optional as well
    @JsonbProperty(value = "other-name", nillable = false)
    private Optional<String> otherName = Optional.empty();

    private Country countryOfOrigin;

    // used to test that primitives use their default value and that public fields with no getters are added
    public boolean enabled;

    @JsonbProperty(nillable = false)
    private Collection<Seller> sellers;

    @JsonbProperty("similar")
    public Map<String, Integer> similarCoffees;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Optional<String> getOtherName() {
        return otherName;
    }

    public void setOtherName(Optional<String> otherName) {
        this.otherName = otherName;
    }

    // used to show that @JsonbProperty can be added to the getters as well
    @JsonbProperty(value = "origin", nillable = true)
    public Country getCountryOfOrigin() {
        return countryOfOrigin;
    }

    public void setCountryOfOrigin(Country countryOfOrigin) {
        this.countryOfOrigin = countryOfOrigin;
    }

    public Collection<Seller> getSellers() {
        return sellers;
    }

    public void setSellers(Collection<Seller> sellers) {
        this.sellers = sellers;
    }

    public Map<String, Integer> getSimilarCoffees() {
        return similarCoffees;
    }

    public void setSimilarCoffees(Map<String, Integer> similarCoffees) {
        this.similarCoffees = similarCoffees;
    }

    // used to verify that null fields don't end up in the output when the value is not nillable
    @JsonbProperty(nillable = false)
    public String getDummyNullValue() {
        return null;
    }

    // used to verify that this will end up the json output since the class is annotated with @JsonNillable
    public Long getNullLongValue() {
        return null;
    }
}
