package io.quarkus.qute.runtime.extensions;

import java.util.List;

import javax.enterprise.inject.Vetoed;

import io.quarkus.qute.Results.Result;
import io.quarkus.qute.TemplateExtension;

@Vetoed // Make sure no bean is created from this class
@TemplateExtension
public class CollectionTemplateExtensions {

    static Object get(List<?> list, int index) {
        return list.get(index);
    }

    @TemplateExtension(matchRegex = "\\d{1,10}")
    static Object getByIndex(List<?> list, String index) {
        int idx = Integer.parseInt(index);
        if (idx >= list.size()) {
            // Be consistent with property resolvers
            return Result.NOT_FOUND;
        }
        return list.get(idx);
    }
}
