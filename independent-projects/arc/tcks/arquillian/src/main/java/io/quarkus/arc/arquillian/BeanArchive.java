package io.quarkus.arc.arquillian;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.asset.Asset;

final class BeanArchive {
    final Set<String> classes; // values are "com/example/MyClass.class"

    private BeanArchive(Set<String> classes) {
        this.classes = classes;
    }

    static BeanArchive detect(Archive<?> archive) throws IOException {
        Map<ArchivePath, Node> beansXmlMap = archive.getContent(Filters.include(".*/beans.xml"));
        if (beansXmlMap.isEmpty()) {
            return null;
        }
        if (beansXmlMap.size() > 1) {
            throw new IllegalStateException("Archive contains multiple beans.xml files: " + archive);
        }

        Map.Entry<ArchivePath, Node> beansXmlEntry = beansXmlMap.entrySet().iterator().next();
        if (!isBeanArchive(beansXmlEntry.getValue().getAsset())) {
            return null;
        }

        String classesPrefix;

        String beansXmlPath = beansXmlEntry.getKey().get();
        if (beansXmlPath.endsWith("/META-INF/beans.xml")) {
            classesPrefix = beansXmlPath.replace("/META-INF/beans.xml", "/");
        } else if (beansXmlPath.endsWith("/WEB-INF/beans.xml")) {
            classesPrefix = beansXmlPath.replace("/WEB-INF/beans.xml", "/WEB-INF/classes/");
        } else {
            throw new IllegalStateException("Invalid beans.xml location: " + beansXmlPath);
        }

        Set<String> beanClasses = new HashSet<>();
        String classesPrefixPattern = Pattern.quote(classesPrefix);
        for (ArchivePath path : archive.getContent(Filters.include("^" + classesPrefixPattern + ".*\\.class$")).keySet()) {
            beanClasses.add(path.get().replaceFirst(classesPrefixPattern, ""));
        }

        return new BeanArchive(beanClasses);
    }

    private static boolean isBeanArchive(Asset beansXml) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (InputStream in = beansXml.openStream()) {
            in.transferTo(bytes);
        }
        String content = bytes.toString(StandardCharsets.UTF_8);

        if (content.trim().isEmpty()) {
            return true;
        }

        if (content.contains("bean-discovery-mode='annotated'")
                || content.contains("bean-discovery-mode=\"annotated\"")) {
            return true;
        }

        if (content.contains("bean-discovery-mode='none'")
                || content.contains("bean-discovery-mode=\"none\"")) {
            return false;
        }

        if (content.contains("bean-discovery-mode='all'")
                || content.contains("bean-discovery-mode=\"all\"")) {
            throw new IllegalStateException("Bean discovery mode of 'all' not supported in CDI Lite");
        }

        // bean discovery mode not present, defaults to `annotated`
        return true;
    }
}
