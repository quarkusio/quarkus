package io.quarkus.qute.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;
import static java.util.stream.Collectors.toMap;
import static org.jboss.jandex.gizmo2.Jandex2Gizmo.classDescOf;
import static org.jboss.jandex.gizmo2.Jandex2Gizmo.methodDescOf;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.constant.ClassDesc;
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

import jakarta.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassInfo.NestingType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
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
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.GeneratedClassGizmo2Adaptor;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.IsProduction;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.gizmo2.ClassOutput;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.ParamVar;
import io.quarkus.gizmo2.Var;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.creator.ClassCreator;
import io.quarkus.gizmo2.desc.ClassMethodDesc;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.gizmo2.desc.MethodDesc;
import io.quarkus.qute.EvalContext;
import io.quarkus.qute.EvaluatedParams;
import io.quarkus.qute.Expression;
import io.quarkus.qute.Expression.Part;
import io.quarkus.qute.Expressions;
import io.quarkus.qute.Namespaces;
import io.quarkus.qute.Resolver;
import io.quarkus.qute.SectionHelperFactory;
import io.quarkus.qute.TemplateException;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.deployment.QuteProcessor.JavaMemberLookupConfig;
import io.quarkus.qute.deployment.QuteProcessor.MatchResult;
import io.quarkus.qute.deployment.TemplatesAnalysisBuildItem.TemplateAnalysis;
import io.quarkus.qute.deployment.Types.AssignabilityCheck;
import io.quarkus.qute.generator.Descriptors;
import io.quarkus.qute.generator.ValueResolverGenerator;
import io.quarkus.qute.i18n.Localized;
import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;
import io.quarkus.qute.i18n.MessageBundles;
import io.quarkus.qute.i18n.MessageTemplateLocator;
import io.quarkus.qute.runtime.MessageBundleRecorder;
import io.quarkus.qute.runtime.MessageBundleRecorder.MessageInfo;
import io.quarkus.qute.runtime.QuteConfig;
import io.quarkus.runtime.LocalesBuildTimeConfig;
import io.quarkus.runtime.util.StringUtil;

public class MessageBundleProcessor {

    private static final Logger LOG = Logger.getLogger(MessageBundleProcessor.class);

    private static final String SUFFIX = "_Bundle";
    private static final String BUNDLE_DEFAULT_KEY = "defaultKey";
    private static final String BUNDLE_LOCALE = "locale";
    private static final String MESSAGES = "messages";
    private static final String MESSAGE = "message";

    @BuildStep
    AdditionalBeanBuildItem beans() {
        return new AdditionalBeanBuildItem(MessageBundles.class, MessageBundle.class, Message.class, Localized.class,
                MessageTemplateLocator.class);
    }

    @BuildStep
    List<MessageBundleBuildItem> processBundles(BeanArchiveIndexBuildItem beanArchiveIndex,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<GeneratedResourceBuildItem> generatedResources,
            BeanRegistrationPhaseBuildItem beanRegistration,
            BuildProducer<BeanConfiguratorBuildItem> configurators,
            BuildProducer<MessageBundleMethodBuildItem> messageTemplateMethods,
            BuildProducer<HotDeploymentWatchedFileBuildItem> watchedFiles,
            LocalesBuildTimeConfig locales) throws IOException {

        IndexView index = beanArchiveIndex.getIndex();
        Map<String, ClassInfo> found = new HashMap<>();
        List<MessageBundleBuildItem> bundles = new ArrayList<>();
        List<DotName> localizedInterfaces = new ArrayList<>();
        // Message files sorted by priority
        List<MessageFile> messageFiles = findMessageFiles(applicationArchivesBuildItem, watchedFiles);

        // First collect all interfaces annotated with @MessageBundle
        for (AnnotationInstance bundleAnnotation : index.getAnnotations(Names.BUNDLE)) {
            if (bundleAnnotation.target().kind() == Kind.CLASS) {
                ClassInfo bundleClass = bundleAnnotation.target().asClass();
                if (Modifier.isInterface(bundleClass.flags())) {
                    AnnotationValue nameValue = bundleAnnotation.value();
                    String name = nameValue != null ? nameValue.asString() : MessageBundle.DEFAULTED_NAME;
                    if (name.equals(MessageBundle.DEFAULTED_NAME)) {
                        if (bundleClass.nestingType() == NestingType.TOP_LEVEL) {
                            name = MessageBundle.DEFAULT_NAME;
                        } else {
                            // The name starts with the DEFAULT_NAME followed by an underscore, followed by simple names of all
                            // declaring classes in the hierarchy separated by underscores
                            List<String> names = new ArrayList<>();
                            names.add(DotNames.simpleName(bundleClass));
                            DotName enclosingName = bundleClass.enclosingClass();
                            while (enclosingName != null) {
                                ClassInfo enclosingClass = index.getClassByName(enclosingName);
                                if (enclosingClass != null) {
                                    names.add(DotNames.simpleName(enclosingClass));
                                    enclosingName = enclosingClass.nestingType() == NestingType.TOP_LEVEL ? null
                                            : enclosingClass.enclosingClass();
                                }
                            }
                            Collections.reverse(names);
                            name = String.join("_", names);
                        }
                        LOG.debugf("Message bundle %s: name defaulted to %s", bundleClass, name);
                    }
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
                    String defaultLocale = getDefaultLocale(bundleAnnotation, locales);
                    List<ClassInfo> localized = new ArrayList<>();
                    for (ClassInfo implementor : index.getKnownDirectSubinterfaces(bundleClass.name())) {
                        localized.add(implementor);
                    }
                    Map<String, ClassInfo> localeToInterface = new HashMap<>();
                    for (ClassInfo localizedInterface : localized) {
                        String locale = localizedInterface.declaredAnnotation(Names.LOCALIZED).value().asString();
                        if (defaultLocale.equals(locale)) {
                            throw new MessageBundleException(
                                    String.format(
                                            "Locale of [%s] conflicts with the locale [%s] of the default message bundle [%s]",
                                            localizedInterface, locale, bundleClass));
                        }
                        ClassInfo previous = localeToInterface.put(locale, localizedInterface);
                        if (previous != null) {
                            throw new MessageBundleException(String.format(
                                    "Cannot register [%s] - a localized message bundle interface exists for locale [%s]: %s",
                                    localizedInterface, locale, previous));
                        }
                        localizedInterfaces.add(localizedInterface.name());
                    }

                    // Find localized files
                    Map<String, List<MessageFile>> localeToFiles = new HashMap<>();
                    // Message templates not specified by a localized interface are looked up in a localized file (merge candidate)
                    Map<String, List<MessageFile>> localeToMergeCandidates = new HashMap<>();
                    for (MessageFile messageFile : messageFiles) {
                        if (messageFile.matchesBundle(name)) {
                            String locale = messageFile.getLocale(name);
                            if (locale == null) {
                                locale = defaultLocale;
                            }
                            ClassInfo localizedInterface = localeToInterface.get(locale);
                            List<MessageFile> files;
                            if (defaultLocale.equals(locale) || localizedInterface != null) {
                                files = localeToMergeCandidates.get(locale);
                                if (files == null) {
                                    files = new ArrayList<>();
                                    localeToMergeCandidates.put(locale, files);
                                }
                            } else {
                                files = localeToFiles.get(locale);
                                if (files == null) {
                                    files = new ArrayList<>();
                                    localeToFiles.put(locale, files);
                                }
                            }
                            files.add(messageFile);
                        }
                    }

                    // Check for duplicates again
                    checkForDuplicates(localeToMergeCandidates);
                    checkForDuplicates(localeToFiles);

                    bundles.add(new MessageBundleBuildItem(name, bundleClass, localeToInterface,
                            localeToFiles, localeToMergeCandidates, defaultLocale));
                } else {
                    throw new MessageBundleException("@MessageBundle must be declared on an interface: " + bundleClass);
                }
            }
        }

        // Detect interfaces annotated with @Localized that don't extend a message bundle interface
        for (AnnotationInstance localizedAnnotation : index.getAnnotations(Names.LOCALIZED)) {
            if (localizedAnnotation.target().kind() == Kind.CLASS) {
                ClassInfo localized = localizedAnnotation.target().asClass();
                if (Modifier.isInterface(localized.flags())) {
                    if (!localizedInterfaces.contains(localized.name())) {
                        throw new MessageBundleException(
                                "A localized message bundle interface must extend a message bundle interface: " + localized);
                    }
                } else {
                    throw new MessageBundleException("@Localized must be declared on an interface: " + localized);
                }
            }
        }

        // Generate implementations
        // name -> impl class
        Map<String, ClassDesc> generatedImplementations = generateImplementations(bundles, generatedClasses, generatedResources,
                messageTemplateMethods, index);

        // Register synthetic beans
        for (MessageBundleBuildItem bundle : bundles) {
            ClassInfo bundleInterface = bundle.getDefaultBundleInterface();
            beanRegistration.getContext().configure(bundleInterface.name())
                    .addType(bundle.getDefaultBundleInterface().name())
                    // The default message bundle - add both @Default and @Localized
                    .addQualifier(DotNames.DEFAULT).addQualifier().annotation(Names.LOCALIZED)
                    .addValue("value", getDefaultLocale(bundleInterface.declaredAnnotation(Names.BUNDLE), locales)).done()
                    .unremovable()
                    .scope(Singleton.class)
                    .creator(cg -> {
                        BlockCreator bc = cg.createMethod();
                        // Just create a new instance of the generated class
                        bc.return_(bc.new_(ConstructorDesc.of(
                                generatedImplementations.get(bundleInterface.name().toString()))));
                    }).done();

            // Localized interfaces
            for (ClassInfo localizedInterface : bundle.getLocalizedInterfaces().values()) {
                beanRegistration.getContext().configure(localizedInterface.name())
                        .addType(bundle.getDefaultBundleInterface().name())
                        .addQualifier(localizedInterface.declaredAnnotation(Names.LOCALIZED))
                        .unremovable()
                        .scope(Singleton.class)
                        .creator(cg -> {
                            BlockCreator bc = cg.createMethod();
                            // Just create a new instance of the generated class
                            bc.return_(bc.new_(ConstructorDesc.of(
                                    generatedImplementations.get(localizedInterface.name().toString()))));
                        }).done();
            }
            // Localized files
            for (Entry<String, List<MessageFile>> entry : bundle.getLocalizedFiles().entrySet()) {
                beanRegistration.getContext().configure(bundle.getDefaultBundleInterface().name())
                        .addType(bundle.getDefaultBundleInterface().name())
                        .addQualifier().annotation(Names.LOCALIZED)
                        .addValue("value", entry.getKey()).done()
                        .unremovable()
                        .scope(Singleton.class).creator(cg -> {
                            BlockCreator bc = cg.createMethod();
                            // Just create a new instance of the generated class
                            bc.return_(bc.new_(ConstructorDesc.of(
                                    generatedImplementations.get(entry.getValue().get(0).fileName()))));
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

        if (bundles.isEmpty()) {
            return;
        }

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

        Map<String, MessageInfo> templateIdToContent = messageBundleMethods.stream()
                .filter(MessageBundleMethodBuildItem::isValidatable).collect(
                        Collectors.toMap(MessageBundleMethodBuildItem::getTemplateId,
                                MessageBundleMethodBuildItem::getMessageInfo));

        syntheticBeans.produce(SyntheticBeanBuildItem.configure(MessageBundleRecorder.BundleContext.class)
                .scope(BuiltinScope.DEPENDENT.getInfo())
                .supplier(recorder.createContext(templateIdToContent, bundleInterfaces))
                .done());
    }

    @BuildStep
    void validateMessageBundleMethods(TemplatesAnalysisBuildItem templatesAnalysis,
            List<MessageBundleMethodBuildItem> messageBundleMethods,
            List<TemplateGlobalBuildItem> templateGlobals,
            BuildProducer<IncorrectExpressionBuildItem> incorrectExpressions) {

        if (messageBundleMethods.isEmpty()) {
            return;
        }

        Map<String, MessageBundleMethodBuildItem> bundleMethods = messageBundleMethods.stream()
                .filter(MessageBundleMethodBuildItem::isValidatable)
                .collect(Collectors.toMap(MessageBundleMethodBuildItem::getTemplateId, Function.identity()));
        Set<String> globals = templateGlobals.stream().map(TemplateGlobalBuildItem::getName)
                .collect(Collectors.toUnmodifiableSet());

        for (TemplateAnalysis analysis : templatesAnalysis.getAnalysis()) {
            MessageBundleMethodBuildItem messageBundleMethod = bundleMethods.get(analysis.id);
            if (messageBundleMethod != null) {
                // All top-level expressions without a namespace should be mapped to a param
                Set<String> usedParamNames = new HashSet<>();
                Set<String> paramNames = messageBundleMethod.hasMethod()
                        ? IntStream.range(0, messageBundleMethod.getMethod().parametersCount())
                                .mapToObj(idx -> getParameterName(messageBundleMethod.getMethod(), idx))
                                .collect(Collectors.toSet())
                        : Set.of();
                for (Expression expression : analysis.expressions) {
                    validateExpression(incorrectExpressions, messageBundleMethod, expression, paramNames, usedParamNames,
                            globals);
                }
                // Log a warning if a parameter is not used in the template
                for (String paramName : paramNames) {
                    if (!usedParamNames.contains(paramName)) {
                        LOG.warnf("Unused parameter found [%s] in the message template of: %s", paramName,
                                messageBundleMethod.getMethod().declaringClass().name() + "#"
                                        + messageBundleMethod.getMethod().name() + "()");
                    }
                }
            }
        }
    }

    private void validateExpression(BuildProducer<IncorrectExpressionBuildItem> incorrectExpressions,
            MessageBundleMethodBuildItem messageBundleMethod, Expression expression, Set<String> paramNames,
            Set<String> usedParamNames, Set<String> globals) {
        if (expression.isLiteral()) {
            return;
        }
        if (!expression.hasNamespace()) {
            Expression.Part firstPart = expression.getParts().get(0);
            String name = firstPart.getName();
            String typeInfo = firstPart.getTypeInfo();
            boolean isGlobal = globals.contains(name);
            boolean isLoopMetadata = typeInfo != null && typeInfo.endsWith(SectionHelperFactory.HINT_METADATA);
            // Type info derived from a parent section, e.g "it<loop#3>" and "foo<set#3>"
            boolean hasDerivedTypeInfo = typeInfo != null && !typeInfo.startsWith("" + Expressions.TYPE_INFO_SEPARATOR);

            if (!isGlobal && !isLoopMetadata && !hasDerivedTypeInfo) {
                if (typeInfo == null || !paramNames.contains(name)) {
                    // Expression has no type info or type info that does not match a method parameter
                    // expressions that have
                    incorrectExpressions.produce(new IncorrectExpressionBuildItem(expression.toOriginalString(),
                            name + " is not a parameter of the message bundle method: "
                                    + messageBundleMethod.getPathForAnalysis(),
                            expression.getOrigin()));
                } else {
                    usedParamNames.add(name);
                }
            }
        }
        // Inspect method params too
        for (Part part : expression.getParts()) {
            if (part.isVirtualMethod()) {
                for (Expression param : part.asVirtualMethod().getParameters()) {
                    validateExpression(incorrectExpressions, messageBundleMethod, param, paramNames, usedParamNames, globals);
                }
            }
        }
    }

    @BuildStep
    void validateMessageBundleMethodsInTemplates(TemplatesAnalysisBuildItem analysis,
            BeanArchiveIndexBuildItem beanArchiveIndex,
            List<TemplateExtensionMethodBuildItem> templateExtensionMethods,
            List<TypeCheckExcludeBuildItem> typeCheckExcludeBuildItems,
            List<MessageBundleBuildItem> messageBundles,
            List<MessageBundleMethodBuildItem> messageBundleMethods,
            List<TemplateExpressionMatchesBuildItem> expressionMatches,
            BuildProducer<IncorrectExpressionBuildItem> incorrectExpressions,
            BuildProducer<ImplicitValueResolverBuildItem> implicitClasses,
            List<CheckedTemplateBuildItem> checkedTemplates,
            BeanDiscoveryFinishedBuildItem beanDiscovery,
            List<TemplateDataBuildItem> templateData,
            QuteConfig config,
            List<TemplateGlobalBuildItem> globals) {

        if (messageBundles.isEmpty()) {
            return;
        }

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

        JavaMemberLookupConfig lookupConfig = new QuteProcessor.FixedJavaMemberLookupConfig(index,
                QuteProcessor.initDefaultMembersFilter(), false);
        AssignabilityCheck assignabilityCheck = new AssignabilityCheck(index);

        // bundle name -> (key -> method)
        Map<String, Map<String, MethodInfo>> bundleToMethods = new HashMap<>();
        for (MessageBundleMethodBuildItem messageBundleMethod : messageBundleMethods) {
            Map<String, MethodInfo> bundleMethods = bundleToMethods.get(messageBundleMethod.getBundleName());
            if (bundleMethods == null) {
                bundleMethods = new HashMap<>();
                bundleToMethods.put(messageBundleMethod.getBundleName(), bundleMethods);
            }
            bundleMethods.put(messageBundleMethod.getKey(), messageBundleMethod.getMethod());
        }
        // bundle name -> bundle interface
        Map<String, ClassInfo> bundlesMap = new HashMap<>();
        for (MessageBundleBuildItem messageBundle : messageBundles) {
            bundlesMap.put(messageBundle.getName(), messageBundle.getDefaultBundleInterface());
        }

        for (Entry<String, Map<String, MethodInfo>> bundleEntry : bundleToMethods.entrySet()) {

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
                    for (String suffix : config.suffixes()) {
                        if (path.endsWith(suffix)) {
                            path = path.substring(0, path.length() - (suffix.length() + 1));
                            break;
                        }
                    }
                    CheckedTemplateBuildItem checkedTemplate = null;
                    for (CheckedTemplateBuildItem item : checkedTemplates) {
                        if (item.isFragment()) {
                            continue;
                        }
                        if (item.templateId.equals(path)) {
                            checkedTemplate = item;
                            break;
                        }
                    }

                    Map<Integer, MatchResult> generatedIdsToMatches = Collections.emptyMap();
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
                            if (methods.containsKey(methodPart.getName())) {
                                // Skip validation - enum constant key
                                continue;
                            }
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
                            List<Type> methodParams = method.parameterTypes();

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
                                    Map<String, MatchResult> results = new HashMap<>();

                                    final List<Predicate<TypeCheckExcludeBuildItem.TypeCheck>> excludes = new ArrayList<>();
                                    // subset of excludes specific for extension methods
                                    final List<Predicate<TypeCheckExcludeBuildItem.TypeCheck>> extensionMethodExcludes = new ArrayList<>();
                                    for (TypeCheckExcludeBuildItem exclude : typeCheckExcludeBuildItems) {
                                        excludes.add(exclude.getPredicate());
                                        if (exclude.isExtensionMethodPredicate()) {
                                            extensionMethodExcludes.add(exclude.getPredicate());
                                        }
                                    }

                                    QuteProcessor.validateNestedExpressions(config, exprEntry.getKey(), defaultBundleInterface,
                                            results, excludes, incorrectExpressions, expression, index,
                                            implicitClassToMembersUsed, templateIdToPathFun, generatedIdsToMatches,
                                            extensionMethodExcludes, checkedTemplate, lookupConfig, namedBeans,
                                            namespaceTemplateData, regularExtensionMethods, namespaceExtensionMethods,
                                            assignabilityCheck, globals);
                                    MatchResult match = results.get(param.toOriginalString());
                                    if (match != null && !match.isEmpty() && !assignabilityCheck.isAssignableFrom(match.type(),
                                            methodParams.get(idx))) {
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
                                                    + checkedTemplate.getDescription()
                                                    + "; an expression must be based on a checked template parameter "
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
                    if (e.getValue().isEmpty()) {
                        // No members
                        continue;
                    }
                    ClassInfo clazz = index.getClassByName(e.getKey());
                    if (clazz != null) {
                        implicitClasses.produce(new ImplicitValueResolverBuildItem(clazz,
                                new TemplateDataBuilder().addIgnore(QuteProcessor.buildIgnorePattern(e.getValue())).build()));
                    }
                }
            }
        }
    }

    @BuildStep(onlyIf = IsProduction.class)
    void generateExamplePropertiesFiles(List<MessageBundleMethodBuildItem> messageBundleMethods,
            BuildSystemTargetBuildItem target, BuildProducer<GeneratedResourceBuildItem> dummy) throws IOException {
        if (messageBundleMethods.isEmpty()) {
            return;
        }
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
            Path exampleProperties = generatedExamplesDir.resolve(entry.getKey() + ".properties");
            List<String> lines = new ArrayList<>();
            for (MessageBundleMethodBuildItem m : messages) {
                if (m.hasMethod()) {
                    if (m.hasGeneratedTemplate()) {
                        // Skip messages with generated templates
                        continue;
                    }
                    // Keys are mapped to method names
                    lines.add(m.getMethod().name() + "=" + m.getTemplate());
                } else {
                    // No corresponding method declared - use the key instead
                    // For example, there is no method for generated enum constant message keys
                    lines.add(m.getKey() + "=" + m.getTemplate());
                }
            }
            Files.write(exampleProperties, lines);
        }
    }

    private Map<String, ClassDesc> generateImplementations(List<MessageBundleBuildItem> bundles,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<GeneratedResourceBuildItem> generatedResources,
            BuildProducer<MessageBundleMethodBuildItem> messageTemplateMethods,
            IndexView index) throws IOException {

        Map<String, ClassDesc> generatedTypes = new HashMap<>();

        ClassOutput defaultClassOutput = new GeneratedClassGizmo2Adaptor(generatedClasses, generatedResources,
                new AppClassPredicate());

        for (MessageBundleBuildItem bundle : bundles) {
            ClassInfo bundleInterface = bundle.getDefaultBundleInterface();

            // take message templates not specified by Message#value from corresponding localized file
            Map<String, String> defaultKeyToMap = getLocalizedFileKeyToTemplate(bundle, bundleInterface,
                    bundle.getDefaultLocale(), bundleInterface.methods(), null, index);
            MergeClassInfoWrapper bundleInterfaceWrapper = new MergeClassInfoWrapper(bundleInterface, null, null);

            // Generate implementation for the default bundle interface
            String bundleImpl = generateImplementation(bundle, null, null, bundleInterfaceWrapper,
                    defaultClassOutput, messageTemplateMethods, defaultKeyToMap, null, index);
            generatedTypes.put(bundleInterface.name().toString(), ClassDesc.of(bundleImpl));

            // Generate imeplementation for each localized interface
            for (Entry<String, ClassInfo> entry : bundle.getLocalizedInterfaces().entrySet()) {
                ClassInfo localizedInterface = entry.getValue();

                // take message templates not specified by Message#value from corresponding localized file
                Map<String, String> keyToMap = getLocalizedFileKeyToTemplate(bundle, bundleInterface, entry.getKey(),
                        localizedInterface.methods(), localizedInterface, index);
                MergeClassInfoWrapper localizedInterfaceWrapper = new MergeClassInfoWrapper(localizedInterface, bundleInterface,
                        keyToMap);

                generatedTypes.put(entry.getValue().name().toString(),
                        ClassDesc.of(generateImplementation(bundle, bundleInterface, bundleImpl, localizedInterfaceWrapper,
                                defaultClassOutput, messageTemplateMethods, keyToMap, null, index)));
            }

            // Generate implementation for each localized file
            for (Entry<String, List<MessageFile>> entry : bundle.getLocalizedFiles().entrySet()) {
                List<MessageFile> localizedFiles = entry.getValue();
                if (localizedFiles.isEmpty()) {
                    continue;
                }
                var keyToTemplate = parseKeyToTemplateFromLocalizedFiles(bundleInterface, localizedFiles, index);

                String locale = entry.getKey();
                ClassOutput localeAwareGizmoAdaptor = new GeneratedClassGizmo2Adaptor(generatedClasses, generatedResources,
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
                generatedTypes.put(localizedFiles.get(0).fileName(),
                        ClassDesc.of(generateImplementation(bundle, bundleInterface, bundleImpl,
                                new SimpleClassInfoWrapper(bundleInterface), localeAwareGizmoAdaptor, messageTemplateMethods,
                                keyToTemplate, locale, index)));
            }
        }
        return generatedTypes;
    }

    private Map<String, String> getLocalizedFileKeyToTemplate(MessageBundleBuildItem bundle,
            ClassInfo bundleInterface, String locale, List<MethodInfo> methods, ClassInfo localizedInterface, IndexView index)
            throws IOException {

        List<MessageFile> localizedFiles = bundle.getMergeCandidates().get(locale);
        if (localizedFiles != null) {
            Map<String, String> keyToTemplate = parseKeyToTemplateFromLocalizedFiles(bundleInterface, localizedFiles, index);
            if (!keyToTemplate.isEmpty()) {

                // keep message templates if value wasn't provided by Message#value
                methods
                        .stream()
                        .filter(method -> keyToTemplate.containsKey(method.name()))
                        .filter(method -> {
                            AnnotationInstance messageAnnotation;
                            if (localizedInterface != null) {
                                MethodInfo defaultBundleMethod = localizedInterface.method(method.name(),
                                        method.parameterTypes().toArray(new Type[] {}));
                                if (defaultBundleMethod == null) {
                                    return true;
                                }
                                messageAnnotation = defaultBundleMethod.annotation(Names.MESSAGE);
                            } else {
                                messageAnnotation = method.annotation(Names.MESSAGE);
                            }
                            if (messageAnnotation == null) {
                                messageAnnotation = AnnotationInstance.builder(Names.MESSAGE).value(Message.DEFAULT_VALUE)
                                        .add("name", Message.DEFAULT_NAME).build();
                            }
                            return getMessageAnnotationValue(messageAnnotation, false) != null;
                        })
                        .map(MethodInfo::name)
                        .forEach(keyToTemplate::remove);
                return keyToTemplate;
            }
        }
        return Map.of();
    }

    private Map<String, String> parseKeyToTemplateFromLocalizedFiles(ClassInfo bundleInterface,
            List<MessageFile> localizedFile, IndexView index) throws IOException {
        Map<String, String> keyToTemplate = new HashMap<>();
        for (MessageFile messageFile : localizedFile) {
            for (ListIterator<String> it = Files.readAllLines(messageFile.path()).listIterator(); it.hasNext();) {
                String line = it.next();
                if (line.isBlank()) {
                    // Blank lines are skipped
                    continue;
                }
                line = line.strip();
                if (line.startsWith("#")) {
                    // Comments are skipped
                    continue;
                }
                int eqIdx = line.indexOf('=');
                if (eqIdx == -1) {
                    throw new MessageBundleException(
                            "Missing key/value separator\n\t- file: " + localizedFile + "\n\t- line " + it.previousIndex());
                }
                String key = line.substring(0, eqIdx).strip();
                if (keyToTemplate.containsKey(key)) {
                    // Message template with higher priority takes precedence
                    continue;
                }
                if (!hasMessageBundleMethod(bundleInterface, key) && !isEnumConstantMessageKey(key, index, bundleInterface)) {
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
        }
        return keyToTemplate;
    }

    /**
     *
     * @param key
     * @param bundleInterface
     * @return {@code true} if the given key represents an enum constant message key, such as {@code myEnum_CONSTANT1}
     */
    boolean isEnumConstantMessageKey(String key, IndexView index, ClassInfo bundleInterface) {
        if (key.isBlank()) {
            return false;
        }
        return isEnumConstantMessageKey("_$", key, index, bundleInterface)
                || isEnumConstantMessageKey("_", key, index, bundleInterface);
    }

    private boolean isEnumConstantMessageKey(String separator, String key, IndexView index, ClassInfo bundleInterface) {
        int lastIdx = key.lastIndexOf(separator);
        if (lastIdx != -1 && lastIdx != key.length()) {
            String methodName = key.substring(0, lastIdx);
            String constant = key.substring(lastIdx + separator.length(), key.length());
            MethodInfo method = messageBundleMethod(bundleInterface, methodName);
            if (method != null && method.parametersCount() == 1) {
                Type paramType = method.parameterType(0);
                if (paramType.kind() == org.jboss.jandex.Type.Kind.CLASS) {
                    ClassInfo maybeEnum = index.getClassByName(paramType.name());
                    if (maybeEnum != null && maybeEnum.isEnum()) {
                        if (maybeEnum.fields().stream()
                                .filter(FieldInfo::isEnumConstant)
                                .map(FieldInfo::name)
                                .anyMatch(constant::equals)) {
                            return true;
                        }
                        throw new MessageBundleException(
                                String.format("%s is not an enum constant of %s: %s", constant, maybeEnum, key));
                    }
                }
            }
        }
        return false;
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
        return messageBundleMethod(bundleInterface, name) != null;
    }

    private MethodInfo messageBundleMethod(ClassInfo bundleInterface, String name) {
        for (MethodInfo method : bundleInterface.methods()) {
            if (method.name().equals(name)) {
                return method;
            }
        }
        return null;
    }

    private String generateImplementation(MessageBundleBuildItem bundle, ClassInfo defaultBundleInterface,
            String defaultBundleImpl, ClassInfoWrapper bundleInterfaceWrapper, ClassOutput classOutput,
            BuildProducer<MessageBundleMethodBuildItem> messageTemplateMethods,
            Map<String, String> messageTemplates, String locale, IndexView index) {

        ClassInfo bundleInterface = bundleInterfaceWrapper.getClassInfo();
        LOG.debugf("Generate bundle implementation for %s", bundleInterface);
        AnnotationInstance bundleAnnotation = defaultBundleInterface != null
                ? defaultBundleInterface.declaredAnnotation(Names.BUNDLE)
                : bundleInterface.declaredAnnotation(Names.BUNDLE);
        String bundleName = bundle.getName();
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
        String generatedClassName = bundleInterface.name()
                + (locale != null ? "_" + locale : "")
                + SUFFIX;
        String resolveMethodPrefix = baseName + SUFFIX;

        Gizmo gizmo = Gizmo.create(classOutput)
                .withDebugInfo(false)
                .withParameters(false);
        gizmo.class_(generatedClassName, cc -> {
            // MyMessages_Bundle implements MyMessages, Resolver
            cc.implements_(classDescOf(bundleInterface));
            cc.implements_(Resolver.class);
            if (defaultBundleImpl != null) {
                cc.extends_(ClassDesc.of(defaultBundleImpl));
            }
            cc.defaultConstructor();

            // key -> method
            Map<String, MessageMethod> keyMap = new LinkedHashMap<>();
            List<MethodInfo> methods = new ArrayList<>(bundleInterfaceWrapper.methods());
            // Sort methods
            methods.sort(Comparator.comparing(MethodInfo::name).thenComparing(Comparator.comparing(MethodInfo::toString)));

            for (MethodInfo method : methods) {
                cc.method(methodDescOf(method), mc -> {
                    List<ParamVar> params = new ArrayList<>(method.parametersCount());
                    for (int i = 0; i < method.parametersCount(); i++) {
                        String paramName = method.parameterName(i);
                        params.add(mc.parameter(paramName != null ? paramName : "arg" + i, i));
                    }
                    if (!method.returnType().name().equals(DotNames.STRING)) {
                        throw new MessageBundleException(
                                String.format("A message bundle method must return java.lang.String: %s#%s",
                                        bundleInterface, method.name()));
                    }
                    LOG.debugf("Found message bundle method %s on %s", method, bundleInterface);

                    AnnotationInstance messageAnnotation;
                    if (defaultBundleInterface != null) {
                        MethodInfo defaultBundleMethod = bundleInterfaceWrapper.method(method.name(),
                                method.parameterTypes().toArray(new Type[] {}));
                        if (defaultBundleMethod == null) {
                            throw new MessageBundleException(
                                    String.format("Default bundle method not found on %s: %s", bundleInterface, method));
                        }
                        messageAnnotation = defaultBundleMethod.annotation(Names.MESSAGE);
                    } else {
                        messageAnnotation = method.annotation(Names.MESSAGE);
                    }

                    if (messageAnnotation == null) {
                        LOG.debugf("@Message not declared on %s#%s - using the default key/value", bundleInterface, method);
                        messageAnnotation = AnnotationInstance.builder(Names.MESSAGE).value(Message.DEFAULT_VALUE)
                                .add("name", Message.DEFAULT_NAME).build();
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
                    keyMap.put(key, new SimpleMessageMethod(method));

                    boolean generatedTemplate = false;
                    String messageTemplate = messageTemplates.get(method.name());
                    if (messageTemplate == null) {
                        messageTemplate = getMessageAnnotationValue(messageAnnotation, true);
                    }

                    if (messageTemplate == null && defaultBundleInterface != null) {
                        // method is annotated with @Message without value() -> fallback to default locale
                        messageTemplate = getMessageAnnotationValue((defaultBundleInterface.method(method.name(),
                                method.parameterTypes().toArray(new Type[] {}))).annotation(Names.MESSAGE), true);
                    }

                    // We need some special handling for enum message bundle methods
                    // A message bundle method that accepts an enum and has no message template receives a generated template:
                    // {#when enumParamName}
                    //   {#is CONSTANT_1}{msg:myEnum_$CONSTANT_1}
                    //   {#is CONSTANT_2}{msg:myEnum_$CONSTANT_2}
                    //   ...
                    // {/when}
                    // Furthermore, a special message method is generated for each enum constant
                    // These methods are used to handle the {msg:myEnum$CONSTANT_1} and {msg:myEnum$CONSTANT_2}
                    if (messageTemplate == null && method.parametersCount() == 1) {
                        Type paramType = method.parameterType(0);
                        if (paramType.kind() == org.jboss.jandex.Type.Kind.CLASS) {
                            ClassInfo maybeEnum = index.getClassByName(paramType.name());
                            if (maybeEnum != null && maybeEnum.isEnum()) {
                                StringBuilder generatedMessageTemplate = new StringBuilder("{#when ")
                                        .append(getParameterName(method, 0))
                                        .append("}");
                                Set<String> enumConstants = maybeEnum.fields().stream().filter(FieldInfo::isEnumConstant)
                                        .map(FieldInfo::name).collect(Collectors.toSet());
                                String separator = enumConstantSeparator(enumConstants);
                                for (String enumConstant : enumConstants) {
                                    // myEnum_CONSTANT
                                    // myEnum_$CONSTANT_1
                                    // myEnum_$CONSTANT$NEXT
                                    String enumConstantKey = toEnumConstantKey(method.name(), separator, enumConstant);
                                    String enumConstantTemplate = messageTemplates.get(enumConstantKey);
                                    if (enumConstantTemplate == null) {
                                        throw new TemplateException(
                                                String.format("Enum constant message not found in bundle [%s] for key: %s",
                                                        bundleName + (locale != null ? "_" + locale : ""), enumConstantKey));
                                    }
                                    generatedMessageTemplate.append("{#is ")
                                            .append(enumConstant)
                                            .append("}{")
                                            .append(bundle.getName())
                                            .append(":")
                                            .append(enumConstantKey)
                                            .append("}");
                                    // For each constant we generate a method:
                                    // myEnum_CONSTANT(MyEnum val)
                                    // myEnum_$CONSTANT_1(MyEnum val)
                                    // myEnum_$CONSTANT$NEXT(MyEnum val)
                                    generateEnumConstantMessageMethod(cc, bundleName, locale, bundleInterface,
                                            defaultBundleInterface, enumConstantKey, keyMap, enumConstantTemplate,
                                            messageTemplateMethods);
                                }
                                generatedMessageTemplate.append("{/when}");
                                messageTemplate = generatedMessageTemplate.toString();
                                generatedTemplate = true;
                            }
                        }
                    }

                    if (messageTemplate == null) {
                        throw new MessageBundleException(
                                String.format("Message template for key [%s] is missing for default locale [%s]", key,
                                        bundle.getDefaultLocale()));
                    }

                    String templateId = null;
                    String defaultLocale = locale;
                    if (messageTemplate.contains("}")) {
                        // Qute is needed - at least one expression/section found
                        if (defaultBundleInterface != null) {
                            if (defaultLocale == null) {
                                AnnotationInstance localizedAnnotation = bundleInterface.declaredAnnotation(Names.LOCALIZED);
                                defaultLocale = localizedAnnotation.value().asString();
                            }
                            templateId = bundleName + "_" + defaultLocale + "_" + key;
                        } else {
                            templateId = bundleName + "_" + key;
                        }
                    }

                    MessageBundleMethodBuildItem messageBundleMethod = new MessageBundleMethodBuildItem(bundleName, key,
                            templateId,
                            method, messageTemplate, defaultBundleInterface == null, generatedTemplate);
                    messageTemplateMethods
                            .produce(messageBundleMethod);

                    String effectiveMessageTemplate = messageTemplate;
                    String effectiveTemplateId = templateId;
                    String effectiveLocale = defaultLocale;
                    mc.body(bc -> {
                        if (!messageBundleMethod.isValidatable()) {
                            // No expression/tag - no need to use qute
                            bc.return_(Const.of(effectiveMessageTemplate));
                        } else {
                            // Obtain the template, e.g. msg_hello_name
                            LocalVar template = bc.localVar("template", bc.invokeStatic(
                                    io.quarkus.qute.deployment.Descriptors.BUNDLES_GET_TEMPLATE,
                                    Const.of(effectiveTemplateId)));
                            // Create a template instance
                            LocalVar templateInstance = bc.localVar("templateInstance", bc
                                    .invokeInterface(io.quarkus.qute.deployment.Descriptors.TEMPLATE_INSTANCE, template));
                            if (effectiveLocale != null) {
                                bc.invokeInterface(
                                        MethodDesc.of(TemplateInstance.class, "setLocale", TemplateInstance.class,
                                                String.class),
                                        templateInstance, Const.of(effectiveLocale));
                            }
                            List<Type> paramTypes = method.parameterTypes();
                            if (!paramTypes.isEmpty()) {
                                // Set data
                                int i = 0;
                                Iterator<Type> it = paramTypes.iterator();
                                while (it.hasNext()) {
                                    String name = getParameterName(method, i);
                                    bc.invokeInterface(io.quarkus.qute.deployment.Descriptors.TEMPLATE_INSTANCE_DATA,
                                            templateInstance,
                                            Const.of(name), params.get(i));
                                    i++;
                                    it.next();
                                }
                            }
                            // Render the template
                            // At this point it's already validated that the method returns String
                            bc.return_(bc.invokeInterface(
                                    io.quarkus.qute.deployment.Descriptors.TEMPLATE_INSTANCE_RENDER, templateInstance));
                        }
                    });
                });
            }
            implementResolve(defaultBundleImpl, cc, keyMap, resolveMethodPrefix, generatedClassName);
        });
        return generatedClassName;
    }

    private String enumConstantSeparator(Set<String> enumConstants) {
        for (String constant : enumConstants) {
            if (constant.contains("_$")) {
                throw new MessageBundleException("A constant of a localized enum may not contain '_$': " + constant);
            }
            if (constant.contains("$") || constant.contains("_")) {
                // If any of the constants contains "_" or "$" then "_$" is used
                return "_$";
            }
        }
        return "_";
    }

    private String toEnumConstantKey(String methodName, String separator, String enumConstant) {
        return methodName + separator + enumConstant;
    }

    private void generateEnumConstantMessageMethod(ClassCreator bundleCreator, String bundleName, String locale,
            ClassInfo bundleInterface, ClassInfo defaultBundleInterface, String enumConstantKey,
            Map<String, MessageMethod> keyMap, String messageTemplate,
            BuildProducer<MessageBundleMethodBuildItem> messageTemplateMethods) {
        String templateId = null;
        if (messageTemplate.contains("}")) {
            if (defaultBundleInterface != null) {
                if (locale == null) {
                    AnnotationInstance localizedAnnotation = bundleInterface
                            .declaredAnnotation(Names.LOCALIZED);
                    locale = localizedAnnotation.value().asString();
                }
                templateId = bundleName + "_" + locale + "_" + enumConstantKey;
            } else {
                templateId = bundleName + "_" + enumConstantKey;
            }
        }

        MessageBundleMethodBuildItem messageBundleMethod = new MessageBundleMethodBuildItem(bundleName, enumConstantKey,
                templateId, null, messageTemplate, defaultBundleInterface == null, true);
        messageTemplateMethods.produce(messageBundleMethod);

        String effectiveTemplateId = templateId;
        String effectiveLocale = locale;
        bundleCreator.method(enumConstantKey, mc -> {
            mc.returning(String.class);
            mc.body(bc -> {
                if (!messageBundleMethod.isValidatable()) {
                    // No expression/tag - no need to use qute
                    bc.return_(Const.of(messageTemplate));
                } else {
                    // Obtain the template, e.g. msg_myEnum$CONSTANT_1
                    LocalVar template = bc.localVar("template", bc.invokeStatic(
                            io.quarkus.qute.deployment.Descriptors.BUNDLES_GET_TEMPLATE,
                            Const.of(effectiveTemplateId)));
                    // Create a template instance
                    LocalVar templateInstance = bc.localVar("templateInstance",
                            bc.invokeInterface(io.quarkus.qute.deployment.Descriptors.TEMPLATE_INSTANCE, template));
                    if (effectiveLocale != null) {
                        bc.invokeInterface(
                                MethodDesc.of(TemplateInstance.class, "setLocale", TemplateInstance.class,
                                        String.class),
                                templateInstance, Const.of(effectiveLocale));
                    }
                    // Render the template
                    bc.return_(bc.invokeInterface(
                            io.quarkus.qute.deployment.Descriptors.TEMPLATE_INSTANCE_RENDER, templateInstance));
                }
            });
            keyMap.put(enumConstantKey, new EnumConstantMessageMethod(mc.desc()));
        });

    }

    /**
     * @return {@link Message#value()} if value was provided
     */
    private String getMessageAnnotationValue(AnnotationInstance messageAnnotation, boolean useDefault) {
        var messageValue = messageAnnotation.value();
        if (messageValue == null || messageValue.asString().equals(Message.DEFAULT_VALUE)) {
            // no value was provided in annotation
            if (useDefault) {
                var defaultMessageValue = messageAnnotation.value("defaultValue");
                if (defaultMessageValue == null || defaultMessageValue.asString().equals(Message.DEFAULT_VALUE)) {
                    return null;
                }
                return defaultMessageValue.asString();
            } else {
                return null;
            }
        }
        return messageValue.asString();
    }

    static String getParameterName(MethodInfo method, int position) {
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

    private void implementResolve(String defaultBundleImpl, ClassCreator bundleCreator, Map<String, MessageMethod> keyMap,
            String resolveMethodPrefix, String generatedClassName) {

        // We do group messages to workaround limits of a JVM method body
        int groupLimit = 300;
        int groupIndex = 0;
        List<List<Entry<String, MessageMethod>>> resolveGroups = new ArrayList<>();
        List<Entry<String, MessageMethod>> resolveGroup = new ArrayList<>();
        for (Entry<String, MessageMethod> entry : keyMap.entrySet()) {
            if (groupIndex++ >= groupLimit) {
                groupIndex = 0;
                resolveGroups.add(resolveGroup);
                resolveGroup = new ArrayList<>();
            } else {
                resolveGroup.add(entry);
            }
        }
        if (!resolveGroup.isEmpty()) {
            // Add the last group
            resolveGroups.add(resolveGroup);
        }
        for (ListIterator<List<Entry<String, MessageMethod>>> it = resolveGroups.listIterator(); it.hasNext();) {
            int idx = it.nextIndex();
            List<Entry<String, MessageMethod>> group = it.next();
            String resolveMethodName = resolveMethodPrefix + "_resolve_" + idx;

            bundleCreator.method(resolveMethodName, mc -> {
                mc.returning(CompletableFuture.class);
                ParamVar name = mc.parameter("name", String.class);
                ParamVar evaluatedParams = mc.parameter("evaluatedParams", EvaluatedParams.class);
                ParamVar ret = mc.parameter("ret", CompletableFuture.class);

                mc.body(bc -> {
                    LocalVar this_ = bc.localVar("this_", bundleCreator.this_());
                    for (Entry<String, MessageMethod> entry : group) {
                        addMessageMethod(bundleCreator, bc, entry.getKey(), entry.getValue(), name, evaluatedParams,
                                ret, this_);
                    }
                    if (it.hasNext()) {
                        // Delegate to the next "resolve_x" method
                        bc.return_(bc.invokeVirtual(ClassMethodDesc.of(ClassDesc.of(generatedClassName),
                                resolveMethodPrefix + "_resolve_" + (idx + 1),
                                CompletableFuture.class, String.class,
                                EvaluatedParams.class, CompletableFuture.class),
                                bundleCreator.this_(), name, evaluatedParams, bundleCreator.this_()));
                    } else {
                        // Last group - return null
                        bc.returnNull();
                    }
                });
            });
        }

        bundleCreator.method("resolve", mc -> {
            mc.returning(CompletionStage.class);
            ParamVar evalContext = mc.parameter("ec", EvalContext.class);

            mc.body(bc -> {
                LocalVar name = bc.localVar("name", bc.invokeInterface(Descriptors.GET_NAME, evalContext));
                LocalVar ret = bc.localVar("ret", bc.new_(CompletableFuture.class));

                // First handle dynamic messages, i.e. the "message" virtual method
                bc.if_(bc.objEquals(name, Const.of(MESSAGE)), dynamicMessage -> {
                    Expr evaluatedMessageKey = dynamicMessage.invokeStatic(Descriptors.EVALUATED_PARAMS_EVALUATE_MESSAGE_KEY,
                            evalContext);
                    Expr paramsReady = evaluatedMessageKey.field(Descriptors.EVALUATED_PARAMS_STAGE);

                    // Define function called when the message key is ready
                    Expr fun = dynamicMessage.lambda(BiConsumer.class, lc -> {
                        Var capturedRet = lc.capture(ret);
                        Var capturedEvalContext = lc.capture(evalContext);
                        Var capturedThis = lc.capture("this_", bundleCreator.this_());
                        ParamVar result = lc.parameter("r", 0);
                        ParamVar throwable = lc.parameter("t", 1);

                        lc.body(whenComplete -> {
                            whenComplete.ifElse(whenComplete.isNull(throwable),
                                    success -> {

                                        // Return if the name is null or NOT_FOUND
                                        success.ifNull(result, isNull -> {
                                            isNull.invokeVirtual(Descriptors.COMPLETABLE_FUTURE_COMPLETE, capturedRet,
                                                    isNull.invokeStatic(Descriptors.NOT_FOUND_FROM_EC, capturedEvalContext));
                                            isNull.return_();
                                        });
                                        success.if_(success.invokeStatic(Descriptors.RESULTS_IS_NOT_FOUND, result),
                                                isNotFound -> {
                                                    isNotFound.invokeVirtual(Descriptors.COMPLETABLE_FUTURE_COMPLETE,
                                                            capturedRet, result);
                                                    isNotFound.return_();
                                                });

                                        // Evaluate the rest of the params
                                        LocalVar evaluatedMessageParams = success.localVar("emp", success.invokeStatic(
                                                Descriptors.EVALUATED_PARAMS_EVALUATE_MESSAGE_PARAMS,
                                                capturedEvalContext));
                                        // Delegate to BundleClassName_resolve_0 (the first group of messages)
                                        Expr res0Ret = success.invokeVirtual(
                                                ClassMethodDesc.of(ClassDesc.of(generatedClassName),
                                                        resolveMethodPrefix + "_resolve_0",
                                                        CompletableFuture.class, String.class,
                                                        EvaluatedParams.class, CompletableFuture.class),
                                                capturedThis, result, evaluatedMessageParams, capturedRet);

                                        success.ifNull(res0Ret, resultIsNull -> {
                                            resultIsNull.invokeVirtual(Descriptors.COMPLETABLE_FUTURE_COMPLETE, capturedRet,
                                                    resultIsNull.invokeStatic(Descriptors.NOT_FOUND_FROM_EC,
                                                            capturedEvalContext));
                                        });

                                        success.return_();

                                    }, failure -> {
                                        failure.invokeVirtual(Descriptors.COMPLETABLE_FUTURE_COMPLETE_EXCEPTIONALLY,
                                                capturedRet,
                                                throwable);
                                        failure.return_();
                                    });
                        });
                    });
                    dynamicMessage.invokeInterface(Descriptors.CF_WHEN_COMPLETE, paramsReady, fun);
                    dynamicMessage.return_(ret);
                });

                Expr evaluatedParams = bc.invokeStatic(Descriptors.EVALUATED_PARAMS_EVALUATE, evalContext);
                LocalVar ret0 = bc.localVar("ret0", bc.invokeVirtual(
                        ClassMethodDesc.of(ClassDesc.of(generatedClassName),
                                resolveMethodPrefix + "_resolve_0",
                                CompletableFuture.class, String.class,
                                EvaluatedParams.class, CompletableFuture.class),
                        bundleCreator.this_(), name, evaluatedParams, ret));
                bc.ifNotNull(ret0, retNotNull -> retNotNull.return_(ret0));

                // Proceed with generated messages
                if (defaultBundleImpl != null) {
                    bc.return_(bc.invokeSpecial(
                            ClassMethodDesc.of(ClassDesc.of(defaultBundleImpl), "resolve", CompletionStage.class,
                                    EvalContext.class),
                            bundleCreator.this_(), evalContext));
                } else {
                    bc.return_(bc.invokeStatic(Descriptors.RESULTS_NOT_FOUND_EC, evalContext));
                }
            });
        });

    }

    private void addMessageMethod(ClassCreator cc, BlockCreator resolve, String key, MessageMethod method, Var name,
            Var evaluatedParams, Var ret, Var this_) {
        List<Type> methodParams = method.parameterTypes();

        resolve.if_(resolve.objEquals(name, Const.of(key)), matched -> {
            if (methodParams.isEmpty()) {
                matched.invokeVirtual(Descriptors.COMPLETABLE_FUTURE_COMPLETE, ret,
                        method.isMessageBundleInterfaceMethod()
                                ? matched.invokeInterface(method.descriptor(), this_)
                                : matched.invokeVirtual(method.descriptor(), this_));
                matched.return_(ret);
            } else {
                // The CompletionStage upon which we invoke whenComplete()
                Expr paramsReady = evaluatedParams.field(Descriptors.EVALUATED_PARAMS_STAGE);

                Expr fun = matched.lambda(BiConsumer.class, lc -> {
                    Var capturedRet = lc.capture(ret);
                    Var capturedThis = lc.capture(this_);
                    Var capturedEvaluatedParams = lc.capture(evaluatedParams);
                    ParamVar result = lc.parameter("r", 0);
                    ParamVar throwable = lc.parameter("t", 1);

                    lc.body(whenComplete -> {
                        whenComplete.ifElse(whenComplete.isNull(throwable),
                                success -> {

                                    Var[] args = new Var[methodParams.size()];
                                    if (methodParams.size() == 1) {
                                        args[0] = result;
                                    } else {
                                        for (int i = 0; i < methodParams.size(); i++) {
                                            args[i] = success.localVar("arg" + i,
                                                    success.invokeVirtual(Descriptors.EVALUATED_PARAMS_GET_RESULT,
                                                            capturedEvaluatedParams,
                                                            Const.of(i)));
                                        }
                                    }
                                    success.try_(tc -> {
                                        tc.body(tryBlock -> {
                                            tryBlock.invokeVirtual(Descriptors.COMPLETABLE_FUTURE_COMPLETE, capturedRet,
                                                    method.isMessageBundleInterfaceMethod()
                                                            ? tryBlock.invokeInterface(method.descriptor(), capturedThis,
                                                                    args)
                                                            : tryBlock.invokeVirtual(method.descriptor(), capturedThis,
                                                                    args));
                                        });
                                        tc.catch_(Throwable.class, "t", (cbc, t) -> {
                                            cbc.invokeVirtual(Descriptors.COMPLETABLE_FUTURE_COMPLETE_EXCEPTIONALLY,
                                                    capturedRet,
                                                    t);
                                        });
                                    });

                                }, failure -> {
                                    failure.invokeVirtual(Descriptors.COMPLETABLE_FUTURE_COMPLETE_EXCEPTIONALLY, capturedRet,
                                            throwable);
                                });
                        whenComplete.return_();
                    });

                });
                matched.invokeInterface(Descriptors.CF_WHEN_COMPLETE, paramsReady, fun);
                matched.return_(ret);
            }
        });
    }

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
                return String.join("_", new Iterable<String>() {
                    @Override
                    public Iterator<String> iterator() {
                        return StringUtil.lowerCase(StringUtil.camelHumpsIterator(method.name()));
                    }

                });
            default:
                return keyValue.asString();
        }
    }

    private String getDefaultLocale(AnnotationInstance bundleAnnotation, LocalesBuildTimeConfig locales) {
        AnnotationValue localeValue = bundleAnnotation.value(BUNDLE_LOCALE);
        String defaultLocale;
        if (localeValue == null || localeValue.asString().equals(MessageBundle.DEFAULT_LOCALE)) {
            defaultLocale = locales.defaultLocale().orElse(Locale.getDefault()).toLanguageTag();
        } else {
            defaultLocale = localeValue.asString();
        }
        return defaultLocale;
    }

    record MessageFile(Path path, String fileName, int priority) implements Comparable<MessageFile> {

        MessageFile(Path path, int priority) {
            this(path, path.getFileName().toString(), priority);
        }

        boolean matchesBundle(String bundleName) {
            String fileName = this.fileName;
            int fileSeparatorIdx = fileName.indexOf('.');
            // Remove file extension if exists
            if (fileSeparatorIdx > -1) {
                fileName = fileName.substring(0, fileSeparatorIdx);
            }
            // Split the filename and the bundle name by underscores
            String[] fileNameParts = fileName.split("_");
            String[] nameParts = bundleName.split("_");

            if (fileNameParts.length < nameParts.length) {
                return false;
            }

            // Compare each part of the filename with the corresponding part of the bundle name
            for (int i = 0; i < nameParts.length; i++) {
                if (!fileNameParts[i].equals(nameParts[i])) {
                    return false;
                }
            }
            return true;
        }

        String getLocale(String bundleName) {
            String fileName = this.fileName;
            int postfixIdx = fileName.indexOf('.');
            if (postfixIdx == bundleName.length()) {
                // msg.txt -> use bundle default locale
                return null;
            } else {
                return fileName
                        // msg_en.txt -> en
                        // msg_Views_Index_cs.properties -> cs
                        // msg_Views_Index_cs-CZ.properties -> cs-CZ
                        // msg_Views_Index_cs_CZ.properties -> cs_CZ
                        .substring(bundleName.length() + 1, postfixIdx)
                        // Support resource bundle naming convention
                        .replace('_', '-');
            }
        }

        @Override
        public int compareTo(MessageFile other) {
            // Higher priority goes first
            return Integer.compare(other.priority(), priority());
        }
    }

    private void checkForDuplicates(Map<String, List<MessageFile>> groupByName) {
        // Check for duplicates
        // If there are multiple message files of the same priority then fail the build
        for (var entry : groupByName.entrySet()) {
            if (entry.getValue().size() > 1) {
                Map<Integer, List<MessageFile>> groupByPriority = entry.getValue().stream()
                        .collect(Collectors.groupingBy(MessageFile::priority));
                for (var groupEntry : groupByPriority.entrySet()) {
                    if (groupEntry.getValue().size() > 1) {
                        StringBuilder builder = new StringBuilder("Duplicate localized files with priority ")
                                .append(groupEntry.getValue().get(0).priority())
                                .append(" found:\n\t- ")
                                .append(entry.getKey())
                                .append(": ")
                                .append(groupEntry.getValue());
                        throw new MessageBundleException(builder.toString());
                    }
                }
            }
        }
    }

    private List<MessageFile> findMessageFiles(ApplicationArchivesBuildItem applicationArchives,
            BuildProducer<HotDeploymentWatchedFileBuildItem> watchedFiles) throws IOException {

        List<MessageFile> messageFiles = new ArrayList<>();

        addMessageFiles(applicationArchives.getRootArchive(), 10, messageFiles);
        for (ApplicationArchive archive : applicationArchives.getApplicationArchives()) {
            addMessageFiles(archive, 1, messageFiles);
        }

        if (messageFiles.isEmpty()) {
            return List.of();
        }

        // Check for duplicates
        Map<String, List<MessageFile>> groupByName = messageFiles.stream()
                .collect(Collectors.groupingBy(MessageFile::fileName));
        checkForDuplicates(groupByName);

        // Sort message files by priority
        messageFiles.sort(null);

        // Hot deployment
        for (MessageFile messageFile : messageFiles) {
            watchedFiles.produce(new HotDeploymentWatchedFileBuildItem(MESSAGES + "/" + messageFile.fileName()));
        }

        return messageFiles;
    }

    private void addMessageFiles(ApplicationArchive archive, int priority,
            List<MessageFile> messageFiles) {
        archive.accept(tree -> {
            final Path messagesPath = tree.getPath(MESSAGES);
            if (messagesPath == null) {
                return;
            }
            try (Stream<Path> files = Files.list(messagesPath)) {
                Iterator<Path> iter = files.iterator();
                while (iter.hasNext()) {
                    Path filePath = iter.next();
                    if (Files.isRegularFile(filePath)) {
                        String messageFileName = messagesPath.relativize(filePath).toString();
                        if (File.separatorChar != '/') {
                            messageFileName = messageFileName.replace(File.separatorChar, '/');
                        }
                        messageFiles.add(new MessageFile(filePath, priority));
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
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

    private interface ClassInfoWrapper {

        ClassInfo getClassInfo();

        List<MethodInfo> methods();

        MethodInfo method(String name, Type... parameters);
    }

    private static class SimpleClassInfoWrapper implements ClassInfoWrapper {

        private final ClassInfo classInfo;

        SimpleClassInfoWrapper(ClassInfo classInfo) {
            this.classInfo = classInfo;
        }

        @Override
        public ClassInfo getClassInfo() {
            return classInfo;
        }

        @Override
        public final List<MethodInfo> methods() {
            return classInfo.methods();
        }

        @Override
        public final MethodInfo method(String name, Type... parameters) {
            return classInfo.method(name, parameters);
        }

    }

    private static class MergeClassInfoWrapper implements ClassInfoWrapper {

        private final ClassInfo classInfo;

        private final ClassInfo interfaceClassInfo;

        private final Map<String, MethodInfo> interfaceKeyToMethodInfo;

        MergeClassInfoWrapper(ClassInfo classInfo, ClassInfo interfaceClassInfo,
                Map<String, String> localizedFileKeyToTemplate) {
            this.classInfo = classInfo;
            this.interfaceClassInfo = interfaceClassInfo;

            // take methods missing in class info so each message template provided in file has its method
            if (interfaceClassInfo != null && localizedFileKeyToTemplate != null) {
                List<MethodInfo> classInfoMethods = classInfo.methods();
                interfaceKeyToMethodInfo = interfaceClassInfo
                        .methods()
                        .stream()
                        // keep method with message template in localized file
                        .filter(method -> localizedFileKeyToTemplate.containsKey(method.name()))
                        // if method is overridden, prefer implementation
                        .filter(method -> classInfoMethods.stream()
                                .noneMatch(m -> m.name().equals(method.name())))
                        .collect(toMap(MethodInfo::name, Function.identity()));
            } else {
                interfaceKeyToMethodInfo = Collections.emptyMap();
            }
        }

        @Override
        public ClassInfo getClassInfo() {
            return classInfo;
        }

        @Override
        public final List<MethodInfo> methods() {
            return Stream.concat(
                    interfaceKeyToMethodInfo.values().stream(),
                    classInfo.methods().stream()).collect(Collectors.toCollection(ArrayList::new));
        }

        @Override
        public final MethodInfo method(String name, Type... parameters) {

            if (interfaceKeyToMethodInfo.containsKey(name)) {
                return interfaceClassInfo.method(name, parameters);
            }
            return classInfo.method(name, parameters);
        }
    }

    interface MessageMethod {

        List<Type> parameterTypes();

        MethodDesc descriptor();

        default boolean isMessageBundleInterfaceMethod() {
            return true;
        }

    }

    static class SimpleMessageMethod implements MessageMethod {

        final MethodInfo method;

        SimpleMessageMethod(MethodInfo method) {
            this.method = method;
        }

        @Override
        public List<Type> parameterTypes() {
            return method.parameterTypes();
        }

        @Override
        public MethodDesc descriptor() {
            return methodDescOf(method);
        }

    }

    static class EnumConstantMessageMethod implements MessageMethod {

        final MethodDesc descriptor;

        EnumConstantMessageMethod(MethodDesc descriptor) {
            this.descriptor = descriptor;
        }

        @Override
        public List<Type> parameterTypes() {
            return List.of();
        }

        @Override
        public MethodDesc descriptor() {
            return descriptor;
        }

        @Override
        public boolean isMessageBundleInterfaceMethod() {
            return false;
        }

    }
}
