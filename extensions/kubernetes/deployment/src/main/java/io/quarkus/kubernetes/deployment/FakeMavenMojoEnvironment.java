package io.quarkus.kubernetes.deployment;

import java.nio.file.Path;

import org.apache.maven.model.Build;
import org.apache.maven.plugins.assembly.archive.AssemblyArchiver;
import org.apache.maven.plugins.assembly.io.AssemblyReader;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.component.repository.ComponentRequirement;
import org.eclipse.jkube.kit.build.maven.MavenBuildContext;
import org.eclipse.jkube.kit.build.maven.assembly.DockerAssemblyManager;
import org.eclipse.jkube.kit.build.service.docker.DockerAccessFactory;
import org.eclipse.jkube.kit.build.service.docker.ServiceHubFactory;
import org.eclipse.jkube.kit.build.service.docker.config.handler.ImageConfigResolver;
import org.eclipse.jkube.kit.build.service.docker.config.handler.property.PropertyConfigHandler;

/**
 * TODO: REMOVE THIS, PoC purpose only.
 * JKube should be updated to avoid this tight coupling with Plexus
 */
class FakeMavenMojoEnvironment {

    static final PlexusContainer PLEXUS_CONTAINER = fakeMavenMojoEnvironment();

    private FakeMavenMojoEnvironment() {
    }

    private static PlexusContainer fakeMavenMojoEnvironment() {
        final PlexusContainer pc;
        try {
            pc = new DefaultPlexusContainer(new DefaultContainerConfiguration());
            for (Class clazz : new Class[] {
                    PropertyConfigHandler.class, ImageConfigResolver.class, DockerAccessFactory.class
            }) {
                pc.addComponentDescriptor(initComponentDescriptor(clazz, pc.getContainerRealm()));
            }
            final ComponentDescriptor<DockerAssemblyManager> dam = initComponentDescriptor(DockerAssemblyManager.class,
                    pc.getContainerRealm());
            dam.addRequirement(initComponentRequirement(Archiver.class, "trackArchiver"));
            dam.addRequirement(initComponentRequirement(AssemblyArchiver.class, "assemblyArchiver"));
            dam.addRequirement(initComponentRequirement(AssemblyReader.class, "assemblyReader"));
            dam.addRequirement(initComponentRequirement(ArchiverManager.class, "archiverManager"));
            pc.addComponentDescriptor(dam);
            final ComponentDescriptor<ServiceHubFactory> shf = initComponentDescriptor(ServiceHubFactory.class,
                    pc.getContainerRealm());
            shf.addRequirement(initComponentRequirement(DockerAssemblyManager.class, "dockerAssemblyManager"));
            pc.addComponentDescriptor(shf);
        } catch (Exception e) {
            throw new RuntimeException("Error instantiating JKubeProcessor", e);
        }
        return pc;
    }

    private static ComponentRequirement initComponentRequirement(Class role, String fieldName) {
        final ComponentRequirement ret = new ComponentRequirement();
        ret.setRole(role.getCanonicalName());
        ret.setFieldName(fieldName);
        return ret;
    }

    private static <T> ComponentDescriptor<T> initComponentDescriptor(
            Class<T> clazz, final ClassRealm classRealm, Class<? extends T> defaultImplementation) {
        final ComponentDescriptor<T> ret = new ComponentDescriptor<>(clazz, classRealm);
        ret.setRoleClass(clazz);
        ret.setImplementationClass(defaultImplementation);
        ret.setRole(clazz.getCanonicalName());
        ret.setIsolatedRealm(false);
        return ret;
    }

    private static <T> ComponentDescriptor<T> initComponentDescriptor(Class<T> clazz, final ClassRealm classRealm) {
        return initComponentDescriptor(clazz, classRealm, clazz);
    }

    static MavenBuildContext fakeMavenBuildContext(Path projectDirectory) {
        final MavenProject mp = new MavenProject();
        mp.getProperties().put("build_dir", projectDirectory.resolve("target").toString());
        mp.setFile(projectDirectory.resolve("pom.xml").toFile());
        mp.setPackaging("jar");
        mp.setBuild(new Build());
        mp.getBuild().setOutputDirectory("target/classes");
        mp.getBuild().setSourceDirectory("src/main/java");
        mp.getBuild().setDirectory("target");
        return new MavenBuildContext.Builder()
                .sourceDirectory("src")
                .outputDirectory("target")
                .project(mp)
                .build();
    }
}
