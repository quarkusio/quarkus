package io.quarkus.devui.deployment.ide;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.ide.EffectiveIdeBuildItem;
import io.quarkus.deployment.ide.Ide;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.devui.runtime.ide.IdeJsonRPCService;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.utilities.OS;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;

/**
 * Processor for Ide interaction in Dev UI
 */
public class IdeProcessor {
    private static final Logger log = Logger.getLogger(IdeProcessor.class);
    private static final Map<String, String> LANG_TO_EXT = Map.of("java", "java", "kotlin", "kt");

    @BuildStep(onlyIf = IsDevelopment.class)
    void createJsonRPCService(BuildProducer<JsonRPCProvidersBuildItem> JsonRPCProvidersProducer,
            Optional<EffectiveIdeBuildItem> effectiveIdeBuildItem) {

        if (effectiveIdeBuildItem.isPresent()) {
            Ide ide = effectiveIdeBuildItem.get().getIde();
            if (ide != null) {
                DevConsoleManager.register("dev-ui-ide-open", map -> {
                    String fileName = map.get("fileName");
                    String lang = map.get("lang");
                    String lineNumber = map.get("lineNumber");
                    return typicalProcessLaunch(fileName, lang, lineNumber, ide);
                });
            }
            JsonRPCProvidersProducer.produce(new JsonRPCProvidersBuildItem("devui-ide-interaction", IdeJsonRPCService.class));
        }
    }

    private boolean typicalProcessLaunch(String className, String lang, String line, Ide ide) {
        String fileName = toFileName(className, lang);
        if (fileName == null) {
            return false;
        }
        List<String> args = ide.createFileOpeningArgs(fileName, line);
        return launchInIDE(ide, args);
    }

    private String toFileName(String className, String lang) {
        String effectiveClassName = className;
        int dollarIndex = className.indexOf("$");
        if (dollarIndex > -1) {
            // in this case we are dealing with inner classes, so we need to get the name of the outer class
            // in order to use for conversion to the file name
            effectiveClassName = className.substring(0, dollarIndex);
        }
        String fileName = effectiveClassName.replace('.', File.separatorChar) + "." + LANG_TO_EXT.get(lang);
        Path sourceFile = Ide.findSourceFile(fileName);
        if (sourceFile == null) {
            return null;
        }
        return sourceFile.toAbsolutePath().toString();
    }

    protected boolean launchInIDE(Ide ide, List<String> args) {
        String effectiveCommand = ide.getEffectiveCommand();
        if (isNullOrEmpty(effectiveCommand)) {
            return false;
        }

        new Thread(new Runnable() {
            public void run() {
                try {
                    List<String> command = new ArrayList<>();
                    command.add(effectiveCommand);
                    command.addAll(args);
                    new ProcessBuilder(command).inheritIO().start().waitFor(10,
                            TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.error("Could not launch IDE", e);
                }
            }
        }, "Launch in IDE Action").start();
        return true;
    }

    public static void openBrowser(HttpRootPathBuildItem rp, NonApplicationRootPathBuildItem np, String path, String host,
            String port) {
        if (path.startsWith("/q")) {
            path = np.resolvePath(path.substring(3));
        } else {
            path = rp.resolvePath(path.substring(1));
        }

        StringBuilder sb = new StringBuilder("http://");
        Config c = ConfigProvider.getConfig();
        sb.append(host);
        sb.append(":");
        sb.append(port);
        sb.append(path);
        String url = sb.toString();

        Runtime rt = Runtime.getRuntime();
        OS os = OS.determineOS();
        String[] command = null;
        try {
            switch (os) {
                case MAC:
                    command = new String[] { "open", url };
                    break;
                case LINUX:
                    command = new String[] { "xdg-open", url };
                    break;
                case WINDOWS:
                    command = new String[] { "rundll32", "url.dll,FileProtocolHandler", url };
                    break;
                case OTHER:
                    log.error("Cannot launch browser on this operating system");
            }
            if (command != null) {
                rt.exec(command);
            }
        } catch (Exception e) {
            log.debug("Failed to launch browser", e);
            if (command != null) {
                log.warn("Unable to open browser using command: '" + String.join(" ", command) + "'. Failure is: '"
                        + e.getMessage() + "'");
            }
        }

    }

    private boolean isNullOrEmpty(String arg) {
        return arg == null || arg.isBlank();
    }
}
