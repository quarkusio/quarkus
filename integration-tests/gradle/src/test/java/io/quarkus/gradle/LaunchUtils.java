package io.quarkus.gradle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.utilities.JavaBinFinder;

public class LaunchUtils {

    protected static Process launch(Path jar, File output) throws IOException {
        List<String> commands = new ArrayList<>();
        commands.add(JavaBinFinder.findBin());
        commands.add("-jar");
        commands.add(jar.toString());
        ProcessBuilder processBuilder = new ProcessBuilder(commands.toArray(new String[0]));
        processBuilder.redirectOutput(output);
        processBuilder.redirectError(output);
        return processBuilder.start();
    }

}
