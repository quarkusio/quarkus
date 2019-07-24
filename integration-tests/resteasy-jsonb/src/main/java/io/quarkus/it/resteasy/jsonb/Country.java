package io.quarkus.it.resteasy.jsonb;

import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;

@JsonbPropertyOrder({ "iso3" }) // used to test that the ordering works properly when
public class Country {

    private final int id;
    private final String name;
    @JsonbProperty("iso")
    private final String iso3;

    public Country(int id, String name, String iso3) {
        this.id = id;
        this.name = name;
        this.iso3 = iso3;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getIso3() {
        return iso3;
    }
}
