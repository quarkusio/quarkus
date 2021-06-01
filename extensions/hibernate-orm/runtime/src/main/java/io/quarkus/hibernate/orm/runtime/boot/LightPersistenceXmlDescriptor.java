package io.quarkus.hibernate.orm.runtime.boot;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;

public final class LightPersistenceXmlDescriptor implements PersistenceUnitDescriptor {

    private final String name;
    private final String providerClassName;
    private final boolean useQuotedIdentifiers;
    private final PersistenceUnitTransactionType transactionType;
    private final ValidationMode validationMode;
    private final SharedCacheMode sharedCachemode;
    private final List<String> managedClassNames;
    private final Properties properties;

    /**
     * Internal constructor, as we're trusting all parameters. Useful for serialization to bytecode.
     * (intentionally set to package-private visibility)
     */
    LightPersistenceXmlDescriptor(String name, String providerClassName, boolean useQuotedIdentifiers,
            PersistenceUnitTransactionType transactionType,
            ValidationMode validationMode, SharedCacheMode sharedCachemode, List<String> managedClassNames,
            Properties properties) {
        this.name = name;
        this.providerClassName = providerClassName;
        this.useQuotedIdentifiers = useQuotedIdentifiers;
        this.transactionType = transactionType;
        this.validationMode = validationMode;
        this.sharedCachemode = sharedCachemode;
        this.managedClassNames = managedClassNames;
        this.properties = properties;
    }

    /**
     * Converts a generic PersistenceUnitDescriptor into one of this specific type, and validates that
     * several options that Quarkus does not support are not set.
     * 
     * @param toClone the descriptor to clone
     * @return a new instance of LightPersistenceXmlDescriptor
     * @throws UnsupportedOperationException on unsupported configurations
     */
    public static LightPersistenceXmlDescriptor validateAndReadFrom(PersistenceUnitDescriptor toClone) {
        Objects.requireNonNull(toClone);
        verifyIgnoredFields(toClone);
        return new LightPersistenceXmlDescriptor(toClone.getName(), toClone.getProviderClassName(),
                toClone.isUseQuotedIdentifiers(),
                toClone.getTransactionType(), toClone.getValidationMode(), toClone.getSharedCacheMode(),
                Collections.unmodifiableList(toClone.getManagedClassNames()), toClone.getProperties());
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
        // enforced
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
        // Mapping files can safely be ignored, see verifyIgnoredFields().
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
        // TODO: we should include the name of the datasource
        return null;
    }

    @Override
    public Properties getProperties() {
        return properties;
    }

    @Override
    public ClassLoader getClassLoader() {
        // enforced
        return null;
    }

    @Override
    public ClassLoader getTempClassLoader() {
        // enforced
        return null;
    }

    @Override
    public void pushClassTransformer(final EnhancementContext enhancementContext) {
        // has never been supported
    }

    private static void verifyIgnoredFields(final PersistenceUnitDescriptor toClone) {
        // This one needs to be ignored:
        // if ( toClone.getPersistenceUnitRootUrl() != null ) {
        // throw new UnsupportedOperationException( "Value found for
        // #getPersistenceUnitRootUrl : not supported yet" );
        // }

        // getMappingFiles() is ignored and replaced with an empty list,
        // because we don't need Hibernate ORM to parse the mappings files:
        // they are parsed at compile time and side-loaded during static init.

        if (toClone.getJarFileUrls() != null && !toClone.getJarFileUrls().isEmpty()) {
            throw new UnsupportedOperationException("Value found for #getJarFileUrls : not supported yet");
        }
        if (toClone.getJtaDataSource() != null) {
            throw new UnsupportedOperationException("Value found for #getJtaDataSource : not supported yet");
        }
        if (toClone.getNonJtaDataSource() != null) {
            throw new UnsupportedOperationException("Value found for #getNonJtaDataSource : not supported");
        }
    }

    @Override
    public String toString() {
        return "PersistenceUnitDescriptor{" + "name='" + name + '\'' + ", providerClassName='" + providerClassName
                + '\'' + ", useQuotedIdentifiers=" + useQuotedIdentifiers + ", transactionType=" + transactionType
                + ", validationMode=" + validationMode + ", sharedCachemode=" + sharedCachemode + ", managedClassNames="
                + managedClassNames + ", properties=" + properties + '}';
    }
}
