package org.jboss.protean.arc.processor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jboss.protean.arc.processor.ResourceOutput.Resource;

/**
 *
 * @author Martin Kouba
 */
class ResourceImpl implements ResourceOutput.Resource {

    static Resource javaClass(String name, byte[] data) {
        return javaClass(name, data, null);
    }

    static Resource javaClass(String name, byte[] data, SpecialType specialType) {
        return new ResourceImpl(name, data, Type.JAVA_CLASS, specialType);

    }

    static Resource serviceProvider(String name, byte[] data) {
        return serviceProvider(name, data, null);
    }

    static Resource serviceProvider(String name, byte[] data, SpecialType specialType) {
        return new ResourceImpl(name, data, Type.SERVICE_PROVIDER, specialType);
    }

    private final String name;

    private final byte[] data;

    private final Type type;

    private final SpecialType specialType;

    private ResourceImpl(String name, byte[] data, Type type, SpecialType specialType) {
        this.name = name;
        this.data = data;
        this.type = type;
        this.specialType = specialType;
    }

    @Override
    public File writeTo(File directory) throws IOException {
        switch (type) {
            case JAVA_CLASS:
                Path outputPath = getOutputDirectory(directory).resolve(getSimpleName() + ".class");
                Files.write(outputPath, data);
                return outputPath.toFile();
            default:
                File file = new File(directory, name);
                file.getParentFile().mkdirs();
                try (FileOutputStream out = new FileOutputStream(file)) {
                    out.write(data);
                }
                return file;
        }
    }

    @Override
    public byte[] getData() {
        return data;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public SpecialType getSpecialType() {
        return specialType;
    }

    private Path getOutputDirectory(File directory) throws IOException {
        Path outputDirectory = directory.toPath();
        if (name.contains("/")) {
            for (String packageComponent : name.substring(0, name.lastIndexOf("/")).split("/")) {
                outputDirectory = outputDirectory.resolve(packageComponent);
            }
        }
        Files.createDirectories(outputDirectory);
        return outputDirectory;
    }

    private String getSimpleName() {
        return name.contains("/") ? name.substring(name.lastIndexOf("/") + 1, name.length()) : name;
    }

}
