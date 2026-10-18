package io.quarkus.arc.tck;

import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.cdi.lang.model.tck.LangModelVerifier;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class LangModelTest {
    @Deployment
    public static Archive<?> deployment() {
        return ShrinkWrap.create(JavaArchive.class)
                .addPackage(LangModelVerifier.class.getPackage())
                .addClasses(LangModelTest.class, LangModelTckExtension.class)
                .addAsServiceProvider(BuildCompatibleExtension.class, LangModelTckExtension.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    public void test() {
        // the test itself runs in LangModelTckExtension
        // and if it fails, deployment fails
    }
}
