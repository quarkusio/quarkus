package io.quarkus.it.spring.data.jpa;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("federalState")
public class FederalStateCatalogValue extends CatalogValue {

}
