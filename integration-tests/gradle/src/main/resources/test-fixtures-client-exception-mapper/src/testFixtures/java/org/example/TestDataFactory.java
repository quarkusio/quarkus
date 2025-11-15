package org.example;

import java.util.List;
import java.util.Set;

public class TestDataFactory {
    
    public static MyRemoteService.Extension createExtension(String id, String name) {
        MyRemoteService.Extension extension = new MyRemoteService.Extension();
        extension.id = id;
        extension.name = name;
        extension.shortName = name.substring(0, Math.min(10, name.length()));
        extension.keywords = List.of("test", "mock");
        return extension;
    }
    
    public static Set<MyRemoteService.Extension> createExtensionSet() {
        return Set.of(
            createExtension("io.quarkus:quarkus-hibernate-validator", "Hibernate Validator"),
            createExtension("io.quarkus:quarkus-rest", "REST")
        );
    }
}