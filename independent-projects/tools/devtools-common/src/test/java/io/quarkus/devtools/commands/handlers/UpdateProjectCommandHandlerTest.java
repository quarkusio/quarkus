package io.quarkus.devtools.commands.handlers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.registry.catalog.ExtensionCatalog;

class UpdateProjectCommandHandlerTest {

    final ExtensionCatalog catalog = ExtensionCatalog.builder().setMetadata(Map.of("project", Map.of("properties", Map.of(
            "compiler-plugin-version", "3.14.0",
            "recommended-java-version", "21",
            "surefire-plugin-version", "4.5.3")))).build();

    @Test
    void shouldWarnWhenLocalValuesDiffer() {
        List<String> warnings = new ArrayList<>();
        Map<String, String> localProps = Map.of("compiler-plugin.version", "3.15.0", "maven.compiler.release", "17");
        MessageWriter log = recordingWriter(warnings);
        UpdateProjectCommandHandler.warnOnPomPropertyDrift(catalog, localProps::get, log);
        assertThat(warnings).hasSize(2);
    }

    @Test
    void shouldNotWarnWhenValuesMatch() {
        List<String> warnings = new ArrayList<>();
        Map<String, String> localProps = Map.of("compiler-plugin.version", "3.14.0");
        MessageWriter log = recordingWriter(warnings);
        UpdateProjectCommandHandler.warnOnPomPropertyDrift(catalog, localProps::get, log);
        assertThat(warnings).hasSize(0);
    }

    @Test
    void shouldNotWarnWhenValuesNotSetLocally() {
        List<String> warnings = new ArrayList<>();
        Map<String, String> localProps = Map.of();
        MessageWriter log = recordingWriter(warnings);
        UpdateProjectCommandHandler.warnOnPomPropertyDrift(catalog, localProps::get, log);
        assertThat(warnings).hasSize(0);
    }

    @Test
    void shouldNotWarnWhenCatalogHasNoEntry() {
        List<String> warnings = new ArrayList<>();
        ExtensionCatalog emptyCatalog = ExtensionCatalog.builder().build();
        Map<String, String> localProps = Map.of("compiler-plugin.version", "3.14.0");
        MessageWriter log = recordingWriter(warnings);
        UpdateProjectCommandHandler.warnOnPomPropertyDrift(emptyCatalog, localProps::get, log);
        assertThat(warnings).hasSize(0);
    }

    private MessageWriter recordingWriter(List<String> warnings) {
        return new MessageWriter() {
            public void warn(String msg) {
                warnings.add(msg);
            }

            public void info(String msg) {
            }

            public void error(String msg) {
            }

            public void debug(String msg) {
            }

            public boolean isDebugEnabled() {
                return false;
            }
        };
    }

}
