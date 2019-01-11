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
import java.util.concurrent.atomic.AtomicReference;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseFilter;

import org.apache.commons.logging.impl.Jdk14Logger;
import org.apache.commons.logging.impl.LogFactoryImpl;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.protean.gizmo.ClassCreator;
import org.jboss.protean.gizmo.ClassOutput;
import org.jboss.protean.gizmo.MethodCreator;
import org.jboss.protean.gizmo.MethodDescriptor;
import org.jboss.protean.gizmo.ResultHandle;
import org.jboss.resteasy.client.jaxrs.internal.proxy.ResteasyClientProxy;
import org.jboss.resteasy.spi.ResteasyConfiguration;
import org.jboss.shamrock.annotations.BuildProducer;
import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.deployment.builditem.AdditionalBeanBuildItem;
import org.jboss.shamrock.deployment.builditem.CombinedIndexBuildItem;
import org.jboss.shamrock.deployment.builditem.FeatureBuildItem;
import org.jboss.shamrock.deployment.builditem.GeneratedClassBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveClassBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.SubstrateProxyDefinitionBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.SubstrateResourceBuildItem;
import org.jboss.shamrock.deployment.cdi.GeneratedBeanBuildItem;
import org.jboss.shamrock.restclient.runtime.DefaultResponseExceptionMapper;
import org.jboss.shamrock.restclient.runtime.RestClientBase;
import org.jboss.shamrock.restclient.runtime.RestClientProxy;

class RestClientProcessor {

    private static final DotName[] CLIENT_ANNOTATIONS = {
            DotName.createSimple("javax.ws.rs.GET"),
            DotName.createSimple("javax.ws.rs.HEAD"),
            DotName.createSimple("javax.ws.rs.DELETE"),
            DotName.createSimple("javax.ws.rs.OPTIONS"),
            DotName.createSimple("javax.ws.rs.PATCH"),
            DotName.createSimple("javax.ws.rs.POST"),
            DotName.createSimple("javax.ws.rs.PUT"),
            DotName.createSimple("javax.ws.rs.PUT"),
            DotName.createSimple(RegisterRestClient.class.getName()),
            DotName.createSimple(Path.class.getName())
    };

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
    CombinedIndexBuildItem combinedIndexBuildItem;

    @BuildStep
    public void build(BuildProducer<GeneratedBeanBuildItem> generatedBeans, BuildProducer<FeatureBuildItem> feature) throws Exception {
        feature.produce(new FeatureBuildItem(FeatureBuildItem.MP_REST_CLIENT));
    	reflectiveClass.produce(new ReflectiveClassBuildItem(false, false,
                DefaultResponseExceptionMapper.class.getName(),
                LogFactoryImpl.class.getName(),
                Jdk14Logger.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, ClientRequestFilter[].class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, ClientResponseFilter[].class.getName()));
        proxyDefinition.produce(new SubstrateProxyDefinitionBuildItem("javax.ws.rs.ext.Providers"));
        additionalBeans.produce(new AdditionalBeanBuildItem(RestClient.class));
        resources.produce(new SubstrateResourceBuildItem("META-INF/services/javax.ws.rs.ext.Providers"));
        //TODO: fix this, we don't want to just add all the providers
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, "org.jboss.resteasy.core.ResteasyProviderFactoryImpl", "org.jboss.resteasy.client.jaxrs.internal.proxy.ProxyBuilderImpl"));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl"));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl"));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, "org.jboss.resteasy.plugins.providers.jsonb.JsonBindingProvider", "org.jboss.resteasy.plugins.providers.jsonb.AbstractJsonBindingProvider"));
        proxyDefinition.produce(new SubstrateProxyDefinitionBuildItem(ResteasyConfiguration.class.getName()));
        Map<DotName, ClassInfo> interfaces = new HashMap<>();
        for (DotName type : CLIENT_ANNOTATIONS) {
            for (AnnotationInstance annotation : combinedIndexBuildItem.getIndex().getAnnotations(type)) {
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
        }

        for (Map.Entry<DotName, ClassInfo> entry : interfaces.entrySet()) {
            String iName = entry.getKey().toString();
            proxyDefinition.produce(new SubstrateProxyDefinitionBuildItem(iName, ResteasyClientProxy.class.getName()));
            proxyDefinition.produce(new SubstrateProxyDefinitionBuildItem(iName, RestClientProxy.class.getName()));
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, iName));

            //now generate CDI beans
            //TODO: do we need to check if CDI is enabled? Are we just assuming it always is?
            String className = iName + "$$RestClientProxy";
            AtomicReference<byte[]> bytes = new AtomicReference<>();
            try (ClassCreator creator = new ClassCreator(new ClassOutput() {
                @Override
                public void write(String name, byte[] data) {
                    bytes.set(data);
                    generatedClass.produce(new GeneratedClassBuildItem(true, name, data));
                }
            }, className, null, RestClientBase.class.getName())) {

                creator.addAnnotation(Dependent.class);
                MethodCreator producer = creator.getMethodCreator("producerMethod", iName);
                producer.addAnnotation(Produces.class);
                producer.addAnnotation(RestClient.class);
                producer.addAnnotation(ApplicationScoped.class);

                ResultHandle ret = producer.invokeVirtualMethod(MethodDescriptor.ofMethod(RestClientBase.class, "create", Object.class), producer.getThis());
                producer.returnValue(ret);

                MethodCreator ctor = creator.getMethodCreator(MethodDescriptor.ofConstructor(className));
                ctor.invokeSpecialMethod(MethodDescriptor.ofConstructor(RestClientBase.class, Class.class), ctor.getThis(), ctor.loadClass(iName));
                ctor.returnValue(null);
            }
            generatedBeans.produce(new GeneratedBeanBuildItem(className, bytes.get()));
        }
    }
}
