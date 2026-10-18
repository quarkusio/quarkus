package io.quarkus.panache.hibernate.common.deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.panache.common.deployment.MetamodelInfo;

public final class HibernateMetamodelForFieldAccessBuildItem extends SimpleBuildItem {
    private final MetamodelInfo metamodelInfo;

    public HibernateMetamodelForFieldAccessBuildItem(MetamodelInfo metamodelInfo) {
        this.metamodelInfo = metamodelInfo;
    }

    public MetamodelInfo getMetamodelInfo() {
        return metamodelInfo;
    }
}
