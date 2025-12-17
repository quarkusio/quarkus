package io.quarkus.tck.lra;

import org.eclipse.microprofile.lra.tck.TckTestBase;
import org.eclipse.microprofile.lra.tck.participant.api.NonParticipatingTckResource;
import org.eclipse.microprofile.lra.tck.participant.api.ResourceParent;
import org.eclipse.microprofile.lra.tck.service.spi.LRARecoveryService;
import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

public class DeploymentProcessor implements ApplicationArchiveProcessor {

    @Override
    public void process(Archive<?> archive, TestClass testClass) {
        if (archive instanceof WebArchive) {
            WebArchive war = (WebArchive) archive;

            war.addAsServiceProvider(LRARecoveryService.class,
                    NarayanaLRARecovery.class);
            war.addPackages(false,
                    "org.eclipse.microprofile.lra");
            war.addClasses(TckTestBase.class, NarayanaLRARecovery.class, NonParticipatingTckResource.class,
                    ResourceParent.class, BaseURLProvider.class);
        }

    }
}
