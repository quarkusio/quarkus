/**
 *
 */
package io.quarkus.bootstrap.workspace.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import java.nio.file.Path;
import java.util.Map;

import org.apache.maven.model.Parent;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.bootstrap.util.IoUtils;

public class LocalWorkspaceDiscoveryTest {

    protected static Path workDir;

    @BeforeClass
    public static void setup() throws Exception {
        workDir = IoUtils.createRandomTmpDir();

        final Parent parent = new Parent();
        parent.setGroupId(MvnProjectBuilder.DEFAULT_GROUP_ID);
        parent.setArtifactId("parent");
        parent.setVersion(MvnProjectBuilder.DEFAULT_VERSION);
        parent.setRelativePath(null);

        MvnProjectBuilder.forArtifact("root")
        .setParent(parent)
        .addModule("module1", "root-no-parent-module", false)
        .getParent()
        .addModule("module2", "root-module-with-parent", true)
        .getParent()
        .addModule("other/module3", "root-module-not-direct-child", true)
        .getParent()
        .build(workDir.resolve("root"));

        final Parent rootParent = new Parent();
        rootParent.setGroupId(MvnProjectBuilder.DEFAULT_GROUP_ID);
        rootParent.setArtifactId("root");
        rootParent.setVersion(MvnProjectBuilder.DEFAULT_VERSION);
        rootParent.setRelativePath(null);

        MvnProjectBuilder.forArtifact("non-module-child")
        .setParent(rootParent)
        .addModule("module1", "another-child", true)
        .getParent()
        .build(workDir.resolve("root").resolve("non-module-child"));

        // independent project in the tree
        MvnProjectBuilder.forArtifact("independent").build(workDir.resolve("root").resolve("independent"));
    }

    @AfterClass
    public static void cleanup() {
        IoUtils.recursiveDelete(workDir);
    }

    @Test
    public void loadIndependentProjectInTheWorkspaceTree() throws Exception {
        final LocalProject project = LocalProject.loadWorkspace(workDir.resolve("root").resolve("independent").resolve("target").resolve("classes"));
        assertNotNull(project);
        assertNotNull(project.getWorkspace());
        assertEquals(MvnProjectBuilder.DEFAULT_GROUP_ID, project.getGroupId());
        assertEquals("independent", project.getArtifactId());
        assertEquals(MvnProjectBuilder.DEFAULT_VERSION, project.getVersion());
        final Map<AppArtifactKey, LocalProject> projects = project.getWorkspace().getProjects();
        assertEquals(1, projects.size());
        projects.containsKey(new AppArtifactKey(MvnProjectBuilder.DEFAULT_GROUP_ID, "independent"));
    }

    @Test
    public void loadModuleProjectWithoutParent() throws Exception {
        final LocalProject project = LocalProject.load(workDir.resolve("root").resolve("module1").resolve("target").resolve("classes"));
        assertNotNull(project);
        assertNull(project.getWorkspace());
        assertEquals(MvnProjectBuilder.DEFAULT_GROUP_ID, project.getGroupId());
        assertEquals("root-no-parent-module", project.getArtifactId());
        assertEquals(MvnProjectBuilder.DEFAULT_VERSION, project.getVersion());
    }

    @Test
    public void loadWorkspaceForModuleWithoutParent() throws Exception {
        final LocalProject project = LocalProject.loadWorkspace(workDir.resolve("root").resolve("module1").resolve("target").resolve("classes"));
        assertNotNull(project);
        assertEquals(MvnProjectBuilder.DEFAULT_GROUP_ID, project.getGroupId());
        assertEquals("root-no-parent-module", project.getArtifactId());
        assertEquals(MvnProjectBuilder.DEFAULT_VERSION, project.getVersion());
        assertNotNull(project.getWorkspace());
        final Map<AppArtifactKey, LocalProject> projects = project.getWorkspace().getProjects();
        assertEquals(1, projects.size());
        projects.containsKey(new AppArtifactKey(MvnProjectBuilder.DEFAULT_GROUP_ID, "root-no-parent-module"));
    }

    @Test
    public void loadModuleProjectWithParent() throws Exception {
        final LocalProject project = LocalProject.load(workDir.resolve("root").resolve("module2").resolve("target").resolve("classes"));
        assertNotNull(project);
        assertNull(project.getWorkspace());
        assertEquals(MvnProjectBuilder.DEFAULT_GROUP_ID, project.getGroupId());
        assertEquals("root-module-with-parent", project.getArtifactId());
        assertEquals(MvnProjectBuilder.DEFAULT_VERSION, project.getVersion());
    }

    @Test
    public void loadWorkspaceForModuleWithParent() throws Exception {
        final LocalProject project = LocalProject.loadWorkspace(workDir.resolve("root").resolve("module2").resolve("target").resolve("classes"));
        assertNotNull(project);
        assertNotNull(project.getWorkspace());
        assertEquals(MvnProjectBuilder.DEFAULT_GROUP_ID, project.getGroupId());
        assertEquals("root-module-with-parent", project.getArtifactId());
        assertEquals(MvnProjectBuilder.DEFAULT_VERSION, project.getVersion());

        assertCompleteWorkspace(project);
    }

    @Test
    public void loadWorkspaceForModuleWithNotDirectParentPath() throws Exception {
        final LocalProject project = LocalProject.loadWorkspace(workDir.resolve("root").resolve("other").resolve("module3").resolve("target").resolve("classes"));
        assertNotNull(project);
        assertNotNull(project.getWorkspace());
        assertEquals(MvnProjectBuilder.DEFAULT_GROUP_ID, project.getGroupId());
        assertEquals("root-module-not-direct-child", project.getArtifactId());
        assertEquals(MvnProjectBuilder.DEFAULT_VERSION, project.getVersion());

        assertCompleteWorkspace(project);
    }

    private void assertCompleteWorkspace(final LocalProject project) {
        final Map<AppArtifactKey, LocalProject> projects = project.getWorkspace().getProjects();
        assertEquals(4, projects.size());
        projects.containsKey(new AppArtifactKey(MvnProjectBuilder.DEFAULT_GROUP_ID, "root-no-parent-module"));
        projects.containsKey(new AppArtifactKey(MvnProjectBuilder.DEFAULT_GROUP_ID, "root-module-with-parent"));
        projects.containsKey(new AppArtifactKey(MvnProjectBuilder.DEFAULT_GROUP_ID, "root-module-not-direct-parent"));
        projects.containsKey(new AppArtifactKey(MvnProjectBuilder.DEFAULT_GROUP_ID, "root"));
    }

    @Test
    public void loadNonModuleChildProject() throws Exception {
        final LocalProject project = LocalProject.loadWorkspace(workDir.resolve("root").resolve("non-module-child").resolve("target").resolve("classes"));
        assertNotNull(project);
        assertNotNull(project.getWorkspace());
        assertEquals("non-module-child", project.getArtifactId());
        final Map<AppArtifactKey, LocalProject> projects = project.getWorkspace().getProjects();
        assertEquals(6, projects.size());
        projects.containsKey(new AppArtifactKey(MvnProjectBuilder.DEFAULT_GROUP_ID, "root-no-parent-module"));
        projects.containsKey(new AppArtifactKey(MvnProjectBuilder.DEFAULT_GROUP_ID, "root-module-with-parent"));
        projects.containsKey(new AppArtifactKey(MvnProjectBuilder.DEFAULT_GROUP_ID, "root-module-not-direct-parent"));
        projects.containsKey(new AppArtifactKey(MvnProjectBuilder.DEFAULT_GROUP_ID, "root"));
        projects.containsKey(new AppArtifactKey(MvnProjectBuilder.DEFAULT_GROUP_ID, "non-module-child"));
        projects.containsKey(new AppArtifactKey(MvnProjectBuilder.DEFAULT_GROUP_ID, "another-child"));
    }
}
