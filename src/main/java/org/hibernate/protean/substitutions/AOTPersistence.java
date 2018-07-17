package org.hibernate.protean.substitutions;


import java.util.List;
import java.util.Map;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceProviderResolver;
import javax.persistence.spi.PersistenceProviderResolverHolder;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "javax.persistence.Persistence")
public final class AOTPersistence {

	@Substitute
	public static EntityManagerFactory createEntityManagerFactory(String persistenceUnitName, Map properties) {
		System.out.println( "Replacement invoked!" );

		EntityManagerFactory emf = null;
		PersistenceProviderResolver resolver = PersistenceProviderResolverHolder.getPersistenceProviderResolver();

		List<PersistenceProvider> providers = resolver.getPersistenceProviders();
		int size = providers.size();
		System.out.println( "Providers found: " + size );

		for (PersistenceProvider provider : providers) {
			System.out.println( "Provider: " + provider.getClass().getName() );
			emf = provider.createEntityManagerFactory(persistenceUnitName, properties);
			if (emf != null) {
				break;
			}
		}
		if (emf == null) {
			throw new PersistenceException( "No Persistence provider for EntityManager named " + persistenceUnitName);
		}
		return emf;
	}
}
