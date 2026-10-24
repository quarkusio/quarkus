package io.quarkus.maven;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.jupiter.api.Test;

import io.quarkus.maven.components.BootstrapSessionListener;

public class ExtensionsHintTest {

    @Test
    public void hintIsLoggedOnTheBuildGoalWhenTheExtensionsAreNotEnabled() {
        BuildMojo mojo = mojo("build");

        mojo.hintEnableExtensions();

        assertThat(warnings(mojo)).singleElement().asString().contains("<extensions>true</extensions>");
    }

    @Test
    public void hintIsNotLoggedOnOtherGoals() {
        BuildMojo mojo = mojo("generate-code");

        mojo.hintEnableExtensions();

        assertThat(warnings(mojo)).isEmpty();
    }

    @Test
    public void hintIsNotLoggedWhenSkipped() {
        BuildMojo mojo = mojo("build");
        mojo.skipExtensionsHint = true;

        mojo.hintEnableExtensions();

        assertThat(warnings(mojo)).isEmpty();
    }

    private static BuildMojo mojo(String goal) {
        BuildMojo mojo = new BuildMojo();
        MojoDescriptor descriptor = new MojoDescriptor();
        descriptor.setGoal(goal);
        mojo.mojoExecution = new MojoExecution(descriptor);
        mojo.bootstrapSessionListener = new BootstrapSessionListener(null, null, null);
        mojo.setLog(new CapturingLog());
        return mojo;
    }

    private static List<CharSequence> warnings(BuildMojo mojo) {
        return ((CapturingLog) mojo.getLog()).warnings;
    }

    private static class CapturingLog extends SystemStreamLog {

        private final List<CharSequence> warnings = new ArrayList<>();

        @Override
        public void warn(CharSequence content) {
            warnings.add(content);
        }
    }
}
