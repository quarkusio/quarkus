package org.jboss.shamrock.maven.runner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.jboss.shamrock.maven.ProcessReader;

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

    @Parameter(defaultValue = "${fakereplace}")
    private boolean fakereplace = true;

    @Parameter(defaultValue = "${debug}")
    private boolean debug = false;

    @Parameter(defaultValue = "${project.build.directory}")
    private File buildDir;

    @Override
    public void execute() throws MojoFailureException {
        try {

            List<String> args = new ArrayList<>();
            args.add("java");
            if (debug) {
                args.add("-Xdebug");
                args.add("-Xnoagent");
                args.add("-Djava.compiler=NONE");
                args.add("-Xrunjdwp:transport=dt_socket,address=5005,server=y,suspend=y");
            }
            args.add("-XX:TieredStopAtLevel=1");
            //build a class-path string for the base platform
            //this stuff does not change
            StringBuilder classPath = new StringBuilder();
            for (Artifact artifact : project.getArtifacts()) {
                classPath.append(artifact.getFile().getAbsolutePath());
                classPath.append(" ");
            }

            if (fakereplace) {
                File target = new File(buildDir, "fakereplace.jar");
                if (!target.exists()) {
                    //this is super yuck, but there does not seen to be an easy way
                    //to get dependency artifacts. Fakereplace must be called fakereplace.jar to work
                    //so we copy it to the target directory
                    URL resource = getClass().getClassLoader().getResource("org/fakereplace/core/Fakereplace.class");
                    if (resource == null) {
                        throw new RuntimeException("Could not determine Fakereplace location");
                    }
                    String filePath = resource.getPath();
                    try (FileInputStream in = new FileInputStream(filePath.substring(5, filePath.lastIndexOf('!')))) {
                        try (FileOutputStream out = new FileOutputStream(target)) {
                            byte[] buffer = new byte[1024];
                            int r;
                            while ((r = in.read(buffer)) > 0) {
                                out.write(buffer, 0, r);
                            }
                        }
                    }
                }
                args.add("-javaagent:" + target.getAbsolutePath());
            }

            //we also want to add the maven plugin jar to the class path
            //this allows us to just directly use classes, without messing around copying them
            //to the runner jar
            URL classFile = getClass().getClassLoader().getResource(getClass().getName().replace(".", "/") + ".class");
            classPath.append(((JarURLConnection) classFile.openConnection()).getJarFileURL().getFile());

            //now we need to build a temporary jar to actually run
            File tempFile = File.createTempFile("shamrock", "-runner.jar");
            tempFile.deleteOnExit();

            try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(tempFile))) {
                out.putNextEntry(new ZipEntry("META-INF/"));
                Manifest manifest = new Manifest();
                manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
                manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, classPath.toString());
                manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, RunMojoMain.class.getName());
                out.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
                manifest.write(out);
            }

            args.add("-Dshamrock.runner.classes=" + outputDirectory.getAbsolutePath());
            args.add("-jar");
            args.add(tempFile.getAbsolutePath());
            args.add(outputDirectory.getAbsolutePath());
            Process p = Runtime.getRuntime().exec(args.toArray(new String[0]), null, outputDirectory);
            new Thread(new ProcessReader(p.getErrorStream(), true)).start();
            new Thread(new ProcessReader(p.getInputStream(), false)).start();

            int val = p.waitFor();

        } catch (Exception e) {
            throw new MojoFailureException("Failed to run", e);
        }
    }

}
