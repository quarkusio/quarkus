package io.quarkus.hibernate.orm.deployment;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.jandex.DotName;

public class ClassNames {

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
    public static final DotName PROXY = createConstant("org.hibernate.annotations.Proxy");
    public static final DotName HIBERNATE_PROXY = createConstant("org.hibernate.proxy.HibernateProxy");
    public static final DotName TYPE = createConstant("org.hibernate.annotations.Type");
    public static final DotName INJECT_SERVICE = createConstant("org.hibernate.service.spi.InjectService");
    public static final DotName ENTITY_MANAGER_FACTORY = createConstant("jakarta.persistence.EntityManagerFactory");
    public static final DotName SESSION_FACTORY = createConstant("org.hibernate.SessionFactory");
    public static final DotName ENTITY_MANAGER = createConstant("jakarta.persistence.EntityManager");
    public static final DotName SESSION = createConstant("org.hibernate.Session");
    public static final DotName STATELESS_SESSION = createConstant("org.hibernate.StatelessSession");

    public static final DotName INTERCEPTOR = createConstant("org.hibernate.Interceptor");
    public static final DotName STATEMENT_INSPECTOR = createConstant("org.hibernate.resource.jdbc.spi.StatementInspector");
    public static final DotName FORMAT_MAPPER = createConstant("org.hibernate.type.format.FormatMapper");
    public static final DotName JSON_FORMAT = createConstant("io.quarkus.hibernate.orm.JsonFormat");
    public static final DotName XML_FORMAT = createConstant("io.quarkus.hibernate.orm.XmlFormat");

    public static final List<DotName> GENERATORS = List.of(
            createConstant("org.hibernate.generator.internal.CurrentTimestampGeneration"),
            createConstant("org.hibernate.generator.internal.GeneratedAlwaysGeneration"),
            createConstant("org.hibernate.generator.internal.GeneratedGeneration"),
            createConstant("org.hibernate.generator.internal.SourceGeneration"),
            createConstant("org.hibernate.generator.internal.TenantIdGeneration"),
            createConstant("org.hibernate.generator.internal.VersionGeneration"),
            createConstant("org.hibernate.id.Assigned"),
            createConstant("org.hibernate.id.ForeignGenerator"),
            createConstant("org.hibernate.id.GUIDGenerator"),
            createConstant("org.hibernate.id.IdentityGenerator"),
            createConstant("org.hibernate.id.IncrementGenerator"),
            createConstant("org.hibernate.id.SelectGenerator"),
            createConstant("org.hibernate.id.UUIDGenerator"),
            createConstant("org.hibernate.id.UUIDHexGenerator"),
            createConstant("org.hibernate.tuple.CreationTimestampGeneration"),
            createConstant("org.hibernate.tuple.UpdateTimestampGeneration"),
            createConstant("org.hibernate.tuple.VmValueGeneration"));

}
