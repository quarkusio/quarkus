package io.quarkus.resteasy.reactive.links.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.UriInfo;

import io.quarkus.resteasy.reactive.links.RestLinksProvider;

final class RestLinksProviderImpl implements RestLinksProvider {

    private static LinksContainer linksContainer;

    private static GetterAccessorsContainer getterAccessorsContainer;

    private final UriInfo uriInfo;

    static void setLinksContainer(LinksContainer context) {
        RestLinksProviderImpl.linksContainer = context;
    }

    static void setGetterAccessorsContainer(GetterAccessorsContainer getterAccessorsContainer) {
        RestLinksProviderImpl.getterAccessorsContainer = getterAccessorsContainer;
    }

    RestLinksProviderImpl(UriInfo uriInfo) {
        this.uriInfo = uriInfo;
    }

    @Override
    public Collection<Link> getTypeLinks(Class<?> elementType) {
        verifyInit();

        List<LinkInfo> linkInfoList = linksContainer.getForClass(elementType);
        List<Link> links = new ArrayList<>(linkInfoList.size());
        for (LinkInfo linkInfo : linkInfoList) {
            if (linkInfo.getPathParameters().size() == 0) {
                links.add(linkBuilderFor(linkInfo).build());
            }
        }
        return links;
    }

    @Override
    public <T> Collection<Link> getInstanceLinks(T instance) {
        verifyInit();

        List<LinkInfo> linkInfoList = linksContainer.getForClass(instance.getClass());
        List<Link> links = new ArrayList<>(linkInfoList.size());
        for (LinkInfo linkInfo : linkInfoList) {
            links.add(linkBuilderFor(linkInfo).build(getPathParameterValues(linkInfo, instance)));
        }
        return links;
    }

    private Link.Builder linkBuilderFor(LinkInfo linkInfo) {
        Link.Builder builder = Link.fromUriBuilder(uriInfo.getBaseUriBuilder().path(linkInfo.getPath()))
                .rel(linkInfo.getRel());
        if (linkInfo.getTitle() != null) {
            builder.title(linkInfo.getTitle());
        }

        if (linkInfo.getType() != null) {
            builder.type(linkInfo.getType());
        }

        return builder;
    }

    private Object[] getPathParameterValues(LinkInfo linkInfo, Object instance) {
        List<Object> values = new ArrayList<>(linkInfo.getPathParameters().size());
        for (String name : linkInfo.getPathParameters()) {
            GetterAccessor accessor = getterAccessorsContainer.get(linkInfo.getEntityType(), name);
            if (accessor != null) {
                values.add(accessor.get(instance));
            } else {
                values.add("{" + name + "}");
            }
        }
        return values.toArray();
    }

    private void verifyInit() {
        if (linksContainer == null) {
            throw new IllegalStateException("Links context has not been initialized");
        }
        if (getterAccessorsContainer == null) {
            throw new IllegalStateException("Getter accessors container has not been initialized");
        }
    }
}
