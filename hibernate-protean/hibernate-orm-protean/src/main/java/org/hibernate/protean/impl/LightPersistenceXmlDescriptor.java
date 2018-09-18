package org.hibernate.protean.impl;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;

final class LightPersistenceXmlDescriptor implements PersistenceUnitDescriptor {

	private final String name;
	private final String providerClassName;
	private final boolean useQuotedIdentifiers;
	private final PersistenceUnitTransactionType transactionType;
	private final ValidationMode validationMode;
	private final SharedCacheMode sharedCachemode;
	private final List<String> managedClassNames;
	private final Properties properties;

	LightPersistenceXmlDescriptor(final PersistenceUnitDescriptor toClone) {
		this.name = toClone.getName();
		this.providerClassName = toClone.getProviderClassName();
		this.useQuotedIdentifiers = toClone.isUseQuotedIdentifiers();
		this.transactionType = toClone.getTransactionType();
		this.validationMode = toClone.getValidationMode();
		this.sharedCachemode = toClone.getSharedCacheMode();
		this.managedClassNames = Collections.unmodifiableList( toClone.getManagedClassNames() );
		this.properties = filterNonStrings( toClone.getProperties() );
		verifyIgnoredFields( toClone );
	}

	private static void verifyIgnoredFields(final PersistenceUnitDescriptor toClone) {
		if ( toClone.getNonJtaDataSource() != null ) {
			throw new UnsupportedOperationException( "Value found for #getNonJtaDataSource : not supported yet" );
		}
		if ( toClone.getJtaDataSource() != null ) {
			throw new UnsupportedOperationException( "Value found for #getJtaDataSource : not supported yet" );
		}
		//This one needs to be ignored:
		//		if ( toClone.getPersistenceUnitRootUrl() != null ) {
		//			throw new UnsupportedOperationException( "Value found for #getPersistenceUnitRootUrl : not supported yet" );
		//		}
		if ( toClone.getMappingFileNames() != null && ! toClone.getMappingFileNames().isEmpty()) {
			throw new UnsupportedOperationException( "Value found for #getMappingFileNames : not supported yet" );
		}
		if ( toClone.getJarFileUrls() != null && ! toClone.getJarFileUrls().isEmpty() ) {
			throw new UnsupportedOperationException( "Value found for #getJarFileUrls : not supported yet" );
		}
	}

	private static final Properties filterNonStrings(final Properties properties) {
		Properties clean = new Properties();
		final Set<Map.Entry<Object, Object>> entries = properties.entrySet();
		for ( Map.Entry<Object, Object> e : entries ) {
			final Object key = e.getKey();
			if ( ! ( key instanceof String ) ) {
				//TODO warn
				continue;
			}
			final Object value = e.getValue();
			if ( ! ( value instanceof String ) ) {
				//TODO warn
				continue;
			}
			clean.setProperty( (String)key, (String)value );
		}
		return clean;
	}

	@Override
	public URL getPersistenceUnitRootUrl() {
		return null;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getProviderClassName() {
		return providerClassName;
	}

	@Override
	public boolean isUseQuotedIdentifiers() {
		return useQuotedIdentifiers;
	}

	@Override
	public boolean isExcludeUnlistedClasses() {
		//enforced
		return true;
	}

	@Override
	public PersistenceUnitTransactionType getTransactionType() {
		return transactionType;
	}

	@Override
	public ValidationMode getValidationMode() {
		return validationMode;
	}

	@Override
	public SharedCacheMode getSharedCacheMode() {
		return sharedCachemode;
	}

	@Override
	public List<String> getManagedClassNames() {
		return managedClassNames;
	}

	@Override
	public List<String> getMappingFileNames() {
		return Collections.emptyList();
	}

	@Override
	public List<URL> getJarFileUrls() {
		return Collections.emptyList();
	}

	@Override
	public Object getNonJtaDataSource() {
		return null;
	}

	@Override
	public Object getJtaDataSource() {
		return null;
	}

	@Override
	public Properties getProperties() {
		return properties;
	}

	@Override
	public ClassLoader getClassLoader() {
		//enforced
		return null;
	}

	@Override
	public ClassLoader getTempClassLoader() {
		//enforced
		return null;
	}

	@Override
	public void pushClassTransformer(final EnhancementContext enhancementContext) {
		//has never been supported
	}
}
