package io.quarkus.creator.phase.generateconfig;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.logging.Logger;

import io.quarkus.bootstrap.BootstrapDependencyProcessingException;
import io.quarkus.bootstrap.DefineClassVisibleURLClassLoader;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.builder.BuildChain;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildExecutionBuilder;
import io.quarkus.builder.BuildResult;
import io.quarkus.creator.AppCreatorException;
import io.quarkus.creator.CuratedApplicationCreator;
import io.quarkus.creator.CuratedTask;
import io.quarkus.creator.curator.CurateOutcome;
import io.quarkus.deployment.ExtensionLoader;
import io.quarkus.deployment.QuarkusConfig;
import io.quarkus.deployment.builditem.ArchiveRootBuildItem;
import io.quarkus.deployment.builditem.ConfigDescriptionBuildItem;
import io.quarkus.deployment.builditem.ExtensionClassLoaderBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.util.FileUtil;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.runtime.configuration.QuarkusConfigFactory;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

/**
 * This phase generates an example configuration file
 *
 * @author Stuart Douglas
 */
public class GenerateConfigTask implements CuratedTask<Path> {

    private static final Logger log = Logger.getLogger(GenerateConfigTask.class);

    private final Path configFile;

    public GenerateConfigTask(Path configFile) {
        this.configFile = configFile;
    }

    @Override
    public Path run(CurateOutcome appState, CuratedApplicationCreator creator) throws AppCreatorException {
        //first lets look for some config, as it is not on the current class path
        //and we need to load it to run the build process
        //TODO: do we actually need to load this config? Does it affect resolution?
        if (Files.exists(configFile)) {
            try {
                SmallRyeConfigBuilder builder = ConfigUtils.configBuilder(false)
                        .withSources(new PropertiesConfigSource(configFile.toUri().toURL()));
                final SmallRyeConfig config = builder.build();
                QuarkusConfigFactory.setConfig(config);
                final ConfigProviderResolver cpr = ConfigProviderResolver.instance();
                final Config existing = cpr.getConfig();
                if (existing != config) {
                    cpr.releaseConfig(existing);
                    // subsequent calls will get the new config
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        final AppModelResolver depResolver = appState.getArtifactResolver();
        List<AppDependency> appDeps;
        try {
            appDeps = appState.getEffectiveModel().getAllDependencies();
        } catch (BootstrapDependencyProcessingException e) {
            throw new AppCreatorException("Failed to resolve application build classpath", e);
        }

        URLClassLoader runnerClassLoader = null;
        try {
            // we need to make sure all the deployment artifacts are on the class path
            final List<URL> cpUrls = new ArrayList<>(appDeps.size());

            for (AppDependency appDep : appDeps) {
                final Path resolvedDep = depResolver.resolve(appDep.getArtifact());
                cpUrls.add(resolvedDep.toUri().toURL());
            }

            runnerClassLoader = new DefineClassVisibleURLClassLoader(cpUrls.toArray(new URL[cpUrls.size()]),
                    getClass().getClassLoader());

            ClassLoader old = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(runnerClassLoader);

                final BuildChainBuilder chainBuilder = BuildChain.builder();

                ExtensionLoader.loadStepsFrom(runnerClassLoader).accept(chainBuilder);
                chainBuilder.loadProviders(runnerClassLoader);

                chainBuilder
                        .addInitial(QuarkusConfig.class)
                        .addInitial(ShutdownContextBuildItem.class)
                        .addInitial(LaunchModeBuildItem.class)
                        .addInitial(ArchiveRootBuildItem.class)
                        .addInitial(LiveReloadBuildItem.class)
                        .addInitial(ExtensionClassLoaderBuildItem.class);
                chainBuilder.addFinal(ConfigDescriptionBuildItem.class);

                BuildChain chain = chainBuilder
                        .build();
                BuildExecutionBuilder execBuilder = chain.createExecutionBuilder("main")
                        .produce(QuarkusConfig.INSTANCE)
                        .produce(new LaunchModeBuildItem(LaunchMode.NORMAL))
                        .produce(new ShutdownContextBuildItem())
                        .produce(new LiveReloadBuildItem())
                        .produce(new ArchiveRootBuildItem(Files.createTempDirectory("empty")))
                        .produce(new ExtensionClassLoaderBuildItem(runnerClassLoader));
                BuildResult buildResult = execBuilder
                        .execute();

                List<ConfigDescriptionBuildItem> descriptions = buildResult.consumeMulti(ConfigDescriptionBuildItem.class);
                Collections.sort(descriptions);

                String existing = "";
                if (Files.exists(configFile)) {
                    try (InputStream in = new FileInputStream(configFile.toFile())) {
                        existing = new String(FileUtil.readFileContents(in), StandardCharsets.UTF_8);
                    }
                }

                StringBuilder sb = new StringBuilder();
                for (ConfigDescriptionBuildItem i : descriptions) {
                    //we don't want to add these if they already exist
                    //either in commended or uncommented form
                    if (existing.contains("\n" + i.getPropertyName() + "=") ||
                            existing.contains("\n#" + i.getPropertyName() + "=")) {
                        continue;
                    }

                    sb.append("\n#\n");
                    sb.append(formatDocs(i.getDocs()));
                    sb.append("\n#\n#");
                    sb.append(i.getPropertyName() + "=" + i.getDefaultValue());
                    sb.append("\n");
                }

                try (FileOutputStream out = new FileOutputStream(configFile.toFile(), true)) {
                    out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
                }

            } finally {
                Thread.currentThread().setContextClassLoader(old);
            }
        } catch (Exception e) {
            throw new AppCreatorException("Failed to generate config file", e);
        } finally {
            if (runnerClassLoader != null) {
                try {
                    runnerClassLoader.close();
                } catch (IOException e) {
                    log.warn("Failed to close runner classloader", e);
                }
            }
        }
        return configFile;
    }

    private String formatDocs(String docs) {

        if (docs == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();

        boolean lastEmpty = false;
        boolean first = true;

        for (String line : docs.replace("<p>", "\n").split("\n")) {
            //process line by line
            String trimmed = line.trim();
            //if the lines are empty we only include a single empty line at most, and add a # character
            if (trimmed.isEmpty()) {
                if (!lastEmpty && !first) {
                    lastEmpty = true;
                    builder.append("\n#");
                }
                continue;
            }
            //add the newlines
            lastEmpty = false;
            if (first) {
                first = false;
            } else {
                builder.append("\n");
            }
            //replace some special characters, others are taken care of by regex below
            builder.append("# " + trimmed.replace("\n", "\n#")
                    .replace("<ul>", "")
                    .replace("</ul>", "")
                    .replace("<li>", " - ")
                    .replace("</li>", ""));
        }

        String ret = builder.toString();
        //replace @code
        ret = Pattern.compile("\\{@code (.*?)\\}").matcher(ret).replaceAll("'$1'");
        //replace @link with a reference to the field name
        Matcher matcher = Pattern.compile("\\{@link #(.*?)\\}").matcher(ret);
        while (matcher.find()) {
            ret = ret.replace(matcher.group(0), "'" + configify(matcher.group(1)) + "'");
        }

        return ret;
    }

    private String configify(String group) {
        //replace uppercase characters with a - followed by lowercase
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < group.length(); ++i) {
            char c = group.charAt(i);
            if (Character.isUpperCase(c)) {
                ret.append("-");
                ret.append(Character.toLowerCase(c));
            } else {
                ret.append(c);
            }
        }
        return ret.toString();
    }
}
