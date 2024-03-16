package io.quarkus.resteasy.reactive.spi;

import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;

public final class ContextResolverBuildItem extends MultiBuildItem implements CheckBean {

    private final String className;
    private final String providedType;
    private final List<String> mediaTypes;
    private final boolean registerAsBean;

    public ContextResolverBuildItem(String className, List<String> mediaTypes, String providedType) {
        this(className, providedType, mediaTypes, true);
    }

    public ContextResolverBuildItem(String className, String providedType, List<String> mediaTypes, boolean registerAsBean) {
        this.className = className;
        this.providedType = providedType;
        this.mediaTypes = mediaTypes;
        this.registerAsBean = registerAsBean;
    }

    public String getClassName() {
        return className;
    }

    @Override
    public boolean isRegisterAsBean() {
        return registerAsBean;
    }

    public List<String> getMediaTypes() {
        return mediaTypes;
    }

    public String getProvidedType() {
        return providedType;
    }
}
