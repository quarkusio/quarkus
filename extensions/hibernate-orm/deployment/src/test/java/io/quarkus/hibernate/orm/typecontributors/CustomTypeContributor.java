package io.quarkus.hibernate.orm.typecontributors;

import jakarta.enterprise.context.ApplicationScoped;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.service.ServiceRegistry;

import io.quarkus.hibernate.orm.PersistenceUnitExtension;

@ApplicationScoped
@PersistenceUnitExtension
public class CustomTypeContributor implements TypeContributor {
    @Override
    public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        typeContributions.getTypeConfiguration()
                .getBasicTypeRegistry()
                .register(new BooleanYesNoType(), "boolean_yes_no");
    }
}
