package io.quarkus.it.resteasy.jsonb;

public class Seller {

    private final String name;
    private final Country country;

    public Seller(String name, Country country) {
        this.name = name;
        this.country = country;
    }

    public String getName() {
        return name;
    }

    public Country getCountry() {
        return country;
    }
}
