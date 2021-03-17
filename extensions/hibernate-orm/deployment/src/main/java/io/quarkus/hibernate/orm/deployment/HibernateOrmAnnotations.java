package io.quarkus.hibernate.orm.deployment;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jboss.jandex.DotName;

public final class HibernateOrmAnnotations {

    private HibernateOrmAnnotations() {
    }

    public static final List<DotName> PACKAGE_ANNOTATIONS = Collections.unmodifiableList(Arrays.asList(
            DotName.createSimple("org.hibernate.annotations.AnyMetaDef"),
            DotName.createSimple("org.hibernate.annotations.AnyMetaDefs"),
            DotName.createSimple("org.hibernate.annotations.FetchProfile"),
            DotName.createSimple("org.hibernate.annotations.FetchProfile$FetchOverride"),
            DotName.createSimple("org.hibernate.annotations.FetchProfiles"),
            DotName.createSimple("org.hibernate.annotations.FilterDef"),
            DotName.createSimple("org.hibernate.annotations.FilterDefs"),
            DotName.createSimple("org.hibernate.annotations.GenericGenerator"),
            DotName.createSimple("org.hibernate.annotations.GenericGenerators"),
            DotName.createSimple("org.hibernate.annotations.ListIndexBase"),
            DotName.createSimple("org.hibernate.annotations.NamedNativeQueries"),
            DotName.createSimple("org.hibernate.annotations.NamedNativeQuery"),
            DotName.createSimple("org.hibernate.annotations.NamedQueries"),
            DotName.createSimple("org.hibernate.annotations.NamedQuery"),
            DotName.createSimple("org.hibernate.annotations.TypeDef"),
            DotName.createSimple("org.hibernate.annotations.TypeDefs")));
}
