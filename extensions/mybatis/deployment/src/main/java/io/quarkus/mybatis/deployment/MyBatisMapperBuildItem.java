package io.quarkus.mybatis.deployment;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

public final class MyBatisMapperBuildItem extends MultiBuildItem {
    private final DotName mapperName;

    public MyBatisMapperBuildItem(DotName mapperName) {
        this.mapperName = mapperName;
    }

    public DotName getMapperName() {
        return mapperName;
    }
}
