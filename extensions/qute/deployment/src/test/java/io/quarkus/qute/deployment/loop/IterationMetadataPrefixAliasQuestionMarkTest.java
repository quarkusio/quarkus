package io.quarkus.qute.deployment.loop;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class IterationMetadataPrefixAliasQuestionMarkTest extends IterationMetadataPrefixTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset("{#for i in total}{i?count}:{i}{#if i?hasNext}::{/if}{/for}"),
                            "templates/loop.html"))
            .overrideConfigKey("quarkus.qute.iteration-metadata-prefix", "<alias?>");

}
