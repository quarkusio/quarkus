package io.quarkus.hal;

import java.util.Map;

import jakarta.ws.rs.core.Link;

public abstract class HalWrapper {

    private final Map<String, HalLink> links;

    public HalWrapper(Map<String, HalLink> links) {
        this.links = links;
    }

    public Map<String, HalLink> getLinks() {
        return links;
    }

    /**
     * This method is used by Rest Data Panache to programmatically add links to the Hal wrapper.
     *
     * @param links The links to add into the Hal wrapper.
     */
    @SuppressWarnings("unused")
    public void addLinks(Link... links) {
        for (Link link : links) {
            this.links.put(link.getRel(), new HalLink(link.getUri().toString()));
        }
    }
}
