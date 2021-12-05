package io.quarkus.qute.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;
import static java.util.stream.Collectors.toMap;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem.BeanConfiguratorBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.Annotations;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassCreator.Builder;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.DescriptorUtils;
import io.quarkus.gizmo.FunctionCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import io.quarkus.qute.EvalContext;
import io.quarkus.qute.EvaluatedParams;
import io.quarkus.qute.Expression;
import io.quarkus.qute.Expression.Part;
import io.quarkus.qute.Namespaces;
import io.quarkus.qute.Resolver;
import io.quarkus.qute.deployment.QuteProcessor.LookupConfig;
import io.quarkus.qute.deployment.QuteProcessor.Match;
import io.quarkus.qute.deployment.TemplatesAnalysisBuildItem.TemplateAnalysis;
import io.quarkus.qute.generator.Descriptors;
import io.quarkus.qute.generator.ValueResolverGenerator;
import io.quarkus.qute.i18n.Localized;
import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;
import io.quarkus.qute.i18n.MessageBundles;
import io.quarkus.qute.runtime.MessageBundleRecorder;
import io.quarkus.qute.runtime.QuteConfig;
import io.quarkus.runtime.util.StringUtil;

public class MessageBundleProcessor {

    private static final Logger LOGGER = Logger.getLogger(MessageBundleProcessor.class);

    private static final String SUFFIX = "_Bundle";
    private static final String BUNDLE_DEFAULT_KEY = "defaultKey";
    private static final String BUNDLE_LOCALE = "locale";
    private static final String MESSAGES = "messages";
    private static final String MESSAGE = "message";

    @BuildStep
    AdditionalBeanBuildItem beans() {
        return new AdditionalBeanBuildItem(MessageBundles.class, MessageBundle.class, Message.class, Localized.class);
    }

    @BuildStep
    List<MessageBundleBuildItem> processBundles(BeanArchiveIndexBuildItem beanArchiveIndex,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            BuildProducer<GeneratedClassBuildItem> generatedClasses, BeanRegistrationPhaseBuildItem beanRegistration,
            BuildProducer<BeanConfiguratorBuildItem> configurators,
            BuildProducer<MessageBundleMethodBuildItem> messageTemplateMethods,
            BuildProducer<HotDeploymentWatchedFileBuildItem> watchedFiles) throws IOException {

        IndexView index = beanArchiveIndex.getIndex();
        Map<String, ClassInfo> found = new HashMap<>();
        List<MessageBundleBuildItem> bundles = new ArrayList<>();
        Set<Path> messageFiles = findMessageFiles(applicationArchivesBuildItem);

        Path messagesPath = applicationArchivesBuildItem.getRootArchive().getChildPath(MESSAGES);
        for (Path messageFile : messageFiles) {
            String messageFilePath = messagesPath.relativize(messageFile).toString();
            if (File.separatorChar != '/') {
                messageFilePath = messageFilePath.replace(File.separatorChar, '/');
            }
            watchedFiles.produce(new HotDeploymentWatchedFileBuildItem(MESSAGES + "/" + messageFilePath));
        }

        // First collect all interfaces annotated with @MessageBundle
        for (AnnotationInstance bundleAnnotation : index.getAnnotations(Names.BUNDLE)) {
            if (bundleAnnotation.target().kind() == Kind.CLASS) {
                ClassInfo bundleClass = bundleAnnotation.target().asClass();
                if (Modifier.isInterface(bundleClass.flags())) {
                    AnnotationValue nameValue = bundleAnnotation.value();
                    String name = nameValue != null ? nameValue.asString() : MessageBundle.DEFAULT_NAME;
                    if (!Namespaces.isValidNamespace(name)) {
                        throw new MessageBundleException(
                                String.format(
                                        "Message bundle name [%s] declared on %s must be a valid namespace - the value can only consist of alphanumeric characters and underscores",
                                        name, bundleClass));
                    }

                    if (found.containsKey(name)) {
                        throw new MessageBundleException(
                                String.format("Message bundle interface name conflict - [%s] is used for both [%s] and [%s]",
                                        name, bundleClass, found.get(name)));
                    }
                    found.put(name, bundleClass);

                    // Find localizations for each interface
                    String defaultLocale = getDefaultLocale(bundleAnnotation);
                    List<ClassInfo> localized = new ArrayList<>();
                    for (ClassInfo implementor : index.getKnownDirectImplementors(bundleClass.name())) {
                        if (Modifier.isInterface(implementor.flags())) {
                            localized.add(implementor);
                        }
                    }
                    Map<String, ClassInfo> localeToInterface = new HashMap<>();
                    for (ClassInfo localizedInterface : localized) {
                        String locale = localizedInterface.classAnnotation(Names.LOCALIZED).value().asString();
                        ClassInfo previous = localeToInterface.put(locale, localizedInterface);
                        if (defaultLocale.equals(locale) || previous != null) {
                            throw new MessageBundleException(String.format(
                                    "A localized message bundle interface [%s] already exists for locale %s: [%s]",
                                    previous != null ? previous : bundleClass, locale, localizedInterface));
                        }
                    }

                    // Find localized files
                    Map<String, Path> localeToFile = new HashMap<>();
                    for (Path messageFile : messageFiles) {
                        String fileName = messageFile.getFileName().toString();
                        if (fileName.startsWith(name)) {
                            // msg_en.txt -> en
                            String locale = fileName.substring(fileName.indexOf('_') + 1, fileName.indexOf('.'));
                            ClassInfo localizedInterface = localeToInterface.get(locale);
                            if (localizedInterface != null) {
                                throw new MessageBundleException(
                                        String.format(
                                                "A localized message bundle interface [%s] already exists for locale %s: [%s]",
                                                localizedInterface, locale, fileName));
                            }
                            localeToFile.put(locale, messageFile);
                        }
                    }

                    bundles.add(new MessageBundleBuildItem(name, bundleClass, localeToInterface, localeToFile));
                } else {
                    throw new MessageBundleException("@MessageBundle must be declared on an interface: " + bundleClass);
                }
            }
        }

        // Generate implementations
        // name -> impl class
        Map<String, String> generatedImplementations = generateImplementations(bundles, generatedClasses,
                messageTemplateMethods);

        // Register synthetic beans
        for (MessageBundleBuildItem bundle : bundles) {
            ClassInfo bundleInterface = bundle.getDefaultBundleInterface();
            beanRegistration.getContext().configure(bundleInterface.name()).addType(bundle.getDefaultBundleInterface().name())
                    // The default message bundle - add both @Default and @Localized
                    .addQualifier(DotNames.DEFAULT).addQualifier().annotation(Names.LOCALIZED)
                    .addValue("value", getDefaultLocale(bundleInterface.classAnnotation(Names.BUNDLE))).done().unremovable()
                    .scope(Singleton.class).creator(mc -> {
                        // Just create a new instance of the generated class
                        mc.returnValue(
                                mc.newInstance(MethodDescriptor
                                        .ofConstructor(generatedImplementations.get(bundleInterface.name().toString()))));
                    }).done();

            // Localized interfaces
            for (ClassInfo localizedInterface : bundle.getLocalizedInterfaces().values()) {
                beanRegistration.getContext().configure(localizedInterface.name())
                        .addType(bundle.getDefaultBundleInterface().name())
                        .addQualifier(localizedInterface.classAnnotation(Names.LOCALIZED))
                        .unremovable()
                        .scope(Singleton.class).creator(mc -> {
                            // Just create a new instance of the generated class
                            mc.returnValue(
                                    mc.newInstance(MethodDescriptor.ofConstructor(
                                            generatedImplementations.get(localizedInterface.name().toString()))));
                        }).done();
            }
            // Localized files
            for (Entry<String, Path> entry : bundle.getLocalizedFiles().entrySet()) {
                beanRegistration.getContext().configure(bundle.getDefaultBundleInterface().name())
                        .addType(bundle.getDefaultBundleInterface().name())
                        .addQualifier().annotation(Names.LOCALIZED)
                        .addValue("value", entry.getKey()).done()
                        .unremovable()
                        .scope(Singleton.class).creator(mc -> {
                            // Just create a new instance of the generated class
                            mc.returnValue(
                                    mc.newInstance(MethodDescriptor
                                            .ofConstructor(generatedImplementations.get(entry.getValue().toString()))));
                        }).done();
            }
        }

        return bundles;
    }

    @Record(value = STATIC_INIT)
    @BuildStep
    void initBundleContext(MessageBundleRecorder recorder,
            List<MessageBundleMethodBuildItem> messageBundleMethods,
            List<MessageBundleBuildItem> bundles,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) throws ClassNotFoundException {

        Map<String, Map<String, Class<?>>> bundleInterfaces = new HashMap<>();
        for (MessageBundleBuildItem bundle : bundles) {
            final Class<?> bundleClass = Class.forName(bundle.getDefaultBundleInterface().toString(), true,
                    Thread.currentThread().getContextClassLoader());
            Map<String, Class<?>> localeToInterface = new HashMap<>();
            localeToInterface.put(MessageBundles.DEFAULT_LOCALE,
                    bundleClass);

            for (String locale : bundle.getLocalizedInterfaces().keySet()) {
                localeToInterface.put(locale, bundleClass);
            }
            for (String locale : bundle.getLocalizedFiles().keySet()) {
                localeToInterface.put(locale, bundleClass);
            }
            bundleInterfaces.put(bundle.getName(), localeToInterface);
        }

        Map<String, String> templateIdToContent = messageBundleMethods.stream()
                .filter(MessageBundleMethodBuildItem::isValidatable).collect(
                        Collectors.toMap(MessageBundleMethodBuildItem::getTemplateId,
                                MessageBundleMethodBuildItem::getTemplate));

        syntheticBeans.produce(SyntheticBeanBuildItem.configure(MessageBundleRecorder.BundleContext.class)
                .supplier(recorder.createContext(templateIdToContent, bundleInterfaces)).done());
    }

    @BuildStep
    void validateMessageBundleMethods(TemplatesAnalysisBuildItem templatesAnalysis,
            List<MessageBundleMethodBuildItem> messageBundleMethods,
            BuildProducer<IncorrectExpressionBuildItem> incorrectExpressions) {

        Map<String, MessageBundleMethodBuildItem> bundleMethods = messageBundleMethods.stream()
                .filter(MessageBundleMethodBuildItem::isValidatable)
                .collect(Collectors.toMap(MessageBundleMethodBuildItem::getTemplateId, Function.identity()));

        for (TemplateAnalysis analysis : templatesAnalysis.getAnalysis()) {
            MessageBundleMethodBuildItem messageBundleMethod = bundleMethods.get(analysis.id);
            if (messageBundleMethod != null) {
                Set<String> usedParamNames = new HashSet<>();
                // All top-level expressions without namespace map to a param
                Set<String> paramNames = IntStream.range(0, messageBundleMethod.getMethod().parameters().size())
                        .mapToObj(idx -> getParameterName(messageBundleMethod.getMethod(), idx)).collect(Collectors.toSet());
                for (Expression expression : analysis.expressions) {
                    validateExpression(incorrectExpressions, messageBundleMethod, expression, paramNames, usedParamNames);
                }
                // Log a warning if a parameter is not used in the template
                for (String paramName : paramNames) {
                    if (!usedParamNames.contains(paramName)) {
                        LOGGER.warnf("Unused parameter found [%s] in the message template of: %s", paramName,
                                messageBundleMethod.getMethod().declaringClass().name() + "#"
                                        + messageBundleMethod.getMethod().name() + "()");
                    }
                }
            }
        }
    }

    private void validateExpression(BuildProducer<IncorrectExpressionBuildItem> incorrectExpressions,
            MessageBundleMethodBuildItem messageBundleMethod, Expression expression, Set<String> paramNames,
            Set<String> usedParamNames) {
        if (expression.isLiteral()) {
            return;
        }
        if (!expression.hasNamespace()) {
            String name = expression.getParts().get(0).getName();
            if (!paramNames.contains(name)) {
                incorrectExpressions.produce(new IncorrectExpressionBuildItem(expression.toOriginalString(),
                        name + " is not a parameter of the message bundle method "
                                + messageBundleMethod.getMethod().declaringClass().name() + "#"
                                + messageBundleMethod.getMethod().name() + "()",
                        expression.getOrigin()));
            } else {
                usedParamNames.add(name);
            }
        }
        // Inspect method params too
        for (Part part : expression.getParts()) {
            if (part.isVirtualMethod()) {
                for (Expression param : part.asVirtualMethod().getParameters()) {
                    validateExpression(incorrectExpressions, messageBundleMethod, param, paramNames, usedParamNames);
                }
            }
        }
    }

    @BuildStep
    void validateMessageBundleMethodsInTemplates(TemplatesAnalysisBuildItem analysis,
            BeanArchiveIndexBuildItem beanArchiveIndex,
            List<TemplateExtensionMethodBuildItem> templateExtensionMethods,
            List<TypeCheckExcludeBuildItem> excludes,
            List<MessageBundleBuildItem> messageBundles,
            List<MessageBundleMethodBuildItem> messageBundleMethods,
            List<TemplateExpressionMatchesBuildItem> expressionMatches,
            BuildProducer<IncorrectExpressionBuildItem> incorrectExpressions,
            BuildProducer<ImplicitValueResolverBuildItem> implicitClasses,
            List<CheckedTemplateBuildItem> checkedTemplates,
            BeanDiscoveryFinishedBuildItem beanDiscovery,
            List<TemplateDataBuildItem> templateData,
            QuteConfig config) {

        IndexView index = beanArchiveIndex.getIndex();
        Function<String, String> templateIdToPathFun = new Function<String, String>() {
            @Override
            public String apply(String id) {
                return QuteProcessor.findTemplatePath(analysis, id);
            }
        };

        // IMPLEMENTATION NOTE: 
        // We do not support injection of synthetic beans with names 
        // Dependency on the ValidationPhaseBuildItem would result in a cycle in the build chain
        Map<String, BeanInfo> namedBeans = beanDiscovery.beanStream().withName()
                .collect(toMap(BeanInfo::getName, Function.identity()));

        Map<String, TemplateDataBuildItem> namespaceTemplateData = templateData.stream()
                .filter(TemplateDataBuildItem::hasNamespace)
                .collect(Collectors.toMap(TemplateDataBuildItem::getNamespace, Function.identity()));

        Map<String, List<TemplateExtensionMethodBuildItem>> namespaceExtensionMethods = templateExtensionMethods.stream()
                .filter(TemplateExtensionMethodBuildItem::hasNamespace)
                .sorted(Comparator.comparingInt(TemplateExtensionMethodBuildItem::getPriority).reversed())
                .collect(Collectors.groupingBy(TemplateExtensionMethodBuildItem::getNamespace));

        List<TemplateExtensionMethodBuildItem> regularExtensionMethods = templateExtensionMethods.stream()
                .filter(Predicate.not(TemplateExtensionMethodBuildItem::hasNamespace)).collect(Collectors.toUnmodifiableList());

        LookupConfig lookupConfig = new QuteProcessor.FixedLookupConfig(index, QuteProcessor.initDefaultMembersFilter(), false);

        // bundle name -> (key -> method)
        Map<String, Map<String, MethodInfo>> bundleMethodsMap = new HashMap<>();
        for (MessageBundleMethodBuildItem messageBundleMethod : messageBundleMethods) {
            Map<String, MethodInfo> bundleMethods = bundleMethodsMap.get(messageBundleMethod.getBundleName());
            if (bundleMethods == null) {
                bundleMethods = new HashMap<>();
                bundleMethodsMap.put(messageBundleMethod.getBundleName(), bundleMethods);
            }
            bundleMethods.put(messageBundleMethod.getKey(), messageBundleMethod.getMethod());
        }
        // bundle name -> bundle interface
        Map<String, ClassInfo> bundlesMap = new HashMap<>();
        for (MessageBundleBuildItem messageBundle : messageBundles) {
            bundlesMap.put(messageBundle.getName(), messageBundle.getDefaultBundleInterface());
        }

        for (Entry<String, Map<String, MethodInfo>> bundleEntry : bundleMethodsMap.entrySet()) {

            Map<TemplateAnalysis, Set<Expression>> expressions = QuteProcessor.collectNamespaceExpressions(analysis,
                    bundleEntry.getKey());
            Map<String, MethodInfo> methods = bundleEntry.getValue();
            ClassInfo defaultBundleInterface = bundlesMap.get(bundleEntry.getKey());

            if (!expressions.isEmpty()) {

                // Map implicit class -> set of used members
                Map<DotName, Set<String>> implicitClassToMembersUsed = new HashMap<>();

                for (Entry<TemplateAnalysis, Set<Expression>> exprEntry : expressions.entrySet()) {

                    TemplateAnalysis templateAnalysis = exprEntry.getKey();

                    String path = templateAnalysis.path;
                    for (String suffix : config.suffixes) {
                        if (path.endsWith(suffix)) {
                            path = path.substring(0, path.length() - (suffix.length() + 1));
                            break;
                        }
                    }
                    CheckedTemplateBuildItem checkedTemplate = null;
                    for (CheckedTemplateBuildItem item : checkedTemplates) {
                        if (item.templateId.equals(path)) {
                            checkedTemplate = item;
                            break;
                        }
                    }

                    Map<Integer, Match> generatedIdsToMatches = Collections.emptyMap();
                    for (TemplateExpressionMatchesBuildItem templateExpressionMatchesBuildItem : expressionMatches) {
                        if (templateExpressionMatchesBuildItem.templateGeneratedId.equals(templateAnalysis.generatedId)) {
                            generatedIdsToMatches = templateExpressionMatchesBuildItem.getGeneratedIdsToMatches();
                            break;
                        }
                    }

                    for (Expression expression : exprEntry.getValue()) {
                        // msg:hello_world(foo.name)
                        Part methodPart = expression.getParts().get(0);
                        if (methodPart.getName().equals(MESSAGE)) {
                            // Skip validation - dynamic key, e.g. msg:message(myKey,param1,param2)
                            continue;
                        }
                        MethodInfo method = methods.get(methodPart.getName());

                        if (method == null) {
                            if (!methodPart.isVirtualMethod() || methodPart.asVirtualMethod().getParameters().isEmpty()) {
                                // The method template may contain no expressions
                                method = defaultBundleInterface.method(methodPart.getName());
                            }
                            if (method == null) {
                                // User is referencing a non-existent message
                                incorrectExpressions.produce(new IncorrectExpressionBuildItem(expression.toOriginalString(),
                                        "Message bundle [name=" + bundleEntry.getKey() + ", interface="
                                                + defaultBundleInterface
                                                + "] does not define a method for key: " + methodPart.getName(),
                                        expression.getOrigin()));
                                continue;
                            }
                        }

                        if (methodPart.isVirtualMethod()) {
                            // For virtual method validate the number of params first  
                            List<Expression> params = methodPart.asVirtualMethod().getParameters();
                            List<Type> methodParams = method.parameters();

                            if (methodParams.size() != params.size()) {
                                incorrectExpressions.produce(new IncorrectExpressionBuildItem(expression.toOriginalString(),
                                        "Message bundle [name=" + bundleEntry.getKey() + ", interface="
                                                + defaultBundleInterface
                                                + "] - wrong number of parameters for method: " + method.toString(),
                                        expression.getOrigin()));
                                continue;
                            }

                            // Then attempt to validate the parameter types
                            int idx = 0;
                            for (Expression param : params) {
                                if (param.hasTypeInfo()) {
                                    Map<String, Match> results = new HashMap<>();
                                    QuteProcessor.validateNestedExpressions(config, exprEntry.getKey(), defaultBundleInterface,
                                            results, excludes, incorrectExpressions, expression, index,
                                            implicitClassToMembersUsed, templateIdToPathFun, generatedIdsToMatches,
                                            checkedTemplate, lookupConfig, namedBeans, namespaceTemplateData,
                                            regularExtensionMethods, namespaceExtensionMethods);
                                    Match match = results.get(param.toOriginalString());
                                    if (match != null && !match.isEmpty() && !Types.isAssignableFrom(match.type(),
                                            methodParams.get(idx), index)) {
                                        incorrectExpressions
                                                .produce(new IncorrectExpressionBuildItem(expression.toOriginalString(),
                                                        "Message bundle method " + method.declaringClass().name() + "#" +
                                                                method.name() + "() parameter [" + method.parameterName(idx)
                                                                + "] does not match the type: " + match.type(),
                                                        expression.getOrigin()));
                                    }
                                } else if (checkedTemplate != null && checkedTemplate.requireTypeSafeExpressions) {
                                    incorrectExpressions.produce(new IncorrectExpressionBuildItem(expression.toOriginalString(),
                                            "Only type-safe expressions are allowed in the checked template defined via: "
                                                    + checkedTemplate.method.declaringClass().name() + "."
                                                    + checkedTemplate.method.name()
                                                    + "(); an expression must be based on a checked template parameter "
                                                    + checkedTemplate.bindings.keySet()
                                                    + ", or bound via a param declaration, or the requirement must be relaxed via @CheckedTemplate(requireTypeSafeExpressions = false)",
                                            expression.getOrigin()));
                                }
                                idx++;
                            }
                        }
                    }
                }

                for (Entry<DotName, Set<String>> e : implicitClassToMembersUsed.entrySet()) {
                    ClassInfo clazz = index.getClassByName(e.getKey());
                    if (clazz != null) {
                        implicitClasses.produce(new ImplicitValueResolverBuildItem(clazz,
                                new TemplateDataBuilder().addIgnore(QuteProcessor.buildIgnorePattern(e.getValue())).build()));
                    }
                }
            }
        }
    }

    @BuildStep(onlyIf = IsNormal.class)
    void generateExamplePropertiesFiles(List<MessageBundleMethodBuildItem> messageBundleMethods,
            BuildSystemTargetBuildItem target, BuildProducer<GeneratedResourceBuildItem> dummy) throws IOException {
        Map<String, List<MessageBundleMethodBuildItem>> bundles = new HashMap<>();
        for (MessageBundleMethodBuildItem messageBundleMethod : messageBundleMethods) {
            if (messageBundleMethod.isDefaultBundle()) {
                List<MessageBundleMethodBuildItem> methods = bundles.get(messageBundleMethod.getBundleName());
                if (methods == null) {
                    methods = new ArrayList<>();
                    bundles.put(messageBundleMethod.getBundleName(), methods);
                }
                methods.add(messageBundleMethod);
            }
        }
        Path generatedExamplesDir = target.getOutputDirectory()
                .resolve("qute-i18n-examples");
        Files.createDirectories(generatedExamplesDir);
        for (Entry<String, List<MessageBundleMethodBuildItem>> entry : bundles.entrySet()) {
            List<MessageBundleMethodBuildItem> messages = entry.getValue();
            messages.sort(Comparator.comparing(MessageBundleMethodBuildItem::getKey));
            Path exampleProperfies = generatedExamplesDir.resolve(entry.getKey() + ".properties");
            Files.write(exampleProperfies,
                    messages.stream().map(m -> m.getMethod().name() + "=" + m.getTemplate()).collect(Collectors.toList()));
        }
    }

    private Map<String, String> generateImplementations(List<MessageBundleBuildItem> bundles,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<MessageBundleMethodBuildItem> messageTemplateMethods) throws IOException {

        Map<String, String> generatedTypes = new HashMap<>();

        ClassOutput defaultClassOutput = new GeneratedClassGizmoAdaptor(generatedClasses, new AppClassPredicate());

        for (MessageBundleBuildItem bundle : bundles) {
            ClassInfo bundleInterface = bundle.getDefaultBundleInterface();
            String bundleImpl = generateImplementation(null, null, bundleInterface, defaultClassOutput, messageTemplateMethods,
                    Collections.emptyMap(), null);
            generatedTypes.put(bundleInterface.name().toString(), bundleImpl);
            for (ClassInfo localizedInterface : bundle.getLocalizedInterfaces().values()) {
                generatedTypes.put(localizedInterface.name().toString(),
                        generateImplementation(bundle.getDefaultBundleInterface(), bundleImpl, localizedInterface,
                                defaultClassOutput,
                                messageTemplateMethods, Collections.emptyMap(), null));
            }

            for (Entry<String, Path> entry : bundle.getLocalizedFiles().entrySet()) {
                Path localizedFile = entry.getValue();
                Map<String, String> keyToTemplate = new HashMap<>();
                for (ListIterator<String> it = Files.readAllLines(localizedFile).listIterator(); it.hasNext();) {
                    String line = it.next();
                    if (line.startsWith("#") || line.isBlank()) {
                        // Comments and blank lines are skipped
                        continue;
                    }
                    int eqIdx = line.indexOf('=');
                    if (eqIdx == -1) {
                        throw new MessageBundleException(
                                "Missing key/value separator\n\t- file: " + localizedFile + "\n\t- line " + it.previousIndex());
                    }
                    String key = line.substring(0, eqIdx).strip();
                    if (!hasMessageBundleMethod(bundleInterface, key)) {
                        throw new MessageBundleException(
                                "Message bundle method " + key + "() not found on: " + bundleInterface + "\n\t- file: "
                                        + localizedFile + "\n\t- line " + it.previousIndex());
                    }
                    String value = adaptLine(line.substring(eqIdx + 1, line.length()));
                    if (value.endsWith("\\")) {
                        // The logical line is spread out across several normal lines
                        StringBuilder builder = new StringBuilder(value.substring(0, value.length() - 1));
                        constructLine(builder, it);
                        keyToTemplate.put(key, builder.toString());
                    } else {
                        keyToTemplate.put(key, value);
                    }
                }

                String locale = entry.getKey();
                ClassOutput localeAwareGizmoAdaptor = new GeneratedClassGizmoAdaptor(generatedClasses,
                        new AppClassPredicate(new Function<String, String>() {
                            @Override
                            public String apply(String className) {
                                String localeSuffix = "_" + locale;
                                if (className.endsWith(localeSuffix)) {
                                    return className.replace(localeSuffix, "");
                                }
                                return className;
                            }
                        }));
                generatedTypes.put(localizedFile.toString(),
                        generateImplementation(bundle.getDefaultBundleInterface(), bundleImpl, bundleInterface,
                                localeAwareGizmoAdaptor,
                                messageTemplateMethods, keyToTemplate, locale));
            }
        }
        return generatedTypes;
    }

    private void constructLine(StringBuilder builder, Iterator<String> it) {
        if (it.hasNext()) {
            String nextLine = adaptLine(it.next());
            if (nextLine.endsWith("\\")) {
                builder.append(nextLine.substring(0, nextLine.length() - 1));
                constructLine(builder, it);
            } else {
                builder.append(nextLine);
            }
        }
    }

    private String adaptLine(String line) {
        return line.stripLeading().replace("\\n", "\n");
    }

    private boolean hasMessageBundleMethod(ClassInfo bundleInterface, String name) {
        for (MethodInfo method : bundleInterface.methods()) {
            if (method.name().equals(name) && method.hasAnnotation(Names.MESSAGE)) {
                return true;
            }
        }
        return false;
    }

    private String generateImplementation(ClassInfo defaultBundleInterface, String defaultBundleImpl, ClassInfo bundleInterface,
            ClassOutput classOutput, BuildProducer<MessageBundleMethodBuildItem> messageTemplateMethods,
            Map<String, String> messageTemplates, String locale) {

        LOGGER.debugf("Generate bundle implementation for %s", bundleInterface);
        AnnotationInstance bundleAnnotation = defaultBundleInterface != null
                ? defaultBundleInterface.classAnnotation(Names.BUNDLE)
                : bundleInterface.classAnnotation(Names.BUNDLE);
        AnnotationValue nameValue = bundleAnnotation.value();
        String bundleName = nameValue != null ? nameValue.asString() : MessageBundle.DEFAULT_NAME;
        AnnotationValue defaultKeyValue = bundleAnnotation.value(BUNDLE_DEFAULT_KEY);

        String baseName;
        if (bundleInterface.enclosingClass() != null) {
            baseName = DotNames.simpleName(bundleInterface.enclosingClass()) + ValueResolverGenerator.NESTED_SEPARATOR
                    + DotNames.simpleName(bundleInterface);
        } else {
            baseName = DotNames.simpleName(bundleInterface);
        }
        if (locale != null) {
            baseName = baseName + "_" + locale;
        }

        String targetPackage = DotNames.internalPackageNameWithTrailingSlash(bundleInterface.name());
        String generatedName = targetPackage + baseName + SUFFIX;

        // MyMessages_Bundle implements MyMessages, Resolver
        Builder builder = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .interfaces(bundleInterface.name().toString(), Resolver.class.getName());
        if (defaultBundleImpl != null) {
            builder.superClass(defaultBundleImpl);
        }
        ClassCreator bundleCreator = builder.build();

        // key -> method
        Map<String, MethodInfo> keyMap = new LinkedHashMap<>();
        List<MethodInfo> methods = new ArrayList<>(bundleInterface.methods());
        // Sort methods
        methods.sort(Comparator.comparing(MethodInfo::name).thenComparing(Comparator.comparing(MethodInfo::toString)));

        for (MethodInfo method : methods) {
            if (!method.returnType().name().equals(DotNames.STRING)) {
                throw new MessageBundleException(
                        String.format("A message bundle interface method must return java.lang.String on %s: %s",
                                bundleInterface, method));
            }
            LOGGER.debugf("Found message bundle method %s on %s", method, bundleInterface);

            MethodCreator bundleMethod = bundleCreator.getMethodCreator(MethodDescriptor.of(method));

            AnnotationInstance messageAnnotation;
            if (defaultBundleInterface != null) {
                MethodInfo defaultBundleMethod = bundleInterface.method(method.name(),
                        method.parameters().toArray(new Type[] {}));
                if (defaultBundleMethod == null) {
                    throw new MessageBundleException(
                            String.format("Default bundle method not found on %s: %s", bundleInterface, method));
                }
                messageAnnotation = defaultBundleMethod.annotation(Names.MESSAGE);
            } else {
                messageAnnotation = method.annotation(Names.MESSAGE);
            }

            if (messageAnnotation == null) {
                throw new MessageBundleException(
                        "A message bundle interface method must be annotated with @Message: " +
                                bundleInterface.name() + "#" + method.name());
            }

            String key = getKey(method, messageAnnotation, defaultKeyValue);
            if (key.equals(MESSAGE)) {
                throw new MessageBundleException(String.format(
                        "A message bundle interface method must not use the key 'message' which is reserved for dynamic lookup; defined for %s#%s()",
                        bundleInterface, method.name()));
            }
            if (keyMap.containsKey(key)) {
                throw new MessageBundleException(String.format("Duplicate key [%s] found on %s", key, bundleInterface));
            }
            keyMap.put(key, method);

            String messageTemplate = messageTemplates.get(method.name());
            if (messageTemplate == null) {
                messageTemplate = messageAnnotation.value().asString();
            }

            String templateId = null;
            if (messageTemplate.contains("}")) {
                if (defaultBundleInterface != null) {
                    if (locale == null) {
                        AnnotationInstance localizedAnnotation = bundleInterface.classAnnotation(Names.LOCALIZED);
                        locale = localizedAnnotation.value().asString();
                    }
                    templateId = bundleName + "_" + locale + "_" + key;
                } else {
                    templateId = bundleName + "_" + key;
                }
            }

            MessageBundleMethodBuildItem messageBundleMethod = new MessageBundleMethodBuildItem(bundleName, key, templateId,
                    method, messageTemplate, defaultBundleInterface == null);
            messageTemplateMethods
                    .produce(messageBundleMethod);

            if (!messageBundleMethod.isValidatable()) {
                // No expression/tag - no need to use qute
                bundleMethod.returnValue(bundleMethod.load(messageTemplate));
            } else {
                // Obtain the template, e.g. msg_hello_name
                ResultHandle template = bundleMethod.invokeStaticMethod(
                        io.quarkus.qute.deployment.Descriptors.BUNDLES_GET_TEMPLATE,
                        bundleMethod.load(templateId));
                // Create a template instance
                ResultHandle templateInstance = bundleMethod
                        .invokeInterfaceMethod(io.quarkus.qute.deployment.Descriptors.TEMPLATE_INSTANCE, template);
                List<Type> paramTypes = method.parameters();
                if (!paramTypes.isEmpty()) {
                    // Set data
                    int i = 0;
                    Iterator<Type> it = paramTypes.iterator();
                    while (it.hasNext()) {
                        String name = getParameterName(method, i);
                        bundleMethod.invokeInterfaceMethod(io.quarkus.qute.deployment.Descriptors.TEMPLATE_INSTANCE_DATA,
                                templateInstance,
                                bundleMethod.load(name), bundleMethod.getMethodParam(i));
                        i++;
                        it.next();
                    }
                }
                // Render the template
                // At this point it's already validated that the method returns String
                bundleMethod.returnValue(bundleMethod.invokeInterfaceMethod(
                        io.quarkus.qute.deployment.Descriptors.TEMPLATE_INSTANCE_RENDER, templateInstance));
            }
        }

        implementResolve(defaultBundleImpl, bundleCreator, keyMap);

        bundleCreator.close();
        return generatedName.replace('/', '.');
    }

    private String getParameterName(MethodInfo method, int position) {
        String name = method.parameterName(position);
        AnnotationInstance paramAnnotation = Annotations
                .find(Annotations.getParameterAnnotations(method.annotations()).stream()
                        .filter(a -> a.target().asMethodParameter().position() == position).collect(Collectors.toList()),
                        Names.MESSAGE_PARAM);
        if (paramAnnotation != null) {
            AnnotationValue paramAnnotationValue = paramAnnotation.value();
            if (paramAnnotationValue != null && !paramAnnotationValue.asString().equals(Message.ELEMENT_NAME)) {
                name = paramAnnotationValue.asString();
            }
        }
        if (name == null) {
            throw new MessageBundleException("Unable to determine the name of the parameter at position " + position
                    + " in method "
                    + method.declaringClass().name() + "#" + method.name()
                    + "() - compile the class with debug info enabled (-g) or parameter names recorded (-parameters), or use @MessageParam to specify the value");
        }
        return name;
    }

    private void implementResolve(String defaultBundleImpl, ClassCreator bundleCreator, Map<String, MethodInfo> keyMap) {
        MethodCreator resolve = bundleCreator.getMethodCreator("resolve", CompletionStage.class, EvalContext.class);
        String resolveMethodPrefix = bundleCreator.getClassName().contains("/")
                ? bundleCreator.getClassName().substring(bundleCreator.getClassName().lastIndexOf('/') + 1)
                : bundleCreator.getClassName();

        ResultHandle evalContext = resolve.getMethodParam(0);
        ResultHandle name = resolve.invokeInterfaceMethod(Descriptors.GET_NAME, evalContext);
        ResultHandle ret = resolve.newInstance(MethodDescriptor.ofConstructor(CompletableFuture.class));

        // First handle dynamic messages, i.e. the "message" virtual method 
        BytecodeCreator dynamicMessage = resolve.ifTrue(resolve.invokeVirtualMethod(Descriptors.EQUALS,
                resolve.load(MESSAGE), name))
                .trueBranch();
        ResultHandle evaluatedMessageKey = dynamicMessage.invokeStaticMethod(Descriptors.EVALUATED_PARAMS_EVALUATE_MESSAGE_KEY,
                evalContext);
        ResultHandle paramsReady = dynamicMessage.readInstanceField(Descriptors.EVALUATED_PARAMS_STAGE,
                evaluatedMessageKey);

        // Define function called when the message key is ready
        FunctionCreator whenCompleteFun = dynamicMessage.createFunction(BiConsumer.class);
        dynamicMessage.invokeInterfaceMethod(Descriptors.CF_WHEN_COMPLETE, paramsReady, whenCompleteFun.getInstance());
        BytecodeCreator whenComplete = whenCompleteFun.getBytecode();
        AssignableResultHandle whenThis = whenComplete
                .createVariable(DescriptorUtils.extToInt(bundleCreator.getClassName()));
        whenComplete.assign(whenThis, dynamicMessage.getThis());
        AssignableResultHandle whenRet = whenComplete.createVariable(CompletableFuture.class);
        whenComplete.assign(whenRet, ret);
        AssignableResultHandle whenEvalContext = whenComplete.createVariable(EvalContext.class);
        whenComplete.assign(whenEvalContext, evalContext);
        BranchResult throwableIsNull = whenComplete.ifNull(whenComplete.getMethodParam(1));
        BytecodeCreator success = throwableIsNull.trueBranch();

        // Return if the name is null or NOT_FOUND
        ResultHandle resultNotFound = success.invokeStaticMethod(Descriptors.NOT_FOUND_FROM_EC, whenEvalContext);
        BytecodeCreator nameIsNull = success.ifNull(whenComplete.getMethodParam(0)).trueBranch();
        nameIsNull.invokeVirtualMethod(Descriptors.COMPLETABLE_FUTURE_COMPLETE, whenRet,
                resultNotFound);
        nameIsNull.returnValue(null);
        BytecodeCreator nameNotFound = success.ifTrue(success.invokeVirtualMethod(Descriptors.EQUALS,
                whenComplete.getMethodParam(0), resultNotFound)).trueBranch();
        nameNotFound.invokeVirtualMethod(Descriptors.COMPLETABLE_FUTURE_COMPLETE, whenRet, resultNotFound);
        nameNotFound.returnValue(null);

        // Evaluate the rest of the params
        ResultHandle evaluatedMessageParams = success.invokeStaticMethod(
                Descriptors.EVALUATED_PARAMS_EVALUATE_MESSAGE_PARAMS,
                whenEvalContext);
        // Delegate to BundleClassName_resolve_0 (the first group of messages)
        ResultHandle res0Ret = success.invokeVirtualMethod(
                MethodDescriptor.ofMethod(bundleCreator.getClassName(), resolveMethodPrefix + "_resolve_0",
                        CompletableFuture.class, String.class,
                        EvaluatedParams.class, CompletableFuture.class),
                whenThis, whenComplete.getMethodParam(0), evaluatedMessageParams, whenRet);
        BytecodeCreator ret0Null = success.ifNull(res0Ret).trueBranch();
        ret0Null.invokeVirtualMethod(Descriptors.COMPLETABLE_FUTURE_COMPLETE, whenRet,
                resultNotFound);
        BytecodeCreator failure = throwableIsNull.falseBranch();
        failure.invokeVirtualMethod(Descriptors.COMPLETABLE_FUTURE_COMPLETE_EXCEPTIONALLY, whenRet,
                whenComplete.getMethodParam(1));
        whenComplete.returnValue(null);
        // Return from the resolve method
        dynamicMessage.returnValue(ret);

        // Proceed with generated messages
        // We do group messages to workaround limits of a JVM method body 
        ResultHandle evaluatedParams = resolve.invokeStaticMethod(Descriptors.EVALUATED_PARAMS_EVALUATE, evalContext);
        final int groupLimit = 300;
        int groupIndex = 0;
        int resolveIndex = 0;
        MethodCreator resolveGroup = null;

        for (Entry<String, MethodInfo> entry : keyMap.entrySet()) {
            if (resolveGroup == null || groupIndex++ >= groupLimit) {
                groupIndex = 0;
                String resolveMethodName = resolveMethodPrefix + "_resolve_" + resolveIndex++;
                if (resolveGroup != null) {
                    // Delegate to the next "resolve_x" method
                    resolveGroup.returnValue(resolveGroup.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(bundleCreator.getClassName(), resolveMethodName, CompletableFuture.class,
                                    String.class,
                                    EvaluatedParams.class, CompletableFuture.class),
                            resolveGroup.getThis(), resolveGroup.getMethodParam(0), resolveGroup.getMethodParam(1),
                            resolveGroup.getMethodParam(2)));
                }
                resolveGroup = bundleCreator.getMethodCreator(resolveMethodName, CompletableFuture.class, String.class,
                        EvaluatedParams.class, CompletableFuture.class).setModifiers(0);
                if (resolveIndex == 1) {
                    ResultHandle resRet = resolve.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(bundleCreator.getClassName(), resolveMethodName, CompletableFuture.class,
                                    String.class, EvaluatedParams.class, CompletableFuture.class),
                            resolve.getThis(), name, evaluatedParams, ret);
                    resolve.ifNotNull(resRet).trueBranch().returnValue(resRet);
                }
            }
            addMessageMethod(resolveGroup, entry.getKey(), entry.getValue(), resolveGroup.getMethodParam(0),
                    resolveGroup.getMethodParam(1), resolveGroup.getMethodParam(2), bundleCreator.getClassName());
        }

        if (resolveGroup != null) {
            // Last group - return null
            resolveGroup.returnValue(resolveGroup.loadNull());
        }

        if (defaultBundleImpl != null) {
            resolve.returnValue(resolve.invokeSpecialMethod(
                    MethodDescriptor.ofMethod(defaultBundleImpl, "resolve", CompletionStage.class, EvalContext.class),
                    resolve.getThis(), evalContext));
        } else {
            resolve.returnValue(resolve.invokeStaticMethod(Descriptors.RESULTS_NOT_FOUND_EC, evalContext));
        }
    }

    private void addMessageMethod(MethodCreator resolve, String key, MethodInfo method, ResultHandle name,
            ResultHandle evaluatedParams,
            ResultHandle ret, String bundleClass) {
        List<Type> methodParams = method.parameters();

        BytecodeCreator matched = resolve.ifTrue(resolve.invokeVirtualMethod(Descriptors.EQUALS,
                resolve.load(key), name))
                .trueBranch();
        if (method.parameters().isEmpty()) {
            matched.invokeVirtualMethod(Descriptors.COMPLETABLE_FUTURE_COMPLETE, ret,
                    matched.invokeInterfaceMethod(method, matched.getThis()));
            matched.returnValue(ret);
        } else {
            // The CompletionStage upon which we invoke whenComplete()
            ResultHandle paramsReady = matched.readInstanceField(Descriptors.EVALUATED_PARAMS_STAGE,
                    evaluatedParams);

            FunctionCreator whenCompleteFun = matched.createFunction(BiConsumer.class);
            matched.invokeInterfaceMethod(Descriptors.CF_WHEN_COMPLETE, paramsReady, whenCompleteFun.getInstance());

            BytecodeCreator whenComplete = whenCompleteFun.getBytecode();

            AssignableResultHandle whenThis = whenComplete
                    .createVariable(DescriptorUtils.extToInt(bundleClass));
            whenComplete.assign(whenThis, matched.getThis());
            AssignableResultHandle whenRet = whenComplete.createVariable(CompletableFuture.class);
            whenComplete.assign(whenRet, ret);

            BranchResult throwableIsNull = whenComplete.ifNull(whenComplete.getMethodParam(1));

            // complete
            BytecodeCreator success = throwableIsNull.trueBranch();

            ResultHandle[] paramsHandle = new ResultHandle[methodParams.size()];
            if (methodParams.size() == 1) {
                paramsHandle[0] = whenComplete.getMethodParam(0);
            } else {
                for (int i = 0; i < methodParams.size(); i++) {
                    paramsHandle[i] = success.invokeVirtualMethod(Descriptors.EVALUATED_PARAMS_GET_RESULT,
                            evaluatedParams,
                            success.load(i));
                }
            }

            AssignableResultHandle invokeRet = success.createVariable(Object.class);
            // try
            TryBlock tryCatch = success.tryBlock();
            // catch (Throwable e)
            CatchBlockCreator exception = tryCatch.addCatch(Throwable.class);
            // CompletableFuture.completeExceptionally(Throwable)
            exception.invokeVirtualMethod(Descriptors.COMPLETABLE_FUTURE_COMPLETE_EXCEPTIONALLY, whenRet,
                    exception.getCaughtException());

            tryCatch.assign(invokeRet,
                    tryCatch.invokeInterfaceMethod(MethodDescriptor.of(method), whenThis, paramsHandle));

            tryCatch.invokeVirtualMethod(Descriptors.COMPLETABLE_FUTURE_COMPLETE, whenRet, invokeRet);
            // CompletableFuture.completeExceptionally(Throwable)
            BytecodeCreator failure = throwableIsNull.falseBranch();
            failure.invokeVirtualMethod(Descriptors.COMPLETABLE_FUTURE_COMPLETE_EXCEPTIONALLY, whenRet,
                    whenComplete.getMethodParam(1));
            whenComplete.returnValue(null);

            matched.returnValue(ret);
        }
    }

    @SuppressWarnings("deprecation")
    private String getKey(MethodInfo method, AnnotationInstance messageAnnotation, AnnotationValue defaultKeyValue) {
        AnnotationValue keyValue = messageAnnotation.value("key");
        String key;
        if (keyValue == null) {
            // Use the strategy from @MessageBundle
            key = defaultKeyValue != null ? defaultKeyValue.asString() : Message.ELEMENT_NAME;
        } else {
            key = keyValue.asString();
        }
        switch (key) {
            case Message.ELEMENT_NAME:
                return method.name();
            case Message.HYPHENATED_ELEMENT_NAME:
                return StringUtil.hyphenate(method.name());
            case Message.UNDERSCORED_ELEMENT_NAME:
                return StringUtil.join("_", StringUtil.lowerCase(StringUtil.camelHumpsIterator(method.name())));
            default:
                return keyValue.asString();
        }
    }

    private String getDefaultLocale(AnnotationInstance bundleAnnotation) {
        AnnotationValue localeValue = bundleAnnotation.value(BUNDLE_LOCALE);
        String defaultLocale;
        if (localeValue == null || localeValue.asString().equals(MessageBundle.DEFAULT_LOCALE)) {
            defaultLocale = Locale.getDefault().toLanguageTag();
        } else {
            defaultLocale = localeValue.asString();
        }
        return defaultLocale;
    }

    private Set<Path> findMessageFiles(ApplicationArchivesBuildItem applicationArchivesBuildItem) throws IOException {
        ApplicationArchive applicationArchive = applicationArchivesBuildItem.getRootArchive();
        Path messagesPath = applicationArchive.getChildPath(MESSAGES);
        if (messagesPath == null) {
            return Collections.emptySet();
        }
        Set<Path> messageFiles = new HashSet<>();
        try (Stream<Path> files = Files.list(messagesPath)) {
            Iterator<Path> iter = files.iterator();
            while (iter.hasNext()) {
                Path filePath = iter.next();
                if (Files.isRegularFile(filePath)) {
                    messageFiles.add(filePath);
                }
            }
        }
        return messageFiles;
    }

    private static class AppClassPredicate implements Predicate<String> {

        private final Function<String, String> additionalClassNameSanitizer;

        public AppClassPredicate() {
            this(Function.identity());
        }

        public AppClassPredicate(Function<String, String> additionalClassNameSanitizer) {
            this.additionalClassNameSanitizer = additionalClassNameSanitizer;
        }

        @Override
        public boolean test(String name) {
            int idx = name.lastIndexOf(SUFFIX);
            // org/acme/Foo_Bundle -> org.acme.Foo
            String className = name.substring(0, idx).replace("/", ".");
            if (className.contains(ValueResolverGenerator.NESTED_SEPARATOR)) {
                className = className.replace(ValueResolverGenerator.NESTED_SEPARATOR, "$");
            }
            // E.g. to match the bundle class generated for a localized file; org.acme.Foo_en -> org.acme.Foo
            className = additionalClassNameSanitizer.apply(className);
            return GeneratedClassGizmoAdaptor.isApplicationClass(className);
        }
    }
}
