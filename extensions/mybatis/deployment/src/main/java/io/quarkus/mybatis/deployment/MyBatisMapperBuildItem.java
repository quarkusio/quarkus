package io.quarkus.mybatis.deployment;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

public final class MyBatisMapperBuildItem extends MultiBuildItem {
    private final DotName dotName;

    public MyBatisMapperBuildItem(DotName dotName) {
        this.dotName = dotName;
    }

    public DotName getDotName() {
        return dotName;
    }
}
