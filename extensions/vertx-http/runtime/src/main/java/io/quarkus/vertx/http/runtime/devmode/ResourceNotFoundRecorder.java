package io.quarkus.vertx.http.runtime.devmode;

import java.util.List;
import java.util.Set;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class ResourceNotFoundRecorder {

    public Handler<RoutingContext> registerNotFoundHandler(RuntimeValue<Router> httpRouter,
            RuntimeValue<Router> mainRouter,
            RuntimeValue<Router> managementRouter,
            BeanContainer beanContainer,
            String baseUrl,
            String httpRoot,
            List<RouteDescription> endpointRoutes,
            Set<String> staticRoots,
            List<AdditionalRouteDescription> additionalEndpoints) {

        ResourceNotFoundData resourceNotFoundData = beanContainer.beanInstance(ResourceNotFoundData.class);
        resourceNotFoundData.setBaseUrl(baseUrl);
        resourceNotFoundData.setHttpRoot(httpRoot);
        resourceNotFoundData.setEndpointRoutes(endpointRoutes);
        resourceNotFoundData.setStaticRoots(staticRoots);
        resourceNotFoundData.setAdditionalEndpoints(additionalEndpoints);

        ResourceNotFoundHandler rbfh = new ResourceNotFoundHandler();

        addErrorHandler(mainRouter, rbfh);
        addErrorHandler(httpRouter, rbfh);
        addErrorHandler(managementRouter, rbfh);

        return rbfh;
    }

    private void addErrorHandler(RuntimeValue<Router> router, ResourceNotFoundHandler rbfh) {
        if (router != null) {
            Router r = router.getValue();
            r.errorHandler(404, rbfh);
        }
    }

}
