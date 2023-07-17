package io.quarkus.it.spring.data.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
public class CatalogValue extends AbstractEntity {

    @Column(name = "key_")
    private String key;
    private String displayName;

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }
}
