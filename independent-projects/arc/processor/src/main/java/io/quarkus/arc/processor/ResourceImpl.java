package io.quarkus.arc.processor;

import io.quarkus.arc.processor.ResourceOutput.Resource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 *
 * @author Martin Kouba
 */
class ResourceImpl implements ResourceOutput.Resource {

    private final boolean applicationClass;

    static Resource javaClass(String name, byte[] data, SpecialType specialType, boolean applicationClass, String source) {
        return new ResourceImpl(applicationClass, name, data, Type.JAVA_CLASS, specialType, source);

    }

    static Resource serviceProvider(String name, byte[] data) {
        return serviceProvider(name, data, null);
    }

    static Resource serviceProvider(String name, byte[] data, SpecialType specialType) {
        return new ResourceImpl(true, name, data, Type.SERVICE_PROVIDER, specialType, null);
    }

    private final String name;
    private final byte[] data;
    private final String source;
    private final Type type;
    private final SpecialType specialType;

    private ResourceImpl(boolean applicationClass, String name, byte[] data, Type type, SpecialType specialType,
            String source) {
        this.applicationClass = applicationClass;
        this.name = name;
        this.data = data;
        this.type = type;
        this.specialType = specialType;
        this.source = source;
    }

    @Override
    public boolean isApplicationClass() {
        return applicationClass;
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

    @Override
    public String getSource() {
        return source;
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
