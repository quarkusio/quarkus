package org.jboss.resteasy.reactive.common.processor.scanning;

import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.BEAN_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.COOKIE_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.DELETE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.FORM_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.GET;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.HEAD;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.HEADER_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.MATRIX_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.OPTIONS;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.PATCH;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.PATH_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.POST;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.PUT;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.QUERY_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.REST_COOKIE_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.REST_FORM_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.REST_HEADER_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.REST_MATRIX_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.REST_PATH_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.REST_QUERY_PARAM;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.core.Application;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.common.processor.BlockingDefault;
import org.jboss.resteasy.reactive.common.processor.JandexUtil;
import org.jboss.resteasy.reactive.common.processor.NameBindingUtil;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;

public class ResteasyReactiveScanner {

    public static final Map<DotName, String> BUILTIN_HTTP_ANNOTATIONS_TO_METHOD = Map.of(GET, "GET",
            POST, "POST",
            HEAD, "HEAD",
            PUT, "PUT",
            DELETE, "DELETE",
            PATCH, "PATCH",
            OPTIONS, "OPTIONS");
    public static final Map<String, DotName> METHOD_TO_BUILTIN_HTTP_ANNOTATIONS = Map.of("GET", GET,
            "POST", POST,
            "HEAD", HEAD,
            "PUT", PUT,
            "DELETE", DELETE,
            "PATCH", PATCH,
            "OPTIONS", OPTIONS);

    public static ApplicationScanningResult scanForApplicationClass(IndexView index, Set<String> excludedClasses) {
        Collection<ClassInfo> applications = index
                .getAllKnownSubclasses(ResteasyReactiveDotNames.APPLICATION);
        Set<String> allowedClasses = new HashSet<>();
        Set<String> singletonClasses = new HashSet<>();
        Set<String> globalNameBindings = new HashSet<>();
        boolean filterClasses = !excludedClasses.isEmpty();
        Application application = null;
        ClassInfo selectedAppClass = null;
        BlockingDefault blocking = BlockingDefault.AUTOMATIC;
        for (ClassInfo applicationClassInfo : applications) {
            if (Modifier.isAbstract(applicationClassInfo.flags())) {
                continue;
            }
            if (excludedClasses.contains(applicationClassInfo.name().toString())) {
                continue;
            }
            if (selectedAppClass != null) {
                throw new RuntimeException("More than one Application class: " + applications);
            }
            selectedAppClass = applicationClassInfo;
            if (appClassHasInject(selectedAppClass)) {
                throw new RuntimeException("'@Inject' cannot be used with a '" + ResteasyReactiveDotNames.APPLICATION
                        + "' class. Offending class is: '" + selectedAppClass.name() + "'");
            }
            String applicationClass = applicationClassInfo.name().toString();
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
            // collect default behaviour, making sure that we don't have multiple contradicting annotations
            int numAnnotations = 0;
            if (applicationClassInfo.hasDeclaredAnnotation(ResteasyReactiveDotNames.BLOCKING)) {
                blocking = BlockingDefault.BLOCKING;
                numAnnotations++;
            }
            if (applicationClassInfo.hasDeclaredAnnotation(ResteasyReactiveDotNames.NON_BLOCKING)) {
                blocking = BlockingDefault.NON_BLOCKING;
                numAnnotations++;
            }
            if (applicationClassInfo.hasDeclaredAnnotation(ResteasyReactiveDotNames.RUN_ON_VIRTUAL_THREAD)) {
                blocking = BlockingDefault.RUN_ON_VIRTUAL_THREAD;
                numAnnotations++;
            }
            if (numAnnotations > 1) {
                throw new DeploymentException("JAX-RS Application class '" + applicationClassInfo.name()
                        + "' contains multiple conflicting @Blocking, @NonBlocking and @RunOnVirtualThread annotations.");
            }
        }
        if (selectedAppClass != null) {
            globalNameBindings = NameBindingUtil.nameBindingNames(index, selectedAppClass);
        }
        return new ApplicationScanningResult(allowedClasses, singletonClasses, excludedClasses, globalNameBindings,
                filterClasses, application,
                selectedAppClass, blocking);
    }

    public static SerializerScanningResult scanForSerializers(IndexView index,
            ApplicationScanningResult applicationScanningResult) {

        Collection<ClassInfo> readers = index
                .getAllKnownImplementors(ResteasyReactiveDotNames.MESSAGE_BODY_READER);
        List<ScannedSerializer> readerList = new ArrayList<>();

        for (ClassInfo readerClass : readers) {
            ApplicationScanningResult.KeepProviderResult keepProviderResult = applicationScanningResult
                    .keepProvider(readerClass);
            if (keepProviderResult != ApplicationScanningResult.KeepProviderResult.DISCARD) {
                List<Type> typeParameters = JandexUtil.resolveTypeParameters(readerClass.name(),
                        ResteasyReactiveDotNames.MESSAGE_BODY_READER,
                        index);
                RuntimeType runtimeType = null;
                if (keepProviderResult == ApplicationScanningResult.KeepProviderResult.SERVER_ONLY) {
                    runtimeType = RuntimeType.SERVER;
                }
                List<String> mediaTypeStrings = Collections.emptyList();
                AnnotationInstance consumesAnnotation = readerClass.declaredAnnotation(ResteasyReactiveDotNames.CONSUMES);
                if (consumesAnnotation != null) {
                    mediaTypeStrings = Arrays.asList(consumesAnnotation.value().asStringArray());
                }
                AnnotationInstance constrainedToInstance = readerClass
                        .declaredAnnotation(ResteasyReactiveDotNames.CONSTRAINED_TO);
                if (constrainedToInstance != null) {
                    runtimeType = RuntimeType.valueOf(constrainedToInstance.value().asEnum());
                }
                int priority = Priorities.USER;
                AnnotationInstance priorityInstance = readerClass.declaredAnnotation(ResteasyReactiveDotNames.PRIORITY);
                if (priorityInstance != null) {
                    priority = priorityInstance.value().asInt();
                }
                readerList.add(new ScannedSerializer(readerClass,
                        typeParameters.get(0).name().toString(), mediaTypeStrings, runtimeType, false, priority));
            }
        }

        List<ScannedSerializer> writerList = new ArrayList<>();
        Collection<ClassInfo> writers = index
                .getAllKnownImplementors(ResteasyReactiveDotNames.MESSAGE_BODY_WRITER);

        for (ClassInfo writerClass : writers) {
            ApplicationScanningResult.KeepProviderResult keepProviderResult = applicationScanningResult
                    .keepProvider(writerClass);
            if (keepProviderResult != ApplicationScanningResult.KeepProviderResult.DISCARD) {
                RuntimeType runtimeType = null;
                if (keepProviderResult == ApplicationScanningResult.KeepProviderResult.SERVER_ONLY) {
                    runtimeType = RuntimeType.SERVER;
                }
                List<String> mediaTypeStrings = Collections.emptyList();
                AnnotationInstance producesAnnotation = writerClass.declaredAnnotation(ResteasyReactiveDotNames.PRODUCES);
                if (producesAnnotation != null) {
                    mediaTypeStrings = Arrays.asList(producesAnnotation.value().asStringArray());
                }
                List<Type> typeParameters = JandexUtil.resolveTypeParameters(writerClass.name(),
                        ResteasyReactiveDotNames.MESSAGE_BODY_WRITER,
                        index);
                AnnotationInstance constrainedToInstance = writerClass
                        .declaredAnnotation(ResteasyReactiveDotNames.CONSTRAINED_TO);
                if (constrainedToInstance != null) {
                    runtimeType = RuntimeType.valueOf(constrainedToInstance.value().asEnum());
                }
                int priority = Priorities.USER;
                AnnotationInstance priorityInstance = writerClass.declaredAnnotation(ResteasyReactiveDotNames.PRIORITY);
                if (priorityInstance != null) {
                    priority = priorityInstance.value().asInt();
                }
                writerList.add(new ScannedSerializer(writerClass,
                        typeParameters.get(0).name().toString(), mediaTypeStrings, runtimeType, false, priority));
            }
        }
        return new SerializerScanningResult(readerList, writerList);
    }

    private static boolean appClassHasInject(ClassInfo appClass) {
        if (appClass.annotationsMap() == null) {
            return false;
        }
        List<AnnotationInstance> injectInstances = appClass.annotationsMap().get(ResteasyReactiveDotNames.CDI_INJECT);
        return (injectInstances != null) && !injectInstances.isEmpty();
    }

    public static ResourceScanningResult scanResources(
            IndexView index, Map<DotName, ClassInfo> additionalResources, Map<DotName, String> additionalResourcePaths) {
        Collection<AnnotationInstance> paths = index.getAnnotations(ResteasyReactiveDotNames.PATH);

        Collection<AnnotationInstance> allPaths = new ArrayList<>(paths);

        Map<DotName, ClassInfo> scannedResources = new HashMap<>(additionalResources);
        Map<DotName, String> scannedResourcePaths = new HashMap<>(additionalResourcePaths);
        Map<DotName, String> pathInterfaces = new HashMap<>();
        Map<DotName, MethodInfo> resourcesThatNeedCustomProducer = new HashMap<>();
        List<MethodInfo> methodExceptionMappers = new ArrayList<>();
        Set<DotName> requestScopedResources = new HashSet<>();

        Set<DotName> interfacesWithPathOnMethods = new HashSet<>();

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
                if (hasJaxRsFieldInjection(clazz, index)) {
                    requestScopedResources.add(clazz.name());
                }
                List<AnnotationInstance> exceptionMapperAnnotationInstances = clazz.annotationsMap()
                        .get(ResteasyReactiveDotNames.SERVER_EXCEPTION_MAPPER);
                if (exceptionMapperAnnotationInstances != null) {
                    for (AnnotationInstance instance : exceptionMapperAnnotationInstances) {
                        if (instance.target().kind() != AnnotationTarget.Kind.METHOD) {
                            continue;
                        }
                        methodExceptionMappers.add(instance.target().asMethod());
                    }
                }
            } else if (annotation.target().kind() == AnnotationTarget.Kind.METHOD) {
                ClassInfo clazz = annotation.target().asMethod().declaringClass();
                if (Modifier.isInterface(clazz.flags())) {
                    interfacesWithPathOnMethods.add(clazz.name());
                }
            }
        }

        // handle abstract classes
        var abstractClasses = scannedResources.values().stream().filter(ClassInfo::isAbstract).toList();
        abstractClasses.forEach(abstractScannedResource -> {
            Collection<ClassInfo> allSubclasses = index.getAllKnownSubclasses(abstractScannedResource.name());
            if (allSubclasses.size() != 1) {
                return; // don't do anything with this case as it's not evident how it's supposed to be handled
            }
            ClassInfo subclass = allSubclasses.iterator().next();
            if (!scannedResources.containsKey(subclass.name())) {
                scannedResources.put(subclass.name(), subclass);
                scannedResources.remove(abstractScannedResource.name());
                scannedResourcePaths.put(subclass.name(), scannedResourcePaths.get(abstractScannedResource.name()));
                scannedResourcePaths.remove(abstractScannedResource.name());
            }
        });

        Map<DotName, String> clientInterfaces = new HashMap<>(pathInterfaces);
        // for clients, it is enough to have @PATH annotations on methods only
        for (DotName interfaceName : interfacesWithPathOnMethods) {
            if (!clientInterfaces.containsKey(interfaceName)) {
                clientInterfaces.put(interfaceName, "");
            }
        }

        for (DotName interfaceName : new ArrayList<>(clientInterfaces.keySet())) {
            addClientSubInterfaces(interfaceName, index, clientInterfaces);
        }

        for (Map.Entry<DotName, String> i : pathInterfaces.entrySet()) {
            for (ClassInfo clazz : index.getAllKnownImplementors(i.getKey())) {
                if (!Modifier.isAbstract(clazz.flags())) {
                    if ((clazz.enclosingClass() == null || Modifier.isStatic(clazz.flags())) &&
                            clazz.enclosingMethod() == null) {
                        if (!scannedResources.containsKey(clazz.name())) {
                            scannedResources.put(clazz.name(), clazz);
                            scannedResourcePaths.put(clazz.name(), i.getValue());

                            // check for server exception mapper method in implementation class of the interface.
                            List<AnnotationInstance> exceptionMapperAnnotationInstances = clazz.annotationsMap()
                                    .get(ResteasyReactiveDotNames.SERVER_EXCEPTION_MAPPER);
                            if (exceptionMapperAnnotationInstances != null) {
                                for (AnnotationInstance instance : exceptionMapperAnnotationInstances) {
                                    if (instance.target().kind() != AnnotationTarget.Kind.METHOD) {
                                        continue;
                                    }
                                    methodExceptionMappers.add(instance.target().asMethod());
                                }
                            }
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

        // for clients, it is also enough to only have @GET, @POST, etc on methods and no PATH whatsoever
        Set<DotName> methodAnnotations = httpAnnotationToMethod.keySet();
        for (DotName methodAnnotation : methodAnnotations) {
            for (AnnotationInstance methodAnnotationInstance : index.getAnnotations(methodAnnotation)) {
                if (methodAnnotationInstance.target().kind() == AnnotationTarget.Kind.METHOD) {
                    MethodInfo annotatedMethod = methodAnnotationInstance.target().asMethod();
                    ClassInfo classWithJaxrsMethod = annotatedMethod.declaringClass();
                    if (Modifier.isAbstract(annotatedMethod.flags())
                            && Modifier.isInterface(classWithJaxrsMethod.flags())
                            && !clientInterfaces.containsKey(classWithJaxrsMethod.name())) {
                        clientInterfaces.put(classWithJaxrsMethod.name(), "");
                    }
                }
            }
        }

        //now index possible sub resources. These are all classes that have method annotations
        //that are not annotated @Path
        Deque<ClassInfo> toScan = new ArrayDeque<>();
        for (DotName methodAnnotation : httpAnnotationToMethod.keySet()) {
            for (AnnotationInstance instance : index.getAnnotations(methodAnnotation)) {
                MethodInfo method = instance.target().asMethod();
                ClassInfo classInfo = method.declaringClass();
                toScan.add(classInfo);
            }
        }
        //sub resources can also have just a path annotation
        //if they are 'intermediate' sub resources
        for (AnnotationInstance instance : index.getAnnotations(ResteasyReactiveDotNames.PATH)) {
            if (instance.target().kind() == AnnotationTarget.Kind.METHOD) {
                MethodInfo method = instance.target().asMethod();
                ClassInfo classInfo = method.declaringClass();
                toScan.add(classInfo);
            }
        }
        Map<DotName, ClassInfo> possibleSubResources = new HashMap<>();
        while (!toScan.isEmpty()) {
            ClassInfo classInfo = toScan.poll();
            if (scannedResources.containsKey(classInfo.name()) ||
                    pathInterfaces.containsKey(classInfo.name()) ||
                    possibleSubResources.containsKey(classInfo.name())) {
                continue;
            }
            if (hasJaxRsFieldInjection(classInfo, index)) {
                requestScopedResources.add(classInfo.name());
            }
            possibleSubResources.put(classInfo.name(), classInfo);
            //we need to also look for all subclasses and interfaces
            //they may have type variables that need to be handled
            toScan.addAll(index.getKnownDirectImplementors(classInfo.name()));
            toScan.addAll(index.getKnownDirectSubclasses(classInfo.name()));
        }

        return new ResourceScanningResult(index, scannedResources,
                scannedResourcePaths, possibleSubResources, pathInterfaces, clientInterfaces, resourcesThatNeedCustomProducer,
                httpAnnotationToMethod, methodExceptionMappers, requestScopedResources);
    }

    private static void addClientSubInterfaces(DotName interfaceName, IndexView index,
            Map<DotName, String> clientInterfaces) {

        Collection<ClassInfo> subclasses = index.getKnownDirectImplementors(interfaceName);
        for (ClassInfo subclass : subclasses) {
            if (!clientInterfaces.containsKey(subclass.name()) && Modifier.isInterface(subclass.flags())) {
                clientInterfaces.put(subclass.name(), clientInterfaces.get(interfaceName));
                addClientSubInterfaces(subclass.name(), index, clientInterfaces);
            }
        }

    }

    private static MethodInfo hasJaxRsCtorParams(ClassInfo classInfo) {
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
        if (ctor.parametersCount() == 0) { // default ctor - we don't need to do anything
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

    public static final Set<DotName> ANNOTATIONS_REQUIRING_FIELD_INJECTION = new HashSet<>(
            Arrays.asList(PATH_PARAM, QUERY_PARAM, HEADER_PARAM, FORM_PARAM, MATRIX_PARAM,
                    COOKIE_PARAM, REST_PATH_PARAM, REST_QUERY_PARAM, REST_HEADER_PARAM, REST_FORM_PARAM, REST_MATRIX_PARAM,
                    REST_COOKIE_PARAM, BEAN_PARAM));

    private static boolean hasJaxRsFieldInjection(ClassInfo classInfo, IndexView index) {
        while (true) {
            for (FieldInfo field : classInfo.fields()) {
                List<AnnotationInstance> annotations = field.annotations();
                if (annotations.stream()
                        .anyMatch(an -> ANNOTATIONS_REQUIRING_FIELD_INJECTION.contains(an.name()))) {
                    return true;
                }
            }
            DotName parentDotName = classInfo.superName();
            if (parentDotName.equals(ResteasyReactiveDotNames.OBJECT)) {
                return false;
            }
            classInfo = index.getClassByName(parentDotName);
            if (classInfo == null) {
                return false;
            }
        }

    }

}
