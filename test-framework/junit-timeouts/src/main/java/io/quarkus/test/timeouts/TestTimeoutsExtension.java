package io.quarkus.test.timeouts;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.security.CodeSource;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;

/**
 * A global JUnit extension (registered through autodetection) that resolves the timeout of each test and exposes it
 * through {@link TestTimeouts#get()}.
 */
public class TestTimeoutsExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback,
        BeforeTestExecutionCallback, AfterTestExecutionCallback {

    private static final Namespace NAMESPACE = Namespace.create(TestTimeoutsExtension.class);
    private static final String ENFORCER_KEY = "enforcer";
    private static final Map<Path, Module> MODULES = new ConcurrentHashMap<>();

    private record Module(TimeoutResolver resolver, String path) {
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        publish(context, null);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        publish(context, context.getRequiredTestMethod().getName());
    }

    @Override
    public void afterEach(ExtensionContext context) {
        // back to the class level value, in case the class level callbacks still need it
        publish(context, null);
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        TimeLimitEnforcer enforcer = TimeLimitEnforcer.startFor(context);
        if (enforcer != null) {
            context.getStore(NAMESPACE).put(ENFORCER_KEY, enforcer);
        }
    }

    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        TimeLimitEnforcer enforcer = context.getStore(NAMESPACE).remove(ENFORCER_KEY, TimeLimitEnforcer.class);
        if (enforcer != null) {
            enforcer.finish();
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        System.clearProperty(TestTimeouts.CURRENT_PROPERTY);
    }

    private static void publish(ExtensionContext context, String method) {
        Class<?> testClass = context.getRequiredTestClass();
        Path moduleDir = moduleDir(testClass);
        Path start = moduleDir != null ? moduleDir : Path.of("").toAbsolutePath();
        Module module = MODULES.computeIfAbsent(start, dir -> {
            Path root = TimeoutResolver.findRoot(dir);
            String modulePath = moduleDir != null && root != null && dir.startsWith(root)
                    ? root.relativize(dir).toString().replace('\\', '/')
                    : null;
            return new Module(TimeoutResolver.load(dir), modulePath);
        });
        String packageName = testClass.getPackageName();
        String className = packageName.isEmpty() ? testClass.getName()
                : testClass.getName().substring(packageName.length() + 1);
        Duration timeout = module.resolver().resolve(module.path(), className.replace('$', '.'), method);
        System.setProperty(TestTimeouts.CURRENT_PROPERTY, timeout.toString());
    }

    /**
     * The module is the directory holding {@code target/test-classes} (or {@code target/classes}).
     */
    private static Path moduleDir(Class<?> testClass) {
        try {
            CodeSource source = testClass.getProtectionDomain().getCodeSource();
            if (source == null) {
                return null;
            }
            Path location = Path.of(source.getLocation().toURI());
            for (Path p = location; p != null; p = p.getParent()) {
                Path parent = p.getParent();
                if (parent != null && "target".equals(parent.getFileName() == null ? null : parent.getFileName().toString())) {
                    return parent.getParent();
                }
            }
            return null;
        } catch (URISyntaxException | RuntimeException e) {
            return null;
        }
    }
}
