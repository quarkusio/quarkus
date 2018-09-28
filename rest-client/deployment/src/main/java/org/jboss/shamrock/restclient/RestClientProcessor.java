package org.jboss.shamrock.restclient;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;
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
import org.jboss.shamrock.deployment.ArchiveContext;
import org.jboss.shamrock.deployment.BeanDeployment;
import org.jboss.shamrock.deployment.ProcessorContext;
import org.jboss.shamrock.deployment.ResourceProcessor;
import org.jboss.shamrock.deployment.ShamrockConfig;
import org.jboss.shamrock.restclient.runtime.DefaultResponseExceptionMapper;
import org.jboss.shamrock.restclient.runtime.RestClientBase;
import org.jboss.shamrock.restclient.runtime.RestClientProxy;

class RestClientProcessor implements ResourceProcessor {

    private static final Logger log = Logger.getLogger(RestClientProxy.class.getName());

    private static final DotName REGISTER_REST_CLIENT = DotName.createSimple(RegisterRestClient.class.getName());
    @Inject
    private BeanDeployment beanDeployment;

    @Inject
    private ShamrockConfig config;

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

    @Override
    public void process(ArchiveContext archiveContext, ProcessorContext processorContext) throws Exception {
        processorContext.addReflectiveClass(false, false,
                DefaultResponseExceptionMapper.class.getName(),
                LogFactoryImpl.class.getName(),
                Jdk14Logger.class.getName());
        processorContext.addReflectiveClass(false, false, ClientRequestFilter[].class.getName());
        processorContext.addReflectiveClass(false, false, ClientResponseFilter[].class.getName());
        processorContext.addProxyDefinition("javax.ws.rs.ext.Providers");
        beanDeployment.addAdditionalBean(RestClient.class);
        processorContext.addResource("META-INF/services/javax.ws.rs.ext.Providers");
        //TODO: fix this, we don't want to just add all the providers
        processorContext.addReflectiveClass(false, false, "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
        processorContext.addReflectiveClass(false, false, "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");
        processorContext.addReflectiveClass(true, true, "org.jboss.resteasy.plugins.providers.jsonb.JsonBindingProvider", "org.jboss.resteasy.plugins.providers.jsonb.AbstractJsonBindingProvider");
        processorContext.addProxyDefinition(ResteasyConfiguration.class.getName());
        Map<DotName, ClassInfo> interfaces = new HashMap<>();
        for (DotName type : CLIENT_ANNOTATIONS) {
            for (AnnotationInstance annotation : archiveContext.getCombinedIndex().getAnnotations(type)) {
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
            processorContext.addProxyDefinition(iName, ResteasyClientProxy.class.getName());
            processorContext.addProxyDefinition(iName, RestClientProxy.class.getName());
            processorContext.addReflectiveClass(true, false, iName);

            //now generate CDI beans
            //TODO: do we need to check if CDI is enabled? Are we just assuming it always is?
            String className = iName + "$$RestClientProxy";
            AtomicReference<byte[]> bytes= new AtomicReference<>();
            try (ClassCreator creator = new ClassCreator(new ClassOutput() {
                @Override
                public void write(String name, byte[] data) {
                    try {
                        bytes.set(data);
                        processorContext.addGeneratedClass(true, name, data);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
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
            beanDeployment.addGeneratedBean(className, bytes.get());
        }
    }

    @Override
    public int getPriority() {
        return 1;
    }
}
