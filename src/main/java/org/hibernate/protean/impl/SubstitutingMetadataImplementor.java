package org.hibernate.protean.impl;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.TypeDefinition;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.annotations.NamedEntityGraphDefinition;
import org.hibernate.cfg.annotations.NamedProcedureCallDefinition;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.mapping.FetchProfile;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.query.spi.NamedQueryRepository;
import org.hibernate.type.Type;
import org.hibernate.type.TypeResolver;
import org.hibernate.type.spi.TypeConfiguration;

public class SubstitutingMetadataImplementor implements MetadataImplementor {

	public SubstitutingMetadataImplementor(MetadataImplementor fullMeta) {
		fullMeta.validate();
	}

	@Override
	public MetadataBuildingOptions getMetadataBuildingOptions() {
		return null;
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return null;
	}

	@Override
	public TypeResolver getTypeResolver() {
		return null;
	}

	@Override
	public NamedQueryRepository buildNamedQueryRepository(SessionFactoryImpl sessionFactory) {
		return null;
	}

	@Override
	public void validate() throws MappingException {

	}

	@Override
	public Set<MappedSuperclass> getMappedSuperclassMappingsCopy() {
		return null;
	}

	@Override
	public SessionFactoryBuilder getSessionFactoryBuilder() {
		return null;
	}

	@Override
	public SessionFactory buildSessionFactory() {
		return null;
	}

	@Override
	public UUID getUUID() {
		return null;
	}

	@Override
	public Database getDatabase() {
		return null;
	}

	@Override
	public Collection<PersistentClass> getEntityBindings() {
		return null;
	}

	@Override
	public PersistentClass getEntityBinding(String entityName) {
		return null;
	}

	@Override
	public Collection<org.hibernate.mapping.Collection> getCollectionBindings() {
		return null;
	}

	@Override
	public org.hibernate.mapping.Collection getCollectionBinding(String role) {
		return null;
	}

	@Override
	public Map<String, String> getImports() {
		return null;
	}

	@Override
	public NamedQueryDefinition getNamedQueryDefinition(String name) {
		return null;
	}

	@Override
	public Collection<NamedQueryDefinition> getNamedQueryDefinitions() {
		return null;
	}

	@Override
	public NamedSQLQueryDefinition getNamedNativeQueryDefinition(String name) {
		return null;
	}

	@Override
	public Collection<NamedSQLQueryDefinition> getNamedNativeQueryDefinitions() {
		return null;
	}

	@Override
	public Collection<NamedProcedureCallDefinition> getNamedProcedureCallDefinitions() {
		return null;
	}

	@Override
	public ResultSetMappingDefinition getResultSetMapping(String name) {
		return null;
	}

	@Override
	public Map<String, ResultSetMappingDefinition> getResultSetMappingDefinitions() {
		return null;
	}

	@Override
	public TypeDefinition getTypeDefinition(String typeName) {
		return null;
	}

	@Override
	public Map<String, FilterDefinition> getFilterDefinitions() {
		return null;
	}

	@Override
	public FilterDefinition getFilterDefinition(String name) {
		return null;
	}

	@Override
	public FetchProfile getFetchProfile(String name) {
		return null;
	}

	@Override
	public Collection<FetchProfile> getFetchProfiles() {
		return null;
	}

	@Override
	public NamedEntityGraphDefinition getNamedEntityGraph(String name) {
		return null;
	}

	@Override
	public Map<String, NamedEntityGraphDefinition> getNamedEntityGraphs() {
		return null;
	}

	@Override
	public IdentifierGeneratorDefinition getIdentifierGenerator(String name) {
		return null;
	}

	@Override
	public Collection<Table> collectTableMappings() {
		return null;
	}

	@Override
	public Map<String, SQLFunction> getSqlFunctionMap() {
		return null;
	}

	@Override
	public IdentifierGeneratorFactory getIdentifierGeneratorFactory() {
		return null;
	}

	@Override
	public Type getIdentifierType(String className) throws MappingException {
		return null;
	}

	@Override
	public String getIdentifierPropertyName(String className) throws MappingException {
		return null;
	}

	@Override
	public Type getReferencedPropertyType(String className, String propertyName) throws MappingException {
		return null;
	}
}
