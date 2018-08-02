package org.jboss.shamrock.example.jpa;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
@Entity
public class Customer {
    @Id
    // no getter explicitly to test field only reflective access
    private Long id;

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
