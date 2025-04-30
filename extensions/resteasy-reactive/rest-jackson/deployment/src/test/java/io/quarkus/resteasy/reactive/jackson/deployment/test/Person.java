package io.quarkus.resteasy.reactive.jackson.deployment.test;

import jakarta.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonView;

import io.quarkus.resteasy.reactive.jackson.SecureField;

public class Person {

    private String first;

    @NotBlank(message = "Title cannot be blank")
    @SecureField(rolesAllowed = "admin")
    private String last;

    @JsonView(Views.Private.class)
    public int id = 0;

    @SecureField(rolesAllowed = { "${admin-expression:disabled}", "${user-expression:disabled}" })
    private String address;

    @SecureField(rolesAllowed = "${birth-date-roles:disabled}")
    private String birthDate;

    public String getFirst() {
        return first;
    }

    public void setFirst(String first) {
        this.first = first;
    }

    public String getLast() {
        return last;
    }

    public void setLast(String last) {
        this.last = last;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }
}
