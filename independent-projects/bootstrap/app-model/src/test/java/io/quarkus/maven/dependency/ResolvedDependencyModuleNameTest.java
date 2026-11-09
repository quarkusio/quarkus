package io.quarkus.maven.dependency;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.bootstrap.workspace.WorkspaceModuleId;

public class ResolvedDependencyModuleNameTest {

    private static final Logger LOG = Logger.getLogger("io.quarkus.maven.dependency");
    private static final String COMPUTED_NAME_MESSAGE = "does not have a defined module name";

    @TempDir
    Path content;

    private final List<LogRecord> records = new CopyOnWriteArrayList<>();
    private final Handler handler = new Handler() {
        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    };
    private Level previousLevel;

    @BeforeEach
    void captureLog() {
        previousLevel = LOG.getLevel();
        LOG.setLevel(Level.ALL);
        handler.setLevel(Level.ALL);
        LOG.addHandler(handler);
    }

    @AfterEach
    void releaseLog() {
        LOG.removeHandler(handler);
        LOG.setLevel(previousLevel);
    }

    @Test
    void quteExtensionDoesNotTakeTheQuteEngineModuleName() {
        assertThat(moduleName("io.quarkus", "quarkus-qute")).isEqualTo("io.quarkus.qute.runtime");
    }

    @Test
    void derivedNameDropsTheRepeatedGroupPart() {
        assertThat(moduleName("io.quarkus", "quarkus-cache")).isEqualTo("io.quarkus.cache");
    }

    @Test
    void derivedNameOfAnExternalArtifactIsLoggedAsAWarning() {
        assertThat(moduleName("org.acme", "some-lib")).isEqualTo("org.acme.some.lib");
        assertThat(records).singleElement().satisfies(record -> {
            assertThat(record.getLevel()).isEqualTo(Level.WARNING);
            assertThat(record.getMessage()).contains(COMPUTED_NAME_MESSAGE);
        });
    }

    @Test
    void derivedNameOfAWorkspaceModuleIsNotLoggedAsAWarning() {
        WorkspaceModule module = WorkspaceModule.builder()
                .setModuleId(WorkspaceModuleId.of("org.acme", "code-with-quarkus", "1.0"))
                .setModuleDir(content)
                .setBuildDir(content.resolve("target"))
                .build();
        String name = builder("org.acme", "code-with-quarkus")
                .setWorkspaceModule(module)
                .build()
                .getModuleName();
        assertThat(name).isEqualTo("org.acme.code.with.quarkus");
        assertThat(records).singleElement().satisfies(record -> {
            assertThat(record.getLevel()).isEqualTo(Level.FINE);
            assertThat(record.getMessage()).contains(COMPUTED_NAME_MESSAGE);
        });
    }

    @Test
    void concurrentCallsComputeTheNameOnce() throws Exception {
        ResolvedDependency dependency = builder("org.acme", "some-lib").build();
        int threads = 8;
        CyclicBarrier barrier = new CyclicBarrier(threads);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            List<Future<String>> names = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                names.add(executor.submit(() -> {
                    barrier.await();
                    return dependency.getModuleName();
                }));
            }
            for (Future<String> name : names) {
                assertThat(name.get()).isEqualTo("org.acme.some.lib");
            }
        } finally {
            executor.shutdownNow();
        }
        assertThat(records).hasSize(1);
    }

    private String moduleName(String groupId, String artifactId) {
        return builder(groupId, artifactId).build().getModuleName();
    }

    private ResolvedDependencyBuilder builder(String groupId, String artifactId) {
        return ResolvedDependencyBuilder.newInstance()
                .setGroupId(groupId)
                .setArtifactId(artifactId)
                .setVersion("1.0")
                .setResolvedPath(content);
    }
}
