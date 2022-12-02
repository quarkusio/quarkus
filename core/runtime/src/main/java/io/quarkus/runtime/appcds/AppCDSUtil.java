package io.quarkus.runtime.appcds;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;

public class AppCDSUtil {

    /**
     * This is never meant to be used in a regular application run.
     * It is only referenced by the generated main with the purpose of
     * loading all the generated and transformed classes in order to give the AppCDS
     * generation process a larger set of classes to work with
     */
    public static void loadGeneratedClasses() throws IOException, ClassNotFoundException {
        try (BufferedReader br = new BufferedReader(new FileReader(Paths.get("generatedAndTransformed.lst").toFile()))) {
            String line;
            while ((line = br.readLine()) != null) {
                Class.forName(line, true, Thread.currentThread().getContextClassLoader());
            }
        } catch (ClassNotFoundException | IOException e) {
            System.err.println("Improperly configured AppCDS generation process launched");
            e.printStackTrace();
            throw e;
        }
    }
}
