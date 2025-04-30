package io.quarkus.resteasy.reactive.links.deployment;

import org.jboss.jandex.DotName;

import io.quarkus.resteasy.reactive.links.InjectRestLinks;
import io.quarkus.resteasy.reactive.links.RestLink;

final class DotNames {

    static final DotName INJECT_REST_LINKS_ANNOTATION = DotName.createSimple(InjectRestLinks.class.getName());
    static final DotName REST_LINK_ANNOTATION = DotName.createSimple(RestLink.class.getName());

    private DotNames() {
    }
}
