package io.quarkus.devtools.project.create;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;

import io.quarkus.devtools.testing.registry.client.TestRegistryClientBuilder;

public class RecommendStreamsFromScenarioBase extends MultiplePlatformBomsTestBase {
    protected static final String UPSTREAM_PLATFORM_KEY = "io.upstream.platform";
    protected static final String DOWNSTREAM_PLATFORM_KEY = "io.downstream.platform";

    @BeforeEach
    public void setup() throws Exception {
        TestRegistryClientBuilder registryClientBuilder = TestRegistryClientBuilder.newInstance()
                //.debug()
                .baseDir(configDir());

        var downstreamRegistry = registryClientBuilder
                // registry
                .newRegistry("downstream.registry.test")
                .recognizedQuarkusVersions("*downstream");
        setDownstreamRegistryExtraOptions(downstreamRegistry);

        downstreamRegistry
                // platform key
                .newPlatform(DOWNSTREAM_PLATFORM_KEY)
                .newStream("2.2")
                // 2.2.2 release
                .newRelease("2.2.2.downstream")
                .quarkusVersion("2.2.2.downstream")
                .upstreamQuarkusVersion("2.2.2")
                // default bom including quarkus-core + essential metadata
                .addCoreMember().release()
                // platform member
                .newMember("acme-a-bom")
                .addExtensionWithMetadata("io.acme", "ext-a", "2.2.2.downstream",
                        Map.of("offering-a-support", List.of("supported")))
                .release()
                .newMember("acme-b-bom")
                .addExtensionWithMetadata("io.acme", "ext-b", "2.2.2.downstream",
                        Map.of("offering-b-support", List.of("supported")))
                .release().stream().platform()
                .newStream("1.1")
                // 1.1.1 release
                .newRelease("1.1.1.downstream")
                .quarkusVersion("1.1.1.downstream")
                .upstreamQuarkusVersion("1.1.1")
                // default bom including quarkus-core + essential metadata
                .addCoreMember().release()
                // platform member
                .newMember("acme-a-bom")
                .addExtensionWithMetadata("io.acme", "ext-a", "1.1.1.downstream",
                        Map.of("offering-a-support", List.of("supported")))
                .release()
                .newMember("acme-b-bom")
                .addExtensionWithMetadata("io.acme", "ext-b", "1.1.1.downstream",
                        // on purpose included in offering-a
                        Map.of("offering-a-support", List.of("supported")));

        var upstreamRegistry = registryClientBuilder.newRegistry("upstream.registry.test");
        setUpstreamRegistryExtraOptions(upstreamRegistry);

        upstreamRegistry
                // platform key
                .newPlatform(UPSTREAM_PLATFORM_KEY)
                // 3.3 STREAM
                .newStream("3.3")
                // 3.3.3 release
                .newRelease("3.3.3")
                .quarkusVersion("3.3.3")
                // default bom including quarkus-core + essential metadata
                .addCoreMember().release()
                .newMember("acme-a-bom").addExtension("io.acme", "ext-a", "3.3.3").release()
                .newMember("acme-b-bom").addExtension("io.acme", "ext-b", "3.3.3").release()
                .stream().platform()
                // 2.2 STREAM
                .newStream("2.2")
                // 2.2.2 release
                .newRelease("2.2.2")
                .quarkusVersion("2.2.2")
                // default bom including quarkus-core + essential metadata
                .addCoreMember().release()
                .newMember("acme-a-bom").addExtension("io.acme", "ext-a", "2.2.2").release()
                .newMember("acme-b-bom").addExtension("io.acme", "ext-b", "2.2.2").release()
                .stream().platform()
                // 1.1 STREAM
                .newStream("1.1")
                // 1.1.1 release
                .newRelease("1.1.1")
                .quarkusVersion("1.1.1")
                // default bom including quarkus-core + essential metadata
                .addCoreMember().release()
                .newMember("acme-a-bom").addExtension("io.acme", "ext-a", "1.1.1").release()
                .newMember("acme-b-bom").addExtension("io.acme", "ext-b", "1.1.1");

        registryClientBuilder.build();
        enableRegistryClient();
    }

    protected void setDownstreamRegistryExtraOptions(TestRegistryClientBuilder.TestRegistryBuilder registry) {
    }

    protected void setUpstreamRegistryExtraOptions(TestRegistryClientBuilder.TestRegistryBuilder registry) {
    }

    protected String getMainPlatformKey() {
        return DOWNSTREAM_PLATFORM_KEY;
    }
}
