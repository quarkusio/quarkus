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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import io.quarkus.qute.Expression;
import io.quarkus.qute.LoopSectionHelper;
import io.quarkus.qute.PublisherFactory;
import io.quarkus.qute.ResultNode;
import io.quarkus.qute.SectionHelper;
import io.quarkus.qute.SectionHelperFactory;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateException;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.TemplateLocator;
import io.quarkus.qute.Variant;
import io.quarkus.qute.api.ResourcePath;
import io.quarkus.qute.api.VariantTemplate;
import io.quarkus.qute.deployment.TemplatesAnalysisBuildItem.TemplateAnalysis;
import io.quarkus.qute.generator.ExtensionMethodGenerator;
import io.quarkus.qute.generator.ValueResolverGenerator;
import io.quarkus.qute.runtime.BuiltinTemplateExtensions;
import io.quarkus.qute.runtime.EngineProducer;
import io.quarkus.qute.runtime.QuteConfig;
import io.quarkus.qute.runtime.QuteRecorder;
import io.quarkus.qute.runtime.QuteRecorder.QuteContext;
import io.quarkus.qute.runtime.TemplateProducer;
import io.quarkus.qute.runtime.VariantTemplateProducer;
import io.quarkus.qute.rxjava.RxjavaPublisherFactory;

public class QuteProcessor {

    private static final Logger LOGGER = Logger.getLogger(QuteProcessor.class);

    public static final DotName RESOURCE_PATH = DotName.createSimple(ResourcePath.class.getName());
    public static final DotName TEMPLATE = DotName.createSimple(Template.class.getName());
    public static final DotName VARIANT_TEMPLATE = DotName.createSimple(VariantTemplate.class.getName());

    static final DotName ITERABLE = DotName.createSimple(Iterable.class.getName());
    static final DotName STREAM = DotName.createSimple(Stream.class.getName());
    static final DotName MAP = DotName.createSimple(Map.class.getName());
    static final DotName MAP_ENTRY = DotName.createSimple(Entry.class.getName());

    private static final String MATCH_NAME = "matchName";

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
                        "Incorrect expression: %s\n\t- property [%s] not found on class [%s] nor handled by an extension method\n\t- found in template [%s] on line %s",
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
            StringBuilder message = new StringBuilder("Found " + errors.size() + " template problems: ");
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
                        Template.class, TemplateInstance.class, BuiltinTemplateExtensions.class)
                .build();
    }

    @BuildStep
    TemplatesAnalysisBuildItem analyzeTemplates(List<TemplatePathBuildItem> templatePaths) {
        long start = System.currentTimeMillis();
        List<TemplateAnalysis> analysis = new ArrayList<>();

        // A dummy engine instance is used to parse and validate all templates during the build. The real engine instance is created at startup.
        Engine dummyEngine = Engine.builder().addDefaultSectionHelpers().computeSectionHelper(name -> {
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
        }).addLocator(new TemplateLocator() {

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
                ;
                return Optional.empty();
            }
        }).build();

        for (TemplatePathBuildItem path : templatePaths) {
            Template template = dummyEngine.getTemplate(path.getPath());
            if (template != null) {
                analysis.add(new TemplateAnalysis(template.getGeneratedId(), template.getExpressions(), path));
            }
        }
        LOGGER.debugf("Finished analysis of %s templates  in %s ms", analysis.size(), System.currentTimeMillis() - start);
        return new TemplatesAnalysisBuildItem(analysis);
    }

    @BuildStep
    void validateExpressions(TemplatesAnalysisBuildItem templatesAnalysis, BeanArchiveIndexBuildItem beanArchiveIndex,
            List<TemplateExtensionMethodBuildItem> templateExtensionMethods,
            List<TypeCheckExcludeBuildItem> excludes,
            BuildProducer<IncorrectExpressionBuildItem> incorrectExpressions,
            BuildProducer<ImplicitValueResolverBuildItem> requiredClasses) {

        IndexView index = beanArchiveIndex.getIndex();
        Function<String, String> templateIdToPathFun = new Function<String, String>() {
            @Override
            public String apply(String id) {
                return findTemplatePath(templatesAnalysis, id);
            }
        };

        for (TemplateAnalysis analysis : templatesAnalysis.getAnalysis()) {
            for (Expression expression : analysis.expressions) {
                if (expression.typeCheckInfo == null) {
                    continue;
                }
                TypeCheckInfo typeCheckInfo = TypeCheckInfo.create(expression, index, templateIdToPathFun);
                if (typeCheckInfo.parts.isEmpty()) {
                    continue;
                }
                Iterator<String> parts = typeCheckInfo.parts.iterator();
                Match match = new Match();
                match.clazz = typeCheckInfo.rawClass;
                match.type = typeCheckInfo.resolvedType;

                String rootHint = typeCheckInfo.getHelperHint(TypeCheckInfo.ROOT_HINT);
                if (rootHint != null) {
                    processHints(rootHint, match, index);
                }

                while (parts.hasNext()) {
                    // Now iterate over all parts of the expression and check each part against the current "match class"
                    String name = parts.next();
                    if (match.clazz != null) {
                        requiredClasses.produce(new ImplicitValueResolverBuildItem(match.clazz));
                        // TODO we don't validate virtual methods atm
                        if (name.contains("(")) {
                            break;
                        }
                        AnnotationTarget member = findProperty(name, match.clazz, index);
                        if (member == null) {
                            member = findTemplateExtensionMethod(name, match.clazz, templateExtensionMethods);
                        }
                        if (member == null && excludes.stream().anyMatch(e -> e.getPredicate().test(name, match.clazz))) {
                            LOGGER.debugf("No property found for %s in [%s] but it is intentionally ignored", name,
                                    expression.toOriginalString(), match.clazz);
                            break;
                        }
                        if (member == null) {
                            incorrectExpressions.produce(new IncorrectExpressionBuildItem(expression.toOriginalString(),
                                    name, match.clazz.toString(), expression.origin.getLine(),
                                    expression.origin.getTemplateId()));
                            break;
                        } else if (parts.hasNext()) {
                            match.type = resolveType(member, match, index);
                            if (match.type.kind() == org.jboss.jandex.Type.Kind.PRIMITIVE) {
                                break;
                            }
                            match.clazz = index.getClassByName(match.type.name());

                            String helperHint = typeCheckInfo.getHelperHint(name);
                            if (helperHint != null) {
                                // For example a loop section needs to validate the type of an element
                                processHints(helperHint, match, index);
                            }
                        }
                    } else {
                        // No match class - skip further validation
                        break;
                    }
                }
            }
        }
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

        for (Entry<MethodInfo, AnnotationInstance> entry : methods.entrySet()) {
            MethodInfo method = entry.getKey();
            ExtensionMethodGenerator.validate(method);
            produceExtensionMethod(index, extensionMethods, method, entry.getValue());
            LOGGER.debugf("Found template extension method %s declared on %s", method,
                    method.declaringClass().name());
        }

        for (Entry<ClassInfo, AnnotationInstance> entry : classes.entrySet()) {
            ClassInfo clazz = entry.getKey();
            for (MethodInfo method : clazz.methods()) {
                if (!Modifier.isStatic(method.flags()) || method.returnType().kind() == org.jboss.jandex.Type.Kind.VOID
                        || method.parameters().isEmpty() || Modifier.isPrivate(method.flags()) || methods.containsKey(method)) {
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
        String matchName = null;
        AnnotationValue matchNameValue = extensionAnnotation.value(MATCH_NAME);
        if (matchNameValue != null) {
            matchName = matchNameValue.asString();
        }
        if (matchName == null) {
            matchName = method.name();
        }
        extensionMethods.produce(new TemplateExtensionMethodBuildItem(method, matchName,
                index.getClassByName(method.parameters().get(0).name())));
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
            BuildProducer<ImplicitValueResolverBuildItem> requiredClasses) {

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
            for (Expression expression : expressions) {

                String beanName = expression.parts.get(0);
                BeanInfo bean = namedBeans.get(beanName);
                if (bean != null) {
                    if (expression.parts.size() == 1) {
                        continue;
                    }
                    TypeCheckInfo typeCheckInfo = TypeCheckInfo.create(expression, index, templateIdToPathFun);
                    if (typeCheckInfo.parts.isEmpty()) {
                        continue;
                    }

                    Iterator<String> parts = expression.parts.listIterator(1);
                    Match match = new Match();
                    match.clazz = bean.getImplClazz();

                    while (parts.hasNext()) {
                        // Now iterate over all parts of the expression and check each part against the current "match class"
                        String name = parts.next();
                        if (match.clazz != null) {
                            requiredClasses.produce(new ImplicitValueResolverBuildItem(match.clazz));

                            // TODO we don't validate virtual methods atm
                            if (name.contains("(")) {
                                break;
                            }
                            AnnotationTarget member = findProperty(name, match.clazz, index);
                            if (member == null) {
                                member = findTemplateExtensionMethod(name, match.clazz, templateExtensionMethods);
                            }
                            if (member == null
                                    && excludes.stream().anyMatch(e -> e.getPredicate().test(name, match.clazz))) {
                                LOGGER.debugf("No property found for %s in [%s] but it is intentionally ignored", name,
                                        expression.toOriginalString(), match.clazz);
                                break;
                            }
                            if (member == null) {
                                incorrectExpressions.produce(new IncorrectExpressionBuildItem(expression.toOriginalString(),
                                        name, match.clazz.toString(), expression.origin.getLine(),
                                        expression.origin.getTemplateId()));
                                break;
                            } else {
                                match.type = resolveType(member, match, index);
                                if (match.type.kind() == org.jboss.jandex.Type.Kind.PRIMITIVE) {
                                    break;
                                }
                                match.clazz = index.getClassByName(match.type.name());

                                String helperHint = typeCheckInfo.getHelperHint(name);
                                if (helperHint != null) {
                                    // For example loop section needs to validate the type of an element
                                    processHints(helperHint, match, index);
                                }
                            }
                        } else {
                            // No match class - skip further validation
                            break;
                        }
                    }

                } else {
                    // User is injecting a non-existing bean
                    incorrectExpressions.produce(new IncorrectExpressionBuildItem(expression.toOriginalString(),
                            beanName, null, expression.origin.getLine(),
                            expression.origin.getTemplateId()));
                }
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
            List<ImplicitValueResolverBuildItem> requiredClasses,
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

        Set<ClassInfo> controlled = new HashSet<>();
        Map<ClassInfo, AnnotationInstance> uncontrolled = new HashMap<>();
        for (AnnotationInstance templateData : index.getAnnotations(ValueResolverGenerator.TEMPLATE_DATA)) {
            processsTemplateData(index, templateData, templateData.target(), controlled, uncontrolled);
        }
        for (AnnotationInstance containerInstance : index.getAnnotations(ValueResolverGenerator.TEMPLATE_DATA_CONTAINER)) {
            for (AnnotationInstance templateData : containerInstance.value().asNestedArray()) {
                processsTemplateData(index, templateData, containerInstance.target(), controlled, uncontrolled);
            }
        }
        for (ImplicitValueResolverBuildItem required : requiredClasses) {
            if (!controlled.contains(required.getClazz()) && !uncontrolled.containsKey(required.getClazz())) {
                controlled.add(required.getClazz());
            }
        }

        ValueResolverGenerator generator = ValueResolverGenerator.builder().setIndex(index).setClassOutput(classOutput)
                .setUncontrolled(uncontrolled)
                .build();

        // @TemplateData
        for (ClassInfo data : controlled) {
            generator.generate(data);
        }
        // Uncontrolled classes
        for (ClassInfo data : uncontrolled.keySet()) {
            generator.generate(data);
        }

        Set<String> generatedTypes = new HashSet<>();
        generatedTypes.addAll(generator.getGeneratedTypes());

        ExtensionMethodGenerator extensionMethodGenerator = new ExtensionMethodGenerator(classOutput);
        for (TemplateExtensionMethodBuildItem templateExtension : templateExtensionMethods) {
            extensionMethodGenerator.generate(templateExtension.getMethod(), templateExtension.getMatchName());
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
        return new ServiceProviderBuildItem(PublisherFactory.class.getName(), RxjavaPublisherFactory.class.getName());
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

    void processHints(String helperHint, Match match, IndexView index) {
        if (LoopSectionHelper.Factory.HINT.equals(helperHint)) {
            // Iterable<Item>, Stream<Item> => Item
            // Map<String,Long> => Entry<String,Long>
            processLoopHint(match, index);
        }
    }

    void processLoopHint(Match match, IndexView index) {
        Set<Type> closure = Types.getTypeClosure(match.clazz, Types.buildResolvedMap(
                match.getParameterizedTypeArguments(), match.getTypeParameters(), new HashMap<>(), index), index);
        Type matchType = null;
        Type iterableType = closure.stream().filter(t -> t.name().equals(ITERABLE)).findFirst().orElse(null);
        if (iterableType != null) {
            // Iterable<Item> => Item
            matchType = iterableType.asParameterizedType().arguments().get(0);
        } else {
            Type streamType = closure.stream().filter(t -> t.name().equals(STREAM)).findFirst().orElse(null);
            if (streamType != null) {
                // Stream<Long> => Long
                matchType = streamType.asParameterizedType().arguments().get(0);
            } else {
                Type mapType = closure.stream().filter(t -> t.name().equals(MAP)).findFirst().orElse(null);
                if (mapType != null) {
                    // Entry<K,V> => Entry<String,Item>
                    Type[] args = new Type[2];
                    args[0] = mapType.asParameterizedType().arguments().get(0);
                    args[1] = mapType.asParameterizedType().arguments().get(1);
                    matchType = ParameterizedType.create(MAP_ENTRY, args, null);
                }
            }
        }
        if (matchType != null) {
            match.type = matchType;
            match.clazz = index.getClassByName(match.type.name());
        } else {
            // TODO better error reporting
            throw new IllegalStateException("Unable to process the loop section hint for type: " + match.type);
        }
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
    }

    private AnnotationTarget findTemplateExtensionMethod(String name, ClassInfo matchClass,
            List<TemplateExtensionMethodBuildItem> templateExtensionMethods) {
        for (TemplateExtensionMethodBuildItem templateExtensionMethod : templateExtensionMethods) {
            if (templateExtensionMethod.matchesClass(matchClass) && templateExtensionMethod.matchesName(name)) {
                return templateExtensionMethod.getMethod();
            }
        }
        return null;
    }

    private AnnotationTarget findProperty(String property, ClassInfo clazz, IndexView index) {
        int start = property.indexOf("(");
        if (start != -1) {
            // TODO validate virtual method parameters?
            property = property.substring(0, start);
        }
        while (clazz != null) {
            // Fields
            for (FieldInfo field : clazz.fields()) {
                if (Modifier.isPublic(field.flags()) && !Modifier.isStatic(field.flags())
                        && !ValueResolverGenerator.isSynthetic(field.flags()) && field.name().equals(property)) {
                    return field;
                }
            }
            // Methods
            for (MethodInfo method : clazz.methods()) {
                if (Modifier.isPublic(method.flags()) && !Modifier.isStatic(method.flags())
                        && !ValueResolverGenerator.isSynthetic(method.flags()) && (method.name().equals(property)
                                || ValueResolverGenerator.getPropertyName(method.name()).equals(property))) {
                    return method;
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
            Set<ClassInfo> controlled, Map<ClassInfo, AnnotationInstance> uncontrolled) {
        AnnotationValue targetValue = templateData.value("target");
        if (targetValue == null || targetValue.asClass().name().equals(ValueResolverGenerator.TEMPLATE_DATA)) {
            controlled.add(annotationTarget.asClass());
        } else {
            ClassInfo uncontrolledClass = index.getClassByName(targetValue.asClass().name());
            if (uncontrolledClass != null) {
                uncontrolled.compute(uncontrolledClass, (c, v) -> {
                    if (v == null) {
                        return templateData;
                    }
                    // Merge annotation values
                    AnnotationValue ignoreValue = templateData.value("ignore");
                    if (ignoreValue == null || !ignoreValue.equals(v.value("ignore"))) {
                        ignoreValue = AnnotationValue.createArrayValue("ignore", new AnnotationValue[] {});
                    }
                    AnnotationValue propertiesValue = templateData.value("properties");
                    if (propertiesValue == null || propertiesValue.equals(v.value("properties"))) {
                        propertiesValue = AnnotationValue.createBooleanValue("properties", false);
                    }
                    return AnnotationInstance.create(templateData.name(), templateData.target(),
                            new AnnotationValue[] { ignoreValue, propertiesValue });
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
            if (expression.literal != null) {
                continue;
            }
            if (EngineProducer.INJECT_NAMESPACE.equals(expression.namespace)) {
                injectExpressions.add(expression);
            }
        }
        return injectExpressions;
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

}
