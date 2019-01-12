package org.jboss.shamrock.test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

final class PathTestHelper {

    private static final String TEST_CLASSES_FRAGMENT = File.separator + "test-classes";
    private static final String CLASSES_FRAGMENT = File.separator + "classes";

    private PathTestHelper() {
    }

    public static final Path getTestClassesLocation(Class<?> testClass) {
        String classFileName = testClass.getName().replace('.', File.separatorChar) + ".class";
        URL resource = testClass.getClassLoader().getResource(classFileName);

		try {
			Path path = Paths.get(resource.toURI());
	        if (!path.toString().contains(TEST_CLASSES_FRAGMENT)) {
	            throw new RuntimeException("The test class " + testClass + " is not located in the " + TEST_CLASSES_FRAGMENT + " directory.");
	        }

	        return path.subpath(0, path.getNameCount() - Paths.get(classFileName).getNameCount());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}

    }

    public static final Path getAppClassLocation(Class<?> testClass) {
        return Paths.get(getTestClassesLocation(testClass).toString().replace(TEST_CLASSES_FRAGMENT, CLASSES_FRAGMENT));
    }
}
