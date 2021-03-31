package io.quarkus.resteasy.reactive.links.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriInfo;

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

        List<Link> links = new LinkedList<>();
        for (LinkInfo linkInfo : linksContainer.getForClass(elementType)) {
            if (linkInfo.getPathParameters().size() == 0) {
                links.add(Link.fromUriBuilder(uriInfo.getBaseUriBuilder().path(linkInfo.getPath()))
                        .rel(linkInfo.getRel())
                        .build());
            }
        }
        return links;
    }

    @Override
    public <T> Collection<Link> getInstanceLinks(T instance) {
        verifyInit();

        List<Link> links = new LinkedList<>();
        for (LinkInfo linkInfo : linksContainer.getForClass(instance.getClass())) {
            links.add(Link.fromUriBuilder(uriInfo.getBaseUriBuilder().path(linkInfo.getPath()))
                    .rel(linkInfo.getRel())
                    .build(getPathParameterValues(linkInfo, instance)));
        }
        return links;
    }

    private Object[] getPathParameterValues(LinkInfo linkInfo, Object instance) {
        List<Object> values = new ArrayList<>(linkInfo.getPathParameters().size());
        for (String name : linkInfo.getPathParameters()) {
            GetterAccessor accessor = getterAccessorsContainer.get(linkInfo.getEntityType(), name);
            if (accessor == null) {
                throw new RuntimeException("Could not get '" + name + "' value");
            }
            values.add(accessor.get(instance));
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
