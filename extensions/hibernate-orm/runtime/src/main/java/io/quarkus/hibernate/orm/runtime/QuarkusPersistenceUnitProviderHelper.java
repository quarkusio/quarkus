package io.quarkus.hibernate.orm.runtime;

public interface QuarkusPersistenceUnitProviderHelper {

    boolean isActive(String persistenceUnitName);

}
