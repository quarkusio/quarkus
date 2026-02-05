package io.quarkus.modular.spi;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.constant.ClassDesc;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.logging.Logger;

import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.modular.spi.model.AutoDependencyGroup;
import io.quarkus.modular.spi.model.DependencyInfo;
import io.quarkus.modular.spi.model.ModuleInfo;
import io.quarkus.paths.PathTree;
import io.smallrye.classfile.Annotation;
import io.smallrye.classfile.AnnotationElement;
import io.smallrye.classfile.AnnotationValue;
import io.smallrye.classfile.ClassFile;
import io.smallrye.classfile.attribute.ModuleAttribute;
import io.smallrye.classfile.attribute.ModuleMainClassAttribute;
import io.smallrye.classfile.attribute.ModulePackagesAttribute;
import io.smallrye.classfile.attribute.RuntimeInvisibleAnnotationsAttribute;
import io.smallrye.classfile.extras.constant.ModuleDesc;
import io.smallrye.classfile.extras.constant.PackageDesc;
import io.smallrye.classfile.extras.reflect.AccessFlag;
import io.smallrye.common.constraint.Assert;
import io.smallrye.common.resource.Resource;
import io.smallrye.modules.desc.Dependency;
import io.smallrye.modules.desc.ModuleDescriptor;
import io.smallrye.modules.desc.PackageAccess;
import io.smallrye.modules.desc.PackageInfo;

/**
 * Utility to help write patched and curated module files to a destination path.
 */
public final class ModuleWriter {
    private static final Logger log = Logger.getLogger("io.quarkus.modular.spi");

    private ModuleWriter() {
    }

    /**
     * Write a patched module to the given output directory.
     *
     * @param moduleInfo the module information (must not be {@code null})
     * @param outputDirectory the output directory (must not be {@code null})
     * @param manifest the JAR manifest (must not be {@code null})
     * @param bootModule {@code true} if this module is a boot module (and thus requires a {@code module-info.class})
     * @return the path of the written module
     * @throws IOException if writing fails for some reason
     */
    public static Path writeModule(ModuleInfo moduleInfo, Path outputDirectory, Manifest manifest, final boolean bootModule)
            throws IOException {
        Assert.checkNotNullParam("moduleInfo", moduleInfo);
        Assert.checkNotNullParam("outputDirectory", outputDirectory);
        Assert.checkNotNullParam("manifest", manifest);
        log.debugf("Writing module %s", moduleInfo.name());
        ResolvedDependency artifact = moduleInfo.resolvedArtifact();
        // get an index of all generated/transformed resources
        Map<String, Resource> generated = moduleInfo.generated().stream().collect(
                Collectors.toMap(Resource::pathName, Function.identity()));
        // compute the file name from the artifact info
        final String fileName = artifact.getArtifactId() + "-" + artifact.getVersion() + "-patched.jar";
        // Create the artifact
        Files.createDirectories(outputDirectory);
        PathTree contentTree = artifact.getContentTree();
        Set<String> directories = new HashSet<>();
        directories.add("META-INF");
        Path outputPath = outputDirectory.resolve(fileName);
        try (OutputStream fos = Files.newOutputStream(outputPath)) {
            try (JarOutputStream jarOutput = new JarOutputStream(fos)) {
                // start with the Manifest
                try (JarDirEntry dir = new JarDirEntry(jarOutput, "META-INF/")) {
                    // todo: set dates & times based on original resource
                }
                try (JarEntryOutputStream os = new JarEntryOutputStream(jarOutput, "META-INF/MANIFEST.MF")) {
                    manifest.write(os);
                }
                // next, write module descriptor
                if (bootModule) {
                    // descriptor for "normal" module
                    byte[] moduleInfoClass = getModuleInfoClass(moduleInfo);
                    try (JarEntryOutputStream os = new JarEntryOutputStream(jarOutput, "module-info.class", false,
                            moduleInfoClass.length)) {
                        os.write(moduleInfoClass);
                    }
                } else {
                    // write XML for automatic module
                    try (JarEntryOutputStream os = new JarEntryOutputStream(jarOutput, "module.xml")) {
                        try (OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
                            try (BufferedWriter bw = new BufferedWriter(osw)) {
                                XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newDefaultFactory();
                                xmlOutputFactory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, Boolean.TRUE);
                                XMLStreamWriter xml = xmlOutputFactory.createXMLStreamWriter(bw);
                                try (XmlCloser ignored = xml::close) {
                                    writeModuleXml(new FormattingXMLStreamWriter(xml), moduleInfo);
                                }
                            } catch (XMLStreamException e) {
                                throw new IOException(e);
                            }
                        }
                    }
                }
                // write all resources
                for (Resource resource : moduleInfo.generated()) {
                    if (resource.isDirectory()) {
                        continue;
                    }
                    // todo: create parent directories
                    try (JarEntryOutputStream os = new JarEntryOutputStream(jarOutput, resource.pathName(), false,
                            resource.size())) {
                        try (InputStream is = resource.openStream()) {
                            is.transferTo(os);
                        }
                    }
                }
                // now, find and process all files using our own walker
                contentTree.walk(visited -> {
                    String rp = visited.getRelativePath();
                    if (generated.containsKey(rp)) {
                        // skip it until the end
                        return;
                    }
                    BasicFileAttributes attr;
                    try {
                        attr = Files.getFileAttributeView(visited.getPath(), BasicFileAttributeView.class).readAttributes();
                    } catch (IOException e) {
                        throw sneak(e);
                    }
                    if (attr.isDirectory()) {
                        if (rp.isEmpty()) {
                            // skip base directory entry
                            return;
                        }
                        if (directories.add(rp)) {
                            try (JarDirEntry dir = new JarDirEntry(jarOutput, rp + "/")) {
                                dir.setCreationTime(attr.creationTime());
                                dir.setLastModifiedTime(attr.lastModifiedTime());
                                dir.setLastAccessTime(attr.lastAccessTime());
                            } catch (IOException e) {
                                throw sneak(e);
                            }
                        }
                    } else {
                        if (rp.equals("META-INF/MANIFEST.MF") || rp.equals("module-info.class")
                                || rp.endsWith("/module-info.class")) {
                            // skip
                            return;
                        }
                        int idx = rp.lastIndexOf('/');
                        if (idx != -1) {
                            // make sure the directory exists
                            // todo: recurse to parents, extract to method for reuse
                            String dirName = rp.substring(0, idx);
                            if (directories.add(dirName)) {
                                try (JarDirEntry dir = new JarDirEntry(jarOutput, dirName)) {
                                    // todo: default date & time
                                } catch (IOException e) {
                                    throw sneak(e);
                                }
                            }
                        }
                        // copy the content
                        try (JarEntryOutputStream os = new JarEntryOutputStream(jarOutput, rp, false, attr.size())) {
                            os.setCreationTime(attr.creationTime());
                            os.setLastModifiedTime(attr.lastModifiedTime());
                            os.setLastAccessTime(attr.lastAccessTime());
                            Files.copy(visited.getPath(), os);
                        } catch (IOException ioe) {
                            throw sneak(ioe);
                        }
                    }
                });
            }
        } catch (Throwable t) {
            // don't leave a half-written file around
            try {
                Files.delete(outputPath);
            } catch (Throwable t2) {
                t.addSuppressed(t2);
            }
            throw t;
        }
        return outputPath;
    }

    /**
     * Get a {@code module-info.class} for the given module.
     * Note that this might lose some information, so this should only be used when necessary.
     *
     * @param moduleInfo the module information (must not be {@code null})
     * @return the {@code module-info.class} bytes (not {@code null})
     */
    private static byte[] getModuleInfoClass(final ModuleInfo moduleInfo) {
        return ClassFile.of().buildModule(ModuleAttribute.of(
                ModuleDesc.of(moduleInfo.name()),
                mab -> {
                    boolean automatic = moduleInfo.modifiers().contains(ModuleDescriptor.Modifier.AUTOMATIC);
                    if (automatic) {
                        log.warnf("Generating module-info for automatic module %s", moduleInfo.name());
                    }
                    boolean open = automatic || moduleInfo.modifiers().contains(ModuleDescriptor.Modifier.OPEN);
                    int bits = 0;
                    if (open) {
                        bits |= AccessFlag.OPEN.mask();
                    }
                    mab.moduleFlags(bits);
                    String version = moduleInfo.version();
                    if (version != null) {
                        mab.moduleVersion(version);
                    }
                    for (Map.Entry<String, PackageInfo> e : moduleInfo.packages().entrySet()) {
                        String pn = e.getKey();
                        if (pn.isEmpty()) {
                            // skip default package
                            continue;
                        }
                        PackageInfo info = e.getValue();
                        switch (info.packageAccess()) {
                            case EXPORTED -> mab.exports(PackageDesc.of(pn), Set.of(),
                                    info.exportTargets().stream().map(ModuleDesc::of).toArray(ModuleDesc[]::new));
                            case OPEN -> mab.opens(PackageDesc.of(pn), Set.of(),
                                    info.openTargets().stream().map(ModuleDesc::of).toArray(ModuleDesc[]::new));
                        }
                    }
                    if (automatic) {
                        // todo: scrape the module for uses (needs analyzer framework)
                    }
                    for (String used : moduleInfo.uses()) {
                        mab.uses(ClassDesc.of(used));
                    }
                    for (Map.Entry<String, List<String>> e : moduleInfo.provides().entrySet()) {
                        String serviceName = e.getKey();
                        List<String> serviceImpls = e.getValue();
                        mab.provides(ClassDesc.of(serviceName),
                                serviceImpls.stream().map(ClassDesc::of).toArray(ClassDesc[]::new));
                    }
                    for (DependencyInfo depInfo : moduleInfo.dependencies()) {
                        processDep(mab, depInfo);
                    }
                    for (AutoDependencyGroup grp : moduleInfo.autoDependencies()) {
                        for (DependencyInfo depInfo : grp.dependencies()) {
                            processDep(mab, depInfo);
                        }
                    }
                }), zb -> {
                    zb.withVersion(ClassFile.JAVA_17_VERSION, 0);
                    if (moduleInfo.mainClassName() != null) {
                        zb.with(ModuleMainClassAttribute.of(ClassDesc.of(moduleInfo.mainClassName())));
                    }
                    zb.with(ModulePackagesAttribute.of(
                            moduleInfo.packages().keySet().stream().map(n -> zb.constantPool().packageEntry(PackageDesc.of(n)))
                                    .toList()));
                    List<Annotation> annotations = new ArrayList<>();
                    if (moduleInfo.modifiers().contains(ModuleDescriptor.Modifier.NATIVE_ACCESS)) {
                        annotations.add(Annotation.of(ClassDesc.of("io.smallrye.common.annotation.NativeAccess")));
                    }
                    List<Annotation> addOpens = new ArrayList<>();
                    List<Annotation> addExports = new ArrayList<>();
                    for (DependencyInfo depInfo : moduleInfo.dependencies()) {
                        processDepAnnotations(depInfo, addExports, addOpens);
                    }
                    // now, automatic module deps
                    for (AutoDependencyGroup grp : moduleInfo.autoDependencies()) {
                        for (DependencyInfo depInfo : grp.dependencies()) {
                            processDepAnnotations(depInfo, addExports, addOpens);
                        }
                    }
                    switch (addOpens.size()) {
                        case 0 -> {
                        }
                        case 1 -> annotations.add(addOpens.get(0));
                        default -> annotations.add(Annotation.of(ClassDesc.of("io.smallrye.common.annotation.AddOpens$List"),
                                addOpens.stream().map(a -> AnnotationElement.ofAnnotation("value", a)).toList()));
                    }
                    switch (addExports.size()) {
                        case 0 -> {
                        }
                        case 1 -> annotations.add(addExports.get(0));
                        default -> annotations.add(Annotation.of(ClassDesc.of("io.smallrye.common.annotation.AddExports$List"),
                                addExports.stream().map(a -> AnnotationElement.ofAnnotation("value", a)).toList()));
                    }
                    if (!annotations.isEmpty()) {
                        zb.with(RuntimeInvisibleAnnotationsAttribute.of(annotations));
                    }
                });
    }

    private static void processDep(final ModuleAttribute.ModuleAttributeBuilder mab, final DependencyInfo depInfo) {
        int bits;
        bits = 0;
        if (!depInfo.modifiers().containsAny(Dependency.Modifier.LINKED, Dependency.Modifier.READ)) {
            // skip it
            return;
        }
        if (depInfo.modifiers().contains(Dependency.Modifier.OPTIONAL)) {
            bits |= AccessFlag.STATIC_PHASE.mask();
        }
        if (depInfo.modifiers().contains(Dependency.Modifier.MANDATED)) {
            bits |= AccessFlag.MANDATED.mask();
        }
        if (depInfo.modifiers().contains(Dependency.Modifier.SYNTHETIC)) {
            bits |= AccessFlag.SYNTHETIC.mask();
        }
        if (depInfo.modifiers().contains(Dependency.Modifier.TRANSITIVE)) {
            bits |= AccessFlag.TRANSITIVE.mask();
        }
        mab.requires(ModuleDesc.of(depInfo.moduleName()), bits, null);
    }

    private static void processDepAnnotations(final DependencyInfo depInfo, final List<Annotation> addExports,
            final List<Annotation> addOpens) {
        if (depInfo.packageAccesses().isEmpty()) {
            return;
        }
        List<String> opens = new ArrayList<>();
        List<String> exports = new ArrayList<>();
        String moduleName = depInfo.moduleName();
        for (Map.Entry<String, PackageAccess> e : depInfo.packageAccesses().entrySet()) {
            String pn = e.getKey();
            PackageAccess access = e.getValue();
            switch (access) {
                case EXPORTED -> exports.add(pn);
                case OPEN -> opens.add(pn);
            }
        }
        if (!exports.isEmpty()) {
            addExports.add(Annotation.of(ClassDesc.of("io.smallrye.common.annotation.AddExports"),
                    AnnotationElement.ofString("module", moduleName),
                    AnnotationElement.ofArray("packages",
                            exports.stream().map(AnnotationValue::ofString).toArray(AnnotationValue[]::new))));
        }
        if (!opens.isEmpty()) {
            addOpens.add(Annotation.of(ClassDesc.of("io.smallrye.common.annotation.AddOpens"),
                    AnnotationElement.ofString("module", moduleName),
                    AnnotationElement.ofArray("packages",
                            opens.stream().map(AnnotationValue::ofString).toArray(AnnotationValue[]::new))));
        }
    }

    private static final String NS = "urn:jboss:module:3.0";

    interface XmlCloser extends AutoCloseable {
        void close() throws XMLStreamException;
    }

    private static void writeModuleXml(final XMLStreamWriter xml, final ModuleInfo moduleInfo)
            throws XMLStreamException {
        xml.writeStartDocument();
        xml.setDefaultNamespace(NS);
        xml.writeStartElement("module");
        xml.writeAttribute("name", moduleInfo.name());
        String version = moduleInfo.version();
        if (version != null) {
            xml.writeAttribute("version", version);
        }
        if (moduleInfo.modifiers().contains(ModuleDescriptor.Modifier.AUTOMATIC)) {
            xml.writeAttribute("automatic", "true");
        }
        if (moduleInfo.modifiers().contains(ModuleDescriptor.Modifier.NATIVE_ACCESS)) {
            xml.writeAttribute("native-access", "true");
        }
        if (moduleInfo.modifiers().contains(ModuleDescriptor.Modifier.OPEN)) {
            xml.writeAttribute("open", "true");
        }
        if (moduleInfo.modifiers().contains(ModuleDescriptor.Modifier.UNNAMED)) {
            xml.writeAttribute("unnamed", "true");
        }
        if (moduleInfo.mainClassName() != null) {
            xml.writeEmptyElement("main-class");
            xml.writeAttribute("name", moduleInfo.mainClassName());
        }
        List<DependencyInfo> deps = moduleInfo.dependencies();
        List<AutoDependencyGroup> autoDeps = moduleInfo.autoDependencies();
        if (deps.isEmpty() && autoDeps.isEmpty()) {
            xml.writeEmptyElement("dependencies");
        } else {
            xml.writeStartElement("dependencies");
            for (DependencyInfo dep : deps) {
                writeDependencyElement(xml, dep);
            }
            for (AutoDependencyGroup autoDep : autoDeps) {
                xml.writeCharacters("\n");
                xml.writeComment("dependencies of " + autoDep.hostModuleName());
                for (DependencyInfo dep : autoDep.dependencies()) {
                    writeDependencyElement(xml, dep);
                }
            }
            xml.writeEndElement();
        }
        // gather package list
        Map<String, PackageInfo> packages = moduleInfo.packages();
        if (!packages.isEmpty()) {
            xml.writeStartElement("packages");
            String[] pkgArray = packages.keySet().toArray(String[]::new);
            Arrays.sort(pkgArray);
            for (String pkg : pkgArray) {
                if (pkg.isEmpty()) {
                    // skip default package
                    continue;
                }
                PackageInfo pi = packages.get(pkg);
                switch (pi.packageAccess()) {
                    case PRIVATE -> {
                        if (pi.openTargets().isEmpty() && pi.exportTargets().isEmpty()) {
                            xml.writeEmptyElement("private");
                            xml.writeAttribute("package", pkg);
                        } else {
                            xml.writeStartElement("export");
                            xml.writeAttribute("package", pkg);
                            for (String exportTarget : pi.exportTargets()) {
                                xml.writeEmptyElement("export-to");
                                xml.writeAttribute("module", exportTarget);
                            }
                            for (String openTarget : pi.openTargets()) {
                                xml.writeEmptyElement("open-to");
                                xml.writeAttribute("module", openTarget);
                            }
                            xml.writeEndElement();
                        }
                    }
                    case EXPORTED -> {
                        if (pi.openTargets().isEmpty()) {
                            xml.writeEmptyElement("export");
                            xml.writeAttribute("package", pkg);
                        } else {
                            xml.writeStartElement("export");
                            xml.writeAttribute("package", pkg);
                            for (String openTarget : pi.openTargets()) {
                                xml.writeEmptyElement("open-to");
                                xml.writeAttribute("module", openTarget);
                            }
                            xml.writeEndElement();
                        }
                    }
                    case OPEN -> {
                        xml.writeEmptyElement("open");
                        xml.writeAttribute("package", pkg);
                    }
                }
            }
            xml.writeEndElement(); // packages
        }
        if (!moduleInfo.uses().isEmpty()) {
            xml.writeStartElement("uses");
            for (String name : moduleInfo.uses()) {
                xml.writeEmptyElement("use");
                xml.writeAttribute("name", name);
            }
            xml.writeEndElement(); // uses
        }
        // ---
        // provides
        Map<String, List<String>> provided = moduleInfo.provides();
        if (!provided.isEmpty()) {
            xml.writeStartElement("provides");
            for (Map.Entry<String, List<String>> entry : provided.entrySet()) {
                String svc = entry.getKey();
                xml.writeStartElement("provide");
                xml.writeAttribute("name", svc);
                for (String impl : entry.getValue()) {
                    xml.writeEmptyElement("with");
                    xml.writeAttribute("name", impl);
                }
                xml.writeEndElement(); // provide
            }
            xml.writeEndElement(); // provides
        }
        xml.writeEndElement(); // module
        xml.writeEndDocument();
    }

    private static void writeDependencyElement(final XMLStreamWriter xml, final DependencyInfo depInfo)
            throws XMLStreamException {
        Map<String, PackageAccess> accesses = depInfo.packageAccesses();
        String depName = depInfo.moduleName();
        boolean empty = accesses.isEmpty();
        if (depName.equals("java.base") && empty) {
            // skip it (it's implied)
            return;
        }
        if (empty) {
            xml.writeEmptyElement("dependency");
        } else {
            xml.writeStartElement("dependency");
        }
        xml.writeAttribute("name", depName);
        if (depInfo.modifiers().contains(Dependency.Modifier.OPTIONAL)) {
            xml.writeAttribute("optional", "true");
        }
        if (depInfo.modifiers().contains(Dependency.Modifier.TRANSITIVE)) {
            xml.writeAttribute("transitive", "true");
        }
        if (!depInfo.modifiers().contains(Dependency.Modifier.LINKED)) {
            xml.writeAttribute("linked", "false");
        }
        if (!depInfo.modifiers().contains(Dependency.Modifier.READ)) {
            xml.writeAttribute("read", "false");
        }
        if (!empty) {
            for (String pn : accesses.keySet()) {
                switch (accesses.get(pn)) {
                    case EXPORTED -> xml.writeEmptyElement("add-exports");
                    case OPEN -> xml.writeEmptyElement("add-opens");
                    default -> {
                        // do not emit
                        continue;
                    }
                }
                xml.writeAttribute("name", pn);
            }
            xml.writeEndElement(); // dependency
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> RuntimeException sneak(Throwable t) throws E {
        throw (E) t;
    }
}
