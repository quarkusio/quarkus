package io.quarkus.platform.descriptor.resolver.json.test;

import static io.quarkus.platform.tools.ToolsConstants.IO_QUARKUS;
import static io.quarkus.platform.tools.ToolsConstants.QUARKUS_CORE_ARTIFACT_ID;
import static io.quarkus.platform.tools.ToolsConstants.DEFAULT_PLATFORM_BOM_ARTIFACT_ID;
import static io.quarkus.platform.tools.ToolsConstants.DEFAULT_PLATFORM_BOM_GROUP_ID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.resolver.ResolverSetupCleanup;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsDependency;
import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.descriptor.resolver.json.QuarkusJsonPlatformDescriptorResolver;
import io.quarkus.platform.tools.DefaultMessageWriter;
import io.quarkus.platform.tools.MessageWriter;
import io.quarkus.platform.tools.ToolsConstants;

public class QuarkusJsonPlatformDescriptorResolverTest extends ResolverSetupCleanup {

    private static Path testDir;

    private MessageWriter log = new DefaultMessageWriter();

    @BeforeEach
    @Override
    public void setup() throws Exception {
        if(workDir != null) {
            return;
        }
        super.setup();
        testDir = workDir;
        doSetup();
    }

    @Override
    protected boolean cleanWorkDir() {
        return false;
    }

    @AfterAll
    public static void afterAll() throws Exception {
        if(testDir != null) {
            IoUtils.recursiveDelete(testDir);
        }
    }

    protected void doSetup() throws Exception {

        final TsArtifact quarkusCore = new TsArtifact(IO_QUARKUS, QUARKUS_CORE_ARTIFACT_ID, null, "jar", "1.0.0.CR50");
        install(quarkusCore, newJar().getPath(workDir));

        final TsArtifact quarkusPlatformDescriptorJson = new TsArtifact(IO_QUARKUS, "quarkus-platform-descriptor-json", null, "jar", "1.0.0.CR50");
        install(quarkusPlatformDescriptorJson, newJar().getPath(workDir));

        // install a few universe versions with the default GA
        installDefaultUniverse(quarkusCore, "1.0.0.CR50");
        installDefaultUniverse(quarkusCore, "1.0.0.CR60");
        installDefaultUniverse(quarkusCore, "1.0.0.CR70");

        // install a universe with a custom GA and JSON descriptor with `-descriptor-json` suffix
        TsArtifact universeBom = new TsArtifact(DEFAULT_PLATFORM_BOM_GROUP_ID, "other-universe", null, "pom", "1.0.0.CR80")
                .addManagedDependency(new TsDependency(quarkusCore));
        install(universeBom);
        final TsArtifact universeJson = new TsArtifact(DEFAULT_PLATFORM_BOM_GROUP_ID, "other-universe" + "-descriptor-json", null, "json", "1.0.0.CR80")
                .setContent(new TestPlatformJsonDescriptorProvider(universeBom));
        install(universeJson);
    }

    @Test
    public void testResolveBundled() throws Exception {
        final QuarkusPlatformDescriptor platform = newResolver().resolveBundled();
        assertBundledPlatform(platform, "1.0.0.CR90");
        assertEquals("1.0.0.CR90", platform.getQuarkusVersion());
    }

    @Test
    public void testResolve() throws Exception {
        final QuarkusPlatformDescriptor platform = newResolver().resolve();
        assertDefaultPlatform(platform, "1.0.0.CR70");
        assertEquals("1.0.0.CR50", platform.getQuarkusVersion());
    }

    @Test
    public void testResolveFromJsonVersion() throws Exception {
        final QuarkusPlatformDescriptor platform = newResolver().resolveFromJson(DEFAULT_PLATFORM_BOM_GROUP_ID, DEFAULT_PLATFORM_BOM_ARTIFACT_ID, "1.0.0.CR60");
        assertDefaultPlatform(platform, "1.0.0.CR60");
        assertEquals("1.0.0.CR50", platform.getQuarkusVersion());
    }

    @Test
    public void testResolveFromJsonFile() throws Exception {
        final Path jsonPath = resolver.resolve(new AppArtifact(DEFAULT_PLATFORM_BOM_GROUP_ID, DEFAULT_PLATFORM_BOM_ARTIFACT_ID, null, "json", "1.0.0.CR50"));
        final QuarkusPlatformDescriptor platform = newResolver().resolveFromJson(jsonPath);
        assertDefaultPlatform(platform, "1.0.0.CR50");
        assertEquals("1.0.0.CR50", platform.getQuarkusVersion());
    }

    @Test
    public void testResolveFromBomWithDescriptorJsonPrefix() throws Exception {
        final QuarkusPlatformDescriptor platform = newResolver().resolveFromBom(DEFAULT_PLATFORM_BOM_GROUP_ID, "other-universe", "1.0.0.CR80");
        assertNotNull(platform);
        assertEquals(ToolsConstants.IO_QUARKUS, platform.getBomGroupId());
        assertEquals("other-universe", platform.getBomArtifactId());
        assertEquals("1.0.0.CR80", platform.getBomVersion());
        assertEquals("1.0.0.CR50", platform.getQuarkusVersion());
    }

    @Test
    public void testResolveFromJsonWithDescriptorJsonPrefix() throws Exception {
        final QuarkusPlatformDescriptor platform = newResolver().resolveLatestFromJson(DEFAULT_PLATFORM_BOM_GROUP_ID, "other-universe" + "-descriptor-json", null);
        assertNotNull(platform);
        assertEquals(ToolsConstants.IO_QUARKUS, platform.getBomGroupId());
        assertEquals("other-universe", platform.getBomArtifactId());
        assertEquals("1.0.0.CR80", platform.getBomVersion());
        assertEquals("1.0.0.CR50", platform.getQuarkusVersion());
    }

    @Test
    public void testResolveFromLatestJson() throws Exception {
        final QuarkusPlatformDescriptor platform = newResolver().resolveLatestFromJson(DEFAULT_PLATFORM_BOM_GROUP_ID, DEFAULT_PLATFORM_BOM_ARTIFACT_ID, null);
        assertDefaultPlatform(platform, "1.0.0.CR70");
        assertEquals("1.0.0.CR50", platform.getQuarkusVersion());
    }

    @Test
    public void testResolveFromLatestJsonWithRange() throws Exception {
        final QuarkusPlatformDescriptor platform = newResolver().resolveLatestFromJson(DEFAULT_PLATFORM_BOM_GROUP_ID, DEFAULT_PLATFORM_BOM_ARTIFACT_ID, "[0,1.0.0.CR60]");
        assertDefaultPlatform(platform, "1.0.0.CR60");
        assertEquals("1.0.0.CR50", platform.getQuarkusVersion());
    }

    @Test
    public void testResolveFromLatestBom() throws Exception {
        final QuarkusPlatformDescriptor platform = newResolver().resolveLatestFromBom(DEFAULT_PLATFORM_BOM_GROUP_ID, DEFAULT_PLATFORM_BOM_ARTIFACT_ID, null);
        assertDefaultPlatform(platform, "1.0.0.CR70");
        assertEquals("1.0.0.CR50", platform.getQuarkusVersion());
    }

    @Test
    public void testResolveFromLatestBomWithRange() throws Exception {
        final QuarkusPlatformDescriptor platform = newResolver().resolveLatestFromJson(DEFAULT_PLATFORM_BOM_GROUP_ID, DEFAULT_PLATFORM_BOM_ARTIFACT_ID, "[0,1.0.0.CR60]");
        assertDefaultPlatform(platform, "1.0.0.CR60");
        assertEquals("1.0.0.CR50", platform.getQuarkusVersion());
    }

    private void installDefaultUniverse(final TsArtifact quarkusCore, String platformVersion) {
        final TsArtifact universeBom = new TsArtifact(DEFAULT_PLATFORM_BOM_GROUP_ID, DEFAULT_PLATFORM_BOM_ARTIFACT_ID, null, "pom", platformVersion)
                .addManagedDependency(new TsDependency(quarkusCore));
        install(universeBom);
        final TsArtifact universeJson = new TsArtifact(DEFAULT_PLATFORM_BOM_GROUP_ID, DEFAULT_PLATFORM_BOM_ARTIFACT_ID, null, "json", platformVersion)
                .setContent(new TestPlatformJsonDescriptorProvider(universeBom));
        install(universeJson);
    }

    private QuarkusJsonPlatformDescriptorResolver newResolver() {
        return QuarkusJsonPlatformDescriptorResolver.newInstance()
                .setMessageWriter(log)
                .setArtifactResolver(resolver);
    }

    private static void assertDefaultPlatform(QuarkusPlatformDescriptor platform, String version) {
        assertNotNull(platform);
        assertEquals(ToolsConstants.IO_QUARKUS, platform.getBomGroupId());
        assertEquals(ToolsConstants.DEFAULT_PLATFORM_BOM_ARTIFACT_ID, platform.getBomArtifactId());
        assertEquals(version, platform.getBomVersion());
    }

    private static void assertBundledPlatform(QuarkusPlatformDescriptor platform, String version) {
        assertNotNull(platform);
        assertEquals(ToolsConstants.IO_QUARKUS, platform.getBomGroupId());
        assertEquals("quarkus-bom", platform.getBomArtifactId());
        assertEquals(version, platform.getBomVersion());
    }
}
