package io.quarkus.resteasy.reactive.links.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.UriInfo;

import io.quarkus.resteasy.reactive.links.RestLinksProvider;

public final class RestLinksProviderImpl implements RestLinksProvider {

    private static LinksContainer linksContainer;

    private static GetterAccessorsContainer getterAccessorsContainer;

    private final UriInfo uriInfo;

    public static void initialize(Map<String, List<LinkInfo>> links, Map<String, Map<String, String>> accessorClassNames) {
        linksContainer = new LinksContainer(links);
        getterAccessorsContainer = GetterAccessorsContainer.of(accessorClassNames);
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
            if (linkInfo.pathParameters().size() == 0) {
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
        Link.Builder builder = Link.fromUriBuilder(uriInfo.getBaseUriBuilder().path(linkInfo.path()))
                .rel(linkInfo.rel());
        if (linkInfo.title() != null) {
            builder.title(linkInfo.title());
        }

        if (linkInfo.type() != null) {
            builder.type(linkInfo.type());
        }

        return builder;
    }

    private Object[] getPathParameterValues(LinkInfo linkInfo, Object instance) {
        List<Object> values = new ArrayList<>(linkInfo.pathParameters().size());
        for (String name : linkInfo.pathParameters()) {
            GetterAccessor accessor = getterAccessorsContainer.get(linkInfo.entityType(), name);
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
