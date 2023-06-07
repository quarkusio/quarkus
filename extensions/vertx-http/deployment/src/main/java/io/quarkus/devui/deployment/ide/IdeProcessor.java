package io.quarkus.devui.deployment.ide;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.ide.EffectiveIdeBuildItem;
import io.quarkus.deployment.ide.Ide;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.devui.runtime.ide.IdeJsonRPCService;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;

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

    private boolean isNullOrEmpty(String arg) {
        return arg == null || arg.isBlank();
    }
}
