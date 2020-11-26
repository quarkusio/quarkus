package io.quarkus.resteasy.reactive.common.deployment;

import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.DELETE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.GET;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.HEAD;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.OPTIONS;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.PATCH;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.POST;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.PUT;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.Application;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.common.processor.NameBindingUtil;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.resteasy.reactive.common.deployment.ApplicationResultBuildItem.KeepProviderResult;
import io.quarkus.resteasy.reactive.spi.AbstractInterceptorBuildItem;
import io.quarkus.resteasy.reactive.spi.ContainerRequestFilterBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyReaderBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyWriterBuildItem;
import io.quarkus.resteasy.reactive.spi.ReaderInterceptorBuildItem;
import io.quarkus.resteasy.reactive.spi.WriterInterceptorBuildItem;

public class QuarkusRestCommonProcessor {

    private static Map<DotName, String> BUILTIN_HTTP_ANNOTATIONS_TO_METHOD = new HashMap<>();

    static {
        BUILTIN_HTTP_ANNOTATIONS_TO_METHOD.put(GET, "GET");
        BUILTIN_HTTP_ANNOTATIONS_TO_METHOD.put(POST, "POST");
        BUILTIN_HTTP_ANNOTATIONS_TO_METHOD.put(HEAD, "HEAD");
        BUILTIN_HTTP_ANNOTATIONS_TO_METHOD.put(PUT, "PUT");
        BUILTIN_HTTP_ANNOTATIONS_TO_METHOD.put(DELETE, "DELETE");
        BUILTIN_HTTP_ANNOTATIONS_TO_METHOD.put(PATCH, "PATCH");
        BUILTIN_HTTP_ANNOTATIONS_TO_METHOD.put(OPTIONS, "OPTIONS");
        BUILTIN_HTTP_ANNOTATIONS_TO_METHOD = Collections.unmodifiableMap(BUILTIN_HTTP_ANNOTATIONS_TO_METHOD);
    }

    @BuildStep
    ApplicationResultBuildItem handleApplication(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {

        IndexView index = combinedIndexBuildItem.getIndex();
        Collection<ClassInfo> applications = index
                .getAllKnownSubclasses(ResteasyReactiveDotNames.APPLICATION);

        Set<String> allowedClasses = new HashSet<>();
        Set<String> singletonClasses = new HashSet<>();
        Set<String> globalNameBindings = new HashSet<>();
        boolean filterClasses = false;
        Application application = null;
        ClassInfo selectedAppClass = null;
        boolean blocking = false;
        for (ClassInfo applicationClassInfo : applications) {
            if (selectedAppClass != null) {
                throw new RuntimeException("More than one Application class: " + applications);
            }
            selectedAppClass = applicationClassInfo;
            // FIXME: yell if there's more than one
            String applicationClass = applicationClassInfo.name().toString();
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, false, applicationClass));
            try {
                Class<?> appClass = Thread.currentThread().getContextClassLoader().loadClass(applicationClass);
                application = (Application) appClass.getConstructor().newInstance();
                Set<Class<?>> classes = application.getClasses();
                if (!classes.isEmpty()) {
                    for (Class<?> klass : classes) {
                        allowedClasses.add(klass.getName());
                    }
                    filterClasses = true;
                }
                classes = application.getSingletons().stream().map(Object::getClass).collect(Collectors.toSet());
                if (!classes.isEmpty()) {
                    for (Class<?> klass : classes) {
                        allowedClasses.add(klass.getName());
                        singletonClasses.add(klass.getName());
                    }
                    filterClasses = true;
                }
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException
                    | InvocationTargetException e) {
                throw new RuntimeException("Unable to handle class: " + applicationClass, e);
            }
            if (applicationClassInfo.classAnnotation(ResteasyReactiveDotNames.NON_BLOCKING) != null) {
                blocking = false;
            } else if (applicationClassInfo.classAnnotation(ResteasyReactiveDotNames.NON_BLOCKING) != null) {
                blocking = true;
            }
        }
        return new ApplicationResultBuildItem(allowedClasses, singletonClasses, globalNameBindings, filterClasses, application,
                selectedAppClass, blocking);
    }

    @BuildStep
    public void scanForIOInterceptors(CombinedIndexBuildItem combinedIndexBuildItem,
            ApplicationResultBuildItem applicationResultBuildItem,
            BuildProducer<WriterInterceptorBuildItem> writerInterceptorProducer,
            BuildProducer<ReaderInterceptorBuildItem> readerInterceptorProducer) {
        IndexView index = combinedIndexBuildItem.getIndex();
        Collection<ClassInfo> readerInterceptors = index
                .getAllKnownImplementors(ResteasyReactiveDotNames.READER_INTERCEPTOR);
        Collection<ClassInfo> writerInterceptors = index
                .getAllKnownImplementors(ResteasyReactiveDotNames.WRITER_INTERCEPTOR);

        for (ClassInfo filterClass : writerInterceptors) {
            handleDiscoveredInterceptor(applicationResultBuildItem, writerInterceptorProducer, index, filterClass,
                    WriterInterceptorBuildItem.Builder::new);
        }
        for (ClassInfo filterClass : readerInterceptors) {
            handleDiscoveredInterceptor(applicationResultBuildItem, readerInterceptorProducer, index, filterClass,
                    ReaderInterceptorBuildItem.Builder::new);
        }
    }

    public static <T extends AbstractInterceptorBuildItem, B extends AbstractInterceptorBuildItem.Builder<T, B>> void handleDiscoveredInterceptor(
            ApplicationResultBuildItem applicationResultBuildItem, BuildProducer<T> producer, IndexView index,
            ClassInfo filterClass, Function<String, B> builderCreator) {
        KeepProviderResult keepProviderResult = applicationResultBuildItem.keepProvider(filterClass);
        if (keepProviderResult != KeepProviderResult.DISCARD) {
            B builder = builderCreator.apply(filterClass.name().toString());
            builder.setRegisterAsBean(true);
            if (filterClass.classAnnotation(ResteasyReactiveDotNames.PRE_MATCHING) != null) {
                if (builder instanceof ContainerRequestFilterBuildItem.Builder) {
                    ((ContainerRequestFilterBuildItem.Builder) builder).setPreMatching(true);
                }
            }
            builder.setNameBindingNames(NameBindingUtil.nameBindingNames(index, filterClass));
            AnnotationInstance priorityInstance = filterClass.classAnnotation(ResteasyReactiveDotNames.PRIORITY);
            if (priorityInstance != null) {
                builder.setPriority(priorityInstance.value().asInt());
            }
            producer.produce(builder.build());
        }
    }

    @BuildStep
    void scanResources(
            // TODO: We need to use this index instead of BeanArchiveIndexBuildItem to avoid build cycles. It it OK?
            CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformerBuildItemBuildProducer,
            BuildProducer<ResourceScanningResultBuildItem> resourceScanningResultBuildItemBuildProducer) {
        IndexView index = combinedIndexBuildItem.getIndex();
        Collection<AnnotationInstance> paths = index.getAnnotations(ResteasyReactiveDotNames.PATH);

        Collection<AnnotationInstance> allPaths = new ArrayList<>(paths);

        if (allPaths.isEmpty()) {
            // no detected @Path, bail out
            return;
        }

        Map<DotName, ClassInfo> scannedResources = new HashMap<>();
        Map<DotName, String> scannedResourcePaths = new HashMap<>();
        Map<DotName, ClassInfo> possibleSubResources = new HashMap<>();
        Map<DotName, String> pathInterfaces = new HashMap<>();
        Map<DotName, MethodInfo> resourcesThatNeedCustomProducer = new HashMap<>();
        List<MethodInfo> methodExceptionMappers = new ArrayList<>();
        Set<String> beanParams = new HashSet<>();

        for (AnnotationInstance beanParamAnnotation : index.getAnnotations(ResteasyReactiveDotNames.BEAN_PARAM)) {
            AnnotationTarget target = beanParamAnnotation.target();
            // FIXME: this isn't right wrt generics
            switch (target.kind()) {
                case FIELD:
                    beanParams.add(target.asField().type().toString());
                    break;
                case METHOD:
                    Type setterParamType = target.asMethod().parameters().get(0);
                    beanParams.add(setterParamType.toString());
                    break;
                case METHOD_PARAMETER:
                    MethodInfo method = target.asMethodParameter().method();
                    int paramIndex = target.asMethodParameter().position();
                    Type paramType = method.parameters().get(paramIndex);
                    beanParams.add(paramType.toString());
                    break;
                default:
                    break;
            }
        }

        for (AnnotationInstance annotation : allPaths) {
            if (annotation.target().kind() == AnnotationTarget.Kind.CLASS) {
                ClassInfo clazz = annotation.target().asClass();
                if (!Modifier.isInterface(clazz.flags())) {
                    scannedResources.put(clazz.name(), clazz);
                    scannedResourcePaths.put(clazz.name(), annotation.value().asString());
                } else {
                    pathInterfaces.put(clazz.name(), annotation.value().asString());
                }
                MethodInfo ctor = hasJaxRsCtorParams(clazz);
                if (ctor != null) {
                    resourcesThatNeedCustomProducer.put(clazz.name(), ctor);
                }
                List<AnnotationInstance> exceptionMapperAnnotationInstances = clazz.annotations()
                        .get(QuarkusResteasyReactiveDotNames.SERVER_EXCEPTION_MAPPER);
                if (exceptionMapperAnnotationInstances != null) {
                    for (AnnotationInstance instance : exceptionMapperAnnotationInstances) {
                        if (instance.target().kind() != AnnotationTarget.Kind.METHOD) {
                            continue;
                        }
                        if ((instance.value() != null) && (instance.value().asClassArray().length > 0)) {
                            methodExceptionMappers.add(instance.target().asMethod());
                        }
                    }
                }
            }
        }

        if (!resourcesThatNeedCustomProducer.isEmpty()) {
            annotationsTransformerBuildItemBuildProducer
                    .produce(new AnnotationsTransformerBuildItem(
                            new VetoingAnnotationTransformer(resourcesThatNeedCustomProducer.keySet())));
        }

        for (Map.Entry<DotName, String> i : pathInterfaces.entrySet()) {
            for (ClassInfo clazz : index.getAllKnownImplementors(i.getKey())) {
                if (!Modifier.isAbstract(clazz.flags())) {
                    if ((clazz.enclosingClass() == null || Modifier.isStatic(clazz.flags())) &&
                            clazz.enclosingMethod() == null) {
                        if (!scannedResources.containsKey(clazz.name())) {
                            scannedResources.put(clazz.name(), clazz);
                            scannedResourcePaths.put(clazz.name(), i.getValue());
                        }
                    }
                }
            }
        }

        Map<DotName, String> httpAnnotationToMethod = new HashMap<>(BUILTIN_HTTP_ANNOTATIONS_TO_METHOD);
        Collection<AnnotationInstance> httpMethodInstances = index.getAnnotations(ResteasyReactiveDotNames.HTTP_METHOD);
        for (AnnotationInstance httpMethodInstance : httpMethodInstances) {
            if (httpMethodInstance.target().kind() != AnnotationTarget.Kind.CLASS) {
                continue;
            }
            httpAnnotationToMethod.put(httpMethodInstance.target().asClass().name(), httpMethodInstance.value().asString());
        }
        resourceScanningResultBuildItemBuildProducer.produce(new ResourceScanningResultBuildItem(scannedResources,
                scannedResourcePaths, possibleSubResources, pathInterfaces, resourcesThatNeedCustomProducer, beanParams,
                httpAnnotationToMethod, methodExceptionMappers));
    }

    @BuildStep
    public void setupEndpoints(BeanArchiveIndexBuildItem beanArchiveIndexBuildItem,
            ApplicationResultBuildItem applicationResultBuildItem,
            BeanContainerBuildItem beanContainerBuildItem,
            Optional<ResourceScanningResultBuildItem> resourceScanningResultBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<MessageBodyWriterBuildItem> messageBodyWriterBuildItemBuildProducer,
            BuildProducer<MessageBodyReaderBuildItem> messageBodyReaderBuildItemBuildProducer) throws NoSuchMethodException {

        if (!resourceScanningResultBuildItem.isPresent()) {
            // no detected @Path, bail out
            return;
        }

        IndexView index = beanArchiveIndexBuildItem.getIndex();
        Collection<ClassInfo> writers = index
                .getAllKnownImplementors(ResteasyReactiveDotNames.MESSAGE_BODY_WRITER);
        Collection<ClassInfo> readers = index
                .getAllKnownImplementors(ResteasyReactiveDotNames.MESSAGE_BODY_READER);

        for (ClassInfo writerClass : writers) {
            KeepProviderResult keepProviderResult = applicationResultBuildItem.keepProvider(writerClass);
            if (keepProviderResult != KeepProviderResult.DISCARD) {
                RuntimeType runtimeType = null;
                if (keepProviderResult == KeepProviderResult.SERVER_ONLY) {
                    runtimeType = RuntimeType.SERVER;
                }
                List<String> mediaTypeStrings = Collections.emptyList();
                AnnotationInstance producesAnnotation = writerClass.classAnnotation(ResteasyReactiveDotNames.PRODUCES);
                if (producesAnnotation != null) {
                    mediaTypeStrings = Arrays.asList(producesAnnotation.value().asStringArray());
                }
                List<Type> typeParameters = JandexUtil.resolveTypeParameters(writerClass.name(),
                        ResteasyReactiveDotNames.MESSAGE_BODY_WRITER,
                        index);
                String writerClassName = writerClass.name().toString();
                AnnotationInstance constrainedToInstance = writerClass.classAnnotation(ResteasyReactiveDotNames.CONSTRAINED_TO);
                if (constrainedToInstance != null) {
                    runtimeType = RuntimeType.valueOf(constrainedToInstance.value().asEnum());
                }
                messageBodyWriterBuildItemBuildProducer.produce(new MessageBodyWriterBuildItem(writerClassName,
                        typeParameters.get(0).name().toString(), mediaTypeStrings, runtimeType, false));
            }
        }

        for (ClassInfo readerClass : readers) {
            KeepProviderResult keepProviderResult = applicationResultBuildItem.keepProvider(readerClass);
            if (keepProviderResult != KeepProviderResult.DISCARD) {
                List<Type> typeParameters = JandexUtil.resolveTypeParameters(readerClass.name(),
                        ResteasyReactiveDotNames.MESSAGE_BODY_READER,
                        index);
                RuntimeType runtimeType = null;
                if (keepProviderResult == KeepProviderResult.SERVER_ONLY) {
                    runtimeType = RuntimeType.SERVER;
                }
                List<String> mediaTypeStrings = Collections.emptyList();
                String readerClassName = readerClass.name().toString();
                AnnotationInstance consumesAnnotation = readerClass.classAnnotation(ResteasyReactiveDotNames.CONSUMES);
                if (consumesAnnotation != null) {
                    mediaTypeStrings = Arrays.asList(consumesAnnotation.value().asStringArray());
                }
                AnnotationInstance constrainedToInstance = readerClass.classAnnotation(ResteasyReactiveDotNames.CONSTRAINED_TO);
                if (constrainedToInstance != null) {
                    runtimeType = RuntimeType.valueOf(constrainedToInstance.value().asEnum());
                }
                messageBodyReaderBuildItemBuildProducer.produce(new MessageBodyReaderBuildItem(readerClassName,
                        typeParameters.get(0).name().toString(), mediaTypeStrings, runtimeType, false));
                reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, false, readerClassName));
            }
        }
    }

    @BuildStep
    void additionalBeans(List<WriterInterceptorBuildItem> writerInterceptors,
            List<ReaderInterceptorBuildItem> readerInterceptors,
            BuildProducer<AdditionalBeanBuildItem> additionalBean) {

        AdditionalBeanBuildItem.Builder additionalProviders = AdditionalBeanBuildItem.builder();
        for (WriterInterceptorBuildItem i : writerInterceptors) {
            if (i.isRegisterAsBean()) {
                additionalProviders.addBeanClass(i.getClassName());
            }
        }
        for (ReaderInterceptorBuildItem i : readerInterceptors) {
            if (i.isRegisterAsBean()) {
                additionalProviders.addBeanClass(i.getClassName());
            }
        }
        additionalBean.produce(additionalProviders.setUnremovable().setDefaultScope(DotNames.SINGLETON).build());
    }

    private MethodInfo hasJaxRsCtorParams(ClassInfo classInfo) {
        List<MethodInfo> methods = classInfo.methods();
        List<MethodInfo> ctors = new ArrayList<>();
        for (MethodInfo method : methods) {
            if (method.name().equals("<init>")) {
                ctors.add(method);
            }
        }
        if (ctors.size() != 1) { // we only need to deal with a single ctor here
            return null;
        }
        MethodInfo ctor = ctors.get(0);
        if (ctor.parameters().size() == 0) { // default ctor - we don't need to do anything
            return null;
        }

        boolean needsHandling = false;
        for (DotName dotName : ResteasyReactiveDotNames.RESOURCE_CTOR_PARAMS_THAT_NEED_HANDLING) {
            if (ctor.hasAnnotation(dotName)) {
                needsHandling = true;
                break;
            }
        }
        return needsHandling ? ctor : null;
    }

}
