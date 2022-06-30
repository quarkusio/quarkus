package io.quarkus.it.spring.data.jpa;

import javax.persistence.Embeddable;

@Embeddable
public class PhoneCallId extends PhoneNumberId {

    PhoneCallId() {
    }

    public PhoneCallId(String areaCode, String number) {
        super(areaCode, number);
    }
}
