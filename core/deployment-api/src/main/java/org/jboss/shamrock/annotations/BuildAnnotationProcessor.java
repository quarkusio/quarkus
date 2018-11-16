package org.jboss.shamrock.annotations;

import static javax.lang.model.util.ElementFilter.fieldsIn;
import static javax.lang.model.util.ElementFilter.methodsIn;
import static org.jboss.protean.gizmo.MethodDescriptor.ofConstructor;
import static org.jboss.protean.gizmo.MethodDescriptor.ofMethod;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Completion;
import javax.annotation.processing.RoundEnvironment;
import javax.inject.Inject;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import org.jboss.builder.BuildChainBuilder;
import org.jboss.builder.BuildContext;
import org.jboss.builder.BuildProvider;
import org.jboss.builder.BuildStepBuilder;
import org.jboss.builder.item.BuildItem;
import org.jboss.builder.item.MultiBuildItem;
import org.jboss.builder.item.SimpleBuildItem;
import org.jboss.protean.gizmo.BranchResult;
import org.jboss.protean.gizmo.CatchBlockCreator;
import org.jboss.protean.gizmo.ClassCreator;
import org.jboss.protean.gizmo.ClassOutput;
import org.jboss.protean.gizmo.FieldDescriptor;
import org.jboss.protean.gizmo.MethodCreator;
import org.jboss.protean.gizmo.MethodDescriptor;
import org.jboss.protean.gizmo.ResultHandle;
import org.jboss.protean.gizmo.TryBlock;

public class BuildAnnotationProcessor extends AbstractProcessor {

    private static final String BUILD_PRODUCER = "org.jboss.shamrock.deployment.BuildProducerImpl";
    private static final String STATIC_RECORDER = "org.jboss.shamrock.deployment.builditem.StaticBytecodeRecorderBuildItem";
    private static final String MAIN_RECORDER = "org.jboss.shamrock.deployment.builditem.MainBytecodeRecorderBuildItem";

    @Override
    public Set<String> getSupportedOptions() {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> ret = new HashSet<>();
        ret.add(Inject.class.getName());
        ret.add(BuildStep.class.getName());
        return ret;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver() && !annotations.isEmpty()) {
            doProcess(annotations, roundEnv);
        }
        return true;
    }

    @Override
    public Iterable<? extends Completion> getCompletions(Element element, AnnotationMirror annotation, ExecutableElement member, String userText) {
        return null;
    }


    public void doProcess(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        int classNameCounter = 0;
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


                for (ExecutableElement method : methodMap.get(processor)) {

                    final String buildStepName = processorClassName + "BuildStep" + (classNameCounter++);
                    ResultHandle step = mc.newInstance(ofConstructor(buildStepName));
                    classNameCounter++;
                    if (method.getModifiers().contains(Modifier.PRIVATE)) {
                        throw new RuntimeException("@BuildStep methods cannot be private: " + processorClassName + ":" + method);
                    }

                    String rawReturnType = "V";
                    String producedType = null;
                    List<InjectedBuildResource> methodInjection = new ArrayList<>();
                    List<String> methodParamTypes = new ArrayList<>();
                    boolean listReturn = false;
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

                    //handle the return type
                    if (method.getReturnType().getKind() != TypeKind.VOID) {
                        if (method.getReturnType().getKind().isPrimitive()) {
                            throw new RuntimeException("@BuildStep method return type cannot be primitive: " + method);
                        }
                        DeclaredType returnTypeElement = (DeclaredType) method.getReturnType();
                        String returnType = processingEnv.getElementUtils().getBinaryName(((TypeElement) returnTypeElement.asElement())).toString();

                        if (returnType.equals(List.class.getName())) {
                            listReturn = true;

                            if (returnTypeElement.getTypeArguments().size() != 1) {
                                throw new RuntimeException("Cannot use @BuildResource on a list that does not include a generic type");
                            }
                            TypeMirror typeMirror = returnTypeElement.getTypeArguments().get(0);

                            verifyType(typeMirror, MultiBuildItem.class);
                            producedType = processingEnv.getElementUtils().getBinaryName(((TypeElement) ((DeclaredType) typeMirror).asElement())).toString();
                            rawReturnType = returnType;
                        } else {
                            verifyType(returnTypeElement, BuildItem.class);
                            rawReturnType = returnType;
                            producedType = returnType;
                        }
                    }

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
                        if (recordAnnotation.value() == ExecutionTime.STATIC_INIT) {
                            mc.invokeVirtualMethod(ofMethod(BuildStepBuilder.class, "produces", BuildStepBuilder.class, Class.class), builder, mc.loadClass(STATIC_RECORDER));
                        } else {
                            mc.invokeVirtualMethod(ofMethod(BuildStepBuilder.class, "produces", BuildStepBuilder.class, Class.class), builder, mc.loadClass(MAIN_RECORDER));
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
                    if (producedType != null) {
                        mc.invokeVirtualMethod(ofMethod(BuildStepBuilder.class, "produces", BuildStepBuilder.class, Class.class), builder, mc.loadClass(producedType));
                    }

                    //install it
                    mc.invokeVirtualMethod(ofMethod(BuildStepBuilder.class, "build", BuildChainBuilder.class), builder);

                    //now generate the actual invoker class that runs the build step
                    try (ClassCreator buildStepCreator = new ClassCreator(new ProcessorClassOutput(processor), buildStepName, null, Object.class.getName(), org.jboss.builder.BuildStep.class.getName())) {
                        MethodCreator buildStepMc = buildStepCreator.getMethodCreator("execute", void.class, BuildContext.class);

                        ResultHandle p = buildStepMc.newInstance(ofConstructor(processorClassName));

                        //do the field injection
                        for (InjectedBuildResource field : fieldList) {
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
                        TryBlock table = buildStepMc.tryBlock();
                        List<ResultHandle> args = new ArrayList<>();
                        ResultHandle bytecodeRecorder = null;
                        if (templatePresent) {
                            bytecodeRecorder = buildStepMc.newInstance(ofConstructor("org.jboss.shamrock.deployment.recording.BytecodeRecorderImpl", boolean.class), buildStepMc.load(recordAnnotation.value() == ExecutionTime.STATIC_INIT));
                        }

                        for (InjectedBuildResource i : methodInjection) {
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
                            args.add(val);
                        }


                        ResultHandle handle = buildStepMc.invokeVirtualMethod(ofMethod(processorClassName, method.getSimpleName().toString(), rawReturnType, methodParamTypes.toArray(new String[0])), p, args.toArray(new ResultHandle[0]));
                        if (producedType != null) {
                            BranchResult ifstm = buildStepMc.ifNull(handle);
                            if (listReturn) {
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
            } catch (IOException e) {
                throw new RuntimeException(e);
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
            if (isTemplate(processingEnv.getTypeUtils().asElement(type))) {
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

    private boolean isTemplate(Element element) {
        for (AnnotationMirror i : element.getAnnotationMirrors()) {
            if (((TypeElement) i.getAnnotationType().asElement()).getQualifiedName().toString().equals("org.jboss.shamrock.runtime.Template")) {
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
}
