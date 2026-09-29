package io.quarkus.resteasy.runtime;

import java.util.List;
import java.util.Set;

import io.quarkus.runtime.annotations.Recorder;

/**
 * Provides the runtime methods for RESTEasy on the servlet container.
 */
@Recorder
public class ResteasyServletRecorder {

    /**
     * Passes the static resources the default servlet serves to the {@link ServletStaticResourceShadowingCheck}, which
     * runs when the application starts.
     *
     * @param staticFiles the paths of the static files, relative to the servlet context path and without the leading slash
     * @param welcomeFiles the files the default servlet serves for the directories containing them
     */
    public void setStaticResources(Set<String> staticFiles, List<String> welcomeFiles) {
        ServletStaticResourceShadowingCheck.setStaticResources(staticFiles, welcomeFiles);
    }
}
