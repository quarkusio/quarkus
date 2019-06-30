package io.quarkus.annotation.processor;

import static io.quarkus.annotation.processor.StringUtil.join;
import static io.quarkus.annotation.processor.StringUtil.lowerCase;
import static io.quarkus.annotation.processor.StringUtil.withoutSuffix;
import static javax.lang.model.util.ElementFilter.constructorsIn;
import static javax.lang.model.util.ElementFilter.fieldsIn;
import static javax.lang.model.util.ElementFilter.methodsIn;
import static javax.lang.model.util.ElementFilter.typesIn;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Completion;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import org.jboss.jdeparser.FormatPreferences;
import org.jboss.jdeparser.JAssignableExpr;
import org.jboss.jdeparser.JCall;
import org.jboss.jdeparser.JClassDef;
import org.jboss.jdeparser.JDeparser;
import org.jboss.jdeparser.JExprs;
import org.jboss.jdeparser.JFiler;
import org.jboss.jdeparser.JMethodDef;
import org.jboss.jdeparser.JMod;
import org.jboss.jdeparser.JSourceFile;
import org.jboss.jdeparser.JSources;
import org.jboss.jdeparser.JType;
import org.jboss.jdeparser.JTypes;
import org.springframework.boot.configurationprocessor.MetadataCollector;
import org.springframework.boot.configurationprocessor.MetadataStore;
import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata;

public class ExtensionAnnotationProcessor extends AbstractProcessor {

    private static final String ANNOTATION_BUILD_STEP = "io.quarkus.deployment.annotations.BuildStep";
    private static final String ANNOTATION_CONFIG_GROUP = "io.quarkus.runtime.annotations.ConfigGroup";
    private static final String ANNOTATION_CONFIG_ITEM = "io.quarkus.runtime.annotations.ConfigItem";
    private static final String ANNOTATION_CONFIG_ROOT = "io.quarkus.runtime.annotations.ConfigRoot";
    private static final String ANNOTATION_TEMPLATE = "io.quarkus.runtime.annotations.Template";
    private static final String INSTANCE_SYM = "__instance";

    private final Set<String> generatedAccessors = new ConcurrentHashMap<String, Boolean>().keySet(Boolean.TRUE);
    private final Set<String> generatedJavaDocs = new ConcurrentHashMap<String, Boolean>().keySet(Boolean.TRUE);

    private MetadataStore metadataStore;

    private MetadataCollector metadataCollector;

    private Types typeUtils;

    public ExtensionAnnotationProcessor() {
    }

    @Override
    public Set<String> getSupportedOptions() {
        return Collections.emptySet();
    }

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        this.metadataStore = new MetadataStore(env);
        this.metadataCollector = new MetadataCollector(env, this.metadataStore.readMetadata());
        this.typeUtils = env.getTypeUtils();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> ret = new HashSet<>();
        ret.add(ANNOTATION_BUILD_STEP);
        ret.add(ANNOTATION_CONFIG_GROUP);
        ret.add(ANNOTATION_CONFIG_ROOT);
        ret.add(ANNOTATION_TEMPLATE);
        return ret;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        doProcess(annotations, roundEnv);
        if (roundEnv.processingOver()) {
            doFinish();
        }
        return true;
    }

    @Override
    public Iterable<? extends Completion> getCompletions(Element element, AnnotationMirror annotation, ExecutableElement member,
            String userText) {
        return Collections.emptySet();
    }

    public void doProcess(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        this.metadataCollector.processing(roundEnv);
        for (TypeElement annotation : annotations) {
            switch (annotation.getQualifiedName().toString()) {
                case ANNOTATION_BUILD_STEP:
                    processBuildStep(roundEnv, annotation);
                    break;
                case ANNOTATION_CONFIG_GROUP:
                    processConfigGroup(roundEnv, annotation);
                    break;
                case ANNOTATION_CONFIG_ROOT:
                    processConfigRoot(roundEnv, annotation);
                    break;
                case ANNOTATION_TEMPLATE:
                    processTemplate(roundEnv, annotation);
                    break;
            }
        }
    }

    void doFinish() {
        final Filer filer = processingEnv.getFiler();
        final FileObject tempResource;
        try {
            tempResource = filer.createResource(StandardLocation.SOURCE_OUTPUT, "", "ignore.tmp");
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Unable to create temp output file: " + e);
            return;
        }
        final URI uri = tempResource.toUri();
        // tempResource.delete();
        Path path;
        try {
            path = Paths.get(uri).getParent();
        } catch (RuntimeException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Resource path URI is invalid: " + uri);
            return;
        }
        Collection<String> bscListClasses = new TreeSet<>();
        Collection<String> crListClasses = new TreeSet<>();
        Properties javaDocProperties = new Properties();
        try {
            Files.walkFileTree(path, new FileVisitor<Path>() {
                public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) {
                    return FileVisitResult.CONTINUE;
                }

                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
                    final String nameStr = file.getFileName().toString();
                    if (nameStr.endsWith(".bsc")) {
                        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                            String line;
                            while ((line = br.readLine()) != null) {
                                line = line.trim();
                                if (!line.isEmpty()) {
                                    bscListClasses.add(line);
                                }
                            }
                        } catch (IOException e) {
                            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                    "Failed to read file " + file + ": " + e);
                        }
                    } else if (nameStr.endsWith(".cr")) {
                        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                            String line;
                            while ((line = br.readLine()) != null) {
                                line = line.trim();
                                if (!line.isEmpty()) {
                                    crListClasses.add(line);
                                }
                            }
                        } catch (IOException e) {
                            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                    "Failed to read file " + file + ": " + e);
                        }
                    } else if (nameStr.endsWith(".jdp")) {
                        final Properties p = new Properties();
                        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                            p.load(br);
                        } catch (IOException e) {
                            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                    "Failed to read file " + file + ": " + e);
                        }
                        final Set<String> names = p.stringPropertyNames();
                        for (String name : names) {
                            javaDocProperties.setProperty(name, p.getProperty(name));
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                public FileVisitResult visitFileFailed(final Path file, final IOException exc) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "Failed to visit file " + file + ": " + exc);
                    return FileVisitResult.CONTINUE;
                }

                public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "File walk failed: " + e);
        }
        if (!bscListClasses.isEmpty())
            try {
                final FileObject listResource = filer.createResource(StandardLocation.CLASS_OUTPUT, "",
                        "META-INF/quarkus-build-steps.list");
                try (OutputStream os = listResource.openOutputStream()) {
                    try (BufferedOutputStream bos = new BufferedOutputStream(os)) {
                        try (OutputStreamWriter osw = new OutputStreamWriter(bos, StandardCharsets.UTF_8)) {
                            try (BufferedWriter bw = new BufferedWriter(osw)) {
                                for (String item : bscListClasses) {
                                    bw.write(item);
                                    bw.newLine();
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to write build steps listing: " + e);
                return;
            }
        if (!crListClasses.isEmpty())
            try {
                final FileObject listResource = filer.createResource(StandardLocation.CLASS_OUTPUT, "",
                        "META-INF/quarkus-config-roots.list");
                try (OutputStream os = listResource.openOutputStream()) {
                    try (BufferedOutputStream bos = new BufferedOutputStream(os)) {
                        try (OutputStreamWriter osw = new OutputStreamWriter(bos, StandardCharsets.UTF_8)) {
                            try (BufferedWriter bw = new BufferedWriter(osw)) {
                                for (String item : crListClasses) {
                                    bw.write(item);
                                    bw.newLine();
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to write config roots listing: " + e);
                return;
            }
        try {
            final FileObject listResource = filer.createResource(StandardLocation.CLASS_OUTPUT, "",
                    "META-INF/quarkus-javadoc.properties");
            try (OutputStream os = listResource.openOutputStream()) {
                try (BufferedOutputStream bos = new BufferedOutputStream(os)) {
                    try (OutputStreamWriter osw = new OutputStreamWriter(bos, StandardCharsets.UTF_8)) {
                        try (BufferedWriter bw = new BufferedWriter(osw)) {
                            javaDocProperties.store(bw, "");
                        }
                    }
                }
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to write javadoc properties: " + e);
            return;
        }
        try {
            ConfigurationMetadata metadata = this.metadataCollector.getMetadata();
            if (!metadata.getItems().isEmpty()) {
                this.metadataStore.writeMetadata(metadata);
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Failed to write spring-boot configuration metadata: " + e);
            return;
        }
    }

    private void processBuildStep(RoundEnvironment roundEnv, TypeElement annotation) {
        final Set<String> processorClassNames = new HashSet<>();

        for (ExecutableElement i : methodsIn(roundEnv.getElementsAnnotatedWith(annotation))) {
            final TypeElement clazz = getClassOf(i);
            if (clazz == null) {
                continue;
            }
            final PackageElement pkg = processingEnv.getElementUtils().getPackageOf(clazz);
            if (pkg == null) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "Element " + clazz + " has no enclosing package");
                continue;
            }
            final String binaryName = processingEnv.getElementUtils().getBinaryName(clazz).toString();
            if (processorClassNames.add(binaryName)) {
                // new class
                recordConfigJavadoc(clazz);
                generateAccessor(clazz);
                final StringBuilder rbn = getRelativeBinaryName(clazz, new StringBuilder());
                try {
                    final FileObject itemResource = processingEnv.getFiler().createResource(
                            StandardLocation.SOURCE_OUTPUT,
                            pkg.getQualifiedName().toString(),
                            rbn.toString() + ".bsc",
                            clazz);
                    try (OutputStream os = itemResource.openOutputStream()) {
                        try (BufferedOutputStream bos = new BufferedOutputStream(os)) {
                            try (OutputStreamWriter osw = new OutputStreamWriter(bos, StandardCharsets.UTF_8)) {
                                try (BufferedWriter bw = new BufferedWriter(osw)) {
                                    bw.write(binaryName);
                                    bw.newLine();
                                }
                            }
                        }
                    }
                } catch (IOException e1) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "Failed to create " + rbn + " in " + pkg + ": " + e1, clazz);
                }
            }
        }
    }

    private StringBuilder getRelativeBinaryName(TypeElement te, StringBuilder b) {
        final Element enclosing = te.getEnclosingElement();
        if (enclosing instanceof TypeElement) {
            getRelativeBinaryName((TypeElement) enclosing, b);
            b.append('$');
        }
        b.append(te.getSimpleName());
        return b;
    }

    private TypeElement getClassOf(Element e) {
        Element t = e;
        while (!(t instanceof TypeElement)) {
            if (t == null) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Element " + e + " has no enclosing class");
                return null;
            }
            t = t.getEnclosingElement();
        }
        return (TypeElement) t;
    }

    private void recordConfigJavadoc(TypeElement clazz) {
        String className = clazz.getQualifiedName().toString();
        if (!generatedJavaDocs.add(className))
            return;
        final Properties javadocProps = new Properties();
        for (Element e : clazz.getEnclosedElements()) {
            switch (e.getKind()) {
                case FIELD: {
                    if (isAnnotationPresent(e, ANNOTATION_CONFIG_ITEM)) {
                        processFieldConfigItem((VariableElement) e, javadocProps, className);
                    }
                    break;
                }
                case CONSTRUCTOR: {
                    final ExecutableElement ex = (ExecutableElement) e;
                    if (hasParameterAnnotated(ex, ANNOTATION_CONFIG_ITEM)) {
                        processCtorConfigItem(ex, javadocProps, className);
                    }
                    break;
                }
                case METHOD: {
                    final ExecutableElement ex = (ExecutableElement) e;
                    if (hasParameterAnnotated(ex, ANNOTATION_CONFIG_ITEM)) {
                        processMethodConfigItem(ex, javadocProps, className);
                    }
                    break;
                }
                default:
            }
        }
        if (javadocProps.isEmpty())
            return;
        final PackageElement pkg = processingEnv.getElementUtils().getPackageOf(clazz);
        final String rbn = getRelativeBinaryName(clazz, new StringBuilder()).append(".jdp").toString();
        try {
            FileObject file = processingEnv.getFiler().createResource(
                    StandardLocation.SOURCE_OUTPUT,
                    pkg.getQualifiedName().toString(),
                    rbn,
                    clazz);
            try (Writer writer = file.openWriter()) {
                javadocProps.store(writer, "");
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to persist resource " + rbn + ": " + e);
        }
    }

    private void processFieldConfigItem(VariableElement field, Properties javadocProps, String className) {
        javadocProps.put(className + "." + field.getSimpleName().toString(), getRequiredJavadoc(field));
    }

    private void processCtorConfigItem(ExecutableElement ctor, Properties javadocProps, String className) {
        final String docComment = getRequiredJavadoc(ctor);
        final StringBuilder buf = new StringBuilder();
        appendParamTypes(ctor, buf);
        javadocProps.put(className + "." + buf.toString(), docComment);
    }

    private void processMethodConfigItem(ExecutableElement method, Properties javadocProps, String className) {
        final String docComment = getRequiredJavadoc(method);
        final StringBuilder buf = new StringBuilder();
        buf.append(method.getSimpleName().toString());
        appendParamTypes(method, buf);
        javadocProps.put(className + "." + buf.toString(), docComment);
    }

    private void processConfigGroup(RoundEnvironment roundEnv, TypeElement annotation) {
        final Set<String> groupClassNames = new HashSet<>();
        for (TypeElement i : typesIn(roundEnv.getElementsAnnotatedWith(annotation))) {
            if (groupClassNames.add(i.getQualifiedName().toString())) {
                generateAccessor(i);
                recordConfigJavadoc(i);
            }
        }
    }

    private void processConfigRoot(RoundEnvironment roundEnv, TypeElement annotation) {
        final Set<String> rootClassNames = new HashSet<>();

        for (TypeElement clazz : typesIn(roundEnv.getElementsAnnotatedWith(annotation))) {
            final PackageElement pkg = processingEnv.getElementUtils().getPackageOf(clazz);
            if (pkg == null) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "Element " + clazz + " has no enclosing package");
                continue;
            }
            final String binaryName = processingEnv.getElementUtils().getBinaryName(clazz).toString();
            if (rootClassNames.add(binaryName)) {
                // new class
                recordConfigJavadoc(clazz);
                recordConfigMetaData(buildConfigRootPrefix(clazz), clazz);
                generateAccessor(clazz);
                final StringBuilder rbn = getRelativeBinaryName(clazz, new StringBuilder());
                try {
                    final FileObject itemResource = processingEnv.getFiler().createResource(
                            StandardLocation.SOURCE_OUTPUT,
                            pkg.getQualifiedName().toString(),
                            rbn.toString() + ".cr",
                            clazz);
                    try (OutputStream os = itemResource.openOutputStream()) {
                        try (BufferedOutputStream bos = new BufferedOutputStream(os)) {
                            try (OutputStreamWriter osw = new OutputStreamWriter(bos, StandardCharsets.UTF_8)) {
                                try (BufferedWriter bw = new BufferedWriter(osw)) {
                                    bw.write(binaryName);
                                    bw.newLine();
                                }
                            }
                        }
                    }
                } catch (IOException e1) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "Failed to create " + rbn + " in " + pkg + ": " + e1, clazz);
                }
            }
        }
    }

    private void processTemplate(RoundEnvironment roundEnv, TypeElement annotation) {
        final Set<String> groupClassNames = new HashSet<>();
        for (TypeElement i : typesIn(roundEnv.getElementsAnnotatedWith(annotation))) {
            if (groupClassNames.add(i.getQualifiedName().toString())) {
                generateAccessor(i);
                recordConfigJavadoc(i);
            }
        }
    }

    private void generateAccessor(final TypeElement clazz) {
        if (!generatedAccessors.add(clazz.getQualifiedName().toString()))
            return;
        final FormatPreferences fp = new FormatPreferences();
        final JSources sources = JDeparser.createSources(JFiler.newInstance(processingEnv.getFiler()), fp);
        final PackageElement packageElement = processingEnv.getElementUtils().getPackageOf(clazz);
        final String className = getRelativeBinaryName(clazz, new StringBuilder()).append("$$accessor").toString();
        final JSourceFile sourceFile = sources.createSourceFile(packageElement.getQualifiedName().toString(), className);
        JType clazzType = JTypes.typeOf(clazz.asType());
        if (clazz.asType() instanceof DeclaredType) {
            DeclaredType declaredType = ((DeclaredType) clazz.asType());
            TypeMirror enclosingType = declaredType.getEnclosingType();
            if (enclosingType != null && enclosingType.getKind() == TypeKind.DECLARED
                    && clazz.getModifiers().contains(Modifier.STATIC)) {
                // Ugly workaround for Eclipse APT and static nested types
                clazzType = unnestStaticNestedType(declaredType);
            }
        }
        final JClassDef classDef = sourceFile._class(JMod.PUBLIC | JMod.FINAL, className);
        classDef.constructor(JMod.PRIVATE); // no construction
        final JAssignableExpr instanceName = JExprs.name(INSTANCE_SYM);
        // iterate fields
        for (VariableElement field : fieldsIn(clazz.getEnclosedElements())) {
            final Set<Modifier> mods = field.getModifiers();
            if (mods.contains(Modifier.PRIVATE) || mods.contains(Modifier.STATIC)) {
                // skip it
                continue;
            }
            final TypeMirror fieldType = field.asType();
            final JType realType = JTypes.typeOf(fieldType);
            final JType publicType = fieldType instanceof PrimitiveType ? realType : JType.OBJECT;

            final String fieldName = field.getSimpleName().toString();
            final JMethodDef getter = classDef.method(JMod.PUBLIC | JMod.STATIC, publicType, "get_" + fieldName);
            getter.annotate(SuppressWarnings.class).value("unchecked");
            getter.param(JType.OBJECT, INSTANCE_SYM);
            getter.body()._return(instanceName.cast(clazzType).field(fieldName));
            final JMethodDef setter = classDef.method(JMod.PUBLIC | JMod.STATIC, JType.VOID, "set_" + fieldName);
            setter.annotate(SuppressWarnings.class).value("unchecked");
            setter.param(JType.OBJECT, INSTANCE_SYM);
            setter.param(publicType, fieldName);
            final JAssignableExpr fieldExpr = JExprs.name(fieldName);
            setter.body().assign(instanceName.cast(clazzType).field(fieldName),
                    (publicType.equals(realType) ? fieldExpr : fieldExpr.cast(realType)));
        }
        // iterate constructors
        for (ExecutableElement ctor : constructorsIn(clazz.getEnclosedElements())) {
            if (ctor.getModifiers().contains(Modifier.PRIVATE)) {
                // skip it
                continue;
            }
            StringBuilder b = new StringBuilder();
            for (VariableElement parameter : ctor.getParameters()) {
                b.append('_');
                b.append(parameter.asType().toString().replace('.', '_'));
            }
            String codedName = b.toString();
            final JMethodDef ctorMethod = classDef.method(JMod.PUBLIC | JMod.STATIC, JType.OBJECT, "construct" + codedName);
            final JCall ctorCall = clazzType._new();
            for (VariableElement parameter : ctor.getParameters()) {
                final TypeMirror paramType = parameter.asType();
                final JType realType = JTypes.typeOf(paramType);
                final JType publicType = paramType instanceof PrimitiveType ? realType : JType.OBJECT;
                final String name = parameter.getSimpleName().toString();
                ctorMethod.param(publicType, name);
                final JAssignableExpr nameExpr = JExprs.name(name);
                ctorCall.arg(publicType.equals(realType) ? nameExpr : nameExpr.cast(realType));
            }
            ctorMethod.body()._return(ctorCall);
        }
        try {
            sources.writeSources();
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to generate source file: " + e, clazz);
        }
    }

    private JType unnestStaticNestedType(DeclaredType declaredType) {
        final TypeElement typeElement = (TypeElement) declaredType.asElement();

        final String name = typeElement.getQualifiedName().toString();
        final JType rawType = JTypes.typeNamed(name);
        final List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
        if (typeArguments.isEmpty()) {
            return rawType;
        }
        JType[] args = new JType[typeArguments.size()];
        for (int i = 0; i < typeArguments.size(); i++) {
            final TypeMirror argument = typeArguments.get(i);
            args[i] = JTypes.typeOf(argument);
        }
        return rawType.typeArg(args);
    }

    private void appendParamTypes(ExecutableElement ex, final StringBuilder buf) {
        final List<? extends VariableElement> params = ex.getParameters();
        if (params.isEmpty()) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Expected at least one parameter", ex);
            return;
        }
        VariableElement param = params.get(0);
        DeclaredType dt = (DeclaredType) param.asType();
        String typeName = processingEnv.getElementUtils().getBinaryName(((TypeElement) dt.asElement())).toString();
        buf.append('(').append(typeName);
        for (int i = 1; i < params.size(); ++i) {
            param = params.get(i);
            dt = (DeclaredType) param.asType();
            typeName = processingEnv.getElementUtils().getBinaryName(((TypeElement) dt.asElement())).toString();
            buf.append(',').append(typeName);
        }
        buf.append(')');
    }

    private String getRequiredJavadoc(Element e) {
        final String docComment = processingEnv.getElementUtils().getDocComment(e);
        if (docComment == null) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Unable to find javadoc for config item " + e, e);
            return "";
        }
        return docComment.trim();
    }

    /**
     * Records the meta-data of ConfigItem elements and recursively builds the prefix of ConfigGroup elements.
     * <p>
     * This method is called from each ConfigRoot.
     * <p>
     * Note: no properties are generated for collections and maps (this is not supported by spring-boot cf see
     * https://github.com/spring-projects/spring-boot/issues/9945)
     */
    private void recordConfigMetaData(String prefix, TypeElement clazz) {
        this.metadataCollector
                .add(ItemMetadata.newGroup(prefix, clazz.asType().toString(), clazz.asType().toString(),
                        null));
        for (Element e : clazz.getEnclosedElements()) {
            if (e.getKind() == ElementKind.FIELD) {
                boolean isConfigGroup = false;
                if (e.asType() instanceof DeclaredType) {
                    TypeElement configGroupType = (TypeElement) ((DeclaredType) e.asType()).asElement();
                    // Unwrap Optional<T>
                    if (configGroupType.toString().equals("java.util.Optional")) {
                        configGroupType = (TypeElement) typeUtils
                                .asElement(((DeclaredType) e.asType()).getTypeArguments().get(0));
                    }
                    if (isAnnotationPresent(configGroupType, ANNOTATION_CONFIG_GROUP)) {
                        isConfigGroup = true;
                        recordConfigMetaData(prefix + "." + buildConfigItemName(e), configGroupType);
                    }
                }
                if (isAnnotationPresent(e, ANNOTATION_CONFIG_ITEM)) {
                    String defaultValue = getAnnotationParameterStringValue(e, ANNOTATION_CONFIG_ITEM, "defaultValue");
                    if (defaultValue == null && e.asType().getKind().isPrimitive()) {
                        defaultValue = getPrimitiveDefaultValue(e.asType());
                    }
                    // TODO Can't read Javadoc (and use #getRequiredJavadoc) if elements are in another module
                    // Ex: HttpConfig in quarkus-undertow can't read sources from ServerSslConfig in quarkus-core
                    String description = processingEnv.getElementUtils().getDocComment(e);
                    if (description != null) {
                        description = description.trim();
                    }
                    // ConfigGroup elements may also be annotated with ConfigItem annotation, but they are not leaf
                    if (!isConfigGroup) {
                        this.metadataCollector
                                .add(ItemMetadata.newProperty(prefix, buildConfigItemName(e),
                                        boxPrimitivesAndUnwrapOptional(e.asType()),
                                        clazz.getQualifiedName().toString(),
                                        null, description, defaultValue, null));
                    }
                }
            }
        }
    }

    /**
     * Primitive types need to be boxed per spring-boot meta-data specification.
     */
    private String boxPrimitivesAndUnwrapOptional(TypeMirror type) {
        if (type.toString().equals("int") || type.toString().equals("java.util.OptionalInt")) {
            return "java.lang.Integer";
        } else if (type.toString().equals("long") || type.toString().equals("java.util.OptionalLong")) {
            return "java.lang.Long";
        } else if (type.toString().equals("double") || type.toString().equals("java.util.OptionalDouble")) {
            return "java.lang.Double";
        } else if (type.toString().equals("float")) {
            return "java.lang.Float";
        } else if (type.toString().equals("boolean")) {
            return "java.lang.Boolean";
        } else if (type.toString().startsWith("java.util.Optional")) {
            return ((DeclaredType) type).getTypeArguments().get(0).toString();
        }
        return type.toString();
    }

    private String getPrimitiveDefaultValue(TypeMirror type) {
        if (type.toString().equals("int")) {
            return "0";
        } else if (type.toString().equals("long")) {
            return "0";
        } else if (type.toString().equals("double")) {
            return "0";
        } else if (type.toString().equals("float")) {
            return "0";
        } else if (type.toString().equals("boolean")) {
            return "false";
        }
        return null;
    }

    /**
     * Duplicates logic from io.quarkus.deployment.configuration.ConfigDefinition
     */
    private String buildConfigRootPrefix(TypeElement clazz) {
        String namespace = "quarkus";
        String name = getAnnotationParameterStringValue(clazz, ANNOTATION_CONFIG_ROOT, "name");
        String prefix;
        if (name == null || name.equals("<<hyphenated element name>>")) {
            prefix = namespace + "." + join("-",
                    withoutSuffix(lowerCase(StringUtil.camelHumpsIterator(clazz.getSimpleName().toString())), "config",
                            "configuration"));
        } else if (name.equals("<<parent>>")) {
            prefix = namespace;
        } else if (name.equals("<<element name>>")) {
            prefix = namespace + "." + clazz.getSimpleName().toString();
        } else {
            prefix = namespace + "." + name;
        }
        return prefix;
    }

    private String buildConfigItemName(Element field) {
        String name = getAnnotationParameterStringValue(field, ANNOTATION_CONFIG_ITEM, "name");
        if (name == null || name.equals("<<hyphenated element name>>")) {
            name = join("-", lowerCase(StringUtil.camelHumpsIterator(field.getSimpleName().toString())));
        } else if (name.equals("<<element name>>")) {
            name = field.getSimpleName().toString();
        }
        // TODO Handle <<parent>>
        return name;
    }

    private static boolean hasParameterAnnotated(ExecutableElement ex, String annotationName) {
        for (VariableElement param : ex.getParameters()) {
            if (isAnnotationPresent(param, annotationName)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAnnotationPresent(Element element, String annotationName) {
        for (AnnotationMirror i : element.getAnnotationMirrors()) {
            if (((TypeElement) i.getAnnotationType().asElement()).getQualifiedName().toString().equals(annotationName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns null when a parameter uses defaults.
     */
    private static String getAnnotationParameterStringValue(Element element,
            String annotationName, String annotationParameterName) {
        for (AnnotationMirror i : element.getAnnotationMirrors()) {
            if (((TypeElement) i.getAnnotationType().asElement()).getQualifiedName().toString().equals(annotationName)) {
                for (Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : i.getElementValues()
                        .entrySet()) {
                    if (entry.getKey().getSimpleName().toString().equals(annotationParameterName)) {

                        if (entry.getValue() != null) {
                            return (String) entry.getValue().getValue();
                        }
                    }
                }
                break;
            }
        }
        return null;
    }
}
