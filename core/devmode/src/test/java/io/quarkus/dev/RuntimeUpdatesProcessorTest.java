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

@PrepareForTest({ RuntimeCompilationSetup.class, Paths.class})
@RunWith(PowerMockRunner.class)
public class RuntimeUpdatesProcessorTest {

    public static final String classes = "classes";
    public static final String sources = "sources";
    public static final String resources = "resources";



    @Test
    public void testPackageInfoExclusion() throws Exception {
        System.setProperty(RuntimeCompilationSetup.PROP_RUNNER_CLASSES, classes);
        System.setProperty(RuntimeCompilationSetup.PROP_RUNNER_SOURCES, sources);
        System.setProperty(RuntimeCompilationSetup.PROP_RUNNER_RESOURCES, resources);

        Path fake = Paths.get(Thread.currentThread().getContextClassLoader().getResource("fake").toURI());




        ClassLoaderCompiler mock = mock(ClassLoaderCompiler.class);

        whenNew(ClassLoaderCompiler.class)
                .withAnyArguments()
                .thenReturn(mock);
        when(mock, "allHandledExtensions").thenReturn(Collections.singleton(".java"));

        mockStatic(Paths.class);

        List<String> files = new ArrayList<>();
        Mockito.doAnswer((Answer<Object>) invocationOnMock -> {
            Map<String, Set<File>> compilation1 = invocationOnMock.getArgument(0, Map.class);
            Set<File> java = compilation1.get(".java");
            for(File file : java){
                files.add(file.getName());
                Files.createFile(fake.resolve(classes).resolve(file.getName().replace("java", "class"))).toFile().deleteOnExit();
            }
            return null;
        }).when(mock).compile(any());



        when(Paths.get(classes)).thenReturn(fake.resolve(classes));
        when(Paths.get(resources)).thenReturn(fake.resolve(resources));
        when(Paths.get(sources)).thenReturn(fake.resolve(sources));

        RuntimeUpdatesProcessor setup = RuntimeCompilationSetup.setup();


        Assert.assertTrue(setup.checkForChangedClasses());
        Assert.assertFalse(files.contains("package-info.java"));
        Assert.assertTrue(files.contains("FakeSource.java"));

    }

}
