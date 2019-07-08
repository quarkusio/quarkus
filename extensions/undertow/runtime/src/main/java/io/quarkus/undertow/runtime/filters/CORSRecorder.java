package io.quarkus.undertow.runtime.filters;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;

import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.undertow.runtime.HttpConfig;
import io.undertow.servlet.ServletExtension;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.util.ImmediateInstanceFactory;

@Recorder
public class CORSRecorder {

    public CORSServletExtension buildCORSExtension() {
        return new CORSServletExtension();
    }

    public void setHttpConfig(HttpConfig config) {
        CORSFilter.corsConfig = config.cors;
    }

    public static class CORSServletExtension implements ServletExtension {

        @Override
        public void handleDeployment(DeploymentInfo deploymentInfo, ServletContext servletContext) {
            CORSFilter filter = new CORSFilter();
            ImmediateInstanceFactory<CORSFilter> filterFactory = new ImmediateInstanceFactory<>(filter);
            FilterInfo filterInfo = new FilterInfo(CORSFilter.class.getName(), CORSFilter.class, filterFactory);
            filterInfo.setAsyncSupported(true);
            deploymentInfo.addFilter(filterInfo);
            // Mappings
            deploymentInfo.addFilterServletNameMapping(CORSFilter.class.getName(), "*", DispatcherType.ERROR);
            deploymentInfo.addFilterServletNameMapping(CORSFilter.class.getName(), "*", DispatcherType.FORWARD);
            deploymentInfo.addFilterServletNameMapping(CORSFilter.class.getName(), "*", DispatcherType.INCLUDE);
            deploymentInfo.addFilterServletNameMapping(CORSFilter.class.getName(), "*", DispatcherType.REQUEST);
            deploymentInfo.addFilterServletNameMapping(CORSFilter.class.getName(), "*", DispatcherType.ASYNC);
        }
    }
}
