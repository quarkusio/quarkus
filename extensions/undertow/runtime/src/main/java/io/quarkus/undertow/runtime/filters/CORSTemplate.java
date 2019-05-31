/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.undertow.runtime.filters;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;

import io.quarkus.runtime.annotations.Template;
import io.quarkus.undertow.runtime.HttpConfig;
import io.undertow.servlet.ServletExtension;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.util.ImmediateInstanceFactory;

@Template
public class CORSTemplate {

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
