package io.quarkus.tck.config;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.asset.ArchiveAsset;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * Arquillian automatically adds the test class to a test deployment. However, some MP Config tests do add the test class to a
 * library jar too. As a result, we get ambiguous dependency when trying to instantiate the test class. Unfortunately, we can't
 * use an ApplicationArchiveProcessor to remove the test class added by Arquillian because it's applied
 * before the test class is added. Therefore, we deciced to remove the test class from any library jar.
 */
public class ConfigApplicationArchiveProcessor implements ApplicationArchiveProcessor {

    private static final ArchivePath PATH_LIBRARY = ArchivePaths.create("WEB-INF/lib");

    @Override
    public void process(Archive<?> applicationArchive, TestClass testClass) {
        if (applicationArchive instanceof WebArchive) {
            WebArchive war = applicationArchive.as(WebArchive.class);
            Node libNode = war.get(PATH_LIBRARY);
            if (libNode != null) {
                for (Node child : libNode.getChildren()) {
                    Asset childAsset = child.getAsset();
                    if (childAsset instanceof ArchiveAsset) {
                        ArchiveAsset archiveAsset = (ArchiveAsset) childAsset;
                        if (archiveAsset.getArchive() instanceof JavaArchive) {
                            JavaArchive libArchive = (JavaArchive) archiveAsset.getArchive();
                            libArchive.deleteClass(testClass.getName());
                            // Remove inner classes
                            libArchive.getContent().keySet().stream()
                                    .filter(archivePath -> archivePath.get()
                                            .contains(testClass.getJavaClass().getSimpleName() + "$"))
                                    .forEach(libArchive::delete);
                        }
                    }
                }
            }
        }
    }

}
