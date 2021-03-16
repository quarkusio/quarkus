package io.quarkus.rest.data.panache.runtime.hal;

import java.util.HashMap;
import java.util.Map;

public class BookHalLinksProvider implements HalLinksProvider {

    @Override
    public Map<String, HalLink> getLinks(Class<?> entityClass) {
        Map<String, HalLink> links = new HashMap<>(2);
        links.put("list", new HalLink("/books"));
        links.put("add", new HalLink("/books"));

        return links;
    }

    @Override
    public Map<String, HalLink> getLinks(Object entity) {
        Book book = (Book) entity;
        Map<String, HalLink> links = new HashMap<>(4);
        links.put("list", new HalLink("/books"));
        links.put("add", new HalLink("/books"));
        links.put("self", new HalLink("/books/" + book.id));
        links.put("update", new HalLink("/books/" + book.id));

        return links;
    }
}
