/**
 *
 */
package io.quarkus.bootstrap.workspace.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.bootstrap.util.IoUtils;

public class LocalWorkspaceDiscoveryTest {

    private static Dependency newDependency(String artifactId) {
        return newDependency(MvnProjectBuilder.DEFAULT_GROUP_ID, artifactId, MvnProjectBuilder.DEFAULT_VERSION);
    }

    private static Dependency newDependency(String groupId, String artifactId, String version) {
        final Dependency dep = new Dependency();
        dep.setGroupId(groupId);
        dep.setArtifactId(artifactId);
        dep.setVersion(version);
        return dep;
    }

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
        .addDependency(newDependency("root-module-not-direct-child"))
        .getParent()

        .addModule("module2", "root-module-with-parent", true)
        .addDependency(newDependency("root-no-parent-module"))
        .addDependency(newDependency("external-dep"))
        .addDependency(newDependency(LocalProject.PROJECT_GROUPID, "root-module-not-direct-child", MvnProjectBuilder.DEFAULT_VERSION))
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
        MvnProjectBuilder.forArtifact("independent")
        .addDependency(newDependency("root-module-with-parent"))
        .build(workDir.resolve("root").resolve("independent"));
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
        assertTrue(projects.containsKey(new AppArtifactKey(MvnProjectBuilder.DEFAULT_GROUP_ID, "independent")));

        assertLocalDeps(project);
    }

    @Test
    public void loadModuleProjectWithoutParent() throws Exception {
        final LocalProject project = LocalProject.load(workDir.resolve("root").resolve("module1").resolve("target").resolve("classes"));
        assertNotNull(project);
        assertNull(project.getWorkspace());
        assertEquals(MvnProjectBuilder.DEFAULT_GROUP_ID, project.getGroupId());
        assertEquals("root-no-parent-module", project.getArtifactId());
        assertEquals(MvnProjectBuilder.DEFAULT_VERSION, project.getVersion());
        assertLocalDeps(project);
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
        assertTrue(projects.containsKey(new AppArtifactKey(MvnProjectBuilder.DEFAULT_GROUP_ID, "root-no-parent-module")));
        assertLocalDeps(project);
    }

    @Test
    public void loadModuleProjectWithParent() throws Exception {
        final LocalProject project = LocalProject.load(workDir.resolve("root").resolve("module2").resolve("target").resolve("classes"));
        assertNotNull(project);
        assertNull(project.getWorkspace());
        assertEquals(MvnProjectBuilder.DEFAULT_GROUP_ID, project.getGroupId());
        assertEquals("root-module-with-parent", project.getArtifactId());
        assertEquals(MvnProjectBuilder.DEFAULT_VERSION, project.getVersion());
        assertLocalDeps(project);
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
        assertLocalDeps(project, "root-module-not-direct-child", "root-no-parent-module");
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
        assertLocalDeps(project);
    }

    @Test
    public void loadNonModuleChildProject() throws Exception {
        final LocalProject project = LocalProject.loadWorkspace(workDir.resolve("root").resolve("non-module-child").resolve("target").resolve("classes"));
        assertNotNull(project);
        assertNotNull(project.getWorkspace());
        assertEquals("non-module-child", project.getArtifactId());
        final Map<AppArtifactKey, LocalProject> projects = project.getWorkspace().getProjects();
        assertEquals(6, projects.size());
        assertTrue(projects.containsKey(new AppArtifactKey(MvnProjectBuilder.DEFAULT_GROUP_ID, "root-no-parent-module")));
        assertTrue(projects.containsKey(new AppArtifactKey(MvnProjectBuilder.DEFAULT_GROUP_ID, "root-module-with-parent")));
        assertTrue(projects.containsKey(new AppArtifactKey(MvnProjectBuilder.DEFAULT_GROUP_ID, "root-module-not-direct-child")));
        assertTrue(projects.containsKey(new AppArtifactKey(MvnProjectBuilder.DEFAULT_GROUP_ID, "root")));
        assertTrue(projects.containsKey(new AppArtifactKey(MvnProjectBuilder.DEFAULT_GROUP_ID, "non-module-child")));
        assertTrue(projects.containsKey(new AppArtifactKey(MvnProjectBuilder.DEFAULT_GROUP_ID, "another-child")));
        assertLocalDeps(project, "another-child");
    }

    private void assertCompleteWorkspace(final LocalProject project) {
        final Map<AppArtifactKey, LocalProject> projects = project.getWorkspace().getProjects();
        assertEquals(4, projects.size());
        assertTrue(projects.containsKey(new AppArtifactKey(MvnProjectBuilder.DEFAULT_GROUP_ID, "root-no-parent-module")));
        assertTrue(projects.containsKey(new AppArtifactKey(MvnProjectBuilder.DEFAULT_GROUP_ID, "root-module-with-parent")));
        assertTrue(projects.containsKey(new AppArtifactKey(MvnProjectBuilder.DEFAULT_GROUP_ID, "root-module-not-direct-child")));
        assertTrue(projects.containsKey(new AppArtifactKey(MvnProjectBuilder.DEFAULT_GROUP_ID, "root")));
    }

    private static void assertLocalDeps(LocalProject project, String... deps) {
        final List<LocalProject> list = project.getSelfWithLocalDeps();
        assertEquals(deps.length + 1, list.size());
        int i = 0;
        while(i < deps.length) {
            final LocalProject dep = list.get(i);
            assertEquals(deps[i++], dep.getArtifactId());
            assertEquals(project.getGroupId(), dep.getGroupId());
        }
        final LocalProject self = list.get(i);
        assertEquals(project.getGroupId(), self.getGroupId());
        assertEquals(project.getArtifactId(), self.getArtifactId());
        assertEquals(project.getVersion(), self.getVersion());
    }
}
