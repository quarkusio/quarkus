package org.jboss.shamrock.maven;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * The run mojo, that runs a shamrock app in a forked process
 * <p>
 * This will use Fakereplace to enable hot replacement, and also enable on demand compile
 */
@Mojo(name = "run", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class RunMojo extends AbstractMojo {

    /**
     * The directory for compiled classes.
     */
    @Parameter(readonly = true, required = true, defaultValue = "${project.build.outputDirectory}")
    private File outputDirectory;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final byte[] buffer = new byte[8000];
        try {
            //build a class-path string for the base platform
            //this stuff does not change
            StringBuilder classPath = new StringBuilder();
            List<URL> classPathUrls = new ArrayList<>();
            for (Artifact artifact : project.getArtifacts()) {
                classPath.append(artifact.getFile().getAbsolutePath());
                classPath.append(" ");
            }

            //now we need to build a temporary jar to actually run
            File tempFile = File.createTempFile("shamrock", "-runner.jar");
            tempFile.deleteOnExit();


            try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(tempFile))) {
                String runMojoClassFile = RunMojoMain.class.getName().replace(".", "/") + ".class";
                out.putNextEntry(new ZipEntry(runMojoClassFile));
                int r;
                try (InputStream in = getClass().getClassLoader().getResourceAsStream(runMojoClassFile)) {
                    while ((r = in.read(buffer)) > 0) {
                        out.write(buffer, 0, r);
                    }
                }

                out.putNextEntry(new ZipEntry("META-INF/"));
                Manifest manifest = new Manifest();
                manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
                manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, classPath.toString());
                manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, RunMojoMain.class.getName());
                out.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
                manifest.write(out);
            }


            List<String> args = new ArrayList<>();
            args.add("java");
            args.add("-jar");
            args.add(tempFile.getAbsolutePath());
            args.add(outputDirectory.getAbsolutePath());
            Process p = Runtime.getRuntime().exec(args.toArray(new String[0]), null, outputDirectory);
            new ProcessReader(p.getErrorStream()).run();
            new ProcessReader(p.getInputStream()).run();

            int val = p.waitFor();

        } catch (Exception e) {
            throw new MojoFailureException("Failed to run", e);
        }
    }

}
