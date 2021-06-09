package io.quarkus.extest.deployment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.atteo.xmlcombiner.XmlCombiner;
import org.xml.sax.SAXException;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.UberJarIgnoredResourceBuildItem;
import io.quarkus.deployment.pkg.builditem.UberJarMergedResourceBuildItem;

/**
 * Used in UberJarMergedResourceBuildItemTest
 */
public class UberJarConfigBuildStep {

    @BuildStep
    UberJarMergedResourceBuildItem uberJarMergedResourceBuildItem() {
        return new UberJarMergedResourceBuildItem("META-INF/cxf/bus-extensions.txt");
    }

    @BuildStep
    void uberJarMergedResourceBuildItem(BuildProducer<GeneratedResourceBuildItem> generatedResourcesProducer,
            PackageConfig packageConfig) {
        if (packageConfig.isUberJar()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                XmlCombiner combiner = new XmlCombiner();
                List<URL> resources = Collections
                        .list(getClass().getClassLoader().getResources("META-INF/wsdl.plugin.xml"));
                for (URL resource : resources) {
                    try (InputStream is = resource.openStream()) {
                        combiner.combine(is);
                    }
                }
                combiner.buildDocument(baos);
            } catch (ParserConfigurationException | SAXException | TransformerException | IOException e) {
                e.printStackTrace();
            }
            generatedResourcesProducer.produce(new GeneratedResourceBuildItem("META-INF/wsdl.plugin.xml", baos.toByteArray()));
        }
    }

    @BuildStep
    UberJarIgnoredResourceBuildItem uberJarIgnoredResourceBuildItem() {
        return new UberJarIgnoredResourceBuildItem("META-INF/cxf/cxf.fixml");
    }

}
