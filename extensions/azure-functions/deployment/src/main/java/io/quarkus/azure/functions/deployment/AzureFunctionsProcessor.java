package io.quarkus.azure.functions.deployment;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.inject.Singleton;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.legacy.function.configurations.FunctionConfiguration;
import com.microsoft.azure.toolkit.lib.legacy.function.handlers.AnnotationHandler;
import com.microsoft.azure.toolkit.lib.legacy.function.handlers.AnnotationHandlerImpl;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.JarBuildItem;
import io.quarkus.deployment.pkg.builditem.LegacyJarRequiredBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;

public class AzureFunctionsProcessor {
    private static final Logger log = Logger.getLogger(AzureFunctionsProcessor.class);

    protected static final String HOST_JSON = "host.json";
    protected static final String LOCAL_SETTINGS_JSON = "local.settings.json";
    public static final String FUNCTION_JSON = "function.json";

    @BuildStep
    public LegacyJarRequiredBuildItem forceLegacy(PackageConfig config) {
        // TODO: Instead of this, consume a LegacyJarBuildItem
        // Azure Functions need a legacy jar and no runner
        return new LegacyJarRequiredBuildItem();
    }

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.AZURE_FUNCTIONS);
    }

    @BuildStep
    AzureFunctionsAppNameBuildItem appName(OutputTargetBuildItem output, AzureFunctionsConfig functionsConfig) {
        String appName = functionsConfig.appName.orElse(output.getBaseName());
        return new AzureFunctionsAppNameBuildItem(appName);
    }

    @BuildStep(onlyIf = IsNormal.class, onlyIfNot = NativeBuild.class)
    public ArtifactResultBuildItem packageFunctions(List<AzureFunctionBuildItem> functions,
            OutputTargetBuildItem target,
            AzureFunctionsConfig functionsConfig,
            PackageConfig packageConfig,
            AzureFunctionsAppNameBuildItem appName,
            JarBuildItem jar) throws Exception {
        if (functions == null || functions.isEmpty()) {
            log.warn("No azure functions exist in deployment");
            return null;
        }
        AnnotationHandler handler = new AnnotationHandlerImpl();
        HashSet<Method> methods = new HashSet<>();
        for (AzureFunctionBuildItem item : functions)
            methods.add(item.getMethod());
        final Map<String, FunctionConfiguration> configMap = handler.generateConfigurations(methods);
        final String scriptFilePath = String.format("../%s.jar", target.getBaseName() + packageConfig.computedRunnerSuffix());
        configMap.values().forEach(config -> config.setScriptFile(scriptFilePath));
        configMap.values().forEach(FunctionConfiguration::validate);

        final ObjectWriter objectWriter = getObjectWriter();

        Path rootPath = target.getOutputDirectory().resolve("..");
        Path outputDirectory = target.getOutputDirectory();
        Path functionStagingDir = outputDirectory.resolve("azure-functions").resolve(appName.getAppName());

        copyHostJson(rootPath, functionStagingDir);

        copyLocalSettingsJson(rootPath, functionStagingDir);

        writeFunctionJsonFiles(objectWriter, configMap, functionStagingDir);

        copyJarsToStageDirectory(jar, functionStagingDir);
        return new ArtifactResultBuildItem(functionStagingDir, "azure-functions", Collections.EMPTY_MAP);
    }

    protected void writeFunctionJsonFiles(final ObjectWriter objectWriter,
            final Map<String, FunctionConfiguration> configMap,
            Path functionStagingDir) throws IOException {
        if (!configMap.isEmpty()) {
            String functionDir = functionStagingDir.toString();
            for (final Map.Entry<String, FunctionConfiguration> config : configMap.entrySet()) {
                writeFunctionJsonFile(objectWriter, config.getKey(), config.getValue(), functionDir);
            }
        }
    }

    protected void writeFunctionJsonFile(final ObjectWriter objectWriter, final String functionName,
            final FunctionConfiguration config,
            final String functionStagingDir) throws IOException {
        final File functionJsonFile = Paths.get(functionStagingDir,
                functionName, FUNCTION_JSON).toFile();
        writeObjectToFile(objectWriter, config, functionJsonFile);
    }

    private static final String DEFAULT_HOST_JSON = "{\"version\":\"2.0\",\"extensionBundle\":" +
            "{\"id\":\"Microsoft.Azure.Functions.ExtensionBundle\",\"version\":\"[3.*, 4.0.0)\"}}\n";

    protected void copyHostJson(Path rootPath, Path functionStagingDir) throws IOException {
        final File sourceHostJsonFile = rootPath.resolve(HOST_JSON).toFile();
        final File destHostJsonFile = functionStagingDir.resolve("host.json").toFile();
        copyFilesWithDefaultContent(sourceHostJsonFile, destHostJsonFile, DEFAULT_HOST_JSON);
    }

    private static final String DEFAULT_LOCAL_SETTINGS_JSON = "{ \"IsEncrypted\": false, \"Values\": " +
            "{ \"FUNCTIONS_WORKER_RUNTIME\": \"java\" } }";

    protected void copyLocalSettingsJson(Path rootPath, Path functionStagingDir) throws IOException {
        final File sourceLocalSettingsJsonFile = rootPath.resolve(LOCAL_SETTINGS_JSON).toFile();
        final File destLocalSettingsJsonFile = functionStagingDir.resolve(LOCAL_SETTINGS_JSON).toFile();
        copyFilesWithDefaultContent(sourceLocalSettingsJsonFile, destLocalSettingsJsonFile, DEFAULT_LOCAL_SETTINGS_JSON);
    }

    private static void copyFilesWithDefaultContent(File source, File dest, String defaultContent)
            throws IOException {
        if (source != null && source.exists()) {
            FileUtils.copyFile(source, dest);
        } else {
            FileUtils.write(dest, defaultContent, Charset.defaultCharset());
        }
    }

    private static final String AZURE_FUNCTIONS_JAVA_CORE_LIBRARY = "com.microsoft.azure.functions.azure-functions-java-core-library";
    protected static final String AZURE_FUNCTIONS_JAVA_LIBRARY = "com.microsoft.azure.functions.azure-functions-java-library";

    protected void copyJarsToStageDirectory(JarBuildItem jar, Path functionStagingDir)
            throws IOException, AzureExecutionException {
        final String stagingDirectory = functionStagingDir.toString();
        final File libFolder = Paths.get(stagingDirectory, "lib").toFile();
        if (libFolder.exists()) {
            FileUtils.cleanDirectory(libFolder);
        }
        for (File dep : jar.getLibraryDir().toFile().listFiles()) {
            if (dep.getName().startsWith(AZURE_FUNCTIONS_JAVA_CORE_LIBRARY)
                    || dep.getName().startsWith(AZURE_FUNCTIONS_JAVA_LIBRARY)) {
                continue;
            }
            FileUtils.copyFileToDirectory(dep, libFolder);
        }
        FileUtils.copyFileToDirectory(jar.getPath().toFile(), functionStagingDir.toFile());
    }

    protected void writeObjectToFile(final ObjectWriter objectWriter, final Object object, final File targetFile)
            throws IOException {
        targetFile.getParentFile().mkdirs();
        targetFile.createNewFile();
        objectWriter.writeValue(targetFile, object);
    }

    protected ObjectWriter getObjectWriter() {
        final DefaultPrettyPrinter.Indenter indenter = DefaultIndenter.SYSTEM_LINEFEED_INSTANCE.withLinefeed(StringUtils.LF);
        final PrettyPrinter prettyPrinter = new DefaultPrettyPrinter().withObjectIndenter(indenter);
        return new ObjectMapper()
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .writer(prettyPrinter);
    }

    @BuildStep
    public void findFunctions(CombinedIndexBuildItem combined,
            BuildProducer<AzureFunctionBuildItem> functions) {
        IndexView index = combined.getIndex();
        Collection<AnnotationInstance> anns = index.getAnnotations(AzureFunctionsDotNames.FUNCTION_NAME);
        anns.forEach(annotationInstance -> {
            MethodInfo methodInfo = annotationInstance.target().asMethod();
            ClassInfo ci = methodInfo.declaringClass();
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            try {
                Class declaring = loader.loadClass(ci.name().toString());
                Class[] params = methodInfo.parameters().stream().map(methodParameterInfo -> {
                    try {
                        return Class.forName(methodParameterInfo.type().name().toString(), false, loader);
                    } catch (ClassNotFoundException e) {
                        throw new DeploymentException(e);
                    }
                }).toArray(Class[]::new);
                Method method = null;
                try {
                    method = declaring.getMethod(methodInfo.name(), params);
                } catch (NoSuchMethodException e) {
                    throw new DeploymentException(e);
                }
                String funcName = annotationInstance.value().asString();
                functions.produce(new AzureFunctionBuildItem(funcName, declaring, method));
            } catch (ClassNotFoundException e) {
                throw new DeploymentException(e);
            }
        });
    }

    @BuildStep
    public void registerArc(BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            List<AzureFunctionBuildItem> functions) {
        if (functions == null || functions.isEmpty())
            return;

        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder()
                .setDefaultScope(BuiltinScope.REQUEST.getName())
                .setUnremovable();
        Set<Class> classes = functions.stream().map(item -> item.getDeclaring()).collect(Collectors.toSet());
        for (Class funcClass : classes) {
            if (Modifier.isInterface(funcClass.getModifiers()) || Modifier.isAbstract(funcClass.getModifiers()))
                continue;
            if (isScoped(funcClass)) {
                //log.info("Add unremovable: " + funcClass.name().toString());
                // It has a built-in scope - just mark it as unremovable
                unremovableBeans
                        .produce(new UnremovableBeanBuildItem(
                                new UnremovableBeanBuildItem.BeanClassNameExclusion(funcClass.getName())));
            } else {
                // No built-in scope found - add as additional bean
                //log.info("Add default: " + funcClass.name().toString());
                builder.addBeanClass(funcClass.getName());
            }
        }
        additionalBeans.produce(builder.build());
    }

    public static boolean isScoped(Class clazz) {
        if (clazz.isAnnotationPresent(Dependent.class))
            return true;
        if (clazz.isAnnotationPresent(Singleton.class))
            return true;
        if (clazz.isAnnotationPresent(ApplicationScoped.class))
            return true;
        if (clazz.isAnnotationPresent(RequestScoped.class))
            return true;
        return false;
    }

}
