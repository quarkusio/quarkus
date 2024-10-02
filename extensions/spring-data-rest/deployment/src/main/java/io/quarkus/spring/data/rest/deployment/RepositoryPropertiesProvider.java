package io.quarkus.spring.data.rest.deployment;

import static io.quarkus.spring.data.rest.deployment.RepositoryMethodsImplementor.ADD;
import static io.quarkus.spring.data.rest.deployment.RepositoryMethodsImplementor.DELETE;
import static io.quarkus.spring.data.rest.deployment.RepositoryMethodsImplementor.GET;
import static io.quarkus.spring.data.rest.deployment.RepositoryMethodsImplementor.LIST;
import static io.quarkus.spring.data.rest.deployment.RepositoryMethodsImplementor.LIST_ITERABLE;
import static io.quarkus.spring.data.rest.deployment.RepositoryMethodsImplementor.LIST_PAGED;
import static io.quarkus.spring.data.rest.deployment.RepositoryMethodsImplementor.SAVE_LIST;
import static io.quarkus.spring.data.rest.deployment.RepositoryMethodsImplementor.UPDATE;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.springframework.data.domain.Pageable;

public class RepositoryPropertiesProvider extends ResourcePropertiesProvider {

    private static final DotName PAGEABLE = DotName.createSimple(Pageable.class.getName());

    public RepositoryPropertiesProvider(IndexView index, boolean paged) {
        super(index, paged);
    }

    protected Map<String, Predicate<MethodInfo>> getMethodPredicates() {
        Map<String, Predicate<MethodInfo>> methodPredicates = new HashMap<>();
        methodPredicates.put("list", methodInfo -> methodInfo.name().equals(LIST.getName()));
        methodPredicates.put("listIterable", methodInfo -> methodInfo.name().equals(LIST_ITERABLE.getName()));
        methodPredicates.put("listPaged", methodInfo -> methodInfo.name().equals(LIST_PAGED.getName())
                && methodInfo.parametersCount() == 1
                && methodInfo.parameterType(0).name().equals(PAGEABLE));
        methodPredicates.put("addAll", methodInfo -> methodInfo.name().equals(SAVE_LIST.getName()));
        methodPredicates.put("get", methodInfo -> methodInfo.name().equals(GET.getName()));
        methodPredicates.put("add", methodInfo -> methodInfo.name().equals(ADD.getName()));
        methodPredicates.put("update", methodInfo -> methodInfo.name().equals(UPDATE.getName()));
        methodPredicates.put("delete", methodInfo -> methodInfo.name().equals(DELETE.getName()));
        return methodPredicates;
    }
}
