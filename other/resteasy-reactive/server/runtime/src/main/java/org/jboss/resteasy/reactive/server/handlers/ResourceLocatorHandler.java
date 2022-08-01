package org.jboss.resteasy.reactive.server.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.CompletionCallback;
import javax.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.mapping.RequestMapper;
import org.jboss.resteasy.reactive.server.mapping.RuntimeResource;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;
import org.jboss.resteasy.reactive.spi.BeanFactory;

public class ResourceLocatorHandler implements ServerRestHandler {

    private final Map<Class<?>, Map<String, RequestMapper<RuntimeResource>>> resourceLocatorHandlers = new ConcurrentHashMap<>();
    private final Function<Class<?>, BeanFactory.BeanInstance<?>> instantiator;

    public ResourceLocatorHandler(Function<Class<?>, BeanFactory.BeanInstance<?>> instantiator) {
        this.instantiator = instantiator;
    }

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        Object locator = requestContext.getResult();
        if (locator == null) {
            return;
        }
        Class<?> locatorClass;
        if (locator instanceof Class) {
            locatorClass = (Class<?>) locator;
            BeanFactory.BeanInstance<?> instance = instantiator.apply(locatorClass);
            requestContext.registerCompletionCallback(new CompletionCallback() {
                @Override
                public void onComplete(Throwable throwable) {
                    instance.close();
                }
            });
            locator = instance.getInstance();
            if (locator == null) {
                //TODO: we should make sure ArC always picks up these classes and makes them beans
                //but until we get a bug report about it lets not worry for now, as I don't think anyone
                //really uses this
                locator = locatorClass.getDeclaredConstructor().newInstance();
            }
        } else {
            locatorClass = locator.getClass();
        }
        Map<String, RequestMapper<RuntimeResource>> target = findTarget(locatorClass);
        if (target == null) {
            throw new RuntimeException("Resource locator method returned object that was not a resource: " + locator);
        }
        RequestMapper<RuntimeResource> mapper = target.get(requestContext.getMethod());
        boolean hadNullMethodMapper = false;
        if (mapper == null) {
            mapper = target.get(null); //another layer of resource locators maybe
            // we set this without checking if we matched, but we only use it after
            // we check for a null mapper, so by the time we use it, it must have meant that
            // we had a matcher for a null method
            hadNullMethodMapper = true;
        }
        if (mapper == null) {
            throw new WebApplicationException(Response.status(Response.Status.METHOD_NOT_ALLOWED.getStatusCode()).build());
        }
        RequestMapper.RequestMatch<RuntimeResource> res = mapper
                .map(requestContext.getRemaining().isEmpty() ? "/" : requestContext.getRemaining());
        if (res == null) {
            // the TCK checks for both these return statuses
            if (hadNullMethodMapper)
                throw new WebApplicationException(Response.status(Response.Status.METHOD_NOT_ALLOWED.getStatusCode()).build());
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND.getStatusCode()).build());
        }
        requestContext.saveUriMatchState();
        requestContext.setRemaining(res.remaining);
        requestContext.setEndpointInstance(locator);
        requestContext.setResult(null);
        requestContext.restart(res.value, true);
        requestContext.setMaxPathParams(res.pathParamValues.length);
        for (int i = 0; i < res.pathParamValues.length; ++i) {
            String pathParamValue = res.pathParamValues[i];
            if (pathParamValue == null) {
                break;
            }
            requestContext.setPathParamValue(i, pathParamValue);
        }

    }

    private Map<String, RequestMapper<RuntimeResource>> findTarget(Class<?> locatorClass) {
        if (locatorClass == Object.class || locatorClass == null) {
            return null;
        }
        Map<String, RequestMapper<RuntimeResource>> res = resourceLocatorHandlers.get(locatorClass);
        if (res != null) {
            return res;
        }
        //not found, so we need to compute one
        //we look through all interfaces and superclasses
        //we need to do this as it could implement multiple interfaces
        List<Map<String, RequestMapper<RuntimeResource>>> results = new ArrayList<>();
        Set<Class<?>> seen = new HashSet<>();
        findTargetRecursive(locatorClass, results, seen);
        Map<String, List<RequestMapper.RequestPath<RuntimeResource>>> newMapper = new HashMap<>();
        for (Map<String, RequestMapper<RuntimeResource>> i : results) {
            for (Map.Entry<String, RequestMapper<RuntimeResource>> entry : i.entrySet()) {
                List<RequestMapper.RequestPath<RuntimeResource>> list = newMapper.get(entry.getKey());
                if (list == null) {
                    newMapper.put(entry.getKey(), list = new ArrayList<>());
                }
                list.addAll(entry.getValue().getTemplates());
            }
        }
        Map<String, RequestMapper<RuntimeResource>> finalResult = new HashMap<>();
        for (Map.Entry<String, List<RequestMapper.RequestPath<RuntimeResource>>> i : newMapper.entrySet()) {
            finalResult.put(i.getKey(), new RequestMapper<RuntimeResource>(i.getValue()));
        }
        //it does not matter if this is computed twice
        resourceLocatorHandlers.put(locatorClass, finalResult);
        return finalResult;
    }

    private void findTargetRecursive(Class<?> locatorClass, List<Map<String, RequestMapper<RuntimeResource>>> found,
            Set<Class<?>> seen) {
        if (locatorClass == Object.class || locatorClass == null) {
            return;
        }
        boolean superRequired = true;
        Map<String, RequestMapper<RuntimeResource>> res = resourceLocatorHandlers.get(locatorClass);
        if (res != null) {
            found.add(res);
            superRequired = false;
        }
        for (Class<?> iface : locatorClass.getInterfaces()) {
            if (seen.contains(iface)) {
                continue;
            }
            seen.add(iface);
            res = resourceLocatorHandlers.get(iface);
            if (res != null) {
                found.add(res);
            }
            for (Class<?> i : iface.getInterfaces()) {
                findTargetRecursive(i, found, seen);
            }
        }
        if (superRequired) {
            findTargetRecursive(locatorClass.getSuperclass(), found, seen);
        }
    }

    public void addResource(Class<?> resourceClass, Map<String, RequestMapper<RuntimeResource>> requestMapper) {
        Class<?> c = resourceClass;
        resourceLocatorHandlers.put(c, requestMapper);

    }
}
