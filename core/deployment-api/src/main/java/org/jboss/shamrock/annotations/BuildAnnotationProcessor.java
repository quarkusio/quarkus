package org.jboss.shamrock.annotations;

import static javax.lang.model.util.ElementFilter.fieldsIn;
import static javax.lang.model.util.ElementFilter.methodsIn;
import static org.jboss.protean.gizmo.MethodDescriptor.ofConstructor;
import static org.jboss.protean.gizmo.MethodDescriptor.ofMethod;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Completion;
import javax.annotation.processing.RoundEnvironment;
import javax.inject.Inject;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import org.jboss.builder.BuildChainBuilder;
import org.jboss.builder.BuildContext;
import org.jboss.builder.BuildProvider;
import org.jboss.builder.BuildStepBuilder;
import org.jboss.builder.ProduceFlag;
import org.jboss.builder.item.BuildItem;
import org.jboss.builder.item.MultiBuildItem;
import org.jboss.builder.item.SimpleBuildItem;
import org.jboss.protean.gizmo.BranchResult;
import org.jboss.protean.gizmo.BytecodeCreator;
import org.jboss.protean.gizmo.CatchBlockCreator;
import org.jboss.protean.gizmo.ClassCreator;
import org.jboss.protean.gizmo.ClassOutput;
import org.jboss.protean.gizmo.FieldCreator;
import org.jboss.protean.gizmo.FieldDescriptor;
import org.jboss.protean.gizmo.FunctionCreator;
import org.jboss.protean.gizmo.MethodCreator;
import org.jboss.protean.gizmo.MethodDescriptor;
import org.jboss.protean.gizmo.ResultHandle;
import org.jboss.protean.gizmo.TryBlock;

public class BuildAnnotationProcessor extends AbstractProcessor {

    private static final String BUILD_PRODUCER = "org.jboss.shamrock.deployment.BuildProducerImpl";
    private static final String STATIC_RECORDER = "org.jboss.shamrock.deployment.builditem.StaticBytecodeRecorderBuildItem";
    private static final String MAIN_RECORDER = "org.jboss.shamrock.deployment.builditem.MainBytecodeRecorderBuildItem";

    private static final AtomicInteger classNameCounter = new AtomicInteger();
    private static final String TEMPLATE_ANNOTATION = "org.jboss.shamrock.runtime.Template";
    private static final String CONFIG_PROPERTY_ANNOTATION = "org.eclipse.microprofile.config.inject.ConfigProperty";
    private static final String CONFIGURED_TYPE_ANNOTATION = "org.jboss.shamrock.runtime.ConfiguredType";
    private static final String SHAMROCK_CONFIG = "org.jboss.shamrock.deployment.ShamrockConfig";

    @Override
    public Set<String> getSupportedOptions() {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> ret = new HashSet<>();
        ret.add(Inject.class.getName());
        ret.add(BuildStep.class.getName());
        ret.add(Record.class.getName());
        ret.add(CONFIGURED_TYPE_ANNOTATION);
        return ret;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver() && !annotations.isEmpty()) {
            try {
                doProcess(annotations, roundEnv);
            } catch (RuntimeException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
                throw e;
            }
        }
        return true;
    }

    @Override
    public Iterable<? extends Completion> getCompletions(Element element, AnnotationMirror annotation, ExecutableElement member, String userText) {
        return null;
    }


    public void doProcess(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        Properties configProperties = new Properties();
        Set<String> serviceNames = new HashSet<>();
        Set<TypeElement> processorElements = new HashSet<>();
        //Call jboss logging tools

        final Set<ExecutableElement> processorMethods = new HashSet<>();
        Set<TypeElement> processorClasses = new HashSet<>();
        Map<TypeElement, List<ExecutableElement>> methodMap = new HashMap<>();
        //create a set of classes, and map this to the build step methods
        for (TypeElement annotation : annotations) {
            if (annotation.getQualifiedName().toString().equals(BuildStep.class.getName())) {
                processorMethods.addAll(methodsIn(roundEnv.getElementsAnnotatedWith(annotation)));

                for (ExecutableElement i : processorMethods) {
                    TypeElement enclosingElement = (TypeElement) i.getEnclosingElement();
                    processorClasses.add(enclosingElement);
                    methodMap.computeIfAbsent(enclosingElement, (a) -> new ArrayList<>()).add(i);
                }
            }
        }

        //process each class individually, we only create a single instance of each even if it has multiple steps
        for (TypeElement processor : processorClasses) {
            List<InjectedBuildResource> fieldList = new ArrayList<>();
            List<VariableElement> configuredFields = new ArrayList<>();
            //resolve field injection
            for (VariableElement element : fieldsIn(processor.getEnclosedElements())) {
                try {
                    if (element.getAnnotation(Inject.class) != null) {
                        if (element.getModifiers().contains(Modifier.STATIC)) {
                            throw new RuntimeException("@Inject fields cannot be static");
                        }
                        if (element.getModifiers().contains(Modifier.FINAL)) {
                            throw new RuntimeException("@Inject fields cannot be final");
                        }
                        if (element.getModifiers().contains(Modifier.PRIVATE)) {
                            throw new RuntimeException("@Inject fields cannot be private");
                        }
                        InjectedBuildResource injectedBuildResource = createInjectionResource(element);
                        fieldList.add(injectedBuildResource);
                    } else {
                        if (isAnnotationPresent(element, CONFIG_PROPERTY_ANNOTATION)) {
                            configuredFields.add(element);
                        }
                    }
                } catch (RuntimeException e) {
                    throw new RuntimeException("Exception processing field " + element + " in type " + processor, e);
                }
            }


            //now lets generate some stuff
            //first we create a build provider, this registers the producers and consumers
            //we only create a single one for the the class, even if there are multiple steps
            String processorClassName = processingEnv.getElementUtils().getBinaryName(processor).toString();
            final String buildProviderName = processorClassName + "BuildProvider";
            serviceNames.add(buildProviderName);
            processorElements.add(processor);

            try (ClassCreator creator = new ClassCreator(new ProcessorClassOutput(processor), buildProviderName, null, Object.class.getName(), BuildProvider.class.getName())) {
                MethodCreator mc = creator.getMethodCreator("installInto", void.class, BuildChainBuilder.class);


                ResultHandle theInstance = mc.newInstance(ofConstructor(processorClassName));

                for (VariableElement i : configuredFields) {
                    injectConfigField(mc, theInstance, i, processorClassName, mc.load(""), configProperties, "");
                }


                for (ExecutableElement method : methodMap.get(processor)) {
                    try {
                        if (method.getModifiers().contains(Modifier.PRIVATE)) {
                            throw new RuntimeException("@BuildStep methods cannot be private: " + processorClassName + ":" + method);
                        }
                        List<InjectedBuildResource> methodInjection = new ArrayList<>();
                        List<String> methodParamTypes = new ArrayList<>();
                        boolean templatePresent = false;
                        boolean recorderContextPresent = false;
                        Record recordAnnotation = method.getAnnotation(Record.class);

                        //resolve method injection
                        for (VariableElement i : method.getParameters()) {
                            InjectedBuildResource injection = createInjectionResource(i);
                            if (injection.injectionType == InjectionType.TEMPLATE) {
                                templatePresent = true;
                            } else if (injection.injectionType == InjectionType.RECORDER_CONTEXT) {
                                recorderContextPresent = true;
                            }
                            methodInjection.add(injection);

                            DeclaredType type = (DeclaredType) i.asType();
                            String simpleType = processingEnv.getElementUtils().getBinaryName(((TypeElement) type.asElement())).toString();
                            methodParamTypes.add(simpleType);
                        }

                        //make sure that this is annotated with @Record if it is using templates
                        if (recordAnnotation == null && templatePresent) {
                            throw new RuntimeException("Cannot inject @Template classes into methods that are not annotated @Record: " + method);
                        } else if (recordAnnotation != null && !templatePresent) {
                            throw new RuntimeException("@Record method does not inject any template classes " + method);
                        } else if (recorderContextPresent && !templatePresent) {
                            throw new RuntimeException("Cannot inject bean factory into a non @Record method");
                        }
                        MethodReturnInfo returnInfo = processMethodReturnType(method);

                        final String buildStepName = registerBuildStep(fieldList, processorClassName, mc, returnInfo.producedReturnType, methodInjection, templatePresent, recordAnnotation, theInstance);


                        //now generate the actual invoker class that runs the build step
                        try (ClassCreator buildStepCreator = new ClassCreator(new ProcessorClassOutput(processor), buildStepName, null, Object.class.getName(), org.jboss.builder.BuildStep.class.getName())) {

                            //the constructor, just sets the instance field
                            MethodCreator ctor = buildStepCreator.getMethodCreator(ofConstructor(buildStepName, processorClassName));
                            ctor.invokeSpecialMethod(ofConstructor(Object.class), ctor.getThis());
                            FieldCreator ifield = buildStepCreator.getFieldCreator("instance", processorClassName);
                            ctor.writeInstanceField(ifield.getFieldDescriptor(), ctor.getThis(), ctor.getMethodParam(0));
                            ctor.returnValue(null);

                            //toString
                            MethodCreator toString = buildStepCreator.getMethodCreator("toString", String.class);
                            toString.returnValue(toString.load(processorClassName + "." + method.getSimpleName()));

                            MethodCreator buildStepMc = buildStepCreator.getMethodCreator("execute", void.class, BuildContext.class);
                            ResultHandle p = buildStepMc.readInstanceField(ifield.getFieldDescriptor(), buildStepMc.getThis());

                            //do the field injection
                            for (InjectedBuildResource field : fieldList) {
                                generateFieldInjection(processorClassName, buildStepMc, p, field);
                            }
                            TryBlock table = buildStepMc.tryBlock();
                            List<ResultHandle> args = new ArrayList<>();
                            ResultHandle bytecodeRecorder = null;
                            if (templatePresent) {
                                bytecodeRecorder = buildStepMc.newInstance(ofConstructor("org.jboss.shamrock.deployment.recording.BytecodeRecorderImpl", boolean.class), buildStepMc.load(recordAnnotation.value() == ExecutionTime.STATIC_INIT));
                            }

                            for (InjectedBuildResource i : methodInjection) {
                                ResultHandle val = generateMethodInjection(buildStepMc, bytecodeRecorder, i);
                                args.add(val);
                            }


                            ResultHandle handle = buildStepMc.invokeVirtualMethod(ofMethod(processorClassName, method.getSimpleName().toString(), returnInfo.rawReturnType, methodParamTypes.toArray(new String[0])), p, args.toArray(new ResultHandle[0]));
                            if (returnInfo.producedReturnType != null) {
                                BranchResult ifstm = buildStepMc.ifNull(handle);
                                if (returnInfo.list) {
                                    ifstm.falseBranch().invokeVirtualMethod(ofMethod(BuildContext.class, "produce", void.class, List.class), buildStepMc.getMethodParam(0), handle);
                                } else {
                                    ifstm.falseBranch().invokeVirtualMethod(ofMethod(BuildContext.class, "produce", void.class, BuildItem.class), buildStepMc.getMethodParam(0), handle);
                                }
                            }
                            if (bytecodeRecorder != null) {
                                ResultHandle buildItem;
                                if (recordAnnotation.value() == ExecutionTime.STATIC_INIT) {
                                    buildItem = buildStepMc.newInstance(ofConstructor(STATIC_RECORDER,
                                            "org.jboss.shamrock.deployment.recording.BytecodeRecorderImpl"), bytecodeRecorder);
                                } else {
                                    buildItem = buildStepMc.newInstance(ofConstructor(MAIN_RECORDER,
                                            "org.jboss.shamrock.deployment.recording.BytecodeRecorderImpl"), bytecodeRecorder);
                                }
                                buildStepMc.invokeVirtualMethod(ofMethod(BuildContext.class, "produce", void.class, BuildItem.class), buildStepMc.getMethodParam(0), buildItem);
                            }


                            CatchBlockCreator catchBlockCreator = table.addCatch(Exception.class);
                            catchBlockCreator.throwException(RuntimeException.class, "Failed to process build step", catchBlockCreator.getCaughtException());
                            buildStepMc.returnValue(null);
                        }


                        registerCapabilities(mc, method);
                    } catch (Exception e) {

                        throw new RuntimeException("Failed to process " + processorClassName + "." + method.getSimpleName(), e);
                    }
                }

                mc.returnValue(null);
            }


        }

        if (!serviceNames.isEmpty()) {
            //we read them first, as if an IDE has processed this we may not have seen the full set of names
            try {
                String relativeName = "META-INF/services/" + BuildProvider.class.getName();
                try {
                    FileObject res = processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", relativeName);
                    try (BufferedReader reader = new BufferedReader(res.openReader(true))) {
                        String r;
                        while ((r = reader.readLine()) != null) {
                            serviceNames.add(r.trim());
                        }
                    }
                } catch (IOException ignore) {
                }

                FileObject res = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", relativeName, processorElements.toArray(new Element[0]));

                try (Writer out = res.openWriter()) {
                    for (String service : serviceNames) {
                        out.write(service);
                        out.write("\n");
                    }
                }
                FileObject descriptions = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/shamrock-descriptions.properties");
                try (OutputStream out = descriptions.openOutputStream()) {
                    configProperties.store(out, "");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void injectConfigField(BytecodeCreator bytecode, ResultHandle obj, VariableElement field, String className, ResultHandle keyPrefix, Properties configProperties, String currentKeyDesc) {
        ConfigInjectionInfo val = readConfigAnnotation(field);
        ResultHandle value = createConfigValue(bytecode, val, keyPrefix, configProperties, currentKeyDesc);
        String jvmType;
        switch (val.type) {
            case PRIMITIVE_BOOLEAN:
                jvmType = "Z";
                break;
            case PRIMITIVE_INT:
                jvmType = "I";
                break;
            case INTEGER:
                jvmType = Integer.class.getName();
                break;
            case BOOLEAN:
                jvmType = Boolean.class.getName();
                break;
            case CUSTOM_TYPE:
                jvmType = val.customTypeName;
                break;
            case MAP:
                jvmType = Map.class.getName();
                break;
            case STRING:
                jvmType = String.class.getName();
                break;
            default:
                throw new RuntimeException("unknown type " + val.type);
        }
        if (val.optional) {
            ResultHandle instance = bytecode.invokeStaticMethod(ofMethod(Optional.class, "ofNullable", Optional.class, Object.class), value);
            bytecode.writeInstanceField(FieldDescriptor.of(className, field.getSimpleName().toString(), Optional.class), obj, instance);
        } else {
            bytecode.writeInstanceField(FieldDescriptor.of(className, field.getSimpleName().toString(), jvmType), obj, value);
        }
    }

    private ResultHandle createConfigValue(BytecodeCreator bc, ConfigInjectionInfo val, ResultHandle keyPrefix, Properties configProperties, String currentKeyDescPrefix) {
        String currentKeyDesc = currentKeyDescPrefix.isEmpty() ? val.name : (currentKeyDescPrefix + "." + val.name);
        ResultHandle sbr = bc.newInstance(ofConstructor(StringBuilder.class));
        bc.invokeVirtualMethod(ofMethod(StringBuilder.class, "append", StringBuilder.class, String.class), sbr, keyPrefix);
        bc.invokeVirtualMethod(ofMethod(StringBuilder.class, "append", StringBuilder.class, String.class), sbr, bc.load(val.name));
        ResultHandle keyName = bc.invokeVirtualMethod(ofMethod(Object.class, "toString", String.class), sbr); //the value name


        if (val.type != ConfigType.MAP && val.type != ConfigType.CUSTOM_TYPE) {
            configProperties.put(currentKeyDesc, val.javadoc);
        }

        ResultHandle defaultVal;
        if (val.defaultValue != null) {
            defaultVal = bc.load(val.defaultValue);
        } else {
            defaultVal = bc.loadNull();
        }
        switch (val.type) {
            case STRING:
                return bc.invokeStaticMethod(ofMethod(SHAMROCK_CONFIG, "getString", String.class, String.class, String.class, boolean.class), keyName, defaultVal, val.optional ? bc.load(true) : bc.load(false));
            case PRIMITIVE_BOOLEAN:
                return bc.invokeStaticMethod(ofMethod(SHAMROCK_CONFIG, "getBoolean", boolean.class, String.class, String.class), keyName, defaultVal);
            case PRIMITIVE_INT:
                return bc.invokeStaticMethod(ofMethod(SHAMROCK_CONFIG, "getInt", int.class, String.class, String.class), keyName, defaultVal);
            case INTEGER:
                return bc.invokeStaticMethod(ofMethod(SHAMROCK_CONFIG, "getBoxedInt", Integer.class, String.class, String.class, boolean.class), keyName, defaultVal, val.optional ? bc.load(true) : bc.load(false));
            case BOOLEAN:
                return bc.invokeStaticMethod(ofMethod(SHAMROCK_CONFIG, "getBoxedBoolean", Boolean.class, String.class, String.class, boolean.class), keyName, defaultVal, val.optional ? bc.load(true) : bc.load(false));
            case CUSTOM_TYPE: {
                ResultHandle instance = bc.newInstance(ofConstructor(val.customTypeName));
                Element element = processingEnv.getElementUtils().getTypeElement(val.customTypeName);
                if (element == null) {
                    throw new RuntimeException("Could not obtain class information for " + val.customTypeName);
                }

                bc.invokeVirtualMethod(ofMethod(StringBuilder.class, "append", StringBuilder.class, String.class), sbr, bc.load("."));
                ResultHandle keyPrefixName = bc.invokeVirtualMethod(ofMethod(Object.class, "toString", String.class), sbr); //the name + a .
                for (Element e : fieldsIn(element.getEnclosedElements())) {
                    if (isAnnotationPresent(e, CONFIG_PROPERTY_ANNOTATION)) {
                        injectConfigField(bc, instance, (VariableElement) e, val.customTypeName, keyPrefixName, configProperties, currentKeyDesc);
                    }
                }
                return instance;
            }
            case MAP:
                //the complex one
                ResultHandle instance = bc.newInstance(ofConstructor(HashMap.class.getName()));
                ResultHandle names = bc.invokeStaticMethod(ofMethod(SHAMROCK_CONFIG, "getNames", Set.class, String.class), keyName);
                FunctionCreator func = bc.createFunction(Consumer.class);
                bc.invokeInterfaceMethod(ofMethod(Set.class, "forEach", void.class, Consumer.class), names, func.getInstance());
                //we now need to set up func to create the objects and insert them into the map
                ResultHandle mi = func.getBytecode().newInstance(ofConstructor(val.customTypeName));
                Element element = processingEnv.getElementUtils().getTypeElement(val.customTypeName);
                for (Element e : fieldsIn(element.getEnclosedElements())) {
                    if (isAnnotationPresent(e, CONFIG_PROPERTY_ANNOTATION)) {
                        ResultHandle sb = func.getBytecode().newInstance(ofConstructor(StringBuilder.class));
                        func.getBytecode().invokeVirtualMethod(ofMethod(StringBuilder.class, "append", StringBuilder.class, String.class), sb, keyPrefix);
                        func.getBytecode().invokeVirtualMethod(ofMethod(StringBuilder.class, "append", StringBuilder.class, String.class), sb, func.getBytecode().getMethodParam(0));
                        func.getBytecode().invokeVirtualMethod(ofMethod(StringBuilder.class, "append", StringBuilder.class, String.class), sb, bc.load("." + val.name + "."));
                        ResultHandle handle = func.getBytecode().invokeVirtualMethod(ofMethod(Object.class, "toString", String.class), sb);
                        injectConfigField(func.getBytecode(), mi, (VariableElement) e, val.customTypeName, handle, configProperties, currentKeyDesc + ".*");
                    }
                }
                func.getBytecode().returnValue(mi);
                func.getBytecode().invokeVirtualMethod(ofMethod(HashMap.class, "put", Object.class, Object.class, Object.class), instance, func.getBytecode().getMethodParam(0), mi);
                return instance;
            default:
                throw new RuntimeException("unknown type " + val.type);
        }
    }

    private ConfigInjectionInfo readConfigAnnotation(VariableElement field) {
        for (AnnotationMirror i : field.getAnnotationMirrors()) {
            if (((TypeElement) i.getAnnotationType().asElement()).getQualifiedName().toString().equals(CONFIG_PROPERTY_ANNOTATION)) {
                ConfigInjectionInfo ret = new ConfigInjectionInfo();
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> elem : i.getElementValues().entrySet()) {
                    switch (elem.getKey().getSimpleName().toString()) {
                        case "name":
                            ret.name = (String) elem.getValue().getValue();
                            break;
                        case "defaultValue":
                            ret.defaultValue = (String) elem.getValue().getValue();
                            break;
                        default:
                            throw new RuntimeException("Unknown annotation value " + elem.getKey());
                    }
                }
                if (field.asType() instanceof PrimitiveType) {
                    PrimitiveType type = (PrimitiveType) field.asType();
                    switch (type.getKind()) {
                        case INT:
                            ret.type = ConfigType.PRIMITIVE_INT;
                            break;
                        case BOOLEAN:
                            ret.type = ConfigType.PRIMITIVE_BOOLEAN;
                            break;
                        default:
                            throw new RuntimeException("Unable to inject config into primitive type of " + type + " this has not been implemented yet");
                    }
                } else if (field.asType() instanceof DeclaredType) {

                    DeclaredType type = (DeclaredType) field.asType();
                    String simpleType = processingEnv.getElementUtils().getBinaryName(((TypeElement) type.asElement())).toString();

                    if (simpleType.equals(Map.class.getName())) {
                        ret.type = ConfigType.MAP;
                        if (type.getTypeArguments().size() != 2) {
                            throw new RuntimeException("Cannot use @ConfigProperty on a Map that does not include a generic type " + field);
                        }
                        TypeMirror typeMirror = type.getTypeArguments().get(1);
                        ret.customTypeName = processingEnv.getElementUtils().getBinaryName(((TypeElement) ((DeclaredType) typeMirror).asElement())).toString();
                    } else if (simpleType.equals(Optional.class.getName())) {
                        if (type.getTypeArguments().size() != 1) {
                            throw new RuntimeException("Cannot use @ConfigProperty on an Optional that does not include a generic type " + field);
                        }
                        ret.optional = true;
                        TypeMirror typeMirror = type.getTypeArguments().get(0);
                        String typeName = processingEnv.getElementUtils().getBinaryName(((TypeElement) ((DeclaredType) typeMirror).asElement())).toString();
                        if (typeName.equals(String.class.getName())) {
                            ret.type = ConfigType.STRING;
                        } else if (typeName.equals(Integer.class.getName())) {
                            ret.type = ConfigType.INTEGER;
                        } else if (typeName.equals(Boolean.class.getName())) {
                            ret.type = ConfigType.BOOLEAN;
                        } else {
                            if (!isAnnotationPresent(processingEnv.getTypeUtils().asElement(typeMirror), CONFIGURED_TYPE_ANNOTATION)) {
                                throw new RuntimeException("Cannot inject a configured instance of " + typeName + " as it is not annotated with " + CONFIGURED_TYPE_ANNOTATION + " into " + field);
                            }
                            ret.type = ConfigType.CUSTOM_TYPE;
                            ret.customTypeName = typeName;
                        }
                    } else if (simpleType.equals(String.class.getName())) {
                        ret.type = ConfigType.STRING;
                    } else if (simpleType.equals(Integer.class.getName())) {
                        ret.type = ConfigType.INTEGER;
                    } else if (simpleType.equals(Boolean.class.getName())) {
                        ret.type = ConfigType.BOOLEAN;
                    } else {
                        ret.type = ConfigType.CUSTOM_TYPE;
                        if (!isAnnotationPresent(processingEnv.getTypeUtils().asElement(type), CONFIGURED_TYPE_ANNOTATION)) {
                            throw new RuntimeException("Cannot inject a configured instance of " + simpleType + " as it is not annotated with " + CONFIGURED_TYPE_ANNOTATION + " into " + field);
                        }
                        ret.customTypeName = simpleType;
                    }
                } else {
                    throw new RuntimeException("Unknown field type " + field);
                }
                if (ret.optional && ret.defaultValue != null) {
                    throw new RuntimeException(field + " is optional but has a default value. Default values must not be set for optional elements");
                }

                if(ret.type != ConfigType.MAP && ret.type != ConfigType.CUSTOM_TYPE) {
                    ret.javadoc = processingEnv.getElementUtils().getDocComment(field);
                    if (ret.javadoc == null) {
                        ret.javadoc = tryLoadJavadocFromFile(field);
                        if (ret.javadoc == null) {
                            throw new RuntimeException("Field must include a javadoc description "  + field.getEnclosingElement() + "." + field);
                        }
                    }
                }
                return ret;
            }
        }
        throw new RuntimeException("Could not find Configured annotation, this should not happen");
    }

    private String tryLoadJavadocFromFile(VariableElement field) {
        TypeElement ownerClass = (TypeElement) field.getEnclosingElement();
        try {
            FileObject file = processingEnv.getFiler().getResource(StandardLocation.CLASS_PATH, "", ownerClass.getQualifiedName().toString().replace(".", "/") + ".confjavadoc");
            try (Reader reader = file.openReader(true)) {
                Properties properties = new Properties();
                properties.load(reader);
                return (String) properties.get(field.getSimpleName().toString());
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "Unable to load pre-saved javadoc", field);
        }

        return null;
    }

    private MethodReturnInfo processMethodReturnType(ExecutableElement method) {
        MethodReturnInfo returnInfo = new MethodReturnInfo();
        //handle the return type
        if (method.getReturnType().getKind() != TypeKind.VOID) {
            if (method.getReturnType().getKind().isPrimitive()) {
                throw new RuntimeException("@BuildStep method return type cannot be primitive: " + method);
            }
            DeclaredType returnTypeElement = (DeclaredType) method.getReturnType();
            String returnType = processingEnv.getElementUtils().getBinaryName(((TypeElement) returnTypeElement.asElement())).toString();

            if (returnType.equals(List.class.getName())) {
                returnInfo.list = true;

                if (returnTypeElement.getTypeArguments().size() != 1) {
                    throw new RuntimeException("Cannot use @BuildResource on a list that does not include a generic type");
                }
                TypeMirror typeMirror = returnTypeElement.getTypeArguments().get(0);

                verifyType(typeMirror, MultiBuildItem.class);
                returnInfo.producedReturnType = processingEnv.getElementUtils().getBinaryName(((TypeElement) ((DeclaredType) typeMirror).asElement())).toString();
                returnInfo.rawReturnType = returnType;
            } else {
                verifyType(returnTypeElement, BuildItem.class);
                returnInfo.rawReturnType = returnType;
                returnInfo.producedReturnType = returnType;
            }
        }
        return returnInfo;
    }

    private String registerBuildStep(List<InjectedBuildResource> fieldList, String processorClassName, MethodCreator mc, String producedReturnType, List<InjectedBuildResource> methodInjection, boolean templatePresent, Record recordAnnotation, ResultHandle theInstance) {
        final String buildStepName = processorClassName + "BuildStep" + classNameCounter.incrementAndGet();

        ResultHandle step = mc.newInstance(ofConstructor(buildStepName, processorClassName), theInstance);
        ResultHandle builder = mc.invokeVirtualMethod(MethodDescriptor.ofMethod(BuildChainBuilder.class, "addBuildStep", BuildStepBuilder.class, org.jboss.builder.BuildStep.class), mc.getMethodParam(0), step);

        //register fields
        for (InjectedBuildResource field : fieldList) {
            if (field.consumedTypeName != null) {
                mc.invokeVirtualMethod(ofMethod(BuildStepBuilder.class, "consumes", BuildStepBuilder.class, Class.class), builder, mc.loadClass(field.consumedTypeName));
            }
            if (field.producedTypeName != null) {
                mc.invokeVirtualMethod(ofMethod(BuildStepBuilder.class, "produces", BuildStepBuilder.class, Class.class), builder, mc.loadClass(field.producedTypeName));
            }
        }
        //if it is using bytecode recording register the production of a new recorder
        if (templatePresent) {
            ResultHandle type;
            if (recordAnnotation.value() == ExecutionTime.STATIC_INIT) {
                type = mc.loadClass(STATIC_RECORDER);
            } else {
                type = mc.loadClass(MAIN_RECORDER);
            }
            if (recordAnnotation.optional()) {
                mc.invokeVirtualMethod(ofMethod(BuildStepBuilder.class, "produces", BuildStepBuilder.class, Class.class, ProduceFlag.class), builder, type, mc.readStaticField(FieldDescriptor.of(ProduceFlag.class, "WEAK", ProduceFlag.class)));
            } else {
                mc.invokeVirtualMethod(ofMethod(BuildStepBuilder.class, "produces", BuildStepBuilder.class, Class.class), builder, type);
            }

        }
        //register parameter injection
        for (InjectedBuildResource injection : methodInjection) {
            if (injection.injectionType != InjectionType.TEMPLATE && injection.injectionType != InjectionType.RECORDER_CONTEXT && injection.injectionType != InjectionType.EXECUTOR) {
                if (injection.consumedTypeName != null) {
                    mc.invokeVirtualMethod(ofMethod(BuildStepBuilder.class, "consumes", BuildStepBuilder.class, Class.class), builder, mc.loadClass(injection.consumedTypeName));
                }
                if (injection.producedTypeName != null) {
                    mc.invokeVirtualMethod(ofMethod(BuildStepBuilder.class, "produces", BuildStepBuilder.class, Class.class), builder, mc.loadClass(injection.producedTypeName));
                }
            }
        }

        //register the production of the return type
        if (producedReturnType != null) {
            mc.invokeVirtualMethod(ofMethod(BuildStepBuilder.class, "produces", BuildStepBuilder.class, Class.class), builder, mc.loadClass(producedReturnType));
        }

        //install it
        mc.invokeVirtualMethod(ofMethod(BuildStepBuilder.class, "build", BuildChainBuilder.class), builder);
        return buildStepName;
    }

    private ResultHandle generateMethodInjection(MethodCreator buildStepMc, ResultHandle bytecodeRecorder, InjectedBuildResource i) {
        ResultHandle val;
        if (i.injectionType == InjectionType.RECORDER_CONTEXT) {
            val = bytecodeRecorder;
        } else if (i.injectionType == InjectionType.TEMPLATE) {
            val = buildStepMc.invokeVirtualMethod(ofMethod("org.jboss.shamrock.deployment.recording.BytecodeRecorderImpl", "getRecordingProxy", Object.class, Class.class), bytecodeRecorder, buildStepMc.loadClass(i.consumedTypeName));
        } else if (i.injectionType == InjectionType.EXECUTOR) {
            val = buildStepMc.invokeVirtualMethod(ofMethod(BuildContext.class, "getExecutor", Executor.class), buildStepMc.getMethodParam(0));
        } else if (i.injectionType == InjectionType.SIMPLE) {
            val = buildStepMc.invokeVirtualMethod(ofMethod(BuildContext.class, "consume", SimpleBuildItem.class, Class.class), buildStepMc.getMethodParam(0), buildStepMc.loadClass(i.consumedTypeName));
        } else if (i.injectionType == InjectionType.LIST) {
            val = buildStepMc.invokeVirtualMethod(ofMethod(BuildContext.class, "consumeMulti", List.class, Class.class), buildStepMc.getMethodParam(0), buildStepMc.loadClass(i.consumedTypeName));
        } else {
            val = buildStepMc.newInstance(ofConstructor(BUILD_PRODUCER, Class.class, BuildContext.class), buildStepMc.loadClass(i.producedTypeName), buildStepMc.getMethodParam(0));
        }
        return val;
    }

    private void generateFieldInjection(String processorClassName, MethodCreator buildStepMc, ResultHandle p, InjectedBuildResource field) {
        if (field.injectionType == InjectionType.SIMPLE) {
            ResultHandle val = buildStepMc.invokeVirtualMethod(ofMethod(BuildContext.class, "consume", SimpleBuildItem.class, Class.class), buildStepMc.getMethodParam(0), buildStepMc.loadClass(field.consumedTypeName));
            buildStepMc.writeInstanceField(FieldDescriptor.of(processorClassName, field.element.getSimpleName().toString(), field.consumedTypeName), p, val);
        } else if (field.injectionType == InjectionType.LIST) {
            ResultHandle val = buildStepMc.invokeVirtualMethod(ofMethod(BuildContext.class, "consumeMulti", List.class, Class.class), buildStepMc.getMethodParam(0), buildStepMc.loadClass(field.consumedTypeName));
            buildStepMc.writeInstanceField(FieldDescriptor.of(processorClassName, field.element.getSimpleName().toString(), List.class), p, val);
        } else if (field.injectionType == InjectionType.EXECUTOR) {
            ResultHandle val = buildStepMc.invokeVirtualMethod(ofMethod(BuildContext.class, "getExecutor", Executor.class), buildStepMc.getMethodParam(0));
            buildStepMc.writeInstanceField(FieldDescriptor.of(processorClassName, field.element.getSimpleName().toString(), Executor.class), p, val);
        } else if (field.injectionType == InjectionType.TEMPLATE || field.injectionType == InjectionType.RECORDER_CONTEXT) {
            throw new RuntimeException("Cannot inject @Template class into a field, only method parameter injection is supported for templates. Field: " + field.element);
        } else {
            ResultHandle val = buildStepMc.newInstance(ofConstructor(BUILD_PRODUCER, Class.class, BuildContext.class), buildStepMc.loadClass(field.producedTypeName), buildStepMc.getMethodParam(0));
            buildStepMc.writeInstanceField(FieldDescriptor.of(processorClassName, field.element.getSimpleName().toString(), BuildProducer.class), p, val);
        }
    }

    private void registerCapabilities(MethodCreator mc, ExecutableElement method) {
        ResultHandle step;
        ResultHandle builder;
        String[] capabilities = method.getAnnotation(BuildStep.class).providesCapabilities();
        if (capabilities.length > 0) {
            for (String i : capabilities) {
                step = mc.newInstance(ofConstructor("org.jboss.shamrock.deployment.steps.CapabilityBuildStep", String.class), mc.load(i));
                builder = mc.invokeVirtualMethod(MethodDescriptor.ofMethod(BuildChainBuilder.class, "addBuildStep", BuildStepBuilder.class, org.jboss.builder.BuildStep.class), mc.getMethodParam(0), step);
                mc.invokeVirtualMethod(ofMethod(BuildStepBuilder.class, "produces", BuildStepBuilder.class, Class.class), builder, mc.loadClass("org.jboss.shamrock.deployment.builditem.CapabilityBuildItem"));
                mc.invokeVirtualMethod(ofMethod(BuildStepBuilder.class, "build", BuildChainBuilder.class), builder);
            }
        }
    }

    private InjectedBuildResource createInjectionResource(Element element) {
        try {
            TypeMirror elementType = element.asType();
            if (elementType.getKind() != TypeKind.DECLARED) {
                throw new RuntimeException("Unexpected field type: " + elementType);
            }
            return createInjectionResource(element, (DeclaredType) elementType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to process " + element, e);
        }
    }

    private InjectedBuildResource createInjectionResource(Element element, DeclaredType elementType) {
        DeclaredType type = elementType;
        String simpleType = processingEnv.getElementUtils().getBinaryName(((TypeElement) type.asElement())).toString();
        InjectionType ft;
        String producedTypeName = null;
        String consumedTypeName = null;

        if (simpleType.equals(List.class.getName())) {
            ft = InjectionType.LIST;
            if (type.getTypeArguments().size() != 1) {
                throw new RuntimeException("Cannot use @BuildResource on a list that does not include a generic type");
            }
            TypeMirror typeMirror = type.getTypeArguments().get(0);

            verifyType(typeMirror, MultiBuildItem.class);
            consumedTypeName = processingEnv.getElementUtils().getBinaryName(((TypeElement) ((DeclaredType) typeMirror).asElement())).toString();

        } else if (simpleType.equals(BuildProducer.class.getName())) {
            ft = InjectionType.PRODUCER;
            if (type.getTypeArguments().size() != 1) {
                throw new RuntimeException("Cannot use @BuildResource on a BuildProducer that does not include a generic type");
            }
            TypeMirror typeMirror = type.getTypeArguments().get(0);
            verifyType(typeMirror, BuildItem.class);
            producedTypeName = processingEnv.getElementUtils().getBinaryName(((TypeElement) ((DeclaredType) typeMirror).asElement())).toString();
        } else {
            consumedTypeName = simpleType;
            if (isAnnotationPresent(processingEnv.getTypeUtils().asElement(type), TEMPLATE_ANNOTATION)) {
                ft = InjectionType.TEMPLATE;
            } else if (simpleType.equals("org.jboss.shamrock.deployment.recording.RecorderContext")) {
                ft = InjectionType.RECORDER_CONTEXT;
            } else if (simpleType.equals("java.util.concurrent.Executor")) {
                ft = InjectionType.EXECUTOR;
            } else {
                verifyType(type, SimpleBuildItem.class);
                ft = InjectionType.SIMPLE;
            }
        }
        return new InjectedBuildResource(element, ft, producedTypeName, consumedTypeName);
    }

    private boolean isAnnotationPresent(Element element, String annotationName) {
        for (AnnotationMirror i : element.getAnnotationMirrors()) {
            if (((TypeElement) i.getAnnotationType().asElement()).getQualifiedName().toString().equals(annotationName)) {
                return true;
            }
        }
        return false;
    }

    private void verifyType(TypeMirror type, Class expected) {
        if (!processingEnv.getTypeUtils().isSubtype(type, processingEnv.getElementUtils().getTypeElement(expected.getName()).asType())) {
            throw new RuntimeException(type + " is not an instance of " + expected);
        }
    }

    enum InjectionType {
        SIMPLE,
        LIST,
        PRODUCER,
        TEMPLATE,
        RECORDER_CONTEXT,
        EXECUTOR
    }

    static class ConfigInjectionInfo {
        String name;
        String defaultValue;
        String javadoc;
        boolean optional;
        ConfigType type;
        String customTypeName;
    }

    enum ConfigType {
        STRING,
        PRIMITIVE_INT,
        PRIMITIVE_BOOLEAN,
        INTEGER,
        BOOLEAN,
        MAP,
        CUSTOM_TYPE
    }

    static class InjectedBuildResource {
        final Element element;
        final InjectionType injectionType;
        final String producedTypeName;
        final String consumedTypeName;

        InjectedBuildResource(Element element, InjectionType injectionType, String producedTypeName, String consumedTypeName) {
            this.element = element;
            this.injectionType = injectionType;
            this.producedTypeName = producedTypeName;
            this.consumedTypeName = consumedTypeName;
        }
    }

    private class ProcessorClassOutput implements ClassOutput {
        private final TypeElement processor;

        public ProcessorClassOutput(TypeElement processor) {
            this.processor = processor;
        }

        @Override
        public void write(String name, byte[] data) {
            try (OutputStream out = processingEnv.getFiler().createClassFile(name.replace("/", "."), processor).openOutputStream()) {
                out.write(data);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class MethodReturnInfo {

        String rawReturnType = "V";
        String producedReturnType = null;
        boolean list;
    }
}
