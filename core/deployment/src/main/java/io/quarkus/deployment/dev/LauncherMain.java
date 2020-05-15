package io.quarkus.deployment.dev;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import io.smallrye.config.SmallRyeConfigProviderResolver;

/**
 * The entry point when launched from an IDE
 */
public class LauncherMain {

    public static void main(Path appClasses, Path wiring, URL[] classPath, String... args) throws Exception {
        DevModeContext context = new DevModeContext();
        context.setAbortOnFailedStart(true);
        context.setTest(false);
        context.setCacheDir(Files.createTempDirectory("quarkus-cache").toFile());
        context.setSourceEncoding("UTF-8");
        File appClassesFile = appClasses.toFile();

        //TODO: huge hacks
        File src = new File(appClassesFile, "../../src/main/java");
        File res = new File(appClassesFile, "../../src/main/resources");

        context.setApplicationRoot(new DevModeContext.ModuleInfo("main", new File("").getAbsolutePath(),
                Collections.singleton(src.getAbsolutePath()), appClassesFile.getAbsolutePath(), res.getAbsolutePath()));
        //the loading of this is super weird, and does its own class loader delegation for some reason
        ConfigProviderResolver.setInstance(new SmallRyeConfigProviderResolver());
        DevModeMain main = new DevModeMain(context);
        main.start();
    }
}
