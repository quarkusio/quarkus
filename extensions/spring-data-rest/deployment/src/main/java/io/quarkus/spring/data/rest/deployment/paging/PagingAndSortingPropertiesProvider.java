package io.quarkus.spring.data.rest.deployment.paging;

import static io.quarkus.spring.data.rest.deployment.crud.CrudMethodsImplementor.ADD;
import static io.quarkus.spring.data.rest.deployment.crud.CrudMethodsImplementor.DELETE;
import static io.quarkus.spring.data.rest.deployment.crud.CrudMethodsImplementor.GET;
import static io.quarkus.spring.data.rest.deployment.crud.CrudMethodsImplementor.UPDATE;
import static io.quarkus.spring.data.rest.deployment.paging.PagingAndSortingMethodsImplementor.LIST_PAGED;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.springframework.data.domain.Pageable;

import io.quarkus.spring.data.rest.deployment.ResourcePropertiesProvider;

public class PagingAndSortingPropertiesProvider extends ResourcePropertiesProvider {

    private static final DotName PAGEABLE = DotName.createSimple(Pageable.class.getName());

    public PagingAndSortingPropertiesProvider(IndexView index) {
        super(index, true);
    }

    protected Map<String, Predicate<MethodInfo>> getMethodPredicates() {
        Map<String, Predicate<MethodInfo>> methodPredicates = new HashMap<>();
        methodPredicates.put("list", methodInfo -> methodInfo.name().equals(LIST_PAGED.getName())
                && methodInfo.parameters().size() == 1
                && methodInfo.parameters().get(0).name().equals(PAGEABLE));
        methodPredicates.put("get", methodInfo -> methodInfo.name().equals(GET.getName()));
        methodPredicates.put("add", methodInfo -> methodInfo.name().equals(ADD.getName()));
        methodPredicates.put("update", methodInfo -> methodInfo.name().equals(UPDATE.getName()));
        methodPredicates.put("delete", methodInfo -> methodInfo.name().equals(DELETE.getName()));
        return methodPredicates;
    }
}
