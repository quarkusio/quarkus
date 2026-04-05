package org.acme.app;

import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.acme.annotations.AnnotationType;
import org.acme.annotations.AnnotationValue;
import org.acme.fieldannos.FieldAnnoHolder;
import org.acme.descriptors.DescriptorService;
import org.acme.descriptors.ParamType;
import org.acme.fieldtypes.FieldHolder;
import org.acme.generics.GenericArg;
import org.acme.generics.GenericContainer;
import org.acme.inner.Outer;
import org.acme.invokedyn.LambdaTarget;
import org.acme.libb.ServiceB;
import org.acme.libc.ServiceC;
import org.acme.libd.ServiceD;
import org.acme.logging.LoggedClass;
import org.acme.multirelease.MultiReleaseClass;
import org.acme.serviceloader.ServiceInterface;
import org.acme.throws_.ThrowingService;
import org.acme.loadchain.ChainProvider;
import org.acme.serialization.ResourceDeserializer;
import org.acme.transform.TransformableClass;

@Path("/tree-shake")
public class TreeShakeResource {

    @GET
    @Path("/basic")
    public String basic() {
        return new ServiceB().process() + "|" + new ServiceC().process() + "|" + new ServiceD().process();
    }

    @GET
    @Path("/serviceloader")
    public String serviceloader() {
        return StreamSupport.stream(ServiceLoader.load(ServiceInterface.class).spliterator(), false)
                .map(ServiceInterface::serve)
                .collect(Collectors.joining(","));
    }

    @GET
    @Path("/forname")
    public String forname() throws ClassNotFoundException {
        Class<?> clazz = Class.forName("org.acme.forname.ForNameTarget");
        return clazz.getSimpleName();
    }

    @GET
    @Path("/loadclass")
    public String loadclass() throws ClassNotFoundException {
        Class<?> clazz = getClass().getClassLoader().loadClass("org.acme.loadclass.LoadClassTarget");
        return clazz.getSimpleName();
    }

    @GET
    @Path("/annotations")
    @AnnotationType(AnnotationValue.class)
    public String annotations() {
        return AnnotationValue.name();
    }

    @GET
    @Path("/field-annotations")
    public String fieldAnnotations() {
        return new FieldAnnoHolder().describe();
    }

    @GET
    @Path("/generics")
    public String generics() {
        GenericContainer<GenericArg> container = new GenericContainer<>();
        container.set(new GenericArg());
        return container.get().name();
    }

    @GET
    @Path("/inner")
    public String inner() {
        return new Outer.Inner().name();
    }

    @GET
    @Path("/method-descriptors")
    public String methodDescriptors() {
        DescriptorService svc = new DescriptorService();
        return svc.process(new ParamType("test")).getValue();
    }

    @GET
    @Path("/multirelease")
    public String multirelease() {
        return new MultiReleaseClass().version();
    }

    @GET
    @Path("/jboss-logging")
    public String jbossLogging() {
        String base = new LoggedClass().doWork();
        // Verify all JBoss Logging companion suffixes are preserved
        for (String suffix : new String[] { "_$logger", "_$bundle", "_impl" }) {
            try {
                Class.forName("org.acme.logging.LoggedClass" + suffix);
            } catch (ClassNotFoundException e) {
                return "MISSING:" + suffix;
            }
        }
        return base;
    }

    @GET
    @Path("/throws")
    public String throwsEndpoint() {
        try {
            return new ThrowingService().doWork();
        } catch (Exception e) {
            return "error";
        }
    }

    @GET
    @Path("/field-types")
    public String fieldTypes() {
        return new FieldHolder().describe();
    }

    @GET
    @Path("/invokedynamic")
    public String invokedynamic() {
        Function<String, LambdaTarget> factory = LambdaTarget::new;
        return factory.apply("test").getValue();
    }

    @GET
    @Path("/sisu")
    public String sisu() {
        try {
            var resources = getClass().getClassLoader()
                    .getResources("META-INF/sisu/javax.inject.Named");
            return resources.hasMoreElements() ? "found" : "not-found";
        } catch (Exception e) {
            return "error";
        }
    }

    @GET
    @Path("/transform")
    public String transform() {
        return new TransformableClass().name();
    }

    @GET
    @Path("/serialization")
    public String serialization() {
        return ResourceDeserializer.load();
    }

    @GET
    @Path("/loadclass-chain")
    public String loadclassChain() {
        return String.join(",", new ChainProvider().getLoaded());
    }
}
