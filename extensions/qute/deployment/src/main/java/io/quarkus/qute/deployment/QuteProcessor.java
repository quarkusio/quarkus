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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
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
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.qute.Engine;
import io.quarkus.qute.EngineBuilder;
import io.quarkus.qute.Expression;
import io.quarkus.qute.Expression.VirtualMethodPart;
import io.quarkus.qute.Expressions;
import io.quarkus.qute.LoopSectionHelper;
import io.quarkus.qute.ParserHelper;
import io.quarkus.qute.ParserHook;
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
import io.quarkus.qute.WhenSectionHelper;
import io.quarkus.qute.api.CheckedTemplate;
import io.quarkus.qute.api.ResourcePath;
import io.quarkus.qute.deployment.TemplatesAnalysisBuildItem.TemplateAnalysis;
import io.quarkus.qute.deployment.TypeCheckExcludeBuildItem.TypeCheck;
import io.quarkus.qute.deployment.TypeInfos.Info;
import io.quarkus.qute.generator.ExtensionMethodGenerator;
import io.quarkus.qute.generator.ExtensionMethodGenerator.NamespaceResolverCreator;
import io.quarkus.qute.generator.ExtensionMethodGenerator.NamespaceResolverCreator.ResolveCreator;
import io.quarkus.qute.generator.ValueResolverGenerator;
import io.quarkus.qute.runtime.ContentTypes;
import io.quarkus.qute.runtime.EngineProducer;
import io.quarkus.qute.runtime.QuteConfig;
import io.quarkus.qute.runtime.QuteRecorder;
import io.quarkus.qute.runtime.QuteRecorder.QuteContext;
import io.quarkus.qute.runtime.TemplateProducer;
import io.quarkus.qute.runtime.extensions.CollectionTemplateExtensions;
import io.quarkus.qute.runtime.extensions.ConfigTemplateExtensions;
import io.quarkus.qute.runtime.extensions.MapTemplateExtensions;
import io.quarkus.qute.runtime.extensions.NumberTemplateExtensions;
import io.quarkus.qute.runtime.extensions.TimeTemplateExtensions;

public class QuteProcessor {

    public static final DotName RESOURCE_PATH = Names.RESOURCE_PATH;

    private static final Logger LOGGER = Logger.getLogger(QuteProcessor.class);

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.QUTE);
    }

    @BuildStep
    void processTemplateErrors(TemplatesAnalysisBuildItem analysis, List<IncorrectExpressionBuildItem> incorrectExpressions,
            BuildProducer<ServiceStartBuildItem> serviceStart) {

        List<TemplateException> errors = new ArrayList<>();

        for (IncorrectExpressionBuildItem incorrectExpression : incorrectExpressions) {
            if (incorrectExpression.reason != null) {
                errors.add(new TemplateException(incorrectExpression.origin, String.format(
                        "Incorrect expression found: {%s}\n\t- %s\n\t- at %s:%s",
                        incorrectExpression.expression, incorrectExpression.reason,
                        findTemplatePath(analysis, incorrectExpression.origin.getTemplateGeneratedId()),
                        incorrectExpression.origin.getLine())));
            } else if (incorrectExpression.clazz != null) {
                errors.add(new TemplateException(incorrectExpression.origin, String.format(
                        "Incorrect expression found: {%s}\n\t- property/method [%s] not found on class [%s] nor handled by an extension method\n\t- at %s:%s",
                        incorrectExpression.expression, incorrectExpression.property, incorrectExpression.clazz,
                        findTemplatePath(analysis, incorrectExpression.origin.getTemplateGeneratedId()),
                        incorrectExpression.origin.getLine())));
            } else {
                errors.add(new TemplateException(incorrectExpression.origin, String.format(
                        "Incorrect expression found: {%s}\n\t- @Named bean not found for [%s]\n\t- at %s:%s",
                        incorrectExpression.expression, incorrectExpression.property,
                        findTemplatePath(analysis, incorrectExpression.origin.getTemplateGeneratedId()),
                        incorrectExpression.origin.getLine())));
            }
        }

        if (!errors.isEmpty()) {
            StringBuilder message = new StringBuilder("Found template problems (").append(errors.size()).append("):");
            int idx = 1;
            for (TemplateException error : errors) {
                message.append("\n").append("[").append(idx++).append("] ").append(error.getMessage());
            }
            TemplateException exception = new TemplateException(message.toString());
            for (TemplateException error : errors) {
                exception.addSuppressed(error);
            }
            throw exception;
        }
    }

    @BuildStep
    AdditionalBeanBuildItem additionalBeans() {
        return AdditionalBeanBuildItem.builder()
                .setUnremovable()
                .addBeanClasses(EngineProducer.class, TemplateProducer.class, ContentTypes.class, ResourcePath.class,
                        Template.class, TemplateInstance.class, CollectionTemplateExtensions.class,
                        MapTemplateExtensions.class, NumberTemplateExtensions.class, ConfigTemplateExtensions.class,
                        TimeTemplateExtensions.class)
                .build();
    }

    @BuildStep
    List<CheckedTemplateBuildItem> collectTemplateTypeInfo(BeanArchiveIndexBuildItem index,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            List<TemplatePathBuildItem> templatePaths,
            List<CheckedTemplateAdapterBuildItem> templateAdaptorBuildItems) {
        List<CheckedTemplateBuildItem> ret = new ArrayList<>();

        Map<DotName, CheckedTemplateAdapter> adaptors = new HashMap<>();
        for (CheckedTemplateAdapterBuildItem templateAdaptorBuildItem : templateAdaptorBuildItems) {
            adaptors.put(DotName.createSimple(templateAdaptorBuildItem.adapter.templateInstanceBinaryName().replace('/', '.')),
                    templateAdaptorBuildItem.adapter);
        }
        String supportedAdaptors;
        if (adaptors.isEmpty()) {
            supportedAdaptors = Names.TEMPLATE_INSTANCE + " is supported";
        } else {
            StringBuffer strbuf = new StringBuffer(Names.TEMPLATE_INSTANCE.toString());
            List<String> adaptorsList = new ArrayList<>(adaptors.size());
            for (DotName name : adaptors.keySet()) {
                adaptorsList.add(name.toString());
            }
            Collections.sort(adaptorsList);
            for (String name : adaptorsList) {
                strbuf.append(", ").append(name);
            }
            supportedAdaptors = strbuf.append(" are supported").toString();
        }

        // template path -> checked template method
        Map<String, MethodInfo> checkedTemplateMethods = new HashMap<>();

        for (AnnotationInstance annotation : index.getIndex().getAnnotations(Names.CHECKED_TEMPLATE)) {
            if (annotation.target().kind() != Kind.CLASS)
                continue;
            ClassInfo classInfo = annotation.target().asClass();
            NativeCheckedTemplateEnhancer enhancer = new NativeCheckedTemplateEnhancer();
            for (MethodInfo methodInfo : classInfo.methods()) {
                // only keep native static methods
                if (!Modifier.isStatic(methodInfo.flags())
                        || !Modifier.isNative(methodInfo.flags())) {
                    continue;
                }
                // check its return type
                if (methodInfo.returnType().kind() != Type.Kind.CLASS) {
                    throw new TemplateException("Incompatible checked template return type: " + methodInfo.returnType()
                            + " only " + supportedAdaptors);
                }
                DotName returnTypeName = methodInfo.returnType().asClassType().name();
                CheckedTemplateAdapter adaptor = null;
                // if it's not the default template instance, try to find an adapter
                if (!returnTypeName.equals(Names.TEMPLATE_INSTANCE)) {
                    adaptor = adaptors.get(returnTypeName);
                    if (adaptor == null)
                        throw new TemplateException("Incompatible checked template return type: " + methodInfo.returnType()
                                + " only " + supportedAdaptors);
                }

                StringBuilder templatePathBuilder = new StringBuilder();
                AnnotationValue basePathValue = annotation.value("basePath");
                if (basePathValue != null && !basePathValue.asString().equals(CheckedTemplate.DEFAULTED)) {
                    templatePathBuilder.append(basePathValue.asString());
                } else if (classInfo.enclosingClass() != null) {
                    ClassInfo enclosingClass = index.getIndex().getClassByName(classInfo.enclosingClass());
                    templatePathBuilder.append(enclosingClass.simpleName());
                }
                if (templatePathBuilder.length() > 0 && templatePathBuilder.charAt(templatePathBuilder.length() - 1) != '/') {
                    templatePathBuilder.append('/');
                }
                String templatePath = templatePathBuilder.append(methodInfo.name()).toString();
                MethodInfo checkedTemplateMethod = checkedTemplateMethods.putIfAbsent(templatePath, methodInfo);
                if (checkedTemplateMethod != null) {
                    throw new TemplateException(
                            String.format(
                                    "Multiple checked template methods exist for the template path %s:\n\t- %s: %s\n\t- %s: %s",
                                    templatePath, methodInfo.declaringClass().name(), methodInfo,
                                    checkedTemplateMethod.declaringClass().name(), checkedTemplateMethod));
                }
                checkTemplatePath(templatePath, templatePaths, classInfo, methodInfo);

                Map<String, String> bindings = new HashMap<>();
                List<Type> parameters = methodInfo.parameters();
                List<String> parameterNames = new ArrayList<>(parameters.size());
                for (int i = 0; i < parameters.size(); i++) {
                    Type type = parameters.get(i);
                    String name = methodInfo.parameterName(i);
                    if (name == null) {
                        throw new TemplateException("Parameter names not recorded for " + classInfo.name()
                                + ": compile the class with -parameters");
                    }
                    bindings.put(name, JandexUtil.getBoxedTypeName(type));
                    parameterNames.add(name);
                }
                ret.add(new CheckedTemplateBuildItem(templatePath, bindings));
                enhancer.implement(methodInfo, templatePath, parameterNames, adaptor);
            }
            transformers.produce(new BytecodeTransformerBuildItem(classInfo.name().toString(),
                    enhancer));
        }

        return ret;
    }

    private void checkTemplatePath(String templatePath, List<TemplatePathBuildItem> templatePaths, ClassInfo enclosingClass,
            MethodInfo methodInfo) {
        for (TemplatePathBuildItem templatePathBuildItem : templatePaths) {
            // perfect match
            if (templatePathBuildItem.getPath().equals(templatePath)) {
                return;
            }
            // if our templatePath is "Foo/hello", make it match "Foo/hello.txt"
            // if they're not equal and they start with our path, there must be something left after
            if (templatePathBuildItem.getPath().startsWith(templatePath)
                    // check that we have an extension, let variant matching work later
                    && templatePathBuildItem.getPath().charAt(templatePath.length()) == '.') {
                return;
            }
        }
        throw new TemplateException(
                "Declared template " + templatePath + " could not be found. Either add it or delete its declaration in "
                        + enclosingClass.name().toString('.') + "." + methodInfo.name());
    }

    @BuildStep
    TemplatesAnalysisBuildItem analyzeTemplates(List<TemplatePathBuildItem> templatePaths,
            List<CheckedTemplateBuildItem> checkedTemplates, List<MessageBundleMethodBuildItem> messageBundleMethods) {
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
        }).addParserHook(new ParserHook() {

            @Override
            public void beforeParsing(ParserHelper parserHelper, String id) {
                if (id != null) {
                    for (CheckedTemplateBuildItem checkedTemplate : checkedTemplates) {
                        // FIXME: check for dot/extension?
                        if (id.startsWith(checkedTemplate.templateId)) {
                            for (Entry<String, String> entry : checkedTemplate.bindings.entrySet()) {
                                parserHelper.addParameter(entry.getKey(), entry.getValue());
                            }
                        }
                    }

                    // Add params to message bundle templates
                    for (MessageBundleMethodBuildItem messageBundleMethod : messageBundleMethods) {
                        if (id.equals(messageBundleMethod.getTemplateId())) {
                            MethodInfo method = messageBundleMethod.getMethod();
                            for (ListIterator<Type> it = method.parameters().listIterator(); it.hasNext();) {
                                Type paramType = it.next();
                                parserHelper.addParameter(method.parameterName(it.previousIndex()),
                                        JandexUtil.getBoxedTypeName(paramType));
                            }
                            break;
                        }
                    }
                }
            }

        }).build();

        Engine dummyEngine = builder.build();

        for (TemplatePathBuildItem path : templatePaths) {
            Template template = dummyEngine.getTemplate(path.getPath());
            if (template != null) {
                analysis.add(new TemplateAnalysis(null, template.getGeneratedId(), template.getExpressions(), path.getPath()));
            }
        }

        // Message bundle templates
        for (MessageBundleMethodBuildItem messageBundleMethod : messageBundleMethods) {
            Template template = dummyEngine.parse(messageBundleMethod.getTemplate(), null, messageBundleMethod.getTemplateId());
            analysis.add(new TemplateAnalysis(messageBundleMethod.getTemplateId(), template.getGeneratedId(),
                    template.getExpressions(),
                    messageBundleMethod.getMethod().declaringClass().name() + "#" + messageBundleMethod.getMethod().name()
                            + "()"));
        }

        LOGGER.debugf("Finished analysis of %s templates in %s ms", analysis.size(), System.currentTimeMillis() - start);
        return new TemplatesAnalysisBuildItem(analysis);
    }

    @BuildStep
    void validateExpressions(TemplatesAnalysisBuildItem templatesAnalysis, BeanArchiveIndexBuildItem beanArchiveIndex,
            List<TemplateExtensionMethodBuildItem> templateExtensionMethods,
            List<TypeCheckExcludeBuildItem> excludes,
            BuildProducer<IncorrectExpressionBuildItem> incorrectExpressions,
            BuildProducer<ImplicitValueResolverBuildItem> implicitClasses,
            BeanRegistrationPhaseBuildItem registrationPhase,
            // This producer is needed to ensure the correct ordering, ie. this build step must be executed before the ArC validation step
            BuildProducer<BeanConfiguratorBuildItem> configurators) {

        IndexView index = beanArchiveIndex.getIndex();
        Function<String, String> templateIdToPathFun = new Function<String, String>() {
            @Override
            public String apply(String id) {
                return findTemplatePath(templatesAnalysis, id);
            }
        };

        // IMPLEMENTATION NOTE: 
        // We do not support injection of synthetic beans with names 
        // Dependency on the ValidationPhaseBuildItem would result in a cycle in the build chain
        Map<String, BeanInfo> namedBeans = registrationPhase.getContext().beans().withName()
                .collect(toMap(BeanInfo::getName, Function.identity()));

        // Map implicit class -> set of used members
        Map<ClassInfo, Set<String>> implicitClassToMembersUsed = new HashMap<>();

        for (TemplateAnalysis templateAnalysis : templatesAnalysis.getAnalysis()) {
            // Maps an expression generated id to the last match of an expression (i.e. the type of the last part)
            Map<Integer, Match> generatedIdsToMatches = new HashMap<>();

            for (Expression expression : templateAnalysis.expressions) {
                if (expression.isLiteral()) {
                    continue;
                }
                if (expression.hasNamespace()) {
                    if (expression.getNamespace().equals(EngineProducer.INJECT_NAMESPACE)) {
                        validateInjectExpression(templateAnalysis, expression, index, incorrectExpressions,
                                templateExtensionMethods, excludes, namedBeans, generatedIdsToMatches,
                                implicitClassToMembersUsed,
                                templateIdToPathFun);
                    } else {
                        continue;
                    }
                } else {
                    generatedIdsToMatches.put(expression.getGeneratedId(),
                            validateNestedExpressions(templateAnalysis, null, new HashMap<>(), templateExtensionMethods,
                                    excludes,
                                    incorrectExpressions, expression, index, implicitClassToMembersUsed, templateIdToPathFun,
                                    generatedIdsToMatches));
                }
            }
        }

        for (Entry<ClassInfo, Set<String>> entry : implicitClassToMembersUsed.entrySet()) {
            implicitClasses.produce(new ImplicitValueResolverBuildItem(entry.getKey(),
                    new TemplateDataBuilder().addIgnore(buildIgnorePattern(entry.getValue())).build()));
        }
    }

    static String buildIgnorePattern(Iterable<String> names) {
        // ^(?!\\Qbar\\P|\\Qfoo\\P).*$
        StringBuilder ignorePattern = new StringBuilder("^(?!");
        for (Iterator<String> iterator = names.iterator(); iterator.hasNext();) {
            String memberName = iterator.next();
            ignorePattern.append(Pattern.quote(memberName));
            if (iterator.hasNext()) {
                ignorePattern.append("|");
            }
        }
        ignorePattern.append(").*$");
        return ignorePattern.toString();
    }

    /**
     * 
     * @param templateAnalysis
     * @param rootClazz
     * @param results Map of cached results within a single expression
     * @param templateExtensionMethods
     * @param excludes
     * @param incorrectExpressions
     * @param expression
     * @param index
     * @param implicitClassToMembersUsed
     * @param templateIdToPathFun
     * @return the last match object
     */
    static Match validateNestedExpressions(TemplateAnalysis templateAnalysis, ClassInfo rootClazz, Map<String, Match> results,
            List<TemplateExtensionMethodBuildItem> templateExtensionMethods,
            List<TypeCheckExcludeBuildItem> excludes,
            BuildProducer<IncorrectExpressionBuildItem> incorrectExpressions, Expression expression, IndexView index,
            Map<ClassInfo, Set<String>> implicitClassToMembersUsed, Function<String, String> templateIdToPathFun,
            Map<Integer, Match> generatedIdsToMatches) {

        // First validate nested virtual methods
        for (Expression.Part part : expression.getParts()) {
            if (part.isVirtualMethod()) {
                for (Expression param : part.asVirtualMethod().getParameters()) {
                    if (!results.containsKey(param.toOriginalString())) {
                        validateNestedExpressions(templateAnalysis, null, results, templateExtensionMethods, excludes,
                                incorrectExpressions, param, index, implicitClassToMembersUsed, templateIdToPathFun,
                                generatedIdsToMatches);
                    }
                }
            }
        }
        // Then validate the expression itself
        Match match = new Match();
        if (rootClazz == null && !expression.hasTypeInfo()) {
            // No type info available or a namespace expression
            results.put(expression.toOriginalString(), match);
            return match;
        }

        List<Info> parts = TypeInfos.create(expression, index, templateIdToPathFun);
        Iterator<Info> iterator = parts.iterator();
        Info root = iterator.next();

        if (rootClazz == null) {
            if (root.isTypeInfo()) {
                // E.g. |org.acme.Item|
                match.clazz = root.asTypeInfo().rawClass;
                match.type = root.asTypeInfo().resolvedType;
                if (root.asTypeInfo().hint != null) {
                    processHints(templateAnalysis, root.asTypeInfo().hint, match, index, expression, generatedIdsToMatches,
                            incorrectExpressions);
                }
            } else {
                if (root.isProperty() && root.asProperty().hint != null) {
                    // Root is not a type info but a property with hint
                    // E.g. 'it<loop#123>' and 'STATUS<when#123>'
                    if (processHints(templateAnalysis, root.asProperty().hint, match, index, expression,
                            generatedIdsToMatches, incorrectExpressions)) {
                        // In some cases it's necessary to reset the iterator
                        iterator = parts.iterator();
                    }
                } else {
                    // No type info available 
                    results.put(expression.toOriginalString(), match);
                    return match;
                }
            }
        } else {
            // The first part is skipped, e.g. for {inject:foo.name} the first part is the name of the bean
            match.clazz = rootClazz;
            match.type = Type.create(rootClazz.name(), org.jboss.jandex.Type.Kind.CLASS);
        }

        while (iterator.hasNext()) {
            // Now iterate over all parts of the expression and check each part against the current "match class"
            Info info = iterator.next();
            if (match.clazz != null) {
                // By default, we only consider properties
                Set<String> membersUsed = implicitClassToMembersUsed.computeIfAbsent(match.clazz, c -> new HashSet<>());
                AnnotationTarget member = null;
                // First try to find java members
                if (info.isVirtualMethod()) {
                    member = findMethod(info.part.asVirtualMethod(), match.clazz, expression, index, templateIdToPathFun,
                            results);
                    if (member != null) {
                        membersUsed.add(member.asMethod().name());
                    }
                } else if (info.isProperty()) {
                    member = findProperty(info.asProperty().name, match.clazz, index);
                    if (member != null) {
                        membersUsed.add(member.kind() == Kind.FIELD ? member.asField().name() : member.asMethod().name());
                    }
                }
                // Java member not found - try extension methods
                if (member == null) {
                    member = findTemplateExtensionMethod(info, match.clazz, templateExtensionMethods, expression, index,
                            templateIdToPathFun, results);
                }

                if (member == null) {
                    // Test whether the validation should be skipped
                    TypeCheck check = new TypeCheck(info.isProperty() ? info.asProperty().name : info.asVirtualMethod().name,
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
                            info.value, match.clazz.toString(), expression.getOrigin()));
                    match.clear();
                    break;
                } else {
                    match.type = resolveType(member, match, index);
                    if (match.type.kind() == Type.Kind.CLASS || match.type.kind() == Type.Kind.PARAMETERIZED_TYPE) {
                        match.clazz = index.getClassByName(match.type.name());
                    }
                    if (info.isProperty()) {
                        String hint = info.asProperty().hint;
                        if (hint != null) {
                            // For example a loop section needs to validate the type of an element
                            processHints(templateAnalysis, hint, match, index, expression, generatedIdsToMatches,
                                    incorrectExpressions);
                        }
                    }
                    if (match.type != null && match.type.kind() == org.jboss.jandex.Type.Kind.PRIMITIVE) {
                        break;
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
        return match;
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
            AnnotationValue namespaceValue = entry.getValue().value(ExtensionMethodGenerator.NAMESPACE);
            ExtensionMethodGenerator.validate(method, method.parameters(),
                    namespaceValue != null ? namespaceValue.asString() : null);
            produceExtensionMethod(index, extensionMethods, method, entry.getValue());
            LOGGER.debugf("Found template extension method %s declared on %s", method,
                    method.declaringClass().name());
        }

        // Class-level annotations
        for (Entry<ClassInfo, AnnotationInstance> entry : classes.entrySet()) {
            ClassInfo clazz = entry.getKey();
            AnnotationValue namespaceValue = entry.getValue().value(ExtensionMethodGenerator.NAMESPACE);
            String namespace = namespaceValue != null ? namespaceValue.asString() : null;
            for (MethodInfo method : clazz.methods()) {
                if (!Modifier.isStatic(method.flags()) || method.returnType().kind() == org.jboss.jandex.Type.Kind.VOID
                        || Modifier.isPrivate(method.flags())
                        || ValueResolverGenerator.isSynthetic(method.flags())) {
                    // Filter out non-static, synthetic, private and void methods
                    continue;
                }
                if ((namespace == null || namespace.isEmpty()) && method.parameters().isEmpty()) {
                    // Filter methods with no params for non-namespace extensions
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
        AnnotationValue matchNameValue = extensionAnnotation.value(ExtensionMethodGenerator.MATCH_NAME);
        if (matchNameValue != null) {
            matchName = matchNameValue.asString();
        }
        if (matchName == null) {
            matchName = method.name();
        }
        int priority = TemplateExtension.DEFAULT_PRIORITY;
        AnnotationValue priorityValue = extensionAnnotation.value(ExtensionMethodGenerator.PRIORITY);
        if (priorityValue != null) {
            priority = priorityValue.asInt();
        }
        String namespace = "";
        AnnotationValue namespaceValue = extensionAnnotation.value(ExtensionMethodGenerator.NAMESPACE);
        if (namespaceValue != null) {
            namespace = namespaceValue.asString();
        }
        String matchRegex = null;
        AnnotationValue matchRegexValue = extensionAnnotation.value(ExtensionMethodGenerator.MATCH_REGEX);
        if (matchRegexValue != null) {
            matchRegex = matchRegexValue.asString();
        }
        extensionMethods.produce(new TemplateExtensionMethodBuildItem(method, matchName, matchRegex,
                namespace.isEmpty() ? index.getClassByName(method.parameters().get(0).name()) : null,
                priority, namespace));
    }

    private void validateInjectExpression(TemplateAnalysis templateAnalysis, Expression expression, IndexView index,
            BuildProducer<IncorrectExpressionBuildItem> incorrectExpressions,
            List<TemplateExtensionMethodBuildItem> templateExtensionMethods, List<TypeCheckExcludeBuildItem> excludes,
            Map<String, BeanInfo> namedBeans, Map<Integer, Match> generatedIdsToMatches,
            Map<ClassInfo, Set<String>> implicitClassToMembersUsed, Function<String, String> templateIdToPathFun) {
        Expression.Part firstPart = expression.getParts().get(0);
        if (firstPart.isVirtualMethod()) {
            incorrectExpressions.produce(new IncorrectExpressionBuildItem(expression.toOriginalString(),
                    "The inject: namespace must be followed by a bean name",
                    expression.getOrigin()));
            return;
        }
        String beanName;
        if (expression.hasNamespace()) {
            beanName = firstPart.getName();
        } else {
            // inject:foo.labels<loop-element> => foo
            String firstInfoPart = Expressions.splitTypeInfoParts(firstPart.getTypeInfo()).get(0);
            beanName = firstInfoPart.substring(EngineProducer.INJECT_NAMESPACE.length() + 1,
                    firstInfoPart.length());
        }

        BeanInfo bean = namedBeans.get(beanName);
        if (bean != null) {
            if (expression.getParts().size() == 1) {
                // Only the bean needs to be validated
                return;
            }
            generatedIdsToMatches.put(expression.getGeneratedId(),
                    validateNestedExpressions(templateAnalysis, bean.getImplClazz(), new HashMap<>(),
                            templateExtensionMethods, excludes, incorrectExpressions, expression, index,
                            implicitClassToMembersUsed, templateIdToPathFun, generatedIdsToMatches));

        } else {
            // User is injecting a non-existing bean
            incorrectExpressions.produce(new IncorrectExpressionBuildItem(expression.toOriginalString(),
                    beanName, null, expression.getOrigin()));
        }
    }

    static String findTemplatePath(TemplatesAnalysisBuildItem analysis, String id) {
        for (TemplateAnalysis templateAnalysis : analysis.getAnalysis()) {
            if (templateAnalysis.generatedId.equals(id)) {
                return templateAnalysis.path;
            }
        }
        return null;
    }

    @BuildStep
    void generateValueResolvers(QuteConfig config, BuildProducer<GeneratedClassBuildItem> generatedClasses,
            CombinedIndexBuildItem combinedIndex, BeanArchiveIndexBuildItem beanArchiveIndex,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            List<TemplatePathBuildItem> templatePaths,
            List<TemplateExtensionMethodBuildItem> templateExtensionMethods,
            List<ImplicitValueResolverBuildItem> implicitClasses,
            TemplatesAnalysisBuildItem templatesAnalysis,
            BuildProducer<GeneratedValueResolverBuildItem> generatedResolvers,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {

        IndexView index = beanArchiveIndex.getIndex();
        ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClasses, new Predicate<String>() {
            @Override
            public boolean test(String name) {
                int idx = name.lastIndexOf(ExtensionMethodGenerator.NAMESPACE_SUFFIX);
                if (idx == -1) {
                    idx = name.lastIndexOf(ExtensionMethodGenerator.SUFFIX);
                }
                if (idx == -1) {
                    idx = name.lastIndexOf(ValueResolverGenerator.SUFFIX);
                }
                String className = name.substring(0, idx);
                if (className.contains(ValueResolverGenerator.NESTED_SEPARATOR)) {
                    className = className.replace(ValueResolverGenerator.NESTED_SEPARATOR, "$");
                }
                //if the class is (directly) in the TCCL (and not its parent) then it is an application class
                QuarkusClassLoader cl = (QuarkusClassLoader) Thread.currentThread().getContextClassLoader();
                return !cl.getElementsWithResource(className + ".class", true).isEmpty();
            }
        });

        ValueResolverGenerator.Builder builder = ValueResolverGenerator.builder().setIndex(index).setClassOutput(classOutput);
        Set<DotName> controlled = new HashSet<>();
        Map<DotName, AnnotationInstance> uncontrolled = new HashMap<>();
        for (AnnotationInstance templateData : index.getAnnotations(ValueResolverGenerator.TEMPLATE_DATA)) {
            processsTemplateData(index, templateData, templateData.target(), controlled, uncontrolled, builder);
        }
        for (AnnotationInstance containerInstance : index.getAnnotations(ValueResolverGenerator.TEMPLATE_DATA_CONTAINER)) {
            for (AnnotationInstance templateData : containerInstance.value().asNestedArray()) {
                processsTemplateData(index, templateData, containerInstance.target(), controlled, uncontrolled, builder);
            }
        }

        for (ImplicitValueResolverBuildItem implicit : implicitClasses) {
            DotName implicitClassName = implicit.getClazz().name();
            if (controlled.contains(implicitClassName)) {
                LOGGER.debugf("Implicit value resolver for %s ignored: class is annotated with @TemplateData",
                        implicitClassName);
                continue;
            }
            if (uncontrolled.containsKey(implicitClassName)) {
                LOGGER.debugf("Implicit value resolver for %d ignored: %s declared on %s", uncontrolled.get(implicitClassName),
                        uncontrolled.get(implicitClassName).target());
                continue;
            }
            builder.addClass(implicit.getClazz(), implicit.getTemplateData());
        }

        ValueResolverGenerator generator = builder.build();
        generator.generate();

        Set<String> generatedTypes = new HashSet<>();
        generatedTypes.addAll(generator.getGeneratedTypes());

        ExtensionMethodGenerator extensionMethodGenerator = new ExtensionMethodGenerator(index, classOutput);
        Map<DotName, List<TemplateExtensionMethodBuildItem>> classToNamespaceExtensions = new HashMap<>();
        Map<String, DotName> namespaceToClass = new HashMap<>();

        for (TemplateExtensionMethodBuildItem templateExtension : templateExtensionMethods) {
            if (templateExtension.hasNamespace()) {
                // Group extension methods declared on the same class by namespace
                DotName declaringClassName = templateExtension.getMethod().declaringClass().name();
                DotName namespaceClassName = namespaceToClass.get(templateExtension.getNamespace());
                if (namespaceClassName == null) {
                    namespaceToClass.put(templateExtension.getNamespace(), declaringClassName);
                } else if (!namespaceClassName.equals(declaringClassName)) {
                    throw new IllegalStateException("Template extension methods that share the namespace "
                            + templateExtension.getNamespace() + " must be declared on the same class; but declared on "
                            + namespaceClassName + " and " + declaringClassName);
                }
                List<TemplateExtensionMethodBuildItem> namespaceMethods = classToNamespaceExtensions
                        .get(declaringClassName);
                if (namespaceMethods == null) {
                    namespaceMethods = new ArrayList<>();
                    classToNamespaceExtensions.put(declaringClassName, namespaceMethods);
                }
                namespaceMethods.add(templateExtension);
            } else {
                // Generate ValueResolver per extension method
                extensionMethodGenerator.generate(templateExtension.getMethod(), templateExtension.getMatchName(),
                        templateExtension.getMatchRegex(), templateExtension.getPriority());
            }
        }

        // Generate a namespace resolver for extension methods declared on the same class
        for (Entry<DotName, List<TemplateExtensionMethodBuildItem>> entry : classToNamespaceExtensions.entrySet()) {
            List<TemplateExtensionMethodBuildItem> methods = entry.getValue();
            // Methods with higher priority take precedence
            methods.sort(Comparator.comparingInt(TemplateExtensionMethodBuildItem::getPriority).reversed());
            try (NamespaceResolverCreator namespaceResolverCreator = extensionMethodGenerator
                    .createNamespaceResolver(methods.get(0).getMethod().declaringClass(), methods.get(0).getNamespace())) {
                try (ResolveCreator resolveCreator = namespaceResolverCreator.implementResolve()) {
                    for (TemplateExtensionMethodBuildItem method : methods) {
                        resolveCreator.addMethod(method.getMethod(), method.getMatchName(), method.getMatchRegex());
                    }
                }
            }
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

            if (injectionPoint.getRequiredType().name().equals(Names.TEMPLATE)) {

                AnnotationInstance resourcePath = injectionPoint.getRequiredQualifier(Names.RESOURCE_PATH);
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
                                new TemplateException("No template found for " + injectionPoint.getTargetInfo())));
                    }
                }
            }
        }
    }

    @BuildStep
    TemplateVariantsBuildItem collectTemplateVariants(List<TemplatePathBuildItem> templatePaths) throws IOException {
        Set<String> allPaths = templatePaths.stream().map(TemplatePathBuildItem::getPath).collect(Collectors.toSet());
        // item -> [item.html, item.txt]
        // ItemResource/item -> -> [ItemResource/item.html, ItemResource/item.xml]
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
        LOGGER.debugf("Template variants found: %s", baseToVariants);
        return new TemplateVariantsBuildItem(baseToVariants);
    }

    @BuildStep
    void excludeTypeChecks(BuildProducer<TypeCheckExcludeBuildItem> excludes) {
        // Exclude all checks that involve built-in value resolvers
        // TODO: We need a better way to exclude value resolvers that are not template extension methods
        List<String> skipOperators = Arrays.asList("?:", "or", ":", "?", "&&", "||");

        excludes.produce(new TypeCheckExcludeBuildItem(new Predicate<TypeCheck>() {
            @Override
            public boolean test(TypeCheck check) {
                // RawString - these properties can be used on any object
                if (check.isProperty() && ("raw".equals(check.name) || "safe".equals(check.name))) {
                    return true;
                }
                // Elvis, ternary and logical operators
                if (check.numberOfParameters == 1 && skipOperators.contains(check.name)) {
                    return true;
                }
                // Collection.contains()
                if (check.numberOfParameters == 1 && check.classNameEquals(Names.COLLECTION) && check.name.equals("contains")) {
                    return true;
                }
                return false;
            }
        }));
    }

    @BuildStep
    @Record(value = STATIC_INIT)
    void initialize(BuildProducer<SyntheticBeanBuildItem> syntheticBeans, QuteRecorder recorder,
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
                .supplier(recorder.createContext(generatedValueResolvers.stream()
                        .map(GeneratedValueResolverBuildItem::getClassName).collect(Collectors.toList()), templates,
                        tags, variants))
                .done());
        ;
    }

    private static Type resolveType(AnnotationTarget member, Match match, IndexView index) {
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

    /**
     * 
     * @param templateAnalysis
     * @param helperHint
     * @param match
     * @param index
     * @param expression
     * @param generatedIdsToMatches
     * @param incorrectExpressions
     * @return {@code true} if it is necessary to reset the type info part iterator
     */
    static boolean processHints(TemplateAnalysis templateAnalysis, String helperHint, Match match, IndexView index,
            Expression expression, Map<Integer, Match> generatedIdsToMatches,
            BuildProducer<IncorrectExpressionBuildItem> incorrectExpressions) {
        if (helperHint == null || helperHint.isEmpty()) {
            return false;
        }
        if (helperHint.equals(LoopSectionHelper.Factory.HINT_ELEMENT)) {
            // Iterable<Item>, Stream<Item> => Item
            // Map<String,Long> => Entry<String,Long>
            processLoopElementHint(match, index, expression, incorrectExpressions);
        } else if (helperHint.startsWith(LoopSectionHelper.Factory.HINT_PREFIX)) {
            Integer iterableExprId = Integer
                    .valueOf(helperHint.substring(LoopSectionHelper.Factory.HINT_PREFIX.length(), helperHint.length() - 1));
            Expression valueExpr = templateAnalysis.findExpression(iterableExprId);
            if (valueExpr != null) {
                Match valueExprMatch = generatedIdsToMatches.get(valueExpr.getGeneratedId());
                if (valueExprMatch != null) {
                    match.type = valueExprMatch.type;
                    match.clazz = valueExprMatch.clazz;
                }
            }
        } else if (helperHint.startsWith(WhenSectionHelper.Factory.HINT_PREFIX)) {
            // If a value expression resolves to an enum we attempt to use the enum type to validate the enum constant  
            // This basically transforms the type info "ON<when:12345>" into something like "|org.acme.Status|.ON"
            Integer valueExprId = Integer
                    .valueOf(helperHint.substring(WhenSectionHelper.Factory.HINT_PREFIX.length(), helperHint.length() - 1));
            Expression valueExpr = templateAnalysis.findExpression(valueExprId);
            if (valueExpr != null) {
                Match valueExprMatch = generatedIdsToMatches.get(valueExpr.getGeneratedId());
                if (valueExprMatch != null && valueExprMatch.clazz.isEnum()) {
                    match.type = valueExprMatch.type;
                    match.clazz = valueExprMatch.clazz;
                    return true;
                }
            }
        }
        return false;
    }

    static void processLoopElementHint(Match match, IndexView index, Expression expression,
            BuildProducer<IncorrectExpressionBuildItem> incorrectExpressions) {
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
            matchType = extractMatchType(closure, Names.ITERABLE, firstParamType);
            if (matchType == null) {
                // Stream<Long> => Long
                matchType = extractMatchType(closure, Names.STREAM, firstParamType);
            }
            if (matchType == null) {
                // Entry<K,V> => Entry<String,Item>
                matchType = extractMatchType(closure, Names.MAP, t -> {
                    Type[] args = new Type[2];
                    args[0] = t.asParameterizedType().arguments().get(0);
                    args[1] = t.asParameterizedType().arguments().get(1);
                    return ParameterizedType.create(Names.MAP_ENTRY, args, null);
                });
            }
            if (matchType == null) {
                // Iterator<Item> => Item
                matchType = extractMatchType(closure, Names.ITERATOR, firstParamType);
            }
        }

        // Handle CompletionStage specially
        if (matchType == null && ValueResolverGenerator.hasCompletionStageInTypeClosure(match.clazz, index)) {
            Set<Type> closure = Types.getTypeClosure(match.clazz, Types.buildResolvedMap(
                    match.getParameterizedTypeArguments(), match.getTypeParameters(), new HashMap<>(), index), index);
            Function<Type, Type> firstParamType = t -> t.asParameterizedType().arguments().get(0);
            // CompletionStage<List<Item>> => List<Item>
            match.type = extractMatchType(closure, Names.COMPLETION_STAGE, firstParamType);
            match.clazz = index.getClassByName(match.type.name());
            processLoopElementHint(match, index, expression, incorrectExpressions);
            return;
        }

        if (matchType != null) {
            match.type = matchType;
            match.clazz = index.getClassByName(match.type.name());
        } else {
            incorrectExpressions.produce(new IncorrectExpressionBuildItem(expression.toOriginalString(),
                    "Unsupported iterable type found: " + match.type, expression.getOrigin()));
            match.type = null;
            match.clazz = null;
        }
    }

    static Type extractMatchType(Set<Type> closure, DotName matchName, Function<Type, Type> extractFun) {
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

    private static AnnotationTarget findTemplateExtensionMethod(Info info, ClassInfo matchClass,
            List<TemplateExtensionMethodBuildItem> templateExtensionMethods, Expression expression, IndexView index,
            Function<String, String> templateIdToPathFun, Map<String, Match> results) {
        if (!info.isProperty() && !info.isVirtualMethod()) {
            return null;
        }
        String name = info.isProperty() ? info.asProperty().name : info.asVirtualMethod().name;
        for (TemplateExtensionMethodBuildItem extensionMethod : templateExtensionMethods) {
            if (extensionMethod.hasNamespace()) {
                // Skip namespace extensions
                continue;
            }
            if (!Types.isAssignableFrom(extensionMethod.getMatchClass().name(), matchClass.name(), index)) {
                // If "Bar extends Foo" then Bar should be matched for the extension method "int get(Foo)"   
                continue;
            }
            if (!extensionMethod.matchesName(name)) {
                // Name does not match
                continue;
            }
            List<Type> parameters = extensionMethod.getMethod().parameters();
            if (parameters.size() > 1 && !info.isVirtualMethod()) {
                // If method accepts additional params the info must be a virtual method
                continue;
            }
            if (info.isVirtualMethod()) {
                // For virtual method validate the number of params and attempt to validate the parameter types if available
                VirtualMethodPart virtualMethod = info.part.asVirtualMethod();
                boolean isVarArgs = ValueResolverGenerator.isVarArgs(extensionMethod.getMethod());
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
    private static AnnotationTarget findProperty(String name, ClassInfo clazz, IndexView index) {
        while (clazz != null) {
            // Fields
            for (FieldInfo field : clazz.fields()) {
                if (!Modifier.isPublic(field.flags()) || ValueResolverGenerator.isSynthetic(field.flags())) {
                    // Skip non-public and synthetic fields 
                    continue;
                }
                if (field.name().equals(name) && (field.isEnumConstant() || !Modifier.isStatic(field.flags()))) {
                    // Name matches and it's either an enum constant or a non-static field
                    return field;
                }
            }
            // Methods
            for (MethodInfo method : clazz.methods()) {
                if (method.returnType().kind() != org.jboss.jandex.Type.Kind.VOID && Modifier.isPublic(method.flags())
                        && !Modifier.isStatic(method.flags())
                        && !ValueResolverGenerator.isSynthetic(method.flags()) && (method.name().equals(name)
                                || ValueResolverGenerator.getPropertyName(method.name()).equals(name))) {
                    // Skip void, non-public, static and synthetic methods
                    // Method name must match (exact or getter)
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
    private static AnnotationTarget findMethod(VirtualMethodPart virtualMethod, ClassInfo clazz, Expression expression,
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
            Set<DotName> controlled, Map<DotName, AnnotationInstance> uncontrolled, ValueResolverGenerator.Builder builder) {
        AnnotationValue targetValue = templateData.value("target");
        if (targetValue == null || targetValue.asClass().name().equals(ValueResolverGenerator.TEMPLATE_DATA)) {
            ClassInfo annotationTargetClass = annotationTarget.asClass();
            controlled.add(annotationTargetClass.name());
            builder.addClass(annotationTargetClass, templateData);
        } else {
            ClassInfo uncontrolledClass = index.getClassByName(targetValue.asClass().name());
            if (uncontrolledClass != null) {
                uncontrolled.compute(uncontrolledClass.name(), (c, v) -> {
                    if (v == null) {
                        builder.addClass(uncontrolledClass, templateData);
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

    static Map<TemplateAnalysis, Set<Expression>> collectNamespaceExpressions(TemplatesAnalysisBuildItem analysis,
            String namespace) {
        Map<TemplateAnalysis, Set<Expression>> namespaceExpressions = new HashMap<>();
        for (TemplateAnalysis template : analysis.getAnalysis()) {
            Set<Expression> expressions = null;
            for (Expression expr : collectNamespaceExpressions(template, namespace)) {
                if (expressions == null) {
                    expressions = new HashSet<>();
                }
                expressions.add(expr);
            }
            if (expressions != null) {
                namespaceExpressions.put(template, expressions);
            }
        }
        return namespaceExpressions;
    }

    static Set<Expression> collectNamespaceExpressions(TemplateAnalysis analysis, String namespace) {
        Set<Expression> namespaceExpressions = new HashSet<>();
        for (Expression expression : analysis.expressions) {
            collectNamespaceExpressions(expression, namespaceExpressions, namespace);
        }
        return namespaceExpressions;
    }

    static void collectNamespaceExpressions(Expression expression, Set<Expression> namespaceExpressions, String namespace) {
        if (expression.isLiteral()) {
            return;
        }
        if (includeNamespaceExpression(expression, namespace)) {
            // The expression itself has namespace
            namespaceExpressions.add(expression);
        }
        // Collect namespace expressions used as params of virtual methods
        for (Expression.Part part : expression.getParts()) {
            if (part.isVirtualMethod()) {
                for (Expression param : part.asVirtualMethod().getParameters()) {
                    collectNamespaceExpressions(param, namespaceExpressions, namespace);
                }
            }
        }
    }

    private static boolean includeNamespaceExpression(Expression expression, String namespace) {
        if (namespace.equals(expression.getNamespace())) {
            return true;
        }
        String typeInfo = expression.getParts().get(0).getTypeInfo();
        return typeInfo != null ? typeInfo.startsWith(namespace) : false;
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

    private static boolean isExcluded(TypeCheck check, List<TypeCheckExcludeBuildItem> excludes) {
        for (TypeCheckExcludeBuildItem exclude : excludes) {
            if (exclude.getPredicate().test(check)) {
                return true;
            }
        }
        return false;
    }

}
