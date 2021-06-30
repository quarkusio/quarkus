package io.quarkus.it.spring.data.jpa;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
public class CatalogValue extends AbstractEntity {

    private String key;
    private String displayName;

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }
}
