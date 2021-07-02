package io.quarkus.helpers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class FileHelper {

    public String getValueFromFile(String fileName) {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(fileName).getFile());
        try {
            return new String(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

}
