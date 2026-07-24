package io.quarkus.core.deployment.action.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

import org.jboss.logging.Logger;

import io.quarkus.core.impl.ServiceGraph;
import io.quarkus.core.impl.ServiceNode;
import io.quarkus.core.impl.SortedNullSafeMap;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.StartupContext;
import io.smallrye.classfile.BootstrapMethodEntry;
import io.smallrye.classfile.ClassFile;
import io.smallrye.classfile.ClassModel;
import io.smallrye.classfile.CodeBuilder;
import io.smallrye.classfile.CodeElement;
import io.smallrye.classfile.CodeModel;
import io.smallrye.classfile.Label;
import io.smallrye.classfile.MethodModel;
import io.smallrye.classfile.Opcode;
import io.smallrye.classfile.TypeKind;
import io.smallrye.classfile.attribute.EnclosingMethodAttribute;
import io.smallrye.classfile.attribute.InnerClassInfo;
import io.smallrye.classfile.attribute.InnerClassesAttribute;
import io.smallrye.classfile.attribute.NestHostAttribute;
import io.smallrye.classfile.attribute.NestMembersAttribute;
import io.smallrye.classfile.components.ClassRemapper;
import io.smallrye.classfile.constantpool.ConstantPoolBuilder;
import io.smallrye.classfile.constantpool.InvokeDynamicEntry;
import io.smallrye.classfile.constantpool.LoadableConstantEntry;
import io.smallrye.classfile.constantpool.MemberRefEntry;
import io.smallrye.classfile.constantpool.MethodHandleEntry;
import io.smallrye.classfile.constantpool.MethodTypeEntry;
import io.smallrye.classfile.extras.constant.ConstantUtils;
import io.smallrye.classfile.extras.constant.ExtraClassDesc;
import io.smallrye.classfile.extras.constant.ExtraConstantDescs;
import io.smallrye.classfile.instruction.IncrementInstruction;
import io.smallrye.classfile.instruction.InvokeDynamicInstruction;
import io.smallrye.classfile.instruction.InvokeInstruction;
import io.smallrye.classfile.instruction.LineNumber;
import io.smallrye.classfile.instruction.LoadInstruction;
import io.smallrye.classfile.instruction.LocalVariable;
import io.smallrye.classfile.instruction.LocalVariableType;
import io.smallrye.classfile.instruction.NewObjectInstruction;
import io.smallrye.classfile.instruction.ReturnInstruction;
import io.smallrye.classfile.instruction.StoreInstruction;
import io.smallrye.classfile.instruction.TypeCheckInstruction;
import io.smallrye.serial.SerialContext;

/**
 * Transliterates serializable lambdas into generated classes for the service
 * action system.
 * <p>
 * The transliteration process is split into two phases:
 * <ol>
 * <li>{@link #extract(Serializable, Class, List, List, boolean, boolean, List) extract()} —
 * eagerly extracts the lambda metadata and body during build step execution,
 * producing a {@link TransliteratedAction.ActionService} data record</li>
 * <li>{@link #generateConsolidatedClass(String, List) generateConsolidatedClass()} —
 * deferred to {@code MainClassBuildStep}, batches multiple actions into a single
 * class with static deploy methods to reduce classloading overhead at startup</li>
 * </ol>
 */
public final class LambdaTransliterator {

    /** Descriptor for {@link ServiceNode}. */
    private static final ClassDesc CD_ServiceNode = ConstantUtils.classDesc(ServiceNode.class);
    /** Descriptor for {@link SortedNullSafeMap}. */
    private static final ClassDesc CD_SortedNullSafeMap = ConstantUtils.classDesc(SortedNullSafeMap.class);
    /** Descriptor for {@link Map}. */
    private static final ClassDesc CD_Map = ConstantUtils.classDesc(Map.class);

    /**
     * Internal names of context interfaces whose invokeinterface calls
     * are rewritten to invokevirtual on ServiceNode during transliteration.
     */
    private static final Set<String> CONTEXT_INTERFACE_INTERNALS = Set.of(
            "io/quarkus/core/StartContext",
            "io/quarkus/core/AsyncStartContext",
            "io/quarkus/core/AsyncVoidStartContext",
            "io/quarkus/core/AsyncStopContext");

    /**
     * Descriptor strings for context interfaces that should be remapped
     * to {@code ServiceNode} in inner method signatures and invokedynamic
     * bootstrap args.
     */
    private static final Set<String> CONTEXT_INTERFACE_DESCRIPTORS = Set.of(
            "Lio/quarkus/core/StartContext;",
            "Lio/quarkus/core/AsyncStartContext;",
            "Lio/quarkus/core/AsyncVoidStartContext;",
            "Lio/quarkus/core/AsyncStopContext;");

    /**
     * Remap any context interface parameter types to {@code ServiceNode}
     * in a method type descriptor. Since the context interfaces are sealed
     * to {@code ServiceNode}, this is always type-safe.
     *
     * @param mtd the original method type descriptor
     * @return the remapped descriptor (unchanged if no context types present)
     */
    private static MethodTypeDesc remapContextTypes(MethodTypeDesc mtd) {
        boolean changed = false;
        ClassDesc[] params = new ClassDesc[mtd.parameterCount()];
        for (int i = 0; i < params.length; i++) {
            ClassDesc p = mtd.parameterType(i);
            if (CONTEXT_INTERFACE_DESCRIPTORS.contains(p.descriptorString())) {
                params[i] = CD_ServiceNode;
                changed = true;
            } else {
                params[i] = p;
            }
        }
        return changed ? MethodTypeDesc.of(mtd.returnType(), params) : mtd;
    }

    private LambdaTransliterator() {
    }

    /**
     * Extract a service action lambda into a {@link TransliteratedAction.ActionService}
     * for deferred class generation.
     * <p>
     * This performs all validation and bytecode analysis of the lambda but does
     * not generate any class. The returned record carries all data needed for
     * later code generation via {@link #generateConsolidatedClass}.
     *
     * @param lambda the serializable lambda instance (must not be {@code null})
     * @param serviceType the service type produced by this action (must not be {@code null})
     * @param serviceNameParts the service name parts (must not be {@code null})
     * @param dependencies the dependency identities, in order (must not be {@code null})
     * @param beforeKeys service keys that should depend on this service (reverse ordering;
     *        must not be {@code null}; may be empty)
     * @param async {@code true} if this is an async action (uses {@code AsyncStartContext})
     * @param staticInit {@code true} if this is a static-init service
     * @param stepId the producing build step's ID
     * @return the extracted action data
     * @throws TransliterationException if the lambda cannot be transliterated
     */
    public static TransliteratedAction.ActionService extract(
            Serializable lambda,
            Class<?> serviceType,
            List<String> serviceNameParts,
            List<Dependency> dependencies,
            List<String> beforeKeys,
            List<String> afterBuildItemClasses,
            boolean async,
            boolean staticInit,
            String stepId) {
        if (staticInit && async) {
            throw new TransliterationException(
                    "Static-init services do not support async actions");
        }
        // extract the SerializedLambda metadata
        SerializedLambda sl = extractSerializedLambda(lambda);

        // only static lambda impl methods are supported
        if (sl.getImplMethodKind() != MethodHandleInfo.REF_invokeStatic) {
            throw new TransliterationException(
                    "Only static lambda implementations are supported; got method kind "
                            + sl.getImplMethodKind() + " for " + sl.getImplClass() + "::" + sl.getImplMethodName()
                            + ". Instance-capturing lambdas and instance method references are not allowed.");
        }

        int capturedArgCount = sl.getCapturedArgCount();

        // resolve captured args to emitters via serialization
        CaptureEmitter[] capturedEmitters = new CaptureEmitter[capturedArgCount];
        MethodTypeDesc mtd0 = MethodTypeDesc.ofDescriptor(sl.getImplMethodSignature());
        var emitter = new SerializedCaptureEmitter();
        // single serializer for the loop: enables cross-capture dedup
        // when the same object is captured in multiple slots
        var serializer = buildSerialContext().createSerializer();
        for (int i = 0; i < capturedArgCount; i++) {
            Object captured = sl.getCapturedArg(i);
            if (captured == null) {
                capturedEmitters[i] = CodeBuilder::aconst_null;
                continue;
            }
            try {
                var serialized = serializer.serialize(captured);
                capturedEmitters[i] = emitter.emitFor(serialized, mtd0.parameterType(i));
            } catch (IOException e) {
                throw new TransliterationException(
                        "Failed to serialize captured argument " + i + " of type "
                                + captured.getClass().getName()
                                + ". Lambda: " + sl.getImplClass().replace('/', '.') + "::" + sl.getImplMethodName(),
                        e);
            }
        }

        // load the impl class bytecode
        String implClassInternal = sl.getImplClass();
        byte[] implClassBytes = loadClassBytes(implClassInternal);

        // parse and find the impl method
        ClassModel classModel = ClassFile.of().parse(implClassBytes);
        String implMethodName = sl.getImplMethodName();
        String implMethodSig = sl.getImplMethodSignature();
        MethodModel implMethod = classModel.methods().stream()
                .filter(m -> m.methodName().stringValue().equals(implMethodName)
                        && m.methodType().stringValue().equals(implMethodSig))
                .findFirst()
                .orElseThrow(() -> new TransliterationException(
                        "Lambda implementation method not found: "
                                + implClassInternal.replace('/', '.') + "::" + implMethodName + implMethodSig));

        CodeModel originalCode = implMethod.code()
                .orElseThrow(() -> new TransliterationException(
                        "Lambda implementation method has no code: " + implMethodName));

        // parse the method descriptor to compute capture slot layout
        MethodTypeDesc mtd = MethodTypeDesc.ofDescriptor(implMethodSig);
        int totalParams = mtd.parameterCount();
        if (capturedArgCount > totalParams) {
            throw new TransliterationException(
                    "Captured arg count (" + capturedArgCount + ") exceeds parameter count (" + totalParams + ")");
        }

        // compute total slot count consumed by captured parameters
        int captureSlots = 0;
        int[] captureSlotStart = new int[capturedArgCount]; // slot index for each capture
        for (int i = 0; i < capturedArgCount; i++) {
            captureSlotStart[i] = captureSlots;
            ClassDesc paramType = mtd.parameterType(i);
            captureSlots += TypeKind.from(paramType).slotSize();
        }
        final int captureSlotCount = captureSlots;

        // build slot→captureIndex mapping for fast lookup
        int[] slotToCaptureIndex = new int[captureSlotCount];
        Arrays.fill(slotToCaptureIndex, -1);
        for (int i = 0; i < capturedArgCount; i++) {
            slotToCaptureIndex[captureSlotStart[i]] = i;
        }

        // compute the transliterated method descriptor (drops captured params)
        ClassDesc[] declaredParamTypes = new ClassDesc[totalParams - capturedArgCount];
        for (int i = capturedArgCount; i < totalParams; i++) {
            declaredParamTypes[i - capturedArgCount] = mtd.parameterType(i);
        }
        MethodTypeDesc actionMethodType = MethodTypeDesc.of(mtd.returnType(), declaredParamTypes);

        String serviceKey = serviceKey(serviceType, serviceNameParts);

        return new TransliteratedAction.ActionService(
                serviceKey,
                List.copyOf(dependencies),
                List.copyOf(beforeKeys),
                List.copyOf(afterBuildItemClasses),
                async,
                staticInit,
                actionMethodType,
                originalCode,
                captureSlotCount,
                slotToCaptureIndex,
                capturedEmitters,
                implClassInternal,
                classModel,
                stepId);
    }

    /**
     * Generate a consolidated class containing deploy methods for multiple
     * service actions.
     * <p>
     * Each action at index {@code i} produces:
     * <ul>
     * <li>{@code public static void deploy$i(StartupContext)} — the entry point</li>
     * <li>For {@link TransliteratedAction.ActionService}: a private static
     * {@code action$i} method (the transliterated lambda body) plus inner
     * lambda methods prefixed with {@code s$i$}</li>
     * <li>For {@link TransliteratedAction.AliasService}: the deploy method
     * simply copies a value between startup context keys</li>
     * <li>For {@link TransliteratedAction.RuntimeValueWrapper}: the deploy method
     * reads the bare service value and stores a {@code RuntimeValue} wrapper
     * under a separate key</li>
     * </ul>
     * The generated class does not implement any interface; deploy methods are
     * invoked directly via {@code invokestatic}.
     *
     * @param classInternalName the internal name for the generated class (using {@code /} separators)
     * @param actions the list of actions to include (must not be empty)
     * @return a map of internal class name → class file bytes; the first entry is the
     *         consolidated class itself, followed by any copied anonymous inner classes
     */
    public static Map<String, byte[]> generateConsolidatedClass(
            String classInternalName,
            List<TransliteratedAction> actions) {
        ClassDesc generatedClassDesc = ExtraClassDesc.ofInternalName(classInternalName);

        Map<String, AnonClassEntry> classRemapTable = new LinkedHashMap<>();
        int[] anonCounter = { 0 };

        byte[] consolidatedBytes = ClassFile.of().build(generatedClassDesc, cb -> {
            cb.withVersion(ClassFile.JAVA_21_VERSION, 0);
            cb.withFlags(ClassFile.ACC_PUBLIC | ClassFile.ACC_FINAL | ClassFile.ACC_SUPER);

            ClassDesc serviceNodeDesc = ConstantUtils.classDesc(ServiceNode.class);

            for (int i = 0; i < actions.size(); i++) {
                TransliteratedAction action = actions.get(i);
                String deployMethodName = "deploy$" + i;

                if (action instanceof TransliteratedAction.ActionService as) {
                    String innerPrefix = "s$" + i + "$";

                    // deploy method: loads deps, inlines action body, signals completion
                    // inner lambdas discovered during inlining are tracked for BFS copy
                    Set<InnerMethod> pendingMethods = new LinkedHashSet<>();
                    cb.withMethodBody(
                            deployMethodName,
                            MethodTypeDesc.of(ConstantDescs.CD_void, serviceNodeDesc),
                            ClassFile.ACC_PUBLIC | ClassFile.ACC_STATIC,
                            code -> generateActionDeployBody(
                                    code, as, generatedClassDesc, deployMethodName, actions,
                                    classRemapTable, classInternalName, anonCounter,
                                    innerPrefix, pendingMethods));

                    // BFS: copy inner lambda methods discovered during inlining
                    UnaryOperator<String> remapper = name -> innerPrefix + name;
                    Set<String> processedMethods = new HashSet<>();
                    while (!pendingMethods.isEmpty()) {
                        Iterator<InnerMethod> it = pendingMethods.iterator();
                        InnerMethod inner = it.next();
                        it.remove();
                        String methodKey = inner.name() + inner.descriptor();
                        if (!processedMethods.add(methodKey)) {
                            continue;
                        }
                        MethodModel innerMethod = as.enclosingClassModel().methods().stream()
                                .filter(m -> m.methodName().stringValue().equals(inner.name())
                                        && m.methodType().stringValue().equals(inner.descriptor()))
                                .findFirst()
                                .orElseThrow(() -> new TransliterationException(
                                        "Inner lambda method not found: "
                                                + as.enclosingClassInternal().replace('/', '.') + "::"
                                                + inner.name() + inner.descriptor()));
                        CodeModel innerCode = innerMethod.code()
                                .orElseThrow(() -> new TransliterationException(
                                        "Inner lambda method has no code: " + inner.name()));
                        MethodTypeDesc innerMtd = remapContextTypes(
                                MethodTypeDesc.ofDescriptor(inner.descriptor()));
                        cb.withMethodBody(
                                inner.remappedName(),
                                innerMtd,
                                ClassFile.ACC_PRIVATE | ClassFile.ACC_STATIC,
                                code -> copyMethodBody(innerCode, code,
                                        0, new int[0], new CaptureEmitter[0],
                                        as.enclosingClassInternal(), generatedClassDesc,
                                        pendingMethods, remapper,
                                        classRemapTable, classInternalName, anonCounter,
                                        inner.remappedName(), innerMtd,
                                        null, -1));
                    }
                } else if (action instanceof TransliteratedAction.AliasService alias) {
                    // alias deploy: copies value from recorder key to service key, then completes
                    cb.withMethodBody(
                            deployMethodName,
                            MethodTypeDesc.of(ConstantDescs.CD_void, serviceNodeDesc),
                            ClassFile.ACC_PUBLIC | ClassFile.ACC_STATIC,
                            code -> generateAliasDeployBody(code, alias));
                } else if (action instanceof TransliteratedAction.RuntimeValueWrapper rvw) {
                    // rv wrapper deploy: reads service value, wraps in RuntimeValue, stores, then completes
                    cb.withMethodBody(
                            deployMethodName,
                            MethodTypeDesc.of(ConstantDescs.CD_void, serviceNodeDesc),
                            ClassFile.ACC_PUBLIC | ClassFile.ACC_STATIC,
                            code -> generateRuntimeValueWrapperDeployBody(code, rvw));
                }
            }

            // emit nest-mate and inner class attributes for any copied anonymous classes
            if (!classRemapTable.isEmpty()) {
                List<ClassDesc> innerDescs = classRemapTable.values().stream()
                        .map(e -> ExtraClassDesc.ofInternalName(e.newInternalName()))
                        .toList();
                cb.with(NestMembersAttribute.ofSymbols(innerDescs));
                cb.with(InnerClassesAttribute.of(innerDescs.stream()
                        .map(d -> InnerClassInfo.of(d,
                                Optional.empty(), Optional.empty(), 0))
                        .toList()));
            }
        });

        // build the result map: consolidated class + any anonymous class copies
        Map<String, byte[]> result = new LinkedHashMap<>();
        if (!classRemapTable.isEmpty()) {
            ClassDesc consolidatedDesc = ExtraClassDesc.ofInternalName(classInternalName);
            Map<ClassDesc, ClassDesc> descMap = new HashMap<>();
            for (var entry : classRemapTable.entrySet()) {
                descMap.put(ExtraClassDesc.ofInternalName(entry.getKey()),
                        ExtraClassDesc.ofInternalName(entry.getValue().newInternalName()));
            }
            // remap remaining references in method descriptors and invokedynamic types
            ClassModel consolidatedModel = ClassFile.of().parse(consolidatedBytes);
            consolidatedBytes = ClassRemapper.of(descMap).remapClass(ClassFile.of(), consolidatedModel);
            for (var entry : classRemapTable.entrySet()) {
                result.put(entry.getValue().newInternalName(),
                        copyInnerClass(entry.getKey(), entry.getValue(),
                                descMap, consolidatedDesc));
            }
        }
        result.put(classInternalName, consolidatedBytes);
        return result;
    }

    /**
     * Generate the deploy method body for an {@link TransliteratedAction.ActionService}.
     * <p>
     * The generated code loads dependencies, invokes the transliterated action
     * method, and signals completion on the {@link ServiceNode}. The node itself
     * serves as the start context (it implements the context interfaces).
     * <p>
     * Slot numbering starts at 0 for the {@code ServiceNode} parameter
     * (static method — no {@code this}).
     *
     * @param code the code builder to emit instructions into
     * @param action the action service data
     * @param generatedClassDesc the generated class descriptor
     * @param deployMethodName the deploy method name (for inner lambda enclosing method references)
     * @param allActions all actions in the consolidated class (for consumeAll resolution)
     * @param classRemapTable shared map tracking anonymous inner class remappings
     * @param consolidatedClassInternalName the internal name of the consolidated class
     * @param anonCounter shared counter for anonymous class naming
     * @param innerPrefix prefix for inner lambda method names
     * @param pendingInnerMethods set to which discovered inner lambda methods are added
     */
    private static void generateActionDeployBody(
            CodeBuilder code,
            TransliteratedAction.ActionService action,
            ClassDesc generatedClassDesc,
            String deployMethodName,
            List<TransliteratedAction> allActions,
            Map<String, AnonClassEntry> classRemapTable,
            String consolidatedClassInternalName,
            int[] anonCounter,
            String innerPrefix,
            Set<InnerMethod> pendingInnerMethods) {
        // slot 0 = ServiceNode (static method — no `this`)
        // the ServiceNode is both the method parameter and the start context
        ClassDesc serviceNodeDesc = ConstantUtils.classDesc(ServiceNode.class);
        ClassDesc objectDesc = ConstantUtils.classDesc(Object.class);
        ClassDesc stringDesc = ConstantUtils.classDesc(String.class);
        MethodTypeDesc actionMethodType = action.actionMethodType();
        List<Dependency> dependencies = action.dependencies();
        String serviceKey = action.serviceKey();
        boolean async = action.async();

        // load injected dependencies via indexed access
        // Normal (non-configDirect, non-consumeAll) deps occupy positions in the
        // ServiceNode's dependency array. The graphDepIndex tracks positions in
        // that array independently of the full dependency list index, since
        // configDirect and consumeAll deps are resolved through other mechanisms
        // and do not have entries in the ServiceNode's dependency array.
        ClassDesc classDesc = ConstantUtils.classDesc(Class.class);
        ClassDesc configLookupDesc = ClassDesc.of("io.quarkus.runtime.configuration.ConfigLookup");
        int resultSlot = 1; // first available slot after ServiceNode param
        int graphDepIndex = 0; // position in the ServiceNode's dependency array
        for (Dependency dep : dependencies) {
            if (!dep.injected()) {
                continue;
            }
            if (dep.configDirect()) {
                // config-direct dep: resolve directly from SmallRye Config
                code.loadConstant(dep.type().describeConstable().orElseThrow());
                code.invokestatic(configLookupDesc, "getConfigMapping",
                        MethodTypeDesc.of(objectDesc, classDesc));
                code.checkcast(dep.type().describeConstable().orElseThrow());
            } else if (dep.consumeAll()) {
                // consumeAll: expanded to multiple consecutive dependency slots by
                // ServiceGraphBuilder (one per matching service, sorted by name).
                // Build a SortedNullSafeMap from the indexed dependency values.
                String prefix = dep.keyPrefix();
                List<String[]> matches = new ArrayList<>();
                for (TransliteratedAction ta : allActions) {
                    if (ta instanceof TransliteratedAction.ActionService as) {
                        String matchKey = as.serviceKey();
                        if (matchKey.startsWith(prefix) && matchKey.length() > prefix.length()) {
                            matches.add(new String[] { matchKey.substring(prefix.length()), matchKey });
                        }
                    }
                }
                matches.sort(Comparator.comparing(a -> a[0]));
                if (matches.isEmpty()) {
                    code.invokestatic(CD_SortedNullSafeMap, "of",
                            MethodTypeDesc.of(CD_Map));
                } else if (matches.size() <= 4) {
                    for (String[] match : matches) {
                        code.loadConstant(match[0]); // service name
                        code.aload(0); // ServiceNode
                        code.loadConstant(graphDepIndex);
                        code.invokevirtual(serviceNodeDesc, "dependencyValue",
                                MethodTypeDesc.of(objectDesc, ConstantDescs.CD_int));
                        graphDepIndex++;
                    }
                    ClassDesc[] paramTypes = new ClassDesc[matches.size() << 1];
                    for (int j = 0; j < matches.size(); j++) {
                        paramTypes[j << 1] = stringDesc;
                        paramTypes[(j << 1) + 1] = objectDesc;
                    }
                    code.invokestatic(CD_SortedNullSafeMap, "of",
                            MethodTypeDesc.of(CD_Map, paramTypes));
                } else {
                    code.loadConstant(matches.size() << 1);
                    code.anewarray(objectDesc);
                    for (int j = 0; j < matches.size(); j++) {
                        code.dup();
                        code.loadConstant(j << 1);
                        code.loadConstant(matches.get(j)[0]); // service name
                        code.aastore();
                        code.dup();
                        code.loadConstant((j << 1) + 1);
                        code.aload(0); // ServiceNode
                        code.loadConstant(graphDepIndex);
                        code.invokevirtual(serviceNodeDesc, "dependencyValue",
                                MethodTypeDesc.of(objectDesc, ConstantDescs.CD_int));
                        graphDepIndex++;
                        code.aastore();
                    }
                    code.invokestatic(CD_SortedNullSafeMap, "ofEntries",
                            MethodTypeDesc.of(CD_Map, objectDesc.arrayType()));
                }
            } else {
                // indexed access: node.dependencyValue(graphDepIndex)
                code.aload(0); // ServiceNode
                code.loadConstant(graphDepIndex);
                code.invokevirtual(serviceNodeDesc, "dependencyValue",
                        MethodTypeDesc.of(objectDesc, ConstantDescs.CD_int));
                graphDepIndex++;
                if (dep.optional()) {
                    // optional dep: wrap in Optional.ofNullable()
                    code.invokestatic(ConstantUtils.classDesc(Optional.class), "ofNullable",
                            MethodTypeDesc.of(ConstantUtils.classDesc(Optional.class), objectDesc));
                } else {
                    ClassDesc depTypeDesc = dep.type().describeConstable().orElseThrow();
                    code.checkcast(depTypeDesc);
                }
            }
            code.astore(resultSlot);
            resultSlot++;
        }

        // inline the action body directly into this deploy method
        // return instructions in the body are replaced with store + goto endLabel
        TypeKind returnKind = TypeKind.from(actionMethodType.returnType());
        Label endLabel = code.newLabel();
        int returnSlot = returnKind != TypeKind.VOID ? resultSlot : -1;

        UnaryOperator<String> remapper = name -> innerPrefix + name;
        MethodTypeDesc deployMtd = MethodTypeDesc.of(ConstantDescs.CD_void, serviceNodeDesc);

        copyMethodBody(action.originalCode(), code,
                action.captureSlotCount(), action.slotToCaptureIndex(),
                action.capturedEmitters(), action.enclosingClassInternal(),
                generatedClassDesc, pendingInnerMethods, remapper,
                classRemapTable, consolidatedClassInternalName, anonCounter,
                deployMethodName, deployMtd,
                endLabel, returnSlot);

        // bind the label — execution reaches here after any return in the inlined body
        code.labelBinding(endLabel);

        // handle result
        if (async) {
            // async: the action already called startComplete/startFailed on the ServiceNode
        } else if (returnKind == TypeKind.VOID) {
            // sync void: signal completion
            code.aload(0); // ServiceNode
            code.invokevirtual(serviceNodeDesc, "startComplete",
                    MethodTypeDesc.of(ConstantDescs.CD_void));
        } else {
            // sync typed: null check, then signal completion with value
            code.aload(returnSlot);
            code.dup();
            code.ifThen(Opcode.IFNULL, b -> {
                b.new_(ConstantUtils.classDesc(IllegalStateException.class));
                b.dup();
                b.loadConstant("Service '" + serviceKey
                        + "' returned null; use a void service if no value is produced");
                b.invokespecial(ConstantUtils.classDesc(IllegalStateException.class),
                        ExtraConstantDescs.INIT_NAME,
                        MethodTypeDesc.of(ConstantDescs.CD_void, stringDesc));
                b.athrow();
            });
            // startComplete(result)
            code.aload(0); // ServiceNode
            code.aload(returnSlot);
            code.invokevirtual(serviceNodeDesc, "startComplete",
                    MethodTypeDesc.of(ConstantDescs.CD_void, objectDesc));

            // also store in the serviceValues map for recorder proxy resolution
            // (legacy recorders resolve service proxies via startupContext.getServiceValue)
            // TODO: remove once all recorder consumers are converted to services
            ClassDesc serviceGraphDesc = ConstantUtils.classDesc(ServiceGraph.class);
            ClassDesc startupContextDesc = ConstantUtils.classDesc(StartupContext.class);
            code.aload(0); // ServiceNode
            code.invokevirtual(CD_ServiceNode, "graph",
                    MethodTypeDesc.of(serviceGraphDesc));
            code.invokevirtual(serviceGraphDesc, "startupContext",
                    MethodTypeDesc.of(startupContextDesc));
            code.loadConstant(serviceKey);
            code.aload(0); // ServiceNode
            code.invokevirtual(CD_ServiceNode, "value",
                    MethodTypeDesc.of(objectDesc));
            code.invokevirtual(startupContextDesc, "putServiceValue",
                    MethodTypeDesc.of(ConstantDescs.CD_void, stringDesc, objectDesc));
        }

        code.return_();
    }

    /**
     * Generate the deploy method body for an {@link TransliteratedAction.AliasService}.
     * <p>
     * The generated code reads a value from the startup context using the recorder
     * proxy key, re-stores it under the service key, and signals completion.
     *
     * @param code the code builder to emit instructions into
     * @param alias the alias service data
     */
    private static void generateAliasDeployBody(
            CodeBuilder code,
            TransliteratedAction.AliasService alias) {
        // slot 0 = ServiceNode (static method)
        ClassDesc serviceNodeDesc = ConstantUtils.classDesc(ServiceNode.class);
        ClassDesc serviceGraphDesc = ConstantUtils.classDesc(ServiceGraph.class);
        ClassDesc startupContextDesc = ConstantUtils.classDesc(StartupContext.class);
        ClassDesc objectDesc = ConstantUtils.classDesc(Object.class);
        ClassDesc stringDesc = ConstantUtils.classDesc(String.class);

        // get StartupContext: node.graph().startupContext()
        code.aload(0); // ServiceNode
        code.invokevirtual(serviceNodeDesc, "graph",
                MethodTypeDesc.of(serviceGraphDesc));
        code.invokevirtual(serviceGraphDesc, "startupContext",
                MethodTypeDesc.of(startupContextDesc));
        int ctxSlot = 1;
        code.astore(ctxSlot);

        // load the value from the recorder proxy key
        code.aload(ctxSlot);
        code.loadConstant(alias.recorderProxyKey());
        code.invokevirtual(startupContextDesc, "getValue",
                MethodTypeDesc.of(objectDesc, stringDesc));
        int valueSlot = 2;
        code.astore(valueSlot);

        // store it under the service key in the service values map
        code.aload(ctxSlot);
        code.loadConstant(alias.serviceKey());
        code.aload(valueSlot);
        code.invokevirtual(startupContextDesc, "putServiceValue",
                MethodTypeDesc.of(ConstantDescs.CD_void, stringDesc, objectDesc));

        // signal completion: typed if value is non-null, void otherwise
        code.aload(valueSlot);
        Label nullValue = code.newLabel();
        code.ifnull(nullValue);
        code.aload(0); // ServiceNode
        code.aload(valueSlot);
        code.invokevirtual(serviceNodeDesc, "startComplete",
                MethodTypeDesc.of(ConstantDescs.CD_void, objectDesc));
        code.return_();
        code.labelBinding(nullValue);
        // diagnostic: log when alias reads null from values map
        ClassDesc loggerDesc = ConstantUtils.classDesc(Logger.class);
        code.loadConstant("io.quarkus.service.alias");
        code.invokestatic(loggerDesc, "getLogger",
                MethodTypeDesc.of(loggerDesc, stringDesc));
        code.loadConstant("Alias '" + alias.serviceKey()
                + "' read null from values map (recorder proxy key: " + alias.recorderProxyKey() + ")");
        code.invokevirtual(loggerDesc, "warn",
                MethodTypeDesc.of(ConstantDescs.CD_void, objectDesc));
        code.aload(0); // ServiceNode
        code.invokevirtual(serviceNodeDesc, "startComplete",
                MethodTypeDesc.of(ConstantDescs.CD_void));
        code.return_();
    }

    /**
     * Generate the deploy method body for a {@link TransliteratedAction.RuntimeValueWrapper}.
     * <p>
     * The generated code reads the bare service value from the startup context
     * using the source service key, wraps it in a new {@code RuntimeValue},
     * stores the wrapper under the rv key, and signals completion.
     *
     * @param code the code builder to emit instructions into
     */
    private static void generateRuntimeValueWrapperDeployBody(
            CodeBuilder code,
            TransliteratedAction.RuntimeValueWrapper rvw) {
        // slot 0 = ServiceNode (static method)
        ClassDesc objectDesc = ConstantUtils.classDesc(Object.class);
        ClassDesc runtimeValueDesc = ConstantUtils.classDesc(RuntimeValue.class);

        // load the source service value via indexed dependency access (dep 0)
        code.aload(0); // ServiceNode
        code.loadConstant(0);
        code.invokevirtual(CD_ServiceNode, "dependencyValue",
                MethodTypeDesc.of(objectDesc, ConstantDescs.CD_int));
        int valueSlot = 1;
        code.astore(valueSlot);

        // wrap in new RuntimeValue<>(value)
        code.new_(runtimeValueDesc);
        code.dup();
        code.aload(valueSlot);
        code.invokespecial(runtimeValueDesc,
                ExtraConstantDescs.INIT_NAME,
                MethodTypeDesc.of(ConstantDescs.CD_void, objectDesc));
        int rvSlot = valueSlot + 1;
        code.astore(rvSlot);

        // store in the serviceValues map for recorder proxy resolution
        // (legacy recorders resolve service proxies via startupContext.getServiceValue
        //  because __service$$value() returns true on the proxy)
        ClassDesc serviceGraphDesc = ConstantUtils.classDesc(ServiceGraph.class);
        ClassDesc startupContextDesc = ConstantUtils.classDesc(StartupContext.class);
        ClassDesc stringDesc = ConstantUtils.classDesc(String.class);
        code.aload(0); // ServiceNode
        code.invokevirtual(CD_ServiceNode, "graph",
                MethodTypeDesc.of(serviceGraphDesc));
        code.invokevirtual(serviceGraphDesc, "startupContext",
                MethodTypeDesc.of(startupContextDesc));
        code.loadConstant(rvw.rvKey());
        code.aload(rvSlot);
        code.invokevirtual(startupContextDesc, "putServiceValue",
                MethodTypeDesc.of(ConstantDescs.CD_void, stringDesc, objectDesc));

        // signal typed completion with the RuntimeValue wrapper
        code.aload(0); // ServiceNode
        code.aload(rvSlot);
        code.invokevirtual(CD_ServiceNode, "startComplete",
                MethodTypeDesc.of(ConstantDescs.CD_void, objectDesc));

        code.return_();
    }

    /**
     * Identifies a method in the original enclosing class that must be copied
     * into the generated class because it is referenced by an {@code invokedynamic}
     * for a nested lambda or method reference.
     *
     * @param name the original method name in the enclosing class
     * @param descriptor the method descriptor
     * @param remappedName the prefixed name to use in the generated class
     */
    private record InnerMethod(String name, String descriptor, String remappedName) {
    }

    /**
     * Tracks a remapped anonymous inner class: its new internal name in the
     * consolidated class space and the enclosing method in the consolidated class
     * (needed to rewrite the {@code EnclosingMethod} attribute).
     *
     * @param newInternalName the new internal name for the anonymous class copy
     * @param enclosingMethodName the name of the method in the consolidated class that contains the {@code NEW} instruction
     * @param enclosingMethodType the type of the enclosing method in the consolidated class
     */
    private record AnonClassEntry(String newInternalName,
            String enclosingMethodName, MethodTypeDesc enclosingMethodType) {
    }

    /**
     * Build the serial context with custom serializers for constants and config mappings.
     */
    private static SerialContext buildSerialContext() {
        return SerialContext.builder()
                .addDefaultProviders()
                .addSerializer(ConstantDescSerializer.INSTANCE)
                .addSerializer(ConfigMappingSerializer.INSTANCE)
                .build();
    }

    private static final String LAMBDA_METAFACTORY_INTERNAL = "java/lang/invoke/LambdaMetafactory";

    /**
     * If the given {@code invokedynamic} instruction is a {@link LambdaMetafactory}
     * call whose implementation method resides in the specified enclosing class,
     * return the implementation {@link MethodHandleEntry}. Otherwise return {@code null}.
     *
     * @param idi the invokedynamic instruction
     * @param enclosingClassInternal the internal name of the enclosing class
     * @return the impl method handle, or {@code null} if not a rewritable lambda factory
     */
    private static MethodHandleEntry extractLambdaImplHandle(
            InvokeDynamicInstruction idi,
            String enclosingClassInternal) {
        InvokeDynamicEntry ide = idi.invokedynamic();
        BootstrapMethodEntry bsm = ide.bootstrap();
        MethodHandleEntry bsmHandle = bsm.bootstrapMethod();
        MemberRefEntry bsmRef = bsmHandle.reference();
        String bsmOwner = bsmRef.owner().asInternalName();
        if (!bsmOwner.equals(LAMBDA_METAFACTORY_INTERNAL)) {
            return null;
        }
        String bsmName = bsmRef.nameAndType().name().stringValue();
        if (!bsmName.equals("metafactory") && !bsmName.equals("altMetafactory")) {
            return null;
        }
        List<LoadableConstantEntry> args = bsm.arguments();
        if (args.size() < 2 || !(args.get(1) instanceof MethodHandleEntry implHandle)) {
            return null;
        }
        String implOwner = implHandle.reference().owner().asInternalName();
        if (!implOwner.equals(enclosingClassInternal)) {
            return null;
        }
        return implHandle;
    }

    /**
     * Rewrite a lambda factory {@code invokedynamic} instruction so that its
     * implementation method handle points to the generated class with a remapped
     * method name instead of the original enclosing class and name.
     *
     * @param code the code builder to emit the rewritten instruction into
     * @param idi the original invokedynamic instruction
     * @param generatedClassDesc the class descriptor of the generated class
     * @param remappedMethodName the prefixed method name to use in the generated class
     */
    private static void rewriteInvokeDynamic(
            CodeBuilder code,
            InvokeDynamicInstruction idi,
            ClassDesc generatedClassDesc,
            String remappedMethodName) {
        InvokeDynamicEntry oldEntry = idi.invokedynamic();
        BootstrapMethodEntry oldBsm = oldEntry.bootstrap();
        List<LoadableConstantEntry> oldArgs = oldBsm.arguments();
        MethodHandleEntry oldImplHandle = (MethodHandleEntry) oldArgs.get(1);
        MemberRefEntry oldImplRef = oldImplHandle.reference();

        ConstantPoolBuilder cpb = code.constantPool();
        // remap context interface types to ServiceNode in the impl method descriptor
        MethodTypeDesc originalImplType = MethodTypeDesc.ofDescriptor(
                oldImplRef.nameAndType().type().stringValue());
        MethodTypeDesc remappedImplType = remapContextTypes(originalImplType);
        var newNameAndType = cpb.nameAndTypeEntry(
                cpb.utf8Entry(remappedMethodName),
                cpb.utf8Entry(remappedImplType.descriptorString()));
        var newMethodRef = cpb.methodRefEntry(
                cpb.classEntry(generatedClassDesc),
                newNameAndType);
        MethodHandleEntry newImplHandle = cpb.methodHandleEntry(oldImplHandle.kind(), newMethodRef);

        // rebuild bootstrap arguments with the new impl handle and remapped instantiated type
        List<LoadableConstantEntry> newArgs = new ArrayList<>(oldArgs);
        newArgs.set(1, newImplHandle);
        // args[2] is the instantiated method type — remap context types there too
        if (newArgs.size() > 2 && newArgs.get(2) instanceof MethodTypeEntry mte) {
            MethodTypeDesc instantiated = MethodTypeDesc.ofDescriptor(mte.descriptor().stringValue());
            MethodTypeDesc remappedInstantiated = remapContextTypes(instantiated);
            if (remappedInstantiated != instantiated) {
                newArgs.set(2, cpb.methodTypeEntry(remappedInstantiated));
            }
        }

        BootstrapMethodEntry newBsm = cpb.bsmEntry(oldBsm.bootstrapMethod(), newArgs);
        // remap context types in the factory signature (captured parameter types)
        MethodTypeDesc factoryType = MethodTypeDesc.ofDescriptor(
                oldEntry.nameAndType().type().stringValue());
        MethodTypeDesc remappedFactoryType = remapContextTypes(factoryType);
        var factoryNameAndType = remappedFactoryType != factoryType
                ? cpb.nameAndTypeEntry(oldEntry.nameAndType().name(), cpb.utf8Entry(remappedFactoryType.descriptorString()))
                : oldEntry.nameAndType();
        InvokeDynamicEntry newEntry = cpb.invokeDynamicEntry(newBsm, factoryNameAndType);
        code.invokedynamic(newEntry);
    }

    /**
     * Copy a method body from the original class into the generated class,
     * optionally remapping captured parameter slots to constants, rewriting
     * nested lambda {@code invokedynamic} instructions, and renaming inner
     * method references to avoid collisions in consolidated classes.
     * <p>
     * For the top-level action method, {@code captureSlotCount} is the number of
     * leading local variable slots consumed by captured parameters (which are
     * replaced by constant loads). For inner lambda methods, pass 0 (no captures).
     *
     * @param originalCode the source code model to copy
     * @param code the target code builder
     * @param captureSlotCount the number of leading slots that are captures (0 for inner methods)
     * @param slotToCaptureIndex mapping from slot index to capture argument index ({@code -1} for non-start slots)
     * @param capturedEmitters the emitters for captured arguments
     * @param enclosingClassInternal the internal name of the original enclosing class
     * @param generatedClassDesc the class descriptor of the generated class
     * @param pendingInnerMethods set to which newly-discovered inner lambda methods are added
     * @param innerMethodNameRemapper transforms inner method names to prefixed form for collision avoidance
     * @param classRemapTable shared map tracking anonymous inner class remappings (original name → entry)
     * @param consolidatedClassInternalName the internal name of the consolidated class being built
     * @param anonCounter shared counter for generating unique anonymous class names
     * @param currentMethodName the name of the method being written in the consolidated class
     * @param currentMethodType the type descriptor of the method being written in the consolidated class
     * @param inlineEndLabel if non-null, return instructions are replaced with jumps to this label
     *        (for inlining the action body into the deploy method). If a value return, the value
     *        is stored in {@code inlineReturnSlot} before the jump.
     * @param inlineReturnSlot the local variable slot to store the return value when inlining
     *        (only meaningful when {@code inlineEndLabel} is non-null and the return is non-void)
     */
    private static void copyMethodBody(
            CodeModel originalCode,
            CodeBuilder code,
            int captureSlotCount,
            int[] slotToCaptureIndex,
            CaptureEmitter[] capturedEmitters,
            String enclosingClassInternal,
            ClassDesc generatedClassDesc,
            Set<InnerMethod> pendingInnerMethods,
            UnaryOperator<String> innerMethodNameRemapper,
            Map<String, AnonClassEntry> classRemapTable,
            String consolidatedClassInternalName,
            int[] anonCounter,
            String currentMethodName,
            MethodTypeDesc currentMethodType,
            Label inlineEndLabel,
            int inlineReturnSlot) {
        // when captures are removed, local variable slots are shifted down by captureSlotCount;
        // reserve the remapped range so that CodeBuilder.allocateLocal() (used by DedupEmitter
        // for captured value deduplication) does not return slots that conflict with remapped locals
        if (captureSlotCount > 0) {
            int maxOriginalSlot = 0;
            for (CodeElement scan : originalCode) {
                if (scan instanceof LoadInstruction li) {
                    maxOriginalSlot = Math.max(maxOriginalSlot, li.slot());
                } else if (scan instanceof StoreInstruction si) {
                    maxOriginalSlot = Math.max(maxOriginalSlot, si.slot());
                }
            }
            int remappedMax = maxOriginalSlot - captureSlotCount;
            // advance allocateLocal past the remapped local range
            while (code.allocateLocal(TypeKind.REFERENCE) < remappedMax) {
                // allocateLocal advances the internal counter; loop until past the range
            }
        }

        for (CodeElement el : originalCode) {
            if (el instanceof LoadInstruction li) {
                int slot = li.slot();
                if (slot < captureSlotCount) {
                    int idx = slotToCaptureIndex[slot];
                    if (idx < 0) {
                        throw new TransliterationException(
                                "Load instruction references middle of a wide capture slot " + slot);
                    }
                    capturedEmitters[idx].emit(code);
                } else {
                    code.loadLocal(li.typeKind(), slot - captureSlotCount);
                }
            } else if (el instanceof StoreInstruction si) {
                int slot = si.slot();
                if (slot < captureSlotCount) {
                    throw new TransliterationException(
                            "Store to captured variable slot " + slot + " is not allowed");
                }
                code.storeLocal(si.typeKind(), slot - captureSlotCount);
            } else if (el instanceof IncrementInstruction ii) {
                int slot = ii.slot();
                if (slot < captureSlotCount) {
                    throw new TransliterationException(
                            "Increment of captured variable slot " + slot + " is not allowed");
                }
                code.iinc(slot - captureSlotCount, ii.constant());
            } else if (el instanceof LineNumber
                    || el instanceof LocalVariable
                    || el instanceof LocalVariableType) {
                // drop debug info
            } else if (el instanceof InvokeDynamicInstruction idi) {
                MethodHandleEntry implHandle = extractLambdaImplHandle(idi, enclosingClassInternal);
                if (implHandle != null) {
                    MemberRefEntry implRef = implHandle.reference();
                    String originalName = implRef.nameAndType().name().stringValue();
                    String remappedName = innerMethodNameRemapper.apply(originalName);
                    pendingInnerMethods.add(new InnerMethod(
                            originalName,
                            implRef.nameAndType().type().stringValue(),
                            remappedName));
                    rewriteInvokeDynamic(code, idi, generatedClassDesc, remappedName);
                } else {
                    code.with(el);
                }
            } else if (el instanceof NewObjectInstruction noi) {
                String className = noi.className().asInternalName();
                AnonClassEntry entry = remapIfInnerClass(className, enclosingClassInternal,
                        classRemapTable, consolidatedClassInternalName, anonCounter,
                        currentMethodName, currentMethodType);
                if (entry != null) {
                    code.new_(ExtraClassDesc.ofInternalName(entry.newInternalName()));
                } else {
                    code.with(el);
                }
            } else if (el instanceof InvokeInstruction ii) {
                String ownerName = ii.owner().asInternalName();
                if (ii.opcode() == Opcode.INVOKEINTERFACE && CONTEXT_INTERFACE_INTERNALS.contains(ownerName)) {
                    // rewrite invokeinterface on context interfaces to invokevirtual on ServiceNode
                    ConstantPoolBuilder cpb = code.constantPool();
                    var newRef = cpb.methodRefEntry(
                            cpb.classEntry(CD_ServiceNode),
                            ii.method().nameAndType());
                    code.invoke(Opcode.INVOKEVIRTUAL, newRef);
                } else {
                    AnonClassEntry entry = classRemapTable.get(ownerName);
                    if (entry != null) {
                        ConstantPoolBuilder cpb = code.constantPool();
                        var newRef = cpb.methodRefEntry(
                                cpb.classEntry(ExtraClassDesc.ofInternalName(entry.newInternalName())),
                                ii.method().nameAndType());
                        code.invoke(ii.opcode(), newRef);
                    } else {
                        code.with(el);
                    }
                }
            } else if (el instanceof TypeCheckInstruction tci) {
                String typeName = tci.type().asInternalName();
                AnonClassEntry entry = classRemapTable.get(typeName);
                if (entry != null) {
                    ConstantPoolBuilder cpb = code.constantPool();
                    code.with(TypeCheckInstruction.of(tci.opcode(),
                            cpb.classEntry(ExtraClassDesc.ofInternalName(entry.newInternalName()))));
                } else {
                    code.with(el);
                }
            } else if (el instanceof ReturnInstruction ri && inlineEndLabel != null) {
                // inline mode: replace return with store (if value) + goto endLabel
                TypeKind tk = ri.typeKind();
                if (tk != TypeKind.VOID) {
                    code.storeLocal(tk, inlineReturnSlot);
                }
                code.goto_(inlineEndLabel);
            } else {
                code.with(el);
            }
        }
    }

    /**
     * If the given class name is an inner class of the enclosing deployment class,
     * register it in the remap table (assigning a new name under the consolidated
     * class) and return the mapping entry. Returns {@code null} if the class is not
     * an inner class of the enclosing class.
     *
     * @param className the internal name of the class to check
     * @param enclosingClassInternal the internal name of the original enclosing class
     * @param classRemapTable the shared remap table to populate
     * @param consolidatedClassInternalName the internal name of the consolidated class
     * @param anonCounter shared counter for unique naming
     * @param currentMethodName the enclosing method name in the consolidated class
     * @param currentMethodType the enclosing method type in the consolidated class
     * @return the remap entry, or {@code null} if not an inner class
     */
    private static AnonClassEntry remapIfInnerClass(
            String className,
            String enclosingClassInternal,
            Map<String, AnonClassEntry> classRemapTable,
            String consolidatedClassInternalName,
            int[] anonCounter,
            String currentMethodName,
            MethodTypeDesc currentMethodType) {
        if (className.startsWith(enclosingClassInternal + "$")) {
            return classRemapTable.computeIfAbsent(className, k -> new AnonClassEntry(
                    consolidatedClassInternalName + "$" + ++anonCounter[0],
                    currentMethodName, currentMethodType));
        }
        return null;
    }

    /**
     * Copy an anonymous inner class, remapping its name and fixing its
     * {@code NestHost}, {@code InnerClasses}, and {@code EnclosingMethod}
     * attributes to point to the consolidated class.
     * <p>
     * Pass 1 uses {@link ClassRemapper} to remap the class name and all
     * internal references (signatures, descriptors, instructions).
     * Pass 2 fixes the three structural attributes that ClassRemapper cannot
     * handle correctly because the original enclosing class is not in the
     * remap map.
     *
     * @param originalInternalName the internal name of the original anonymous class
     * @param mapping the remap entry with new name and enclosing method info
     * @param descMap the ClassDesc remap map (original → new) for ClassRemapper
     * @param consolidatedClassDesc the class descriptor of the consolidated class
     * @return the transformed class file bytes
     */
    private static byte[] copyInnerClass(
            String originalInternalName,
            AnonClassEntry mapping,
            Map<ClassDesc, ClassDesc> descMap,
            ClassDesc consolidatedClassDesc) {
        byte[] originalBytes = loadClassBytes(originalInternalName);
        ClassModel cm = ClassFile.of().parse(originalBytes);

        // Pass 1: remap all class references including the class name
        byte[] remapped = ClassRemapper.of(descMap).remapClass(ClassFile.of(), cm);

        // Pass 2: fix nest/inner class/enclosing method attributes
        ClassModel remappedModel = ClassFile.of().parse(remapped);
        ClassDesc newClassDesc = remappedModel.thisClass().asSymbol();
        return ClassFile.of().transformClass(remappedModel, (cb, ce) -> {
            if (ce instanceof NestHostAttribute) {
                cb.with(NestHostAttribute.of(consolidatedClassDesc));
            } else if (ce instanceof InnerClassesAttribute) {
                cb.with(InnerClassesAttribute.of(
                        InnerClassInfo.of(newClassDesc,
                                Optional.empty(), Optional.empty(), 0)));
            } else if (ce instanceof EnclosingMethodAttribute) {
                cb.with(EnclosingMethodAttribute.of(
                        consolidatedClassDesc,
                        Optional.of(mapping.enclosingMethodName()),
                        Optional.of(mapping.enclosingMethodType())));
            } else {
                cb.with(ce);
            }
        });
    }

    /**
     * Extract the {@link SerializedLambda} from a serializable lambda instance
     * by invoking its compiler-generated {@code writeReplace()} method.
     *
     * @param lambda the lambda instance
     * @return the serialized lambda metadata
     * @throws TransliterationException if extraction fails
     */
    private static SerializedLambda extractSerializedLambda(Serializable lambda) {
        try {
            Method writeReplace = lambda.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            return (SerializedLambda) writeReplace.invoke(lambda);
        } catch (NoSuchMethodException e) {
            throw new TransliterationException(
                    "Lambda class " + lambda.getClass().getName()
                            + " does not have writeReplace() — is it a serializable lambda?",
                    e);
        } catch (ReflectiveOperationException e) {
            throw new TransliterationException(
                    "Failed to extract SerializedLambda from " + lambda.getClass().getName(), e);
        }
    }

    /**
     * Load the bytecode of a class by its internal name from the current thread's context classloader.
     *
     * @param internalName the class internal name (using {@code /} separators)
     * @return the class file bytes
     * @throws TransliterationException if the class cannot be loaded
     */
    private static byte[] loadClassBytes(String internalName) {
        String resourceName = internalName + ".class";
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try (InputStream is = cl.getResourceAsStream(resourceName)) {
            if (is == null) {
                throw new TransliterationException(
                        "Cannot load class bytecode for " + internalName.replace('/', '.')
                                + " from context classloader");
            }
            return is.readAllBytes();
        } catch (IOException e) {
            throw new TransliterationException(
                    "Failed to read class bytecode for " + internalName.replace('/', '.'), e);
        }
    }

    /**
     * Compute the service key used to store/retrieve values in the {@link StartupContext}.
     * NOTE: This is only temporary, until bytecode recording has been eliminated.
     *
     * @param type the service type
     * @param nameParts the service name parts (must not be {@code null})
     * @return the key string
     */
    public static String serviceKey(Class<?> type, List<String> nameParts) {
        return type.getName() + ":" + String.join("/", nameParts);
    }

    /**
     * Thrown when lambda transliteration fails due to an unsupported construct,
     * invalid capture, or bytecode processing error.
     */
    public static final class TransliterationException extends RuntimeException {
        /**
         * Construct a new instance.
         *
         * @param message the error message
         */
        public TransliterationException(String message) {
            super(message);
        }

        /**
         * Construct a new instance.
         *
         * @param message the error message
         * @param cause the cause
         */
        public TransliterationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
