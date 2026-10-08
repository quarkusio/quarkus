package io.quarkus.resteasy.runtime;

import java.util.Collection;

import jakarta.enterprise.event.Observes;
import jakarta.servlet.ServletContext;

import org.jboss.resteasy.spi.Registry;

import io.quarkus.runtime.StartupEvent;

/**
 * Runs the {@link StaticResourceShadowingCheck} when RESTEasy runs on the servlet container, see
 * {@code ResteasyServletProcessor}.
 */
public class ServletStaticResourceShadowingCheck {

    // set by ResteasyServletRecorder, and cleared by the check as they are only needed then
    private static Collection<String> staticFiles;
    private static Collection<String> welcomeFiles;

    /**
     * Sets the static resources the default servlet serves, see {@link ResteasyServletRecorder#setStaticResources}.
     */
    static void setStaticResources(Collection<String> staticFiles, Collection<String> welcomeFiles) {
        ServletStaticResourceShadowingCheck.staticFiles = staticFiles;
        ServletStaticResourceShadowingCheck.welcomeFiles = welcomeFiles;
    }

    void onStart(@Observes StartupEvent event, ServletContext servletContext) {
        Collection<String> files = staticFiles;
        Collection<String> indexPages = welcomeFiles;
        staticFiles = null;
        welcomeFiles = null;
        // set by ResteasyFilter
        Registry registry = (Registry) servletContext.getAttribute(Registry.class.getName());
        if (registry != null && files != null) {
            String contextPath = servletContext.getContextPath();
            // the default servlet serves a directory containing a welcome file, see UndertowDeploymentRecorder#createDeployment
            new StaticResourceShadowingCheck(registry, contextPath).check(contextPath, files, indexPages);
        }
    }
}
