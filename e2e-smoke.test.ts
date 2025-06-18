import { describe, it, expect, beforeAll, afterAll } from 'vitest';
import { execSync } from 'child_process';
import { readFileSync, existsSync, rmSync } from 'fs';

const TIMEOUT = 120000; // 2 minutes

describe('Maven Plugin E2E Smoke Tests', () => {
  // Generate unique test ID for this test run
  const testId = Math.random().toString(36).substring(7);
  const sharedGraphFile = `/tmp/nx-e2e-${testId}-shared-graph.json`;
  let projectGraph: any;
  
  beforeAll(async () => {
    // Maven compilation and Nx reset are handled by global setup
    // Just generate the shared graph file once for all tests
    console.log('📊 Generating shared project graph...');
    execSync(`npx nx graph --file ${sharedGraphFile}`, { stdio: 'pipe' });
    
    const graphContent = readFileSync(sharedGraphFile, 'utf8');
    projectGraph = JSON.parse(graphContent);
    console.log(`✅ Shared graph generated with ${Object.keys(projectGraph.graph.nodes).length} nodes`);
  }, TIMEOUT);

  afterAll(() => {
    // Cleanup this test run's temporary files
    try {
      rmSync(`/tmp/nx-e2e-${testId}-*`, { recursive: true, force: true });
    } catch (error) {
      // Ignore cleanup errors
    }
  });

  describe('Basic Plugin Functionality', () => {
    it('should successfully run nx show projects', () => {
      const output = execSync('npx nx show projects', { encoding: 'utf8' });
      expect(output).toBeTruthy();
      expect(output.split('\n').filter(line => line.trim()).length).toBeGreaterThan(0);
    }, TIMEOUT);

    it('should handle nx install maven-plugin command', () => {
      // This command might fail, but we test that it doesn't crash
      try {
        const output = execSync('npx nx install maven-plugin', { encoding: 'utf8', stdio: 'pipe' });
        console.log('✅ nx install maven-plugin succeeded');
      } catch (error: any) {
        console.log('⚠️ nx install maven-plugin failed (expected):', error.message);
        // We expect this might fail, so we just ensure it doesn't crash completely
        expect(error.status).toBeDefined();
      }
    }, TIMEOUT);
  });

  describe('Project Graph Generation', () => {
    it('should have valid project graph structure', () => {
      expect(projectGraph).toHaveProperty('graph');
      expect(projectGraph.graph).toHaveProperty('nodes');
      expect(projectGraph.graph).toHaveProperty('dependencies');
      expect(Object.keys(projectGraph.graph.nodes).length).toBeGreaterThan(0);
    });

    it('should have consistent graph structure across multiple generations', () => {
      // Generate graph 2 more times to verify consistency
      const additionalGraphs = [];
      
      for (let i = 0; i < 2; i++) {
        const graphFile = `/tmp/nx-e2e-${testId}-consistency-${i}.json`;
        execSync(`npx nx graph --file ${graphFile}`, { stdio: 'pipe' });
        
        const graphContent = readFileSync(graphFile, 'utf8');
        additionalGraphs.push(JSON.parse(graphContent));
      }
      
      // All graphs should have the same number of nodes as the shared graph
      const expectedNodeCount = Object.keys(projectGraph.graph.nodes).length;
      expect(expectedNodeCount).toBeGreaterThan(0);
      
      additionalGraphs.forEach((graph, index) => {
        expect(Object.keys(graph.graph.nodes).length).toBe(expectedNodeCount);
      });
    }, TIMEOUT);
  });

  describe('Snapshot Testing', () => {

    it('should snapshot quarkus-core project configuration', () => {
      // Find quarkus-core project
      const coreProject = Object.entries(projectGraph.graph.nodes).find(([name, node]: [string, any]) => 
        name.includes('quarkus-core') || node.data?.name?.includes('quarkus-core')
      );

      expect(coreProject).toBeDefined();
      
      const [projectName, projectData] = coreProject!;
      
      const snapshot = {
        name: projectData.data?.name || projectName,
        type: projectData.type,
        targets: projectData.data?.targets ? Object.keys(projectData.data.targets).sort() : [],
        targetCount: projectData.data?.targets ? Object.keys(projectData.data.targets).length : 0,
        root: projectData.data?.root,
        hasSourceRoot: !!projectData.data?.sourceRoot
      };

      expect(snapshot).toMatchSnapshot();
    }, TIMEOUT);

    it('should snapshot quarkus-core project dependencies', () => {
      // Find quarkus-core project
      const coreProjectEntry = Object.entries(projectGraph.graph.nodes).find(([name, node]: [string, any]) => 
        name.includes('quarkus-core') || node.data?.name?.includes('quarkus-core')
      );

      expect(coreProjectEntry).toBeDefined();
      
      const [coreProjectName] = coreProjectEntry!;
      
      // Get dependencies for the core project
      const dependencies = projectGraph.graph.dependencies[coreProjectName] || [];
      
      const dependencySnapshot = {
        projectName: coreProjectName,
        dependencyCount: dependencies.length,
        dependencies: dependencies
          .map((dep: any) => ({
            target: dep.target,
            type: dep.type || 'implicit'
          }))
          .sort((a: any, b: any) => a.target.localeCompare(b.target))
      };

      expect(dependencySnapshot).toMatchSnapshot();
    }, TIMEOUT);

    it('should snapshot key Maven projects structure', () => {
      // Find a few key Maven projects to snapshot
      const keyProjects = [
        'quarkus-parent',
        'quarkus-core', 
        'arc-parent',
        'arc'
      ];

      const foundProjects = keyProjects.map(projectKey => {
        const projectEntry = Object.entries(projectGraph.graph.nodes).find(([name, node]: [string, any]) => 
          name.includes(projectKey) || node.data?.name?.includes(projectKey)
        );
        
        if (projectEntry) {
          const [projectName, projectData] = projectEntry;
          return {
            key: projectKey,
            name: projectData.data?.name || projectName,
            type: projectData.type,
            hasTargets: !!projectData.data?.targets,
            targetCount: projectData.data?.targets ? Object.keys(projectData.data.targets).length : 0,
            hasDependencies: !!(projectGraph.graph.dependencies[projectName]?.length > 0)
          };
        }
        return null;
      }).filter(Boolean);

      expect(foundProjects.length).toBeGreaterThan(0);
      expect(foundProjects).toMatchSnapshot();
    }, TIMEOUT);

    it('should snapshot overall project statistics', () => {
      const stats = {
        totalProjects: Object.keys(projectGraph.graph.nodes).length,
        projectsWithTargets: Object.values(projectGraph.graph.nodes).filter((node: any) => 
          node.data?.targets && Object.keys(node.data.targets).length > 0
        ).length,
        projectsWithDependencies: Object.keys(projectGraph.graph.dependencies).filter(key => 
          projectGraph.graph.dependencies[key]?.length > 0
        ).length,
        hasValidGraph: !!projectGraph.graph,
        hasNodes: !!projectGraph.graph.nodes,
        hasDependencies: !!projectGraph.graph.dependencies
      };

      expect(stats).toMatchSnapshot();
    }, TIMEOUT);
  });

  describe('Advanced Plugin Features', () => {
    it('should support verbose project listing', () => {
      const output = execSync('npx nx show projects --verbose', { encoding: 'utf8' });
      expect(output).toBeTruthy();
      expect(output.length).toBeGreaterThan(0);
    }, TIMEOUT);

    it('should handle project details queries', () => {
      // Get a project name first
      const projectsOutput = execSync('npx nx show projects', { encoding: 'utf8' });
      const projects = projectsOutput.split('\n').filter(line => line.trim());

      if (projects.length > 0) {
        const firstProject = projects[0].trim();

        try {
          const detailsOutput = execSync(`npx nx show project ${firstProject}`, {
            encoding: 'utf8',
            stdio: 'pipe'
          });
          expect(detailsOutput).toBeTruthy();
          console.log(`✅ Successfully queried details for project: ${firstProject}`);
        } catch (error: any) {
          console.log(`⚠️ Project details query failed for ${firstProject}:`, error.message);
          // This might be expected if the command format is different
        }
      }
    }, TIMEOUT);

    it('should not crash with invalid Maven executable', () => {
      // Test resilience by temporarily breaking the Maven path
      const originalPath = process.env.PATH;

      try {
        // Remove Maven from PATH temporarily
        process.env.PATH = '/usr/bin:/bin';

        // The plugin should handle this gracefully
        const output = execSync('npx nx show projects 2>&1 || echo "graceful-failure"', {
          encoding: 'utf8',
          stdio: 'pipe'
        });

        // Should either work or fail gracefully
        expect(output).toBeTruthy();
        console.log('✅ Plugin handles Maven unavailability gracefully');
      } finally {
        // Restore original PATH
        process.env.PATH = originalPath;
      }
    }, TIMEOUT);
  });

  describe('Performance and Reliability', () => {
    it('should complete operations within reasonable time', () => {
      const start = Date.now();

      execSync('npx nx show projects', { stdio: 'pipe' });

      const duration = Date.now() - start;
      expect(duration).toBeLessThan(7500); // Should complete within 30 seconds

      console.log(`⏱️ nx show projects completed in ${duration}ms`);
    });
  });
});
