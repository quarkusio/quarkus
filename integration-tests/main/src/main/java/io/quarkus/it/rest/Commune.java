package io.quarkus.it.rest;

import java.util.Set;

public class Commune {
    public String code;
    public String nom;
    public Set<String> codesPostaux;
    public String codeDepartement;
    public String codeRegion;
    // departement
    // region
    public Integer population;
    public Float surface;

    public Commune() {
    }

    public Commune(String nom, String code, String codeDepartement, String codeRegion, Set<String> codePostaux,
            int population) {
        this.nom = nom;
        this.code = code;
        this.codeDepartement = codeDepartement;
        this.codeRegion = codeRegion;
        this.codesPostaux = codePostaux;
        this.population = population;
    }
}