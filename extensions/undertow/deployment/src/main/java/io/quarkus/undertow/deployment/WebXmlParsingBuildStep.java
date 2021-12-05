package io.quarkus.undertow.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.metadata.merge.web.spec.WebCommonMetaDataMerger;
import org.jboss.metadata.parser.servlet.WebFragmentMetaDataParser;
import org.jboss.metadata.parser.servlet.WebMetaDataParser;
import org.jboss.metadata.parser.util.MetaDataElementParser;
import org.jboss.metadata.parser.util.NoopXMLResolver;
import org.jboss.metadata.property.PropertyReplacers;
import org.jboss.metadata.web.spec.FilterMetaData;
import org.jboss.metadata.web.spec.ListenerMetaData;
import org.jboss.metadata.web.spec.ServletMetaData;
import org.jboss.metadata.web.spec.WebFragmentMetaData;
import org.jboss.metadata.web.spec.WebMetaData;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalApplicationArchiveMarkerBuildItem;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;

/**
 * Build step that handles web.xml and web-fragment.xml parsing
 */
public class WebXmlParsingBuildStep {

    public static final String WEB_XML = "META-INF/web.xml";
    private static final String WEB_FRAGMENT_XML = "META-INF/web-fragment.xml";

    @BuildStep
    List<HotDeploymentWatchedFileBuildItem> configFile() {
        return Arrays.asList(new HotDeploymentWatchedFileBuildItem(WEB_XML),
                new HotDeploymentWatchedFileBuildItem(WEB_FRAGMENT_XML));
    }

    @BuildStep
    AdditionalApplicationArchiveMarkerBuildItem marker() {
        return new AdditionalApplicationArchiveMarkerBuildItem(WEB_FRAGMENT_XML);
    }

    @BuildStep
    WebMetadataBuildItem createWebMetadata(ApplicationArchivesBuildItem applicationArchivesBuildItem,
            Consumer<AdditionalBeanBuildItem> additionalBeanBuildItemConsumer) throws Exception {

        WebMetaData result;
        Path webXml = applicationArchivesBuildItem.getRootArchive().getChildPath(WEB_XML);
        if (webXml != null) {
            Set<String> additionalBeans = new HashSet<>();

            final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            inputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            MetaDataElementParser.DTDInfo dtdInfo = new MetaDataElementParser.DTDInfo();
            inputFactory.setXMLResolver(dtdInfo);
            try (InputStream in = Files.newInputStream(webXml)) {
                final XMLStreamReader xmlReader = inputFactory.createXMLStreamReader(in);
                result = WebMetaDataParser.parse(xmlReader, dtdInfo,
                        PropertyReplacers.resolvingReplacer(new MPConfigPropertyResolver()));
            }
            if (result.getServlets() != null) {
                for (ServletMetaData i : result.getServlets()) {
                    additionalBeans.add(i.getServletClass());
                }
            }
            if (result.getFilters() != null) {
                for (FilterMetaData i : result.getFilters()) {
                    additionalBeans.add(i.getFilterClass());
                }
            }
            if (result.getListeners() != null) {
                for (ListenerMetaData i : result.getListeners()) {
                    additionalBeans.add(i.getListenerClass());
                }
            }
            additionalBeanBuildItemConsumer
                    .accept(AdditionalBeanBuildItem.builder().setUnremovable().addBeanClasses(additionalBeans).build());
        } else {
            result = new WebMetaData();
        }
        List<WebFragmentMetaData> webFragments = parseWebFragments(applicationArchivesBuildItem);
        for (WebFragmentMetaData webFragment : webFragments) {
            //merge in any web fragments
            //at the moment this is fairly simplistic, as it does not handle all the ordering and metadata complete bits
            //of the spec. I am not sure how important this is, and it is very complex and does not 100% map to the quarkus
            //deployment model. If there is demand for it we can look at adding it later

            WebCommonMetaDataMerger.augment(result, webFragment, null, false);
        }

        return new WebMetadataBuildItem(result);
    }

    /**
     * parse web-fragment.xml
     */
    private List<WebFragmentMetaData> parseWebFragments(ApplicationArchivesBuildItem applicationArchivesBuildItem) {
        List<WebFragmentMetaData> webFragments = new ArrayList<>();
        for (ApplicationArchive archive : applicationArchivesBuildItem.getAllApplicationArchives()) {
            Path webFragment = archive.getChildPath(WEB_FRAGMENT_XML);
            if (webFragment != null && Files.isRegularFile(webFragment)) {
                try (InputStream is = Files.newInputStream(webFragment)) {
                    final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
                    inputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
                    inputFactory.setXMLResolver(NoopXMLResolver.create());
                    XMLStreamReader xmlReader = inputFactory.createXMLStreamReader(is);

                    WebFragmentMetaData webFragmentMetaData = WebFragmentMetaDataParser.parse(xmlReader,
                            PropertyReplacers.resolvingReplacer(new MPConfigPropertyResolver()));
                    webFragments.add(webFragmentMetaData);

                } catch (XMLStreamException e) {
                    throw new RuntimeException("Failed to parse " + webFragment + " " + e.getLocation(), e);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to parse " + webFragment, e);
                }
            }
        }
        return webFragments;
    }

}
