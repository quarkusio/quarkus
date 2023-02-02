package org.jboss.logging;

/**
 * Custom logger provider to plug the logger from Azure functions inside
 */
public class AzureFunctionsLoggerProvider extends AbstractMdcLoggerProvider implements LoggerProvider {
    private static ThreadLocal<java.util.logging.Logger> azureFunctionsLoggerHolder;

    public static void setAzureFunctionsLogger(java.util.logging.Logger logger) {
        azureFunctionsLoggerHolder.set(logger);
    }

    public static void resetAzureFunctionsLogger() {
        azureFunctionsLoggerHolder.remove();
    }

    public static java.util.logging.Logger getAzureFunctionsLogger() {
        return azureFunctionsLoggerHolder.get();
    }

    @Override
    public Logger getLogger(String name) {
        return new AzureFunctionsLogger(name);
    }
}
