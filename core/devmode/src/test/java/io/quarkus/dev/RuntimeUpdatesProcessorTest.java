package io.quarkus.dev;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ RuntimeCompilationSetup.class, Paths.class})
public class RuntimeUpdatesProcessorTest {

    static final String classes = "classes";
    static final String sources = "sources";
    static final String resources = "resources";


    /**
     * Deals with issue of {@link ClassLoaderCompiler} trying to recompile package-info.java
     * @see <a href="https://github.com/quarkusio/quarkus/issues/1992">https://github.com/quarkusio/quarkus/issues/1992</a>
     * @throws Exception {@link RuntimeCompilationSetup#setup()} may throw a null pointer
     */
    @Test
    public void testPackageInfoExclusion() throws Exception {
        System.setProperty(RuntimeCompilationSetup.PROP_RUNNER_CLASSES, classes);
        System.setProperty(RuntimeCompilationSetup.PROP_RUNNER_SOURCES, sources);
        System.setProperty(RuntimeCompilationSetup.PROP_RUNNER_RESOURCES, resources);

        Path fakePath = Paths.get(Thread.currentThread().getContextClassLoader().getResource("fake").toURI());
        Path classesPath = fakePath.resolve(classes);

        ClassLoaderCompiler mockedClassLoaderCompiler = mock(ClassLoaderCompiler.class);

        // When a new instance of ClassLoaderCompiler is called inside RuntimeCompilationSetup, return the mock
        whenNew(ClassLoaderCompiler.class).withAnyArguments().thenReturn(mockedClassLoaderCompiler);

        // Handles the call to ClassLoaderCompiler#allHandledExtensions()
        when(mockedClassLoaderCompiler, "allHandledExtensions").thenReturn(Collections.singleton(".java"));

        // Mock all the calls to path
        mockStatic(Paths.class);

        // Return these paths instead
        when(Paths.get(classes)).thenReturn(classesPath);
        when(Paths.get(resources)).thenReturn(fakePath.resolve(resources));
        when(Paths.get(sources)).thenReturn(fakePath.resolve(sources));

        List<String> files = new ArrayList<>();

        // Invocation of ClassLoaderCompiler#compile(Map<String, Set<File>> extensionToChangedFiles)
        Mockito.doAnswer((Answer<Void>) invocationOnMock -> {
            Map<String, Set<File>> compilation = invocationOnMock.getArgument(0, Map.class);

            // Get Java Compilation Targets
            Set<File> java = compilation.get(".java");
            for(File file : java){
                files.add(file.getName());

                // Change the file extension to .class
                String compileName = file.getName().replace("java", "class");

                // Fake compile by just puting them in the classes directory
                Path compiledPath = Files.createFile(classesPath.resolve(compileName));
                compiledPath.toFile().deleteOnExit();
            }
            return null;
        }).when(mockedClassLoaderCompiler).compile(any());





        RuntimeUpdatesProcessor setup = RuntimeCompilationSetup.setup();


        Assert.assertTrue("RuntimeUpdatesProcessor should have detected a change",
                setup.checkForChangedClasses());

        Assert.assertFalse("RuntimeUpdatesProcessor should not have picked up 'package-info.java'",
                files.contains("package-info.java"));

        Assert.assertTrue("RuntimeUpdatesProcessor should have detected a change on FakeSource.java because it was " +
                        "not in the compiled path",
                files.contains("FakeSource.java"));

    }

}
