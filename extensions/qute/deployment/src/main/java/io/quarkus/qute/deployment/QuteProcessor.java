package io.quarkus.qute.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;
import static io.quarkus.qute.Namespaces.isDataNamespace;
import static io.quarkus.qute.ValueResolvers.OR;
import static io.quarkus.qute.runtime.EngineProducer.CDI_NAMESPACE;
import static io.quarkus.qute.runtime.EngineProducer.INJECT_NAMESPACE;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toMap;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
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
import org.jboss.jandex.ClassInfo.NestingType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.PrimitiveType;
import org.jboss.jandex.PrimitiveType.Primitive;
import org.jboss.jandex.Type;
import org.jboss.jandex.TypeVariable;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.deployment.CompletedApplicationClassPredicateBuildItem;
import io.quarkus.arc.deployment.QualifierRegistrarBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.processor.Annotations;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.arc.processor.QualifierRegistrar;
import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.fs.util.ZipUtils;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.panache.common.deployment.PanacheEntityClassesBuildItem;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.Engine;
import io.quarkus.qute.EngineBuilder;
import io.quarkus.qute.EngineConfiguration;
import io.quarkus.qute.ErrorCode;
import io.quarkus.qute.Expression;
import io.quarkus.qute.Expression.VirtualMethodPart;
import io.quarkus.qute.Expressions;
import io.quarkus.qute.LoopSectionHelper;
import io.quarkus.qute.NamespaceResolver;
import io.quarkus.qute.ParameterDeclaration;
import io.quarkus.qute.ParserHelper;
import io.quarkus.qute.ParserHook;
import io.quarkus.qute.ResultNode;
import io.quarkus.qute.SectionHelper;
import io.quarkus.qute.SectionHelperFactory;
import io.quarkus.qute.SetSectionHelper;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateData;
import io.quarkus.qute.TemplateException;
import io.quarkus.qute.TemplateExtension;
import io.quarkus.qute.TemplateGlobal;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.TemplateLocator;
import io.quarkus.qute.TemplateNode;
import io.quarkus.qute.UserTagSectionHelper;
import io.quarkus.qute.ValueResolver;
import io.quarkus.qute.Variant;
import io.quarkus.qute.WhenSectionHelper;
import io.quarkus.qute.deployment.TemplatesAnalysisBuildItem.TemplateAnalysis;
import io.quarkus.qute.deployment.TypeCheckExcludeBuildItem.TypeCheck;
import io.quarkus.qute.deployment.TypeInfos.Info;
import io.quarkus.qute.deployment.TypeInfos.TypeInfo;
import io.quarkus.qute.deployment.Types.AssignabilityCheck;
import io.quarkus.qute.generator.ExtensionMethodGenerator;
import io.quarkus.qute.generator.ExtensionMethodGenerator.NamespaceResolverCreator;
import io.quarkus.qute.generator.ExtensionMethodGenerator.NamespaceResolverCreator.ResolveCreator;
import io.quarkus.qute.generator.ExtensionMethodGenerator.Param;
import io.quarkus.qute.generator.TemplateGlobalGenerator;
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
import io.quarkus.qute.runtime.extensions.OrOperatorTemplateExtensions;
import io.quarkus.qute.runtime.extensions.StringTemplateExtensions;
import io.quarkus.qute.runtime.extensions.TimeTemplateExtensions;
import io.quarkus.runtime.util.StringUtil;

public class QuteProcessor {

    public static final DotName LOCATION = Names.LOCATION;

    private static final Logger LOGGER = Logger.getLogger(QuteProcessor.class);

    private static final String CHECKED_TEMPLATE_REQUIRE_TYPE_SAFE = "requireTypeSafeExpressions";
    private static final String CHECKED_TEMPLATE_BASE_PATH = "basePath";
    private static final String CHECKED_TEMPLATE_DEFAULT_NAME = "defaultName";
    private static final String IGNORE_FRAGMENTS = "ignoreFragments";
    private static final String BASE_PATH = "templates";

    private static final Set<String> ITERATION_METADATA_KEYS = Set.of("count", "index", "indexParity", "hasNext", "odd",
            "isOdd", "even", "isEven", "isLast", "isFirst");

    private static final Function<FieldInfo, String> GETTER_FUN = new Function<FieldInfo, String>() {
        @Override
        public String apply(FieldInfo field) {
            String prefix;
            if (field.type().kind() == org.jboss.jandex.Type.Kind.PRIMITIVE
                    && field.type().asPrimitiveType().primitive() == Primitive.BOOLEAN) {
                prefix = ValueResolverGenerator.IS_PREFIX;
            } else {
                prefix = ValueResolverGenerator.GET_PREFIX;
            }
            return prefix + ValueResolverGenerator.capitalize(field.name());
        }
    };

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.QUTE);
    }

    @BuildStep
    List<BeanDefiningAnnotationBuildItem> beanDefiningAnnotations() {
        return List.of(
                new BeanDefiningAnnotationBuildItem(Names.LOCATE, DotNames.SINGLETON),
                new BeanDefiningAnnotationBuildItem(Names.LOCATES, DotNames.SINGLETON),
                new BeanDefiningAnnotationBuildItem(Names.ENGINE_CONFIGURATION));
    }

    @BuildStep
    void processTemplateErrors(TemplatesAnalysisBuildItem analysis, List<IncorrectExpressionBuildItem> incorrectExpressions,
            BuildProducer<ServiceStartBuildItem> serviceStart) {

        List<TemplateException> errors = new ArrayList<>();

        for (IncorrectExpressionBuildItem incorrectExpression : incorrectExpressions) {
            if (incorrectExpression.reason != null) {
                errors.add(TemplateException.builder()
                        .code(Code.INCORRECT_EXPRESSION)
                        .origin(incorrectExpression.origin)
                        .message(
                                "{templatePath.or(origin.templateId)}:{origin.line}:{origin.lineCharacterStart} - \\{{expression}\\}: {reason}")
                        .argument("templatePath",
                                findTemplatePath(analysis, incorrectExpression.origin.getTemplateGeneratedId()))
                        .argument("expression", incorrectExpression.expression)
                        .argument("reason", incorrectExpression.reason)
                        .build());
            } else if (incorrectExpression.clazz != null) {
                errors.add(TemplateException.builder()
                        .code(Code.INCORRECT_EXPRESSION)
                        .origin(incorrectExpression.origin)
                        .message(
                                "{templatePath.or(origin.templateId)}:{origin.line}:{origin.lineCharacterStart} - \\{{expression}}: Property/method [{property}] not found on class [{clazz}] nor handled by an extension method")
                        .argument("templatePath",
                                findTemplatePath(analysis, incorrectExpression.origin.getTemplateGeneratedId()))
                        .argument("expression", incorrectExpression.expression)
                        .argument("property", incorrectExpression.property)
                        .argument("clazz", incorrectExpression.clazz)
                        .build());
            } else {
                errors.add(TemplateException.builder()
                        .code(Code.INCORRECT_EXPRESSION)
                        .origin(incorrectExpression.origin)
                        .message(
                                "{templatePath.or(origin.templateId)}:{origin.line}:{origin.lineCharacterStart} - \\{{expression}}: @Named bean not found for [{property}]")
                        .argument("templatePath",
                                findTemplatePath(analysis, incorrectExpression.origin.getTemplateGeneratedId()))
                        .argument("expression", incorrectExpression.expression)
                        .argument("property", incorrectExpression.property)
                        .build());
            }
        }

        if (!errors.isEmpty()) {
            StringBuilder message = new StringBuilder("Found incorrect expressions (").append(errors.size())
                    .append("):");
            int idx = 1;
            for (TemplateException error : errors) {
                message.append("\n\t").append("[").append(idx++).append("] ").append(error.getMessage());
            }
            message.append("\n");
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
                .addBeanClasses(EngineProducer.class, TemplateProducer.class, ContentTypes.class, Template.class,
                        TemplateInstance.class, CollectionTemplateExtensions.class,
                        MapTemplateExtensions.class, NumberTemplateExtensions.class, ConfigTemplateExtensions.class,
                        TimeTemplateExtensions.class, StringTemplateExtensions.class, OrOperatorTemplateExtensions.class)
                .build();
    }

    @BuildStep
    List<CheckedTemplateBuildItem> collectCheckedTemplates(BeanArchiveIndexBuildItem index,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            List<TemplatePathBuildItem> templatePaths,
            List<CheckedTemplateAdapterBuildItem> templateAdaptorBuildItems,
            TemplateFilePathsBuildItem filePaths,
            CustomTemplateLocatorPatternsBuildItem locatorPatternsBuildItem) {
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
            if (annotation.target().kind() != Kind.CLASS) {
                continue;
            }
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
                String fragmentId = getCheckedFragmentId(methodInfo, annotation);
                StringBuilder templatePathBuilder = new StringBuilder();
                AnnotationValue basePathValue = annotation.value(CHECKED_TEMPLATE_BASE_PATH);
                if (basePathValue != null && !basePathValue.asString().equals(CheckedTemplate.DEFAULTED)) {
                    templatePathBuilder.append(basePathValue.asString());
                } else if (classInfo.enclosingClass() != null) {
                    ClassInfo enclosingClass = index.getIndex().getClassByName(classInfo.enclosingClass());
                    templatePathBuilder.append(enclosingClass.simpleName());
                }
                if (templatePathBuilder.length() > 0 && templatePathBuilder.charAt(templatePathBuilder.length() - 1) != '/') {
                    templatePathBuilder.append('/');
                }
                String templatePath = templatePathBuilder
                        .append(getCheckedTemplateName(methodInfo, annotation, fragmentId != null)).toString();
                String fullPath = templatePath + (fragmentId != null ? "$" + fragmentId : "");
                MethodInfo checkedTemplateMethod = checkedTemplateMethods.putIfAbsent(fullPath, methodInfo);
                if (checkedTemplateMethod != null) {
                    throw new TemplateException(
                            String.format(
                                    "Multiple checked template methods exist for the template path %s:\n\t- %s: %s\n\t- %s: %s",
                                    fullPath, methodInfo.declaringClass().name(), methodInfo,
                                    checkedTemplateMethod.declaringClass().name(), checkedTemplateMethod));
                }
                if (!filePaths.contains(templatePath)
                        && isNotLocatedByCustomTemplateLocator(locatorPatternsBuildItem.getLocationPatterns(),
                                templatePath)) {
                    List<String> startsWith = new ArrayList<>();
                    for (String filePath : filePaths.getFilePaths()) {
                        if (filePath.startsWith(templatePath)
                                && filePath.charAt(templatePath.length()) == '.') {
                            startsWith.add(filePath);
                        }
                    }
                    if (startsWith.isEmpty()) {
                        throw new TemplateException(
                                "No template matching the path " + templatePath + " could be found for: "
                                        + classInfo.name() + "." + methodInfo.name());
                    } else {
                        throw new TemplateException(
                                startsWith + " match the path " + templatePath
                                        + " but the file suffix is not configured via the quarkus.qute.suffixes property");
                    }
                }

                Map<String, String> bindings = new HashMap<>();
                List<Type> parameters = methodInfo.parameterTypes();
                List<String> parameterNames = new ArrayList<>(parameters.size());
                for (int i = 0; i < parameters.size(); i++) {
                    Type type = parameters.get(i);
                    String name = methodInfo.parameterName(i);
                    if (name == null) {
                        throw new TemplateException("Parameter names not recorded for " + classInfo.name()
                                + ": compile the class with -parameters");
                    }
                    bindings.put(name, getCheckedTemplateParameterTypeName(type));
                    parameterNames.add(name);
                }
                AnnotationValue requireTypeSafeExpressions = annotation.value(CHECKED_TEMPLATE_REQUIRE_TYPE_SAFE);
                ret.add(new CheckedTemplateBuildItem(templatePath, fragmentId, bindings, methodInfo,
                        requireTypeSafeExpressions != null ? requireTypeSafeExpressions.asBoolean() : true));
                enhancer.implement(methodInfo, templatePath, fragmentId, parameterNames, adaptor);
            }
            transformers.produce(new BytecodeTransformerBuildItem(classInfo.name().toString(),
                    enhancer));
        }

        return ret;
    }

    private String getCheckedTemplateName(MethodInfo method, AnnotationInstance checkedTemplateAnnotation,
            boolean checkedFragment) {
        AnnotationValue nameValue = checkedTemplateAnnotation.value(CHECKED_TEMPLATE_DEFAULT_NAME);
        String defaultName;
        if (nameValue == null) {
            defaultName = CheckedTemplate.ELEMENT_NAME;
        } else {
            defaultName = nameValue.asString();
        }
        String methodName = method.name();
        if (checkedFragment) {
            // the name is the part before the last occurence of a dollar sign
            methodName = methodName.substring(0, methodName.lastIndexOf('$'));
        }
        return defaultedName(defaultName, methodName);
    }

    private String getCheckedFragmentId(MethodInfo method, AnnotationInstance checkedTemplateAnnotation) {
        AnnotationValue ignoreFragmentsValue = checkedTemplateAnnotation.value(IGNORE_FRAGMENTS);
        if (ignoreFragmentsValue != null && ignoreFragmentsValue.asBoolean()) {
            return null;
        }
        String methodName = method.name();
        // the id is the part after the last occurence of a dollar sign
        int idx = methodName.lastIndexOf('$');
        if (idx == -1 || idx == methodName.length()) {
            return null;
        }
        AnnotationValue nameValue = checkedTemplateAnnotation.value(CHECKED_TEMPLATE_DEFAULT_NAME);
        String defaultName;
        if (nameValue == null) {
            defaultName = CheckedTemplate.ELEMENT_NAME;
        } else {
            defaultName = nameValue.asString();
        }
        return defaultedName(defaultName, methodName.substring(idx + 1, methodName.length()));
    }

    private String defaultedName(String defaultNameStrategy, String value) {
        switch (defaultNameStrategy) {
            case CheckedTemplate.ELEMENT_NAME:
                return value;
            case CheckedTemplate.HYPHENATED_ELEMENT_NAME:
                return StringUtil.hyphenate(value);
            case CheckedTemplate.UNDERSCORED_ELEMENT_NAME:
                return String.join("_", new Iterable<String>() {
                    @Override
                    public Iterator<String> iterator() {
                        return StringUtil.lowerCase(StringUtil.camelHumpsIterator(value));
                    }
                });
            default:
                throw new IllegalArgumentException("Unsupported @CheckedTemplate#defaultName() value: " + defaultNameStrategy);
        }
    }

    private boolean isNotLocatedByCustomTemplateLocator(
            Collection<Pattern> locationPatterns, String templatePath) {
        if (!locationPatterns.isEmpty() && templatePath != null) {
            for (Pattern locationPattern : locationPatterns) {
                if (locationPattern.matcher(templatePath).matches()) {
                    return false;
                }
            }
        }
        return true;
    }

    @BuildStep
    TemplatesAnalysisBuildItem analyzeTemplates(List<TemplatePathBuildItem> templatePaths,
            TemplateFilePathsBuildItem filePaths, List<CheckedTemplateBuildItem> checkedTemplates,
            List<MessageBundleMethodBuildItem> messageBundleMethods, List<TemplateGlobalBuildItem> globals, QuteConfig config,
            Optional<EngineConfigurationsBuildItem> engineConfigurations,
            BeanArchiveIndexBuildItem beanArchiveIndex,
            BuildProducer<CheckedFragmentValidationBuildItem> checkedFragmentValidations) {
        long start = System.nanoTime();

        checkDuplicatePaths(templatePaths);

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
                    tagName = tagName.substring(0, tagName.indexOf('.'));
                }
                builder.addSectionHelper(new UserTagSectionHelper.Factory(tagName, tagPath));
            }
        }

        // Register additional section factories
        if (engineConfigurations.isPresent()) {
            Collection<ClassInfo> sectionFactories = engineConfigurations.get().getConfigurations().stream()
                    .filter(c -> Types.isImplementorOf(c, Names.SECTION_HELPER_FACTORY, beanArchiveIndex.getIndex()))
                    .collect(Collectors.toList());
            // Use the deployment class loader - it can load application classes; it's non-persistent and isolated
            ClassLoader tccl = Thread.currentThread().getContextClassLoader();
            for (ClassInfo factoryClass : sectionFactories) {
                try {
                    Class<?> sectionHelperFactoryClass = tccl.loadClass(factoryClass.toString());
                    SectionHelperFactory<?> factory = (SectionHelperFactory<?>) sectionHelperFactoryClass
                            .getDeclaredConstructor().newInstance();
                    builder.addSectionHelper(factory);
                    LOGGER.debugf("SectionHelperFactory registered during template analysis: " + factoryClass);
                } catch (Exception e) {
                    throw new IllegalStateException("Unable to instantiate SectionHelperFactory: " + factoryClass, e);
                }
            }
        }

        builder.computeSectionHelper(name -> {
            // Create a dummy section helper factory for an unknown section that could be potentially registered at runtime
            return new SectionHelperFactory<SectionHelper>() {
                @Override
                public SectionHelper initialize(SectionInitContext context) {
                    return new SectionHelper() {
                        @Override
                        public CompletionStage<ResultNode> resolve(SectionResolutionContext context) {
                            return ResultNode.NOOP;
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
                    return Optional.of(new TemplateLocation() {
                        @Override
                        public Reader read() {
                            return new StringReader(found.getContent());
                        }

                        @Override
                        public Optional<Variant> getVariant() {
                            return Optional.empty();
                        }
                    });
                }
                return Optional.empty();
            }
        });

        // It's a file-based template
        // We need to find out whether the parsed template represents a checked template
        Map<String, String> pathToPathWithoutSuffix = new HashMap<>();
        for (String path : filePaths.getFilePaths()) {
            for (String suffix : config.suffixes) {
                if (path.endsWith(suffix)) {
                    // Remove the suffix and add to Map
                    pathToPathWithoutSuffix.put(path, path.substring(0, path.length() - (suffix.length() + 1)));
                    break;
                }
            }

            // Path has already no suffix
            if (!pathToPathWithoutSuffix.containsKey(path)) {
                pathToPathWithoutSuffix.put(path, path);
            }
        }

        // Checked Template id -> method parameter declaration
        Map<String, Map<String, MethodParameterDeclaration>> checkedTemplateIdToParamDecl = new HashMap<>();
        for (CheckedTemplateBuildItem checkedTemplate : checkedTemplates) {
            if (checkedTemplate.isFragment()) {
                continue;
            }
            for (Entry<String, String> entry : checkedTemplate.bindings.entrySet()) {
                checkedTemplateIdToParamDecl
                        .computeIfAbsent(checkedTemplate.templateId, s -> new HashMap<>())
                        .put(entry.getKey(), new MethodParameterDeclaration(entry.getValue(), entry.getKey()));
            }
        }

        // Message Bundle Template id -> method parameter declaration
        Map<String, Map<String, MethodParameterDeclaration>> msgBundleTemplateIdToParamDecl = new HashMap<>();
        for (MessageBundleMethodBuildItem messageBundleMethod : messageBundleMethods) {
            MethodInfo method = messageBundleMethod.getMethod();
            for (ListIterator<Type> it = method.parameterTypes().listIterator(); it.hasNext();) {
                Type paramType = it.next();
                String name = MessageBundleProcessor.getParameterName(method, it.previousIndex());
                msgBundleTemplateIdToParamDecl
                        .computeIfAbsent(messageBundleMethod.getTemplateId(), s -> new HashMap<>())
                        .put(name, new MethodParameterDeclaration(getCheckedTemplateParameterTypeName(paramType), name));
            }
        }

        builder.addParserHook(new ParserHook() {

            @Override
            public void beforeParsing(ParserHelper parserHelper) {
                // The template id may be the full path, e.g. "items.html" or a path without the suffix, e.g. "items"
                String templateId = parserHelper.getTemplateId();

                if (filePaths.contains(templateId)) {
                    // Set the bindings for globals first so that type-safe templates can override them
                    for (TemplateGlobalBuildItem global : globals) {
                        parserHelper.addParameter(global.getName(),
                                getCheckedTemplateParameterTypeName(global.getVariableType()).toString());
                    }

                    addMethodParamsToParserHelper(parserHelper, pathToPathWithoutSuffix.get(templateId),
                            checkedTemplateIdToParamDecl);
                }

                addMethodParamsToParserHelper(parserHelper, templateId, msgBundleTemplateIdToParamDecl);
            }

        }).build();

        Engine dummyEngine = builder.build();
        List<CheckedTemplateBuildItem> checkedFragments = checkedTemplates.stream().filter(CheckedTemplateBuildItem::isFragment)
                .collect(Collectors.toList());

        for (TemplatePathBuildItem path : templatePaths) {
            Template template = dummyEngine.getTemplate(path.getPath());
            if (template != null) {
                String templateIdWithoutSuffix = pathToPathWithoutSuffix.get(template.getId());

                final List<ParameterDeclaration> parameterDeclarations;
                if (checkedTemplateIdToParamDecl.isEmpty()) {
                    parameterDeclarations = template.getParameterDeclarations();
                } else {
                    // Add method parameter declarations if they were not overridden in the template
                    parameterDeclarations = mergeParamDeclarations(
                            template.getParameterDeclarations(),
                            checkedTemplateIdToParamDecl.get(templateIdWithoutSuffix));
                }

                if (!checkedFragments.isEmpty()) {
                    for (CheckedTemplateBuildItem checkedFragment : checkedFragments) {
                        if (checkedFragment.templateId.equals(templateIdWithoutSuffix)) {
                            // Template matches a type-safe fragment
                            Template.Fragment fragment = template.getFragment(checkedFragment.fragmentId);
                            if (fragment == null) {
                                throw new TemplateException(
                                        "Fragment [" + checkedFragment.fragmentId + "] not defined in template "
                                                + template.getId());
                            }
                            checkedFragmentValidations
                                    .produce(new CheckedFragmentValidationBuildItem(template.getGeneratedId(),
                                            checkedFragment.templateId,
                                            checkedFragment.fragmentId, fragment.getExpressions(), checkedFragment.method));
                        }
                    }
                }

                analysis.add(new TemplateAnalysis(null, template.getGeneratedId(), template.getExpressions(),
                        parameterDeclarations, path.getPath(), template.getFragmentIds()));
            }
        }

        // Message bundle templates
        for (MessageBundleMethodBuildItem messageBundleMethod : messageBundleMethods) {
            Template template = dummyEngine.parse(messageBundleMethod.getTemplate(), null, messageBundleMethod.getTemplateId());

            // Add method parameter declarations if they were not overridden in the template
            List<ParameterDeclaration> paramDeclarations = mergeParamDeclarations(
                    template.getParameterDeclarations(),
                    msgBundleTemplateIdToParamDecl.get(messageBundleMethod.getTemplateId()));

            analysis.add(new TemplateAnalysis(messageBundleMethod.getTemplateId(), template.getGeneratedId(),
                    template.getExpressions(), paramDeclarations,
                    messageBundleMethod.getMethod().declaringClass().name() + "#" + messageBundleMethod.getMethod().name()
                            + "()",
                    template.getFragmentIds()));
        }

        LOGGER.debugf("Finished analysis of %s templates in %s ms", analysis.size(),
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
        return new TemplatesAnalysisBuildItem(analysis);
    }

    @BuildStep
    void validateCheckedFragments(List<CheckedFragmentValidationBuildItem> validations,
            List<TemplateExpressionMatchesBuildItem> expressionMatches,
            List<TemplateGlobalBuildItem> templateGlobals,
            BeanArchiveIndexBuildItem beanArchiveIndex,
            BuildProducer<ValidationErrorBuildItem> validationErrors) {

        if (validations.isEmpty()) {
            return;
        }
        IndexView index = beanArchiveIndex.getIndex();
        AssignabilityCheck assignabilityCheck = new AssignabilityCheck(index);
        String[] hintPrefixes = { LoopSectionHelper.Factory.HINT_PREFIX, WhenSectionHelper.Factory.HINT_PREFIX,
                SetSectionHelper.Factory.HINT_PREFIX };
        Set<String> globals = templateGlobals.stream().map(TemplateGlobalBuildItem::getName)
                .collect(Collectors.toUnmodifiableSet());

        for (CheckedFragmentValidationBuildItem validation : validations) {
            Map<String, Type> paramNamesToTypes = new HashMap<>();
            TemplateExpressionMatchesBuildItem matchResults = null;
            for (TemplateExpressionMatchesBuildItem m : expressionMatches) {
                if (m.templateGeneratedId.equals(validation.templateGeneratedId)) {
                    matchResults = m;
                }
            }
            if (matchResults == null) {
                throw new IllegalStateException(
                        "Match results not found for: " + validation.templateId);
            }

            for (Expression expression : validation.fragmentExpressions) {
                // Note that we ignore:
                // - literals,
                // - globals,
                // - expressions with no type info,
                // - loop metadata; e.g. |java.lang.Integer|<loop-metadata>
                // - expressions with a hint referencing an expression from inside the fragment
                if (expression.isLiteral() || globals.contains(expression.getParts().get(0).getName())) {
                    continue;
                }
                String typeInfo = expression.getParts().get(0).getTypeInfo();
                if (typeInfo == null || (typeInfo != null && typeInfo.endsWith(LoopSectionHelper.Factory.HINT_METADATA))) {
                    continue;
                }
                Info info = TypeInfos.create(expression, index, null).get(0);
                if (info.isTypeInfo()) {
                    // |org.acme.Foo|.name
                    paramNamesToTypes.put(expression.getParts().get(0).getName(), info.asTypeInfo().resolvedType);
                } else if (info.hasHints()) {
                    // foo<set#123>.name
                    hintLoop: for (String helperHint : info.asHintInfo().hints) {
                        for (String prefix : hintPrefixes) {
                            if (helperHint.startsWith(prefix)) {
                                int generatedId = parseHintId(helperHint, prefix);
                                Expression localExpression = findExpression(generatedId, validation.fragmentExpressions);
                                if (localExpression == null) {
                                    MatchResult match = matchResults.getMatch(generatedId);
                                    if (match == null) {
                                        throw new IllegalStateException(
                                                "Match result not found for expression [" + expression.toOriginalString()
                                                        + "] in: "
                                                        + validation.templateId);
                                    }
                                    paramNamesToTypes.put(expression.getParts().get(0).getName(), match.type);
                                    break hintLoop;
                                }
                            }
                        }
                    }
                }
            }
            if (!paramNamesToTypes.isEmpty()) {
                for (Entry<String, Type> e : paramNamesToTypes.entrySet()) {
                    String paramName = e.getKey();
                    MethodParameterInfo param = validation.method.parameters().stream()
                            .filter(mp -> mp.name().equals(paramName)).findFirst().orElse(null);
                    if (param == null || !assignabilityCheck.isAssignableFrom(e.getValue(), param.type())) {
                        throw new TemplateException(
                                validation.method.declaringClass().name().withoutPackagePrefix() + "#"
                                        + validation.method.name() + "() must declare a parameter of name [" + paramName
                                        + "] and type [" + e.getValue() + "]");
                    }
                }
            }
        }
    }

    @SuppressWarnings("incomplete-switch")
    private static String getCheckedTemplateParameterTypeName(Type type) {
        switch (type.kind()) {
            case PARAMETERIZED_TYPE:
                return getCheckedTemplateParameterParameterizedTypeName((ParameterizedType) type);
            case ARRAY:
                // in the case of an array, we get back to using Type#toString()
                // otherwise, we end up with java.lang.[I] for int[]
                return type.toString();
        }
        return type.name().toString();
    }

    private static String getCheckedTemplateParameterParameterizedTypeName(ParameterizedType parameterizedType) {
        StringBuilder builder = new StringBuilder();

        if (parameterizedType.owner() != null) {
            builder.append(parameterizedType.owner().name());
            builder.append('.');
            builder.append(parameterizedType.name().local());
        } else {
            builder.append(parameterizedType.name());
        }

        List<Type> arguments = parameterizedType.arguments();
        if (arguments.size() > 0) {
            builder.append('<');
            builder.append(getCheckedTemplateParameterTypeName(arguments.get(0)));
            for (int i = 1; i < arguments.size(); i++) {
                builder.append(", ").append(getCheckedTemplateParameterTypeName(arguments.get(i)));
            }
            builder.append('>');
        }

        return builder.toString();
    }

    private List<ParameterDeclaration> mergeParamDeclarations(List<ParameterDeclaration> parameterDeclarations,
            Map<String, MethodParameterDeclaration> paramNameToDeclaration) {
        if (paramNameToDeclaration != null) {
            Map<String, ParameterDeclaration> mergeResult = new HashMap<>(paramNameToDeclaration);
            for (ParameterDeclaration paramDeclaration : parameterDeclarations) {
                // Template parameter declarations override method parameter declarations
                mergeResult.put(paramDeclaration.getKey(), paramDeclaration);
            }
            return List.copyOf(mergeResult.values());
        }
        return parameterDeclarations;
    }

    private void addMethodParamsToParserHelper(ParserHelper parserHelper, String templateId,
            Map<String, Map<String, MethodParameterDeclaration>> templateIdToParamDecl) {
        var paramNameToDeclaration = templateIdToParamDecl.get(templateId);
        if (paramNameToDeclaration != null) {
            for (MethodParameterDeclaration parameterDeclaration : paramNameToDeclaration.values()) {
                parserHelper.addParameter(parameterDeclaration.getKey(), parameterDeclaration.getParamType());
            }
        }
    }

    @BuildStep
    void validateExpressions(TemplatesAnalysisBuildItem templatesAnalysis,
            BeanArchiveIndexBuildItem beanArchiveIndex,
            List<TemplateExtensionMethodBuildItem> templateExtensionMethods,
            List<TypeCheckExcludeBuildItem> typeCheckExcludeBuildItems,
            BuildProducer<IncorrectExpressionBuildItem> incorrectExpressions,
            BuildProducer<ImplicitValueResolverBuildItem> implicitClasses,
            BuildProducer<TemplateExpressionMatchesBuildItem> expressionMatches,
            BeanDiscoveryFinishedBuildItem beanDiscovery,
            List<CheckedTemplateBuildItem> checkedTemplates,
            List<TemplateDataBuildItem> templateData,
            QuteConfig config,
            PackageConfig packageConfig) {

        long start = System.nanoTime();

        // ===================================================
        // Initialize shared structures needed for validation
        // ===================================================
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
        Map<String, BeanInfo> namedBeans = beanDiscovery.beanStream().withName()
                .collect(toMap(BeanInfo::getName, Function.identity()));
        // Map implicit class -> set of used members
        Map<DotName, Set<String>> implicitClassToMembersUsed = new HashMap<>();

        Map<String, TemplateDataBuildItem> namespaceTemplateData = templateData.stream()
                .filter(TemplateDataBuildItem::hasNamespace)
                .collect(Collectors.toMap(TemplateDataBuildItem::getNamespace, Function.identity()));

        Map<String, List<TemplateExtensionMethodBuildItem>> namespaceExtensionMethods = templateExtensionMethods.stream()
                .filter(TemplateExtensionMethodBuildItem::hasNamespace)
                .sorted(Comparator.comparingInt(TemplateExtensionMethodBuildItem::getPriority).reversed())
                .collect(Collectors.groupingBy(TemplateExtensionMethodBuildItem::getNamespace));

        List<TemplateExtensionMethodBuildItem> regularExtensionMethods = templateExtensionMethods.stream()
                .filter(Predicate.not(TemplateExtensionMethodBuildItem::hasNamespace)).collect(Collectors.toList());

        JavaMemberLookupConfig lookupConfig = new FixedJavaMemberLookupConfig(index, initDefaultMembersFilter(), false);
        AssignabilityCheck assignabilityCheck = new AssignabilityCheck(index);
        int expressionsValidated = 0;

        final List<Predicate<TypeCheck>> excludes = new ArrayList<>();
        // subset of excludes specific for extension methods
        final List<Predicate<TypeCheck>> extensionMethodExcludes = new ArrayList<>();
        for (TypeCheckExcludeBuildItem exclude : typeCheckExcludeBuildItems) {
            excludes.add(exclude.getPredicate());
            if (exclude.isExtensionMethodPredicate()) {
                extensionMethodExcludes.add(exclude.getPredicate());
            }
        }

        // =============================================
        // Perform validation for each template found
        // =============================================
        for (TemplateAnalysis templateAnalysis : templatesAnalysis.getAnalysis()) {
            // Find the relevant checked template, may be null
            CheckedTemplateBuildItem checkedTemplate = findCheckedTemplate(config, templateAnalysis, checkedTemplates);
            // Maps an expression generated id to the last match of an expression (i.e. the type of the last part)
            Map<Integer, MatchResult> generatedIdsToMatches = new HashMap<>();

            // Register all param declarations as targets of implicit value resolvers
            for (ParameterDeclaration paramDeclaration : templateAnalysis.parameterDeclarations) {
                Type type = TypeInfos.resolveTypeFromTypeInfo(paramDeclaration.getTypeInfo());
                if (type != null) {
                    implicitClassToMembersUsed.put(type.name(), new HashSet<>());
                }
            }

            // Iterate over all top-level expressions found in the template
            for (Expression expression : templateAnalysis.expressions) {
                if (expression.isLiteral()) {
                    continue;
                }
                MatchResult match = validateNestedExpressions(config, templateAnalysis, null, new HashMap<>(), excludes,
                        incorrectExpressions, expression, index, implicitClassToMembersUsed, templateIdToPathFun,
                        generatedIdsToMatches, extensionMethodExcludes,
                        checkedTemplate, lookupConfig, namedBeans, namespaceTemplateData, regularExtensionMethods,
                        namespaceExtensionMethods, assignabilityCheck);
                generatedIdsToMatches.put(expression.getGeneratedId(), match);
            }

            // Validate default values of parameter declarations
            validateDefaultValuesOfParameterDeclarations(templateAnalysis, index, assignabilityCheck,
                    generatedIdsToMatches, templateIdToPathFun, incorrectExpressions);

            expressionMatches
                    .produce(new TemplateExpressionMatchesBuildItem(templateAnalysis.generatedId, generatedIdsToMatches));

            expressionsValidated += generatedIdsToMatches.size();
        }

        LOGGER.debugf("Validated %s expressions in %s ms", expressionsValidated,
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));

        // ==========================================================================
        // Register implicit value resolvers for classes collected during validation
        // ==========================================================================
        boolean isNonNativeBuild = !packageConfig.isNativeOrNativeSources();
        for (Entry<DotName, Set<String>> entry : implicitClassToMembersUsed.entrySet()) {
            if (entry.getValue().isEmpty() && isNonNativeBuild) {
                // No members used - skip the generation for non-native builds
                continue;
            }
            ClassInfo clazz = index.getClassByName(entry.getKey());
            if (clazz != null) {
                TemplateDataBuilder builder = new TemplateDataBuilder();
                if (isNonNativeBuild) {
                    // Optimize the generated value resolvers
                    // I.e. only fields/methods used in templates are considered and all other members are ignored
                    builder.addIgnore(buildIgnorePattern(entry.getValue()));
                }
                implicitClasses.produce(new ImplicitValueResolverBuildItem(clazz, builder.build()));
            }
        }
    }

    private static void validateDefaultValuesOfParameterDeclarations(TemplateAnalysis templateAnalysis, IndexView index,
            AssignabilityCheck assignabilityCheck,
            Map<Integer, MatchResult> generatedIdsToMatches, Function<String, String> templateIdToPathFun,
            BuildProducer<IncorrectExpressionBuildItem> incorrectExpressions) {
        for (ParameterDeclaration parameterDeclaration : templateAnalysis.parameterDeclarations) {
            Expression defaultValue = parameterDeclaration.getDefaultValue();
            if (defaultValue != null) {
                MatchResult match;
                if (defaultValue.isLiteral()) {
                    match = new MatchResult(assignabilityCheck);
                    setMatchValues(match, defaultValue, generatedIdsToMatches, index);
                } else {
                    match = generatedIdsToMatches.get(defaultValue.getGeneratedId());
                    if (match == null) {
                        LOGGER.debugf(
                                "No type info available - unable to validate the default value of a parameter declaration ["
                                        + parameterDeclaration.getKey() + "] in " + defaultValue.getOrigin());
                        continue;
                    }
                }
                Info info = TypeInfos.create(parameterDeclaration.getTypeInfo(), null, index, templateIdToPathFun,
                        parameterDeclaration.getDefaultValue().getOrigin());
                if (!info.isTypeInfo()) {
                    throw new IllegalStateException("Invalid type info [" + info + "] of parameter declaration ["
                            + parameterDeclaration.getKey() + "] in "
                            + defaultValue.getOrigin().toString());
                }
                if (!assignabilityCheck.isAssignableFrom(info.asTypeInfo().resolvedType, match.type())) {
                    incorrectExpressions.produce(new IncorrectExpressionBuildItem(defaultValue.toOriginalString(),
                            "The type of the default value [" + match.type()
                                    + "] does not match the type of the parameter declaration ["
                                    + info.asTypeInfo().resolvedType + "]",
                            defaultValue.getOrigin()));
                }
            }
        }
    }

    static Predicate<AnnotationTarget> initDefaultMembersFilter() {
        // By default, synthetic, non-public and static members (excl. enum constants) are ignored
        Predicate<AnnotationTarget> filter = QuteProcessor::defaultFilter;
        Predicate<AnnotationTarget> enumConstantFilter = QuteProcessor::enumConstantFilter;
        filter = filter.and(enumConstantFilter.or(not(QuteProcessor::staticsFilter)));
        return filter;
    }

    private CheckedTemplateBuildItem findCheckedTemplate(QuteConfig config, TemplateAnalysis analysis,
            List<CheckedTemplateBuildItem> checkedTemplates) {
        // Try to find the checked template
        String path = analysis.path;
        for (String suffix : config.suffixes) {
            if (path.endsWith(suffix)) {
                path = path.substring(0, path.length() - (suffix.length() + 1));
                break;
            }
        }
        for (CheckedTemplateBuildItem item : checkedTemplates) {
            if (item.isFragment()) {
                continue;
            }
            if (item.templateId.equals(path)) {
                return item;
            }
        }
        return null;
    }

    static String buildIgnorePattern(Iterable<String> names) {
        // ^(?!\\Qbar\\P|\\Qfoo\\P).*$
        StringBuilder pattern = new StringBuilder("^(?!");
        Iterator<String> it = names.iterator();
        if (!it.hasNext()) {
            throw new IllegalArgumentException();
        }
        while (it.hasNext()) {
            String name = (String) it.next();
            pattern.append(Pattern.quote(name));
            if (it.hasNext()) {
                pattern.append("|");
            }
        }
        pattern.append(").*$");
        return pattern.toString();
    }

    static MatchResult validateNestedExpressions(QuteConfig config, TemplateAnalysis templateAnalysis, ClassInfo rootClazz,
            Map<String, MatchResult> results,
            Iterable<Predicate<TypeCheck>> excludes, BuildProducer<IncorrectExpressionBuildItem> incorrectExpressions,
            Expression expression, IndexView index,
            Map<DotName, Set<String>> implicitClassToMembersUsed, Function<String, String> templateIdToPathFun,
            Map<Integer, MatchResult> generatedIdsToMatches, Iterable<Predicate<TypeCheck>> extensionMethodExcludes,
            CheckedTemplateBuildItem checkedTemplate, JavaMemberLookupConfig lookupConfig, Map<String, BeanInfo> namedBeans,
            Map<String, TemplateDataBuildItem> namespaceTemplateData,
            List<TemplateExtensionMethodBuildItem> regularExtensionMethods,
            Map<String, List<TemplateExtensionMethodBuildItem>> namespaceToExtensionMethods,
            AssignabilityCheck assignabilityCheck) {

        LOGGER.debugf("Validate %s from %s", expression, expression.getOrigin());

        // ==============================================
        // Validate parameters of nested virtual methods
        // ==============================================
        validateParametersOfNestedVirtualMethods(config, templateAnalysis, results, excludes, incorrectExpressions, expression,
                index, implicitClassToMembersUsed, templateIdToPathFun, generatedIdsToMatches, extensionMethodExcludes,
                checkedTemplate, lookupConfig, namedBeans, namespaceTemplateData, regularExtensionMethods,
                namespaceToExtensionMethods, assignabilityCheck);

        MatchResult match = new MatchResult(assignabilityCheck);

        // ======================
        // Process the namespace
        // ======================
        NamespaceResult namespaceResult = processNamespace(expression, match, index, incorrectExpressions, namedBeans, results,
                templateAnalysis, namespaceTemplateData, lookupConfig, namespaceToExtensionMethods, templateIdToPathFun);
        if (namespaceResult.ignoring) {
            return match;
        }
        if (namespaceResult.hasRootClazz()) {
            rootClazz = namespaceResult.rootClazz;
        }
        if (namespaceResult.hasLookupConfig()) {
            lookupConfig = namespaceResult.lookupConfig;
        }

        // =====================================
        // Validate checked template expression
        // =====================================
        if (isInvalidCheckedTemplateExpression(config, checkedTemplate, expression, match, results,
                namespaceResult.dataNamespaceExpTypeInfo,
                incorrectExpressions)) {
            return match;
        }

        // ==========================================
        // Skip validation if no type info available
        // ==========================================
        if (rootClazz == null && !expression.hasTypeInfo() && !namespaceResult.hasDataNamespaceInfo()) {
            return putResult(match, results, expression);
        }

        // Parse the type info
        List<Info> parts = TypeInfos.create(expression, index, templateIdToPathFun);

        Iterator<Info> iterator = parts.iterator();
        Info root = iterator.next();

        // ======================
        // Process the root part
        // ======================
        RootResult rootResult = processRoot(expression, match, root, iterator, templateAnalysis, index, incorrectExpressions,
                rootClazz, parts, results, generatedIdsToMatches, templateIdToPathFun, assignabilityCheck, namespaceResult);
        if (rootResult.ignoring) {
            return match;
        }
        // Reset the iterator if necessary
        iterator = rootResult.iterator;

        // Iterate over all parts of the expression and check each part against the current match type
        while (iterator.hasNext()) {
            Info info = iterator.next();
            if (!match.isEmpty()) {
                // Arrays are handled specifically
                // We use the built-in resolver at runtime because the extension methods cannot be used to cover all combinations of dimensions and component types
                if (match.isArray() && processArray(info, match)) {
                    continue;
                }

                AnnotationTarget member = null;
                TemplateExtensionMethodBuildItem extensionMethod = null;
                Type type = null;

                if (!match.isPrimitive()) {
                    // Try to find a java member
                    Set<String> membersUsed = implicitClassToMembersUsed.get(match.type().name());
                    if (membersUsed == null) {
                        membersUsed = new HashSet<>();
                        implicitClassToMembersUsed.put(match.type().name(), membersUsed);
                    }
                    if (match.clazz() != null) {
                        if (info.isVirtualMethod()) {
                            member = findMethod(info.part.asVirtualMethod(), match.clazz(), expression, index,
                                    templateIdToPathFun, results, lookupConfig, assignabilityCheck);
                            if (member != null) {
                                membersUsed.add(member.asMethod().name());
                            }
                        } else if (info.isProperty()) {
                            member = findProperty(info.asProperty().name, match.clazz(), lookupConfig);
                            if (member != null) {
                                membersUsed
                                        .add(member.kind() == Kind.FIELD ? member.asField().name() : member.asMethod().name());
                            }
                        }
                    }
                }

                if (member == null) {
                    // Try to find an extension method
                    extensionMethod = findTemplateExtensionMethod(info, match.type(), regularExtensionMethods, expression,
                            index, templateIdToPathFun, results, assignabilityCheck);
                    if (extensionMethod != null) {
                        type = resolveType(extensionMethod.getMethod(), match, index, extensionMethod, results, info);
                        // Test whether the validation of extension method should be skipped
                        if (skipValidation(extensionMethodExcludes, expression, match, info, type)) {
                            break;
                        }
                        member = extensionMethod.getMethod();
                    }
                }

                // Test whether the validation should be skipped
                if (member == null && skipValidation(excludes, expression, match, info, match.type())) {
                    break;
                }

                if (member == null) {
                    // No member found - incorrect expression
                    incorrectExpressions.produce(new IncorrectExpressionBuildItem(expression.toOriginalString(),
                            info.value, match.type().toString(), expression.getOrigin()));
                    match.clearValues();
                    break;
                } else {
                    if (type == null) {
                        type = resolveType(member, match, index, extensionMethod, results, info);
                    }
                    ClassInfo clazz = null;
                    if (type.kind() == Type.Kind.CLASS || type.kind() == Type.Kind.PARAMETERIZED_TYPE) {
                        clazz = index.getClassByName(type.name());
                    }
                    match.setValues(clazz, type);
                    if (info.hasHints()) {
                        // For example a loop section needs to validate the type of an element
                        processHints(templateAnalysis, info.asHintInfo().hints, match, index, expression, generatedIdsToMatches,
                                incorrectExpressions);
                    }
                }
            } else {
                LOGGER.debugf(
                        "No match class available - skip further validation for [%s] in expression [%s] in template [%s] on line %s",
                        info.part, expression.toOriginalString(), expression.getOrigin().getTemplateId(),
                        expression.getOrigin().getLine());
                match.clearValues();
                break;
            }
            lookupConfig.nextPart();
        }
        return putResult(match, results, expression);
    }

    private static final class RootResult {

        private final Iterator<Info> iterator;
        private final boolean ignoring;

        public RootResult(Iterator<Info> iterator, boolean ignoring) {
            this.iterator = iterator;
            this.ignoring = ignoring;
        }

    }

    private static RootResult processRoot(Expression expression, MatchResult match, Info root, Iterator<Info> it,
            TemplateAnalysis templateAnalysis, IndexView index,
            BuildProducer<IncorrectExpressionBuildItem> incorrectExpressions,
            ClassInfo rootClazz,
            List<Info> parts,
            Map<String, MatchResult> results,
            Map<Integer, MatchResult> generatedIdsToMatches,
            Function<String, String> templateIdToPathFun,
            AssignabilityCheck assignabilityCheck,
            NamespaceResult namespace) {
        Iterator<Info> iterator = it;
        boolean ignoring = false;

        if (namespace.hasExtensionMethods()) {
            // Namespace is used and at least one namespace extension method exists for the given namespace
            TemplateExtensionMethodBuildItem extensionMethod = findTemplateExtensionMethod(root, null,
                    namespace.extensionMethods,
                    expression, index, templateIdToPathFun, results, assignabilityCheck);
            if (extensionMethod != null) {
                MethodInfo method = extensionMethod.getMethod();
                ClassInfo returnType = index.getClassByName(method.returnType().name());
                if (returnType != null) {
                    match.setValues(returnType, method.returnType());
                    iterator = processHintsIfNeeded(root, iterator, parts, templateAnalysis, root.asHintInfo().hints, match,
                            index, expression, generatedIdsToMatches, incorrectExpressions);
                } else {
                    // Return type not available
                    putResult(match, results, expression);
                    ignoring = true;
                }
            } else {
                // No namespace extension method found - incorrect expression
                incorrectExpressions.produce(new IncorrectExpressionBuildItem(expression.toOriginalString(),
                        String.format("No matching namespace [%s] extension method found", namespace.namespace),
                        expression.getOrigin()));
                match.clearValues();
                putResult(match, results, expression);
                ignoring = true;
            }

        } else if (namespace.hasDataNamespaceInfo()) {
            // Validate as Data namespace expression has parameter declaration bound to the variable
            // Skip the first part, e.g. for {data:item.name} we start validation with "name"
            match.setValues(namespace.dataNamespaceExpTypeInfo.rawClass, namespace.dataNamespaceExpTypeInfo.resolvedType);
        } else if (rootClazz == null) {
            // No namespace is used or no declarative resolver (extension methods, @TemplateData, etc.)
            if (root.isTypeInfo()) {
                // E.g. |org.acme.Item|
                match.setValues(root.asTypeInfo().rawClass, root.asTypeInfo().resolvedType);
                processHintsIfNeeded(root, iterator, parts, templateAnalysis, root.asHintInfo().hints, match, index, expression,
                        generatedIdsToMatches, incorrectExpressions);
            } else {
                if (root.hasHints()) {
                    iterator = processHintsIfNeeded(root, iterator, parts, templateAnalysis, root.asHintInfo().hints, match,
                            index, expression, generatedIdsToMatches, incorrectExpressions);
                } else {
                    // No type info available
                    putResult(match, results, expression);
                    ignoring = true;
                }
            }
        } else {
            if (namespace.isIn(INJECT_NAMESPACE, CDI_NAMESPACE)) {
                iterator = processHintsIfNeeded(root, iterator, parts, templateAnalysis, root.asHintInfo().hints, match, index,
                        expression, generatedIdsToMatches, incorrectExpressions);
            } else if (namespace.templateData != null) {
                // Set the root type and reset the iterator
                match.setValues(rootClazz, Type.create(rootClazz.name(), org.jboss.jandex.Type.Kind.CLASS));
                iterator = parts.iterator();
            } else {
                putResult(match, results, expression);
                ignoring = true;
            }
        }

        return new RootResult(iterator, ignoring);
    }

    private static boolean processArray(Info info, MatchResult match) {
        if (info.isProperty()) {
            String name = info.asProperty().name;
            if (name.equals("length") || name.equals("size")) {
                // myArray.length
                match.setValues(null, PrimitiveType.INT);
                return true;
            } else {
                // myArray[0], myArray.1
                try {
                    Integer.parseInt(name);
                    match.setValues(null, match.type().asArrayType().constituent());
                    return true;
                } catch (NumberFormatException e) {
                    // not an integer index
                }
            }
        } else if (info.isVirtualMethod()) {
            List<Expression> params = info.asVirtualMethod().part.asVirtualMethod().getParameters();
            String name = info.asVirtualMethod().name;
            if (name.equals("get") && params.size() == 1) {
                // array.get(84)
                Expression param = params.get(0);
                Object literalValue = param.getLiteral();
                if (literalValue == null || literalValue instanceof Integer) {
                    match.setValues(null, match.type().asArrayType().constituent());
                    return true;
                }
            } else if (name.equals("take") || name.equals("takeLast")) {
                // The returned array has the same component type
                return true;
            }
        }
        return false;
    }

    private static NamespaceResult processNamespace(Expression expression, MatchResult match, IndexView index,
            BuildProducer<IncorrectExpressionBuildItem> incorrectExpressions, Map<String, BeanInfo> namedBeans,
            Map<String, MatchResult> results, TemplateAnalysis templateAnalysis,
            Map<String, TemplateDataBuildItem> namespaceTemplateData, JavaMemberLookupConfig lookupConfig,
            Map<String, List<TemplateExtensionMethodBuildItem>> namespaceToExtensionMethods,
            Function<String, String> templateIdToPathFun) {
        String namespace = expression.getNamespace();
        if (namespace == null) {
            return NamespaceResult.EMPTY;
        }
        ClassInfo rootClazz = null;
        TypeInfos.TypeInfo dataNamespaceTypeInfo = null;
        TemplateDataBuildItem templateData = null;
        List<TemplateExtensionMethodBuildItem> namespaceExtensionMethods = null;
        boolean ignored = false;

        if (namespace.equals(INJECT_NAMESPACE) || namespace.equals(CDI_NAMESPACE)) {
            // cdi:, inject:
            BeanInfo bean = findBean(expression, index, incorrectExpressions, namedBeans);
            if (bean != null) {
                rootClazz = bean.getImplClazz();
                // Skip the first part - the name of the bean, e.g. for {inject:foo.name} we start validation with "name"
                match.setValues(rootClazz, bean.getProviderType());
            } else {
                // Bean not found
                putResult(match, results, expression);
                ignored = true;
            }
        } else if (isDataNamespace(namespace)) {
            // data:
            Expression.Part firstPart = expression.getParts().get(0);
            String firstPartName = firstPart.getName();
            for (ParameterDeclaration paramDeclaration : templateAnalysis.parameterDeclarations) {
                if (paramDeclaration.getKey().equals(firstPartName)) {
                    // Data Namespace expression has bounded parameter declaration
                    dataNamespaceTypeInfo = TypeInfos
                            .create(paramDeclaration.getTypeInfo(), firstPart, index, templateIdToPathFun,
                                    expression.getOrigin())
                            .asTypeInfo();
                    break;
                }
            }
            if (dataNamespaceTypeInfo == null) {
                putResult(match, results, expression);
                ignored = true;
            }
        } else {
            templateData = namespaceTemplateData.get(namespace);
            if (templateData != null) {
                // @TemplateData with namespace defined
                rootClazz = templateData.getTargetClass();
                // Only include the static members that are not ignored
                Predicate<AnnotationTarget> filter = QuteProcessor::defaultFilter;
                filter = filter.and(QuteProcessor::staticsFilter);
                filter = filter.and(templateData::filter);
                lookupConfig = new FirstPassJavaMemberLookupConfig(lookupConfig, filter, true);
            } else {
                // Extension methods exist for the given namespace
                namespaceExtensionMethods = namespaceToExtensionMethods.get(namespace);
                if (namespaceExtensionMethods == null) {
                    // All other namespaces are ignored
                    putResult(match, results, expression);
                    ignored = true;
                }
            }
        }
        return new NamespaceResult(namespace, rootClazz, dataNamespaceTypeInfo, templateData, namespaceExtensionMethods,
                ignored, lookupConfig);
    }

    private static class NamespaceResult {

        static final NamespaceResult EMPTY = new NamespaceResult(null, null, null, null, null, false, null);

        private final String namespace;
        private final ClassInfo rootClazz;
        private final TypeInfos.TypeInfo dataNamespaceExpTypeInfo;
        private final TemplateDataBuildItem templateData;
        private final List<TemplateExtensionMethodBuildItem> extensionMethods;
        private final boolean ignoring;
        private final JavaMemberLookupConfig lookupConfig;

        NamespaceResult(String namespace, ClassInfo rootClazz, TypeInfo dataNamespaceExpTypeInfo,
                TemplateDataBuildItem templateData,
                List<TemplateExtensionMethodBuildItem> namespaceExtensionMethods, boolean ignoring,
                JavaMemberLookupConfig lookupConfig) {
            this.namespace = namespace;
            this.rootClazz = rootClazz;
            this.dataNamespaceExpTypeInfo = dataNamespaceExpTypeInfo;
            this.templateData = templateData;
            this.extensionMethods = namespaceExtensionMethods;
            this.ignoring = ignoring;
            this.lookupConfig = lookupConfig;
        }

        boolean hasExtensionMethods() {
            return extensionMethods != null;
        }

        boolean hasDataNamespaceInfo() {
            return dataNamespaceExpTypeInfo != null;
        }

        boolean hasRootClazz() {
            return rootClazz != null;
        }

        boolean hasLookupConfig() {
            return lookupConfig != null;
        }

        boolean isIn(String... values) {
            for (String value : values) {
                if (value.equals(namespace)) {
                    return true;
                }
            }
            return false;
        }

    }

    private static boolean isInvalidCheckedTemplateExpression(QuteConfig config, CheckedTemplateBuildItem checkedTemplate,
            Expression expression, MatchResult match, Map<String, MatchResult> results,
            TypeInfos.TypeInfo dataNamespaceExpTypeInfo,
            BuildProducer<IncorrectExpressionBuildItem> incorrectExpressions) {
        if (checkedTemplate != null && checkedTemplate.requireTypeSafeExpressions && !expression.hasTypeInfo()
                && dataNamespaceExpTypeInfo == null) {
            if (!expression.hasNamespace() && expression.getParts().size() == 1
                    && ITERATION_METADATA_KEYS.contains(expression.getParts().get(0).getName())) {
                String prefixInfo;
                if (config.iterationMetadataPrefix
                        .equals(LoopSectionHelper.Factory.ITERATION_METADATA_PREFIX_ALIAS_UNDERSCORE)) {
                    prefixInfo = String.format(
                            "based on the iteration alias, i.e. the correct key should be something like {it_%1$s} or {element_%1$s}",
                            expression.getParts().get(0).getName());
                } else if (config.iterationMetadataPrefix
                        .equals(LoopSectionHelper.Factory.ITERATION_METADATA_PREFIX_ALIAS_QM)) {
                    prefixInfo = String.format(
                            "based on the iteration alias, i.e. the correct key should be something like {it?%1$s} or {element?%1$s}",
                            expression.getParts().get(0).getName());
                } else {
                    prefixInfo = ": " + config.iterationMetadataPrefix + ", i.e. the correct key should be: "
                            + config.iterationMetadataPrefix + expression.getParts().get(0).getName();
                }
                incorrectExpressions.produce(new IncorrectExpressionBuildItem(expression.toOriginalString(),
                        "An invalid iteration metadata key is probably used\n\t- The configured iteration metadata prefix is "
                                + prefixInfo
                                + "\n\t- You can configure the prefix via the io.quarkus.qute.iteration-metadata-prefix configuration property",
                        expression.getOrigin()));
            } else {
                incorrectExpressions.produce(new IncorrectExpressionBuildItem(expression.toOriginalString(),
                        "Only type-safe expressions are allowed in the checked template defined via: "
                                + checkedTemplate.method.declaringClass().name() + "."
                                + checkedTemplate.method.name()
                                + "(); an expression must be based on a checked template parameter "
                                + checkedTemplate.bindings.keySet()
                                + ", or bound via a param declaration, or the requirement must be relaxed via @CheckedTemplate(requireTypeSafeExpressions = false)",
                        expression.getOrigin()));

            }
            putResult(match, results, expression);
            return true;
        } else {
            return false;
        }
    }

    private static void validateParametersOfNestedVirtualMethods(QuteConfig config, TemplateAnalysis templateAnalysis,
            Map<String, MatchResult> results, Iterable<Predicate<TypeCheck>> excludes,
            BuildProducer<IncorrectExpressionBuildItem> incorrectExpressions,
            Expression expression, IndexView index, Map<DotName, Set<String>> implicitClassToMembersUsed,
            Function<String, String> templateIdToPathFun,
            Map<Integer, MatchResult> generatedIdsToMatches, Iterable<Predicate<TypeCheck>> extensionMethodExcludes,
            CheckedTemplateBuildItem checkedTemplate, JavaMemberLookupConfig lookupConfig, Map<String, BeanInfo> namedBeans,
            Map<String, TemplateDataBuildItem> namespaceTemplateData,
            List<TemplateExtensionMethodBuildItem> regularExtensionMethods,
            Map<String, List<TemplateExtensionMethodBuildItem>> namespaceExtensionMethods,
            AssignabilityCheck assignabilityCheck) {
        for (Expression.Part part : expression.getParts()) {
            if (part.isVirtualMethod()) {
                for (Expression param : part.asVirtualMethod().getParameters()) {
                    if (param.isLiteral() && param.getLiteral() == null) {
                        // "null" literal has no type info
                        continue;
                    }
                    if (!results.containsKey(param.toOriginalString())) {
                        validateNestedExpressions(config, templateAnalysis, null, results, excludes,
                                incorrectExpressions, param, index, implicitClassToMembersUsed, templateIdToPathFun,
                                generatedIdsToMatches, extensionMethodExcludes, checkedTemplate, lookupConfig, namedBeans,
                                namespaceTemplateData, regularExtensionMethods, namespaceExtensionMethods, assignabilityCheck);
                    }
                }
            }
        }
    }

    private static boolean skipValidation(Iterable<Predicate<TypeCheck>> excludes, Expression expression, MatchResult match,
            Info info,
            Type type) {
        TypeCheck check = new TypeCheck(
                info.isProperty() ? info.asProperty().name : info.asVirtualMethod().name,
                match.clazz(), type,
                info.part.isVirtualMethod() ? info.part.asVirtualMethod().getParameters().size() : -1);
        if (isExcluded(check, excludes)) {
            LOGGER.debugf(
                    "Expression part [%s] excluded from validation of [%s] against type [%s]",
                    info.value,
                    expression.toOriginalString(), match.type());
            match.clearValues();
            return true;
        }
        return false;
    }

    private static MatchResult putResult(MatchResult match, Map<String, MatchResult> results, Expression expression) {
        results.put(expression.toOriginalString(), match);
        return match;
    }

    @BuildStep
    void collectTemplateExtensionMethods(BeanArchiveIndexBuildItem beanArchiveIndex,
            BuildProducer<TemplateExtensionMethodBuildItem> extensionMethods) {

        IndexView index = beanArchiveIndex.getIndex();
        Map<MethodInfo, AnnotationInstance> methods = new HashMap<>();
        Map<DotName, AnnotationInstance> classes = new HashMap<>();

        for (AnnotationInstance templateExtension : index.getAnnotations(ExtensionMethodGenerator.TEMPLATE_EXTENSION)) {
            if (templateExtension.target().kind() == Kind.METHOD) {
                methods.put(templateExtension.target().asMethod(), templateExtension);
            } else if (templateExtension.target().kind() == Kind.CLASS) {
                classes.put(templateExtension.target().asClass().name(), templateExtension);
            }
        }

        // Method-level annotations
        for (Entry<MethodInfo, AnnotationInstance> entry : methods.entrySet()) {
            MethodInfo method = entry.getKey();
            AnnotationValue namespaceValue = entry.getValue().value(ExtensionMethodGenerator.NAMESPACE);
            ExtensionMethodGenerator.validate(method, namespaceValue != null ? namespaceValue.asString() : null);
            produceExtensionMethod(index, extensionMethods, method, entry.getValue());
            LOGGER.debugf("Found template extension method %s declared on %s", method,
                    method.declaringClass().name());
        }

        // Class-level annotations
        boolean skippedMethodLevelAnnotation = false;
        for (Entry<DotName, AnnotationInstance> entry : classes.entrySet()) {
            ClassInfo clazz = entry.getValue().target().asClass();
            AnnotationValue namespaceValue = entry.getValue().value(ExtensionMethodGenerator.NAMESPACE);
            String namespace = namespaceValue != null ? namespaceValue.asString() : null;
            List<MethodInfo> found = new ArrayList<>();
            for (MethodInfo method : clazz.methods()) {
                if (!Modifier.isStatic(method.flags()) || method.returnType().kind() == org.jboss.jandex.Type.Kind.VOID
                        || Modifier.isPrivate(method.flags())
                        || ValueResolverGenerator.isSynthetic(method.flags())) {
                    // Filter out non-static, synthetic, private and void methods
                    continue;
                }
                if ((namespace == null || namespace.isEmpty()) && method.parameterTypes().isEmpty()) {
                    // Filter out methods with no params for non-namespace extensions
                    continue;
                }
                if (methods.containsKey(method)) {
                    // Skip methods annotated with @TemplateExtension - method-level annotation takes precedence
                    skippedMethodLevelAnnotation = true;
                    continue;
                }
                found.add(method);
                LOGGER.debugf("Found template extension method %s declared on %s", method,
                        method.declaringClass().name());
            }

            if (found.isEmpty() && !skippedMethodLevelAnnotation) {
                throw new IllegalStateException("No template extension methods declared on " + entry.getKey()
                        + "; a template extension method must be static, non-private and must not return void");
            }
            for (MethodInfo method : found) {
                produceExtensionMethod(index, extensionMethods, method, entry.getValue());
            }
        }
    }

    private void produceExtensionMethod(IndexView index, BuildProducer<TemplateExtensionMethodBuildItem> extensionMethods,
            MethodInfo method, AnnotationInstance extensionAnnotation) {
        // Analyze matchName and priority so that it could be used during validation
        byte matchers = 0;
        String matchName = null;
        AnnotationValue matchNameValue = extensionAnnotation.value(ExtensionMethodGenerator.MATCH_NAME);
        if (matchNameValue != null) {
            matchName = matchNameValue.asString();
            matchers++;
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
            matchers++;
        }
        List<String> matchNames = new ArrayList<>();
        AnnotationValue matchNamesValue = extensionAnnotation.value(ExtensionMethodGenerator.MATCH_NAMES);
        if (matchNamesValue != null) {
            matchers++;
            for (String name : matchNamesValue.asStringArray()) {
                matchNames.add(name);
            }
        }

        if (matchers > 1) {
            LOGGER.warnf("Ignoring superfluous matching conditions defined on %s declared on: %s", extensionAnnotation,
                    extensionAnnotation.target());
        }

        extensionMethods.produce(new TemplateExtensionMethodBuildItem(method, matchName, matchNames, matchRegex,
                namespace.isEmpty() ? method.parameterType(0) : null, priority, namespace));
    }

    private static BeanInfo findBean(Expression expression, IndexView index,
            BuildProducer<IncorrectExpressionBuildItem> incorrectExpressions,
            Map<String, BeanInfo> namedBeans) {
        Expression.Part firstPart = expression.getParts().get(0);
        if (firstPart.isVirtualMethod()) {
            incorrectExpressions.produce(new IncorrectExpressionBuildItem(expression.toOriginalString(),
                    "The inject: namespace must be followed by a bean name",
                    expression.getOrigin()));
            return null;
        }
        String beanName = firstPart.getName();
        BeanInfo bean = namedBeans.get(beanName);
        if (bean != null) {
            return bean;
        } else {
            // User is injecting a non-existing bean
            incorrectExpressions.produce(new IncorrectExpressionBuildItem(expression.toOriginalString(),
                    beanName, null, expression.getOrigin()));
            return null;
        }
    }

    static boolean defaultFilter(AnnotationTarget target) {
        short flags;
        switch (target.kind()) {
            case METHOD:
                flags = target.asMethod().flags();
                break;
            case FIELD:
                flags = target.asField().flags();
                break;
            default:
                throw new IllegalArgumentException();
        }
        // public and non-synthetic members
        return Modifier.isPublic(flags) && !ValueResolverGenerator.isSynthetic(flags);
    }

    static boolean staticsFilter(AnnotationTarget target) {
        switch (target.kind()) {
            case METHOD:
                return Modifier.isStatic(target.asMethod().flags());
            case FIELD:
                // Enum constants are handled specifically due to {#when} enum support
                return Modifier.isStatic(target.asField().flags());
            default:
                throw new IllegalArgumentException();
        }
    }

    static boolean enumConstantFilter(AnnotationTarget target) {
        if (target.kind() == Kind.FIELD) {
            return target.asField().isEnumConstant();
        }
        return false;
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
            BeanArchiveIndexBuildItem beanArchiveIndex,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            List<TemplatePathBuildItem> templatePaths,
            List<TemplateExtensionMethodBuildItem> templateExtensionMethods,
            List<ImplicitValueResolverBuildItem> implicitClasses,
            TemplatesAnalysisBuildItem templatesAnalysis,
            List<PanacheEntityClassesBuildItem> panacheEntityClasses,
            List<TemplateDataBuildItem> templateData,
            List<TemplateGlobalBuildItem> templateGlobals,
            List<IncorrectExpressionBuildItem> incorrectExpressions,
            LiveReloadBuildItem liveReloadBuildItem,
            CompletedApplicationClassPredicateBuildItem applicationClassPredicate,
            BuildProducer<GeneratedValueResolverBuildItem> generatedResolvers,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<GeneratedTemplateInitializerBuildItem> generatedInitializers) {

        if (!incorrectExpressions.isEmpty()) {
            // Skip generation if a validation error occurs
            return;
        }

        IndexView index = beanArchiveIndex.getIndex();
        ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClasses, new Function<String, String>() {
            @Override
            public String apply(String name) {
                int idx = name.lastIndexOf(ExtensionMethodGenerator.NAMESPACE_SUFFIX);
                if (idx == -1) {
                    idx = name.lastIndexOf(ExtensionMethodGenerator.SUFFIX);
                }
                if (idx == -1) {
                    idx = name.lastIndexOf(ValueResolverGenerator.NAMESPACE_SUFFIX);
                }
                if (idx == -1) {
                    idx = name.lastIndexOf(ValueResolverGenerator.SUFFIX);
                }
                if (idx == -1) {
                    idx = name.lastIndexOf(TemplateGlobalGenerator.SUFFIX);
                }
                String className = name.substring(0, idx);
                if (className.contains(ValueResolverGenerator.NESTED_SEPARATOR)) {
                    className = className.replace(ValueResolverGenerator.NESTED_SEPARATOR, "$");
                }
                return className;
            }
        });

        // NOTE: We can't use this optimization for classes generated by ValueResolverGenerator because we cannot easily
        // map a target class to a specific set of generated classes
        ExistingValueResolvers existingValueResolvers = liveReloadBuildItem.getContextObject(ExistingValueResolvers.class);
        if (existingValueResolvers == null) {
            existingValueResolvers = new ExistingValueResolvers();
            liveReloadBuildItem.setContextObject(ExistingValueResolvers.class, existingValueResolvers);
        }
        Set<String> generatedValueResolvers = new HashSet<>();

        ValueResolverGenerator.Builder builder = ValueResolverGenerator.builder()
                .setIndex(index).setClassOutput(classOutput);

        if (!panacheEntityClasses.isEmpty()) {
            Set<String> entityClasses = new HashSet<>();
            for (PanacheEntityClassesBuildItem panaecheEntityClasses : panacheEntityClasses) {
                entityClasses.addAll(panaecheEntityClasses.getEntityClasses());
            }
            builder.setForceGettersFunction(new Function<ClassInfo, Function<FieldInfo, String>>() {
                @Override
                public Function<FieldInfo, String> apply(ClassInfo clazz) {
                    if (entityClasses.contains(clazz.name().toString())) {
                        return GETTER_FUN;
                    }
                    return null;
                }
            });
        }

        Set<DotName> controlled = new HashSet<>();
        Map<DotName, AnnotationInstance> uncontrolled = new HashMap<>();
        for (TemplateDataBuildItem data : templateData) {
            processTemplateData(data, controlled, uncontrolled, builder);
        }

        for (ImplicitValueResolverBuildItem implicit : implicitClasses) {
            DotName implicitClassName = implicit.getClazz().name();
            if (controlled.contains(implicitClassName)) {
                LOGGER.debugf("Implicit value resolver for %s ignored: class is annotated with @TemplateData",
                        implicitClassName);
                continue;
            }
            if (uncontrolled.containsKey(implicitClassName)) {
                LOGGER.debugf("Implicit value resolver for %s ignored: %s declared on %s", implicitClassName,
                        uncontrolled.get(implicitClassName),
                        uncontrolled.get(implicitClassName).target());
                continue;
            }
            builder.addClass(implicit.getClazz(), implicit.getTemplateData());
        }

        ValueResolverGenerator generator = builder.build();
        generator.generate();
        generatedValueResolvers.addAll(generator.getGeneratedTypes());

        ExtensionMethodGenerator extensionMethodGenerator = new ExtensionMethodGenerator(index, classOutput);
        Map<DotName, Map<String, List<TemplateExtensionMethodBuildItem>>> classToNamespaceExtensions = new HashMap<>();
        Map<String, DotName> namespaceToClass = new HashMap<>();

        for (TemplateExtensionMethodBuildItem templateExtension : templateExtensionMethods) {
            String generatedValueResolverClass = existingValueResolvers.getGeneratedClass(templateExtension.getMethod());
            if (generatedValueResolverClass != null) {
                // A ValueResolver of a non-application class was already generated
                generatedValueResolvers.add(generatedValueResolverClass);
                continue;
            }

            if (templateExtension.hasNamespace()) {
                // Group extension methods declared on the same class by namespace
                DotName declaringClassName = templateExtension.getMethod().declaringClass().name();
                DotName namespaceClassName = namespaceToClass.get(templateExtension.getNamespace());
                if (namespaceClassName == null) {
                    namespaceToClass.put(templateExtension.getNamespace(), namespaceClassName);
                } else if (!namespaceClassName.equals(declaringClassName)) {
                    throw new IllegalStateException("Template extension methods that share the namespace "
                            + templateExtension.getNamespace() + " must be declared on the same class; but declared on "
                            + namespaceClassName + " and " + declaringClassName);
                }
                Map<String, List<TemplateExtensionMethodBuildItem>> namespaceToExtensions = classToNamespaceExtensions
                        .get(declaringClassName);
                if (namespaceToExtensions == null) {
                    namespaceToExtensions = new HashMap<>();
                    classToNamespaceExtensions.put(declaringClassName, namespaceToExtensions);
                }
                List<TemplateExtensionMethodBuildItem> namespaceMethods = namespaceToExtensions
                        .get(templateExtension.getNamespace());
                if (namespaceMethods == null) {
                    namespaceMethods = new ArrayList<>();
                    namespaceToExtensions.put(templateExtension.getNamespace(), namespaceMethods);
                }
                namespaceMethods.add(templateExtension);
            } else {
                // Generate ValueResolver per extension method
                String generatedClass = extensionMethodGenerator.generate(templateExtension.getMethod(),
                        templateExtension.getMatchName(),
                        templateExtension.getMatchNames(), templateExtension.getMatchRegex(), templateExtension.getPriority());
                existingValueResolvers.add(templateExtension.getMethod(), generatedClass, applicationClassPredicate);
            }
        }

        // Generate a namespace resolver for extension methods declared on the same class and of the same priority
        for (Entry<DotName, Map<String, List<TemplateExtensionMethodBuildItem>>> classEntry : classToNamespaceExtensions
                .entrySet()) {
            Map<String, List<TemplateExtensionMethodBuildItem>> namespaceToMethods = classEntry.getValue();
            for (Entry<String, List<TemplateExtensionMethodBuildItem>> nsEntry : namespaceToMethods.entrySet()) {

                Map<Integer, List<TemplateExtensionMethodBuildItem>> priorityToMethods = nsEntry.getValue().stream()
                        .collect(Collectors.groupingBy(TemplateExtensionMethodBuildItem::getPriority));

                for (Entry<Integer, List<TemplateExtensionMethodBuildItem>> priorityEntry : priorityToMethods.entrySet()) {
                    try (NamespaceResolverCreator namespaceResolverCreator = extensionMethodGenerator
                            .createNamespaceResolver(priorityEntry.getValue().get(0).getMethod().declaringClass(),
                                    nsEntry.getKey(), priorityEntry.getKey())) {
                        for (TemplateExtensionMethodBuildItem extensionMethod : priorityEntry.getValue()) {
                            existingValueResolvers.add(extensionMethod.getMethod(), namespaceResolverCreator.getClassName(),
                                    applicationClassPredicate);
                        }
                        try (ResolveCreator resolveCreator = namespaceResolverCreator.implementResolve()) {
                            for (TemplateExtensionMethodBuildItem method : priorityEntry.getValue()) {
                                resolveCreator.addMethod(method.getMethod(), method.getMatchName(), method.getMatchNames(),
                                        method.getMatchRegex());
                            }
                        }
                    }
                }
            }
        }

        generatedValueResolvers.addAll(extensionMethodGenerator.getGeneratedTypes());

        LOGGER.debugf("Generated %s value resolvers: %s", generatedValueResolvers.size(), generatedValueResolvers);

        for (String generatedType : generatedValueResolvers) {
            generatedResolvers.produce(new GeneratedValueResolverBuildItem(generatedType));
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(generatedType).build());
        }

        if (!templateGlobals.isEmpty()) {
            TemplateGlobalGenerator globalGenerator = new TemplateGlobalGenerator(classOutput);

            Map<DotName, Map<String, AnnotationTarget>> classToTargets = new HashMap<>();
            Map<DotName, List<TemplateGlobalBuildItem>> classToGlobals = templateGlobals.stream()
                    .collect(Collectors.groupingBy(TemplateGlobalBuildItem::getDeclaringClass));
            for (Entry<DotName, List<TemplateGlobalBuildItem>> entry : classToGlobals.entrySet()) {
                classToTargets.put(entry.getKey(), entry.getValue().stream().collect(
                        Collectors.toMap(TemplateGlobalBuildItem::getName, TemplateGlobalBuildItem::getTarget)));
            }

            for (Entry<DotName, Map<String, AnnotationTarget>> e : classToTargets.entrySet()) {
                globalGenerator.generate(index.getClassByName(e.getKey()), e.getValue());
            }

            for (String generatedType : globalGenerator.getGeneratedTypes()) {
                generatedInitializers.produce(new GeneratedTemplateInitializerBuildItem(generatedType));
                reflectiveClass.produce(ReflectiveClassBuildItem.builder(generatedType).build());
            }
        }
    }

    /**
     * Tracks non-application value resolvers that have already been generated. There is no need to spend time
     * generating them again on a hot reload.
     */
    static class ExistingValueResolvers {

        final Map<String, String> identifiersToGeneratedClass = new HashMap<>();

        boolean contains(MethodInfo extensionMethod) {
            return identifiersToGeneratedClass
                    .containsKey(toKey(extensionMethod));
        }

        String getGeneratedClass(MethodInfo extensionMethod) {
            return identifiersToGeneratedClass.get(toKey(extensionMethod));
        }

        void add(MethodInfo extensionMethod, String className, Predicate<DotName> applicationClassPredicate) {
            if (!applicationClassPredicate.test(extensionMethod.declaringClass().name())) {
                identifiersToGeneratedClass.put(toKey(extensionMethod), className);
            }
        }

        private String toKey(MethodInfo extensionMethod) {
            return extensionMethod.declaringClass().toString() + "#" + extensionMethod.toString();
        }
    }

    @BuildStep
    void collectTemplates(ApplicationArchivesBuildItem applicationArchives,
            CurateOutcomeBuildItem curateOutcome,
            BuildProducer<HotDeploymentWatchedFileBuildItem> watchedPaths,
            BuildProducer<TemplatePathBuildItem> templatePaths,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            QuteConfig config)
            throws IOException {
        Set<Path> basePaths = new HashSet<>();
        Set<ApplicationArchive> allApplicationArchives = applicationArchives.getAllApplicationArchives();
        List<ResolvedDependency> extensionArtifacts = curateOutcome.getApplicationModel().getDependencies().stream()
                .filter(Dependency::isRuntimeExtensionArtifact).collect(Collectors.toList());

        for (ResolvedDependency artifact : extensionArtifacts) {
            if (isApplicationArchive(artifact, allApplicationArchives)) {
                // Skip extension archives that are also application archives
                continue;
            }
            for (Path path : artifact.getResolvedPaths()) {
                if (Files.isDirectory(path)) {
                    // Try to find the templates in the root dir
                    try (Stream<Path> paths = Files.list(path)) {
                        Path basePath = paths.filter(QuteProcessor::isBasePath).findFirst().orElse(null);
                        if (basePath != null) {
                            LOGGER.debugf("Found extension templates dir: %s", path);
                            scan(basePath, basePath, BASE_PATH + "/", watchedPaths, templatePaths, nativeImageResources,
                                    config);
                            break;
                        }
                    }
                } else {
                    try (FileSystem artifactFs = ZipUtils.newFileSystem(path)) {
                        Path basePath = artifactFs.getPath(BASE_PATH);
                        if (Files.exists(basePath)) {
                            LOGGER.debugf("Found extension templates in: %s", path);
                            scan(basePath, basePath, BASE_PATH + "/", watchedPaths, templatePaths, nativeImageResources,
                                    config);
                        }
                    } catch (IOException e) {
                        LOGGER.warnf(e, "Unable to create the file system from the path: %s", path);
                    }
                }
            }
        }
        for (ApplicationArchive archive : allApplicationArchives) {
            archive.accept(tree -> {
                for (Path rootDir : tree.getRoots()) {
                    // Note that we cannot use ApplicationArchive.getChildPath(String) here because we would not be able to detect
                    // a wrong directory name on case-insensitive file systems
                    try (Stream<Path> rootDirPaths = Files.list(rootDir)) {
                        Path basePath = rootDirPaths.filter(QuteProcessor::isBasePath).findFirst().orElse(null);
                        if (basePath != null) {
                            LOGGER.debugf("Found templates dir: %s", basePath);
                            basePaths.add(basePath);
                            scan(basePath, basePath, BASE_PATH + "/", watchedPaths, templatePaths, nativeImageResources,
                                    config);
                            break;
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            });
        }
    }

    @BuildStep
    TemplateFilePathsBuildItem collectTemplateFilePaths(QuteConfig config, List<TemplatePathBuildItem> templatePaths) {
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
        return new TemplateFilePathsBuildItem(filePaths);
    }

    @BuildStep
    void validateTemplateInjectionPoints(TemplateFilePathsBuildItem filePaths, List<TemplatePathBuildItem> templatePaths,
            ValidationPhaseBuildItem validationPhase, BuildProducer<ValidationErrorBuildItem> validationErrors,
            CustomTemplateLocatorPatternsBuildItem locatorPatternsBuildItem) {

        for (InjectionPointInfo injectionPoint : validationPhase.getContext().getInjectionPoints()) {
            if (injectionPoint.getRequiredType().name().equals(Names.TEMPLATE)) {
                AnnotationInstance location = injectionPoint.getRequiredQualifier(Names.LOCATION);
                String name;
                if (location != null) {
                    name = location.value().asString();
                } else if (injectionPoint.hasDefaultedQualifier()) {
                    name = getName(injectionPoint);
                } else {
                    name = null;
                }
                if (name != null) {
                    // For "@Inject Template items" we try to match "items"
                    // For "@Location("github/pulls") Template pulls" we try to match "github/pulls"
                    // For "@Location("foo/bar/baz.txt") Template baz" we try to match "foo/bar/baz.txt"
                    if (!filePaths.contains(name)
                            && isNotLocatedByCustomTemplateLocator(locatorPatternsBuildItem.getLocationPatterns(),
                                    name)) {
                        validationErrors.produce(new ValidationErrorBuildItem(
                                new TemplateException(
                                        String.format(
                                                "No template found for path [%s] defined at %s\n\t- available templates: %s",
                                                name, injectionPoint.getTargetInfo(), templatePaths.stream()
                                                        .map(TemplatePathBuildItem::getPath).collect(Collectors.toList())))));
                    }
                }
            }
        }
    }

    @BuildStep
    CustomTemplateLocatorPatternsBuildItem validateAndCollectCustomTemplateLocatorLocations(
            BeanArchiveIndexBuildItem beanArchiveIndex,
            BuildProducer<ValidationErrorBuildItem> validationErrors) {

        Collection<Pattern> locationPatterns = new ArrayList<>();

        // Collect TemplateLocators annotated with io.quarkus.qute.Locate
        for (AnnotationInstance locate : beanArchiveIndex.getIndex().getAnnotations(Names.LOCATE)) {
            AnnotationTarget locateTarget = locate.target();
            if (locateTarget.kind() == Kind.CLASS) {
                if (Types.isImplementorOf(locateTarget.asClass(), Names.TEMPLATE_LOCATOR, beanArchiveIndex.getIndex())) {
                    addLocationRegExToLocators(locationPatterns, locate.value(), locateTarget, validationErrors);
                } else {
                    reportFoundInvalidTarget(validationErrors, locateTarget);
                }
            }
        }

        // Collect TemplateLocators annotated with multiple 'io.quarkus.qute.Locate'
        for (AnnotationInstance locates : beanArchiveIndex.getIndex().getAnnotations(Names.LOCATES)) {
            AnnotationTarget locatesTarget = locates.target();
            if (locatesTarget.kind() == Kind.CLASS) {
                if (Types.isImplementorOf(locatesTarget.asClass(), Names.TEMPLATE_LOCATOR, beanArchiveIndex.getIndex())) {
                    // locates.value() is array of 'io.quarkus.qute.Locate'
                    for (AnnotationInstance locate : locates.value().asNestedArray()) {
                        addLocationRegExToLocators(locationPatterns, locate.value(), locatesTarget, validationErrors);
                    }
                } else {
                    reportFoundInvalidTarget(validationErrors, locatesTarget);
                }
            }
        }

        return new CustomTemplateLocatorPatternsBuildItem(locationPatterns);
    }

    @BuildStep
    void collectEngineConfigurations(
            BeanArchiveIndexBuildItem beanArchiveIndex,
            BuildProducer<EngineConfigurationsBuildItem> engineConfig,
            BuildProducer<ValidationErrorBuildItem> validationErrors) {

        Collection<AnnotationInstance> engineConfigAnnotations = beanArchiveIndex.getIndex()
                .getAnnotations(Names.ENGINE_CONFIGURATION);
        if (engineConfigAnnotations.isEmpty()) {
            return;
        }

        List<ClassInfo> engineConfigClasses = new ArrayList<>();
        IndexView index = beanArchiveIndex.getIndex();

        for (AnnotationInstance annotation : engineConfigAnnotations) {
            AnnotationTarget target = annotation.target();
            if (target.kind() == Kind.CLASS) {
                ClassInfo targetClass = target.asClass();

                if (targetClass.nestingType() != NestingType.TOP_LEVEL
                        && (targetClass.nestingType() != NestingType.INNER || !Modifier.isStatic(targetClass.flags()))) {
                    validationErrors.produce(
                            new ValidationErrorBuildItem(
                                    new TemplateException(String.format(
                                            "Only top-level and static nested classes may be annotated with @%s: %s",
                                            EngineConfiguration.class.getSimpleName(), targetClass.name()))));
                } else if (Types.isImplementorOf(targetClass, Names.SECTION_HELPER_FACTORY, index)) {
                    if (targetClass.hasNoArgsConstructor()) {
                        engineConfigClasses.add(targetClass);
                    } else {
                        validationErrors.produce(
                                new ValidationErrorBuildItem(
                                        new TemplateException(String.format(
                                                "A class annotated with @%s that also implements io.quarkus.qute.SectionHelperFactory must declare a no-args constructor: %s",
                                                EngineConfiguration.class.getSimpleName(), targetClass.name()))));
                    }
                } else if (Types.isImplementorOf(targetClass, Names.VALUE_RESOLVER, index)
                        || Types.isImplementorOf(targetClass, Names.NAMESPACE_RESOLVER, index)) {
                    engineConfigClasses.add(targetClass);
                } else {
                    validationErrors.produce(
                            new ValidationErrorBuildItem(
                                    new TemplateException(String.format(
                                            "A class annotated with @%s must implement one of the %s: %s",
                                            EngineConfiguration.class.getSimpleName(), Arrays.toString(
                                                    new String[] { SectionHelperFactory.class.getName(),
                                                            ValueResolver.class.getName(),
                                                            NamespaceResolver.class.getName() }),
                                            targetClass.name()))));
                }
            }
        }
        engineConfig.produce(new EngineConfigurationsBuildItem(engineConfigClasses));
    }

    private void addLocationRegExToLocators(Collection<Pattern> locationToLocators,
            AnnotationValue value, AnnotationTarget target,
            BuildProducer<ValidationErrorBuildItem> validationErrors) {
        String regex = value.asString();
        if (regex.isBlank()) {
            validationErrors.produce(
                    new ValidationErrorBuildItem(
                            new TemplateException(String.format(
                                    "'io.quarkus.qute.Locate#value()' must not be blank: %s",
                                    target.asClass().name().toString()))));
        } else {
            locationToLocators.add(Pattern.compile(regex));
        }
    }

    private void reportFoundInvalidTarget(BuildProducer<ValidationErrorBuildItem> validationErrors,
            AnnotationTarget locateTarget) {
        validationErrors.produce(
                new ValidationErrorBuildItem(
                        new TemplateException(String.format(
                                "Classes annotated with 'io.quarkus.qute.Locate' must implement 'io.quarkus.qute.TemplateLocator': %s",
                                locateTarget.asClass().name().toString()))));
    }

    @BuildStep
    TemplateVariantsBuildItem collectTemplateVariants(List<TemplatePathBuildItem> templatePaths) throws IOException {
        Set<String> allPaths = templatePaths.stream().map(TemplatePathBuildItem::getPath).collect(Collectors.toSet());
        // item -> [item.html, item.txt]
        // ItemResource/item -> -> [ItemResource/item.html, ItemResource/item.xml]
        Map<String, List<String>> baseToVariants = new HashMap<>();
        for (String path : allPaths) {
            int idx = path.indexOf('.');
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
    void excludeTypeChecks(QuteConfig config, BuildProducer<TypeCheckExcludeBuildItem> excludes) {
        // Exclude all checks that involve built-in value resolvers
        List<String> skipOperators = Arrays.asList("?:", "or", ":", "?", "ifTruthy", "&&", "||");
        excludes.produce(new TypeCheckExcludeBuildItem(new Predicate<TypeCheck>() {
            @Override
            public boolean test(TypeCheck check) {
                // RawString and orEmpty - these properties can be used on any object
                if (check.isProperty()
                        && ("raw".equals(check.name) || "safe".equals(check.name) || "orEmpty".equals(check.name))) {
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

        excludes.produce(new TypeCheckExcludeBuildItem(new Predicate<TypeCheck>() {
            @Override
            public boolean test(TypeCheck typeCheck) {
                // OrOperatorTemplateExtensions should only be validated if we were able to resolve actual type
                return OR.equals(typeCheck.name) && typeCheck.type != null && DotNames.OBJECT.equals(typeCheck.type.name());
            }
        }, true));

        if (config.typeCheckExcludes.isPresent()) {
            for (String exclude : config.typeCheckExcludes.get()) {
                //
                String[] parts = exclude.split("\\.");
                if (parts.length < 2) {
                    // An exclude rule must have at least two parts separated by dot
                    continue;
                }
                String className = Arrays.stream(parts).limit(parts.length - 1).collect(Collectors.joining("."));
                String propertyName = parts[parts.length - 1];
                excludes.produce(new TypeCheckExcludeBuildItem(new Predicate<TypeCheck>() {
                    @Override
                    public boolean test(TypeCheck check) {
                        if (!className.equals("*") && !check.clazz.name().toString().equals(className)) {
                            return false;
                        }
                        if (!propertyName.equals("*") && !check.name.equals(propertyName)) {
                            return false;
                        }
                        return true;
                    }
                }));
            }
        }
    }

    @BuildStep
    @Record(value = STATIC_INIT)
    void initialize(BuildProducer<SyntheticBeanBuildItem> syntheticBeans, QuteRecorder recorder,
            List<GeneratedValueResolverBuildItem> generatedValueResolvers, List<TemplatePathBuildItem> templatePaths,
            Optional<TemplateVariantsBuildItem> templateVariants,
            List<GeneratedTemplateInitializerBuildItem> templateInitializers) {

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
                        tags, variants, templateInitializers.stream()
                                .map(GeneratedTemplateInitializerBuildItem::getClassName).collect(Collectors.toList())))
                .done());
    }

    @BuildStep
    QualifierRegistrarBuildItem turnLocationIntoQualifier() {

        return new QualifierRegistrarBuildItem(new QualifierRegistrar() {

            @Override
            public Map<DotName, Set<String>> getAdditionalQualifiers() {
                return Collections.singletonMap(Names.LOCATION, Collections.singleton("value"));
            }
        });
    }

    private static Type resolveType(AnnotationTarget member, MatchResult match, IndexView index,
            TemplateExtensionMethodBuildItem extensionMethod, Map<String, MatchResult> results, Info info) {
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

            if (match.clazz == null) {
                if (member.kind() == Kind.METHOD && match.isPrimitive()) {
                    final Type wrapperType = Types.box(match.type.asPrimitiveType());
                    match.setValues(index.getClassByName(wrapperType.name()), wrapperType);
                } else {
                    // we can't resolve type without class
                    return matchType;
                }
            }

            // First get the type closure of the current match type
            Set<Type> closure = Types.getTypeClosure(match.clazz,
                    Types.buildResolvedMap(
                            match.getParameterizedTypeArguments(), match.getTypeParameters(),
                            new HashMap<>(), index),
                    index);

            DotName declaringClassName = null;
            Type extensionMatchBase = null;
            if (member.kind() == Kind.METHOD) {
                MethodInfo method = member.asMethod();
                List<TypeVariable> typeParameters = method.typeParameters();
                if (extensionMethod != null && !extensionMethod.hasNamespace() && !typeParameters.isEmpty()) {
                    // Special handling for extension methods with type parameters
                    // For example "static <T> Iterator<T> reversed(List<T> list)"
                    // 1. identify the type used to match the base object; List<T>
                    // 2. resolve this type; List<String>
                    // 3. if needed apply to the return type; Iterator<String>
                    List<Type> params = method.parameterTypes();
                    Set<AnnotationInstance> attributeAnnotations = Annotations.getAnnotations(Kind.METHOD_PARAMETER,
                            ExtensionMethodGenerator.TEMPLATE_ATTRIBUTE, method.annotations());
                    if (attributeAnnotations.isEmpty()) {
                        extensionMatchBase = params.get(0);
                    } else {
                        for (int i = 0; i < params.size(); i++) {
                            int position = i;
                            if (attributeAnnotations.stream()
                                    .noneMatch(a -> a.target().asMethodParameter().position() == position)) {
                                // The first parameter that is not annotated with @TemplateAttribute is used to match the base object
                                extensionMatchBase = params.get(i);
                                break;
                            }
                        }
                    }
                    if (extensionMatchBase != null && Types.containsTypeVariable(extensionMatchBase)) {

                        // special handling for T methodName(T t, ...) without attribute annotations
                        if (extensionMatchBase.kind() == Type.Kind.TYPE_VARIABLE && attributeAnnotations.isEmpty()
                                && extensionMatchBase.name().equals(matchType.name()) && info.isVirtualMethod()) {

                            // take into consideration other formal parameters of type T (f.e. T methodName(T t, T u))
                            // keep it simple and skip parametrized types e.g. List<T>
                            if (info.part.asVirtualMethod().getParameters() != null
                                    && !info.part.asVirtualMethod().getParameters().isEmpty()) {
                                final var paramExpressions = info.part.asVirtualMethod().getParameters();
                                for (int i = 1; i < params.size() && (i - 1) < paramExpressions.size(); i++) {
                                    // whether params.get(i) has same type as the extension base (e.g. T)
                                    if (params.get(i).name().equals(extensionMatchBase.name())) {
                                        var paramMatch = results.get(paramExpressions.get(i - 1).toOriginalString());
                                        if (paramMatch != null) {
                                            Type paramMatchType = paramMatch.type();
                                            if (paramMatch.isPrimitive()) {
                                                // use boxed type
                                                paramMatchType = Types.box(paramMatch.type());
                                            }
                                            // if all T params are not of exactly same type, we do not try to determine
                                            // right superclass/interface as it's expensive
                                            if (!match.type().equals(paramMatchType)) {
                                                return matchType;
                                            }
                                        }
                                    }
                                }
                            }

                            // obj.methodName(T t, ...) => 'obj' type equals T
                            return match.type();
                        }

                        declaringClassName = extensionMatchBase.name();
                    }
                } else {
                    declaringClassName = method.declaringClass().name();
                }
            } else if (member.kind() == Kind.FIELD) {
                declaringClassName = member.asField().declaringClass().name();
            }
            // Then find the declaring type with resolved type variables
            Type declaringType = null;
            if (declaringClassName != null) {
                for (Type type : closure) {
                    if (type.name().equals(declaringClassName)) {
                        declaringType = type;
                        break;
                    }
                }
            }
            if (declaringType != null
                    && declaringType.kind() == org.jboss.jandex.Type.Kind.PARAMETERIZED_TYPE) {
                List<TypeVariable> typeParameters;
                if (extensionMatchBase != null) {
                    typeParameters = extensionMethod.getMethod().typeParameters();
                } else {
                    typeParameters = index.getClassByName(declaringType.name()).typeParameters();
                }
                matchType = Types.resolveTypeParam(matchType,
                        Types.buildResolvedMap(declaringType.asParameterizedType().arguments(),
                                typeParameters,
                                Collections.emptyMap(),
                                index),
                        index);
            }
        }
        return matchType;
    }

    static Iterator<Info> processHintsIfNeeded(Info root, Iterator<Info> iterator, List<Info> parts,
            TemplateAnalysis templateAnalysis, List<String> helperHints, MatchResult match, IndexView index,
            Expression expression, Map<Integer, MatchResult> generatedIdsToMatches,
            BuildProducer<IncorrectExpressionBuildItem> incorrectExpressions) {
        if (root.hasHints()) {
            // Root is a property with hint
            // E.g. 'it<loop#123>' and 'STATUS<when#123>'
            if (processHints(templateAnalysis, root.asHintInfo().hints, match, index, expression,
                    generatedIdsToMatches, incorrectExpressions)) {
                // In some cases it's necessary to reset the iterator
                return parts.iterator();
            }
        }
        return iterator;
    }

    /**
     * @param templateAnalysis
     * @param helperHints
     * @param match
     * @param index
     * @param expression
     * @param generatedIdsToMatches
     * @param incorrectExpressions
     * @return {@code true} if it is necessary to reset the type info part iterator
     */
    static boolean processHints(TemplateAnalysis templateAnalysis, List<String> helperHints, MatchResult match, IndexView index,
            Expression expression, Map<Integer, MatchResult> generatedIdsToMatches,
            BuildProducer<IncorrectExpressionBuildItem> incorrectExpressions) {
        if (helperHints == null || helperHints.isEmpty()) {
            return false;
        }
        for (String helperHint : helperHints) {
            if (helperHint.equals(LoopSectionHelper.Factory.HINT_ELEMENT)) {
                // Iterable<Item>, Stream<Item> => Item
                // Map<String,Long> => Entry<String,Long>
                processLoopElementHint(match, index, expression, incorrectExpressions);
            } else if (helperHint.startsWith(LoopSectionHelper.Factory.HINT_PREFIX)) {
                setMatchValues(match, findExpression(helperHint, LoopSectionHelper.Factory.HINT_PREFIX, templateAnalysis),
                        generatedIdsToMatches, index);
            } else if (helperHint.startsWith(WhenSectionHelper.Factory.HINT_PREFIX)) {
                // If a value expression resolves to an enum we attempt to use the enum type to validate the enum constant
                // This basically transforms the type info "ON<when:12345>" into something like "|org.acme.Status|.ON"
                Expression valueExpr = findExpression(helperHint, WhenSectionHelper.Factory.HINT_PREFIX, templateAnalysis);
                if (valueExpr != null) {
                    MatchResult valueExprMatch = generatedIdsToMatches.get(valueExpr.getGeneratedId());
                    if (valueExprMatch != null && valueExprMatch.clazz.isEnum()) {
                        match.setValues(valueExprMatch.clazz, valueExprMatch.type);
                        return true;
                    }
                }
            } else if (helperHint.startsWith(SetSectionHelper.Factory.HINT_PREFIX)) {
                setMatchValues(match, findExpression(helperHint, SetSectionHelper.Factory.HINT_PREFIX, templateAnalysis),
                        generatedIdsToMatches, index);
            }
        }
        return false;
    }

    private static void setMatchValues(MatchResult match, Expression valueExpr, Map<Integer, MatchResult> generatedIdsToMatches,
            IndexView index) {
        if (valueExpr != null) {
            if (valueExpr.isLiteral()) {
                Object literalValue = valueExpr.getLiteral();
                if (literalValue == null) {
                    match.clearValues();
                } else {
                    if (literalValue instanceof Boolean) {
                        match.setValues(index.getClassByName(DotNames.BOOLEAN), Types.box(Primitive.BOOLEAN));
                    } else if (literalValue instanceof String) {
                        match.setValues(index.getClassByName(DotNames.STRING),
                                Type.create(DotNames.STRING, org.jboss.jandex.Type.Kind.CLASS));
                    } else if (literalValue instanceof Integer) {
                        match.setValues(index.getClassByName(DotNames.INTEGER), Types.box(Primitive.INT));
                    } else if (literalValue instanceof Long) {
                        match.setValues(index.getClassByName(DotNames.LONG), Types.box(Primitive.LONG));
                    } else if (literalValue instanceof Double) {
                        match.setValues(index.getClassByName(DotNames.DOUBLE), Types.box(Primitive.DOUBLE));
                    } else if (literalValue instanceof Float) {
                        match.setValues(index.getClassByName(DotNames.FLOAT), Types.box(Primitive.FLOAT));
                    }
                }
            } else {
                MatchResult valueExprMatch = generatedIdsToMatches.get(valueExpr.getGeneratedId());
                if (valueExprMatch != null) {
                    match.setValues(valueExprMatch.clazz, valueExprMatch.type);
                }
            }
        }
    }

    private static Expression findExpression(String helperHint, String hintPrefix, TemplateAnalysis templateAnalysis) {
        return templateAnalysis
                .findExpression(Integer.parseInt(helperHint.substring(hintPrefix.length(), helperHint.length() - 1)));
    }

    private static Expression findExpression(int generatedId, Iterable<Expression> expressions) {
        for (Expression expression : expressions) {
            if (expression.getGeneratedId() == generatedId) {
                return expression;
            }
        }
        return null;
    }

    private static int parseHintId(String helperHint, String hintPrefix) {
        return Integer.parseInt(helperHint.substring(hintPrefix.length(), helperHint.length() - 1));
    }

    static void processLoopElementHint(MatchResult match, IndexView index, Expression expression,
            BuildProducer<IncorrectExpressionBuildItem> incorrectExpressions) {
        if (match.isEmpty()
                || match.type().name().equals(DotNames.INTEGER)
                || match.type().equals(PrimitiveType.INT)) {
            return;
        }
        Type matchType = null;
        if (match.isArray()) {
            matchType = match.type().asArrayType().constituent();
        } else if (match.isClass() || match.isParameterizedType()) {
            Set<Type> closure = Types.getTypeClosure(match.clazz, Types.buildResolvedMap(
                    match.getParameterizedTypeArguments(), match.getTypeParameters(), new HashMap<>(), index), index);
            // Iterable<Item> => Item
            matchType = extractMatchType(closure, Names.ITERABLE, FIRST_PARAM_TYPE_EXTRACT_FUN);
            if (matchType == null) {
                // Stream<Long> => Long
                matchType = extractMatchType(closure, Names.STREAM, FIRST_PARAM_TYPE_EXTRACT_FUN);
            }
            if (matchType == null) {
                // Entry<K,V> => Entry<String,Item>
                matchType = extractMatchType(closure, Names.MAP, MAP_ENTRY_EXTRACT_FUN);
            }
            if (matchType == null) {
                // Iterator<Item> => Item
                matchType = extractMatchType(closure, Names.ITERATOR, FIRST_PARAM_TYPE_EXTRACT_FUN);
            }
        }

        if (matchType != null) {
            match.setValues(index.getClassByName(matchType.name()), matchType);
        } else {
            incorrectExpressions.produce(new IncorrectExpressionBuildItem(expression.toOriginalString(),
                    "Unsupported iterable type found: " + match.type, expression.getOrigin()));
            match.clearValues();
        }
    }

    static final Function<Type, Type> FIRST_PARAM_TYPE_EXTRACT_FUN = new Function<Type, Type>() {

        @Override
        public Type apply(Type type) {
            return type.asParameterizedType().arguments().get(0);
        }

    };

    static final Function<Type, Type> MAP_ENTRY_EXTRACT_FUN = new Function<Type, Type>() {

        @Override
        public Type apply(Type type) {
            Type[] args = new Type[2];
            args[0] = type.asParameterizedType().arguments().get(0);
            args[1] = type.asParameterizedType().arguments().get(1);
            return ParameterizedType.create(Names.MAP_ENTRY, args, null);
        }

    };

    static Type extractMatchType(Set<Type> closure, DotName matchName, Function<Type, Type> extractFun) {
        Type type = null;
        for (Type t : closure) {
            if (t.name().equals(matchName)) {
                type = t;
            }
        }
        return type != null ? extractFun.apply(type) : null;
    }

    static class MatchResult {

        private final AssignabilityCheck assignabilityCheck;

        private ClassInfo clazz;
        private Type type;

        MatchResult(AssignabilityCheck assignabilityCheck) {
            this.assignabilityCheck = assignabilityCheck;
        }

        List<Type> getParameterizedTypeArguments() {
            return type.kind() == org.jboss.jandex.Type.Kind.PARAMETERIZED_TYPE ? type.asParameterizedType().arguments()
                    : Collections.emptyList();
        }

        List<TypeVariable> getTypeParameters() {
            return clazz.typeParameters();
        }

        ClassInfo clazz() {
            return clazz;
        }

        Type type() {
            return type;
        }

        boolean isPrimitive() {
            return type != null && type.kind() == org.jboss.jandex.Type.Kind.PRIMITIVE;
        }

        boolean isArray() {
            return type != null && type.kind() == org.jboss.jandex.Type.Kind.ARRAY;
        }

        boolean isParameterizedType() {
            return type != null && type.kind() == org.jboss.jandex.Type.Kind.PARAMETERIZED_TYPE;
        }

        boolean isClass() {
            return type != null && type.kind() == org.jboss.jandex.Type.Kind.CLASS;
        }

        void setValues(ClassInfo clazz, Type type) {
            this.clazz = clazz;
            this.type = type;
            autoExtractType();
        }

        void clearValues() {
            clazz = null;
            type = null;
        }

        boolean isEmpty() {
            // For arrays the class is null
            return type == null;
        }

        void autoExtractType() {
            if (clazz != null) {
                boolean hasCompletionStage = assignabilityCheck.isAssignableFrom(Names.COMPLETION_STAGE, clazz.name());
                boolean hasUni = hasCompletionStage ? false : assignabilityCheck.isAssignableFrom(Names.UNI, clazz.name());
                if (hasCompletionStage || hasUni) {
                    Set<Type> closure = Types.getTypeClosure(clazz, Types.buildResolvedMap(
                            getParameterizedTypeArguments(), getTypeParameters(), new HashMap<>(),
                            assignabilityCheck.computingIndex),
                            assignabilityCheck.computingIndex);
                    // CompletionStage<List<Item>> => List<Item>
                    // Uni<List<String>> => List<String>
                    this.type = extractMatchType(closure, hasCompletionStage ? Names.COMPLETION_STAGE : Names.UNI,
                            FIRST_PARAM_TYPE_EXTRACT_FUN);
                    this.clazz = assignabilityCheck.computingIndex.getClassByName(type.name());
                }
            }
        }
    }

    private static TemplateExtensionMethodBuildItem findTemplateExtensionMethod(Info info, Type matchType,
            List<TemplateExtensionMethodBuildItem> templateExtensionMethods, Expression expression, IndexView index,
            Function<String, String> templateIdToPathFun, Map<String, MatchResult> results,
            AssignabilityCheck assignabilityCheck) {
        if (!info.isProperty() && !info.isVirtualMethod()) {
            return null;
        }
        String name = info.isProperty() ? info.asProperty().name : info.asVirtualMethod().name;
        for (TemplateExtensionMethodBuildItem extensionMethod : templateExtensionMethods) {
            if (!extensionMethod.matchesName(name)) {
                // Name does not match
                continue;
            }
            if (matchType != null
                    && !assignabilityCheck.isAssignableFrom(extensionMethod.getMatchType(), matchType)) {
                // If "Bar extends Foo" then Bar should be matched for the extension method "int get(Foo)"
                continue;
            }
            List<Param> evaluatedParams = extensionMethod.getParams().evaluated();
            if (evaluatedParams.size() > 0 && !info.isVirtualMethod()) {
                // If method accepts additional params the info must be a virtual method
                continue;
            }
            if (info.isVirtualMethod()) {
                // For virtual method validate the number of params and attempt to validate the parameter types if available
                VirtualMethodPart virtualMethod = info.part.asVirtualMethod();

                boolean isVarArgs = ValueResolverGenerator.isVarArgs(extensionMethod.getMethod());
                int lastParamIdx = evaluatedParams.size() - 1;

                if (isVarArgs) {
                    // For varargs methods match the minimal number of params
                    if ((evaluatedParams.size() - 1) > virtualMethod.getParameters().size()) {
                        continue;
                    }
                } else {
                    if (virtualMethod.getParameters().size() != evaluatedParams.size()) {
                        // Check number of parameters; some of params of the extension method must be ignored
                        continue;
                    }
                }

                // Check parameter types if available
                boolean matches = true;
                int idx = 0;

                for (Expression param : virtualMethod.getParameters()) {

                    MatchResult result = results.get(param.toOriginalString());
                    if (result != null && !result.isEmpty()) {
                        // Type info available - validate parameter type
                        Type paramType;
                        if (isVarArgs && (idx >= lastParamIdx)) {
                            // Replace the type for varargs methods
                            paramType = evaluatedParams.get(lastParamIdx).type.asArrayType().constituent();
                        } else {
                            paramType = evaluatedParams.get(idx).type;
                        }
                        if (!assignabilityCheck.isAssignableFrom(paramType, result.type)) {
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
            return extensionMethod;
        }
        return null;
    }

    private static AnnotationTarget findProperty(String name, ClassInfo clazz, JavaMemberLookupConfig config) {
        // Attempts to find a property with the specified name
        // i.e. a public non-static non-synthetic field with the given name or a public non-static non-synthetic method with no params and the given name
        Set<DotName> interfaceNames = config.declaredMembersOnly() ? null : new HashSet<>();
        while (clazz != null) {
            if (interfaceNames != null) {
                addInterfaces(clazz, config.index(), interfaceNames);
            }
            // Methods; getters should take precedence over fields
            for (MethodInfo method : clazz.methods()) {
                if (method.returnType().kind() != org.jboss.jandex.Type.Kind.VOID
                        && config.filter().test(method)
                        && (method.name().equals(name)
                                || ValueResolverGenerator.getPropertyName(method.name()).equals(name))) {
                    // Skip void, non-public, static and synthetic methods
                    // Method name must match (exact or getter)
                    return method;
                }
            }
            // Fields
            for (FieldInfo field : clazz.fields()) {
                if (!config.filter().test(field)) {
                    continue;
                }
                if (field.name().equals(name)) {
                    // Name matches and it's either an enum constant or a non-static field
                    return field;
                }
            }
            DotName superName = clazz.superName();
            if (config.declaredMembersOnly() || superName == null) {
                clazz = null;
            } else {
                clazz = config.index().getClassByName(clazz.superName());
            }
        }
        // Try interface methods
        if (interfaceNames != null) {
            for (DotName interfaceName : interfaceNames) {
                ClassInfo interfaceClassInfo = config.index().getClassByName(interfaceName);
                if (interfaceClassInfo != null) {
                    for (MethodInfo method : interfaceClassInfo.methods()) {
                        if (config.filter().test(method)
                                && (method.name().equals(name)
                                        || ValueResolverGenerator.getPropertyName(method.name()).equals(name))) {
                            return method;
                        }
                    }
                }
            }
        }
        // No matching method found
        return null;
    }

    private static void addInterfaces(ClassInfo clazz, IndexView index, Set<DotName> interfaceNames) {
        if (clazz == null) {
            return;
        }
        List<DotName> names = clazz.interfaceNames();
        if (!names.isEmpty()) {
            interfaceNames.addAll(names);
            for (DotName name : names) {
                addInterfaces(index.getClassByName(name), index, interfaceNames);
            }
        }
    }

    private static AnnotationTarget findMethod(VirtualMethodPart virtualMethod, ClassInfo clazz, Expression expression,
            IndexView index, Function<String, String> templateIdToPathFun, Map<String, MatchResult> results,
            JavaMemberLookupConfig config, AssignabilityCheck assignabilityCheck) {
        // Find a method with the given name, matching number of params and assignable parameter types
        Set<DotName> interfaceNames = config.declaredMembersOnly() ? null : new HashSet<>();
        while (clazz != null) {
            if (interfaceNames != null) {
                addInterfaces(clazz, index, interfaceNames);
            }
            for (MethodInfo method : clazz.methods()) {
                if (config.filter().test(method)
                        && methodMatches(method, virtualMethod, expression, index, templateIdToPathFun, results,
                                assignabilityCheck)) {
                    return method;
                }
            }
            DotName superName = clazz.superName();
            if (config.declaredMembersOnly() || superName == null || DotNames.OBJECT.equals(superName)) {
                clazz = null;
            } else {
                clazz = index.getClassByName(clazz.superName());
            }
        }
        // Try interface methods
        if (interfaceNames != null) {
            for (DotName interfaceName : interfaceNames) {
                ClassInfo interfaceClassInfo = index.getClassByName(interfaceName);
                if (interfaceClassInfo != null) {
                    for (MethodInfo method : interfaceClassInfo.methods()) {
                        if (config.filter().test(method)
                                && methodMatches(method, virtualMethod, expression, index, templateIdToPathFun, results,
                                        assignabilityCheck)) {
                            return method;
                        }
                    }
                }
            }
        }
        // No matching method found
        return null;
    }

    private static boolean methodMatches(MethodInfo method, VirtualMethodPart virtualMethod, Expression expression,
            IndexView index, Function<String, String> templateIdToPathFun, Map<String, MatchResult> results,
            AssignabilityCheck assignabilityCheck) {

        if (!method.name().equals(virtualMethod.getName())) {
            return false;
        }

        boolean isVarArgs = ValueResolverGenerator.isVarArgs(method);
        List<Type> parameters = method.parameterTypes();
        int lastParamIdx = parameters.size() - 1;

        if (isVarArgs) {
            // For varargs methods match the minimal number of params
            if (lastParamIdx > virtualMethod.getParameters().size()) {
                return false;
            }
        } else {
            if (virtualMethod.getParameters().size() != parameters.size()) {
                // Number of params must be equal
                return false;
            }
        }

        // Check parameter types if available
        boolean matches = true;
        byte idx = 0;

        for (Expression param : virtualMethod.getParameters()) {
            MatchResult result = results.get(param.toOriginalString());
            if (result != null && !result.isEmpty()) {
                // Type info available - validate parameter type
                Type paramType;
                if (isVarArgs && idx >= lastParamIdx) {
                    // Replace the type for varargs methods
                    paramType = parameters.get(lastParamIdx).asArrayType().constituent();
                } else {
                    paramType = parameters.get(idx);
                }
                if (!assignabilityCheck.isAssignableFrom(paramType, result.type)) {
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
        return matches;
    }

    private void processTemplateData(TemplateDataBuildItem templateData,
            Set<DotName> controlled, Map<DotName, AnnotationInstance> uncontrolled, ValueResolverGenerator.Builder builder) {
        DotName targetClassName = templateData.getTargetClass().name();
        if (templateData.isTargetAnnotatedType()) {
            controlled.add(targetClassName);
            builder.addClass(templateData.getTargetClass(), templateData.getAnnotationInstance());
        } else {
            // At this point we can be sure that multiple unequal @TemplateData do not exist for a specific target
            uncontrolled.computeIfAbsent(targetClassName, name -> {
                builder.addClass(templateData.getTargetClass(), templateData.getAnnotationInstance());
                return templateData.getAnnotationInstance();
            });
        }
    }

    @BuildStep
    void collectTemplateGlobals(BeanArchiveIndexBuildItem beanArchiveIndex, BuildProducer<TemplateGlobalBuildItem> globals) {
        IndexView index = beanArchiveIndex.getIndex();
        Map<String, TemplateGlobalBuildItem> nameToGlobal = new HashMap<>();
        for (AnnotationInstance annotation : index.getAnnotations(TemplateGlobalGenerator.TEMPLATE_GLOBAL)) {
            switch (annotation.target().kind()) {
                case CLASS:
                    addGlobalClass(annotation.target().asClass(), nameToGlobal);
                    break;
                case FIELD:
                    addGlobalField(annotation.value(TemplateGlobalGenerator.NAME), annotation.target().asField(), nameToGlobal);
                    break;
                case METHOD:
                    addGlobalMethod(annotation.value(TemplateGlobalGenerator.NAME), annotation.target().asMethod(),
                            nameToGlobal);
                    break;
                default:
                    throw new TemplateException("Invalid annotation target for @TemplateGlobal: " + annotation);
            }
        }
        nameToGlobal.values().forEach(globals::produce);
    }

    private void addGlobalClass(ClassInfo clazz, Map<String, TemplateGlobalBuildItem> nameToGlobal) {
        for (FieldInfo field : clazz.fields()) {
            if (Modifier.isStatic(field.flags())
                    && !Modifier.isPrivate(field.flags())
                    && !field.isSynthetic()
                    && !field.hasAnnotation(TemplateGlobalGenerator.TEMPLATE_GLOBAL)) {
                addGlobalField(null, field, nameToGlobal);
            }
        }
        for (MethodInfo method : clazz.methods()) {
            if (Modifier.isStatic(method.flags())
                    && !Modifier.isPrivate(method.flags())
                    && method.returnType().kind() != org.jboss.jandex.Type.Kind.VOID
                    && !method.isSynthetic()
                    && !method.hasAnnotation(TemplateGlobalGenerator.TEMPLATE_GLOBAL)) {
                addGlobalMethod(null, method, nameToGlobal);
            }
        }
    }

    private void addGlobalMethod(AnnotationValue nameValue, MethodInfo method,
            Map<String, TemplateGlobalBuildItem> nameToGlobal) {
        TemplateGlobalGenerator.validate(method);
        String name = TemplateGlobal.ELEMENT_NAME;
        if (nameValue != null) {
            name = nameValue.asString();
        }
        if (name.equals(TemplateGlobal.ELEMENT_NAME)) {
            name = method.name();
        }
        TemplateGlobalBuildItem global = new TemplateGlobalBuildItem(name, method, method.returnType());
        addGlobalVariable(global, nameToGlobal);
    }

    private void addGlobalField(AnnotationValue nameValue, FieldInfo field, Map<String, TemplateGlobalBuildItem> nameToGlobal) {
        TemplateGlobalGenerator.validate(field);
        String name = TemplateGlobal.ELEMENT_NAME;
        if (nameValue != null) {
            name = nameValue.asString();
        }
        if (name.equals(TemplateGlobal.ELEMENT_NAME)) {
            name = field.name();
        }
        TemplateGlobalBuildItem global = new TemplateGlobalBuildItem(name, field, field.type());
        addGlobalVariable(global, nameToGlobal);
    }

    private void addGlobalVariable(TemplateGlobalBuildItem global, Map<String, TemplateGlobalBuildItem> nameToGlobal) {
        TemplateGlobalBuildItem prev = nameToGlobal.put(global.getName(), global);
        if (prev != null) {
            throw new TemplateException(
                    String.format("Duplicate global variable defined via @TemplateGlobal for the name [%s]:\n\t- %s\n\t- %s",
                            global.getName(), global, prev));
        }
    }

    @BuildStep
    void collectTemplateDataAnnotations(BeanArchiveIndexBuildItem beanArchiveIndex,
            BuildProducer<TemplateDataBuildItem> templateDataAnnotations) {
        IndexView index = beanArchiveIndex.getIndex();
        Set<AnnotationInstance> annotationInstances = new HashSet<>();
        annotationInstances.addAll(index.getAnnotations(ValueResolverGenerator.TEMPLATE_DATA));
        for (AnnotationInstance containingInstance : index.getAnnotations(ValueResolverGenerator.TEMPLATE_DATA_CONTAINER)) {
            for (AnnotationInstance nestedInstance : containingInstance.value().asNestedArray()) {
                // We need to use the target of the containing instance
                annotationInstances.add(
                        AnnotationInstance.create(nestedInstance.name(), containingInstance.target(), nestedInstance.values()));
            }
        }

        Map<DotName, AnnotationInstance> uncontrolled = new HashMap<>();

        for (AnnotationInstance templateData : annotationInstances) {
            AnnotationValue targetValue = templateData.value(ValueResolverGenerator.TARGET);
            ClassInfo targetClass = null;
            if (targetValue == null || targetValue.asClass().name().equals(ValueResolverGenerator.TEMPLATE_DATA)) {
                targetClass = templateData.target().asClass();
            } else {
                targetClass = index.getClassByName(targetValue.asClass().name());
            }
            if (targetClass == null) {
                LOGGER.warnf("@TemplateData declared on %s is ignored: target %s it is not available in the index",
                        templateData.target(), targetClass);
                continue;
            }
            uncontrolled.compute(targetClass.name(), (c, v) -> {
                if (v == null) {
                    return templateData;
                }
                if (!Objects.equals(v.value(ValueResolverGenerator.IGNORE),
                        templateData.value(ValueResolverGenerator.IGNORE))
                        || !Objects.equals(v.value(ValueResolverGenerator.PROPERTIES),
                                templateData.value(ValueResolverGenerator.PROPERTIES))
                        || !Objects.equals(v.value(ValueResolverGenerator.IGNORE_SUPERCLASSES),
                                templateData.value(ValueResolverGenerator.IGNORE_SUPERCLASSES))
                        || !Objects.equals(v.value(ValueResolverGenerator.NAMESPACE),
                                templateData.value(ValueResolverGenerator.NAMESPACE))) {
                    throw new IllegalStateException(
                            "Multiple unequal @TemplateData declared for " + c + ": " + v + " and " + templateData);
                }
                return v;
            });
            templateDataAnnotations.produce(new TemplateDataBuildItem(templateData, targetClass));
        }

        // Add synthetic @TemplateData for template enums
        for (AnnotationInstance templateEnum : index.getAnnotations(Names.TEMPLATE_ENUM)) {
            ClassInfo targetEnum = templateEnum.target().asClass();
            if (!targetEnum.isEnum()) {
                LOGGER.warnf("@TemplateEnum declared on %s is ignored: the target of this annotation must be an enum type",
                        targetEnum);
                continue;
            }
            if (targetEnum.declaredAnnotation(ValueResolverGenerator.TEMPLATE_DATA) != null) {
                LOGGER.debugf("@TemplateEnum declared on %s is ignored: enum is annotated with @TemplateData", targetEnum);
                continue;
            }
            AnnotationInstance uncontrolledDeclaration = uncontrolled.get(targetEnum.name());
            if (uncontrolledDeclaration != null) {
                LOGGER.debugf("@TemplateEnum declared on %s is ignored: %s declared on %s", targetEnum, uncontrolledDeclaration,
                        uncontrolledDeclaration.target());
                continue;
            }
            templateDataAnnotations.produce(new TemplateDataBuildItem(
                    new TemplateDataBuilder().annotationTarget(templateEnum.target()).namespace(TemplateData.SIMPLENAME)
                            .build(),
                    targetEnum));
        }

    }

    @BuildStep
    void validateTemplateDataNamespaces(List<TemplateDataBuildItem> templateData,
            BuildProducer<ServiceStartBuildItem> serviceStart) {

        Map<String, List<TemplateDataBuildItem>> namespaceToData = templateData.stream()
                .filter(TemplateDataBuildItem::hasNamespace)
                .collect(Collectors.groupingBy(TemplateDataBuildItem::getNamespace));
        for (Map.Entry<String, List<TemplateDataBuildItem>> e : namespaceToData.entrySet()) {
            if (e.getValue().size() > 1) {
                throw new TemplateException(
                        String.format(
                                "The namespace [%s] is defined by multiple @TemplateData and/or @TemplateEnum annotations; make sure the annotation declared on the following classes do not collide:\n\t- %s",
                                e.getKey(), e.getValue()
                                        .stream().map(TemplateDataBuildItem::getAnnotationInstance)
                                        .map(AnnotationInstance::target).map(Object::toString)
                                        .collect(Collectors.joining("\n\t- "))));
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
        if (namespace.equals(expression.getNamespace())) {
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
            Path originalPath, QuteConfig config) {
        if (filePath.isEmpty()) {
            return;
        }
        String fullPath = basePath + filePath;
        LOGGER.debugf("Produce template build items [filePath: %s, fullPath: %s, originalPath: %s", filePath, fullPath,
                originalPath);
        // NOTE: we cannot just drop the template because a template param can be added
        watchedPaths.produce(new HotDeploymentWatchedFileBuildItem(fullPath, true));
        nativeImageResources.produce(new NativeImageResourceBuildItem(fullPath));
        templatePaths.produce(
                new TemplatePathBuildItem(filePath, originalPath, readTemplateContent(originalPath, config.defaultCharset)));
    }

    private void scan(Path root, Path directory, String basePath, BuildProducer<HotDeploymentWatchedFileBuildItem> watchedPaths,
            BuildProducer<TemplatePathBuildItem> templatePaths,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            QuteConfig config)
            throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            Iterator<Path> iter = files.iterator();
            while (iter.hasNext()) {
                Path filePath = iter.next();
                /*
                 * Fix for https://github.com/quarkusio/quarkus/issues/25751 where running tests in Eclipse
                 * sometimes produces `/templates/tags` (absolute) files listed for `templates` (relative)
                 * directories, so we work around this
                 */
                if (!directory.isAbsolute()
                        && filePath.isAbsolute()
                        && filePath.getRoot() != null) {
                    filePath = filePath.getRoot().relativize(filePath);
                }
                if (Files.isRegularFile(filePath)) {
                    LOGGER.debugf("Found template: %s", filePath);
                    String templatePath = root.relativize(filePath).toString();
                    if (File.separatorChar != '/') {
                        templatePath = templatePath.replace(File.separatorChar, '/');
                    }
                    if (config.templatePathExclude.matcher(templatePath).matches()) {
                        LOGGER.debugf("Template file excluded: %s", filePath);
                        continue;
                    }
                    produceTemplateBuildItems(templatePaths, watchedPaths, nativeImageResources, basePath, templatePath,
                            filePath, config);
                } else if (Files.isDirectory(filePath)) {
                    LOGGER.debugf("Scan directory: %s", filePath);
                    scan(root, filePath, basePath, watchedPaths, templatePaths, nativeImageResources, config);
                }
            }
        }
    }

    private static boolean isExcluded(TypeCheck check, Iterable<Predicate<TypeCheck>> excludes) {
        for (Predicate<TypeCheck> exclude : excludes) {
            if (exclude.test(check)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBasePath(Path path) {
        return path.getFileName().toString().equals(BASE_PATH);
    }

    private void checkDuplicatePaths(List<TemplatePathBuildItem> templatePaths) {
        Map<String, List<TemplatePathBuildItem>> duplicates = templatePaths.stream()
                .collect(Collectors.groupingBy(TemplatePathBuildItem::getPath));
        for (Iterator<List<TemplatePathBuildItem>> it = duplicates.values().iterator(); it.hasNext();) {
            List<TemplatePathBuildItem> paths = it.next();
            if (paths.isEmpty() || paths.size() == 1) {
                it.remove();
            }
        }
        if (!duplicates.isEmpty()) {
            StringBuilder builder = new StringBuilder("Duplicate templates found:");
            for (Entry<String, List<TemplatePathBuildItem>> e : duplicates.entrySet()) {
                builder.append("\n\t- ").append(e.getKey()).append(": ")
                        .append(e.getValue().stream().map(TemplatePathBuildItem::getFullPath).collect(Collectors.toList()));
            }
            throw new IllegalStateException(builder.toString());
        }
    }

    private boolean isApplicationArchive(ResolvedDependency dependency, Set<ApplicationArchive> applicationArchives) {
        for (ApplicationArchive archive : applicationArchives) {
            if (archive.getKey() == null) {
                continue;
            }
            if (dependency.getGroupId().equals(archive.getKey().getGroupId())
                    && dependency.getArtifactId().equals(archive.getKey().getArtifactId())) {
                return true;
            }
        }
        return false;
    }

    static String readTemplateContent(Path path, Charset defaultCharset) {
        try {
            return Files.readString(path, defaultCharset);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read the template content from path: " + path, e);
        }
    }

    /**
     * Java members lookup config.
     *
     * @see QuteProcessor#findProperty(String, ClassInfo, JavaMemberLookupConfig)
     * @see QuteProcessor#findMethod(VirtualMethodPart, ClassInfo, Expression, IndexView, Function, Map, JavaMemberLookupConfig)
     */
    interface JavaMemberLookupConfig {

        IndexView index();

        Predicate<AnnotationTarget> filter();

        boolean declaredMembersOnly();

        default void nextPart() {
        }

    }

    static class FixedJavaMemberLookupConfig implements JavaMemberLookupConfig {

        private final IndexView index;
        private final Predicate<AnnotationTarget> filter;
        private final boolean declaredMembersOnly;

        FixedJavaMemberLookupConfig(IndexView index, Predicate<AnnotationTarget> filter, boolean declaredMembersOnly) {
            this.index = index;
            this.filter = filter;
            this.declaredMembersOnly = declaredMembersOnly;
        }

        @Override
        public IndexView index() {
            return index;
        }

        @Override
        public Predicate<AnnotationTarget> filter() {
            return filter;
        }

        @Override
        public boolean declaredMembersOnly() {
            return declaredMembersOnly;
        }
    }

    static class FirstPassJavaMemberLookupConfig implements JavaMemberLookupConfig {

        private final JavaMemberLookupConfig next;
        // used for the first part
        private Predicate<AnnotationTarget> filter;
        private Boolean declaredMembersOnly;

        FirstPassJavaMemberLookupConfig(JavaMemberLookupConfig next, Predicate<AnnotationTarget> filter,
                Boolean declaredMembersOnly) {
            this.next = next;
            this.filter = filter;
            this.declaredMembersOnly = declaredMembersOnly;
        }

        @Override
        public IndexView index() {
            return next.index();
        }

        @Override
        public Predicate<AnnotationTarget> filter() {
            return filter != null ? filter : next.filter();
        }

        @Override
        public boolean declaredMembersOnly() {
            return declaredMembersOnly != null ? declaredMembersOnly : next.declaredMembersOnly();
        }

        @Override
        public void nextPart() {
            filter = null;
            declaredMembersOnly = null;
        }

    }

    enum Code implements ErrorCode {

        INCORRECT_EXPRESSION,
        ;

        @Override
        public String getName() {
            return "BUILD_" + name();
        }

    }

    private static final class MethodParameterDeclaration implements ParameterDeclaration {

        private final String paramType;
        private final String paramName;

        private MethodParameterDeclaration(String paramType, String paramName) {
            this.paramType = paramType;
            this.paramName = paramName;
        }

        public String getParamType() {
            return paramType;
        }

        @Override
        public String getTypeInfo() {
            return Expressions.typeInfoFrom(paramType);
        }

        @Override
        public String getKey() {
            return paramName;
        }

        @Override
        public Expression getDefaultValue() {
            return null;
        }

        @Override
        public TemplateNode.Origin getOrigin() {
            return null;
        }
    }

}
