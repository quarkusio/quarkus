package io.quarkus.jaxb.deployment;

import java.io.IOError;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.annotation.XmlAccessOrder;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAnyAttribute;
import jakarta.xml.bind.annotation.XmlAnyElement;
import jakarta.xml.bind.annotation.XmlAttachmentRef;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlElementRefs;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlID;
import jakarta.xml.bind.annotation.XmlIDREF;
import jakarta.xml.bind.annotation.XmlInlineBinaryData;
import jakarta.xml.bind.annotation.XmlList;
import jakarta.xml.bind.annotation.XmlMimeType;
import jakarta.xml.bind.annotation.XmlMixed;
import jakarta.xml.bind.annotation.XmlNs;
import jakarta.xml.bind.annotation.XmlRegistry;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSchema;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlSchemaTypes;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapters;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SynthesisFinishedBuildItem;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BeanResolver;
import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyIgnoreWarningBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.jaxb.runtime.JaxbConfig;
import io.quarkus.jaxb.runtime.JaxbContextConfigRecorder;
import io.quarkus.jaxb.runtime.JaxbContextProducer;

public class JaxbProcessor {

    private static Logger LOG = Logger.getLogger(JaxbProcessor.class);

    private static final List<Class<? extends Annotation>> JAXB_ANNOTATIONS = List.of(
            XmlAccessorType.class,
            XmlAnyAttribute.class,
            XmlAnyElement.class,
            XmlAttachmentRef.class,
            XmlAttribute.class,
            XmlElement.class,
            XmlElementDecl.class,
            XmlElementRef.class,
            XmlElementRefs.class,
            XmlElements.class,
            XmlElementWrapper.class,
            XmlEnum.class,
            XmlEnumValue.class,
            XmlID.class,
            XmlIDREF.class,
            XmlInlineBinaryData.class,
            XmlList.class,
            XmlMimeType.class,
            XmlMixed.class,
            XmlNs.class,
            XmlRegistry.class,
            XmlRootElement.class,
            XmlSchema.class,
            XmlSchemaType.class,
            XmlSchemaTypes.class,
            XmlSeeAlso.class,
            XmlTransient.class,
            XmlType.class,
            XmlValue.class,
            XmlJavaTypeAdapter.class,
            XmlJavaTypeAdapters.class);

    private static final List<Class<?>> JAXB_REFLECTIVE_CLASSES = List.of(XmlAccessOrder.class);

    private static final DotName XML_ROOT_ELEMENT = DotName.createSimple(XmlRootElement.class.getName());
    private static final DotName XML_TYPE = DotName.createSimple(XmlType.class.getName());
    private static final DotName XML_REGISTRY = DotName.createSimple(XmlRegistry.class.getName());
    private static final DotName XML_SCHEMA = DotName.createSimple(XmlSchema.class.getName());
    private static final DotName XML_JAVA_TYPE_ADAPTER = DotName.createSimple(XmlJavaTypeAdapter.class.getName());
    private static final DotName XML_ANY_ELEMENT = DotName.createSimple(XmlAnyElement.class.getName());
    private static final DotName XML_SEE_ALSO = DotName.createSimple(XmlSeeAlso.class.getName());
    private static final DotName XML_TRANSIENT = DotName.createSimple(XmlTransient.class.getName());
    private static final DotName XML_ACCESSOR_TYPE = DotName.createSimple(XmlAccessorType.class.getName());

    private static final List<DotName> JAXB_ROOT_ANNOTATIONS = List.of(XML_ROOT_ELEMENT, XML_TYPE, XML_REGISTRY);

    private static final List<DotName> IGNORE_TYPES = List.of(DotName.createSimple("javax.xml.datatype.XMLGregorianCalendar"));

    private static final List<String> NATIVE_PROXY_DEFINITIONS = List.of(
            "org.glassfish.jaxb.core.marshaller.CharacterEscapeHandler",
            "com.sun.xml.txw2.output.CharacterEscapeHandler",
            "org.glassfish.jaxb.core.v2.schemagen.episode.Bindings",
            "org.glassfish.jaxb.core.v2.schemagen.episode.SchemaBindings",
            "org.glassfish.jaxb.core.v2.schemagen.episode.Klass",
            "org.glassfish.jaxb.core.v2.schemagen.episode.Package",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.Annotated",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.Annotation",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.Any",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.Appinfo",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.AttrDecls",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.AttributeType",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.ComplexContent",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.ComplexExtension",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.ComplexRestriction",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.ComplexType",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.ComplexTypeHost",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.ComplexTypeModel",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.ContentModelContainer",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.Documentation",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.Element",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.ExplicitGroup",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.ExtensionType",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.FixedOrDefault",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.Import",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.List",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.LocalAttribute",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.LocalElement",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.NestedParticle",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.NoFixedFacet",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.Occurs",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.Particle",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.Redefinable",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.Schema",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.SchemaTop",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.SimpleContent",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.SimpleDerivation",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.SimpleExtension",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.SimpleRestriction",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.SimpleRestrictionModel",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.SimpleType",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.SimpleTypeHost",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.TopLevelAttribute",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.TopLevelElement",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.TypeDefParticle",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.TypeHost",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.Union",
            "org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.Wildcard",
            "com.sun.xml.txw2.TypedXmlWriter");

    @BuildStep
    void processAnnotationsAndIndexFiles(
            BuildProducer<NativeImageSystemPropertyBuildItem> nativeImageProps,
            BuildProducer<ServiceProviderBuildItem> providerItem,
            BuildProducer<NativeImageProxyDefinitionBuildItem> proxyDefinitions,
            CombinedIndexBuildItem combinedIndexBuildItem,
            List<JaxbFileRootBuildItem> fileRoots,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchies,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<NativeImageResourceBuildItem> resource,
            BuildProducer<NativeImageResourceBundleBuildItem> resourceBundle,
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeClasses,
            BuildProducer<JaxbClassesToBeBoundBuildItem> classesToBeBoundProducer,
            ApplicationArchivesBuildItem applicationArchivesBuildItem) throws ClassNotFoundException {

        List<String> classesToBeBound = new ArrayList<>();
        IndexView index = combinedIndexBuildItem.getIndex();

        // Register classes for reflection based on JAXB annotations
        boolean jaxbRootAnnotationsDetected = false;

        for (DotName jaxbRootAnnotation : JAXB_ROOT_ANNOTATIONS) {
            for (AnnotationInstance jaxbRootAnnotationInstance : index
                    .getAnnotations(jaxbRootAnnotation)) {
                if (jaxbRootAnnotationInstance.target().kind() == Kind.CLASS
                        && !JAXB_ANNOTATIONS.contains(jaxbRootAnnotationInstance.target().asClass().getClass())) {
                    ClassInfo targetClassInfo = jaxbRootAnnotationInstance.target().asClass();
                    addReflectiveHierarchyClass(targetClassInfo, reflectiveHierarchies, index);
                    classesToBeBound.add(targetClassInfo.name().toString());
                    jaxbRootAnnotationsDetected = true;
                }
            }
        }

        if (!jaxbRootAnnotationsDetected && fileRoots.isEmpty()) {
            return;
        }

        // Register package-infos for reflection
        for (AnnotationInstance xmlSchemaInstance : index.getAnnotations(XML_SCHEMA)) {
            if (xmlSchemaInstance.target().kind() == Kind.CLASS) {
                String className = xmlSchemaInstance.target().asClass().name().toString();

                reflectiveClass.produce(ReflectiveClassBuildItem.builder(className).build());
            }
        }

        // Register XML Java type adapters for reflection
        for (AnnotationInstance xmlJavaTypeAdapterInstance : index.getAnnotations(XML_JAVA_TYPE_ADAPTER)) {
            reflectiveClass.produce(
                    ReflectiveClassBuildItem.builder(xmlJavaTypeAdapterInstance.value().asClass().name().toString())
                            .methods().fields().build());
        }

        if (!index.getAnnotations(XML_ANY_ELEMENT).isEmpty()) {
            addReflectiveClass(reflectiveClass, false, false, "jakarta.xml.bind.annotation.W3CDomHandler");
        }

        JAXB_ANNOTATIONS.stream()
                .map(Class::getName)
                .forEach(className -> {
                    addReflectiveClass(reflectiveClass, true, false, className);
                });

        // Register @XmlSeeAlso
        proxyDefinitions.produce(new NativeImageProxyDefinitionBuildItem(XmlSeeAlso.class.getName(),
                "org.glassfish.jaxb.core.v2.model.annotation.Locatable"));
        for (AnnotationInstance xmlSeeAlsoAnn : index.getAnnotations(XML_SEE_ALSO)) {
            AnnotationValue value = xmlSeeAlsoAnn.value();
            Type[] types = value.asClassArray();
            for (Type t : types) {
                addReflectiveClass(reflectiveClass, false, false, t.name().toString());
            }
        }
        // Register Native proxy definitions
        for (String s : NATIVE_PROXY_DEFINITIONS) {
            proxyDefinitions.produce(new NativeImageProxyDefinitionBuildItem(s));
        }

        for (JaxbFileRootBuildItem i : fileRoots) {
            iterateResources(applicationArchivesBuildItem, i.getFileRoot(), resource, reflectiveClass, classesToBeBound);
        }

        classesToBeBoundProducer.produce(new JaxbClassesToBeBoundBuildItem(classesToBeBound));
    }

    @BuildStep
    void ignoreWarnings(BuildProducer<ReflectiveHierarchyIgnoreWarningBuildItem> ignoreWarningProducer) {
        for (DotName type : IGNORE_TYPES) {
            ignoreWarningProducer.produce(new ReflectiveHierarchyIgnoreWarningBuildItem(type));
        }
    }

    @BuildStep
    void registerClasses(
            BuildProducer<NativeImageSystemPropertyBuildItem> nativeImageProps,
            BuildProducer<ServiceProviderBuildItem> providerItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<NativeImageResourceBundleBuildItem> resourceBundle) {
        addReflectiveClass(reflectiveClass, true, false, "org.glassfish.jaxb.runtime.v2.ContextFactory");
        addReflectiveClass(reflectiveClass, true, false, "com.sun.xml.internal.stream.XMLInputFactoryImpl");
        addReflectiveClass(reflectiveClass, true, false, "com.sun.xml.internal.stream.XMLOutputFactoryImpl");
        addReflectiveClass(reflectiveClass, true, false, "com.sun.org.apache.xpath.internal.functions.FuncNot");
        addReflectiveClass(reflectiveClass, true, false, "com.sun.org.apache.xerces.internal.impl.dv.xs.SchemaDVFactoryImpl");

        addResourceBundle(resourceBundle, "jakarta.xml.bind.Messages");
        addResourceBundle(resourceBundle, "jakarta.xml.bind.helpers.Messages");

        nativeImageProps
                .produce(new NativeImageSystemPropertyBuildItem("com.sun.xml.bind.v2.bytecode.ClassTailor.noOptimize", "true"));

        JAXB_REFLECTIVE_CLASSES.stream()
                .map(Class::getName)
                .forEach(className -> addReflectiveClass(reflectiveClass, true, false, className));

        providerItem
                .produce(new ServiceProviderBuildItem(JAXBContext.class.getName(),
                        "org.glassfish.jaxb.runtime.v2.ContextFactory"));
    }

    @BuildStep
    FilteredJaxbClassesToBeBoundBuildItem filterBoundClasses(
            JaxbConfig config,
            List<JaxbClassesToBeBoundBuildItem> classesToBeBoundBuildItems) {

        FilteredJaxbClassesToBeBoundBuildItem.Builder builder = FilteredJaxbClassesToBeBoundBuildItem.builder();
        classesToBeBoundBuildItems.stream()
                .map(JaxbClassesToBeBoundBuildItem::getClasses)
                .forEach(builder::classNames);

        // remove classes that have been excluded by users
        if (config.excludeClasses.isPresent()) {
            builder.classNameExcludes(config.excludeClasses.get());
        }
        return builder.build();
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void bindClassesToJaxbContext(
            JaxbConfig config,
            FilteredJaxbClassesToBeBoundBuildItem filteredClassesToBeBound,
            SynthesisFinishedBuildItem beanContainerState,
            JaxbContextConfigRecorder jaxbContextConfig /* Force the build time container to invoke this method */) {

        jaxbContextConfig.reset();
        final BeanResolver beanResolver = beanContainerState.getBeanResolver();
        final Set<BeanInfo> beans = beanResolver
                .resolveBeans(Type.create(DotName.createSimple(JAXBContext.class), org.jboss.jandex.Type.Kind.CLASS));
        if (!beans.isEmpty()) {
            jaxbContextConfig.addClassesToBeBound(filteredClassesToBeBound.getClasses());
            if (config.validateJaxbContext) {
                validateJaxbContext(filteredClassesToBeBound, beanResolver, beans);
            }
        }
    }

    @BuildStep
    void registerProduces(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(new AdditionalBeanBuildItem(JaxbContextProducer.class));
    }

    private void validateJaxbContext(FilteredJaxbClassesToBeBoundBuildItem filteredClassesToBeBound, BeanResolver beanResolver,
            Set<BeanInfo> beans) {
        final BeanInfo bean = beanResolver.resolveAmbiguity(beans);
        if (bean.isDefaultBean()) {
            /*
             * Validate the default JAXB context at build time and fail early.
             * Do this only if the user application actually requires the default JAXBContext bean
             */
            try {
                JAXBContext.newInstance(filteredClassesToBeBound.getClasses().toArray(new Class[0]));
            } catch (JAXBException e) {
                /*
                 * Producing a ValidationErrorBuildItem would perhaps be more natural here,
                 * but doing so causes a cycle between this and reactive JAXB extension
                 * Throwing from here works well too
                 */
                throw new DeploymentException("Failed to create or validate the default JAXBContext", e);
            }
        }
    }

    private void handleJaxbFile(Path p, BuildProducer<NativeImageResourceBuildItem> resource,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            List<String> classesToBeBound) {
        try {
            String path = p.toAbsolutePath().toString().substring(1);
            String pkg = p.toAbsolutePath().getParent().toString().substring(1)
                    .replace(p.getFileSystem().getSeparator(), ".") + ".";

            resource.produce(new NativeImageResourceBuildItem(path));

            for (String line : Files.readAllLines(p)) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    String clazz = pkg + line;
                    Class<?> cl = Class.forName(clazz, false, Thread.currentThread().getContextClassLoader());
                    classesToBeBound.add(clazz);

                    while (cl != Object.class) {
                        reflectiveClass.produce(ReflectiveClassBuildItem.builder(cl).methods().fields().build());
                        cl = cl.getSuperclass();
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void iterateResources(ApplicationArchivesBuildItem applicationArchivesBuildItem, String path,
            BuildProducer<NativeImageResourceBuildItem> resource, BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            List<String> classesToBeBound) {
        for (ApplicationArchive archive : applicationArchivesBuildItem.getAllApplicationArchives()) {
            archive.accept(tree -> {
                var arch = tree.getPath(path);
                if (arch != null && Files.isDirectory(arch)) {
                    JaxbProcessor.safeWalk(arch)
                            .filter(Files::isRegularFile)
                            .filter(p -> p.getFileName().toString().equals("jaxb.index"))
                            .forEach(p1 -> handleJaxbFile(p1, resource, reflectiveClass, classesToBeBound));
                }
            });
        }
    }

    public static Stream<Path> safeWalk(Path p) {
        try {
            return Files.walk(p);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    private void addReflectiveHierarchyClass(ClassInfo classInfo,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy,
            IndexView index) {
        Type jandexType = Type.create(classInfo.name(), Type.Kind.CLASS);

        reflectiveHierarchy.produce(new ReflectiveHierarchyBuildItem.Builder()
                .type(jandexType)
                .index(index)
                .ignoreTypePredicate(t -> ReflectiveHierarchyBuildItem.DefaultIgnoreTypePredicate.INSTANCE.test(t)
                        || IGNORE_TYPES.contains(t))
                .ignoreFieldPredicate(JaxbProcessor::isFieldIgnored)
                .ignoreMethodPredicate(JaxbProcessor::isMethodIgnored)
                .source(getClass().getSimpleName() + " > " + jandexType.name().toString())
                .build());
    }

    private void addReflectiveClass(BuildProducer<ReflectiveClassBuildItem> reflectiveClass, boolean methods, boolean fields,
            String... className) {
        reflectiveClass.produce(new ReflectiveClassBuildItem(methods, fields, className));
    }

    private void addResourceBundle(BuildProducer<NativeImageResourceBundleBuildItem> resourceBundle, String bundle) {
        resourceBundle.produce(new NativeImageResourceBundleBuildItem(bundle));
    }

    private static boolean isFieldIgnored(FieldInfo fieldInfo) {
        // see JakartaXmlBindingAnnotationIntrospector#isVisible(AnnotatedField f)
        // and XmlAccessType
        if (fieldInfo.hasAnnotation(XML_TRANSIENT)) {
            return true;
        }
        if (Modifier.isStatic(fieldInfo.flags())) {
            return true;
        }

        for (Class<? extends Annotation> jaxbAnnotation : JAXB_ANNOTATIONS) {
            if (fieldInfo.hasAnnotation(jaxbAnnotation)) {
                return true;
            }
        }

        ClassInfo declaringClass = fieldInfo.declaringClass();
        XmlAccessType xmlAccessType = getXmlAccessType(declaringClass);
        switch (xmlAccessType) {
            case FIELD:
                return false;
            case PROPERTY:
                return true;
            case PUBLIC_MEMBER:
                return !Modifier.isPublic(fieldInfo.flags());
            case NONE:
                return true;
            default:
                return true;
        }
    }

    private static boolean isMethodIgnored(MethodInfo methodInfo) {
        // see JakartaXmlBindingAnnotationIntrospector#isVisible(AnnotatedMethod m)
        // and XmlAccessType
        MethodInfo getterSetterCounterpart = getGetterSetterCounterPart(methodInfo);

        if (methodInfo.hasAnnotation(XML_TRANSIENT) ||
                (getterSetterCounterpart != null && getterSetterCounterpart.hasAnnotation(XML_TRANSIENT))) {
            return true;
        }
        if (Modifier.isStatic(methodInfo.flags())) {
            return true;
        }

        // if method has a JAXB annotation, we consider it
        for (Class<? extends Annotation> jaxbAnnotation : JAXB_ANNOTATIONS) {
            if (methodInfo.hasAnnotation(jaxbAnnotation)) {
                return false;
            }
        }

        ClassInfo declaringClass = methodInfo.declaringClass();
        XmlAccessType xmlAccessType = getXmlAccessType(declaringClass);
        switch (xmlAccessType) {
            case FIELD:
                return true;
            case PROPERTY:
            case PUBLIC_MEMBER:
                return !Modifier.isPublic(methodInfo.flags());
            case NONE:
                return true;
            default:
                return true;
        }
    }

    private static MethodInfo getGetterSetterCounterPart(MethodInfo methodInfo) {
        if (!methodInfo.name().startsWith("get") || methodInfo.parametersCount() > 0) {
            return null;
        }

        return methodInfo.declaringClass().method(methodInfo.name().replaceFirst("get", "set"), methodInfo.returnType());
    }

    private static XmlAccessType getXmlAccessType(ClassInfo classInfo) {
        AnnotationInstance xmlAccessorTypeAi = classInfo.annotation(XML_ACCESSOR_TYPE);
        if (xmlAccessorTypeAi == null) {
            return XmlAccessType.PUBLIC_MEMBER;
        }

        AnnotationValue xmlAccessorType = xmlAccessorTypeAi.value();
        if (xmlAccessorType == null) {
            return XmlAccessType.PUBLIC_MEMBER;
        }

        return XmlAccessType.valueOf(xmlAccessorTypeAi.value().asEnum());
    }
}
