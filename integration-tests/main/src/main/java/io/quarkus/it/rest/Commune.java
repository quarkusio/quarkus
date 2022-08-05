package io.quarkus.it.rest;

import java.util.Set;

public class Commune {
    private String code;
    private String nom;
    private Set<String> codesPostaux;
    private String codeDepartement;
    private String codeRegion;
    // departement
    // region
    private Integer population;
    private Float surface;

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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public Set<String> getCodesPostaux() {
        return codesPostaux;
    }

    public void setCodesPostaux(Set<String> codesPostaux) {
        this.codesPostaux = codesPostaux;
    }

    public String getCodeDepartement() {
        return codeDepartement;
    }

    public void setCodeDepartement(String codeDepartement) {
        this.codeDepartement = codeDepartement;
    }

    public String getCodeRegion() {
        return codeRegion;
    }

    public void setCodeRegion(String codeRegion) {
        this.codeRegion = codeRegion;
    }

    public Integer getPopulation() {
        return population;
    }

    public void setPopulation(Integer population) {
        this.population = population;
    }

    public Float getSurface() {
        return surface;
    }

    public void setSurface(Float surface) {
        this.surface = surface;
    }
}