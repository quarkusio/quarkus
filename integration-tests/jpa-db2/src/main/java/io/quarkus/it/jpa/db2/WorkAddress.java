package io.quarkus.it.jpa.db2;

import javax.persistence.Embeddable;

/**
 * Class marked @Embeddable explicitly so it is picked up.
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
@Embeddable
public class WorkAddress {
    private String company;

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }
}
