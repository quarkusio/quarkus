package io.quarkus.cli.build;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ExecuteUtilTest {

    private final static String MVNW = "mvnw.junittest";
    private final static String[] WINDOWS_WRAPPER = new String[] { MVNW };

    @TempDir
    Path tempDir;

    @Test
    public void findWrapperRecursionTest() throws IOException {
        File wrapper = ExecuteUtil.findWrapper(tempDir, WINDOWS_WRAPPER, MVNW);
        assertThat(wrapper).isNull();

        wrapper = ExecuteUtil.findWrapper(tempDir.resolve("subproject/nestedSubproject"), WINDOWS_WRAPPER, MVNW);
        assertThat(wrapper).isNull();

        // create a file representing the maven wrapper
        Path wrapperLocation = tempDir.resolve(MVNW);
        assertThat(wrapperLocation.toFile().createNewFile()).isTrue();

        wrapper = ExecuteUtil.findWrapper(tempDir, WINDOWS_WRAPPER, MVNW);
        assertThat(wrapper).isNotNull();
        assertThat(wrapper).isEqualTo(wrapperLocation.toFile());

        wrapper = ExecuteUtil.findWrapper(tempDir.resolve("subproject/nestedSubproject"), WINDOWS_WRAPPER, MVNW);
        assertThat(wrapper).isNotNull();
        assertThat(wrapper).isEqualTo(wrapperLocation.toFile());

        wrapper = ExecuteUtil.findWrapper(tempDir.resolve(".."), WINDOWS_WRAPPER, MVNW);
        assertThat(wrapper).isNull();
    }
}
