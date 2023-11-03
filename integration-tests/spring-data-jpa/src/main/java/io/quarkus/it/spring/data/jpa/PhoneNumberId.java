package io.quarkus.it.spring.data.jpa;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

// this is a bit artificial, but next to PhoneCallId there could be e.g. a PhoneBookEntryId subclass
@MappedSuperclass
public abstract class PhoneNumberId implements Serializable {

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

    @Override
    public int hashCode() {
        return Objects.hash(areaCode, number);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PhoneNumberId other = (PhoneNumberId) obj;
        return Objects.equals(areaCode, other.areaCode) && Objects.equals(number, other.number);
    }
}
