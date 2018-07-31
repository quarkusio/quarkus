package org.jboss.shamrock.maven;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.jboss.shamrock.runtime.RuntimeRunner;

@Mojo(name = "run", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class RunMojo extends AbstractMojo {

    /**
     * The directory for compiled classes.
     */
    @Parameter(readonly = true, required = true, defaultValue = "${project.build.outputDirectory}")
    private File outputDirectory;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Parameter(defaultValue = "org.jboss.shamrock.runner.GeneratedMain")
    private String mainClass;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final byte[] buffer = new byte[8000];
        try {
            StringBuilder classPath = new StringBuilder();
            List<URL> classPathUrls = new ArrayList<>();
            for (Artifact artifact : project.getArtifacts()) {
                classPathUrls.add(artifact.getFile().toURL());
            }

            //the class loader for all the library jars
            //the actual application will be in a child class loader
            URLClassLoader libraryClassLoader = new URLClassLoader(classPathUrls.toArray(new URL[0]), ClassLoader.getSystemClassLoader());

            do {
                //we can potentially throw away this class loader, and reload the app
                CountDownLatch changeLatch = new CountDownLatch(1);
                URLClassLoader runtimeCl = new URLClassLoader(new URL[]{outputDirectory.toURL()}, libraryClassLoader);
                ClassLoader old = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(runtimeCl);
                    Class<?> runnerClass = runtimeCl.loadClass("org.jboss.shamrock.runtime.RuntimeRunner");
                    Constructor ctor = runnerClass.getDeclaredConstructor(Path.class, ClassLoader.class);
                    Object runner = ctor.newInstance(outputDirectory.toPath(), runtimeCl);
                    ((Runnable)runner).run();

                    changeLatch.await();
                } finally {
                    Thread.currentThread().setContextClassLoader(old);
                }

            } while (true);

        } catch (Exception e) {
            throw new MojoFailureException("Failed to run", e);
        }
    }

}
