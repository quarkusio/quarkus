package org.jboss.resteasy.reactive.server.processor.generation.filters;

import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.resteasy.reactive.common.model.ResourceInterceptor;
import org.jboss.resteasy.reactive.server.processor.ScannedApplication;
import org.jboss.resteasy.reactive.server.processor.scanning.FeatureScanner;
import org.jboss.resteasy.reactive.server.processor.util.GeneratedClass;

public class FilterFeature implements FeatureScanner {

    final Set<DotName> unwrappableTypes;
    final Set<String> additionalBeanAnnotations;

    /**
     *
     * @param unwrappableTypes Types that can be unwrapped using
     *        {@link org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext#unwrap(Class)}
     * @param additionalBeanAnnotations Annotations that should be added to generated beans
     */
    public FilterFeature(Set<DotName> unwrappableTypes, Set<String> additionalBeanAnnotations) {
        this.unwrappableTypes = unwrappableTypes;
        this.additionalBeanAnnotations = additionalBeanAnnotations;
    }

    @Override
    public FeatureScanResult integrate(IndexView application, ScannedApplication scannedApplication) {
        List<GeneratedClass> generatedClasses = new ArrayList<>();
        List<FilterGeneration.GeneratedFilter> result = FilterGeneration.generate(application, unwrappableTypes,
                additionalBeanAnnotations);
        for (var i : result) {
            generatedClasses.addAll(i.getGeneratedClasses());
            if (i.isRequestFilter()) {
                ResourceInterceptor<ContainerRequestFilter> request = scannedApplication.getResourceInterceptors()
                        .getContainerRequestFilters().create();
                request.setClassName(i.generatedClassName);
                request.setPriority(i.priority);
                request.setNonBlockingRequired(i.nonBlocking);
                request.setNameBindingNames(i.nameBindingNames);
                if (i.isPreMatching()) {
                    scannedApplication.getResourceInterceptors().getContainerRequestFilters().addPreMatchInterceptor(request);
                } else if (!i.getNameBindingNames().isEmpty()) {
                    scannedApplication.getResourceInterceptors().getContainerRequestFilters()
                            .addNameRequestInterceptor(request);
                } else {
                    scannedApplication.getResourceInterceptors().getContainerRequestFilters()
                            .addGlobalRequestInterceptor(request);
                }
            } else {

                ResourceInterceptor<ContainerResponseFilter> request = scannedApplication.getResourceInterceptors()
                        .getContainerResponseFilters().create();
                request.setClassName(i.generatedClassName);
                request.setPriority(i.priority);
                request.setNameBindingNames(i.nameBindingNames);
                if (!i.getNameBindingNames().isEmpty()) {
                    scannedApplication.getResourceInterceptors().getContainerResponseFilters()
                            .addNameRequestInterceptor(request);
                } else {
                    scannedApplication.getResourceInterceptors().getContainerResponseFilters()
                            .addGlobalRequestInterceptor(request);
                }
            }
        }
        return new FeatureScanResult(generatedClasses);
    }
}
