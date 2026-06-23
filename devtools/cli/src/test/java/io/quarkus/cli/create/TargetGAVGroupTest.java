package io.quarkus.cli.create;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.devtools.commands.CreateProjectHelper;
import io.quarkus.quickcli.CommandLine.TypeConversionException;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;

@QuarkusMainTest
public class TargetGAVGroupTest {

    TargetGAVGroup gav = new TargetGAVGroup();

    // quarkus create -x micrometer,opentelemetry g:a:v

    // Where g:a:v could be any of:

    //     artifact-id or :artifact-id: -- use defaults for group id and version (common/demo)
    //     my-group:artifact-id or my-group:artifact-id: -- use default version (common/real project)
    //     my-group:artifact-id:version -- specify all (real project)
    //     :artifact-id:version -- how common would this be!?
    //     my-group:: -- use defaults for artifact id and version (rare)

    @Test
    void testArtifactId(QuarkusMainLauncher launcher) {
        gav.gav = "a";

        Assertions.assertEquals(CreateProjectHelper.DEFAULT_GROUP_ID, gav.getGroupId());
        Assertions.assertEquals("a", gav.getArtifactId());
        Assertions.assertEquals(CreateProjectHelper.DEFAULT_VERSION, gav.getVersion());
    }

    @Test
    void testArtifactIdLonger(QuarkusMainLauncher launcher) {
        gav.gav = ":a:";

        Assertions.assertEquals(CreateProjectHelper.DEFAULT_GROUP_ID, gav.getGroupId());
        Assertions.assertEquals("a", gav.getArtifactId());
        Assertions.assertEquals(CreateProjectHelper.DEFAULT_VERSION, gav.getVersion());
    }

    @Test
    void testGroupIdArtifactId(QuarkusMainLauncher launcher) {
        gav.gav = "g:a";

        Assertions.assertEquals("g", gav.getGroupId());
        Assertions.assertEquals("a", gav.getArtifactId());
        Assertions.assertEquals(CreateProjectHelper.DEFAULT_VERSION, gav.getVersion());
    }

    @Test
    void testGroupIdArtifactIdLonger(QuarkusMainLauncher launcher) {
        gav.gav = "g:a:";

        Assertions.assertEquals("g", gav.getGroupId());
        Assertions.assertEquals("a", gav.getArtifactId());
        Assertions.assertEquals(CreateProjectHelper.DEFAULT_VERSION, gav.getVersion());
    }

    @Test
    void testGroupIdArtifactIdVersion(QuarkusMainLauncher launcher) {
        gav.gav = "g:a:v";

        Assertions.assertEquals("g", gav.getGroupId());
        Assertions.assertEquals("a", gav.getArtifactId());
        Assertions.assertEquals("v", gav.getVersion());
    }

    @Test
    void testArtifactIdVersion(QuarkusMainLauncher launcher) {
        gav.gav = ":a:v";

        Assertions.assertEquals(CreateProjectHelper.DEFAULT_GROUP_ID, gav.getGroupId());
        Assertions.assertEquals("a", gav.getArtifactId());
        Assertions.assertEquals("v", gav.getVersion());
    }

    @Test
    void testGroupId(QuarkusMainLauncher launcher) {
        gav.gav = "g::";

        Assertions.assertEquals("g", gav.getGroupId());
        Assertions.assertEquals(CreateProjectHelper.DEFAULT_ARTIFACT_ID, gav.getArtifactId());
        Assertions.assertEquals(CreateProjectHelper.DEFAULT_VERSION, gav.getVersion());
    }

    @Test
    void testOldParameters(QuarkusMainLauncher launcher) {
        gav.groupId = "g";
        gav.artifactId = "a";
        gav.version = "v";

        Assertions.assertEquals("g", gav.getGroupId());
        Assertions.assertEquals("a", gav.getArtifactId());
        Assertions.assertEquals("v", gav.getVersion());
    }

    @Test
    void testBadArtifactId(QuarkusMainLauncher launcher) {
        gav.gav = "g:a/x:v";
        Assertions.assertThrows(TypeConversionException.class, () -> gav.getArtifactId());
    }

    @Test
    void testBadGroupId(QuarkusMainLauncher launcher) {
        gav.gav = "g,x:a:v";
        Assertions.assertThrows(TypeConversionException.class, () -> gav.getGroupId());
    }
}
