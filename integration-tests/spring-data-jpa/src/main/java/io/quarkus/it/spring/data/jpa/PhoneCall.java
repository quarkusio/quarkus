package io.quarkus.it.spring.data.jpa;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "phone_call")
public class PhoneCall {

    @EmbeddedId
    private PhoneNumberId id;

    private int duration;

    PhoneCall() {
    }

    public PhoneCall(PhoneNumberId id) {
        this.id = id;
    }

    public PhoneNumberId getId() {
        return id;
    }

    public int getDuration() {
        return duration;
    }
}
