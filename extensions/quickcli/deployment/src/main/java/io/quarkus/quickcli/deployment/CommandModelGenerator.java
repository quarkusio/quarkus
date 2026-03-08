package io.quarkus.quickcli.deployment;

import java.lang.constant.ClassDesc;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.This;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.creator.ClassCreator;
import io.quarkus.gizmo2.desc.ClassMethodDesc;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.MethodDesc;
import io.quarkus.quickcli.CommandSpec;
import io.quarkus.quickcli.ScopeType;
import io.quarkus.quickcli.model.BuiltCommandModel;
import io.quarkus.quickcli.model.CommandModelRegistry;
import io.quarkus.quickcli.model.FieldAccessor;

/**
 * Generates {@code *_QuickCliModel} classes using Gizmo2 at Quarkus build time.
 * <p>
 * For each {@code @Command} class, generates a single class that:
 * <ul>
 * <li>Implements {@link FieldAccessor} (dispatches by field index)</li>
 * <li>Implements {@link Supplier} (creates command instances)</li>
 * <li>Has a static initializer that builds and registers a {@link BuiltCommandModel}</li>
 * </ul>
 */
class CommandModelGenerator {

    private static final DotName COMMAND = DotName.createSimple("io.quarkus.quickcli.annotations.Command");
    private static final DotName OPTION = DotName.createSimple("io.quarkus.quickcli.annotations.Option");
    private static final DotName PARAMETERS = DotName.createSimple("io.quarkus.quickcli.annotations.Parameters");
    private static final DotName MIXIN = DotName.createSimple("io.quarkus.quickcli.annotations.Mixin");
    private static final DotName ARG_GROUP = DotName.createSimple("io.quarkus.quickcli.annotations.ArgGroup");
    private static final DotName SPEC = DotName.createSimple("io.quarkus.quickcli.annotations.Spec");
    private static final DotName PARENT_COMMAND = DotName.createSimple("io.quarkus.quickcli.annotations.ParentCommand");
    private static final DotName UNMATCHED = DotName.createSimple("io.quarkus.quickcli.annotations.Unmatched");

    private static final DotName LIST = DotName.createSimple(java.util.List.class.getName());
    private static final DotName SET = DotName.createSimple(java.util.Set.class.getName());
    private static final DotName MAP = DotName.createSimple(java.util.Map.class.getName());
    private static final DotName OPTIONAL = DotName.createSimple(Optional.class.getName());

    private final IndexView index;

    CommandModelGenerator(IndexView index) {
        this.index = index;
    }

    void generateAll(BuildProducer<GeneratedClassBuildItem> generatedClass) {
        Set<DotName> generated = new HashSet<>();
        for (AnnotationInstance ann : index.getAnnotations(COMMAND)) {
            if (ann.target().kind() != AnnotationTarget.Kind.CLASS) {
                continue;
            }
            ClassInfo classInfo = ann.target().asClass();
            if (generated.add(classInfo.name())) {
                generate(classInfo, generatedClass);
            }
        }
    }

    private void generate(ClassInfo commandClass, BuildProducer<GeneratedClassBuildItem> generatedClass) {
        String commandClassName = commandClass.name().toString();
        String modelClassName = commandClassName + "_QuickCliModel";
        String commandPkg = packageOf(commandClassName);

        // Collect all field metadata
        CommandData data = collectCommandData(commandClass);

        io.quarkus.gizmo2.ClassOutput classOutput = createClassOutput(generatedClass);

        // Generate accessor classes in cross-package superclass packages.
        // This avoids reflection: each accessor is in the same package as the fields it accesses.
        List<FieldSetInfo> allFields = collectAllFieldSetInfos(data, commandClassName);
        Map<String, String> accessorClassNames = generateCrossPackageAccessors(
                allFields, commandClassName, commandPkg, classOutput);

        Gizmo gizmo = Gizmo.create(classOutput);
        gizmo.class_(modelClassName, cc -> {
            cc.implements_(FieldAccessor.class);
            cc.implements_(Supplier.class);

            This this_ = cc.this_();

            // Field: int fieldIndex
            FieldDesc fieldIndexField = cc.field("fieldIndex", fc -> {
                fc.private_();
                fc.final_();
                fc.setType(int.class);
            });

            // Constructor(int fieldIndex)
            cc.constructor(mc -> {
                var fieldIndex = mc.parameter("fieldIndex", int.class);
                mc.body(bc -> {
                    bc.invokeSpecial(ConstructorDesc.of(Object.class), this_);
                    bc.set(this_.field(fieldIndexField), fieldIndex);
                    bc.return_();
                });
            });

            // Supplier.get() -> new CommandClass()
            cc.method("get", mc -> {
                mc.public_();
                mc.returning(Object.class);
                mc.body(bc -> bc.return_(bc.new_(ClassDesc.of(commandClassName))));
            });

            // FieldAccessor.set(Object instance, Object value) - dispatch by fieldIndex
            generateSetMethod(cc, this_, fieldIndexField, commandClassName, data);

            // Static initializer: build and register the model
            generateStaticInitializer(cc, modelClassName, commandClassName, data, accessorClassNames);
        });
    }

    private static io.quarkus.gizmo2.ClassOutput createClassOutput(
            BuildProducer<GeneratedClassBuildItem> generatedClass) {
        return (resourceName, bytes) -> {
            if (resourceName.endsWith(".class")) {
                String cn = resourceName.substring(0, resourceName.length() - 6).replace('/', '.');
                generatedClass.produce(new GeneratedClassBuildItem(true, cn, bytes));
            }
        };
    }

    // --- Field metadata collection via Jandex ---

    private CommandData collectCommandData(ClassInfo commandClass) {
        AnnotationInstance cmdAnn = commandClass.annotation(COMMAND);
        CommandData data = new CommandData();
        data.commandClassName = commandClass.name().toString();

        // @Command attributes
        data.name = annotationString(cmdAnn, "name", "");
        if (data.name.isEmpty()) {
            data.name = commandClass.simpleName().toLowerCase();
        }
        data.description = annotationStringArray(cmdAnn, "description");
        data.version = annotationStringArray(cmdAnn, "version");
        data.mixinStandardHelpOptions = annotationBool(cmdAnn, "mixinStandardHelpOptions", false);
        data.header = annotationStringArray(cmdAnn, "header");
        data.footer = annotationStringArray(cmdAnn, "footer");
        data.scope = annotationEnum(cmdAnn, "scope", ScopeType.class, ScopeType.LOCAL);
        data.aliases = annotationStringArray(cmdAnn, "aliases");
        data.showDefaultValues = annotationBool(cmdAnn, "showDefaultValues", false);
        data.commandListHeading = annotationString(cmdAnn, "commandListHeading", "Commands:%n");
        data.synopsisHeading = annotationString(cmdAnn, "synopsisHeading", "Usage: ");
        data.optionListHeading = annotationString(cmdAnn, "optionListHeading", "Options:%n");
        data.headerHeading = annotationString(cmdAnn, "headerHeading", "");
        data.parameterListHeading = annotationString(cmdAnn, "parameterListHeading", "%n");

        // Version provider
        AnnotationValue vpValue = cmdAnn.value("versionProvider");
        if (vpValue != null) {
            data.versionProviderClassName = vpValue.asClass().name().toString();
        }

        // Subcommands
        AnnotationValue subValue = cmdAnn.value("subcommands");
        if (subValue != null) {
            for (Type t : subValue.asClassArray()) {
                data.subcommandClassNames.add(t.name().toString());
            }
        }

        int fieldIndex = 0;

        // Scan this class for annotated fields
        fieldIndex = scanClassFields(commandClass, data, null, fieldIndex, false);

        // Scan superclasses
        String commandPkg = packageOf(commandClass.name().toString());
        Type superType = commandClass.superClassType();
        while (superType != null && !superType.name().equals(DotName.createSimple("java.lang.Object"))) {
            ClassInfo superClass = index.getClassByName(superType.name());
            if (superClass == null) {
                break;
            }
            boolean crossPackage = !commandPkg.equals(packageOf(superClass.name().toString()));
            fieldIndex = scanClassFields(superClass, data, superClass.name().toString(), fieldIndex, crossPackage);
            superType = superClass.superClassType();
        }

        // Sort parameters by explicit index so help output and value assignment are correct
        data.parameters.sort(java.util.Comparator.comparingInt(p -> p.paramIndex));

        return data;
    }

    private int scanClassFields(ClassInfo classInfo, CommandData data, String declaringClass,
            int fieldIndex, boolean crossPackage) {

        for (FieldInfo field : classInfo.fields()) {
            AnnotationInstance optionAnn = field.annotation(OPTION);
            AnnotationInstance paramsAnn = field.annotation(PARAMETERS);
            AnnotationInstance mixinAnn = field.annotation(MIXIN);
            AnnotationInstance argGroupAnn = field.annotation(ARG_GROUP);
            AnnotationInstance specAnn = field.annotation(SPEC);
            AnnotationInstance parentAnn = field.annotation(PARENT_COMMAND);
            AnnotationInstance unmatchedAnn = field.annotation(UNMATCHED);

            if (optionAnn == null && paramsAnn == null && mixinAnn == null
                    && argGroupAnn == null && specAnn == null && parentAnn == null
                    && unmatchedAnn == null) {
                continue;
            }

            boolean isPublic = Modifier.isPublic(field.flags());
            // Reflection fallback is needed for non-public fields on cross-package superclasses.
            // Private fields on the command class itself are bytecode-transformed to package-private,
            // so PUTFIELD works without reflection. Same approach as Arc.
            boolean needsReflection = crossPackage && !isPublic;
            String fieldTypeName = fieldTypeClassName(field.type());
            BuiltCommandModel.FieldKind fieldKind = detectFieldKind(field.type());
            String componentTypeName = detectComponentType(field.type());
            String optionalInnerTypeName = fieldKind == BuiltCommandModel.FieldKind.OPTIONAL
                    ? detectOptionalInnerType(field.type())
                    : null;

            if (optionAnn != null) {
                OptionData od = createOptionData(optionAnn, fieldIndex++, field.name(),
                        fieldTypeName, fieldKind, componentTypeName, optionalInnerTypeName,
                        needsReflection, declaringClass, null, null);
                data.options.add(od);
            }

            if (paramsAnn != null) {
                ParameterData pd = createParameterData(paramsAnn, fieldIndex++, field.name(),
                        fieldTypeName, fieldKind, componentTypeName,
                        needsReflection, declaringClass, null, null, data);
                data.parameters.add(pd);
            }

            if (mixinAnn != null) {
                MixinData md = new MixinData();
                md.fieldIndex = fieldIndex++;
                md.fieldName = field.name();
                md.mixinTypeName = fieldTypeName;
                md.isPrivate = needsReflection;
                md.declaringClassName = declaringClass;
                data.mixins.add(md);

                // Scan mixin type for @Option and @Parameters fields
                ClassInfo mixinClass = index.getClassByName(field.type().name());
                if (mixinClass != null) {
                    fieldIndex = scanMixinOrArgGroupFields(mixinClass, data, field.name(),
                            fieldTypeName, fieldIndex, false);

                    // Check for @Spec(MIXEE) fields in the mixin
                    for (FieldInfo mixinField : mixinClass.fields()) {
                        AnnotationInstance mixinSpec = mixinField.annotation(SPEC);
                        if (mixinSpec != null) {
                            AnnotationValue targetValue = mixinSpec.value("value");
                            String target = targetValue != null ? targetValue.asEnum() : "SELF";
                            if ("MIXEE".equals(target)) {
                                MixeeSpecData msd = new MixeeSpecData();
                                msd.fieldIndex = fieldIndex++;
                                msd.mixinFieldName = field.name();
                                msd.mixinTypeName = fieldTypeName;
                                msd.specFieldName = mixinField.name();
                                msd.isPrivate = Modifier.isPrivate(mixinField.flags());
                                data.mixeeSpecs.add(msd);
                            }
                        }
                    }
                } else {
                    throw new IllegalStateException(
                            "Mixin class " + fieldTypeName + " is not in the Jandex index. "
                                    + "Add an IndexDependencyBuildItem for the library containing this class.");
                }
            }

            if (argGroupAnn != null) {
                ArgGroupData agd = new ArgGroupData();
                agd.fieldIndex = fieldIndex++;
                agd.fieldName = field.name();
                agd.argGroupTypeName = fieldTypeName;
                agd.isPrivate = needsReflection;
                agd.declaringClassName = declaringClass;
                data.argGroups.add(agd);

                ClassInfo argGroupClass = index.getClassByName(field.type().name());
                if (argGroupClass != null) {
                    boolean exclusive = annotationBool(argGroupAnn, "exclusive", true);
                    if (exclusive) {
                        // Collect exclusive group option names
                        List<String> groupOptionNames = new ArrayList<>();
                        for (FieldInfo agField : argGroupClass.fields()) {
                            AnnotationInstance agOption = agField.annotation(OPTION);
                            if (agOption != null) {
                                String[] names = annotationStringArray(agOption, "names");
                                if (names.length > 0) {
                                    groupOptionNames.add(names[names.length - 1]); // longest name
                                }
                            }
                        }
                        if (!groupOptionNames.isEmpty()) {
                            data.exclusiveGroups.add(groupOptionNames);
                        }
                    }
                    fieldIndex = scanMixinOrArgGroupFields(argGroupClass, data, field.name(),
                            fieldTypeName, fieldIndex, false);
                } else {
                    throw new IllegalStateException(
                            "ArgGroup class " + fieldTypeName + " is not in the Jandex index. "
                                    + "Add an IndexDependencyBuildItem for the library containing this class.");
                }
            }

            if (specAnn != null) {
                String target = "SELF";
                AnnotationValue targetVal = specAnn.value();
                if (targetVal != null) {
                    target = targetVal.asString();
                }
                if ("SELF".equals(target)) {
                    data.specFieldIndex = fieldIndex++;
                    data.specFieldName = field.name();
                    data.specIsPrivate = needsReflection;
                    data.specDeclaringClassName = declaringClass;
                }
            }

            if (parentAnn != null) {
                data.parentCommandFieldIndex = fieldIndex++;
                data.parentCommandFieldName = field.name();
                data.parentCommandIsPrivate = needsReflection;
                data.parentCommandTypeName = fieldTypeName;
                data.parentCommandDeclaringClassName = declaringClass;
            }

            if (unmatchedAnn != null) {
                data.unmatchedFieldIndex = fieldIndex++;
                data.unmatchedFieldName = field.name();
                data.unmatchedIsPrivate = needsReflection;
                data.unmatchedDeclaringClassName = declaringClass;
            }
        }

        // Scan methods for @Option annotations (e.g., setter methods like setProperty(Map))
        fieldIndex = scanMethodOptions(classInfo, data, declaringClass, fieldIndex, crossPackage, null, null);

        return fieldIndex;
    }

    private int scanMixinOrArgGroupFields(ClassInfo ownerClass, CommandData data,
            String ownerFieldName, String ownerTypeName, int fieldIndex, boolean crossPackage) {
        String cmdPkg = packageOf(data.commandClassName);

        for (FieldInfo field : ownerClass.fields()) {
            AnnotationInstance optionAnn = field.annotation(OPTION);
            AnnotationInstance paramsAnn = field.annotation(PARAMETERS);
            AnnotationInstance nestedArgGroupAnn = field.annotation(ARG_GROUP);

            if (optionAnn == null && paramsAnn == null && nestedArgGroupAnn == null) {
                continue;
            }

            // Handle nested @ArgGroup fields inside a mixin or arg group
            if (nestedArgGroupAnn != null) {
                String nestedTypeName = fieldTypeClassName(field.type());
                ArgGroupData agd = new ArgGroupData();
                agd.fieldIndex = fieldIndex++;
                agd.fieldName = field.name();
                agd.argGroupTypeName = nestedTypeName;
                agd.isPrivate = false;
                agd.ownerPath = ownerFieldName;
                agd.ownerTypeName = ownerTypeName;
                data.argGroups.add(agd);

                ClassInfo nestedArgGroupClass = index.getClassByName(field.type().name());
                if (nestedArgGroupClass != null) {
                    boolean exclusive = annotationBool(nestedArgGroupAnn, "exclusive", true);
                    if (exclusive) {
                        List<String> groupOptionNames = new ArrayList<>();
                        for (FieldInfo agField : nestedArgGroupClass.fields()) {
                            AnnotationInstance agOption = agField.annotation(OPTION);
                            if (agOption != null) {
                                String[] names = annotationStringArray(agOption, "names");
                                if (names.length > 0) {
                                    groupOptionNames.add(names[names.length - 1]);
                                }
                            }
                        }
                        if (!groupOptionNames.isEmpty()) {
                            data.exclusiveGroups.add(groupOptionNames);
                        }
                    }
                    String nestedOwnerPath = ownerFieldName != null
                            ? ownerFieldName + "." + field.name()
                            : field.name();
                    fieldIndex = scanMixinOrArgGroupFields(nestedArgGroupClass, data, nestedOwnerPath,
                            nestedTypeName, fieldIndex, false);
                } else {
                    throw new IllegalStateException(
                            "ArgGroup class " + nestedTypeName + " is not in the Jandex index. "
                                    + "Add an IndexDependencyBuildItem for the library containing this class.");
                }
                continue;
            }

            String fieldTypeName = fieldTypeClassName(field.type());
            BuiltCommandModel.FieldKind fieldKind = detectFieldKind(field.type());
            String componentTypeName = detectComponentType(field.type());
            String optionalInnerTypeName = fieldKind == BuiltCommandModel.FieldKind.OPTIONAL
                    ? detectOptionalInnerType(field.type())
                    : null;

            if (optionAnn != null) {
                OptionData od = createOptionData(optionAnn, fieldIndex++, field.name(),
                        fieldTypeName, fieldKind, componentTypeName, optionalInnerTypeName,
                        false, null, ownerFieldName, ownerTypeName);
                data.options.add(od);
            }

            if (paramsAnn != null) {
                ParameterData pd = createParameterData(paramsAnn, fieldIndex++, field.name(),
                        fieldTypeName, fieldKind, componentTypeName,
                        false, null, ownerFieldName, ownerTypeName, data);
                data.parameters.add(pd);
            }
        }

        // Scan methods for @Option annotations in mixin/arggroup classes
        fieldIndex = scanMethodOptions(ownerClass, data, null, fieldIndex, crossPackage,
                ownerFieldName, ownerTypeName);

        return fieldIndex;
    }

    /**
     * Scans methods of a class for @Option annotations (setter-style options).
     * For method-level options, the type is derived from the method's single parameter.
     */
    private int scanMethodOptions(ClassInfo classInfo, CommandData data, String declaringClass,
            int fieldIndex, boolean crossPackage, String ownerFieldName, String ownerTypeName) {
        for (MethodInfo method : classInfo.methods()) {
            AnnotationInstance optionAnn = method.annotation(OPTION);
            if (optionAnn == null) {
                continue;
            }
            if (method.parametersCount() != 1) {
                throw new IllegalStateException("@Option on method " + classInfo.name() + "." + method.name()
                        + " must have exactly one parameter");
            }

            Type paramType = method.parameterType(0);
            String paramTypeName = fieldTypeClassName(paramType);
            BuiltCommandModel.FieldKind fieldKind = detectFieldKind(paramType);
            String componentTypeName = detectComponentType(paramType);
            String optionalInnerTypeName = fieldKind == BuiltCommandModel.FieldKind.OPTIONAL
                    ? detectOptionalInnerType(paramType)
                    : null;

            OptionData od = createOptionData(optionAnn, fieldIndex++, method.name(),
                    paramTypeName, fieldKind, componentTypeName, optionalInnerTypeName,
                    false, declaringClass, ownerFieldName, ownerTypeName);
            od.setterMethodName = method.name();
            od.setterMethodParamType = paramTypeName;
            data.options.add(od);
        }
        return fieldIndex;
    }

    private OptionData createOptionData(AnnotationInstance optionAnn, int fieldIndex, String fieldName,
            String typeName, BuiltCommandModel.FieldKind fieldKind,
            String componentTypeName, String optionalInnerTypeName,
            boolean isPrivate, String declaringClassName, String ownerPath, String ownerTypeName) {
        OptionData od = new OptionData();
        od.fieldIndex = fieldIndex;
        od.fieldName = fieldName;
        od.names = annotationStringArray(optionAnn, "names");
        od.description = joinDescriptionArray(annotationStringArray(optionAnn, "description"));
        od.typeName = typeName;
        od.required = annotationBool(optionAnn, "required", false);
        od.defaultValue = annotationString(optionAnn, "defaultValue", "");
        od.arity = annotationString(optionAnn, "arity", "");
        od.hidden = annotationBool(optionAnn, "hidden", false);
        od.paramLabel = annotationString(optionAnn, "paramLabel", "");
        od.versionHelp = annotationBool(optionAnn, "versionHelp", false);
        od.usageHelp = annotationBool(optionAnn, "usageHelp", false);
        od.negatable = annotationBool(optionAnn, "negatable", false);
        od.order = annotationInt(optionAnn, "order", -1);
        od.fieldKind = fieldKind;
        od.componentTypeName = componentTypeName;
        od.optionalInnerTypeName = optionalInnerTypeName;
        od.split = annotationString(optionAnn, "split", "");
        od.mapFallbackValue = annotationString(optionAnn, "mapFallbackValue", "");
        od.isPrivate = isPrivate;
        od.declaringClassName = declaringClassName;
        od.ownerPath = ownerPath;
        od.ownerTypeName = ownerTypeName;
        return od;
    }

    private ParameterData createParameterData(AnnotationInstance paramsAnn, int fieldIndex, String fieldName,
            String typeName, BuiltCommandModel.FieldKind fieldKind, String componentTypeName,
            boolean isPrivate, String declaringClassName, String ownerPath, String ownerTypeName,
            CommandData data) {
        ParameterData pd = new ParameterData();
        pd.fieldIndex = fieldIndex;
        pd.fieldName = fieldName;
        pd.typeName = typeName;
        pd.description = annotationString(paramsAnn, "description", "");
        pd.defaultValue = annotationString(paramsAnn, "defaultValue", "");
        pd.arity = annotationString(paramsAnn, "arity", "");
        pd.hidden = annotationBool(paramsAnn, "hidden", false);
        pd.paramLabel = annotationString(paramsAnn, "paramLabel", "");
        pd.split = annotationString(paramsAnn, "split", "");
        pd.isMultiValue = fieldKind == BuiltCommandModel.FieldKind.LIST
                || fieldKind == BuiltCommandModel.FieldKind.SET
                || fieldKind == BuiltCommandModel.FieldKind.ARRAY;
        pd.fieldKind = fieldKind;
        pd.componentTypeName = componentTypeName;
        pd.isPrivate = isPrivate;
        pd.declaringClassName = declaringClassName;
        pd.ownerPath = ownerPath;
        pd.ownerTypeName = ownerTypeName;
        String indexStr = annotationString(paramsAnn, "index", "");
        if (!indexStr.isEmpty()) {
            int dotPos = indexStr.indexOf('.');
            pd.paramIndex = Integer.parseInt(dotPos > 0 ? indexStr.substring(0, dotPos) : indexStr);
        } else {
            pd.paramIndex = data.nextParamIndex++;
        }
        return pd;
    }

    // --- Gizmo2 generation ---

    /**
     * Generates accessor classes in cross-package superclass packages so that non-public
     * fields can be accessed via direct PUTFIELD without reflection.
     *
     * @return map of declaring class name -> generated accessor class name
     */
    private Map<String, String> generateCrossPackageAccessors(
            List<FieldSetInfo> allFields, String commandClassName, String commandPkg,
            io.quarkus.gizmo2.ClassOutput classOutput) {

        // Group cross-package fields by their declaring class
        Map<String, List<FieldSetInfo>> fieldsByDeclaringClass = new HashMap<>();
        for (FieldSetInfo fsi : allFields) {
            if (!fsi.useReflection()) {
                continue;
            }
            // Determine which class declares the field that needs cross-package access
            String declaringClass;
            if (fsi.ownerPath() != null && fsi.ownerDeclaringClassName() != null) {
                // The owner field (mixin/arggroup) is on a cross-package superclass
                declaringClass = fsi.ownerDeclaringClassName();
            } else if (fsi.declaringClassName() != null) {
                declaringClass = fsi.declaringClassName();
            } else {
                continue;
            }
            fieldsByDeclaringClass.computeIfAbsent(declaringClass, k -> new ArrayList<>()).add(fsi);
        }

        Map<String, String> accessorClassNames = new HashMap<>();
        for (Map.Entry<String, List<FieldSetInfo>> entry : fieldsByDeclaringClass.entrySet()) {
            String declaringClass = entry.getKey();
            List<FieldSetInfo> fields = entry.getValue();
            // Generate accessor in the declaring class's package, named after both
            // the command and declaring class to avoid conflicts when multiple commands
            // share the same superclass. Use full command class name (dots → underscores)
            // to handle same-named classes in different packages.
            String commandId = commandClassName.replace('.', '_');
            String accessorClassName = declaringClass + "_" + commandId + "_Accessor";
            accessorClassNames.put(declaringClass, accessorClassName);

            generateAccessorClass(classOutput, accessorClassName, commandClassName, declaringClass, fields);
        }
        return accessorClassNames;
    }

    /**
     * Generates a FieldAccessor class in the declaring class's package.
     * Being in the same package allows direct PUTFIELD/GETFIELD on package-private and protected fields.
     */
    private void generateAccessorClass(io.quarkus.gizmo2.ClassOutput classOutput,
            String accessorClassName, String commandClassName, String declaringClass,
            List<FieldSetInfo> fields) {

        Gizmo gizmo = Gizmo.create(classOutput);
        gizmo.class_(accessorClassName, cc -> {
            cc.implements_(FieldAccessor.class);

            This this_ = cc.this_();

            FieldDesc fieldIndexField = cc.field("fieldIndex", fc -> {
                fc.private_();
                fc.final_();
                fc.setType(int.class);
            });

            // Constructor(int fieldIndex)
            cc.constructor(mc -> {
                var fieldIndex = mc.parameter("fieldIndex", int.class);
                mc.body(bc -> {
                    bc.invokeSpecial(ConstructorDesc.of(Object.class), this_);
                    bc.set(this_.field(fieldIndexField), fieldIndex);
                    bc.return_();
                });
            });

            // FieldAccessor.set(Object instance, Object value)
            cc.method("set", mc -> {
                mc.public_();
                mc.returning(void.class);
                var instance = mc.parameter("instance", Object.class);
                var value = mc.parameter("value", Object.class);
                mc.body(bc -> {
                    LocalVar fieldIndexExpr = bc.localVar("fieldIndex", this_.field(fieldIndexField));
                    LocalVar cmd = bc.localVar("cmd", bc.cast(instance, ClassDesc.of(commandClassName)));

                    for (FieldSetInfo fsi : fields) {
                        bc.if_(bc.eq(fieldIndexExpr, Const.of(fsi.fieldIndex())), tb -> {
                            if (fsi.ownerPath() != null) {
                                // Read the owner field (mixin/arggroup) — same package, direct access works
                                Expr owner = cmd.field(ClassDesc.of(declaringClass),
                                        fsi.ownerPath(), classDesc(fsi.ownerTypeName()));
                                if (fsi.setterMethodName() != null) {
                                    Expr castedValue = castValue(tb, value, fsi.fieldTypeName());
                                    tb.invokeVirtual(
                                            ClassMethodDesc.of(classDesc(fsi.ownerTypeName()),
                                                    fsi.setterMethodName(), ClassDesc.ofDescriptor("V"),
                                                    classDesc(fsi.setterMethodParamType())),
                                            owner, castedValue);
                                } else {
                                    Expr castedValue = castValue(tb, value, fsi.fieldTypeName());
                                    tb.set(owner.field(classDesc(fsi.ownerTypeName()),
                                            fsi.fieldName(), classDesc(fsi.fieldTypeName())), castedValue);
                                }
                            } else {
                                // Direct field on the declaring class
                                if (fsi.setterMethodName() != null) {
                                    Expr castedValue = castValue(tb, value, fsi.fieldTypeName());
                                    tb.invokeVirtual(
                                            ClassMethodDesc.of(ClassDesc.of(declaringClass),
                                                    fsi.setterMethodName(), ClassDesc.ofDescriptor("V"),
                                                    classDesc(fsi.setterMethodParamType())),
                                            cmd, castedValue);
                                } else {
                                    Expr castedValue = castValue(tb, value, fsi.fieldTypeName());
                                    tb.set(cmd.field(ClassDesc.of(declaringClass),
                                            fsi.fieldName(), classDesc(fsi.fieldTypeName())), castedValue);
                                }
                            }
                            tb.return_();
                        });
                    }
                    bc.return_();
                });
            });
        });
    }

    private void generateSetMethod(ClassCreator cc, This this_, FieldDesc fieldIndexField,
            String commandClassName, CommandData data) {
        cc.method("set", mc -> {
            mc.public_();
            mc.returning(void.class);
            var instance = mc.parameter("instance", Object.class);
            var value = mc.parameter("value", Object.class);
            mc.body(bc -> {
                LocalVar fieldIndexExpr = bc.localVar("fieldIndex", this_.field(fieldIndexField));
                LocalVar cmd = bc.localVar("cmd", bc.cast(instance, ClassDesc.of(commandClassName)));

                // Generate if-else chain for each field index.
                // Cross-package fields are handled by separate accessor classes, so skip them here.
                List<FieldSetInfo> allFields = collectAllFieldSetInfos(data, commandClassName);
                for (FieldSetInfo fsi : allFields) {
                    if (fsi.useReflection()) {
                        continue; // handled by cross-package accessor class
                    }
                    bc.if_(bc.eq(fieldIndexExpr, Const.of(fsi.fieldIndex)), tb -> {
                        if (fsi.ownerPath() != null) {
                            // Navigate through mixin/arggroup chain
                            Expr owner;
                            String ownerType;
                            if (fsi.ownerPath().contains(".")) {
                                // Multi-level path (e.g. "rewrite.run")
                                String[] segments = fsi.ownerPath().split("\\.");
                                Expr current = cmd;
                                String currentType = commandClassName;
                                for (int s = 0; s < segments.length; s++) {
                                    String segType = findFieldType(data, segments, s, commandClassName);
                                    current = current.field(ClassDesc.of(currentType),
                                            segments[s], ClassDesc.of(segType));
                                    currentType = segType;
                                }
                                owner = current;
                                ownerType = currentType;
                            } else {
                                owner = cmd.field(ClassDesc.of(commandClassName),
                                        fsi.ownerPath(), classDesc(fsi.ownerTypeName()));
                                ownerType = fsi.ownerTypeName();
                            }
                            if (fsi.setterMethodName() != null) {
                                Expr castedValue = castValue(tb, value, fsi.fieldTypeName());
                                tb.invokeVirtual(
                                        ClassMethodDesc.of(classDesc(ownerType),
                                                fsi.setterMethodName(), ClassDesc.ofDescriptor("V"),
                                                classDesc(fsi.setterMethodParamType())),
                                        owner, castedValue);
                            } else {
                                Expr castedValue = castValue(tb, value, fsi.fieldTypeName());
                                tb.set(owner.field(classDesc(fsi.ownerTypeName()),
                                        fsi.fieldName(), classDesc(fsi.fieldTypeName())), castedValue);
                            }
                        } else {
                            // Direct field or method on command class
                            String targetClass = fsi.declaringClassName() != null
                                    ? fsi.declaringClassName()
                                    : commandClassName;
                            if (fsi.setterMethodName() != null) {
                                Expr castedValue = castValue(tb, value, fsi.fieldTypeName());
                                tb.invokeVirtual(
                                        ClassMethodDesc.of(ClassDesc.of(targetClass),
                                                fsi.setterMethodName(), ClassDesc.ofDescriptor("V"),
                                                classDesc(fsi.setterMethodParamType())),
                                        cmd, castedValue);
                            } else {
                                Expr castedValue = castValue(tb, value, fsi.fieldTypeName());
                                tb.set(cmd.field(ClassDesc.of(targetClass),
                                        fsi.fieldName(), classDesc(fsi.fieldTypeName())), castedValue);
                            }
                        }
                        tb.return_();
                    });
                }

                bc.return_(); // fallthrough
            });
        });
    }

    private void generateStaticInitializer(ClassCreator cc, String modelClassName,
            String commandClassName, CommandData data,
            Map<String, String> accessorClassNames) {
        cc.staticInitializer(bc -> {
            ClassDesc modelClassDesc = ClassDesc.of(modelClassName);

            // Supplier<Object> supplier = new ModelClass(0); (fieldIndex doesn't matter for get())
            LocalVar supplier = bc.localVar("supplier",
                    bc.new_(ConstructorDesc.of(modelClassDesc, int.class), Const.of(-1)));

            // BuiltCommandModel.Builder builder = BuiltCommandModel.builder(CommandClass.class, supplier);
            LocalVar builder = bc.localVar("builder", bc.invokeStatic(
                    MethodDesc.of(BuiltCommandModel.class, "builder",
                            BuiltCommandModel.Builder.class, Class.class, Supplier.class),
                    Const.of(ClassDesc.of(commandClassName)), supplier));

            // Set command metadata
            bc.invokeVirtual(m("name", String.class), builder, Const.of(data.name));
            invokeBuilderStringArray(bc, builder, "description", data.description);
            invokeBuilderStringArray(bc, builder, "version", data.version);
            bc.invokeVirtual(m("mixinStandardHelpOptions", boolean.class), builder,
                    Const.of(data.mixinStandardHelpOptions));
            invokeBuilderStringArray(bc, builder, "header", data.header);
            invokeBuilderStringArray(bc, builder, "footer", data.footer);
            invokeBuilderStringArray(bc, builder, "aliases", data.aliases);
            bc.invokeVirtual(m("showDefaultValues", boolean.class), builder,
                    Const.of(data.showDefaultValues));
            bc.invokeVirtual(m("commandListHeading", String.class), builder,
                    Const.of(data.commandListHeading));
            bc.invokeVirtual(m("synopsisHeading", String.class), builder,
                    Const.of(data.synopsisHeading));
            bc.invokeVirtual(m("optionListHeading", String.class), builder,
                    Const.of(data.optionListHeading));
            bc.invokeVirtual(m("headerHeading", String.class), builder,
                    Const.of(data.headerHeading));
            bc.invokeVirtual(m("parameterListHeading", String.class), builder,
                    Const.of(data.parameterListHeading));

            // Scope
            LocalVar scopeValue = bc.localVar("scope",
                    bc.get(Expr.staticField(FieldDesc.of(ScopeType.class, data.scope.name()))));
            bc.invokeVirtual(m("scope", ScopeType.class), builder, scopeValue);

            // Version provider
            if (data.versionProviderClassName != null
                    && !"io.quarkus.quickcli.VersionProvider.NoVersionProvider".equals(data.versionProviderClassName)
                    && !"io.quarkus.quickcli.VersionProvider$NoVersionProvider"
                            .equals(data.versionProviderClassName)) {
                bc.invokeVirtual(m("versionProviderClass", Class.class),
                        builder, Const.of(ClassDesc.of(data.versionProviderClassName)));
            }

            // HasUnmatchedField / HasSpecField
            bc.invokeVirtual(m("hasUnmatchedField", boolean.class),
                    builder, Const.of(data.unmatchedFieldName != null));
            bc.invokeVirtual(m("hasSpecField", boolean.class),
                    builder, Const.of(data.specFieldName != null));

            // Build owner-declaring-class map for accessor resolution.
            // Only includes entries where the owner field needs cross-package access (isPrivate).
            Map<String, String> ownerDeclaring = new HashMap<>();
            for (MixinData md : data.mixins) {
                if (md.isPrivate && md.declaringClassName != null) {
                    ownerDeclaring.put(md.fieldName, md.declaringClassName);
                }
            }
            for (ArgGroupData agd : data.argGroups) {
                if (agd.isPrivate && agd.ownerPath == null && agd.declaringClassName != null) {
                    ownerDeclaring.put(agd.fieldName, agd.declaringClassName);
                }
            }

            // Options
            for (OptionData od : data.options) {
                String ownerDecl = od.ownerPath != null ? ownerDeclaring.get(firstSegment(od.ownerPath)) : null;
                boolean needsAccessor = od.isPrivate || ownerDecl != null;
                String accClass = needsAccessor
                        ? accessorClass(modelClassName, ownerDecl != null ? ownerDecl : od.declaringClassName,
                                accessorClassNames)
                        : modelClassName;
                LocalVar accessor = bc.localVar("accessor",
                        bc.new_(ConstructorDesc.of(ClassDesc.of(accClass), int.class),
                                Const.of(od.fieldIndex)));
                LocalVar namesArr = bc.localVar("namesArr", createStringArray(bc, od.names));
                LocalVar fieldKind = bc.localVar("fieldKind", bc.get(Expr.staticField(
                        FieldDesc.of(BuiltCommandModel.FieldKind.class, od.fieldKind.name()))));
                Expr componentType = od.componentTypeName != null
                        ? classConstExpr(od.componentTypeName)
                        : Const.ofNull(Class.class);
                Expr optionalInnerType = od.optionalInnerTypeName != null
                        ? classConstExpr(od.optionalInnerTypeName)
                        : Const.ofNull(Class.class);

                LocalVar binding = bc.localVar("binding", bc.new_(
                        ConstructorDesc.of(BuiltCommandModel.OptionBinding.class,
                                String[].class, String.class, Class.class, boolean.class, String.class,
                                String.class, boolean.class, String.class, String.class, boolean.class,
                                boolean.class, boolean.class, int.class, BuiltCommandModel.FieldKind.class,
                                Class.class, Class.class, String.class, String.class, FieldAccessor.class),
                        namesArr, Const.of(od.description), classConstExpr(od.typeName),
                        Const.of(od.required),
                        Const.of(od.defaultValue), Const.of(od.arity), Const.of(od.hidden),
                        Const.of(od.paramLabel), Const.of(od.fieldName), Const.of(od.versionHelp),
                        Const.of(od.usageHelp), Const.of(od.negatable), Const.of(od.order),
                        fieldKind, componentType, optionalInnerType, Const.of(od.split),
                        Const.of(od.mapFallbackValue), accessor));

                bc.invokeVirtual(
                        MethodDesc.of(BuiltCommandModel.Builder.class, "addOption",
                                BuiltCommandModel.Builder.class, BuiltCommandModel.OptionBinding.class),
                        builder, binding);
            }

            // Parameters
            for (ParameterData pd : data.parameters) {
                String pdOwnerDecl = pd.ownerPath != null ? ownerDeclaring.get(firstSegment(pd.ownerPath)) : null;
                boolean pdNeedsAccessor = pd.isPrivate || pdOwnerDecl != null;
                String accClass = pdNeedsAccessor
                        ? accessorClass(modelClassName,
                                pdOwnerDecl != null ? pdOwnerDecl : pd.declaringClassName, accessorClassNames)
                        : modelClassName;
                LocalVar accessor = bc.localVar("accessor",
                        bc.new_(ConstructorDesc.of(ClassDesc.of(accClass), int.class),
                                Const.of(pd.fieldIndex)));
                LocalVar fieldKind = bc.localVar("fieldKind", bc.get(Expr.staticField(
                        FieldDesc.of(BuiltCommandModel.FieldKind.class, pd.fieldKind.name()))));
                Expr componentType = pd.componentTypeName != null
                        ? classConstExpr(pd.componentTypeName)
                        : Const.ofNull(Class.class);

                LocalVar binding = bc.localVar("binding", bc.new_(
                        ConstructorDesc.of(BuiltCommandModel.ParameterBinding.class,
                                int.class, String.class, Class.class, String.class, String.class,
                                boolean.class, String.class, String.class, boolean.class,
                                BuiltCommandModel.FieldKind.class, Class.class,
                                FieldAccessor.class, String.class),
                        Const.of(pd.paramIndex), Const.of(pd.description),
                        classConstExpr(pd.typeName),
                        Const.of(pd.defaultValue), Const.of(pd.arity), Const.of(pd.hidden),
                        Const.of(pd.paramLabel), Const.of(pd.fieldName), Const.of(pd.isMultiValue),
                        fieldKind, componentType, accessor, Const.of(pd.split)));

                bc.invokeVirtual(
                        MethodDesc.of(BuiltCommandModel.Builder.class, "addParameter",
                                BuiltCommandModel.Builder.class, BuiltCommandModel.ParameterBinding.class),
                        builder, binding);
            }

            // Subcommands
            for (String subCls : data.subcommandClassNames) {
                bc.invokeVirtual(
                        MethodDesc.of(BuiltCommandModel.Builder.class, "addSubcommand",
                                BuiltCommandModel.Builder.class, Class.class),
                        builder, Const.of(ClassDesc.of(subCls)));
            }

            // Mixins
            for (MixinData md : data.mixins) {
                String accClass = md.isPrivate
                        ? accessorClass(modelClassName, md.declaringClassName, accessorClassNames)
                        : modelClassName;
                LocalVar accessor = bc.localVar("accessor",
                        bc.new_(ConstructorDesc.of(ClassDesc.of(accClass), int.class),
                                Const.of(md.fieldIndex)));
                LocalVar binding = bc.localVar("binding", bc.new_(
                        ConstructorDesc.of(BuiltCommandModel.MixinBinding.class,
                                String.class, Class.class, FieldAccessor.class),
                        Const.of(md.fieldName), Const.of(ClassDesc.of(md.mixinTypeName)), accessor));
                bc.invokeVirtual(
                        MethodDesc.of(BuiltCommandModel.Builder.class, "addMixin",
                                BuiltCommandModel.Builder.class, BuiltCommandModel.MixinBinding.class),
                        builder, binding);
            }

            // ArgGroups
            for (ArgGroupData agd : data.argGroups) {
                String accClass = agd.isPrivate
                        ? accessorClass(modelClassName, agd.declaringClassName, accessorClassNames)
                        : modelClassName;
                LocalVar accessor = bc.localVar("accessor",
                        bc.new_(ConstructorDesc.of(ClassDesc.of(accClass), int.class),
                                Const.of(agd.fieldIndex)));
                LocalVar binding = bc.localVar("binding", bc.new_(
                        ConstructorDesc.of(BuiltCommandModel.ArgGroupBinding.class,
                                String.class, Class.class, FieldAccessor.class),
                        Const.of(agd.fieldName), Const.of(ClassDesc.of(agd.argGroupTypeName)), accessor));
                bc.invokeVirtual(
                        MethodDesc.of(BuiltCommandModel.Builder.class, "addArgGroup",
                                BuiltCommandModel.Builder.class, BuiltCommandModel.ArgGroupBinding.class),
                        builder, binding);
            }

            // Exclusive groups
            for (List<String> group : data.exclusiveGroups) {
                LocalVar arr = bc.localVar("arr", createStringArray(bc, group.toArray(new String[0])));
                LocalVar list = bc.localVar("list", bc.invokeStatic(
                        MethodDesc.of(Arrays.class, "asList", java.util.List.class, Object[].class),
                        arr));
                bc.invokeVirtual(
                        MethodDesc.of(BuiltCommandModel.Builder.class, "addExclusiveGroup",
                                BuiltCommandModel.Builder.class, java.util.List.class),
                        builder, list);
            }

            // Parent command accessor
            if (data.parentCommandFieldName != null) {
                String accClass = data.parentCommandIsPrivate
                        ? accessorClass(modelClassName, data.parentCommandDeclaringClassName, accessorClassNames)
                        : modelClassName;
                LocalVar accessor = bc.localVar("accessor",
                        bc.new_(ConstructorDesc.of(ClassDesc.of(accClass), int.class),
                                Const.of(data.parentCommandFieldIndex)));
                bc.invokeVirtual(
                        MethodDesc.of(BuiltCommandModel.Builder.class, "parentCommandAccessor",
                                BuiltCommandModel.Builder.class, FieldAccessor.class),
                        builder, accessor);
            }

            // Unmatched accessor
            if (data.unmatchedFieldName != null) {
                String accClass = data.unmatchedIsPrivate
                        ? accessorClass(modelClassName, data.unmatchedDeclaringClassName, accessorClassNames)
                        : modelClassName;
                LocalVar accessor = bc.localVar("accessor",
                        bc.new_(ConstructorDesc.of(ClassDesc.of(accClass), int.class),
                                Const.of(data.unmatchedFieldIndex)));
                bc.invokeVirtual(
                        MethodDesc.of(BuiltCommandModel.Builder.class, "unmatchedAccessor",
                                BuiltCommandModel.Builder.class, FieldAccessor.class),
                        builder, accessor);
            }

            // Spec accessor
            if (data.specFieldName != null) {
                String accClass = data.specIsPrivate
                        ? accessorClass(modelClassName, data.specDeclaringClassName, accessorClassNames)
                        : modelClassName;
                LocalVar accessor = bc.localVar("accessor",
                        bc.new_(ConstructorDesc.of(ClassDesc.of(accClass), int.class),
                                Const.of(data.specFieldIndex)));
                bc.invokeVirtual(
                        MethodDesc.of(BuiltCommandModel.Builder.class, "specAccessor",
                                BuiltCommandModel.Builder.class, FieldAccessor.class),
                        builder, accessor);
            }

            // Mixee spec accessors
            for (MixeeSpecData msd : data.mixeeSpecs) {
                String mixinOwnerDecl = null;
                for (MixinData md : data.mixins) {
                    if (md.fieldName.equals(msd.mixinFieldName)) {
                        mixinOwnerDecl = md.declaringClassName;
                        break;
                    }
                }
                boolean msdNeedsAccessor = msd.isPrivate || mixinOwnerDecl != null;
                String accClass = msdNeedsAccessor
                        ? accessorClass(modelClassName, mixinOwnerDecl, accessorClassNames)
                        : modelClassName;
                LocalVar accessor = bc.localVar("accessor",
                        bc.new_(ConstructorDesc.of(ClassDesc.of(accClass), int.class),
                                Const.of(msd.fieldIndex)));
                bc.invokeVirtual(
                        MethodDesc.of(BuiltCommandModel.Builder.class, "addMixeeSpecAccessor",
                                BuiltCommandModel.Builder.class, FieldAccessor.class),
                        builder, accessor);
            }

            // Build and register
            Expr model = bc.invokeVirtual(
                    MethodDesc.of(BuiltCommandModel.Builder.class, "build", BuiltCommandModel.class),
                    builder);
            bc.invokeStatic(
                    MethodDesc.of(CommandModelRegistry.class, "register", void.class,
                            io.quarkus.quickcli.model.CommandModel.class),
                    model);

            // Register instance creators for mixin and arg group types
            Set<String> registeredCreators = new HashSet<>();
            for (MixinData md : data.mixins) {
                if (registeredCreators.add(md.mixinTypeName)) {
                    registerInstanceCreator(bc, md.mixinTypeName);
                }
            }
            for (ArgGroupData agd : data.argGroups) {
                if (registeredCreators.add(agd.argGroupTypeName)) {
                    registerInstanceCreator(bc, agd.argGroupTypeName);
                }
            }
        });
    }

    /**
     * Generates bytecode to register an instance creator (Supplier) for a non-@Command class
     * (mixin or arg group) so that Factory can instantiate it without reflection.
     */
    private void registerInstanceCreator(BlockCreator bc, String typeName) {
        ClassDesc typeClassDesc = ClassDesc.of(typeName);
        LocalVar supplierLambda = bc.localVar("supplier", bc.lambda(Supplier.class, lc -> {
            lc.body(lbc -> lbc.return_(lbc.new_(typeClassDesc)));
        }));
        bc.invokeStatic(
                MethodDesc.of(CommandModelRegistry.class, "registerInstanceCreator",
                        void.class, Class.class, Supplier.class),
                Const.of(typeClassDesc), supplierLambda);
    }

    // --- Helpers ---

    private List<FieldSetInfo> collectAllFieldSetInfos(CommandData data, String commandClassName) {
        List<FieldSetInfo> result = new ArrayList<>();

        // Build a map of ownerPath -> declaring class for owner field navigation.
        // Only includes entries where the owner field needs cross-package access (isPrivate).
        Map<String, String> ownerDeclaring = new HashMap<>();
        for (MixinData md : data.mixins) {
            if (md.isPrivate) {
                ownerDeclaring.put(md.fieldName, md.declaringClassName);
            }
        }
        for (ArgGroupData agd : data.argGroups) {
            if (agd.isPrivate && agd.ownerPath == null) {
                ownerDeclaring.put(agd.fieldName, agd.declaringClassName);
            }
        }

        for (OptionData od : data.options) {
            String ownerDecl = od.ownerPath != null ? ownerDeclaring.get(firstSegment(od.ownerPath)) : null;
            boolean needsAccessor = od.isPrivate || ownerDecl != null;
            result.add(new FieldSetInfo(od.fieldIndex, od.fieldName, od.typeName,
                    od.ownerPath, od.ownerTypeName, ownerDecl, od.declaringClassName,
                    od.setterMethodName, od.setterMethodParamType, needsAccessor));
        }
        for (ParameterData pd : data.parameters) {
            String ownerDecl = pd.ownerPath != null ? ownerDeclaring.get(firstSegment(pd.ownerPath)) : null;
            boolean needsAccessor = pd.isPrivate || ownerDecl != null;
            result.add(new FieldSetInfo(pd.fieldIndex, pd.fieldName, pd.typeName,
                    pd.ownerPath, pd.ownerTypeName, ownerDecl, pd.declaringClassName,
                    null, null, needsAccessor));
        }
        for (MixinData md : data.mixins) {
            result.add(new FieldSetInfo(md.fieldIndex, md.fieldName, md.mixinTypeName,
                    null, null, null, md.declaringClassName,
                    null, null, md.isPrivate));
        }
        for (ArgGroupData agd : data.argGroups) {
            String ownerDecl = agd.ownerPath != null ? ownerDeclaring.get(firstSegment(agd.ownerPath)) : null;
            result.add(new FieldSetInfo(agd.fieldIndex, agd.fieldName, agd.argGroupTypeName,
                    agd.ownerPath, agd.ownerTypeName, ownerDecl, agd.declaringClassName,
                    null, null, agd.isPrivate));
        }
        if (data.parentCommandFieldName != null) {
            result.add(new FieldSetInfo(data.parentCommandFieldIndex, data.parentCommandFieldName,
                    data.parentCommandTypeName, null, null, data.parentCommandDeclaringClassName,
                    data.parentCommandIsPrivate));
        }
        if (data.unmatchedFieldName != null) {
            result.add(new FieldSetInfo(data.unmatchedFieldIndex, data.unmatchedFieldName,
                    "java.util.List", null, null, data.unmatchedDeclaringClassName,
                    data.unmatchedIsPrivate));
        }
        if (data.specFieldName != null) {
            result.add(new FieldSetInfo(data.specFieldIndex, data.specFieldName,
                    CommandSpec.class.getName(), null, null, data.specDeclaringClassName,
                    data.specIsPrivate));
        }
        for (MixeeSpecData msd : data.mixeeSpecs) {
            String ownerDecl = ownerDeclaring.get(msd.mixinFieldName);
            // Route to accessor if either the spec field is private OR the mixin field
            // is on a cross-package superclass (need same-package access to read the mixin field)
            boolean needsAccessor = msd.isPrivate || ownerDecl != null;
            result.add(new FieldSetInfo(msd.fieldIndex, msd.specFieldName,
                    CommandSpec.class.getName(), msd.mixinFieldName, msd.mixinTypeName, ownerDecl,
                    null, null, null, needsAccessor));
        }
        return result;
    }

    private static String firstSegment(String path) {
        int dot = path.indexOf('.');
        return dot > 0 ? path.substring(0, dot) : path;
    }

    /**
     * Returns the accessor class name for a given declaring class. If the declaring class
     * has a cross-package accessor, returns that; otherwise returns the model class name.
     */
    private String accessorClass(String modelClassName, String declaringClassName,
            Map<String, String> accessorClassNames) {
        if (declaringClassName != null) {
            String accessor = accessorClassNames.get(declaringClassName);
            if (accessor != null) {
                return accessor;
            }
        }
        return modelClassName;
    }

    private Expr castValue(BlockCreator bc, Expr value, String targetTypeName) {
        return switch (targetTypeName) {
            case "int" -> bc.invokeVirtual(
                    MethodDesc.of(Integer.class, "intValue", int.class),
                    bc.cast(value, Integer.class));
            case "long" -> bc.invokeVirtual(
                    MethodDesc.of(Long.class, "longValue", long.class),
                    bc.cast(value, Long.class));
            case "boolean" -> bc.invokeVirtual(
                    MethodDesc.of(Boolean.class, "booleanValue", boolean.class),
                    bc.cast(value, Boolean.class));
            case "double" -> bc.invokeVirtual(
                    MethodDesc.of(Double.class, "doubleValue", double.class),
                    bc.cast(value, Double.class));
            case "float" -> bc.invokeVirtual(
                    MethodDesc.of(Float.class, "floatValue", float.class),
                    bc.cast(value, Float.class));
            case "short" -> bc.invokeVirtual(
                    MethodDesc.of(Short.class, "shortValue", short.class),
                    bc.cast(value, Short.class));
            case "byte" -> bc.invokeVirtual(
                    MethodDesc.of(Byte.class, "byteValue", byte.class),
                    bc.cast(value, Byte.class));
            case "char" -> bc.invokeVirtual(
                    MethodDesc.of(Character.class, "charValue", char.class),
                    bc.cast(value, Character.class));
            default -> bc.cast(value, classDesc(targetTypeName));
        };
    }

    /**
     * Find the type of a field at a given depth in a dot-separated path by looking through
     * mixin, argGroup, and option data.
     */
    private String findFieldType(CommandData data, String[] segments, int segIndex, String commandClassName) {
        String fieldName = segments[segIndex];
        String parentPath = segIndex == 0 ? null : String.join(".", java.util.Arrays.copyOf(segments, segIndex));

        // Check mixins
        for (MixinData md : data.mixins) {
            if (md.fieldName.equals(fieldName) && (parentPath == null) == (md.declaringClassName == null)) {
                return md.mixinTypeName;
            }
        }
        // Check argGroups
        for (ArgGroupData agd : data.argGroups) {
            if (agd.fieldName.equals(fieldName)) {
                if (segIndex == 0 && agd.ownerPath == null) {
                    return agd.argGroupTypeName;
                }
                if (parentPath != null && parentPath.equals(agd.ownerPath)) {
                    return agd.argGroupTypeName;
                }
            }
        }
        throw new IllegalStateException("Cannot find type for field '" + fieldName + "' at segment " + segIndex
                + " in path '" + String.join(".", segments) + "'");
    }

    private void invokeBuilderStringArray(BlockCreator bc, Expr builder, String methodName, String[] values) {
        LocalVar arr = bc.localVar("arr", createStringArray(bc, values));
        bc.invokeVirtual(
                MethodDesc.of(BuiltCommandModel.Builder.class, methodName,
                        BuiltCommandModel.Builder.class, String[].class),
                builder, arr);
    }

    private Expr createStringArray(BlockCreator bc, String[] values) {
        return bc.newArray(String.class, java.util.Arrays.asList(values), Const::of);
    }

    private static MethodDesc m(String name, Class<?>... paramTypes) {
        return MethodDesc.of(BuiltCommandModel.Builder.class, name, BuiltCommandModel.Builder.class, paramTypes);
    }

    /**
     * Returns a {@link Const} expression that loads the {@link Class} for the given type name.
     * For primitives, uses {@code Const.of(int.class)} etc. (generates {@code getstatic Integer.TYPE}),
     * since {@code ldc} cannot load primitive class references.
     */
    private static Const classConstExpr(String typeName) {
        return switch (typeName) {
            case "int" -> Const.of(int.class);
            case "long" -> Const.of(long.class);
            case "boolean" -> Const.of(boolean.class);
            case "double" -> Const.of(double.class);
            case "float" -> Const.of(float.class);
            case "short" -> Const.of(short.class);
            case "byte" -> Const.of(byte.class);
            case "char" -> Const.of(char.class);
            default -> Const.of(classDesc(typeName));
        };
    }

    /**
     * Converts a type name string to a {@link ClassDesc}.
     * Handles primitives, arrays (e.g., {@code [Ljava.lang.String;}), and regular class names.
     */
    private static ClassDesc classDesc(String typeName) {
        return switch (typeName) {
            case "int" -> ClassDesc.ofDescriptor("I");
            case "long" -> ClassDesc.ofDescriptor("J");
            case "boolean" -> ClassDesc.ofDescriptor("Z");
            case "double" -> ClassDesc.ofDescriptor("D");
            case "float" -> ClassDesc.ofDescriptor("F");
            case "short" -> ClassDesc.ofDescriptor("S");
            case "byte" -> ClassDesc.ofDescriptor("B");
            case "char" -> ClassDesc.ofDescriptor("C");
            case "void" -> ClassDesc.ofDescriptor("V");
            default -> {
                if (typeName.startsWith("[")) {
                    // Array type like [Ljava.lang.String; — convert dots to slashes for JVM descriptor
                    yield ClassDesc.ofDescriptor(typeName.replace('.', '/'));
                }
                yield ClassDesc.of(typeName);
            }
        };
    }

    // --- Type detection ---

    private BuiltCommandModel.FieldKind detectFieldKind(Type type) {
        if (type.kind() == Type.Kind.ARRAY) {
            return BuiltCommandModel.FieldKind.ARRAY;
        }
        DotName typeName = type.name();
        if (typeName.equals(LIST) || isSubtypeOf(typeName, LIST)) {
            return BuiltCommandModel.FieldKind.LIST;
        }
        if (typeName.equals(SET) || isSubtypeOf(typeName, SET)) {
            return BuiltCommandModel.FieldKind.SET;
        }
        if (typeName.equals(MAP) || isSubtypeOf(typeName, MAP)) {
            return BuiltCommandModel.FieldKind.MAP;
        }
        if (typeName.equals(OPTIONAL)) {
            return BuiltCommandModel.FieldKind.OPTIONAL;
        }
        return BuiltCommandModel.FieldKind.SINGLE;
    }

    private boolean isSubtypeOf(DotName typeName, DotName superTypeName) {
        ClassInfo classInfo = index.getClassByName(typeName);
        if (classInfo == null) {
            return false;
        }
        for (DotName iface : classInfo.interfaceNames()) {
            if (iface.equals(superTypeName)) {
                return true;
            }
        }
        return false;
    }

    private String detectComponentType(Type type) {
        if (type.kind() == Type.Kind.ARRAY) {
            return fieldTypeClassName(type.asArrayType().constituent());
        }
        if (type.kind() == Type.Kind.PARAMETERIZED_TYPE) {
            ParameterizedType pt = type.asParameterizedType();
            if (!pt.arguments().isEmpty()) {
                return fieldTypeClassName(pt.arguments().get(0));
            }
        }
        return "java.lang.String"; // default fallback
    }

    private String detectOptionalInnerType(Type type) {
        if (type.kind() == Type.Kind.PARAMETERIZED_TYPE) {
            ParameterizedType pt = type.asParameterizedType();
            if (!pt.arguments().isEmpty()) {
                return fieldTypeClassName(pt.arguments().get(0));
            }
        }
        return "java.lang.String";
    }

    /**
     * Returns the type name in a format that works with field descriptors.
     * For arrays, uses JVM format like {@code [Ljava.lang.String;}.
     */
    private String fieldTypeClassName(Type type) {
        switch (type.kind()) {
            case PRIMITIVE:
                return type.asPrimitiveType().primitive().name().toLowerCase();
            case ARRAY:
                return "[" + fieldTypeDescriptor(type.asArrayType().constituent());
            case CLASS:
            case PARAMETERIZED_TYPE:
                return type.name().toString();
            default:
                return type.name().toString();
        }
    }

    private String fieldTypeDescriptor(Type type) {
        switch (type.kind()) {
            case PRIMITIVE:
                switch (type.asPrimitiveType().primitive()) {
                    case BOOLEAN:
                        return "Z";
                    case BYTE:
                        return "B";
                    case CHAR:
                        return "C";
                    case DOUBLE:
                        return "D";
                    case FLOAT:
                        return "F";
                    case INT:
                        return "I";
                    case LONG:
                        return "J";
                    case SHORT:
                        return "S";
                    default:
                        throw new IllegalArgumentException("Unknown primitive: " + type);
                }
            case ARRAY:
                return "[" + fieldTypeDescriptor(type.asArrayType().constituent());
            case CLASS:
            case PARAMETERIZED_TYPE:
                return "L" + type.name().toString() + ";";
            default:
                return "L" + type.name().toString() + ";";
        }
    }

    // --- Annotation helpers ---

    private static String annotationString(AnnotationInstance ann, String key, String defaultValue) {
        AnnotationValue value = ann.value(key);
        return value != null ? value.asString() : defaultValue;
    }

    private static String[] annotationStringArray(AnnotationInstance ann, String key) {
        AnnotationValue value = ann.value(key);
        return value != null ? value.asStringArray() : new String[0];
    }

    private static boolean annotationBool(AnnotationInstance ann, String key, boolean defaultValue) {
        AnnotationValue value = ann.value(key);
        return value != null ? value.asBoolean() : defaultValue;
    }

    private static int annotationInt(AnnotationInstance ann, String key, int defaultValue) {
        AnnotationValue value = ann.value(key);
        return value != null ? value.asInt() : defaultValue;
    }

    @SuppressWarnings("unchecked")
    private static <E extends Enum<E>> E annotationEnum(AnnotationInstance ann, String key,
            Class<E> enumClass, E defaultValue) {
        AnnotationValue value = ann.value(key);
        if (value != null) {
            return Enum.valueOf(enumClass, value.asEnum());
        }
        return defaultValue;
    }

    private static String joinDescriptionArray(String[] arr) {
        return arr.length == 0 ? "" : String.join(" ", arr);
    }

    private static String packageOf(String className) {
        int dot = className.lastIndexOf('.');
        return dot > 0 ? className.substring(0, dot) : "";
    }

    // --- Data classes ---

    static class CommandData {
        String commandClassName;
        String name;
        String[] description = new String[0];
        String[] version = new String[0];
        boolean mixinStandardHelpOptions;
        String[] header = new String[0];
        String[] footer = new String[0];
        ScopeType scope = ScopeType.LOCAL;
        String[] aliases = new String[0];
        boolean showDefaultValues;
        String commandListHeading = "Commands:%n";
        String synopsisHeading = "Usage: ";
        String optionListHeading = "Options:%n";
        String headerHeading = "";
        String parameterListHeading = "%n";
        String versionProviderClassName;
        List<String> subcommandClassNames = new ArrayList<>();
        List<OptionData> options = new ArrayList<>();
        List<ParameterData> parameters = new ArrayList<>();
        List<MixinData> mixins = new ArrayList<>();
        List<ArgGroupData> argGroups = new ArrayList<>();
        List<MixeeSpecData> mixeeSpecs = new ArrayList<>();
        List<List<String>> exclusiveGroups = new ArrayList<>();
        int nextParamIndex;

        // Special fields
        String specFieldName;
        int specFieldIndex = -1;
        boolean specIsPrivate;
        String specDeclaringClassName;
        String parentCommandFieldName;
        int parentCommandFieldIndex = -1;
        boolean parentCommandIsPrivate;
        String parentCommandTypeName;
        String parentCommandDeclaringClassName;
        String unmatchedFieldName;
        int unmatchedFieldIndex = -1;
        boolean unmatchedIsPrivate;
        String unmatchedDeclaringClassName;
    }

    static class OptionData {
        int fieldIndex;
        String fieldName;
        String[] names;
        String description;
        String typeName;
        boolean required;
        String defaultValue;
        String arity;
        boolean hidden;
        String paramLabel;
        boolean versionHelp;
        boolean usageHelp;
        boolean negatable;
        int order;
        BuiltCommandModel.FieldKind fieldKind;
        String componentTypeName;
        String optionalInnerTypeName;
        String split;
        String mapFallbackValue;
        boolean isPrivate;
        String declaringClassName;
        String ownerPath; // null for direct fields, mixin/arggroup field name for navigated fields
        String ownerTypeName;
        String setterMethodName; // non-null for method-level @Option (e.g., "setProperty")
        String setterMethodParamType; // parameter type descriptor for the setter method
    }

    static class ParameterData {
        int fieldIndex;
        String fieldName;
        String typeName;
        String description;
        String defaultValue;
        String arity;
        boolean hidden;
        String paramLabel;
        boolean isMultiValue;
        BuiltCommandModel.FieldKind fieldKind;
        String componentTypeName;
        boolean isPrivate;
        String declaringClassName;
        String ownerPath;
        String ownerTypeName;
        int paramIndex;
        String split = "";
    }

    static class MixinData {
        int fieldIndex;
        String fieldName;
        String mixinTypeName;
        boolean isPrivate;
        String declaringClassName;
    }

    static class ArgGroupData {
        int fieldIndex;
        String fieldName;
        String argGroupTypeName;
        boolean isPrivate;
        String declaringClassName;
        String ownerPath;
        String ownerTypeName;
    }

    static class MixeeSpecData {
        int fieldIndex;
        String mixinFieldName;
        String mixinTypeName;
        String specFieldName;
        boolean isPrivate;
    }

    record FieldSetInfo(int fieldIndex, String fieldName, String fieldTypeName,
            String ownerPath, String ownerTypeName, String ownerDeclaringClassName,
            String declaringClassName,
            String setterMethodName, String setterMethodParamType, boolean useReflection) {

        FieldSetInfo(int fieldIndex, String fieldName, String fieldTypeName,
                String ownerPath, String ownerTypeName, String declaringClassName) {
            this(fieldIndex, fieldName, fieldTypeName, ownerPath, ownerTypeName, null,
                    declaringClassName, null, null, false);
        }

        FieldSetInfo(int fieldIndex, String fieldName, String fieldTypeName,
                String ownerPath, String ownerTypeName, String declaringClassName,
                boolean useReflection) {
            this(fieldIndex, fieldName, fieldTypeName, ownerPath, ownerTypeName, null,
                    declaringClassName, null, null, useReflection);
        }
    }
}
