package io.quarkus.it.jpa.db2;

import javax.persistence.Embeddable;

/**
 * This is an enmarked @Embeddable class.
 * Let's see if just being referenced by the main entity is enough to be detected.
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
//FIXME : this used to be non-annotated explicitly for testing purposes
// added the annotation as it's illegal according to the ORM metadata validation
@Embeddable
public class Address {
    private String street1;
    private String street2;
    private String zipCode;

    public String getStreet1() {
        return street1;
    }

    public void setStreet1(String street1) {
        this.street1 = street1;
    }

    public String getStreet2() {
        return street2;
    }

    public void setStreet2(String street2) {
        this.street2 = street2;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }
}
