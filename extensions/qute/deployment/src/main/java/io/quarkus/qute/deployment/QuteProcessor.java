package io.quarkus.qute.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;
import static java.util.stream.Collectors.toMap;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.TypeVariable;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem.BeanConfiguratorBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BuildExtension;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.qute.Engine;
import io.quarkus.qute.EngineBuilder;
import io.quarkus.qute.Expression;
import io.quarkus.qute.Expression.VirtualMethodPart;
import io.quarkus.qute.LoopSectionHelper;
import io.quarkus.qute.PublisherFactory;
import io.quarkus.qute.ResultNode;
import io.quarkus.qute.SectionHelper;
import io.quarkus.qute.SectionHelperFactory;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateException;
import io.quarkus.qute.TemplateExtension;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.TemplateLocator;
import io.quarkus.qute.UserTagSectionHelper;
import io.quarkus.qute.Variant;
import io.quarkus.qute.api.ResourcePath;
import io.quarkus.qute.api.VariantTemplate;
import io.quarkus.qute.deployment.TemplatesAnalysisBuildItem.TemplateAnalysis;
import io.quarkus.qute.deployment.TypeCheckExcludeBuildItem.Check;
import io.quarkus.qute.deployment.TypeInfos.Info;
import io.quarkus.qute.generator.ExtensionMethodGenerator;
import io.quarkus.qute.generator.ValueResolverGenerator;
import io.quarkus.qute.mutiny.MutinyPublisherFactory;
import io.quarkus.qute.runtime.EngineProducer;
import io.quarkus.qute.runtime.QuteConfig;
import io.quarkus.qute.runtime.QuteRecorder;
import io.quarkus.qute.runtime.QuteRecorder.QuteContext;
import io.quarkus.qute.runtime.TemplateProducer;
import io.quarkus.qute.runtime.VariantTemplateProducer;
import io.quarkus.qute.runtime.extensions.CollectionTemplateExtensions;
import io.quarkus.qute.runtime.extensions.MapTemplateExtensions;
import io.quarkus.qute.runtime.extensions.NumberTemplateExtensions;

public class QuteProcessor {

    private static final Logger LOGGER = Logger.getLogger(QuteProcessor.class);

    public static final DotName RESOURCE_PATH = DotName.createSimple(ResourcePath.class.getName());

    public static final DotName TEMPLATE = DotName.createSimple(Template.class.getName());

    public static final DotName VARIANT_TEMPLATE = DotName.createSimple(VariantTemplate.class.getName());

    static final DotName ITERABLE = DotName.createSimple(Iterable.class.getName());
    static final DotName ITERATOR = DotName.createSimple(Iterator.class.getName());
    static final DotName STREAM = DotName.createSimple(Stream.class.getName());
    static final DotName MAP = DotName.createSimple(Map.class.getName());

    static final DotName MAP_ENTRY = DotName.createSimple(Entry.class.getName());
    static final DotName COLLECTION = DotName.createSimple(Collection.class.getName());
    static final DotName STRING = DotName.createSimple(String.class.getName());

    private static final String MATCH_NAME = "matchName";
    private static final String PRIORITY = "priority";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.QUTE);
    }

    @BuildStep
    void processTemplateErrors(TemplatesAnalysisBuildItem analysis, List<IncorrectExpressionBuildItem> incorrectExpressions,
            BuildProducer<ServiceStartBuildItem> serviceStart) {

        List<String> errors = new ArrayList<>();

        for (IncorrectExpressionBuildItem incorrectExpression : incorrectExpressions) {
            if (incorrectExpression.clazz != null) {
                errors.add(String.format(
                        "Incorrect expression: %s\n\t- property/method [%s] not found on class [%s] nor handled by an extension method\n\t- found in template [%s] on line %s",
                        incorrectExpression.expression, incorrectExpression.property, incorrectExpression.clazz,
                        findTemplatePath(analysis, incorrectExpression.templateId), incorrectExpression.line));
            } else {
                errors.add(String.format(
                        "Incorrect expression %s\n\t @Named bean not found for [%s]\n\t- found in template [%s] on line %s",
                        incorrectExpression.expression, incorrectExpression.property,
                        findTemplatePath(analysis, incorrectExpression.templateId), incorrectExpression.line));
            }
        }

        if (!errors.isEmpty()) {
            StringBuilder message = new StringBuilder("Found template problems (").append(errors.size()).append("):");
            int idx = 1;
            for (String errorMessage : errors) {
                message.append("\n").append("[").append(idx++).append("] ").append(errorMessage);
            }
            throw new TemplateException(message.toString());
        }
    }

    @BuildStep
    AdditionalBeanBuildItem additionalBeans() {
        return AdditionalBeanBuildItem.builder()
                .setUnremovable()
                .addBeanClasses(EngineProducer.class, TemplateProducer.class, VariantTemplateProducer.class, ResourcePath.class,
                        Template.class, TemplateInstance.class, CollectionTemplateExtensions.class,
                        MapTemplateExtensions.class, NumberTemplateExtensions.class)
                .build();
    }

    @BuildStep
    TemplatesAnalysisBuildItem analyzeTemplates(List<TemplatePathBuildItem> templatePaths) {
        long start = System.currentTimeMillis();
        List<TemplateAnalysis> analysis = new ArrayList<>();

        // A dummy engine instance is used to parse and validate all templates during the build
        // The real engine instance is created at startup
        EngineBuilder builder = Engine.builder().addDefaultSectionHelpers();

        // Register user tags
        for (TemplatePathBuildItem path : templatePaths) {
            if (path.isTag()) {
                String tagPath = path.getPath();
                String tagName = tagPath.substring(TemplatePathBuildItem.TAGS.length(), tagPath.length());
                if (tagName.contains(".")) {
                    tagName = tagName.substring(0, tagName.lastIndexOf('.'));
                }
                builder.addSectionHelper(new UserTagSectionHelper.Factory(tagName, tagPath));
            }
        }

        builder.computeSectionHelper(name -> {
            // Create a dummy section helper factory for an uknown section that could be potentially registered at runtime 
            return new SectionHelperFactory<SectionHelper>() {
                @Override
                public SectionHelper initialize(SectionInitContext context) {
                    return new SectionHelper() {
                        @Override
                        public CompletionStage<ResultNode> resolve(SectionResolutionContext context) {
                            return CompletableFuture.completedFuture(ResultNode.NOOP);
                        }
                    };
                }
            };
        });

        builder.addLocator(new TemplateLocator() {
            @Override
            public Optional<TemplateLocation> locate(String id) {
                TemplatePathBuildItem found = templatePaths.stream().filter(p -> p.getPath().equals(id)).findAny().orElse(null);
                if (found != null) {
                    try {
                        byte[] content = Files.readAllBytes(found.getFullPath());
                        return Optional.of(new TemplateLocation() {
                            @Override
                            public Reader read() {
                                return new StringReader(new String(content, StandardCharsets.UTF_8));
                            }

                            @Override
                            public Optional<Variant> getVariant() {
                                return Optional.empty();
                            }
                        });
                    } catch (IOException e) {
                        LOGGER.warn("Unable to read the template from path: " + found.getFullPath(), e);
                    }
                }
                return Optional.empty();
            }
        });

        Engine dummyEngine = builder.build();

        for (TemplatePathBuildItem path : templatePaths) {
            Template template = dummyEngine.getTemplate(path.getPath());
            if (template != null) {
                analysis.add(new TemplateAnalysis(template.getGeneratedId(), template.getExpressions(), path));
            }
        }
        LOGGER.debugf("Finished analysis of %s templates in %s ms", analysis.size(), System.currentTimeMillis() - start);
        return new TemplatesAnalysisBuildItem(analysis);
    }

    @BuildStep
    void validateExpressions(TemplatesAnalysisBuildItem templatesAnalysis, BeanArchiveIndexBuildItem beanArchiveIndex,
            List<TemplateExtensionMethodBuildItem> templateExtensionMethods,
            List<TypeCheckExcludeBuildItem> excludes,
            BuildProducer<IncorrectExpressionBuildItem> incorrectExpressions,
            BuildProducer<ImplicitValueResolverBuildItem> implicitClasses) {

        IndexView index = beanArchiveIndex.getIndex();
        Function<String, String> templateIdToPathFun = new Function<String, String>() {
            @Override
            public String apply(String id) {
                return findTemplatePath(templatesAnalysis, id);
            }
        };

        // Map implicit class -> true if methods were used
        Map<ClassInfo, Boolean> implicitClassToMethodUsed = new HashMap<>();

        for (TemplateAnalysis analysis : templatesAnalysis.getAnalysis()) {
            for (Expression expression : analysis.expressions) {
                if (expression.hasNamespace() || expression.isLiteral()) {
                    continue;
                }
                validateNestedExpressions(null, new HashMap<>(), templateExtensionMethods, excludes, incorrectExpressions,
                        expression, index, implicitClassToMethodUsed, templateIdToPathFun);
            }
        }

        for (Entry<ClassInfo, Boolean> implicit : implicitClassToMethodUsed.entrySet()) {
            implicitClasses.produce(implicit.getValue()
                    ? new ImplicitValueResolverBuildItem(implicit.getKey(), new TemplateDataBuilder().properties(false).build())
                    : new ImplicitValueResolverBuildItem(implicit.getKey()));
        }
    }

    void validateNestedExpressions(ClassInfo rootClazz, Map<String, Match> results,
            List<TemplateExtensionMethodBuildItem> templateExtensionMethods,
            List<TypeCheckExcludeBuildItem> excludes,
            BuildProducer<IncorrectExpressionBuildItem> incorrectExpressions, Expression expression, IndexView index,
            Map<ClassInfo, Boolean> implicitClassToMethodUsed, Function<String, String> templateIdToPathFun) {

        // First validate nested virtual methods
        for (Expression.Part part : expression.getParts()) {
            if (part.isVirtualMethod()) {
                for (Expression param : part.asVirtualMethod().getParameters()) {
                    if (!results.containsKey(param.toOriginalString())) {
                        validateNestedExpressions(null, results, templateExtensionMethods, excludes, incorrectExpressions,
                                param, index, implicitClassToMethodUsed, templateIdToPathFun);
                    }
                }
            }
        }
        // Then validate the expression itself
        Match match = new Match();
        if (rootClazz == null && !expression.hasTypeInfo()) {
            // No type info available or a namespace expression
            results.put(expression.toOriginalString(), match);
            return;
        }

        Iterator<Info> parts = TypeInfos.create(expression, index, templateIdToPathFun).iterator();

        if (rootClazz == null) {
            Info root = parts.next();
            match.clazz = root.asTypeInfo().rawClass;
            match.type = root.asTypeInfo().resolvedType;
            if (root.asTypeInfo().hint != null) {
                processHints(root.asTypeInfo().hint, match, index, expression);
            }
        } else {
            // The first part is the name of the bean
            parts.next();
            match.clazz = rootClazz;
            match.type = Type.create(rootClazz.name(), org.jboss.jandex.Type.Kind.CLASS);
        }

        while (parts.hasNext()) {
            // Now iterate over all parts of the expression and check each part against the current "match class"
            Info info = parts.next();
            if (match.clazz != null) {
                // By default, we only consider properties
                implicitClassToMethodUsed.putIfAbsent(match.clazz, false);
                AnnotationTarget member = null;
                // First try to find java members
                if (info.isVirtualMethod()) {
                    member = findMethod(info.part.asVirtualMethod(), match.clazz, expression, index, templateIdToPathFun,
                            results);
                    if (member != null) {
                        implicitClassToMethodUsed.put(match.clazz, true);
                    }
                } else if (info.isProperty()) {
                    member = findProperty(info.asProperty().name, match.clazz, index);
                }
                // Java member not found - try extension methods
                if (member == null) {
                    member = findTemplateExtensionMethod(info, match.clazz, templateExtensionMethods, expression, index,
                            templateIdToPathFun, results);
                }

                if (member == null) {
                    // Test whether the validation should be skipped
                    Check check = new Check(info.isProperty() ? info.asProperty().name : info.asVirtualMethod().name,
                            match.clazz, info.part.isVirtualMethod() ? info.part.asVirtualMethod().getParameters().size() : -1);
                    if (isExcluded(check, excludes)) {
                        LOGGER.debugf(
                                "Expression part [%s] excluded from validation of [%s] against class [%s]",
                                info.value,
                                expression.toOriginalString(), match.clazz);
                        match.clear();
                        break;
                    }
                }

                if (member == null) {
                    // No member found - incorrect expression
                    incorrectExpressions.produce(new IncorrectExpressionBuildItem(expression.toOriginalString(),
                            info.value, match.clazz.toString(), expression.getOrigin().getLine(),
                            expression.getOrigin().getTemplateGeneratedId()));
                    match.clear();
                    break;
                } else {
                    match.type = resolveType(member, match, index);
                    if (match.type.kind() == org.jboss.jandex.Type.Kind.PRIMITIVE) {
                        break;
                    }
                    if (match.type.kind() == Type.Kind.CLASS || match.type.kind() == Type.Kind.PARAMETERIZED_TYPE) {
                        match.clazz = index.getClassByName(match.type.name());
                    }
                    if (info.isProperty()) {
                        String hint = info.asProperty().hint;
                        if (hint != null) {
                            // For example a loop section needs to validate the type of an element
                            processHints(hint, match, index, expression);
                        }
                    }
                }
            } else {
                LOGGER.debugf(
                        "No match class available - skip further validation for [%s] in expression [%s] in template [%s] on line %s",
                        info.part, expression.toOriginalString(), expression.getOrigin().getTemplateId(),
                        expression.getOrigin().getLine());
                match.clear();
                break;
            }
        }
        results.put(expression.toOriginalString(), match);
    }

    @BuildStep
    void collectTemplateExtensionMethods(BeanArchiveIndexBuildItem beanArchiveIndex,
            BuildProducer<TemplateExtensionMethodBuildItem> extensionMethods) {

        IndexView index = beanArchiveIndex.getIndex();
        Map<MethodInfo, AnnotationInstance> methods = new HashMap<>();
        Map<ClassInfo, AnnotationInstance> classes = new HashMap<>();

        for (AnnotationInstance templateExtension : index.getAnnotations(ExtensionMethodGenerator.TEMPLATE_EXTENSION)) {
            if (templateExtension.target().kind() == Kind.METHOD) {
                methods.put(templateExtension.target().asMethod(), templateExtension);
            } else if (templateExtension.target().kind() == Kind.CLASS) {
                classes.put(templateExtension.target().asClass(), templateExtension);
            }
        }

        // Method-level annotations
        for (Entry<MethodInfo, AnnotationInstance> entry : methods.entrySet()) {
            MethodInfo method = entry.getKey();
            ExtensionMethodGenerator.validate(method);
            produceExtensionMethod(index, extensionMethods, method, entry.getValue());
            LOGGER.debugf("Found template extension method %s declared on %s", method,
                    method.declaringClass().name());
        }

        // Class-level annotations
        for (Entry<ClassInfo, AnnotationInstance> entry : classes.entrySet()) {
            ClassInfo clazz = entry.getKey();
            for (MethodInfo method : clazz.methods()) {
                if (!Modifier.isStatic(method.flags()) || method.returnType().kind() == org.jboss.jandex.Type.Kind.VOID
                        || method.parameters().isEmpty() || Modifier.isPrivate(method.flags())
                        || ValueResolverGenerator.isSynthetic(method.flags())) {
                    // Filter out non-static, synthetic, private and void methods with no params
                    continue;
                }
                if (methods.containsKey(method)) {
                    // Skip methods annotated with @TemplateExtension - method-level annotation takes precedence
                    continue;
                }
                produceExtensionMethod(index, extensionMethods, method, entry.getValue());
                LOGGER.debugf("Found template extension method %s declared on %s", method,
                        method.declaringClass().name());
            }
        }
    }

    private void produceExtensionMethod(IndexView index, BuildProducer<TemplateExtensionMethodBuildItem> extensionMethods,
            MethodInfo method, AnnotationInstance extensionAnnotation) {
        // Analyze matchName and priority so that it could be used during validation 
        String matchName = null;
        AnnotationValue matchNameValue = extensionAnnotation.value(MATCH_NAME);
        if (matchNameValue != null) {
            matchName = matchNameValue.asString();
        }
        if (matchName == null) {
            matchName = method.name();
        }
        int priority = TemplateExtension.DEFAULT_PRIORITY;
        AnnotationValue priorityValue = extensionAnnotation.value(PRIORITY);
        if (priorityValue != null) {
            priority = priorityValue.asInt();
        }
        extensionMethods.produce(new TemplateExtensionMethodBuildItem(method, matchName,
                index.getClassByName(method.parameters().get(0).name()), priority));
    }

    @BuildStep
    void validateBeansInjectedInTemplates(ApplicationArchivesBuildItem applicationArchivesBuildItem,
            TemplatesAnalysisBuildItem analysis, BeanArchiveIndexBuildItem beanArchiveIndex,
            BuildProducer<IncorrectExpressionBuildItem> incorrectExpressions,
            List<TemplateExtensionMethodBuildItem> templateExtensionMethods,
            List<TypeCheckExcludeBuildItem> excludes,
            BeanRegistrationPhaseBuildItem registrationPhase,
            // This producer is needed to ensure the correct ordering, ie. this build step must be executed before the ArC validation step
            BuildProducer<BeanConfiguratorBuildItem> configurators,
            BuildProducer<ImplicitValueResolverBuildItem> implicitClasses) {

        IndexView index = beanArchiveIndex.getIndex();
        Function<String, String> templateIdToPathFun = new Function<String, String>() {
            @Override
            public String apply(String id) {
                return findTemplatePath(analysis, id);
            }
        };
        Set<Expression> injectExpressions = collectInjectExpressions(analysis);

        if (!injectExpressions.isEmpty()) {
            // IMPLEMENTATION NOTE: 
            // We do not support injection of synthetic beans with names 
            // Dependency on the ValidationPhaseBuildItem would result in a cycle in the build chain
            Map<String, BeanInfo> namedBeans = registrationPhase.getContext().beans().withName()
                    .collect(toMap(BeanInfo::getName, Function.identity()));

            Set<Expression> expressions = collectInjectExpressions(analysis);

            // Map implicit class -> true if methods were used
            Map<ClassInfo, Boolean> implicitClassToMethodUsed = new HashMap<>();

            for (Expression expression : expressions) {

                String beanName = expression.getParts().get(0).getName();
                BeanInfo bean = namedBeans.get(beanName);
                if (bean != null) {
                    if (expression.getParts().size() == 1) {
                        // Only the bean needs to be validated
                        continue;
                    }
                    validateNestedExpressions(bean.getImplClazz(), new HashMap<>(), templateExtensionMethods, excludes,
                            incorrectExpressions, expression, index, implicitClassToMethodUsed, templateIdToPathFun);

                } else {
                    // User is injecting a non-existing bean
                    incorrectExpressions.produce(new IncorrectExpressionBuildItem(expression.toOriginalString(),
                            beanName, null, expression.getOrigin().getLine(),
                            expression.getOrigin().getTemplateGeneratedId()));
                }
            }

            for (Entry<ClassInfo, Boolean> implicit : implicitClassToMethodUsed.entrySet()) {
                implicitClasses.produce(implicit.getValue()
                        ? new ImplicitValueResolverBuildItem(implicit.getKey(),
                                new TemplateDataBuilder().properties(false).build())
                        : new ImplicitValueResolverBuildItem(implicit.getKey()));
            }
        }
    }

    private String findTemplatePath(TemplatesAnalysisBuildItem analysis, String id) {
        for (TemplateAnalysis templateAnalysis : analysis.getAnalysis()) {
            if (templateAnalysis.id.equals(id)) {
                return templateAnalysis.path.getPath();
            }
        }
        return null;
    }

    @BuildStep
    void generateValueResolvers(QuteConfig config, BuildProducer<GeneratedClassBuildItem> generatedClass,
            CombinedIndexBuildItem combinedIndex, BeanArchiveIndexBuildItem beanArchiveIndex,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            List<TemplatePathBuildItem> templatePaths,
            List<TemplateExtensionMethodBuildItem> templateExtensionMethods,
            List<ImplicitValueResolverBuildItem> implicitClasses,
            TemplatesAnalysisBuildItem templatesAnalysis,
            BuildProducer<GeneratedValueResolverBuildItem> generatedResolvers,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {

        IndexView index = beanArchiveIndex.getIndex();
        Predicate<String> appClassPredicate = new Predicate<String>() {
            @Override
            public boolean test(String name) {
                if (applicationArchivesBuildItem.getRootArchive().getIndex()
                        .getClassByName(DotName.createSimple(name)) != null) {
                    return true;
                }
                // TODO generated classes?
                return false;
            }
        };
        ClassOutput classOutput = new ClassOutput() {
            @Override
            public void write(String name, byte[] data) {
                int idx = name.lastIndexOf(ExtensionMethodGenerator.SUFFIX);
                if (idx == -1) {
                    idx = name.lastIndexOf(ValueResolverGenerator.SUFFIX);
                }
                String className = name.substring(0, idx).replace("/", ".");
                if (className.contains(ValueResolverGenerator.NESTED_SEPARATOR)) {
                    className = className.replace(ValueResolverGenerator.NESTED_SEPARATOR, "$");
                }
                boolean appClass = appClassPredicate.test(className);
                LOGGER.debugf("Writing %s [appClass=%s]", name, appClass);
                generatedClass.produce(new GeneratedClassBuildItem(appClass, name, data));
            }
        };

        Map<DotName, ClassInfo> nameToClass = new HashMap<>();
        Set<DotName> controlled = new HashSet<>();
        Map<DotName, AnnotationInstance> uncontrolled = new HashMap<>();
        for (AnnotationInstance templateData : index.getAnnotations(ValueResolverGenerator.TEMPLATE_DATA)) {
            processsTemplateData(index, templateData, templateData.target(), controlled, uncontrolled, nameToClass);
        }
        for (AnnotationInstance containerInstance : index.getAnnotations(ValueResolverGenerator.TEMPLATE_DATA_CONTAINER)) {
            for (AnnotationInstance templateData : containerInstance.value().asNestedArray()) {
                processsTemplateData(index, templateData, containerInstance.target(), controlled, uncontrolled, nameToClass);
            }
        }

        for (ImplicitValueResolverBuildItem implicit : implicitClasses) {
            if (controlled.contains(implicit.getClazz().name())) {
                LOGGER.debugf("Implicit value resolver build item ignored: %s is annotated with @TemplateData");
                continue;
            }
            AnnotationInstance templateData = uncontrolled.get(implicit.getClazz().name());
            if (templateData != null) {
                if (!templateData.equals(implicit.getTemplateData())) {
                    throw new IllegalStateException("Multiple implicit value resolver build items produced for "
                            + implicit.getClazz() + " and the synthetic template data is not equal");
                }
                continue;
            }
            uncontrolled.put(implicit.getClazz().name(), implicit.getTemplateData());
            nameToClass.put(implicit.getClazz().name(), implicit.getClazz());
        }

        ValueResolverGenerator generator = ValueResolverGenerator.builder().setIndex(index).setClassOutput(classOutput)
                .setUncontrolled(uncontrolled)
                .build();

        // @TemplateData
        for (DotName name : controlled) {
            generator.generate(nameToClass.get(name));
        }
        // Uncontrolled classes
        for (DotName name : uncontrolled.keySet()) {
            generator.generate(nameToClass.get(name));
        }

        Set<String> generatedTypes = new HashSet<>();
        generatedTypes.addAll(generator.getGeneratedTypes());

        ExtensionMethodGenerator extensionMethodGenerator = new ExtensionMethodGenerator(classOutput);
        for (TemplateExtensionMethodBuildItem templateExtension : templateExtensionMethods) {
            extensionMethodGenerator.generate(templateExtension.getMethod(), templateExtension.getMatchName(),
                    templateExtension.getPriority());
        }
        generatedTypes.addAll(extensionMethodGenerator.getGeneratedTypes());

        LOGGER.debugf("Generated types: %s", generatedTypes);

        for (String generateType : generatedTypes) {
            generatedResolvers.produce(new GeneratedValueResolverBuildItem(generateType));
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, generateType));
        }
    }

    @BuildStep
    void collectTemplates(ApplicationArchivesBuildItem applicationArchivesBuildItem,
            BuildProducer<HotDeploymentWatchedFileBuildItem> watchedPaths,
            BuildProducer<TemplatePathBuildItem> templatePaths,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources)
            throws IOException {
        ApplicationArchive applicationArchive = applicationArchivesBuildItem.getRootArchive();
        String basePath = "templates";
        Path templatesPath = applicationArchive.getChildPath(basePath);

        if (templatesPath != null) {
            scan(templatesPath, templatesPath, basePath + "/", watchedPaths, templatePaths, nativeImageResources);
        }
    }

    @BuildStep
    void validateTemplateInjectionPoints(QuteConfig config, List<TemplatePathBuildItem> templatePaths,
            ValidationPhaseBuildItem validationPhase,
            BuildProducer<ValidationErrorBuildItem> validationErrors) {

        Set<String> filePaths = new HashSet<String>();
        for (TemplatePathBuildItem templatePath : templatePaths) {
            String path = templatePath.getPath();
            filePaths.add(path);
            // Also add version without suffix from the path
            // For example for "items.html" also add "items"
            for (String suffix : config.suffixes) {
                if (path.endsWith(suffix)) {
                    filePaths.add(path.substring(0, path.length() - (suffix.length() + 1)));
                }
            }
        }

        for (InjectionPointInfo injectionPoint : validationPhase.getContext().get(BuildExtension.Key.INJECTION_POINTS)) {

            if (injectionPoint.getRequiredType().name().equals(TEMPLATE)) {

                AnnotationInstance resourcePath = injectionPoint.getRequiredQualifier(RESOURCE_PATH);
                String name;
                if (resourcePath != null) {
                    name = resourcePath.value().asString();
                } else if (injectionPoint.hasDefaultedQualifier()) {
                    name = getName(injectionPoint);
                } else {
                    name = null;
                }
                if (name != null) {
                    // For "@Inject Template items" we try to match "items"
                    // For "@ResourcePath("github/pulls") Template pulls" we try to match "github/pulls"
                    if (filePaths.stream().noneMatch(path -> path.endsWith(name))) {
                        validationErrors.produce(new ValidationErrorBuildItem(
                                new IllegalStateException("No template found for " + injectionPoint.getTargetInfo())));
                    }
                }

            } else if (injectionPoint.getRequiredType().name().equals(VARIANT_TEMPLATE)) {

                AnnotationInstance resourcePath = injectionPoint.getRequiredQualifier(RESOURCE_PATH);
                String name;
                if (resourcePath != null) {
                    name = resourcePath.value().asString();
                } else if (injectionPoint.hasDefaultedQualifier()) {
                    name = getName(injectionPoint);
                } else {
                    name = null;
                }
                if (name != null) {
                    if (filePaths.stream().noneMatch(path -> path.endsWith(name))) {
                        validationErrors.produce(new ValidationErrorBuildItem(
                                new IllegalStateException("No variant template found for " + injectionPoint.getTargetInfo())));
                    }
                }
            }
        }
    }

    @BuildStep
    TemplateVariantsBuildItem collectTemplateVariants(List<TemplatePathBuildItem> templatePaths) throws IOException {
        Set<String> allPaths = templatePaths.stream().map(TemplatePathBuildItem::getPath).collect(Collectors.toSet());
        // item -> [item.html, item.txt]
        Map<String, List<String>> baseToVariants = new HashMap<>();
        for (String path : allPaths) {
            int idx = path.lastIndexOf('.');
            if (idx != -1) {
                String base = path.substring(0, idx);
                List<String> variants = baseToVariants.get(base);
                if (variants == null) {
                    variants = new ArrayList<>();
                    baseToVariants.put(base, variants);
                }
                variants.add(path);
            }
        }
        LOGGER.debugf("Variant templates found: %s", baseToVariants);
        return new TemplateVariantsBuildItem(baseToVariants);
    }

    @BuildStep
    ServiceProviderBuildItem registerPublisherFactory() {
        return new ServiceProviderBuildItem(PublisherFactory.class.getName(), MutinyPublisherFactory.class.getName());
    }

    @BuildStep
    void excludeTypeChecks(BuildProducer<TypeCheckExcludeBuildItem> excludes) {
        // Exclude all checks that involve built-in value resolvers
        // TODO we need a better way to exclude value resolvers that are not template extension methods
        excludes.produce(new TypeCheckExcludeBuildItem(new Predicate<Check>() {
            @Override
            public boolean test(Check check) {
                // RawString
                if (check.isProperty() && check.nameIn("raw", "safe")) {
                    return true;
                }
                // Elvis and ternary operators
                if (check.numberOfParameters == 1 && check.nameIn("?:", "or", ":", "?")) {
                    return true;
                }
                // Collection.contains()
                if (check.numberOfParameters == 1 && check.classNameEquals(COLLECTION) && check.name.equals("contains")) {
                    return true;
                }
                return false;
            }
        }));
    }

    @BuildStep
    @Record(value = STATIC_INIT)
    void initialize(QuteConfig config, BuildProducer<SyntheticBeanBuildItem> syntheticBeans, QuteRecorder recorder,
            List<GeneratedValueResolverBuildItem> generatedValueResolvers, List<TemplatePathBuildItem> templatePaths,
            Optional<TemplateVariantsBuildItem> templateVariants) {

        List<String> templates = new ArrayList<>();
        List<String> tags = new ArrayList<>();
        for (TemplatePathBuildItem templatePath : templatePaths) {
            if (templatePath.isTag()) {
                // tags/myTag.html -> myTag.html
                String tagPath = templatePath.getPath();
                tags.add(tagPath.substring(TemplatePathBuildItem.TAGS.length(), tagPath.length()));
            } else {
                templates.add(templatePath.getPath());
            }
        }
        Map<String, List<String>> variants;
        if (templateVariants.isPresent()) {
            variants = templateVariants.get().getVariants();
        } else {
            variants = Collections.emptyMap();
        }

        syntheticBeans.produce(SyntheticBeanBuildItem.configure(QuteContext.class)
                .supplier(recorder.createContext(config, generatedValueResolvers.stream()
                        .map(GeneratedValueResolverBuildItem::getClassName).collect(Collectors.toList()), templates,
                        tags, variants))
                .done());
        ;
    }

    private Type resolveType(AnnotationTarget member, Match match, IndexView index) {
        Type matchType;
        if (member.kind() == Kind.FIELD) {
            matchType = member.asField().type();
        } else if (member.kind() == Kind.METHOD) {
            matchType = member.asMethod().returnType();
        } else {
            throw new IllegalStateException("Unsupported member type: " + member);
        }
        // If needed attempt to resolve the type variables using the declaring type
        if (Types.containsTypeVariable(matchType)) {
            // First get the type closure of the current match type
            Set<Type> closure = Types.getTypeClosure(match.clazz, Types.buildResolvedMap(
                    match.getParameterizedTypeArguments(), match.getTypeParameters(),
                    new HashMap<>(), index), index);
            DotName declaringClassName = member.kind() == Kind.METHOD ? member.asMethod().declaringClass().name()
                    : member.asField().declaringClass().name();
            // Then find the declaring type with resolved type variables
            Type declaringType = closure.stream()
                    .filter(t -> t.name().equals(declaringClassName)).findAny()
                    .orElse(null);
            if (declaringType != null
                    && declaringType.kind() == org.jboss.jandex.Type.Kind.PARAMETERIZED_TYPE) {
                matchType = Types.resolveTypeParam(matchType,
                        Types.buildResolvedMap(declaringType.asParameterizedType().arguments(),
                                index.getClassByName(declaringType.name()).typeParameters(),
                                Collections.emptyMap(),
                                index),
                        index);
            }
        }
        return matchType;
    }

    void processHints(String helperHint, Match match, IndexView index, Expression expression) {
        if (LoopSectionHelper.Factory.HINT.equals(helperHint)) {
            // Iterable<Item>, Stream<Item> => Item
            // Map<String,Long> => Entry<String,Long>
            processLoopHint(match, index, expression);
        }
    }

    void processLoopHint(Match match, IndexView index, Expression expression) {
        if (match.type.name().equals(DotNames.INTEGER)) {
            return;
        }
        Type matchType = null;
        if (match.type.kind() == Type.Kind.ARRAY) {
            matchType = match.type.asArrayType().component();
        } else if (match.type.kind() == Type.Kind.CLASS || match.type.kind() == Type.Kind.PARAMETERIZED_TYPE) {
            Set<Type> closure = Types.getTypeClosure(match.clazz, Types.buildResolvedMap(
                    match.getParameterizedTypeArguments(), match.getTypeParameters(), new HashMap<>(), index), index);
            Function<Type, Type> firstParamType = t -> t.asParameterizedType().arguments().get(0);
            // Iterable<Item> => Item
            matchType = extractMatchType(closure, ITERABLE, firstParamType);
            if (matchType == null) {
                // Stream<Long> => Long
                matchType = extractMatchType(closure, STREAM, firstParamType);
            }
            if (matchType == null) {
                // Entry<K,V> => Entry<String,Item>
                matchType = extractMatchType(closure, MAP, t -> {
                    Type[] args = new Type[2];
                    args[0] = t.asParameterizedType().arguments().get(0);
                    args[1] = t.asParameterizedType().arguments().get(1);
                    return ParameterizedType.create(MAP_ENTRY, args, null);
                });
            }
            if (matchType == null) {
                // Iterator<Item> => Item
                matchType = extractMatchType(closure, ITERATOR, firstParamType);
            }
        }
        if (matchType != null) {
            match.type = matchType;
            match.clazz = index.getClassByName(match.type.name());
        } else {
            throw new IllegalStateException(String.format(
                    "Unsupported iterable type found in [%s]\n\t- matching type: %s \n\t- found in template [%s] on line %s",
                    expression.toOriginalString(),
                    match.type, expression.getOrigin().getTemplateId(), expression.getOrigin().getLine()));
        }
    }

    Type extractMatchType(Set<Type> closure, DotName matchName, Function<Type, Type> extractFun) {
        Type type = closure.stream().filter(t -> t.name().equals(matchName)).findFirst().orElse(null);
        return type != null ? extractFun.apply(type) : null;
    }

    static class Match {

        ClassInfo clazz;
        Type type;

        List<Type> getParameterizedTypeArguments() {
            return type.kind() == org.jboss.jandex.Type.Kind.PARAMETERIZED_TYPE ? type.asParameterizedType().arguments()
                    : Collections.emptyList();
        }

        List<TypeVariable> getTypeParameters() {
            return clazz.typeParameters();
        }

        void clear() {
            clazz = null;
            type = null;
        }

        boolean isEmpty() {
            return clazz == null;
        }
    }

    private AnnotationTarget findTemplateExtensionMethod(Info info, ClassInfo matchClass,
            List<TemplateExtensionMethodBuildItem> templateExtensionMethods, Expression expression, IndexView index,
            Function<String, String> templateIdToPathFun, Map<String, Match> results) {
        if (!info.isProperty() && !info.isVirtualMethod()) {
            return null;
        }
        String name = info.isProperty() ? info.asProperty().name : info.asVirtualMethod().name;
        for (TemplateExtensionMethodBuildItem extensionMethod : templateExtensionMethods) {
            if (!Types.isAssignableFrom(extensionMethod.getMatchClass().name(), matchClass.name(), index)) {
                // If "Bar extends Foo" then Bar should be matched for the extension method "int get(Foo)"   
                continue;
            }
            if (!extensionMethod.matchesName(name)) {
                // Name does not match
                continue;
            }
            if (info.isVirtualMethod()) {
                // For virtual method validate the number of params and attempt to validate the parameter types if available
                VirtualMethodPart virtualMethod = info.part.asVirtualMethod();
                boolean isVarArgs = ValueResolverGenerator.isVarArgs(extensionMethod.getMethod());
                List<Type> parameters = extensionMethod.getMethod().parameters();
                int lastParamIdx = parameters.size() - 1;
                int realParamSize = parameters.size() - (TemplateExtension.ANY.equals(extensionMethod.getMatchName()) ? 2 : 1);

                if (isVarArgs) {
                    // For varargs methods match the minimal number of params
                    if ((realParamSize - 1) > virtualMethod.getParameters().size()) {
                        continue;
                    }
                } else {
                    if (virtualMethod.getParameters().size() != realParamSize) {
                        // Check number of parameters; some of params of the extension method must be ignored
                        continue;
                    }
                }

                // Check parameter types if available
                boolean matches = true;
                // Skip base and name param if needed
                int idx = TemplateExtension.ANY.equals(extensionMethod.getMatchName()) ? 2 : 1;

                for (Expression param : virtualMethod.getParameters()) {

                    Match result = results.get(param.toOriginalString());
                    if (result != null && !result.isEmpty()) {
                        // Type info available - validate parameter type
                        Type paramType;
                        if (isVarArgs && (idx >= lastParamIdx)) {
                            // Replace the type for varargs methods
                            paramType = parameters.get(lastParamIdx).asArrayType().component();
                        } else {
                            paramType = parameters.get(idx);
                        }
                        if (!Types.isAssignableFrom(result.type,
                                paramType, index)) {
                            matches = false;
                            break;
                        }
                    } else {
                        LOGGER.debugf(
                                "Type info not available - skip validation for parameter [%s] of extension method [%s] for expression [%s] in template [%s] on line %s",
                                extensionMethod.getMethod().parameterName(idx),
                                extensionMethod.getMethod().declaringClass().name() + "#" + extensionMethod.getMethod(),
                                expression.toOriginalString(),
                                templateIdToPathFun.apply(expression.getOrigin().getTemplateId()),
                                expression.getOrigin().getLine());
                    }
                    idx++;
                }
                if (!matches) {
                    continue;
                }
            }
            return extensionMethod.getMethod();
        }
        return null;
    }

    /**
     * Attempts to find a property with the specified name, ie. a public non-static non-synthetic field with the given name or a
     * public non-static non-synthetic method with no params and the given name.
     * 
     * @param name
     * @param clazz
     * @param index
     * @return the property or null
     */
    private AnnotationTarget findProperty(String name, ClassInfo clazz, IndexView index) {
        while (clazz != null) {
            // Fields
            for (FieldInfo field : clazz.fields()) {
                if (Modifier.isPublic(field.flags()) && !Modifier.isStatic(field.flags())
                        && !ValueResolverGenerator.isSynthetic(field.flags()) && field.name().equals(name)) {
                    return field;
                }
            }
            // Methods
            for (MethodInfo method : clazz.methods()) {
                if (Modifier.isPublic(method.flags()) && !Modifier.isStatic(method.flags())
                        && !ValueResolverGenerator.isSynthetic(method.flags()) && (method.name().equals(name)
                                || ValueResolverGenerator.getPropertyName(method.name()).equals(name))) {
                    return method;
                }
            }
            DotName superName = clazz.superName();
            if (superName == null) {
                clazz = null;
            } else {
                clazz = index.getClassByName(clazz.superName());
            }
        }
        return null;
    }

    /**
     * Find a non-static non-synthetic method with the given name, matching number of params and assignable parameter types.
     * 
     * @param virtualMethod
     * @param clazz
     * @param expression
     * @param index
     * @param templateIdToPathFun
     * @param results
     * @return the method or null
     */
    private AnnotationTarget findMethod(VirtualMethodPart virtualMethod, ClassInfo clazz, Expression expression,
            IndexView index, Function<String, String> templateIdToPathFun, Map<String, Match> results) {
        while (clazz != null) {
            for (MethodInfo method : clazz.methods()) {
                if (Modifier.isPublic(method.flags()) && !Modifier.isStatic(method.flags())
                        && !ValueResolverGenerator.isSynthetic(method.flags())
                        && method.name().equals(virtualMethod.getName())) {
                    boolean isVarArgs = ValueResolverGenerator.isVarArgs(method);
                    List<Type> parameters = method.parameters();
                    int lastParamIdx = parameters.size() - 1;

                    if (isVarArgs) {
                        // For varargs methods match the minimal number of params
                        if (lastParamIdx > virtualMethod.getParameters().size()) {
                            continue;
                        }
                    } else {
                        if (virtualMethod.getParameters().size() != parameters.size()) {
                            // Number of params must be equal
                            continue;
                        }
                    }

                    // Check parameter types if available
                    boolean matches = true;
                    byte idx = 0;

                    for (Expression param : virtualMethod.getParameters()) {
                        Match result = results.get(param.toOriginalString());
                        if (result != null && !result.isEmpty()) {
                            // Type info available - validate parameter type
                            Type paramType;
                            if (isVarArgs && idx >= lastParamIdx) {
                                // Replace the type for varargs methods
                                paramType = parameters.get(lastParamIdx).asArrayType().component();
                            } else {
                                paramType = parameters.get(idx);
                            }
                            if (!Types.isAssignableFrom(result.type,
                                    paramType, index)) {
                                matches = false;
                                break;
                            }
                        } else {
                            LOGGER.debugf(
                                    "Type info not available - skip validation for parameter [%s] of method [%s] for expression [%s] in template [%s] on line %s",
                                    method.parameterName(idx),
                                    method.declaringClass().name() + "#" + method,
                                    expression.toOriginalString(),
                                    templateIdToPathFun.apply(expression.getOrigin().getTemplateId()),
                                    expression.getOrigin().getLine());
                        }
                        idx++;
                    }
                    return matches ? method : null;
                }
            }
            DotName superName = clazz.superName();
            if (superName == null || DotNames.OBJECT.equals(superName)) {
                clazz = null;
            } else {
                clazz = index.getClassByName(clazz.superName());
            }
        }
        return null;
    }

    private void processsTemplateData(IndexView index, AnnotationInstance templateData, AnnotationTarget annotationTarget,
            Set<DotName> controlled, Map<DotName, AnnotationInstance> uncontrolled, Map<DotName, ClassInfo> nameToClass) {
        AnnotationValue targetValue = templateData.value("target");
        if (targetValue == null || targetValue.asClass().name().equals(ValueResolverGenerator.TEMPLATE_DATA)) {
            ClassInfo annotationTargetClass = annotationTarget.asClass();
            controlled.add(annotationTargetClass.name());
            nameToClass.put(annotationTargetClass.name(), annotationTargetClass);
        } else {
            ClassInfo uncontrolledClass = index.getClassByName(targetValue.asClass().name());
            if (uncontrolledClass != null) {
                uncontrolled.compute(uncontrolledClass.name(), (c, v) -> {
                    if (v == null) {
                        nameToClass.put(uncontrolledClass.name(), uncontrolledClass);
                        return templateData;
                    }
                    if (!Objects.equals(v.value(ValueResolverGenerator.IGNORE),
                            templateData.value(ValueResolverGenerator.IGNORE))
                            || !Objects.equals(v.value(ValueResolverGenerator.PROPERTIES),
                                    templateData.value(ValueResolverGenerator.PROPERTIES))
                            || !Objects.equals(v.value(ValueResolverGenerator.IGNORE_SUPERCLASSES),
                                    templateData.value(ValueResolverGenerator.IGNORE_SUPERCLASSES))) {
                        throw new IllegalStateException(
                                "Multiple unequal @TemplateData declared for " + c + ": " + v + " and " + templateData);
                    }
                    return v;
                });
            } else {
                LOGGER.warnf("@TemplateData#target() not available: %s", annotationTarget.asClass().name());
            }
        }
    }

    private Set<Expression> collectInjectExpressions(TemplatesAnalysisBuildItem analysis) {
        Set<Expression> injectExpressions = new HashSet<>();
        for (TemplateAnalysis template : analysis.getAnalysis()) {
            injectExpressions.addAll(collectInjectExpressions(template));
        }
        return injectExpressions;
    }

    private Set<Expression> collectInjectExpressions(TemplateAnalysis analysis) {
        Set<Expression> injectExpressions = new HashSet<>();
        for (Expression expression : analysis.expressions) {
            collectInjectExpressions(expression, injectExpressions);
        }
        return injectExpressions;
    }

    private void collectInjectExpressions(Expression expression, Set<Expression> injectExpressions) {
        if (expression.isLiteral()) {
            return;
        }
        if (EngineProducer.INJECT_NAMESPACE.equals(expression.getNamespace())) {
            injectExpressions.add(expression);
        }
        for (Expression.Part part : expression.getParts()) {
            if (part.isVirtualMethod()) {
                for (Expression param : part.asVirtualMethod().getParameters()) {
                    collectInjectExpressions(param, injectExpressions);
                }
            }
        }
    }

    public static String getName(InjectionPointInfo injectionPoint) {
        if (injectionPoint.isField()) {
            return injectionPoint.getTarget().asField().name();
        } else if (injectionPoint.isParam()) {
            String name = injectionPoint.getTarget().asMethod().parameterName(injectionPoint.getPosition());
            return name == null ? injectionPoint.getTarget().asMethod().name() : name;
        }
        throw new IllegalArgumentException();
    }

    private static void produceTemplateBuildItems(BuildProducer<TemplatePathBuildItem> templatePaths,
            BuildProducer<HotDeploymentWatchedFileBuildItem> watchedPaths,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources, String basePath, String filePath,
            Path originalPath) {
        if (filePath.isEmpty()) {
            return;
        }
        String fullPath = basePath + filePath;
        LOGGER.debugf("Produce template build items [filePath: %s, fullPath: %s, originalPath: %s", filePath, fullPath,
                originalPath);
        // NOTE: we cannot just drop the template because a template param can be added 
        watchedPaths.produce(new HotDeploymentWatchedFileBuildItem(fullPath, true));
        nativeImageResources.produce(new NativeImageResourceBuildItem(fullPath));
        templatePaths.produce(new TemplatePathBuildItem(filePath, originalPath));
    }

    private void scan(Path root, Path directory, String basePath, BuildProducer<HotDeploymentWatchedFileBuildItem> watchedPaths,
            BuildProducer<TemplatePathBuildItem> templatePaths,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources)
            throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            Iterator<Path> iter = files.iterator();
            while (iter.hasNext()) {
                Path filePath = iter.next();
                if (Files.isRegularFile(filePath)) {
                    LOGGER.debugf("Found template: %s", filePath);
                    String templatePath = root.relativize(filePath).toString();
                    if (File.separatorChar != '/') {
                        templatePath = templatePath.replace(File.separatorChar, '/');
                    }
                    produceTemplateBuildItems(templatePaths, watchedPaths, nativeImageResources, basePath, templatePath,
                            filePath);
                } else if (Files.isDirectory(filePath)) {
                    LOGGER.debugf("Scan directory: %s", filePath);
                    scan(root, filePath, basePath, watchedPaths, templatePaths, nativeImageResources);
                }
            }
        }
    }

    private boolean isExcluded(Check check, List<TypeCheckExcludeBuildItem> excludes) {
        for (TypeCheckExcludeBuildItem exclude : excludes) {
            if (exclude.getPredicate().test(check)) {
                return true;
            }
        }
        return false;
    }

}
