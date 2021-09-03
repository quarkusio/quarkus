package io.quarkus.cli.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.cli.Version;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.platform.tools.ToolsConstants;
import io.quarkus.registry.catalog.PlatformStreamCoords;

public class TargetQuarkusVersionGroupTest {
    final static String clientVersion = Version.clientVersion();

    TargetQuarkusVersionGroup qvg = new TargetQuarkusVersionGroup();

    @Test
    void testPlatformFullyQualified() {
        qvg.setPlatformBom("io.something:custom-bom:1.3.0.Final");

        ArtifactCoords coords = qvg.getPlatformBom();
        Assertions.assertEquals("io.something", coords.getGroupId());
        Assertions.assertEquals("custom-bom", coords.getArtifactId());
        Assertions.assertEquals("1.3.0.Final", coords.getVersion());
        Assertions.assertEquals("io.something:custom-bom:1.3.0.Final", qvg.validPlatformBom);
    }

    @Test
    void testPlatformUseDefaultArtifactVersion() {
        qvg.setPlatformBom("just.group::");

        ArtifactCoords coords = qvg.getPlatformBom();
        Assertions.assertEquals("just.group", coords.getGroupId());
        Assertions.assertEquals(ToolsConstants.DEFAULT_PLATFORM_BOM_ARTIFACT_ID, coords.getArtifactId());
        Assertions.assertEquals(clientVersion, coords.getVersion());
        Assertions.assertEquals("just.group:" + ToolsConstants.DEFAULT_PLATFORM_BOM_ARTIFACT_ID + ":" + clientVersion,
                qvg.validPlatformBom);
    }

    @Test
    void testPlatformUseDefaultArtifact() {
        qvg.setPlatformBom("group::version");

        ArtifactCoords coords = qvg.getPlatformBom();
        Assertions.assertEquals("group", coords.getGroupId());
        Assertions.assertEquals(ToolsConstants.DEFAULT_PLATFORM_BOM_ARTIFACT_ID, coords.getArtifactId());
        Assertions.assertEquals("version", coords.getVersion());
        Assertions.assertEquals("group:" + ToolsConstants.DEFAULT_PLATFORM_BOM_ARTIFACT_ID + ":version",
                qvg.validPlatformBom);
    }

    @Test
    void testPlatformUseDefaultGroupVersion() {
        qvg.setPlatformBom(":artifact:");

        ArtifactCoords coords = qvg.getPlatformBom();
        Assertions.assertEquals(ToolsConstants.DEFAULT_PLATFORM_BOM_GROUP_ID, coords.getGroupId());
        Assertions.assertEquals("artifact", coords.getArtifactId());
        Assertions.assertEquals(clientVersion, coords.getVersion());
        Assertions.assertEquals(ToolsConstants.DEFAULT_PLATFORM_BOM_GROUP_ID + ":artifact:" + clientVersion,
                qvg.validPlatformBom);
    }

    @Test
    void testStreamUseDFullyQualified() {
        qvg.setStream("stream-platform:stream-version");

        PlatformStreamCoords coords = qvg.getStream();
        Assertions.assertEquals("stream-platform", coords.getPlatformKey());
        Assertions.assertEquals("stream-version", coords.getStreamId());
    }

    @Test
    void testStreamUseDefaultPlatformKey() {
        qvg.setStream(":stream-version");

        PlatformStreamCoords coords = qvg.getStream();
        Assertions.assertNull(coords.getPlatformKey());
        Assertions.assertEquals("stream-version", coords.getStreamId());
    }

    @Test
    void testStreamUseDefaultStreamId() {
        qvg.setStream("stream-platform:");

        PlatformStreamCoords coords = qvg.getStream();
        Assertions.assertEquals("stream-platform", coords.getPlatformKey());
        Assertions.assertEquals("", coords.getStreamId());
    }

}
