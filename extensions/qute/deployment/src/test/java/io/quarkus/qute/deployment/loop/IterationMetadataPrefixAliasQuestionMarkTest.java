package io.quarkus.qute.deployment.loop;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class IterationMetadataPrefixAliasQuestionMarkTest extends IterationMetadataPrefixTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset("{#for i in total}{i?count}:{i}{#if i?hasNext}::{/if}{/for}"),
                            "templates/loop.html"))
            .overrideConfigKey("quarkus.qute.iteration-metadata-prefix", "<alias?>");

}
