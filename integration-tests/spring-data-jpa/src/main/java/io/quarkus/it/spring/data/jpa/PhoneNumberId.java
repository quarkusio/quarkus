package io.quarkus.it.spring.data.jpa;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class PhoneNumberId implements Serializable {

    @Column(name = "area_code")
    private String areaCode;

    private String number;

    PhoneNumberId() {
    }

    public PhoneNumberId(String areaCode, String number) {
        this.areaCode = areaCode;
        this.number = number;
    }

    public String getAreaCode() {
        return areaCode;
    }

    public String getNumber() {
        return number;
    }
}
