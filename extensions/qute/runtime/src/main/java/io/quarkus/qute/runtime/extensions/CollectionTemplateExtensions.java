package io.quarkus.qute.runtime.extensions;

import java.util.List;

import io.quarkus.qute.TemplateExtension;

@TemplateExtension
public class CollectionTemplateExtensions {

    static Object get(List<?> list, int index) {
        return list.get(index);
    }
}