package io.quarkus.jfr.runtime.http.rest.reactive;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.jboss.resteasy.reactive.server.SimpleResourceInfo;

public class ReactiveServerFilters {

    private static final Logger LOG = Logger.getLogger(ReactiveServerFilters.class);

    private final ReactiveServerRecorder recorder;

    public ReactiveServerFilters(ReactiveServerRecorder recorder) {
        this.recorder = recorder;
    }

    /**
     * Executed if request processing proceeded correctly.
     * We now have to update the start event with the resource class and method data and also commit the event.
     */
    @ServerRequestFilter
    public void requestFilter(SimpleResourceInfo resourceInfo) {
        Class<?> resourceClass = resourceInfo.getResourceClass();
        if (resourceClass != null) { // should always be the case
            String resourceClassName = resourceClass.getName();
            String resourceMethodName = resourceInfo.getMethodName();
            recorder
                    .updateResourceInfo(new ResourceInfo(resourceClassName, resourceMethodName))
                    .commitStartEventIfNecessary();
        }

    }

    /**
     * This will execute regardless of a processing failure or not.
     * If there was a failure, we need to check if the start event was not commited
     * (which happens when request was not matched to any resource method) and if so, commit it.
     */
    @ServerResponseFilter
    public void responseFilter() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Enter Jfr Reactive Response Filter");
        }
        recorder
                .recordEndEvent()
                .endPeriodEvent();
    }

}
