import { describe, it, expect, beforeAll } from 'vitest';
import { createNodesV2, createDependencies, MavenPluginOptions } from '.';
import { CreateNodesContextV2, CreateDependenciesContext } from '@nx/devkit';

describe('Maven Plugin', () => {
  const workspaceRoot = __dirname;

  let analysisResults: any;

  beforeAll(async () => {
    const context: CreateNodesContextV2 = {
      nxJsonConfiguration: {},
      workspaceRoot,
      configFiles: ['pom.xml']
    };

    const options: MavenPluginOptions = {
      mavenExecutable: 'mvn',
      verbose: false
    };

    analysisResults = await createNodesV2[1](['pom.xml'], options, context);
  }, 60000);

  describe('createNodesV2', () => {
    it('should discover Maven projects', () => {
      expect(analysisResults.length).toBeGreaterThan(0);

      // Results are in format [configFile, resultData]
      let totalProjects = 0;
      analysisResults.forEach((resultTuple: any) => {
        const [configFile, resultData] = resultTuple;
        if (resultData && resultData.projects) {
          totalProjects += Object.keys(resultData.projects).length;
        }
      });

      expect(totalProjects).toBe(1267);
    });

    it('should discover specific projects of different types', () => {
      // Collect all discovered project names (using the name field)
      const allProjectNames = new Set();

      analysisResults.forEach((resultTuple: any) => {
        const [configFile, resultData] = resultTuple;
        if (resultData && resultData.projects) {
          Object.values(resultData.projects).forEach((projectData: any) => {
            if (projectData.name) {
              allProjectNames.add(projectData.name);
            }
          });
        }
      });

      // Verify we have a good variety of projects discovered

      // Test for specific known Maven artifact names across different areas of Quarkus
      const expectedProjects = [
        // Parent and config projects
        'io.quarkus:quarkus-parent',
        'io.quarkus:quarkus-ide-config',
        'io.quarkus:quarkus-enforcer-rules',

        // Independent projects (ARC CDI)
        'io.quarkus.arc:arc-parent',
        'io.quarkus.arc:arc',
        'io.quarkus.arc:arc-processor'
      ];

      expectedProjects.forEach(project => {
        expect(allProjectNames.has(project)).toBe(true);
      });
    });

    it('should create targets for Maven projects', () => {
      let hasTargets = false;

      analysisResults.forEach((resultTuple: any) => {
        const [configFile, resultData] = resultTuple;
        if (resultData && resultData.projects) {
          Object.values(resultData.projects).forEach((project: any) => {
            if (project.targets && Object.keys(project.targets).length > 0) {
              hasTargets = true;
            }
          });
        }
      });

      expect(hasTargets).toBe(true);
    });
  });

  describe('createDependencies', () => {
    it('should handle graceful failure with invalid Maven executable', async () => {
      const context: CreateDependenciesContext = {
        nxJsonConfiguration: {},
        workspaceRoot,
        externalNodes: {},
        projects: {},
        fileMap: { nonProjectFiles: [], projectFileMap: {} },
        filesToProcess: { nonProjectFiles: [], projectFileMap: {} }
      };

      const options: MavenPluginOptions = {
        mavenExecutable: 'non-existent-command'
      };

      const result = await createDependencies(options, context);
      expect(Array.isArray(result)).toBe(true);
    });
  });
});
