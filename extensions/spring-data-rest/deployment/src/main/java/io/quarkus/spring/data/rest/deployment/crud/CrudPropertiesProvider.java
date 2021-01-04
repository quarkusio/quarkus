package io.quarkus.spring.data.rest.deployment.crud;

import static io.quarkus.spring.data.rest.deployment.crud.CrudMethodsImplementor.ADD;
import static io.quarkus.spring.data.rest.deployment.crud.CrudMethodsImplementor.DELETE;
import static io.quarkus.spring.data.rest.deployment.crud.CrudMethodsImplementor.GET;
import static io.quarkus.spring.data.rest.deployment.crud.CrudMethodsImplementor.LIST;
import static io.quarkus.spring.data.rest.deployment.crud.CrudMethodsImplementor.UPDATE;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import io.quarkus.spring.data.rest.deployment.ResourcePropertiesProvider;

public class CrudPropertiesProvider extends ResourcePropertiesProvider {

    public CrudPropertiesProvider(IndexView index) {
        super(index, false);
    }

    protected Map<String, Predicate<MethodInfo>> getMethodPredicates() {
        Map<String, Predicate<MethodInfo>> methodPredicates = new HashMap<>();
        methodPredicates.put("list",
                methodInfo -> methodInfo.name().equals(LIST.getName()) && methodInfo.parameters().isEmpty());
        methodPredicates.put("get", methodInfo -> methodInfo.name().equals(GET.getName()));
        methodPredicates.put("add", methodInfo -> methodInfo.name().equals(ADD.getName()));
        methodPredicates.put("update", methodInfo -> methodInfo.name().equals(UPDATE.getName()));
        methodPredicates.put("delete", methodInfo -> methodInfo.name().equals(DELETE.getName()));
        return methodPredicates;
    }
}
