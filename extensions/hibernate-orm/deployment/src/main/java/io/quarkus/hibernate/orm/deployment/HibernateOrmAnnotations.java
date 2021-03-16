package io.quarkus.hibernate.orm.deployment;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hibernate.annotations.AnyMetaDef;
import org.hibernate.annotations.AnyMetaDefs;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.annotations.FetchProfiles;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterDefs;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.GenericGenerators;
import org.hibernate.annotations.ListIndexBase;
import org.hibernate.annotations.NamedNativeQueries;
import org.hibernate.annotations.NamedNativeQuery;
import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.jboss.jandex.DotName;

public final class HibernateOrmAnnotations {

    private HibernateOrmAnnotations() {
    }

    public static final List<DotName> PACKAGE_ANNOTATIONS = Collections.unmodifiableList(Arrays.asList(
            DotName.createSimple(AnyMetaDef.class.getName()),
            DotName.createSimple(AnyMetaDefs.class.getName()),
            DotName.createSimple(FetchProfile.class.getName()),
            DotName.createSimple(FetchProfile.FetchOverride.class.getName()),
            DotName.createSimple(FetchProfiles.class.getName()),
            DotName.createSimple(FilterDef.class.getName()),
            DotName.createSimple(FilterDefs.class.getName()),
            DotName.createSimple(GenericGenerator.class.getName()),
            DotName.createSimple(GenericGenerators.class.getName()),
            DotName.createSimple(ListIndexBase.class.getName()),
            DotName.createSimple(NamedNativeQueries.class.getName()),
            DotName.createSimple(NamedNativeQuery.class.getName()),
            DotName.createSimple(NamedQueries.class.getName()),
            DotName.createSimple(NamedQuery.class.getName()),
            DotName.createSimple(TypeDef.class.getName()),
            DotName.createSimple(TypeDefs.class.getName())));
}
