package io.quarkus.it.spring.data.jpa;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("federalState")
public class FederalStateCatalogValue extends CatalogValue {

}
