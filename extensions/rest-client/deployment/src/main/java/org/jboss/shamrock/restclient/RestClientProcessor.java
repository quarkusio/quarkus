/*
 * Copyright 2018 Red Hat, Inc.
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

package org.jboss.shamrock.restclient;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.ext.Providers;

import org.apache.commons.logging.impl.Jdk14Logger;
import org.apache.commons.logging.impl.LogFactoryImpl;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.protean.arc.processor.BeanConfigurator;
import org.jboss.protean.arc.processor.BeanRegistrar;
import org.jboss.protean.arc.processor.ScopeInfo;
import org.jboss.protean.gizmo.MethodDescriptor;
import org.jboss.protean.gizmo.ResultHandle;
import org.jboss.resteasy.client.jaxrs.internal.proxy.ResteasyClientProxy;
import org.jboss.resteasy.spi.ResteasyConfiguration;
import org.jboss.shamrock.arc.deployment.AdditionalBeanBuildItem;
import org.jboss.shamrock.arc.deployment.BeanRegistrarBuildItem;
import org.jboss.shamrock.deployment.annotations.BuildProducer;
import org.jboss.shamrock.deployment.annotations.BuildStep;
import org.jboss.shamrock.deployment.builditem.CombinedIndexBuildItem;
import org.jboss.shamrock.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import org.jboss.shamrock.deployment.builditem.FeatureBuildItem;
import org.jboss.shamrock.deployment.builditem.GeneratedClassBuildItem;
import org.jboss.shamrock.deployment.builditem.SslNativeConfigBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveClassBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.SubstrateProxyDefinitionBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.SubstrateResourceBuildItem;
import org.jboss.shamrock.deployment.util.ServiceUtil;
import org.jboss.shamrock.restclient.runtime.DefaultResponseExceptionMapper;
import org.jboss.shamrock.restclient.runtime.RestClientBase;
import org.jboss.shamrock.restclient.runtime.RestClientProxy;

class RestClientProcessor {

    private static final DotName REST_CLIENT = DotName.createSimple(RestClient.class.getName());

    private static final DotName REGISTER_REST_CLIENT = DotName.createSimple(RegisterRestClient.class.getName());

    private static final String PROVIDERS_SERVICE_FILE = "META-INF/services/" + Providers.class.getName();

    @Inject
    BuildProducer<GeneratedClassBuildItem> generatedClass;

    @Inject
    BuildProducer<ReflectiveClassBuildItem> reflectiveClass;

    @Inject
    BuildProducer<AdditionalBeanBuildItem> additionalBeans;

    @Inject
    BuildProducer<SubstrateProxyDefinitionBuildItem> proxyDefinition;

    @Inject
    BuildProducer<SubstrateResourceBuildItem> resources;

    @Inject
    BuildProducer<BeanRegistrarBuildItem> beanRegistrars;

    @Inject
    BuildProducer<FeatureBuildItem> feature;

    @Inject
    CombinedIndexBuildItem combinedIndexBuildItem;

    @Inject
    SslNativeConfigBuildItem SslNativeConfigBuildItem;

    @Inject
    BuildProducer<ExtensionSslNativeSupportBuildItem> extensionSslNativeSupport;

    @BuildStep
    public void build() throws Exception {
        feature.produce(new FeatureBuildItem(FeatureBuildItem.MP_REST_CLIENT));
    	reflectiveClass.produce(new ReflectiveClassBuildItem(false, false,
                DefaultResponseExceptionMapper.class.getName(),
                LogFactoryImpl.class.getName(),
                Jdk14Logger.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, ClientRequestFilter[].class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, ClientResponseFilter[].class.getName()));
        proxyDefinition.produce(new SubstrateProxyDefinitionBuildItem("javax.ws.rs.ext.Providers"));
        additionalBeans.produce(new AdditionalBeanBuildItem(RestClient.class));

        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, "org.jboss.resteasy.core.ResteasyProviderFactoryImpl", "org.jboss.resteasy.client.jaxrs.internal.proxy.ProxyBuilderImpl"));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, javax.ws.rs.ext.ReaderInterceptor[].class.getName()));

        // for now, register all the providers for reflection. This is not something we want to keep but not having it generates a pile of warnings.
        // we will improve that later with the SmallRye REST client.
        resources.produce(new SubstrateResourceBuildItem(PROVIDERS_SERVICE_FILE));
        for (String provider : ServiceUtil.classNamesNamedIn(getClass().getClassLoader(), PROVIDERS_SERVICE_FILE)) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, provider));
        }
        // This abstract one is also accessed directly via reflection
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, "org.jboss.resteasy.plugins.providers.jsonb.AbstractJsonBindingProvider"));

        proxyDefinition.produce(new SubstrateProxyDefinitionBuildItem(ResteasyConfiguration.class.getName()));

        // According to the spec only rest client interfaces annotated with RegisterRestClient are registered as beans
        Map<DotName, ClassInfo> interfaces = new HashMap<>();
        for (AnnotationInstance annotation : combinedIndexBuildItem.getIndex().getAnnotations(REGISTER_REST_CLIENT)) {
            AnnotationTarget target = annotation.target();
            ClassInfo theInfo;
            if (target.kind() == AnnotationTarget.Kind.CLASS) {
                theInfo = target.asClass();
            } else if (target.kind() == AnnotationTarget.Kind.METHOD) {
                theInfo = target.asMethod().declaringClass();
            } else {
                continue;
            }
            if (!Modifier.isInterface(theInfo.flags())) {
                continue;
            }
            interfaces.put(theInfo.name(), theInfo);
        }

        if (interfaces.isEmpty()) {
            return;
        }

        for (Map.Entry<DotName, ClassInfo> entry : interfaces.entrySet()) {
            String iName = entry.getKey().toString();
            proxyDefinition.produce(new SubstrateProxyDefinitionBuildItem(iName, ResteasyClientProxy.class.getName()));
            proxyDefinition.produce(new SubstrateProxyDefinitionBuildItem(iName, RestClientProxy.class.getName()));
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, iName));
        }

        BeanRegistrar beanRegistrar = new BeanRegistrar() {

            @Override
            public void register(RegistrationContext registrationContext) {
                for (Map.Entry<DotName, ClassInfo> entry : interfaces.entrySet()) {
                    BeanConfigurator<Object> configurator = registrationContext.configure(entry.getKey());
                    // The spec is not clear whether we should add superinterfaces too - let's keep aligned with SmallRye for now
                    configurator.addType(entry.getKey());
                    // We use @Singleton here as we do not need another proxy
                    configurator.scope(ScopeInfo.SINGLETON);
                    configurator.addQualifier(REST_CLIENT);
                    configurator.creator(m -> {
                        // return new RestClientBase(proxyType).create();
                        ResultHandle interfaceHandle = m.loadClass(entry.getKey().toString());
                        ResultHandle baseHandle = m.newInstance(MethodDescriptor.ofConstructor(RestClientBase.class, Class.class), interfaceHandle);
                        ResultHandle ret = m.invokeVirtualMethod(MethodDescriptor.ofMethod(RestClientBase.class, "create", Object.class), baseHandle);
                        m.returnValue(ret);
                    });
                    configurator.done();
                }
            }
        };
        beanRegistrars.produce(new BeanRegistrarBuildItem(beanRegistrar));

        // Indicates that this extension would like the SSL support to be enabled
        extensionSslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(FeatureBuildItem.MP_REST_CLIENT));
    }
}
