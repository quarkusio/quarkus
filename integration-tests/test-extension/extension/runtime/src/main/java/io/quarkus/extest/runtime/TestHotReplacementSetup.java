package io.quarkus.extest.runtime;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import io.quarkus.dev.ErrorPageGenerators;
import io.quarkus.dev.spi.HotReplacementContext;
import io.quarkus.dev.spi.HotReplacementSetup;

public class TestHotReplacementSetup implements HotReplacementSetup {

    public final static String HOT_REPLACEMENT_FILE = "hot.replacement";

    private static final String HOT_REPLACEMENT_EXCEPTION = HotReplacementException.class.getName();

    private HotReplacementContext context;

    @Override
    public void setupHotDeployment(HotReplacementContext context) {
        context.consumeNoRestartChanges(this::noRestartChanges);
        this.context = context;
        ErrorPageGenerators.register(HOT_REPLACEMENT_EXCEPTION, this::generatePage);
    }

    public void noRestartChanges(Set<String> changedFiles) {
        if (changedFiles.contains(HOT_REPLACEMENT_FILE)) {
            for (Path resourcePath : context.getResourcesDir()) {
                Path myFile = resourcePath.resolve(HOT_REPLACEMENT_FILE);
                if (Files.exists(myFile)) {
                    try {
                        String contents = Files.readString(myFile);
                        if ("throw".equals(contents))
                            throw new RuntimeException(new HotReplacementException());
                        return;
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            }
        }
    }

    private String generatePage(Throwable x) {
        return "Generated page for exception " + x;
    }
}
