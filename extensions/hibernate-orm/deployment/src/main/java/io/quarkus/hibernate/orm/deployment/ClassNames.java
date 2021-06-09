package io.quarkus.hibernate.orm.deployment;

import java.util.HashSet;
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

    public static final DotName STATIC_METAMODEL = createConstant("javax.persistence.metamodel.StaticMetamodel");

    public static final DotName QUARKUS_PERSISTENCE_UNIT = createConstant("io.quarkus.hibernate.orm.PersistenceUnit");
    public static final DotName QUARKUS_PERSISTENCE_UNIT_REPEATABLE_CONTAINER = createConstant(
            "io.quarkus.hibernate.orm.PersistenceUnit$List");
    public static final DotName JPA_PERSISTENCE_UNIT = createConstant("javax.persistence.PersistenceUnit");
    public static final DotName JPA_PERSISTENCE_CONTEXT = createConstant("javax.persistence.PersistenceContext");

    public static final DotName JPA_ENTITY = createConstant("javax.persistence.Entity");
    public static final DotName MAPPED_SUPERCLASS = createConstant("javax.persistence.MappedSuperclass");
    public static final DotName EMBEDDABLE = createConstant("javax.persistence.Embeddable");
    public static final DotName EMBEDDED = createConstant("javax.persistence.Embedded");
    public static final DotName ELEMENT_COLLECTION = createConstant("javax.persistence.ElementCollection");
    public static final DotName PROXY = createConstant("org.hibernate.annotations.Proxy");
    public static final DotName HIBERNATE_PROXY = createConstant("org.hibernate.proxy.HibernateProxy");
    public static final DotName TYPE = createConstant("org.hibernate.annotations.Type");
    public static final DotName TYPE_DEFINITION = createConstant("org.hibernate.annotations.TypeDef");
    public static final DotName TYPE_DEFINITIONS = createConstant("org.hibernate.annotations.TypeDefs");
    public static final DotName INJECT_SERVICE = createConstant("org.hibernate.service.spi.InjectService");

    public static final DotName ENTITY_MANAGER_FACTORY = createConstant("javax.persistence.EntityManagerFactory");
    public static final DotName SESSION_FACTORY = createConstant("org.hibernate.SessionFactory");
    public static final DotName ENTITY_MANAGER = createConstant("javax.persistence.EntityManager");
    public static final DotName SESSION = createConstant("org.hibernate.Session");

}
