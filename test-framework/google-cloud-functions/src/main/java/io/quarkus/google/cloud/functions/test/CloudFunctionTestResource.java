package io.quarkus.google.cloud.functions.test;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.test.common.QuarkusTestResourceConfigurableLifecycleManager;

/**
 * Test resource that starts a Google Cloud Function invoker at the beginning of the test and stops it at the end.
 * It must be configured with the {@link WithFunction} annotation.
 */
public class CloudFunctionTestResource implements QuarkusTestResourceConfigurableLifecycleManager<WithFunction> {

    private FunctionType functionType;
    private String functionName;
    private CloudFunctionsInvoker invoker;
    private final AtomicBoolean started = new AtomicBoolean(false);

    @Override
    public void init(WithFunction withFunction) {
        this.functionType = withFunction.value();
        this.functionName = withFunction.functionName();
    }

    @Override
    public Map<String, String> start() {
        return "".equals(functionName) ? Collections.emptyMap() : Map.of(functionType.getFunctionProperty(), functionName);
    }

    @Override
    public void inject(TestInjector testInjector) {
        if (started.compareAndSet(false, true)) {
            // This is a hack, we cannot start the invoker in the start() method as Quarkus is not yet initialized,
            // so we start it here as this method is called later (the same for reading the test port).
            int port = ConfigProvider.getConfig().getOptionalValue("quarkus.http.test-port", Integer.class).orElse(8081);
            this.invoker = new CloudFunctionsInvoker(functionType, port);
            try {
                this.invoker.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void stop() {
        try {
            this.invoker.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
