package org.jboss.protean.arc.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.jboss.protean.arc.Arc;
import org.jboss.protean.arc.ComponentsProvider;
import org.jboss.protean.arc.processor.BeanProcessor;
import org.jboss.protean.arc.processor.ResourceOutput;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class ArcTestContainer implements TestRule {

    private final List<Class<?>> beanClasses;

    public ArcTestContainer(Class<?>... beanClasses) {
        this.beanClasses = Arrays.asList(beanClasses);
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                ClassLoader oldTccl = init(description.getTestClass());
                try {
                    base.evaluate();
                } finally {
                    Thread.currentThread().setContextClassLoader(oldTccl);
                    shutdown();
                }
            }
        };
    }

    private void shutdown() {
        Arc.shutdown();
    }

    private ClassLoader init(Class<?> testClass) {

        // Make sure Arc is down
        Arc.shutdown();

        // Build index
        Index index;
        try {
            index = index(beanClasses);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create index", e);
        }

        File generatedSourcesDirectory = new File("target/generated-arc-sources");
        File testOutputDirectory = new File("target/test-classes");
        File componentsProviderFile = new File(generatedSourcesDirectory + "/" + nameToPath(testClass.getPackage().getName()),
                ComponentsProvider.class.getSimpleName());

        BeanProcessor beanProcessor = BeanProcessor.builder().setName(testClass.getSimpleName()).setIndex(index).setOutput(new ResourceOutput() {

            @Override
            public void writeResource(Resource resource) throws IOException {
                switch (resource.getType()) {
                    case JAVA_CLASS:
                        resource.writeTo(testOutputDirectory);
                        break;
                    case SERVICE_PROVIDER:
                        if (resource.getName().endsWith(ComponentsProvider.class.getName())) {
                            componentsProviderFile.getParentFile().mkdirs();
                            try (FileOutputStream out = new FileOutputStream(componentsProviderFile)) {
                                out.write(resource.getData());
                            }
                        }
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
            }
        }).build();
        try {
            beanProcessor.process();
        } catch (IOException e) {
            throw new IllegalStateException("Error generating resources", e);
        }

        ClassLoader old = Thread.currentThread().getContextClassLoader();
        ClassLoader testClassLoader = new URLClassLoader(new URL[] {}, old) {
            @Override
            public Enumeration<URL> getResources(String name) throws IOException {
                if (("META-INF/services/" + ComponentsProvider.class.getName()).equals(name)) {
                    // return URL that points to the correct test bean provider
                    return Collections.enumeration(Collections.singleton(componentsProviderFile.toURI().toURL()));
                }
                return super.getResources(name);
            }
        };
        Thread.currentThread().setContextClassLoader(testClassLoader);
        
        // Now we are ready to initialize Arc
        Arc.initialize();
        
        return old;
    }

    private Index index(Iterable<Class<?>> classes) throws IOException {
        Indexer indexer = new Indexer();
        for (Class<?> clazz : classes) {
            try (InputStream stream = ArcTestContainer.class.getClassLoader().getResourceAsStream(clazz.getName().replace('.', '/') + ".class")) {
                indexer.index(stream);
            }
        }
        return indexer.complete();
    }

    private String nameToPath(String packName) {
        return packName.replace('.', '/');
    }
}
