package io.quarkus.vertx.http.deployment.devmode.console;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;

import io.quarkus.deployment.ide.Ide;
import io.quarkus.devconsole.runtime.spi.DevConsolePostHandler;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

/**
 * The purpose of this class is to open a class in the IDE on the server-side - meaning the machine
 * that is running the Quarkus application itself
 */
public class OpenIdeHandler extends DevConsolePostHandler {

    private static final Logger log = Logger.getLogger(OpenIdeHandler.class);
    private static final Map<String, String> LANG_TO_EXT = Map.of("java", "java", "kotlin", "kt");

    private final Ide ide;

    public OpenIdeHandler(Ide ide) {
        this.ide = ide;
    }

    @Override
    protected void dispatch(RoutingContext routingContext, MultiMap form) {
        String className = form.get("className");
        String lang = form.get("lang");
        String srcMainPath = form.get("srcMainPath");
        String line = form.get("line");

        if (isNullOrEmpty(className) || isNullOrEmpty(lang) || isNullOrEmpty(srcMainPath)) {
            routingContext.fail(400);
        }

        if (ide != null) {
            typicalProcessLaunch(routingContext, className, lang, srcMainPath, line, ide);
        } else {
            log.debug("Unhandled IDE : " + ide);
            routingContext.fail(500);
        }
    }

    private void typicalProcessLaunch(RoutingContext routingContext, String className, String lang, String srcMainPath,
            String line, Ide ide) {
        String arg = toFileName(className, lang, srcMainPath);
        if (!isNullOrEmpty(line)) {
            arg = arg + ":" + line;
        }
        launchInIDE(ide, arg, routingContext);
    }

    private String toFileName(String className, String lang, String srcMainPath) {
        String effectiveClassName = className;
        int dollarIndex = className.indexOf("$");
        if (dollarIndex > -1) {
            // in this case we are dealing with inner classes, so we need to get the name of the outer class
            // in order to use for conversion to the file name
            effectiveClassName = className.substring(0, dollarIndex);
        }
        return srcMainPath + File.separator + lang + File.separator
                + (effectiveClassName.replace('.', File.separatorChar) + "." + LANG_TO_EXT.get(lang));

    }

    protected void launchInIDE(Ide ide, String arg, RoutingContext routingContext) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    String effectiveCommand = ide.getEffectiveCommand();
                    if (isNullOrEmpty(effectiveCommand)) {
                        log.debug("Unable to determine proper launch command for IDE: " + ide);
                        routingContext.response().setStatusCode(500).end();
                        return;
                    }
                    new ProcessBuilder(Arrays.asList(effectiveCommand, arg)).inheritIO().start().waitFor(10,
                            TimeUnit.SECONDS);
                    routingContext.response().setStatusCode(200).end();
                } catch (Exception e) {
                    routingContext.fail(e);
                }
            }
        }, "Launch in IDE Action").start();
    }

    private boolean isNullOrEmpty(String arg) {
        return arg == null || arg.isEmpty();
    }
}
