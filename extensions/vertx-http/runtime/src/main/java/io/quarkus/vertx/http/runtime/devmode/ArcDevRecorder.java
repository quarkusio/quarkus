package io.quarkus.vertx.http.runtime.devmode;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableObserverMethod;
import io.quarkus.arc.RemovedBean;
import io.quarkus.arc.impl.ArcContainerImpl;
import io.quarkus.arc.runtime.devconsole.InvocationsMonitor;
import io.quarkus.arc.runtime.devmode.EventsMonitor;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.devconsole.runtime.spi.DevConsolePostHandler;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.devmode.Json.JsonArrayBuilder;
import io.quarkus.vertx.http.runtime.devmode.Json.JsonObjectBuilder;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class ArcDevRecorder {

    public Handler<RoutingContext> createSummaryHandler(Map<String, String> configProperties, String rootPath,
            int removedInterceptorsDecorators) {
        return new Handler<RoutingContext>() {

            @Override
            public void handle(RoutingContext ctx) {
                ctx.response().putHeader("Content-Type", "application/json");
                ArcContainerImpl container = ArcContainerImpl.instance();
                JsonObjectBuilder summary = Json.object();
                summary.put("beans", container.getBeans().size());
                summary.put("removedBeans", container.getRemovedBeans().size() + removedInterceptorsDecorators);
                summary.put("observers", container.getObservers().size());
                summary.put("interceptors", container.getInterceptors().size());
                JsonArrayBuilder scopes = Json.array();
                container.getScopes().stream().map(Class::getName).forEach(scopes::add);
                summary.put("scopes", scopes);
                JsonObjectBuilder config = Json.object();
                for (Entry<String, String> entry : configProperties.entrySet()) {
                    if (entry.getValue().equals("true") || entry.getValue().equals("false")) {
                        config.put(entry.getKey(), Boolean.parseBoolean(entry.getValue()));
                    } else {
                        config.put(entry.getKey(), entry.getValue());
                    }
                }
                summary.put("config", config);
                JsonObjectBuilder links = Json.object();
                links.put("beans", rootPath + "arc/beans");
                links.put("observers", rootPath + "arc/observers");
                links.put("removed-beans", rootPath + "arc/removed-beans");
                summary.put("links", links);
                ctx.response().end(summary.build());
            }
        };
    }

    public Handler<RoutingContext> createBeansHandler(String beanDependenciesGlobalKey) {
        return new Handler<RoutingContext>() {

            @Override
            public void handle(RoutingContext ctx) {
                ctx.response().putHeader("Content-Type", "application/json");

                ArcContainerImpl container = ArcContainerImpl.instance();
                List<InjectableBean<?>> beans = new ArrayList<>();
                beans.addAll(container.getBeans());
                beans.addAll(container.getInterceptors());
                beans.addAll(container.getDecorators());

                String kindParam = ctx.request().getParam("kind");
                InjectableBean.Kind kind = kindParam != null ? InjectableBean.Kind.from(kindParam.toUpperCase()) : null;
                String scopeEndsWith = ctx.request().getParam("scope");
                String beanClassStartsWith = ctx.request().getParam("beanClass");

                for (Iterator<InjectableBean<?>> it = beans.iterator(); it.hasNext();) {
                    InjectableBean<?> injectableBean = it.next();
                    if (kind != null && !kind.equals(injectableBean.getKind())) {
                        it.remove();
                    }
                    if (scopeEndsWith != null && !injectableBean.getScope().getName().endsWith(scopeEndsWith)) {
                        it.remove();
                    }
                    if (beanClassStartsWith != null
                            && !injectableBean.getBeanClass().getName().startsWith(beanClassStartsWith)) {
                        it.remove();
                    }
                }

                Map<String, List<String>> beanDependenciesMap = DevConsoleManager.getGlobal(beanDependenciesGlobalKey);

                JsonArrayBuilder array = Json.array();
                for (InjectableBean<?> injectableBean : beans) {
                    JsonObjectBuilder bean = Json.object(true);
                    bean.put("id", injectableBean.getIdentifier());
                    bean.put("kind", injectableBean.getKind().toString());
                    bean.put("generatedClass", injectableBean.getClass().getName());
                    bean.put("beanClass", injectableBean.getBeanClass().getName());
                    JsonArrayBuilder types = Json.array();
                    for (Type beanType : injectableBean.getTypes()) {
                        types.add(beanType.getTypeName());
                    }
                    bean.put("types", types);
                    JsonArrayBuilder qualifiers = Json.array();
                    for (Annotation qualifier : injectableBean.getQualifiers()) {
                        if (qualifier.annotationType().equals(Any.class) || qualifier.annotationType().equals(Default.class)) {
                            qualifiers.add("@" + qualifier.annotationType().getSimpleName());
                        } else {
                            qualifiers.add(qualifier.toString());
                        }
                    }
                    bean.put("qualifiers", qualifiers);
                    bean.put("scope", injectableBean.getScope().getName());

                    if (injectableBean.getDeclaringBean() != null) {
                        bean.put("declaringBean", injectableBean.getDeclaringBean().getIdentifier());
                    }
                    if (injectableBean.getName() != null) {
                        bean.put("name", injectableBean.getName());
                    }
                    if (injectableBean.isAlternative()) {
                        bean.put("alternativePriority", injectableBean.getAlternativePriority());
                    }
                    if (injectableBean.isDefaultBean()) {
                        bean.put("isDefault", true);
                    }

                    List<String> beanDependencies = Collections.emptyList();
                    if (beanDependenciesMap != null) {
                        beanDependencies = beanDependenciesMap.getOrDefault(injectableBean.getIdentifier(),
                                Collections.emptyList());
                    }
                    JsonArrayBuilder dependencies = Json.array();
                    for (String beanId : beanDependencies) {
                        dependencies.add(beanId);
                    }
                    bean.put("dependencies", dependencies);

                    array.add(bean);
                }
                ctx.response().end(array.build());
            }
        };
    }

    public Handler<RoutingContext> createObserversHandler() {
        return new Handler<RoutingContext>() {

            @Override
            public void handle(RoutingContext ctx) {
                ctx.response().putHeader("Content-Type", "application/json");

                ArcContainerImpl container = ArcContainerImpl.instance();
                List<InjectableObserverMethod<?>> observers = container.getObservers();

                JsonArrayBuilder array = Json.array();
                for (InjectableObserverMethod<?> injectableObserver : observers) {
                    JsonObjectBuilder observer = Json.object();
                    observer.put("generatedClass", injectableObserver.getClass().getName());
                    observer.put("observedType", injectableObserver.getObservedType().getTypeName());
                    if (!injectableObserver.getObservedQualifiers().isEmpty()) {
                        JsonArrayBuilder qualifiers = Json.array();
                        for (Annotation qualifier : injectableObserver.getObservedQualifiers()) {
                            qualifiers.add(qualifier.toString());
                        }
                        observer.put("qualifiers", qualifiers);
                    }
                    observer.put("priority", injectableObserver.getPriority());
                    observer.put("reception", injectableObserver.getReception().toString());
                    observer.put("transactionPhase", injectableObserver.getTransactionPhase().toString());
                    observer.put("async", injectableObserver.isAsync());
                    if (injectableObserver.getDeclaringBeanIdentifier() != null) {
                        observer.put("declaringBean", injectableObserver.getDeclaringBeanIdentifier());
                    }
                    array.add(observer);
                }
                ctx.response().end(array.build());
            }
        };
    }

    public Handler<RoutingContext> createRemovedBeansHandler(List<String[]> removedInterceptorsDecorators) {
        return new Handler<RoutingContext>() {

            @Override
            public void handle(RoutingContext ctx) {
                ArcContainerImpl container = ArcContainerImpl.instance();
                ctx.response().putHeader("Content-Type", "application/json");
                JsonArrayBuilder removed = Json.array();
                for (RemovedBean removedBean : container.getRemovedBeans()) {
                    JsonObjectBuilder bean = Json.object();
                    bean.put("kind", removedBean.getKind().toString());
                    bean.put("description", removedBean.getDescription());
                    JsonArrayBuilder types = Json.array();
                    for (Type beanType : removedBean.getTypes()) {
                        types.add(beanType instanceof Class ? ((Class<?>) beanType).getName() : beanType.toString());
                    }
                    // java.lang.Object is always skipped
                    types.add(Object.class.getName());
                    bean.put("types", types);
                    JsonArrayBuilder qualifiers = Json.array();
                    for (Annotation qualifier : removedBean.getQualifiers()) {
                        if (qualifier.annotationType().equals(Any.class) || qualifier.annotationType().equals(Default.class)) {
                            qualifiers.add("@" + qualifier.annotationType().getSimpleName());
                        } else {
                            qualifiers.add(qualifier.toString());
                        }
                    }
                    bean.put("qualifiers", qualifiers);
                    removed.add(bean);
                }
                for (String[] interceptorDecorator : removedInterceptorsDecorators) {
                    JsonObjectBuilder bean = Json.object();
                    bean.put("kind", interceptorDecorator[0]);
                    bean.put("description", interceptorDecorator[1]);
                    removed.add(bean);
                }
                ctx.response().end(removed.build());
            }
        };
    }

    // NOTE: we can't add this recorder to the ArC extension as it would cause a cyclic dependency
    public Handler<RoutingContext> events() {
        return new DevConsolePostHandler() {
            @Override
            protected void handlePost(RoutingContext event, MultiMap form)
                    throws Exception {
                String action = form.get("action");
                if ("skipContext".equals(action)) {
                    Arc.container().instance(EventsMonitor.class).get().toggleSkipContextEvents();
                } else {
                    Arc.container().instance(EventsMonitor.class).get().clear();
                }
            }
        };
    }

    // NOTE: we can't add this recorder to the ArC extension as it would cause a cyclic dependency
    public Handler<RoutingContext> invocations() {
        return new DevConsolePostHandler() {
            @Override
            protected void handlePost(RoutingContext event, MultiMap form)
                    throws Exception {
                String action = form.get("action");
                if ("filterOutQuarkusBeans".equals(action)) {
                    Arc.container().instance(InvocationsMonitor.class).get().toggleFilterOutQuarkusBeans();
                } else {
                    Arc.container().instance(InvocationsMonitor.class).get().clear();
                }
            }
        };
    }

}
