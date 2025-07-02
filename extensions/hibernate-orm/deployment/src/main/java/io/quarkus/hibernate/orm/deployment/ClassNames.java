package io.quarkus.hibernate.orm.deployment;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.jandex.DotName;

public final class ClassNames {
    static final Set<DotName> CREATED_CONSTANTS = new HashSet<>();

    private ClassNames() {
    }

    private static DotName createConstant(String fqcn) {
        DotName result = DotName.createSimple(fqcn);
        CREATED_CONSTANTS.add(result);
        return result;
    }

    public static final DotName ENUM = createConstant("java.lang.Enum");

    public static final DotName TENANT_CONNECTION_RESOLVER = createConstant(
            "io.quarkus.hibernate.orm.runtime.tenant.TenantConnectionResolver");
    public static final DotName TENANT_RESOLVER = createConstant("io.quarkus.hibernate.orm.runtime.tenant.TenantResolver");

    public static final DotName STATIC_METAMODEL = createConstant("jakarta.persistence.metamodel.StaticMetamodel");

    public static final DotName QUARKUS_PERSISTENCE_UNIT = createConstant("io.quarkus.hibernate.orm.PersistenceUnit");
    public static final DotName QUARKUS_PERSISTENCE_UNIT_REPEATABLE_CONTAINER = createConstant(
            "io.quarkus.hibernate.orm.PersistenceUnit$List");
    public static final DotName JPA_PERSISTENCE_UNIT = createConstant("jakarta.persistence.PersistenceUnit");
    public static final DotName JPA_PERSISTENCE_CONTEXT = createConstant("jakarta.persistence.PersistenceContext");
    public static final DotName PERSISTENCE_UNIT_EXTENSION = createConstant(
            "io.quarkus.hibernate.orm.PersistenceUnitExtension");
    public static final DotName PERSISTENCE_UNIT_EXTENSION_REPEATABLE_CONTAINER = createConstant(
            "io.quarkus.hibernate.orm.PersistenceUnitExtension$List");

    public static final DotName JPA_ENTITY = createConstant("jakarta.persistence.Entity");
    public static final DotName MAPPED_SUPERCLASS = createConstant("jakarta.persistence.MappedSuperclass");
    public static final DotName EMBEDDABLE = createConstant("jakarta.persistence.Embeddable");
    public static final DotName ID_CLASS = createConstant("jakarta.persistence.IdClass");
    public static final DotName CONVERTER = createConstant("jakarta.persistence.Converter");
    public static final DotName EMBEDDED = createConstant("jakarta.persistence.Embedded");
    public static final DotName EMBEDDED_ID = createConstant("jakarta.persistence.EmbeddedId");
    public static final DotName ELEMENT_COLLECTION = createConstant("jakarta.persistence.ElementCollection");
    public static final DotName HIBERNATE_PROXY = createConstant("org.hibernate.proxy.HibernateProxy");
    public static final DotName TYPE = createConstant("org.hibernate.annotations.Type");
    public static final DotName INJECT_SERVICE = createConstant("org.hibernate.service.spi.InjectService");
    public static final DotName ENTITY_MANAGER_FACTORY = createConstant("jakarta.persistence.EntityManagerFactory");
    public static final DotName SESSION_FACTORY = createConstant("org.hibernate.SessionFactory");
    public static final DotName ENTITY_MANAGER = createConstant("jakarta.persistence.EntityManager");
    public static final DotName SESSION = createConstant("org.hibernate.Session");
    public static final DotName STATELESS_SESSION = createConstant("org.hibernate.StatelessSession");
    public static final DotName CRITERIA_BUILDER = createConstant("jakarta.persistence.criteria.CriteriaBuilder");
    public static final DotName HIBERNATE_CRITERIA_BUILDER = createConstant(
            "org.hibernate.query.criteria.HibernateCriteriaBuilder");
    public static final DotName METAMODEL = createConstant("jakarta.persistence.metamodel.Metamodel");
    public static final DotName SCHEMA_MANAGER = createConstant("jakarta.persistence.SchemaManager");
    public static final DotName HIBERNATE_SCHEMA_MANAGER = createConstant("org.hibernate.relational.SchemaManager");
    public static final DotName CACHE = createConstant("jakarta.persistence.Cache");
    public static final DotName HIBERNATE_CACHE = createConstant("org.hibernate.Cache");
    public static final DotName PERSISTENCE_UNIT_UTIL = createConstant("jakarta.persistence.PersistenceUnitUtil");

    public static final DotName GENERIC_GENERATOR = createConstant("org.hibernate.annotations.GenericGenerator");
    public static final DotName ID_GENERATOR_TYPE = createConstant("org.hibernate.annotations.IdGeneratorType");
    public static final DotName VALUE_GENERATION_TYPE = createConstant("org.hibernate.annotations.ValueGenerationType");

    public static final DotName INTERCEPTOR = createConstant("org.hibernate.Interceptor");
    public static final DotName STATEMENT_INSPECTOR = createConstant("org.hibernate.resource.jdbc.spi.StatementInspector");
    public static final DotName FORMAT_MAPPER = createConstant("org.hibernate.type.format.FormatMapper");
    public static final DotName JSON_FORMAT = createConstant("io.quarkus.hibernate.orm.JsonFormat");
    public static final DotName XML_FORMAT = createConstant("io.quarkus.hibernate.orm.XmlFormat");

    public static final List<DotName> GENERATORS = List.of(
            createConstant("org.hibernate.generator.Assigned"),
            createConstant("org.hibernate.generator.internal.CurrentTimestampGeneration"),
            createConstant("org.hibernate.generator.internal.GeneratedAlwaysGeneration"),
            createConstant("org.hibernate.generator.internal.GeneratedGeneration"),
            createConstant("org.hibernate.generator.internal.SourceGeneration"),
            createConstant("org.hibernate.generator.internal.TenantIdGeneration"),
            createConstant("org.hibernate.generator.internal.VersionGeneration"),
            createConstant("org.hibernate.id.Assigned"),
            createConstant("org.hibernate.id.CompositeNestedGeneratedValueGenerator"),
            createConstant("org.hibernate.id.ForeignGenerator"),
            createConstant("org.hibernate.id.GUIDGenerator"),
            createConstant("org.hibernate.id.IdentityGenerator"),
            createConstant("org.hibernate.id.IncrementGenerator"),
            createConstant("org.hibernate.id.NativeGenerator"),
            createConstant("org.hibernate.id.SelectGenerator"),
            createConstant("org.hibernate.id.UUIDGenerator"),
            createConstant("org.hibernate.id.UUIDHexGenerator"),
            createConstant("org.hibernate.id.enhanced.SequenceStyleGenerator"),
            createConstant("org.hibernate.id.enhanced.TableGenerator"),
            createConstant("org.hibernate.id.uuid.UuidGenerator"),
            createConstant("org.hibernate.tuple.entity.CompositeGeneratorBuilder$CompositeBeforeExecutionGenerator"),
            createConstant("org.hibernate.tuple.entity.CompositeGeneratorBuilder$CompositeOnExecutionGenerator"),
            createConstant("org.hibernate.tuple.entity.CompositeGeneratorBuilder$DummyGenerator"));

    public static final List<DotName> OPTIMIZERS = List.of(
            createConstant("org.hibernate.id.enhanced.HiLoOptimizer"),
            createConstant("org.hibernate.id.enhanced.LegacyHiLoAlgorithmOptimizer"),
            createConstant("org.hibernate.id.enhanced.NoopOptimizer"),
            createConstant("org.hibernate.id.enhanced.PooledLoOptimizer"),
            createConstant("org.hibernate.id.enhanced.PooledLoThreadLocalOptimizer"),
            createConstant("org.hibernate.id.enhanced.PooledOptimizer"));

    // Only including naming strategies that will for sure be in every native binary and be instantiated at runtime.
    // PhysicalNamingStrategy and ImplicitNamingStrategy implementations,
    // in particular, are instantiated at static init.
    // ImplicitDatabaseObjectNamingStrategy cannot be overridden through supported settings in Quarkus ATM,
    // so we're left with only the default (and the legacy one for ORM 5.6 compatibility).
    public static final List<DotName> NAMING_STRATEGIES = List.of(
            createConstant("org.hibernate.id.enhanced.LegacyNamingStrategy"),
            createConstant("org.hibernate.id.enhanced.StandardNamingStrategy"));

    public static final List<DotName> PACKAGE_ANNOTATIONS = List.of(
            createConstant("jakarta.persistence.SequenceGenerator"),
            createConstant("jakarta.persistence.SequenceGenerators"),
            createConstant("jakarta.persistence.TableGenerator"),
            createConstant("jakarta.persistence.TableGenerators"),
            createConstant("org.hibernate.annotations.CollectionTypeRegistration"),
            createConstant("org.hibernate.annotations.CompositeTypeRegistration"),
            createConstant("org.hibernate.annotations.CompositeTypeRegistrations"),
            createConstant("org.hibernate.annotations.ConverterRegistration"),
            createConstant("org.hibernate.annotations.ConverterRegistrations"),
            createConstant("org.hibernate.annotations.DialectOverride$FilterDefOverrides"),
            createConstant("org.hibernate.annotations.DialectOverride$FilterDefs"),
            createConstant("org.hibernate.annotations.DialectOverride$Version"),
            createConstant("org.hibernate.annotations.EmbeddableInstantiatorRegistration"),
            createConstant("org.hibernate.annotations.EmbeddableInstantiatorRegistrations"),
            createConstant("org.hibernate.annotations.FetchProfile"),
            createConstant("org.hibernate.annotations.FetchProfile$FetchOverride"),
            createConstant("org.hibernate.annotations.FetchProfiles"),
            createConstant("org.hibernate.annotations.FilterDef"),
            createConstant("org.hibernate.annotations.FilterDefs"),
            createConstant("org.hibernate.annotations.GenericGenerator"),
            createConstant("org.hibernate.annotations.GenericGenerators"),
            createConstant("org.hibernate.annotations.JavaTypeRegistration"),
            createConstant("org.hibernate.annotations.JavaTypeRegistrations"),
            createConstant("org.hibernate.annotations.JdbcTypeRegistration"),
            createConstant("org.hibernate.annotations.JdbcTypeRegistrations"),
            createConstant("org.hibernate.annotations.ListIndexBase"),
            createConstant("org.hibernate.annotations.NamedEntityGraph"),
            createConstant("org.hibernate.annotations.NamedEntityGraphs"),
            createConstant("org.hibernate.annotations.NamedNativeQueries"),
            createConstant("org.hibernate.annotations.NamedNativeQuery"),
            createConstant("org.hibernate.annotations.NamedQueries"),
            createConstant("org.hibernate.annotations.NamedQuery"),
            createConstant("org.hibernate.annotations.NativeGenerator"),
            createConstant("org.hibernate.annotations.SoftDelete"),
            createConstant("org.hibernate.annotations.TypeRegistration"),
            createConstant("org.hibernate.annotations.TypeRegistrations"));

    public static final List<DotName> JPA_MAPPING_ANNOTATIONS = List.of(
            createConstant("jakarta.persistence.Access"),
            createConstant("jakarta.persistence.AssociationOverride"),
            createConstant("jakarta.persistence.AssociationOverrides"),
            createConstant("jakarta.persistence.AttributeOverride"),
            createConstant("jakarta.persistence.AttributeOverrides"),
            createConstant("jakarta.persistence.Basic"),
            createConstant("jakarta.persistence.Cacheable"),
            createConstant("jakarta.persistence.CheckConstraint"),
            createConstant("jakarta.persistence.CollectionTable"),
            createConstant("jakarta.persistence.Column"),
            createConstant("jakarta.persistence.ColumnResult"),
            createConstant("jakarta.persistence.ConstructorResult"),
            createConstant("jakarta.persistence.Convert"),
            createConstant("jakarta.persistence.Converter"),
            createConstant("jakarta.persistence.Converts"),
            createConstant("jakarta.persistence.DiscriminatorColumn"),
            createConstant("jakarta.persistence.DiscriminatorValue"),
            createConstant("jakarta.persistence.ElementCollection"),
            createConstant("jakarta.persistence.Embeddable"),
            createConstant("jakarta.persistence.Embedded"),
            createConstant("jakarta.persistence.EmbeddedId"),
            createConstant("jakarta.persistence.EnumeratedValue"),
            createConstant("jakarta.persistence.Entity"),
            createConstant("jakarta.persistence.EntityListeners"),
            createConstant("jakarta.persistence.EntityResult"),
            createConstant("jakarta.persistence.Enumerated"),
            createConstant("jakarta.persistence.ExcludeDefaultListeners"),
            createConstant("jakarta.persistence.ExcludeSuperclassListeners"),
            createConstant("jakarta.persistence.FieldResult"),
            createConstant("jakarta.persistence.ForeignKey"),
            createConstant("jakarta.persistence.GeneratedValue"),
            createConstant("jakarta.persistence.Id"),
            createConstant("jakarta.persistence.IdClass"),
            createConstant("jakarta.persistence.Index"),
            createConstant("jakarta.persistence.Inheritance"),
            createConstant("jakarta.persistence.JoinColumn"),
            createConstant("jakarta.persistence.JoinColumns"),
            createConstant("jakarta.persistence.JoinTable"),
            createConstant("jakarta.persistence.Lob"),
            createConstant("jakarta.persistence.ManyToMany"),
            createConstant("jakarta.persistence.ManyToOne"),
            createConstant("jakarta.persistence.MapKey"),
            createConstant("jakarta.persistence.MapKeyClass"),
            createConstant("jakarta.persistence.MapKeyColumn"),
            createConstant("jakarta.persistence.MapKeyEnumerated"),
            createConstant("jakarta.persistence.MapKeyJoinColumn"),
            createConstant("jakarta.persistence.MapKeyJoinColumns"),
            createConstant("jakarta.persistence.MapKeyTemporal"),
            createConstant("jakarta.persistence.MappedSuperclass"),
            createConstant("jakarta.persistence.MapsId"),
            createConstant("jakarta.persistence.NamedAttributeNode"),
            createConstant("jakarta.persistence.NamedEntityGraph"),
            createConstant("jakarta.persistence.NamedEntityGraphs"),
            createConstant("jakarta.persistence.NamedNativeQueries"),
            createConstant("jakarta.persistence.NamedNativeQuery"),
            createConstant("jakarta.persistence.NamedQueries"),
            createConstant("jakarta.persistence.NamedQuery"),
            createConstant("jakarta.persistence.NamedStoredProcedureQueries"),
            createConstant("jakarta.persistence.NamedStoredProcedureQuery"),
            createConstant("jakarta.persistence.NamedSubgraph"),
            createConstant("jakarta.persistence.OneToMany"),
            createConstant("jakarta.persistence.OneToOne"),
            createConstant("jakarta.persistence.OrderBy"),
            createConstant("jakarta.persistence.OrderColumn"),
            createConstant("jakarta.persistence.PersistenceContext"),
            createConstant("jakarta.persistence.PersistenceContexts"),
            createConstant("jakarta.persistence.PersistenceProperty"),
            createConstant("jakarta.persistence.PersistenceUnit"),
            createConstant("jakarta.persistence.PersistenceUnits"),
            createConstant("jakarta.persistence.PostLoad"),
            createConstant("jakarta.persistence.PostPersist"),
            createConstant("jakarta.persistence.PostRemove"),
            createConstant("jakarta.persistence.PostUpdate"),
            createConstant("jakarta.persistence.PrePersist"),
            createConstant("jakarta.persistence.PreRemove"),
            createConstant("jakarta.persistence.PreUpdate"),
            createConstant("jakarta.persistence.PrimaryKeyJoinColumn"),
            createConstant("jakarta.persistence.PrimaryKeyJoinColumns"),
            createConstant("jakarta.persistence.QueryHint"),
            createConstant("jakarta.persistence.SecondaryTable"),
            createConstant("jakarta.persistence.SecondaryTables"),
            createConstant("jakarta.persistence.SequenceGenerator"),
            createConstant("jakarta.persistence.SequenceGenerators"),
            createConstant("jakarta.persistence.SqlResultSetMapping"),
            createConstant("jakarta.persistence.SqlResultSetMappings"),
            createConstant("jakarta.persistence.StoredProcedureParameter"),
            createConstant("jakarta.persistence.Table"),
            createConstant("jakarta.persistence.TableGenerator"),
            createConstant("jakarta.persistence.TableGenerators"),
            createConstant("jakarta.persistence.Temporal"),
            createConstant("jakarta.persistence.Transient"),
            createConstant("jakarta.persistence.UniqueConstraint"),
            createConstant("jakarta.persistence.Version"));

    public static final List<DotName> HIBERNATE_MAPPING_ANNOTATIONS = List.of(
            createConstant("org.hibernate.annotations.Any"),
            createConstant("org.hibernate.annotations.AnyDiscriminator"),
            createConstant("org.hibernate.annotations.AnyDiscriminatorValue"),
            createConstant("org.hibernate.annotations.AnyDiscriminatorValues"),
            createConstant("org.hibernate.annotations.AnyDiscriminatorImplicitValues"),
            createConstant("org.hibernate.annotations.AnyKeyJavaClass"),
            createConstant("org.hibernate.annotations.AnyKeyJavaType"),
            createConstant("org.hibernate.annotations.AnyKeyJdbcType"),
            createConstant("org.hibernate.annotations.AnyKeyJdbcTypeCode"),
            createConstant("org.hibernate.annotations.Array"),
            createConstant("org.hibernate.annotations.AttributeAccessor"),
            createConstant("org.hibernate.annotations.AttributeBinderType"),
            createConstant("org.hibernate.annotations.Bag"),
            createConstant("org.hibernate.annotations.BatchSize"),
            createConstant("org.hibernate.annotations.Cache"),
            createConstant("org.hibernate.annotations.Cascade"),
            createConstant("org.hibernate.annotations.Check"),
            createConstant("org.hibernate.annotations.Checks"),
            createConstant("org.hibernate.annotations.Collate"),
            createConstant("org.hibernate.annotations.CollectionId"),
            createConstant("org.hibernate.annotations.CollectionIdJavaClass"),
            createConstant("org.hibernate.annotations.CollectionIdJavaType"),
            createConstant("org.hibernate.annotations.CollectionIdJdbcType"),
            createConstant("org.hibernate.annotations.CollectionIdJdbcTypeCode"),
            createConstant("org.hibernate.annotations.CollectionIdMutability"),
            createConstant("org.hibernate.annotations.CollectionIdType"),
            createConstant("org.hibernate.annotations.CollectionType"),
            createConstant("org.hibernate.annotations.CollectionTypeRegistration"),
            createConstant("org.hibernate.annotations.CollectionTypeRegistrations"),
            createConstant("org.hibernate.annotations.ColumnDefault"),
            createConstant("org.hibernate.annotations.ColumnTransformer"),
            createConstant("org.hibernate.annotations.ColumnTransformers"),
            createConstant("org.hibernate.annotations.Columns"),
            createConstant("org.hibernate.annotations.Comment"),
            createConstant("org.hibernate.annotations.Comments"),
            createConstant("org.hibernate.annotations.CompositeType"),
            createConstant("org.hibernate.annotations.CompositeTypeRegistration"),
            createConstant("org.hibernate.annotations.CompositeTypeRegistrations"),
            createConstant("org.hibernate.annotations.ConcreteProxy"),
            createConstant("org.hibernate.annotations.ConverterRegistration"),
            createConstant("org.hibernate.annotations.ConverterRegistrations"),
            createConstant("org.hibernate.annotations.CreationTimestamp"),
            createConstant("org.hibernate.annotations.CurrentTimestamp"),
            createConstant("org.hibernate.annotations.DialectOverride$Check"),
            createConstant("org.hibernate.annotations.DialectOverride$Checks"),
            createConstant("org.hibernate.annotations.DialectOverride$ColumnDefault"),
            createConstant("org.hibernate.annotations.DialectOverride$ColumnDefaults"),
            createConstant("org.hibernate.annotations.DialectOverride$DiscriminatorFormula"),
            createConstant("org.hibernate.annotations.DialectOverride$DiscriminatorFormulas"),
            createConstant("org.hibernate.annotations.DialectOverride$FilterDefOverrides"),
            createConstant("org.hibernate.annotations.DialectOverride$FilterDefs"),
            createConstant("org.hibernate.annotations.DialectOverride$FilterOverrides"),
            createConstant("org.hibernate.annotations.DialectOverride$Filters"),
            createConstant("org.hibernate.annotations.DialectOverride$Formula"),
            createConstant("org.hibernate.annotations.DialectOverride$Formulas"),
            createConstant("org.hibernate.annotations.DialectOverride$GeneratedColumn"),
            createConstant("org.hibernate.annotations.DialectOverride$GeneratedColumns"),
            createConstant("org.hibernate.annotations.DialectOverride$JoinFormula"),
            createConstant("org.hibernate.annotations.DialectOverride$JoinFormulas"),
            createConstant("org.hibernate.annotations.DialectOverride$OverridesAnnotation"),
            createConstant("org.hibernate.annotations.DialectOverride$SQLDelete"),
            createConstant("org.hibernate.annotations.DialectOverride$SQLDeleteAll"),
            createConstant("org.hibernate.annotations.DialectOverride$SQLDeleteAlls"),
            createConstant("org.hibernate.annotations.DialectOverride$SQLDeletes"),
            createConstant("org.hibernate.annotations.DialectOverride$SQLInsert"),
            createConstant("org.hibernate.annotations.DialectOverride$SQLInserts"),
            createConstant("org.hibernate.annotations.DialectOverride$SQLOrder"),
            createConstant("org.hibernate.annotations.DialectOverride$SQLOrders"),
            createConstant("org.hibernate.annotations.DialectOverride$SQLRestriction"),
            createConstant("org.hibernate.annotations.DialectOverride$SQLRestrictions"),
            createConstant("org.hibernate.annotations.DialectOverride$SQLSelect"),
            createConstant("org.hibernate.annotations.DialectOverride$SQLSelects"),
            createConstant("org.hibernate.annotations.DialectOverride$SQLUpdate"),
            createConstant("org.hibernate.annotations.DialectOverride$SQLUpdates"),
            createConstant("org.hibernate.annotations.DialectOverride$Version"),
            createConstant("org.hibernate.annotations.DiscriminatorFormula"),
            createConstant("org.hibernate.annotations.DiscriminatorOptions"),
            createConstant("org.hibernate.annotations.DynamicInsert"),
            createConstant("org.hibernate.annotations.DynamicUpdate"),
            createConstant("org.hibernate.annotations.EmbeddableInstantiator"),
            createConstant("org.hibernate.annotations.EmbeddedColumnNaming"),
            createConstant("org.hibernate.annotations.EmbeddableInstantiatorRegistration"),
            createConstant("org.hibernate.annotations.EmbeddableInstantiatorRegistrations"),
            createConstant("org.hibernate.annotations.Fetch"),
            createConstant("org.hibernate.annotations.FetchProfile"),
            createConstant("org.hibernate.annotations.FetchProfile$FetchOverride"),
            createConstant("org.hibernate.annotations.FetchProfileOverride"),
            createConstant("org.hibernate.annotations.FetchProfileOverrides"),
            createConstant("org.hibernate.annotations.FetchProfiles"),
            createConstant("org.hibernate.annotations.Filter"),
            createConstant("org.hibernate.annotations.FilterDef"),
            createConstant("org.hibernate.annotations.FilterDefs"),
            createConstant("org.hibernate.annotations.FilterJoinTable"),
            createConstant("org.hibernate.annotations.FilterJoinTables"),
            createConstant("org.hibernate.annotations.Filters"),
            createConstant("org.hibernate.annotations.Formula"),
            createConstant("org.hibernate.annotations.FractionalSeconds"),
            createConstant("org.hibernate.annotations.Generated"),
            createConstant("org.hibernate.annotations.GeneratedColumn"),
            createConstant("org.hibernate.annotations.GenericGenerator"),
            createConstant("org.hibernate.annotations.GenericGenerators"),
            createConstant("org.hibernate.annotations.HQLSelect"),
            createConstant("org.hibernate.annotations.IdGeneratorType"),
            createConstant("org.hibernate.annotations.Immutable"),
            createConstant("org.hibernate.annotations.Imported"),
            createConstant("org.hibernate.annotations.Instantiator"),
            createConstant("org.hibernate.annotations.JavaType"),
            createConstant("org.hibernate.annotations.JavaTypeRegistration"),
            createConstant("org.hibernate.annotations.JavaTypeRegistrations"),
            createConstant("org.hibernate.annotations.JdbcType"),
            createConstant("org.hibernate.annotations.JdbcTypeCode"),
            createConstant("org.hibernate.annotations.JdbcTypeRegistration"),
            createConstant("org.hibernate.annotations.JdbcTypeRegistrations"),
            createConstant("org.hibernate.annotations.JoinColumnOrFormula"),
            createConstant("org.hibernate.annotations.JoinColumnsOrFormulas"),
            createConstant("org.hibernate.annotations.JoinFormula"),
            createConstant("org.hibernate.annotations.LazyGroup"),
            createConstant("org.hibernate.annotations.ListIndexBase"),
            createConstant("org.hibernate.annotations.ListIndexJavaType"),
            createConstant("org.hibernate.annotations.ListIndexJdbcType"),
            createConstant("org.hibernate.annotations.ListIndexJdbcTypeCode"),
            createConstant("org.hibernate.annotations.ManyToAny"),
            createConstant("org.hibernate.annotations.MapKeyJavaType"),
            createConstant("org.hibernate.annotations.MapKeyJdbcType"),
            createConstant("org.hibernate.annotations.MapKeyJdbcTypeCode"),
            createConstant("org.hibernate.annotations.MapKeyMutability"),
            createConstant("org.hibernate.annotations.MapKeyType"),
            createConstant("org.hibernate.annotations.Mutability"),
            createConstant("org.hibernate.annotations.NamedEntityGraph"),
            createConstant("org.hibernate.annotations.NamedEntityGraphs"),
            createConstant("org.hibernate.annotations.NamedNativeQueries"),
            createConstant("org.hibernate.annotations.NamedNativeQuery"),
            createConstant("org.hibernate.annotations.NamedQueries"),
            createConstant("org.hibernate.annotations.NamedQuery"),
            createConstant("org.hibernate.annotations.Nationalized"),
            createConstant("org.hibernate.annotations.NativeGenerator"),
            createConstant("org.hibernate.annotations.NaturalId"),
            createConstant("org.hibernate.annotations.NaturalIdCache"),
            createConstant("org.hibernate.annotations.NotFound"),
            createConstant("org.hibernate.annotations.OnDelete"),
            createConstant("org.hibernate.annotations.OptimisticLock"),
            createConstant("org.hibernate.annotations.OptimisticLocking"),
            createConstant("org.hibernate.annotations.ParamDef"),
            createConstant("org.hibernate.annotations.Parameter"),
            createConstant("org.hibernate.annotations.Parent"),
            createConstant("org.hibernate.annotations.PartitionKey"),
            createConstant("org.hibernate.annotations.PropertyRef"),
            createConstant("org.hibernate.annotations.QueryCacheLayout"),
            createConstant("org.hibernate.annotations.RowId"),
            createConstant("org.hibernate.annotations.SQLDelete"),
            createConstant("org.hibernate.annotations.SQLDeleteAll"),
            createConstant("org.hibernate.annotations.SQLDeletes"),
            createConstant("org.hibernate.annotations.SQLInsert"),
            createConstant("org.hibernate.annotations.SQLInserts"),
            createConstant("org.hibernate.annotations.SQLJoinTableRestriction"),
            createConstant("org.hibernate.annotations.SQLOrder"),
            createConstant("org.hibernate.annotations.SQLRestriction"),
            createConstant("org.hibernate.annotations.SQLSelect"),
            createConstant("org.hibernate.annotations.SQLUpdate"),
            createConstant("org.hibernate.annotations.SQLUpdates"),
            createConstant("org.hibernate.annotations.SecondaryRow"),
            createConstant("org.hibernate.annotations.SecondaryRows"),
            createConstant("org.hibernate.annotations.SoftDelete"),
            createConstant("org.hibernate.annotations.SortComparator"),
            createConstant("org.hibernate.annotations.SortNatural"),
            createConstant("org.hibernate.annotations.Source"),
            createConstant("org.hibernate.annotations.SqlFragmentAlias"),
            createConstant("org.hibernate.annotations.Struct"),
            createConstant("org.hibernate.annotations.Subselect"),
            createConstant("org.hibernate.annotations.Synchronize"),
            createConstant("org.hibernate.annotations.TargetEmbeddable"),
            createConstant("org.hibernate.annotations.TenantId"),
            createConstant("org.hibernate.annotations.TimeZoneColumn"),
            createConstant("org.hibernate.annotations.TimeZoneStorage"),
            createConstant("org.hibernate.annotations.Type"),
            createConstant("org.hibernate.annotations.TypeBinderType"),
            createConstant("org.hibernate.annotations.TypeRegistration"),
            createConstant("org.hibernate.annotations.TypeRegistrations"),
            createConstant("org.hibernate.annotations.UpdateTimestamp"),
            createConstant("org.hibernate.annotations.UuidGenerator"),
            createConstant("org.hibernate.annotations.ValueGenerationType"),
            createConstant("org.hibernate.annotations.View"));

    public static final List<DotName> ANNOTATED_WITH_INJECT_SERVICE = List.of(
            createConstant("org.hibernate.engine.jdbc.connections.internal.DatasourceConnectionProviderImpl"));

    public static final List<DotName> JPA_LISTENER_ANNOTATIONS = List.of(
            createConstant("jakarta.persistence.PostLoad"),
            createConstant("jakarta.persistence.PostPersist"),
            createConstant("jakarta.persistence.PostRemove"),
            createConstant("jakarta.persistence.PostUpdate"),
            createConstant("jakarta.persistence.PrePersist"),
            createConstant("jakarta.persistence.PreRemove"),
            createConstant("jakarta.persistence.PreUpdate"));

    public static final List<DotName> JDBC_JAVA_TYPES = List.of(
            createConstant("java.lang.Boolean"),
            createConstant("java.lang.Byte"),
            createConstant("java.lang.Character"),
            createConstant("java.lang.Class"),
            createConstant("java.lang.Double"),
            createConstant("java.lang.Float"),
            createConstant("java.lang.Integer"),
            createConstant("java.lang.Long"),
            createConstant("java.lang.Object"),
            createConstant("java.lang.Short"),
            createConstant("java.lang.String"),
            createConstant("java.math.BigDecimal"),
            createConstant("java.math.BigInteger"),
            createConstant("java.net.InetAddress"),
            createConstant("java.net.URL"),
            createConstant("java.sql.Blob"),
            createConstant("java.sql.Clob"),
            createConstant("java.sql.NClob"),
            createConstant("java.time.Duration"),
            createConstant("java.time.Instant"),
            createConstant("java.time.LocalDate"),
            createConstant("java.time.LocalDateTime"),
            createConstant("java.time.LocalTime"),
            createConstant("java.time.OffsetDateTime"),
            createConstant("java.time.OffsetTime"),
            createConstant("java.time.Year"),
            createConstant("java.time.ZoneId"),
            createConstant("java.time.ZoneOffset"),
            createConstant("java.time.ZonedDateTime"),
            createConstant("java.util.Calendar"),
            createConstant("java.util.Currency"),
            createConstant("java.util.Date"),
            createConstant("java.util.Locale"),
            createConstant("java.util.Map$Entry"),
            createConstant("java.util.TimeZone"),
            createConstant("java.util.UUID"),
            createConstant("java.lang.Void"));

    public static final DotName HIBERNATE_ORM_PROCESSOR = createConstant(
            "io.quarkus.hibernate.orm.deployment.HibernateOrmProcessor");

    public static final DotName HIBERNATE_USER_TYPE_PROCESSOR = createConstant(
            "io.quarkus.hibernate.orm.deployment.HibernateUserTypeProcessor");

    public static final DotName GRAAL_VM_FEATURES = createConstant("io.quarkus.hibernate.orm.deployment.GraalVMFeatures");

    public static final List<DotName> SERVICE_PROVIDERS = List.of(
            // Accessed in org.hibernate.query.sqm.internal.SqmCriteriaNodeBuilder.<init>
            createConstant("org.hibernate.query.criteria.spi.CriteriaBuilderExtension"),
            // Accessed in io.quarkus.hibernate.orm.runtime.customized.QuarkusStrategySelectorBuilder.buildSelector
            createConstant("org.hibernate.boot.registry.selector.StrategyRegistrationProvider"),
            // Accessed in org.hibernate.internal.FastSessionServices.<init>
            createConstant("org.hibernate.event.spi.EventManager"),
            // Accessed in org.hibernate.query.internal.QueryEngineImpl.sortedFunctionContributors
            createConstant("org.hibernate.boot.model.FunctionContributor"),
            // Accessed in org.hibernate.event.spi.EventEngine.<init>
            createConstant("org.hibernate.event.spi.EventEngineContributor"),
            // Accessed in org.hibernate.service.internal.SessionFactoryServiceRegistryFactoryImpl.buildServiceRegistry
            createConstant("org.hibernate.service.spi.SessionFactoryServiceContributor"));
}
