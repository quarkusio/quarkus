package io.quarkus.devui.runtime.ide;

import java.util.Map;

import io.quarkus.dev.console.DevConsoleManager;

/**
 * This allows opening the IDE
 */
public class IdeJsonRPCService {
    private static final org.jboss.logging.Logger LOG = org.jboss.logging.Logger.getLogger(IdeJsonRPCService.class);

    public boolean open(String fileName, String lang, int lineNumber) {
        if (isNullOrEmpty(fileName) || isNullOrEmpty(lang)) {
            return false;
        }

        DevConsoleManager.invoke("dev-ui-ide-open", Map.of(
                "fileName", fileName,
                "lang", lang,
                "lineNumber", String.valueOf(lineNumber)));
        return true;
    }

    private boolean isNullOrEmpty(String arg) {
        return arg == null || arg.trim().isEmpty();
    }
}
