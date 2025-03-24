package io.quarkus.hibernate.orm.runtime.recording;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.MappingException;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.internal.MetadataImpl;
import org.hibernate.boot.internal.SessionFactoryOptionsBuilder;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.NamedEntityGraphDefinition;
import org.hibernate.boot.model.TypeDefinition;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.query.NamedHqlQueryDefinition;
import org.hibernate.boot.query.NamedNativeQueryDefinition;
import org.hibernate.boot.query.NamedProcedureCallDefinition;
import org.hibernate.boot.query.NamedResultSetMappingDescriptor;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.FetchProfile;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.metamodel.mapping.DiscriminatorType;
import org.hibernate.query.named.NamedObjectRepository;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.type.Type;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * This is a Quarkus custom implementation of Metadata wrapping the original
 * implementation from Hibernate ORM.
 * The goal is to run the {@link MetadataImpl#validate()} method
 * earlier than when it is normally performed, for two main reasons: further reduce
 * the work that is still necessary when performing a runtime boot, and to be
 * able to still use reflection as it's necessary e.g. to validate enum fields.
 *
 * We also make sure that methods {@link #getSessionFactoryBuilder()} and {@link #buildSessionFactory()}
 * are unavailable, as these would normally trigger an additional validation phase:
 * we can actually boot Quarkus in a simpler way.
 */
public final class PrevalidatedQuarkusMetadata implements MetadataImplementor {

    private final MetadataImpl metadata;

    private PrevalidatedQuarkusMetadata(final MetadataImpl metadata) {
        this.metadata = metadata;
    }

    public static PrevalidatedQuarkusMetadata validateAndWrap(final MetadataImpl original) {
        original.validate();
        return new PrevalidatedQuarkusMetadata(original);
    }

    // New helpers on this Quarkus specific metadata; these are useful to boot and manage the recorded state:

    public SessionFactoryOptionsBuilder buildSessionFactoryOptionsBuilder() {
        SessionFactoryOptionsBuilder builder = new SessionFactoryOptionsBuilder(
                metadata.getMetadataBuildingOptions().getServiceRegistry(),
                metadata.getBootstrapContext());
        // This would normally be done by the constructor of SessionFactoryBuilderImpl,
        // but we don't use a builder to create the session factory, for some reason.

        Map<String, SqmFunctionDescriptor> sqlFunctions = metadata.getSqlFunctionMap();
        if (sqlFunctions != null) {
            for (Map.Entry<String, SqmFunctionDescriptor> entry : sqlFunctions.entrySet()) {
                builder.applySqlFunction(entry.getKey(), entry.getValue());
            }
        }
        return builder;
    }

    //Relevant overrides:

    @Override
    public SessionFactoryBuilder getSessionFactoryBuilder() {
        //Ensure we don't boot Hibernate using this, but rather use the #buildSessionFactoryOptionsBuilder above.
        throw new IllegalStateException("This method is not supposed to be used in Quarkus");
    }

    @Override
    public SessionFactoryImplementor buildSessionFactory() {
        //Ensure we don't boot Hibernate using this, but rather use the #buildSessionFactoryOptionsBuilder above.
        throw new IllegalStateException("This method is not supposed to be used in Quarkus");
    }

    @Override
    public void validate() throws MappingException {
        //Intentional no-op
    }

    //All other contracts from Metadata delegating:

    @Override
    public UUID getUUID() {
        return metadata.getUUID();
    }

    @Override
    public Database getDatabase() {
        return metadata.getDatabase();
    }

    @Override
    public Collection<PersistentClass> getEntityBindings() {
        return metadata.getEntityBindings();
    }

    @Override
    public PersistentClass getEntityBinding(final String entityName) {
        return metadata.getEntityBinding(entityName);
    }

    @Override
    public Collection<org.hibernate.mapping.Collection> getCollectionBindings() {
        return metadata.getCollectionBindings();
    }

    @Override
    public org.hibernate.mapping.Collection getCollectionBinding(final String role) {
        return metadata.getCollectionBinding(role);
    }

    @Override
    public Map<String, String> getImports() {
        return metadata.getImports();
    }

    @Override
    public NamedHqlQueryDefinition getNamedHqlQueryMapping(String name) {
        return metadata.getNamedHqlQueryMapping(name);
    }

    @Override
    public void visitNamedHqlQueryDefinitions(Consumer<NamedHqlQueryDefinition<?>> definitionConsumer) {
        metadata.visitNamedHqlQueryDefinitions(definitionConsumer);
    }

    @Override
    public NamedNativeQueryDefinition getNamedNativeQueryMapping(String name) {
        return metadata.getNamedNativeQueryMapping(name);
    }

    @Override
    public void visitNamedNativeQueryDefinitions(Consumer<NamedNativeQueryDefinition<?>> definitionConsumer) {
        metadata.visitNamedNativeQueryDefinitions(definitionConsumer);
    }

    @Override
    public NamedProcedureCallDefinition getNamedProcedureCallMapping(String name) {
        return metadata.getNamedProcedureCallMapping(name);
    }

    @Override
    public void visitNamedProcedureCallDefinition(Consumer<NamedProcedureCallDefinition> definitionConsumer) {
        metadata.visitNamedProcedureCallDefinition(definitionConsumer);
    }

    @Override
    public NamedResultSetMappingDescriptor getResultSetMapping(String name) {
        return metadata.getResultSetMapping(name);
    }

    @Override
    public void visitNamedResultSetMappingDefinition(Consumer<NamedResultSetMappingDescriptor> definitionConsumer) {
        metadata.visitNamedResultSetMappingDefinition(definitionConsumer);
    }

    @Override
    public TypeDefinition getTypeDefinition(final String typeName) {
        return metadata.getTypeDefinition(typeName);
    }

    @Override
    public Map<String, FilterDefinition> getFilterDefinitions() {
        return metadata.getFilterDefinitions();
    }

    @Override
    public FilterDefinition getFilterDefinition(final String name) {
        return metadata.getFilterDefinition(name);
    }

    @Override
    public FetchProfile getFetchProfile(final String name) {
        return metadata.getFetchProfile(name);
    }

    @Override
    public Collection<FetchProfile> getFetchProfiles() {
        return metadata.getFetchProfiles();
    }

    @Override
    public NamedEntityGraphDefinition getNamedEntityGraph(final String name) {
        return metadata.getNamedEntityGraph(name);
    }

    @Override
    public Map<String, NamedEntityGraphDefinition> getNamedEntityGraphs() {
        return metadata.getNamedEntityGraphs();
    }

    @Override
    public IdentifierGeneratorDefinition getIdentifierGenerator(final String name) {
        return metadata.getIdentifierGenerator(name);
    }

    @Override
    public Collection<Table> collectTableMappings() {
        return metadata.collectTableMappings();
    }

    @Override
    public Map<String, SqmFunctionDescriptor> getSqlFunctionMap() {
        return metadata.getSqlFunctionMap();
    }

    @Override
    public Set<String> getContributors() {
        return metadata.getContributors();
    }

    //All methods from org.hibernate.engine.spi.Mapping, the parent of Metadata:

    @Override
    public Type getIdentifierType(final String className) throws MappingException {
        return metadata.getIdentifierType(className);
    }

    @Override
    public String getIdentifierPropertyName(final String className) throws MappingException {
        return metadata.getIdentifierPropertyName(className);
    }

    @Override
    public Type getReferencedPropertyType(final String className, final String propertyName) throws MappingException {
        return metadata.getReferencedPropertyType(className, propertyName);
    }

    // Delegates for MetadataImplementor:

    @Override
    public MetadataBuildingOptions getMetadataBuildingOptions() {
        return metadata.getMetadataBuildingOptions();
    }

    @Override
    public TypeConfiguration getTypeConfiguration() {
        return metadata.getTypeConfiguration();
    }

    @Override
    public SqmFunctionRegistry getFunctionRegistry() {
        return metadata.getFunctionRegistry();
    }

    @Override
    public NamedObjectRepository buildNamedQueryRepository() {
        return metadata.buildNamedQueryRepository();
    }

    @Override
    public void orderColumns(boolean forceOrdering) {
        metadata.orderColumns(forceOrdering);
    }

    @Override
    public Set<MappedSuperclass> getMappedSuperclassMappingsCopy() {
        return metadata.getMappedSuperclassMappingsCopy();
    }

    @Override
    public void initSessionFactory(SessionFactoryImplementor sessionFactoryImplementor) {
        metadata.initSessionFactory(sessionFactoryImplementor);
    }

    @Override
    public void visitRegisteredComponents(Consumer<Component> consumer) {
        metadata.visitRegisteredComponents(consumer);
    }

    @Override
    public Component getGenericComponent(Class<?> componentClass) {
        return metadata.getGenericComponent(componentClass);
    }

    @Override
    public DiscriminatorType<?> resolveEmbeddableDiscriminatorType(Class<?> embeddableClass,
            Supplier<DiscriminatorType<?>> supplier) {
        return metadata.resolveEmbeddableDiscriminatorType(embeddableClass, supplier);
    }

    public Map<String, PersistentClass> getEntityBindingMap() {
        return metadata.getEntityBindingMap();
    }

    public Map<String, org.hibernate.mapping.Collection> getCollectionBindingMap() {
        return metadata.getCollectionBindingMap();
    }

    public Map<String, TypeDefinition> getTypeDefinitionMap() {
        return metadata.getTypeDefinitionMap();
    }

    public Map<String, FetchProfile> getFetchProfileMap() {
        return metadata.getFetchProfileMap();
    }

    public Map<Class<?>, MappedSuperclass> getMappedSuperclassMap() {
        return metadata.getMappedSuperclassMap();
    }

    public Map<String, IdentifierGeneratorDefinition> getIdGeneratorDefinitionMap() {
        return metadata.getIdGeneratorDefinitionMap();
    }

    public Map<String, NamedEntityGraphDefinition> getNamedEntityGraphMap() {
        return metadata.getNamedEntityGraphMap();
    }

    public BootstrapContext getBootstrapContext() {
        return metadata.getBootstrapContext();
    }

    public Map<String, NamedHqlQueryDefinition<?>> getNamedQueryMap() {
        return metadata.getNamedQueryMap();
    }

    public Map<String, NamedNativeQueryDefinition<?>> getNamedNativeQueryMap() {
        return metadata.getNamedNativeQueryMap();
    }

    public Map<String, NamedProcedureCallDefinition> getNamedProcedureCallMap() {
        return metadata.getNamedProcedureCallMap();
    }

    public Map<String, NamedResultSetMappingDescriptor> getSqlResultSetMappingMap() {
        return metadata.getSqlResultSetMappingMap();
    }

    public List<Component> getComposites() {
        return metadata.getComposites();
    }

}
