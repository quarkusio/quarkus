package io.quarkus.cxf.deployment;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.jws.WebParam;
import javax.servlet.DispatcherType;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.cxf.common.jaxb.JAXBUtils;
import org.apache.cxf.common.util.StringUtils;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.quarkus.arc.Unremovable;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.cxf.runtime.CXFQuarkusServlet;
import io.quarkus.cxf.runtime.CXFServletRecorder;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.util.HashUtil;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.jaxb.deployment.ForceJaxbBuildItem;
import io.quarkus.undertow.deployment.FilterBuildItem;
import io.quarkus.undertow.deployment.ServletBuildItem;
import io.quarkus.undertow.deployment.ServletInitParamBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;

/**
 * Processor that finds CXF web service classes in the deployment
 */
public class CxfProcessor {

    private static final String JAX_WS_SERVLET_NAME = "org.apache.cxf.transport.servlet.CXFNonSpringServlet;";
    private static final String JAX_WS_FILTER_NAME = JAX_WS_SERVLET_NAME;
    private static final DotName WEBSERVICE_ANNOTATION = DotName.createSimple("javax.jws.WebService");
    private static final DotName WEBMETHOD_ANNOTATION = DotName.createSimple("javax.jws.WebMethod");
    private static final DotName WEBPARAM_ANNOTATION = DotName.createSimple("javax.jws.WebParam");
    private static final DotName WEBRESULT_ANNOTATION = DotName.createSimple("javax.jws.WebResult");
    private static final DotName REQUEST_WRAPPER_ANNOTATION = DotName.createSimple("javax.xml.ws.RequestWrapper");
    private static final DotName RESPONSE_WRAPPER_ANNOTATION = DotName.createSimple("javax.xml.ws.ResponseWrapper");
    private static final DotName SOAPBINDING_ANNOTATION = DotName.createSimple("javax.jws.soap.SOAPBinding");
    private static final DotName WEBFAULT_ANNOTATION = DotName.createSimple("javax.jws.WebFault");
    private static final DotName ABSTRACT_FEATURE = DotName.createSimple("org.apache.cxf.feature.AbstractFeature");
    private static final DotName ABSTRACT_INTERCEPTOR = DotName.createSimple("org.apache.cxf.phase.AbstractPhaseInterceptor");
    private static final DotName DATABINDING = DotName.createSimple("org.apache.cxf.databinding");
    private static final Logger LOGGER = Logger.getLogger(CxfProcessor.class);
    private static final List<Class<? extends Annotation>> JAXB_ANNOTATIONS = Arrays.asList(
            XmlList.class,
            XmlAttachmentRef.class,
            XmlJavaTypeAdapter.class,
            XmlMimeType.class,
            XmlElement.class,
            XmlElementWrapper.class);

    /**
     * JAX-RS configuration.
     */
    CxfConfig cxfConfig;

    @BuildStep
    public void generateWSDL(BuildProducer<NativeImageResourceBuildItem> ressources) {
        for (CxfEndpointConfig endpointCfg : cxfConfig.endpoints.values()) {
            if (endpointCfg.wsdlPath.isPresent()) {
                ressources.produce(new NativeImageResourceBuildItem(endpointCfg.wsdlPath.get()));
            }
        }
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void build(List<CXFServletInfoBuildItem> cxfServletInfos,
            BuildProducer<RouteBuildItem> routes,
            CXFServletRecorder recorder) {
        for (CXFServletInfoBuildItem cxfServletInfo : cxfServletInfos) {
            recorder.registerCXFServlet(cxfServletInfo.getPath(),
                    cxfServletInfo.getClassName(), cxfServletInfo.getInInterceptors(),
                    cxfServletInfo.getOutInterceptors(), cxfServletInfo.getOutFaultInterceptors(),
                    cxfServletInfo.getInFaultInterceptors(), cxfServletInfo.getFeatures(), cxfServletInfo.getSei(),
                    cxfServletInfo.getWsdlPath());
        }
    }

    private static final String RESPONSE_CLASS_POSTFIX = "Response";

    //TODO check if better to reuse the cxf parsing system to generate only asm from their.
    private MethodDescriptor createWrapper(boolean isRequest, String operationName, String namespace, String resultName,
            String resultType,
            List<WrapperParameter> params, ClassOutput classOutput, String pkg, String className,
            List<MethodDescriptor> getters, List<MethodDescriptor> setters) {
        //WrapperClassGenerator
        ClassCreator classCreator = ClassCreator.builder().classOutput(classOutput)
                .className(pkg + "." + className + (isRequest ? "" : RESPONSE_CLASS_POSTFIX))
                .build();
        MethodDescriptor ctorDescriptor = null;

        classCreator.addAnnotation(AnnotationInstance.create(
                DotName.createSimple(XmlRootElement.class.getName()), null,
                new AnnotationValue[] { AnnotationValue.createStringValue("name", operationName),
                        AnnotationValue.createStringValue("namespace", namespace) }));
        classCreator.addAnnotation(AnnotationInstance.create(
                DotName.createSimple(XmlAccessorType.class.getName()), null,
                new AnnotationValue[] {
                        AnnotationValue.createEnumValue("value",
                                DotName.createSimple(XmlAccessType.class.getName()), "FIELD") }));
        //if (!anonymous)
        classCreator.addAnnotation(AnnotationInstance.create(
                DotName.createSimple(XmlType.class.getName()), null,
                new AnnotationValue[] { AnnotationValue.createStringValue("name", operationName),
                        Antrtt(gtnotationValue.createStringValue("namespace", namespace) }));

        classCreator.addAnnotation(AnnotationInstance.create(
                DotName.createSimple(Unremovable.class.getName()), null,
                new AnnotationValue[] { AnnotationValue.createStringValue("name", operationName),
                        AnnotationValue.createStringValue("namespace", namespace) }));
        // else
        //classCreator.addAnnotation(AnnotationInstance.create(
        //        DotName.createSimple(XmlType.class.getName()), null,
        //        new AnnotationValue[] { AnnotationValue.createStringValue("name", "")}));
        try (MethodCreator ctor = classCreator.getMethodCreator("<init>", "V")) {
            ctor.setModifiers(Modifier.PUBLIC);
            ctor.invokeSpecialMethod(MethodDescriptor.ofConstructor(Object.class), ctor.getThis());
            ctor.returnValue(null);
            ctorDescriptor = ctor.getMethodDescriptor();
        }
        if (!isRequest && resultName != null && resultType != null && !resultType.equals("void")) {
            //TODO check if method annotation must been forwarded
            try {
                createWrapperClassField(getters, setters, classCreator, resultType, resultName,
                        new ArrayList<AnnotationInstance>());
            } catch (Exception e) {
                throw new RuntimeException("failed to create fields:" + resultType);
            }
        }
        int i = 0;
        for (WrapperParameter param : params) {

            AnnotationInstance webparamAnnotation = param.getAnnotation(WEBPARAM_ANNOTATION);
            String webParamName = "arg" + i;
            String webParamTargetNamespace = namespace;
            WebParam.Mode webParamMode = WebParam.Mode.IN;
            boolean webParamHeader = false;
            //not used
            //String webParamPartName = webparamAnnotation.value("partName").asString();
            if (webparamAnnotation != null) {
                AnnotationValue val = webparamAnnotation.value("name");
                if (val != null) {
                    webParamName = val.asString();
                }
                val = webparamAnnotation.value("targetNamespace");
                if (val != null) {
                    webParamTargetNamespace = val.asString();
                }
                val = webparamAnnotation.value("mode");
                if (val != null) {
                    webParamMode = WebParam.Mode.valueOf(val.asEnum());
                }
                val = webparamAnnotation.value("header");
                if (val != null) {
                    webParamHeader = val.asBoolean();
                }
            }

            if ((webParamMode == WebParam.Mode.OUT && isRequest)
                    || (webParamMode == WebParam.Mode.IN && !isRequest))
                continue;

            createWrapperClassField(getters, setters, classCreator, param.getParameterType().name().toString(), webParamName,
                    param.getAnnotations());
            i++;
        }
        classCreator.close();
        return ctorDescriptor;
    }

    private void createWrapperClassField(List<MethodDescriptor> getters, List<MethodDescriptor> setters,
            ClassCreator classCreator, String identifier, String webParamName, List<AnnotationInstance> paramAnnotations) {
        String fieldName = JAXBUtils.nameToIdentifier(webParamName, JAXBUtils.IdentifierType.VARIABLE);

        FieldCreator field = classCreator.getFieldCreator(fieldName, identifier)
                .setModifiers(Modifier.PRIVATE);
        List<DotName> jaxbAnnotationDotNames = JAXB_ANNOTATIONS.stream()
                .map(Class::getName)
                .map(DotName::createSimple)
                .collect(Collectors.toList());
        boolean annotationAdded = false;
        for (AnnotationInstance ann : paramAnnotations) {
            if (jaxbAnnotationDotNames.contains(ann.name())) {
                // copy jaxb annotation from param to
                field.addAnnotation(AnnotationInstance.create(ann.name(), null, ann.values()));
                annotationAdded = true;
            }
        }
        if (!annotationAdded) {
            List<AnnotationValue> annotationValues = new ArrayList<>();
            annotationValues.add(AnnotationValue.createStringValue("name", webParamName));
            //TODO handle a config for factory.isWrapperPartQualified, factory.isWrapperPartNillable, factory.getWrapperPartMinOccurs
            // and add annotation value here for it
            field.addAnnotation(AnnotationInstance.create(DotName.createSimple(XmlElement.class.getName()),
                    null, annotationValues));
        }
        MethodCreator getter = classCreator.getMethodCreator(
                JAXBUtils.nameToIdentifier(webParamName, JAXBUtils.IdentifierType.GETTER),
                identifier);
        getter.setModifiers(Modifier.PUBLIC);
        getter.returnValue(getter.readInstanceField(field.getFieldDescriptor(), getter.getThis()));
        getters.add(getter.getMethodDescriptor());

        MethodCreator setter = classCreator.getMethodCreator(
                JAXBUtils.nameToIdentifier(webParamName, JAXBUtils.IdentifierType.SETTER), void.class,
                identifier);
        setter.setModifiers(Modifier.PUBLIC);
        setter.writeInstanceField(field.getFieldDescriptor(), setter.getThis(), setter.getMethodParam(0));
        setters.add(setter.getMethodDescriptor());

    }

    private String getNamespaceFromPackage(String pkg) {
        //TODO XRootElement then XmlSchema then derived of package
        String[] strs = pkg.split("\\.");
        StringBuilder b = new StringBuilder("http://");
        for (int i = strs.length - 1; i >= 0; i--) {
            b.append(strs[i]);
            b.append("/");
        }

        return b.toString();
    }

    private static Set<String> classHelpers = new HashSet<>();
    static final MethodDescriptor LIST_GET = MethodDescriptor.ofMethod(List.class, "get", Object.class, int.class);
    static final MethodDescriptor LIST_ADDALL = MethodDescriptor.ofMethod(List.class, "addAll", Collection.class,
            boolean.class);

    static final MethodDescriptor ARRAYLIST_CTOR = MethodDescriptor.ofConstructor(ArrayList.class, int.class);
    static final MethodDescriptor JAXBELEMENT_GETVALUE = MethodDescriptor.ofMethod(JAXBElement.class, "getValue", Object.class);

    static final MethodDescriptor LIST_ADD = MethodDescriptor.ofMethod(List.class, "add", boolean.class, Object.class);
    private static final String WRAPPER_HELPER_POSTFIX = "_WrapperTypeHelper";
    private static final String WRAPPER_FACTORY_POSTFIX = "Factory";

    private String computeSignature(List<MethodDescriptor> getters, List<MethodDescriptor> setters) {
        StringBuilder b = new StringBuilder();
        b.append(setters.size()).append(':');
        for (int x = 0; x < setters.size(); x++) {
            if (getters.get(x) == null) {
                b.append("null,");
            } else {
                b.append(getters.get(x).getName()).append('/');
                b.append(getters.get(x).getReturnType()).append(',');
            }
        }
        return b.toString();
    }

    private void createWrapperHelper(ClassOutput classOutput, String pkg, String className,
            MethodDescriptor ctorDescriptor, List<MethodDescriptor> getters, List<MethodDescriptor> setters) {
        //WrapperClassGenerator
        int count = 1;
        String newClassName = pkg + "." + className + WRAPPER_HELPER_POSTFIX + count;

        while (classHelpers.contains(newClassName)) {
            count++;
            newClassName = pkg + "." + className + WRAPPER_HELPER_POSTFIX + count;
        }
        classHelpers.add(newClassName);
        ClassCreator classCreator = ClassCreator.builder().classOutput(classOutput)
                .className(newClassName)
                .build();
        Class<?> objectFactoryCls = null;
        try {
            objectFactoryCls = Class.forName(pkg + ".ObjectFactory");
            //TODO object factory creator
            // but must be always null for generated class so not sure if we keep that.
            //String methodName = "create" + className + setMethod.getName().substring(3);
        } catch (ClassNotFoundException e) {
            //silently failed
        }

        FieldCreator factoryField = null;
        if (objectFactoryCls != null) {
            factoryField = classCreator.getFieldCreator("factory", objectFactoryCls.getName());
        }

        try (MethodCreator ctor = classCreator.getMethodCreator("<init>", "V")) {
            ctor.setModifiers(Modifier.PUBLIC);
            ctor.invokeSpecialMethod(MethodDescriptor.ofConstructor(Object.class), ctor.getThis());
            if (objectFactoryCls != null && factoryField != null) {
                ResultHandle factoryRH = ctor.newInstance(MethodDescriptor.ofConstructor(objectFactoryCls));
                ctor.writeInstanceField(factoryField.getFieldDescriptor(), ctor.getThis(), factoryRH);
            }
            ctor.returnValue(null);
        }

        try (MethodCreator getSignature = classCreator.getMethodCreator("getSignature", String.class)) {
            getSignature.setModifiers(Modifier.PUBLIC);
            ResultHandle signatureRH = getSignature.load(computeSignature(getters, setters));
            getSignature.returnValue(signatureRH);
        }
        try (MethodCreator createWrapperObject = classCreator.getMethodCreator("createWrapperObject", Object.class,
                List.class)) {
            createWrapperObject.setModifiers(Modifier.PUBLIC);
            ResultHandle wrapperRH = createWrapperObject.newInstance(ctorDescriptor);
            // get list<Object>
            ResultHandle listRH = createWrapperObject.getMethodParam(0);

            for (int i = 0; i < setters.size(); i++) {
                MethodDescriptor setter = setters.get(i);
                boolean isList = false;
                try {
                    isList = List.class.isAssignableFrom(Class.forName(setter.getParameterTypes()[0]));
                } catch (ClassNotFoundException e) {
                    // silent fail
                }
                if (isList) {
                    MethodDescriptor getter = getters.get(i);
                    ResultHandle getterListRH = createWrapperObject.invokeVirtualMethod(getter, wrapperRH);
                    ResultHandle listValRH = createWrapperObject.invokeInterfaceMethod(LIST_GET, listRH,
                            createWrapperObject.load(i));
                    createWrapperObject.checkCast(listValRH, List.class);
                    BranchResult isNullBranch = createWrapperObject.ifNull(getterListRH);
                    try (BytecodeCreator getterValIsNull = isNullBranch.trueBranch()) {
                        createWrapperObject.checkCast(listValRH, getter.getReturnType());
                        createWrapperObject.invokeVirtualMethod(setter, listValRH);

                    }
                    try (BytecodeCreator getterValIsNotNull = isNullBranch.falseBranch()) {
                        createWrapperObject.invokeInterfaceMethod(LIST_ADDALL, getterListRH, listValRH);
                    }
                } else {
                    boolean isjaxbElement = false;
                    try {
                        isjaxbElement = JAXBElement.class.isAssignableFrom(Class.forName(setter.getParameterTypes()[0]));
                    } catch (ClassNotFoundException e) {
                        // silent fail
                    }

                    ResultHandle listValRH = createWrapperObject.invokeInterfaceMethod(LIST_GET, listRH,
                            createWrapperObject.load(i));
                    if (isjaxbElement) {
                        ResultHandle factoryRH = createWrapperObject.readInstanceField(factoryField.getFieldDescriptor(),
                                createWrapperObject.getThis());
                        //TODO invoke virtual objectFactoryClass jaxbmethod
                    }
                    createWrapperObject.invokeVirtualMethod(setter, wrapperRH, listValRH);
                }
                // TODO if setter not created we add by field, but do not think that is needed because I generate everythings
            }

            createWrapperObject.returnValue(wrapperRH);
        }
        try (MethodCreator getWrapperParts = classCreator.getMethodCreator("getWrapperParts", List.class, Object.class)) {
            getWrapperParts.setModifiers(Modifier.PUBLIC);
            ResultHandle arraylistRH = getWrapperParts.newInstance(ARRAYLIST_CTOR, getWrapperParts.load(getters.size()));
            ResultHandle objRH = getWrapperParts.getMethodParam(0);
            ResultHandle wrapperRH = getWrapperParts.checkCast(objRH, pkg + "." + className);
            for (MethodDescriptor getter : getters) {
                ResultHandle wrapperValRH = getWrapperParts.invokeVirtualMethod(getter, wrapperRH);
                boolean isjaxbElement = false;
                try {
                    isjaxbElement = JAXBElement.class.isAssignableFrom(Class.forName(getter.getReturnType()));
                } catch (ClassNotFoundException e) {
                    // silent fail
                }
                if (isjaxbElement) {
                    wrapperValRH = getWrapperParts.ifNull(wrapperValRH).falseBranch().invokeVirtualMethod(JAXBELEMENT_GETVALUE,
                            wrapperValRH);
                }

                getWrapperParts.invokeInterfaceMethod(LIST_ADD, arraylistRH, wrapperValRH);
            }
            getWrapperParts.returnValue(arraylistRH);
        }

        classCreator.close();
    }

    private void createWrapperFactory(ClassOutput classOutput, String pkg, String className,
            MethodDescriptor ctorDescriptor) {
        String factoryClassName = pkg + "." + className + WRAPPER_FACTORY_POSTFIX;
        ClassCreator classCreator = ClassCreator.builder().classOutput(classOutput)
                .className(factoryClassName)
                .build();
        try (MethodCreator createWrapper = classCreator.getMethodCreator("create" + className, pkg + "." + className)) {
            createWrapper.setModifiers(Modifier.PUBLIC);
            ResultHandle[] argsRH = new ResultHandle[ctorDescriptor.getParameterTypes().length];
            for (int i = 0; i < ctorDescriptor.getParameterTypes().length; i++) {
                argsRH[i] = createWrapper.loadNull();
            }
            ResultHandle wrapperInstanceRH = createWrapper.newInstance(ctorDescriptor, argsRH);

            createWrapper.returnValue(wrapperInstanceRH);
        }

    }

    private void createException(ClassOutput classOutput, String exceptionClassName, DotName name) {
        ClassCreator classCreator = ClassCreator.builder().classOutput(classOutput).superClass(Exception.class)
                .className(exceptionClassName)
                .build();

        String FaultClassName = name.toString();

        FieldCreator field = classCreator.getFieldCreator("faultInfo", FaultClassName).setModifiers(Modifier.PRIVATE);
        //constructor
        try (MethodCreator ctor = classCreator.getMethodCreator("<init>", "V", String.class, FaultClassName)) {
            ctor.setModifiers(Modifier.PUBLIC);
            ctor.invokeSpecialMethod(MethodDescriptor.ofConstructor(Exception.class, String.class), ctor.getThis(),
                    ctor.getMethodParam(0));
            ctor.writeInstanceField(field.getFieldDescriptor(), ctor.getThis(), ctor.getMethodParam(1));
            ctor.returnValue(null);
        }
        try (MethodCreator getter = classCreator.getMethodCreator("getFaultInfo", FaultClassName)) {
            getter.setModifiers(Modifier.PUBLIC);
            getter.returnValue(getter.readInstanceField(field.getFieldDescriptor(), getter.getThis()));
        }
    }

    @BuildStep
    void markBeansAsUnremovable(BuildProducer<UnremovableBeanBuildItem> unremovables) {
        unremovables.produce(new UnremovableBeanBuildItem(new Predicate<BeanInfo>() {
            @Override
            public boolean test(BeanInfo beanInfo) {
                String nameWithPackage = beanInfo.getBeanClass().local();
                return nameWithPackage.contains(".jaxws_asm") || nameWithPackage.endsWith("ObjectFactory");
            }
        }));
        Set<String> extensibilities = new HashSet<>(Arrays.asList(
                "io.quarkus.cxf.runtime.AddressTypeExtensibility",
                "io.quarkus.cxf.runtime.HTTPClientPolicyExtensibility",
                "io.quarkus.cxf.runtime.HTTPServerPolicyExtensibility",
                "io.quarkus.cxf.runtime.XMLBindingMessageFormatExtensibility",
                "io.quarkus.cxf.runtime.XMLFormatBindingExtensibility"));
        unremovables
                .produce(new UnremovableBeanBuildItem(new UnremovableBeanBuildItem.BeanClassNamesExclusion(extensibilities)));
    }

    private static final String ANNOTATION_VALUE_INTERCEPTORS = "interceptors";

    class WrapperParameter {
        private Type parameterType;
        private List<AnnotationInstance> annotations;
        private String Name;

        WrapperParameter(Type parameterType, List<AnnotationInstance> annotations, String name) {
            this.parameterType = parameterType;
            this.annotations = annotations;
            Name = name;
        }

        public String getName() {
            return Name;
        }

        public List<AnnotationInstance> getAnnotations() {
            return annotations;
        }

        public AnnotationInstance getAnnotation(DotName dotname) {
            for (AnnotationInstance ai : annotations) {
                if (ai.name().equals(dotname))
                    return ai;
            }
            return null;
        }

        public Type getParameterType() {
            return parameterType;
        }
    }

    @BuildStep
    public void build(
            Capabilities capabilities,
            CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<ServletBuildItem> servlet,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<FilterBuildItem> filters,
            BuildProducer<CXFServletInfoBuildItem> cxfServletInfos,
            BuildProducer<ServletInitParamBuildItem> servletInitParameters,
            BuildProducer<ForceJaxbBuildItem> forceJaxb,
            BuildProducer<NativeImageProxyDefinitionBuildItem> proxies,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {
        IndexView index = combinedIndexBuildItem.getIndex();
        if (!capabilities.isCapabilityPresent(Capabilities.SERVLET)) {
            LOGGER.info("CXF running without servlet container.");
            LOGGER.info("- Add quarkus-undertow to run CXF within a servlet container");
            return;
        }

        forceJaxb.produce(new ForceJaxbBuildItem());

        for (AnnotationInstance annotation : index.getAnnotations(WEBSERVICE_ANNOTATION)) {
            if (annotation.target().kind() != AnnotationTarget.Kind.CLASS) {
                continue;
            }
            ClassInfo wsClassInfo = annotation.target().asClass();
            reflectiveClass
                    .produce(new ReflectiveClassBuildItem(true, true, wsClassInfo.name().toString()));
            unremovableBeans.produce(new UnremovableBeanBuildItem(
                    new UnremovableBeanBuildItem.BeanClassNameExclusion(wsClassInfo.name().toString())));
            if (!Modifier.isInterface(wsClassInfo.flags())) {
                continue;
            }
            //ClientProxyFactoryBean
            proxies.produce(new NativeImageProxyDefinitionBuildItem("java.io.Closeable",
                    "org.apache.cxf.endpoint.Client", wsClassInfo.name().toString()));
            String pkg = wsClassInfo.name().toString();
            int idx = pkg.lastIndexOf('.');
            if (idx != -1 && idx < pkg.length() - 1) {
                pkg = pkg.substring(0, idx);
            }

            pkg = pkg + ".jaxws_asm";
            //TODO config for boolean anonymous = factory.getAnonymousWrapperTypes();
            //if (getAnonymousWrapperTypes) pkg += "_an";
            AnnotationValue namespaceVal = annotation.value("targetNamespace");
            String namespace = namespaceVal != null ? namespaceVal.asString() : getNamespaceFromPackage(pkg);

            ClassOutput classOutput = new GeneratedBeanGizmoAdaptor(generatedBeans);
            //https://github.com/apache/cxf/blob/master/rt/frontend/jaxws/src/main/java/org/apache/cxf/jaxws/WrapperClassGenerator.java#L234
            ClassCreator PackageInfoCreator = ClassCreator.builder().classOutput(classOutput)
                    .className(pkg + ".package-info")
                    .build();
            List<AnnotationValue> annotationValues = new ArrayList<>();
            annotationValues.add(AnnotationValue.createStringValue("namespace", namespace));
            annotationValues.add(AnnotationValue.createEnumValue("elementFormDefault",
                    DotName.createSimple("javax.xml.bind.annotation.XmlNsForm"),
                    (namespaceVal != null) ? "QUALIFIED" : "UNQUALIFIED"));
            PackageInfoCreator.addAnnotation(AnnotationInstance.create(DotName.createSimple(XmlSchema.class.getName()),
                    null, annotationValues));
            // TODO find package annotation with yandex (AnnotationTarget.Kind.package do not exists...
            // then forward value and type of XmlJavaTypeAdapter
            //            PackageInfoCreator.addAnnotation(AnnotationInstance.create(DotName.createSimple(XmlJavaTypeAdapters.class.getName()),
            //                    null, annotationValues));
            //            PackageInfoCreator.addAnnotation(AnnotationInstance.create(DotName.createSimple(XmlJavaTypeAdapter.class.getName()),
            //                    null, annotationValues));

            //TODO get SOAPBINDING_ANNOTATION to get iRPC
            List<MethodDescriptor> setters = new ArrayList<>();
            List<MethodDescriptor> getters = new ArrayList<>();
            for (MethodInfo mi : wsClassInfo.methods()) {
                for (Type exceptionType : mi.exceptions()) {
                    String exceptionName = exceptionType.name().withoutPackagePrefix() + "_Exception";
                    if (exceptionType.annotation(WEBFAULT_ANNOTATION) != null) {
                        exceptionName = exceptionType.annotation(WEBFAULT_ANNOTATION).value("name").asString();

                    }
                    createException(classOutput, exceptionName, exceptionType.name());
                }
                String className = StringUtils.capitalize(mi.name());
                String operationName = mi.name();
                AnnotationInstance webMethodAnnotation = mi.annotation(WEBMETHOD_ANNOTATION);
                if (webMethodAnnotation != null) {
                    AnnotationValue nameVal = webMethodAnnotation.value("operationName");
                    if (nameVal != null) {
                        operationName = nameVal.asString();
                    }
                }

                AnnotationInstance webResultAnnotation = mi.annotation(WEBRESULT_ANNOTATION);
                String resultName = "return";
                if (webResultAnnotation != null) {
                    AnnotationValue resultNameVal = webResultAnnotation.value("name");
                    if (resultNameVal != null) {
                        resultName = resultNameVal.asString();
                    }
                }
                List<WrapperParameter> wrapperParams = new ArrayList<WrapperParameter>();
                for (int i = 0; i < mi.parameters().size(); i++) {
                    Type paramType = mi.parameters().get(i);
                    String paramName = mi.parameterName(i);
                    List<AnnotationInstance> paramAnnotations = new ArrayList<>();
                    for (AnnotationInstance methodAnnotation : mi.annotations()) {
                        if (methodAnnotation.target().kind() != AnnotationTarget.Kind.METHOD_PARAMETER)
                            continue;
                        MethodParameterInfo paramInfo = methodAnnotation.target().asMethodParameter();
                        if (paramInfo != null && paramName.equals(paramInfo.name())) {
                            paramAnnotations.add(methodAnnotation);
                        }

                    }

                    wrapperParams.add(new WrapperParameter(paramType, paramAnnotations, paramName));
                }
                // todo get REQUEST_WRAPPER_ANNOTATION to avoid creation of wrapper but create helper based on it
                MethodDescriptor requestCtor = createWrapper(true, operationName, namespace, resultName,
                        mi.returnType().toString(), wrapperParams,
                        classOutput, pkg, className, getters, setters);
                createWrapperHelper(classOutput, pkg, className, requestCtor, getters, setters);
                createWrapperFactory(classOutput, pkg, className, requestCtor);
                getters.clear();
                setters.clear();
                // todo get RESPONSE_WRAPPER_ANNOTATION to avoid creation of wrapper but create helper based on it

                MethodDescriptor responseCtor = createWrapper(false, operationName, namespace, resultName,
                        mi.returnType().toString(), wrapperParams,
                        classOutput, pkg, className, getters, setters);
                createWrapperHelper(classOutput, pkg, className + RESPONSE_CLASS_POSTFIX, responseCtor, getters, setters);
                createWrapperFactory(classOutput, pkg, className + RESPONSE_CLASS_POSTFIX, responseCtor);
                getters.clear();
                setters.clear();
                reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, pkg + "." + className));
                reflectiveClass
                        .produce(new ReflectiveClassBuildItem(true, true, pkg + "." + className + RESPONSE_CLASS_POSTFIX));
                reflectiveClass
                        .produce(new ReflectiveClassBuildItem(true, true, pkg + "." + className + WRAPPER_HELPER_POSTFIX));
                reflectiveClass.produce(new ReflectiveClassBuildItem(true, true,
                        pkg + "." + className + RESPONSE_CLASS_POSTFIX + WRAPPER_HELPER_POSTFIX));
                reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, pkg + ".ObjectFactory"));
                reflectiveClass
                        .produce(new ReflectiveClassBuildItem(true, true, pkg + "." + className + WRAPPER_FACTORY_POSTFIX));
                reflectiveClass.produce(
                        new ReflectiveClassBuildItem(true, true,
                                pkg + "." + className + RESPONSE_CLASS_POSTFIX + WRAPPER_FACTORY_POSTFIX));

            }
            //MethodDescriptor requestCtor = createWrapper("parameters", namespace,mi.typeParameters(), classOutput, pkg, pkg+"Parameters", getters, setters);
            //createWrapperHelper(classOutput, pkg, className, requestCtor, getters, setters);
            //getters.clear();
            //setters.clear();
        }

        feature.produce(new FeatureBuildItem(FeatureBuildItem.CXF));

        //if JAX-WS is installed at the root location we use a filter, otherwise we use a Servlet and take over the whole mapped path
        if (cxfConfig.path.equals("/") || cxfConfig.path.isEmpty()) {
            filters.produce(FilterBuildItem.builder(JAX_WS_FILTER_NAME, CXFQuarkusServlet.class.getName()).setLoadOnStartup(1)
                    .addFilterServletNameMapping("default", DispatcherType.REQUEST).setAsyncSupported(true)
                    .build());
        } else {
            String mappingPath = getMappingPath(cxfConfig.path);
            servlet.produce(ServletBuildItem.builder(JAX_WS_SERVLET_NAME, CXFQuarkusServlet.class.getName())
                    .setLoadOnStartup(0).addMapping(mappingPath).setAsyncSupported(true).build());
        }
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, CXFQuarkusServlet.class.getName()));
        //TODO servletInitParameters
        /*
         * AbstractHTTPServlet.STATIC_RESOURCES_PARAMETER
         * AbstractHTTPServlet.STATIC_WELCOME_FILE_PARAMETER
         * AbstractHTTPServlet.STATIC_CACHE_CONTROL
         * AbstractHTTPServlet.REDIRECTS_PARAMETER
         * AbstractHTTPServlet.REDIRECT_SERVLET_NAME_PARAMETER
         * AbstractHTTPServlet.REDIRECT_SERVLET_PATH_PARAMETER
         * AbstractHTTPServlet.REDIRECT_ATTRIBUTES_PARAMETER
         * AbstractHTTPServlet.REDIRECT_QUERY_CHECK_PARAMETER
         * AbstractHTTPServlet.REDIRECT_WITH_INCLUDE_PARAMETER
         * AbstractHTTPServlet.USE_X_FORWARDED_HEADERS_PARAMETER
         * CXFNonSpringJaxrsServlet.USER_MODEL_PARAM;
         * CXFNonSpringJaxrsServlet.SERVICE_ADDRESS_PARAM;
         * CXFNonSpringJaxrsServlet.IGNORE_APP_PATH_PARAM;
         * CXFNonSpringJaxrsServlet.SERVICE_CLASSES_PARAM;
         * CXFNonSpringJaxrsServlet.PROVIDERS_PARAM;
         * CXFNonSpringJaxrsServlet.FEATURES_PARAM;
         * CXFNonSpringJaxrsServlet.OUT_INTERCEPTORS_PARAM;
         * CXFNonSpringJaxrsServlet.OUT_FAULT_INTERCEPTORS_PARAM;
         * CXFNonSpringJaxrsServlet.IN_INTERCEPTORS_PARAM;
         * CXFNonSpringJaxrsServlet.INVOKER_PARAM;
         * CXFNonSpringJaxrsServlet.SERVICE_SCOPE_PARAM;
         * CXFNonSpringJaxrsServlet.EXTENSIONS_PARAM;
         * CXFNonSpringJaxrsServlet.LANGUAGES_PARAM;
         * CXFNonSpringJaxrsServlet.PROPERTIES_PARAM;
         * CXFNonSpringJaxrsServlet.SCHEMAS_PARAM;
         * CXFNonSpringJaxrsServlet.DOC_LOCATION_PARAM;
         * CXFNonSpringJaxrsServlet.STATIC_SUB_RESOLUTION_PARAM;
         */

        for (Entry<String, CxfEndpointConfig> webServicesByPath : cxfConfig.endpoints.entrySet()) {

            CxfEndpointConfig cxfEndPointConfig = webServicesByPath.getValue();
            DotName webServiceImplementor = DotName.createSimple(cxfEndPointConfig.implementor);
            ClassInfo wsclass = index.getClassByName(webServiceImplementor);
            String sei = null;
            if (wsclass != null) {
                for (Type wsInterfaceType : wsclass.interfaceTypes()) {
                    //TODO annotation is not seen do not know why so comment it for moment
                    //if (wsInterfaceType.hasAnnotation(WEBSERVICE_ANNOTATION)) {
                    sei = wsInterfaceType.name().toString();
                    //}
                }
            }
            String wsdlPath = null;
            if (cxfEndPointConfig.wsdlPath.isPresent()) {
                wsdlPath = cxfEndPointConfig.wsdlPath.get();
            }
            CXFServletInfoBuildItem cxfServletInfo = new CXFServletInfoBuildItem(webServicesByPath.getKey(),
                    webServicesByPath.getValue().implementor, sei, wsdlPath);
            for (AnnotationInstance annotation : wsclass.classAnnotations()) {
                switch (annotation.name().toString()) {
                    case "org.apache.cxf.feature.Features":
                        HashSet<String> features = new HashSet<>(
                                Arrays.asList(annotation.value("features").asStringArray()));
                        cxfServletInfo.getFeatures().addAll(features);
                        unremovableBeans.produce(new UnremovableBeanBuildItem(
                                new UnremovableBeanBuildItem.BeanClassNamesExclusion(features)));
                        reflectiveClass
                                .produce(
                                        new ReflectiveClassBuildItem(true, true, annotation.value("features").asStringArray()));
                        break;
                    case "org.apache.cxf.interceptor.InInterceptors":
                        HashSet<String> inInterceptors = new HashSet<>(
                                Arrays.asList(annotation.value(ANNOTATION_VALUE_INTERCEPTORS).asStringArray()));
                        cxfServletInfo.getInInterceptors().addAll(inInterceptors);
                        unremovableBeans.produce(new UnremovableBeanBuildItem(
                                new UnremovableBeanBuildItem.BeanClassNamesExclusion(inInterceptors)));
                        reflectiveClass
                                .produce(new ReflectiveClassBuildItem(true, true,
                                        annotation.value(ANNOTATION_VALUE_INTERCEPTORS).asStringArray()));
                        break;
                    case "org.apache.cxf.interceptor.OutInterceptors":
                        HashSet<String> outInterceptors = new HashSet<>(
                                Arrays.asList(annotation.value(ANNOTATION_VALUE_INTERCEPTORS).asStringArray()));
                        cxfServletInfo.getOutInterceptors().addAll(outInterceptors);
                        unremovableBeans.produce(new UnremovableBeanBuildItem(
                                new UnremovableBeanBuildItem.BeanClassNamesExclusion(outInterceptors)));
                        reflectiveClass
                                .produce(new ReflectiveClassBuildItem(true, true,
                                        annotation.value(ANNOTATION_VALUE_INTERCEPTORS).asStringArray()));
                        break;
                    case "org.apache.cxf.interceptor.OutFaultInterceptors":
                        HashSet<String> outFaultInterceptors = new HashSet<>(
                                Arrays.asList(annotation.value(ANNOTATION_VALUE_INTERCEPTORS).asStringArray()));
                        cxfServletInfo.getOutFaultInterceptors().addAll(outFaultInterceptors);
                        unremovableBeans.produce(new UnremovableBeanBuildItem(
                                new UnremovableBeanBuildItem.BeanClassNamesExclusion(outFaultInterceptors)));
                        reflectiveClass
                                .produce(new ReflectiveClassBuildItem(true, true,
                                        annotation.value(ANNOTATION_VALUE_INTERCEPTORS).asStringArray()));
                        break;
                    case "org.apache.cxf.interceptor.InFaultInterceptors":
                        HashSet<String> inFaultInterceptors = new HashSet<>(
                                Arrays.asList(annotation.value(ANNOTATION_VALUE_INTERCEPTORS).asStringArray()));
                        cxfServletInfo.getInFaultInterceptors().addAll(inFaultInterceptors);
                        unremovableBeans.produce(new UnremovableBeanBuildItem(
                                new UnremovableBeanBuildItem.BeanClassNamesExclusion(inFaultInterceptors)));
                        reflectiveClass
                                .produce(new ReflectiveClassBuildItem(true, true,
                                        annotation.value(ANNOTATION_VALUE_INTERCEPTORS).asStringArray()));
                        break;
                    default:
                        break;
                }
            }
            cxfServletInfos.produce(cxfServletInfo);
        }

        for (ClassInfo subclass : index.getAllKnownSubclasses(ABSTRACT_FEATURE)) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, subclass.name().toString()));
        }
        for (ClassInfo subclass : index.getAllKnownSubclasses(ABSTRACT_INTERCEPTOR)) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, subclass.name().toString()));
        }
        for (ClassInfo subclass : index.getAllKnownImplementors(DATABINDING)) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, subclass.name().toString()));
        }

    }

    @BuildStep
    List<RuntimeInitializedClassBuildItem> runtimeInitializedClasses() {
        return Arrays.asList(
                new RuntimeInitializedClassBuildItem("io.netty.buffer.PooledByteBufAllocator"),
                new RuntimeInitializedClassBuildItem("io.netty.buffer.UnpooledHeapByteBuf"),
                new RuntimeInitializedClassBuildItem("io.netty.buffer.UnpooledUnsafeHeapByteBuf"),
                new RuntimeInitializedClassBuildItem(
                        "io.netty.buffer.UnpooledByteBufAllocator$InstrumentedUnpooledUnsafeHeapByteBuf"),
                new RuntimeInitializedClassBuildItem("io.netty.buffer.AbstractReferenceCountedByteBuf"),
                new RuntimeInitializedClassBuildItem("org.apache.cxf.staxutils.validation.W3CMultiSchemaFactory"));
    }

    @BuildStep
    void httpProxies(BuildProducer<NativeImageProxyDefinitionBuildItem> proxies) {
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.common.jaxb.JAXBContextProxy"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.common.jaxb.JAXBBeanInfo"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.common.jaxb.JAXBUtils$BridgeWrapper"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.common.jaxb.JAXBUtils$SchemaCompiler"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.common.util.ASMHelper$ClassWriter"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("javax.wsdl.extensions.soap.SOAPOperation"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("javax.wsdl.extensions.soap.SOAPBody"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("javax.wsdl.extensions.soap.SOAPHeader"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("javax.wsdl.extensions.soap.SOAPAddress"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("javax.wsdl.extensions.soap.SOAPBinding"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("javax.wsdl.extensions.soap.SOAPFault"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.binding.soap.wsdl.extensions.SoapBinding"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.binding.soap.wsdl.extensions.SoapAddress"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.binding.soap.wsdl.extensions.SoapHeader"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.binding.soap.wsdl.extensions.SoapBody"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.binding.soap.wsdl.extensions.SoapFault"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("org.apache.cxf.binding.soap.wsdl.extensions.SoapOperation"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.marshaller.CharacterEscapeHandler"));

    }

    @BuildStep
    public void registerReflectionItems(BuildProducer<ReflectiveClassBuildItem> reflectiveItems) {
        //TODO load all bus-extensions.txt file and parse it to generate the reflective class.
        reflectiveItems.produce(new ReflectiveClassBuildItem(true, true,
                "io.quarkus.cxf.runtime.QuarkusJAXBBeanInfo",
                "org.apache.cxf.common.jaxb.JAXBBeanInfo",
                "javax.xml.bind.JAXBContext",
                "com.sun.xml.bind.v2.runtime.LeafBeanInfoImpl",
                "com.sun.xml.bind.v2.runtime.ArrayBeanInfoImpl",
                "com.sun.xml.bind.v2.runtime.ValueListBeanInfoImpl",
                "com.sun.xml.bind.v2.runtime.AnyTypeBeanInfo",
                "com.sun.xml.bind.v2.runtime.JaxBeanInfo",
                "com.sun.xml.bind.v2.runtime.ClassBeanInfoImpl",
                "com.sun.xml.bind.v2.runtime.CompositeStructureBeanInfo",
                "com.sun.xml.bind.v2.runtime.ElementBeanInfoImpl",
                "com.sun.xml.bind.v2.runtime.MarshallerImpl",
                "com.sun.xml.bind.v2.runtime.BridgeContextImpl",
                "com.sun.xml.bind.v2.runtime.JAXBContextImpl"));
        reflectiveItems.produce(new ReflectiveClassBuildItem(false, false,
                //manually added
                "org.apache.cxf.wsdl.interceptors.BareInInterceptor",
                "com.sun.msv.reader.GrammarReaderController",
                "org.apache.cxf.binding.soap.interceptor.RPCInInterceptor",
                "org.apache.cxf.wsdl.interceptors.DocLiteralInInterceptor",
                "StaxSchemaValidationInInterceptor",
                "org.apache.cxf.binding.soap.interceptor.SoapHeaderInterceptor",
                "org.w3c.dom.Node",
                "org.apache.cxf.binding.soap.model.SoapHeaderInfo",
                "javax.xml.stream.XMLStreamReader",
                "java.util.List",
                "org.apache.cxf.service.model.BindingOperationInfo",
                "org.apache.cxf.binding.soap.interceptor.CheckFaultInterceptor",
                "org.apache.cxf.interceptor.ClientFaultConverter",
                "org.apache.cxf.binding.soap.interceptor.EndpointSelectionInterceptor",
                "java.io.InputStream",
                "org.apache.cxf.service.model.MessageInfo",
                "org.apache.cxf.binding.soap.interceptor.MustUnderstandInterceptor",
                "org.apache.cxf.interceptor.OneWayProcessorInterceptor",
                "java.io.OutputStream",
                "org.apache.cxf.binding.soap.interceptor.ReadHeadersInterceptor",
                "org.apache.cxf.binding.soap.interceptor.RPCOutInterceptor",
                "org.apache.cxf.binding.soap.interceptor.Soap11FaultInInterceptor",
                "org.apache.cxf.binding.soap.interceptor.Soap11FaultOutInterceptor",
                "org.apache.cxf.binding.soap.interceptor.Soap12FaultInInterceptor",
                "org.apache.cxf.binding.soap.interceptor.Soap12FaultOutInterceptor",
                "org.apache.cxf.binding.soap.interceptor.SoapActionInInterceptor",
                "org.apache.cxf.binding.soap.wsdl.extensions.SoapBody",
                "javax.wsdl.extensions.soap.SOAPBody",
                "org.apache.cxf.binding.soap.model.SoapOperationInfo",
                "org.apache.cxf.binding.soap.interceptor.SoapOutInterceptor$SoapOutEndingInterceptor",
                "org.apache.cxf.binding.soap.interceptor.SoapOutInterceptor",
                "org.apache.cxf.binding.soap.interceptor.StartBodyInterceptor",
                "java.net.URI",
                "java.lang.Exception",
                "org.apache.cxf.staxutils.W3CDOMStreamWriter",
                "javax.xml.stream.XMLStreamReader",
                "javax.xml.stream.XMLStreamWriter",
                "org.apache.cxf.common.jaxb.SchemaCollectionContextProxy",
                "com.ctc.wstx.sax.WstxSAXParserFactory",
                "com.ibm.wsdl.BindingFaultImpl",
                "com.ibm.wsdl.BindingImpl",
                "com.ibm.wsdl.BindingInputImpl",
                "com.ibm.wsdl.BindingOperationImpl",
                "com.ibm.wsdl.BindingOutputImpl",
                "com.ibm.wsdl.extensions.soap.SOAPAddressImpl",
                "com.ibm.wsdl.extensions.soap.SOAPBindingImpl",
                "com.ibm.wsdl.extensions.soap.SOAPBodyImpl",
                "com.ibm.wsdl.extensions.soap.SOAPFaultImpl",
                "com.ibm.wsdl.extensions.soap.SOAPHeaderImpl",
                "com.ibm.wsdl.extensions.soap.SOAPOperationImpl",
                "com.ibm.wsdl.factory.WSDLFactoryImpl",
                "com.ibm.wsdl.FaultImpl",
                "com.ibm.wsdl.InputImpl",
                "com.ibm.wsdl.MessageImpl",
                "com.ibm.wsdl.OperationImpl",
                "com.ibm.wsdl.OutputImpl",
                "com.ibm.wsdl.PartImpl",
                "com.ibm.wsdl.PortImpl",
                "com.ibm.wsdl.PortTypeImpl",
                "com.ibm.wsdl.ServiceImpl",
                "com.ibm.wsdl.TypesImpl",
                "com.oracle.xmlns.webservices.jaxws_databinding.ObjectFactory",
                "com.sun.codemodel.internal.writer.FileCodeWriter",
                "com.sun.codemodel.writer.FileCodeWriter",
                "com.sun.org.apache.xerces.internal.utils.XMLSecurityManager",
                "com.sun.org.apache.xerces.internal.utils.XMLSecurityPropertyManager",
                "com.sun.tools.internal.xjc.api.XJC",
                "com.sun.tools.xjc.api.XJC",
                "com.sun.xml.bind.api.JAXBRIContext",
                "com.sun.xml.bind.api.TypeReference",
                "com.sun.xml.bind.DatatypeConverterImpl",
                "com.sun.xml.bind.marshaller.CharacterEscapeHandler",
                "com.sun.xml.bind.marshaller.MinimumEscapeHandler",
                "com.sun.xml.bind.v2.ContextFactory",
                "com.sun.xml.internal.bind.api.JAXBRIContext",
                "com.sun.xml.internal.bind.api.TypeReference",
                "com.sun.xml.internal.bind.DatatypeConverterImpl",
                "com.sun.xml.internal.bind.marshaller.MinimumEscapeHandler",
                "com.sun.xml.internal.bind.v2.ContextFactory",
                "com.sun.xml.ws.runtime.config.ObjectFactory",
                "ibm.wsdl.DefinitionImpl",
                "io.quarkus.cxf.runtime.AddressTypeExtensibility",
                "io.quarkus.cxf.runtime.CXFException",
                "io.quarkus.cxf.runtime.HTTPClientPolicyExtensibility",
                "io.quarkus.cxf.runtime.HTTPServerPolicyExtensibility",
                "io.quarkus.cxf.runtime.XMLBindingMessageFormatExtensibility",
                "io.quarkus.cxf.runtime.XMLFormatBindingExtensibility",
                "io.swagger.jaxrs.DefaultParameterExtension",
                "io.undertow.server.HttpServerExchange",
                "io.undertow.UndertowOptions",
                "java.lang.invoke.MethodHandles",
                "java.rmi.RemoteException",
                "java.rmi.ServerException",
                "java.security.acl.Group",
                "javax.enterprise.inject.spi.CDI",
                "javax.jws.Oneway",
                "javax.jws.WebMethod",
                "javax.jws.WebParam",
                "javax.jws.WebResult",
                "javax.jws.WebService",
                "javax.security.auth.login.Configuration",
                "javax.servlet.WriteListener",
                "javax.wsdl.Binding",
                "javax.wsdl.Binding",
                "javax.wsdl.BindingFault",
                "javax.wsdl.BindingFault",
                "javax.wsdl.BindingInput",
                "javax.wsdl.BindingOperation",
                "javax.wsdl.BindingOperation",
                "javax.wsdl.BindingOutput",
                "javax.wsdl.Definition",
                "javax.wsdl.Fault",
                "javax.wsdl.Import",
                "javax.wsdl.Input",
                "javax.wsdl.Message",
                "javax.wsdl.Operation",
                "javax.wsdl.Output",
                "javax.wsdl.Part",
                "javax.wsdl.Port",
                "javax.wsdl.Port",
                "javax.wsdl.PortType",
                "javax.wsdl.Service",
                "javax.wsdl.Types",
                "javax.xml.bind.annotation.XmlSeeAlso",
                "javax.xml.bind.JAXBElement",
                "javax.xml.namespace.QName",
                "javax.xml.soap.SOAPMessage",
                "javax.xml.transform.stax.StAXSource",
                "javax.xml.ws.Action",
                "javax.xml.ws.BindingType",
                "javax.xml.ws.Provider",
                "javax.xml.ws.RespectBinding",
                "javax.xml.ws.Service",
                "javax.xml.ws.ServiceMode",
                "javax.xml.ws.soap.Addressing",
                "javax.xml.ws.soap.MTOM",
                "javax.xml.ws.soap.SOAPBinding",
                "javax.xml.ws.WebFault",
                "javax.xml.ws.WebServiceProvider",
                "net.sf.cglib.proxy.Enhancer",
                "net.sf.cglib.proxy.MethodInterceptor",
                "net.sf.cglib.proxy.MethodProxy",
                "net.sf.ehcache.CacheManager",
                "org.apache.commons.logging.LogFactory",
                "org.apache.cxf.binding.soap.SoapBinding",
                "org.apache.cxf.binding.soap.SoapFault",
                "org.apache.cxf.binding.soap.SoapHeader",
                "org.apache.cxf.binding.soap.SoapMessage",
                "org.apache.cxf.binding.xml.XMLFault",
                "org.apache.cxf.bindings.xformat.ObjectFactory",
                "org.apache.cxf.bindings.xformat.XMLBindingMessageFormat",
                "org.apache.cxf.bindings.xformat.XMLFormatBinding",
                "org.apache.cxf.bus.CXFBusFactory",
                "org.apache.cxf.bus.managers.BindingFactoryManagerImpl",
                "org.apache.cxf.common.jaxb.NamespaceMapper",
                "org.apache.cxf.common.jaxb.SchemaCollectionContextProxy",
                "org.apache.cxf.interceptor.Fault",
                "org.apache.cxf.jaxb.DatatypeFactory",
                "org.apache.cxf.jaxb.JAXBDataBinding",
                "org.apache.cxf.jaxrs.utils.JAXRSUtils",
                "org.apache.cxf.jaxws.binding.soap.SOAPBindingImpl",
                "org.apache.cxf.metrics.codahale.CodahaleMetricsProvider",
                "org.apache.cxf.message.Exchange",
                "org.apache.cxf.message.ExchangeImpl",
                "org.apache.cxf.message.StringMapImpl",
                "org.apache.cxf.message.StringMap",
                "org.apache.cxf.tools.fortest.cxf523.Database",
                "org.apache.cxf.tools.fortest.cxf523.DBServiceFault",
                "org.apache.cxf.tools.fortest.withannotation.doc.HelloWrapped",
                "org.apache.cxf.transports.http.configuration.HTTPClientPolicy",
                "org.apache.cxf.transports.http.configuration.HTTPServerPolicy",
                "org.apache.cxf.transports.http.configuration.ObjectFactory",
                "org.apache.cxf.ws.addressing.wsdl.AttributedQNameType",
                "org.apache.cxf.ws.addressing.wsdl.AttributedQNameType",
                "org.apache.cxf.ws.addressing.wsdl.ObjectFactory",
                "org.apache.cxf.ws.addressing.wsdl.ServiceNameType",
                "org.apache.cxf.ws.addressing.wsdl.ServiceNameType",
                "org.apache.cxf.wsdl.http.AddressType",
                "org.apache.cxf.wsdl.http.ObjectFactory",
                "org.apache.hello_world.Greeter",
                "org.apache.hello_world_soap_http.types.StringStruct",
                "org.apache.karaf.jaas.boot.principal.Group",
                "org.apache.xerces.impl.Version",
                "org.apache.yoko.orb.OB.BootManager",
                "org.apache.yoko.orb.OB.BootManagerHelper",
                "org.codehaus.stax2.XMLStreamReader2",
                "org.eclipse.jetty.jaas.spi.PropertyFileLoginModule",
                "org.eclipse.jetty.jmx.MBeanContainer",
                "org.eclipse.jetty.plus.jaas.spi.PropertyFileLoginModule",
                "org.hsqldb.jdbcDriver",
                "org.jdom.Document",
                "org.jdom.Element",
                "org.osgi.framework.Bundle",
                "org.osgi.framework.BundleContext",
                "org.osgi.framework.FrameworkUtil",
                "org.slf4j.impl.StaticLoggerBinder",
                "org.slf4j.LoggerFactory",
                "org.springframework.aop.framework.Advised",
                "org.springframework.aop.support.AopUtils",
                "org.springframework.core.io.support.PathMatchingResourcePatternResolver",
                "org.springframework.core.type.classreading.CachingMetadataReaderFactory",
                "org.springframework.osgi.io.OsgiBundleResourcePatternResolver",
                "org.springframework.osgi.util.BundleDelegatingClassLoader",
                // org.apache.cxf.extension.BusExtension interface without duplicate
                "org.apache.cxf.configuration.spring.ConfigurerImpl",
                // policy bus-extensions.txt
                "org.apache.cxf.ws.policy.PolicyEngineImpl",
                "org.apache.cxf.ws.policy.PolicyEngine",
                "org.apache.cxf.policy.PolicyDataEngine",
                "org.apache.cxf.ws.policy.PolicyDataEngineImpl",
                "org.apache.cxf.ws.policy.AssertionBuilderRegistry",
                "org.apache.cxf.ws.policy.AssertionBuilderRegistryImpl",
                "org.apache.cxf.ws.policy.PolicyInterceptorProviderRegistry",
                "org.apache.cxf.ws.policy.PolicyInterceptorProviderRegistryImpl",
                "org.apache.cxf.ws.policy.PolicyBuilder",
                "org.apache.cxf.ws.policy.PolicyBuilderImpl",
                "org.apache.cxf.ws.policy.PolicyAnnotationListener",
                "org.apache.cxf.ws.policy.attachment.ServiceModelPolicyProvider",
                "org.apache.cxf.ws.policy.attachment.external.DomainExpressionBuilderRegistry",
                "org.apache.cxf.ws.policy.attachment.external.EndpointReferenceDomainExpressionBuilder",
                "org.apache.cxf.ws.policy.attachment.external.URIDomainExpressionBuilder",
                "org.apache.cxf.ws.policy.attachment.wsdl11.Wsdl11AttachmentPolicyProvider",
                "org.apache.cxf.ws.policy.mtom.MTOMAssertionBuilder",
                "org.apache.cxf.ws.policy.mtom.MTOMPolicyInterceptorProvider",
                //transport undertow bus-extensions.txt
                "org.apache.cxf.transport.http_undertow.UndertowDestinationFactory",
                "org.apache.cxf.transport.http_undertow.UndertowHTTPServerEngineFactory",
                //transport http bus-extensions.txt
                "org.apache.cxf.transport.http.HTTPTransportFactory",
                "org.apache.cxf.transport.http.HTTPWSDLExtensionLoader",
                "org.apache.cxf.transport.http.policy.HTTPClientAssertionBuilder",
                "org.apache.cxf.transport.http.policy.HTTPServerAssertionBuilder",
                "org.apache.cxf.transport.http.policy.NoOpPolicyInterceptorProvider",
                //jaxws bus-extensions.txt
                "org.apache.cxf.jaxws.context.WebServiceContextResourceResolver",
                //management bus-extensions.txt
                "org.apache.cxf.management.InstrumentationManager",
                "org.apache.cxf.management.jmx.InstrumentationManagerImpl",
                //rt reliable message bus-extensions.txt
                "org.apache.cxf.ws.rm.RMManager",
                //mex bus-extensions.txt
                "org.apache.cxf.ws.mex.MEXServerListener",
                //sse bus-extensions.txt
                "org.apache.cxf.transport.sse.SseProvidersExtension",
                //transport websocket (over undertow) bus-extensions.txt
                "org.apache.cxf.transport.websocket.WebSocketTransportFactory",
                //rt wsdl bus-extensions.txt
                "org.apache.cxf.wsdl.WSDLManager",
                "org.apache.cxf.wsdl11.WSDLManagerImpl",
                //xml binding bus-extensions.txt
                "org.apache.cxf.binding.xml.XMLBindingFactory",
                "org.apache.cxf.binding.xml.wsdl11.XMLWSDLExtensionLoader",
                //rt soap binding bus-extensions.txt
                "org.apache.cxf.binding.soap.SoapTransportFactory",
                "org.apache.cxf.binding.soap.SoapBindingFactory",
                // core bus-extensions.txt
                "org.apache.cxf.bus.managers.PhaseManagerImpl",
                "org.apache.cxf.phase.PhaseManager",
                "org.apache.cxf.bus.managers.WorkQueueManagerImpl",
                "org.apache.cxf.workqueue.WorkQueueManager",
                "org.apache.cxf.bus.managers.CXFBusLifeCycleManager",
                "org.apache.cxf.buslifecycle.BusLifeCycleManager",
                "org.apache.cxf.bus.managers.ServerRegistryImpl",
                "org.apache.cxf.endpoint.ServerRegistry",
                "org.apache.cxf.bus.managers.EndpointResolverRegistryImpl",
                "org.apache.cxf.endpoint.EndpointResolverRegistry",
                "org.apache.cxf.bus.managers.HeaderManagerImpl",
                "org.apache.cxf.headers.HeaderManager",
                "org.apache.cxf.service.factory.FactoryBeanListenerManager",
                "org.apache.cxf.bus.managers.ServerLifeCycleManagerImpl",
                "org.apache.cxf.endpoint.ServerLifeCycleManager",
                "org.apache.cxf.bus.managers.ClientLifeCycleManagerImpl",
                "org.apache.cxf.endpoint.ClientLifeCycleManager",
                "org.apache.cxf.bus.resource.ResourceManagerImpl",
                "org.apache.cxf.resource.ResourceManager",
                "org.apache.cxf.catalog.OASISCatalogManager",
                "org.apache.cxf.catalog.OASISCatalogManager"));
    }

    @BuildStep
    NativeImageResourceBuildItem nativeImageResourceBuildItem() {
        return new NativeImageResourceBuildItem("com/sun/xml/fastinfoset/resources/ResourceBundle.properties",
                "META-INF/cxf/bus-extensions.txt",
                "META-INF/cxf/cxf.xml",
                "META-INF/cxf/org.apache.cxf.bus.factory",
                "META-INF/services/org.apache.cxf.bus.factory",
                "META-INF/blueprint.handlers",
                "META-INF/spring.handlers",
                "META-INF/spring.schemas",
                "OSGI-INF/metatype/workqueue.xml",
                "schemas/core.xsd",
                "schemas/blueprint/core.xsd",
                "schemas/wsdl/XMLSchema.xsd",
                "schemas/wsdl/addressing.xjb",
                "schemas/wsdl/addressing.xsd",
                "schemas/wsdl/addressing200403.xjb",
                "schemas/wsdl/addressing200403.xsd",
                "schemas/wsdl/http.xjb",
                "schemas/wsdl/http.xsd",
                "schemas/wsdl/mime-binding.xsd",
                "schemas/wsdl/soap-binding.xsd",
                "schemas/wsdl/soap-encoding.xsd",
                "schemas/wsdl/soap12-binding.xsd",
                "schemas/wsdl/swaref.xsd",
                "schemas/wsdl/ws-addr-wsdl.xjb",
                "schemas/wsdl/ws-addr-wsdl.xsd",
                "schemas/wsdl/ws-addr.xsd",
                "schemas/wsdl/wsdl.xjb",
                "schemas/wsdl/wsdl.xsd",
                "schemas/wsdl/wsrm.xsd",
                "schemas/wsdl/xmime.xsd",
                "schemas/wsdl/xml.xsd",
                "schemas/configuratio/cxf-beans.xsd",
                "schemas/configuration/extension.xsd",
                "schemas/configuration/parameterized-types.xsd",
                "schemas/configuration/security.xjb",
                "schemas/configuration/security.xsd");
    }

    private String getMappingPath(String path) {
        String mappingPath;
        if (path.endsWith("/")) {
            mappingPath = path + "*";
        } else {
            mappingPath = path + "/*";
        }
        return mappingPath;
    }

    @BuildStep
    public void createBeans(
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            BuildProducer<ReflectiveClassBuildItem> reflectiveItems) {
        for (Entry<String, CxfEndpointConfig> webServicesByPath : cxfConfig.endpoints.entrySet()) {
            String webServiceName = webServicesByPath.getValue().implementor;
            String producerClassName = webServiceName + "Producer";
            ClassOutput classOutput = new GeneratedBeanGizmoAdaptor(generatedBeans);

            createProducer(producerClassName, classOutput, webServiceName);
            unremovableBeans.produce(new UnremovableBeanBuildItem(
                    new UnremovableBeanBuildItem.BeanClassNameExclusion(producerClassName)));
            reflectiveItems.produce(new ReflectiveClassBuildItem(true, true, producerClassName));

        }

    }

    private void createProducer(String producerClassName,
            ClassOutput classOutput,
            String webServiceName) {
        ClassCreator classCreator = ClassCreator.builder().classOutput(classOutput)
                .className(producerClassName)
                .build();
        classCreator.addAnnotation(ApplicationScoped.class);

        MethodCreator namedWebServiceMethodCreator = classCreator.getMethodCreator(
                "createWebService_" + HashUtil.sha1(webServiceName),
                webServiceName);
        namedWebServiceMethodCreator.addAnnotation(ApplicationScoped.class);
        namedWebServiceMethodCreator.addAnnotation(Unremovable.class);
        namedWebServiceMethodCreator.addAnnotation(Produces.class);
        namedWebServiceMethodCreator.addAnnotation(AnnotationInstance.create(DotNames.NAMED, null,
                new AnnotationValue[] { AnnotationValue.createStringValue("value", webServiceName) }));

        ResultHandle namedWebService = namedWebServiceMethodCreator
                .newInstance(MethodDescriptor.ofConstructor(webServiceName));

        namedWebServiceMethodCreator.returnValue(namedWebService);
        classCreator.close();
    }

}
