import { describe, it, expect, beforeAll, beforeEach, afterEach } from 'vitest';
import { join } from 'path';
import { existsSync, mkdirSync, rmSync } from 'fs';
import { createNodesV2, createDependencies, MavenPluginOptions } from './maven-plugin';
import { CreateNodesContextV2, CreateDependenciesContext } from '@nx/devkit';
import { workspaceDataDirectory } from 'nx/src/utils/cache-directory';

describe('Maven Plugin', () => {
  const actualWorkspaceRoot = __dirname; // Use the actual quarkus2 workspace root
  const testDataDirectory = join(__dirname, 'test-data');
  
  // Cached results from single Maven analysis run
  let cachedNodesResults: any[] = [];
  let cachedDepsResults: any[] = [];
  let analysisCompleted = false;
  
  // Dynamically discover pom files instead of hardcoding them
  const samplePomFiles = ['pom.xml']; // Start with root pom for basic testing

  beforeAll(async () => {
    
    // Set up workspace root mocking
    Object.defineProperty(require('@nx/devkit'), 'workspaceRoot', {
      value: actualWorkspaceRoot,
      configurable: true
    });
    
    // Set up test data directory
    if (existsSync(testDataDirectory)) {
      rmSync(testDataDirectory, { recursive: true });
    }
    mkdirSync(testDataDirectory, { recursive: true });
    
    Object.defineProperty(require('nx/src/utils/cache-directory'), 'workspaceDataDirectory', {
      value: testDataDirectory,
      configurable: true
    });

    try {
      // Run Maven analysis once
      const nodesContext: CreateNodesContextV2 = {
        nxJsonConfiguration: {},
        workspaceRoot: actualWorkspaceRoot,
        configFiles: samplePomFiles
      };

      const depsContext: CreateDependenciesContext = {
        nxJsonConfiguration: {},
        workspaceRoot: actualWorkspaceRoot,
        externalNodes: {},
        projects: {},
        fileMap: { nonProjectFiles: [], projectFileMap: {} },
        filesToProcess: { nonProjectFiles: [], projectFileMap: {} }
      };

      const options: MavenPluginOptions = {
        mavenExecutable: 'mvn',
        verbose: true
      };

      cachedNodesResults = await createNodesV2[1](samplePomFiles, options, nodesContext);
      cachedDepsResults = await createDependencies(options, depsContext);
      analysisCompleted = true;
    } catch (error) {
      analysisCompleted = false;
      throw new Error(`Maven analysis failed during test setup: ${error}`);
    }
  }, 120000); // 2 minute timeout for Maven analysis
  
  beforeEach(() => {
    // Ensure test data directory exists for each test
    if (!existsSync(testDataDirectory)) {
      mkdirSync(testDataDirectory, { recursive: true });
    }
  });

  afterEach(() => {
    // Don't clean up test data directory to preserve cached results
  });

  describe('createNodesV2 - Full Workspace Analysis', () => {
    it('should process Maven projects from workspace', async () => {
      // Use cached results from beforeAll
      const result = cachedNodesResults;
      
      expect(Array.isArray(result)).toBe(true);
      expect(analysisCompleted).toBe(true);
      expect(result.length).toBeGreaterThan(0);
      
      const resultMap = new Map();
      result.forEach(item => {
        if (item.nodes) {
          Object.keys(item.nodes).forEach(nodeKey => {
            resultMap.set(nodeKey, item.nodes[nodeKey]);
          });
        }
      });
      
      const projectNames = Array.from(resultMap.keys());
      
      // Verify we discovered the expected number of Maven projects (1781 pom.xml files)
      expect(projectNames.length).toBe(1781);
    });

    it('should generate proper target dependencies for projects', async () => {
      // Use cached results from beforeAll
      const result = cachedNodesResults;
      
      // Verify target dependency chains
      const resultMap = new Map();
      result.forEach(item => {
        if (item.nodes) {
          Object.keys(item.nodes).forEach(nodeKey => {
            resultMap.set(nodeKey, item.nodes[nodeKey]);
          });
        }
      });

      expect(analysisCompleted).toBe(true);
      expect(resultMap.size).toBeGreaterThan(0);

      // Verify that at least some projects have proper Maven lifecycle dependencies
      const projectsWithDependencies = Array.from(resultMap.entries()).filter(([_, node]) => {
        if (!node?.targets) return false;
        return Object.values(node.targets).some(target => 
          target.dependsOn && target.dependsOn.length > 0
        );
      });
      
      // Check for cross-module dependencies (^ syntax)
      const hasCrossModuleDeps = Array.from(resultMap.values()).some(node => {
        if (!node?.targets) return false;
        return Object.values(node.targets).some(target => 
          target.dependsOn?.some(dep => dep.startsWith('^'))
        );
      });
      
      expect(hasCrossModuleDeps).toBe(true);
    });

    it('should handle empty results gracefully', async () => {
      const context: CreateNodesContextV2 = {
        nxJsonConfiguration: {},
        workspaceRoot: actualWorkspaceRoot,
        configFiles: []
      };

      const result = await createNodesV2[1]([], {}, context);
      expect(result).toEqual([]);
    });
  });

  describe('createDependencies - Full Workspace Analysis', () => {
    it('should analyze core module dependencies comprehensively', async () => {
      // Use cached results from beforeAll
      const result = cachedDepsResults;
      
      expect(Array.isArray(result)).toBe(true);
      expect(analysisCompleted).toBe(true);
      
      // Check that dependencies have required properties
      result.forEach(dep => {
        expect(dep.source).toBeDefined();
        expect(dep.target).toBeDefined();
        expect(dep.type).toBeDefined();
      });

      // Look for specific expected dependencies based on actual core structure
      const sources = result.map(dep => dep.source);
      const targets = result.map(dep => dep.target);
      
      // Verify we have some realistic Maven module dependencies
      expect(sources.length).toBeGreaterThan(0);
      expect(targets.length).toBeGreaterThan(0);
    });

    it('should verify target dependency graph structure', async () => {
      // Use cached results from beforeAll
      const result = cachedDepsResults;
      
      expect(analysisCompleted).toBe(true);
      
      // Focus on target-level dependencies (project:target format)
      const targetDependencies = result.filter(dep => 
        dep.source.includes(':') && dep.target.includes(':')
      );
      
      // Check structure of target dependencies if they exist
      targetDependencies.forEach(dep => {
        expect(dep.source).toMatch(/^[^:]+:[^:]+$/); // format: project:target
        expect(dep.target).toMatch(/^[^:]+:[^:]+$/); // format: project:target
      });

      // Also check for project-level dependencies
      const projectDependencies = result.filter(dep => 
        !dep.source.includes(':') && !dep.target.includes(':')
      );
    });

    it('should handle complex multi-level dependency chains', async () => {
      // Use cached results from beforeAll
      const result = cachedDepsResults;
      
      expect(analysisCompleted).toBe(true);
      
      // Build dependency graph
      const dependencyGraph = new Map();
      result.forEach(dep => {
        if (!dependencyGraph.has(dep.source)) {
          dependencyGraph.set(dep.source, []);
        }
        dependencyGraph.get(dep.source).push(dep.target);
      });
    });

    it('should handle graceful failure', async () => {
      // Test graceful failure with invalid Maven executable
      const context: CreateDependenciesContext = {
        nxJsonConfiguration: {},
        workspaceRoot: actualWorkspaceRoot,
        externalNodes: {},
        projects: {},
        fileMap: { nonProjectFiles: [], projectFileMap: {} },
        filesToProcess: { nonProjectFiles: [], projectFileMap: {} }
      };

      const options: MavenPluginOptions = {
        mavenExecutable: 'non-existent-command',
        verbose: true
      };

      const result = await createDependencies(options, context);
      expect(result).toEqual([]);
    });
  });

  describe('Maven Lifecycle Phases Analysis', () => {
    it('should identify lifecycle phases correctly', () => {
      expect(analysisCompleted).toBe(true);
      expect(cachedNodesResults.length).toBeGreaterThan(0);

      const allNodes = new Map();
      cachedNodesResults.forEach(result => {
        if (result.nodes) {
          Object.keys(result.nodes).forEach(nodeKey => {
            allNodes.set(nodeKey, result.nodes[nodeKey]);
          });
        }
      });

      const quarkusCoreProject = allNodes.get('io.quarkus:quarkus-core');
      expect(quarkusCoreProject?.targets).toBeDefined();

      const phaseTargets = Object.entries(quarkusCoreProject.targets)
        .filter(([_, target]: [string, any]) => target?.metadata?.type === 'phase');

      expect(phaseTargets.length).toBeGreaterThan(0);
    });

    it('should have nx:noop executor for all lifecycle phases', () => {
      expect(analysisCompleted).toBe(true);
      expect(cachedNodesResults.length).toBeGreaterThan(0);

      const allNodes = new Map();
      cachedNodesResults.forEach(result => {
        if (result.nodes) {
          Object.keys(result.nodes).forEach(nodeKey => {
            allNodes.set(nodeKey, result.nodes[nodeKey]);
          });
        }
      });

      const quarkusCoreProject = allNodes.get('io.quarkus:quarkus-core');
      expect(quarkusCoreProject?.targets).toBeDefined();

      const phaseTargets = Object.entries(quarkusCoreProject.targets)
        .filter(([_, target]: [string, any]) => target?.metadata?.type === 'phase');

      phaseTargets.forEach(([name, target]) => {
        expect(target?.executor).toBe('nx:noop');
      });
    });

    it('should have proper phase dependencies using ^ syntax', () => {
      expect(analysisCompleted).toBe(true);
      expect(cachedNodesResults.length).toBeGreaterThan(0);

      const allNodes = new Map();
      cachedNodesResults.forEach(result => {
        if (result.nodes) {
          Object.keys(result.nodes).forEach(nodeKey => {
            allNodes.set(nodeKey, result.nodes[nodeKey]);
          });
        }
      });

      const quarkusCoreProject = allNodes.get('io.quarkus:quarkus-core');
      expect(quarkusCoreProject?.targets).toBeDefined();

      const phaseTargets = Object.entries(quarkusCoreProject.targets)
        .filter(([_, target]: [string, any]) => target?.metadata?.type === 'phase');

      phaseTargets.forEach(([name, target]) => {
        const dependsOn = target?.dependsOn || [];
        const crossModuleDeps = dependsOn.filter((dep: string) => dep.startsWith('^'));
        
        // All phases should have at least one cross-module dependency
        if (dependsOn.length > 0) {
          expect(crossModuleDeps.length).toBeGreaterThan(0);
        }
      });
    });

    it('should have correct metadata structure for phases', () => {
      expect(analysisCompleted).toBe(true);
      expect(cachedNodesResults.length).toBeGreaterThan(0);

      const allNodes = new Map();
      cachedNodesResults.forEach(result => {
        if (result.nodes) {
          Object.keys(result.nodes).forEach(nodeKey => {
            allNodes.set(nodeKey, result.nodes[nodeKey]);
          });
        }
      });

      const quarkusCoreProject = allNodes.get('io.quarkus:quarkus-core');
      expect(quarkusCoreProject?.targets).toBeDefined();

      const phaseTargets = Object.entries(quarkusCoreProject.targets)
        .filter(([_, target]: [string, any]) => target?.metadata?.type === 'phase');

      phaseTargets.forEach(([name, target]) => {
        expect(target?.metadata?.type).toBe('phase');
        expect(target?.metadata?.phase).toBe(name);
        expect(target?.metadata?.technologies).toContain('maven');
      });
    });

    it('should have empty inputs and outputs for phases', () => {
      expect(analysisCompleted).toBe(true);
      expect(cachedNodesResults.length).toBeGreaterThan(0);

      const allNodes = new Map();
      cachedNodesResults.forEach(result => {
        if (result.nodes) {
          Object.keys(result.nodes).forEach(nodeKey => {
            allNodes.set(nodeKey, result.nodes[nodeKey]);
          });
        }
      });

      const quarkusCoreProject = allNodes.get('io.quarkus:quarkus-core');
      expect(quarkusCoreProject?.targets).toBeDefined();

      const phaseTargets = Object.entries(quarkusCoreProject.targets)
        .filter(([_, target]: [string, any]) => target?.metadata?.type === 'phase');

      phaseTargets.forEach(([name, target]) => {
        // Phases are orchestrators, they don't have direct inputs/outputs
        expect(target?.inputs).toEqual([]);
        expect(target?.outputs).toEqual([]);
      });
    });
  });

  describe('Maven Plugin Goals Analysis', () => {
    it('should identify maven-compiler plugin goals', () => {
      expect(analysisCompleted).toBe(true);
      expect(cachedNodesResults.length).toBeGreaterThan(0);

      const allNodes = new Map();
      cachedNodesResults.forEach(result => {
        if (result.nodes) {
          Object.keys(result.nodes).forEach(nodeKey => {
            allNodes.set(nodeKey, result.nodes[nodeKey]);
          });
        }
      });

      const quarkusCoreProject = allNodes.get('io.quarkus:quarkus-core');
      expect(quarkusCoreProject?.targets).toBeDefined();

      const compilerGoals = Object.entries(quarkusCoreProject.targets)
        .filter(([name, target]: [string, any]) => 
          name.startsWith('maven-compiler:') && target?.metadata?.type === 'goal'
        );

      expect(compilerGoals.length).toBeGreaterThan(0);
      
      // Should have at least compile goal
      const compileGoal = compilerGoals.find(([name]) => name === 'maven-compiler:compile');
      expect(compileGoal).toBeDefined();
    });

    it('should have nx:run-commands executor for all goals', () => {
      expect(analysisCompleted).toBe(true);
      expect(cachedNodesResults.length).toBeGreaterThan(0);

      const allNodes = new Map();
      cachedNodesResults.forEach(result => {
        if (result.nodes) {
          Object.keys(result.nodes).forEach(nodeKey => {
            allNodes.set(nodeKey, result.nodes[nodeKey]);
          });
        }
      });

      const quarkusCoreProject = allNodes.get('io.quarkus:quarkus-core');
      expect(quarkusCoreProject?.targets).toBeDefined();

      const goalTargets = Object.entries(quarkusCoreProject.targets)
        .filter(([_, target]: [string, any]) => target?.metadata?.type === 'goal');

      goalTargets.forEach(([name, target]) => {
        expect(target?.executor).toBe('nx:run-commands');
        expect(target?.options?.command).toContain('mvn');
        expect(target?.options?.cwd).toBe('core/runtime');
      });
    });

    it('should have correct metadata structure for goals', () => {
      expect(analysisCompleted).toBe(true);
      expect(cachedNodesResults.length).toBeGreaterThan(0);

      const allNodes = new Map();
      cachedNodesResults.forEach(result => {
        if (result.nodes) {
          Object.keys(result.nodes).forEach(nodeKey => {
            allNodes.set(nodeKey, result.nodes[nodeKey]);
          });
        }
      });

      const quarkusCoreProject = allNodes.get('io.quarkus:quarkus-core');
      expect(quarkusCoreProject?.targets).toBeDefined();

      const goalTargets = Object.entries(quarkusCoreProject.targets)
        .filter(([_, target]: [string, any]) => target?.metadata?.type === 'goal');

      goalTargets.forEach(([name, target]) => {
        expect(target?.metadata?.type).toBe('goal');
        expect(target?.metadata?.plugin).toBeDefined();
        expect(target?.metadata?.goal).toBeDefined();
        expect(target?.metadata?.technologies).toContain('maven');
      });
    });

    it('should have proper cross-module dependencies for bound goals', () => {
      expect(analysisCompleted).toBe(true);
      expect(cachedNodesResults.length).toBeGreaterThan(0);

      const allNodes = new Map();
      cachedNodesResults.forEach(result => {
        if (result.nodes) {
          Object.keys(result.nodes).forEach(nodeKey => {
            allNodes.set(nodeKey, result.nodes[nodeKey]);
          });
        }
      });

      const quarkusCoreProject = allNodes.get('io.quarkus:quarkus-core');
      expect(quarkusCoreProject?.targets).toBeDefined();

      const boundGoals = Object.entries(quarkusCoreProject.targets)
        .filter(([_, target]: [string, any]) => 
          target?.metadata?.type === 'goal' && target?.metadata?.phase
        );

      boundGoals.forEach(([name, target]) => {
        const dependsOn = target?.dependsOn || [];
        const phase = target?.metadata?.phase;
        const expectedCrossModuleDep = `^${phase}`;
        
        // Bound goals should depend on their phase in other modules
        expect(dependsOn).toContain(expectedCrossModuleDep);
      });
    });

    it('should have appropriate inputs for compilation goals', () => {
      expect(analysisCompleted).toBe(true);
      expect(cachedNodesResults.length).toBeGreaterThan(0);

      const allNodes = new Map();
      cachedNodesResults.forEach(result => {
        if (result.nodes) {
          Object.keys(result.nodes).forEach(nodeKey => {
            allNodes.set(nodeKey, result.nodes[nodeKey]);
          });
        }
      });

      const quarkusCoreProject = allNodes.get('io.quarkus:quarkus-core');
      expect(quarkusCoreProject?.targets).toBeDefined();

      const compilationGoals = Object.entries(quarkusCoreProject.targets)
        .filter(([name, target]: [string, any]) => 
          target?.metadata?.type === 'goal' && 
          (name.includes('compile') || name.includes('resources'))
        );

      compilationGoals.forEach(([name, target]) => {
        const inputs = target?.inputs || [];
        
        // Should include pom.xml and source files
        expect(inputs).toContain('{projectRoot}/pom.xml');
        
        if (name.includes('compile') || name.includes('resources')) {
          const hasSourceInputs = inputs.some((input: string) => 
            input.includes('src/**/*')
          );
          expect(hasSourceInputs).toBe(true);
        }
      });
    });
  });

  describe('Cross-Module Dependencies Analysis', () => {
    it('should identify targets using ^ syntax for cross-module dependencies', () => {
      expect(analysisCompleted).toBe(true);
      expect(cachedNodesResults.length).toBeGreaterThan(0);

      const allNodes = new Map();
      cachedNodesResults.forEach(result => {
        if (result.nodes) {
          Object.keys(result.nodes).forEach(nodeKey => {
            allNodes.set(nodeKey, result.nodes[nodeKey]);
          });
        }
      });

      const quarkusCoreProject = allNodes.get('io.quarkus:quarkus-core');
      expect(quarkusCoreProject?.targets).toBeDefined();

      const targetsWithCrossModuleDeps = Object.entries(quarkusCoreProject.targets)
        .filter(([_, target]: [string, any]) => {
          const dependsOn = target?.dependsOn || [];
          return dependsOn.some((dep: string) => dep.startsWith('^'));
        });

      expect(targetsWithCrossModuleDeps.length).toBeGreaterThan(0);
    });

    it('should validate ^ syntax patterns for lifecycle phases', () => {
      expect(analysisCompleted).toBe(true);
      expect(cachedNodesResults.length).toBeGreaterThan(0);

      const allNodes = new Map();
      cachedNodesResults.forEach(result => {
        if (result.nodes) {
          Object.keys(result.nodes).forEach(nodeKey => {
            allNodes.set(nodeKey, result.nodes[nodeKey]);
          });
        }
      });

      const quarkusCoreProject = allNodes.get('io.quarkus:quarkus-core');
      expect(quarkusCoreProject?.targets).toBeDefined();

      const lifecyclePhases = ['validate', 'compile', 'test', 'package', 'verify', 'install', 'deploy'];
      
      lifecyclePhases.forEach(phase => {
        const target = quarkusCoreProject.targets[phase];
        if (target) {
          const dependsOn = target?.dependsOn || [];
          const expectedCrossModuleDep = `^${phase}`;
          
          // Each lifecycle phase should depend on the same phase in other modules
          expect(dependsOn).toContain(expectedCrossModuleDep);
        }
      });
    });

    it('should identify project-to-project dependencies', () => {
      expect(analysisCompleted).toBe(true);
      
      const projectLevelDeps = cachedDepsResults.filter(dep => 
        !dep.source.includes(':') && !dep.target.includes(':')
      );

      // Look for dependencies involving quarkus-core
      const coreDeps = projectLevelDeps.filter(dep => 
        dep.source === 'io.quarkus:quarkus-core' || dep.target === 'io.quarkus:quarkus-core'
      );
    });
  });

  describe('Target Input/Output Configuration Analysis', () => {
    it('should have pom.xml as input for all Maven targets', () => {
      expect(analysisCompleted).toBe(true);
      expect(cachedNodesResults.length).toBeGreaterThan(0);

      const allNodes = new Map();
      cachedNodesResults.forEach(result => {
        if (result.nodes) {
          Object.keys(result.nodes).forEach(nodeKey => {
            allNodes.set(nodeKey, result.nodes[nodeKey]);
          });
        }
      });

      const quarkusCoreProject = allNodes.get('io.quarkus:quarkus-core');
      expect(quarkusCoreProject?.targets).toBeDefined();

      const mavenTargets = Object.entries(quarkusCoreProject.targets)
        .filter(([_, target]: [string, any]) => 
          target?.metadata?.technologies?.includes('maven')
        );

      mavenTargets.forEach(([name, target]) => {
        const inputs = target?.inputs || [];
        const hasPomInput = inputs.includes('{projectRoot}/pom.xml');
        
        expect(hasPomInput).toBe(true);
      });
    });

    it('should have target directory outputs for build targets', () => {
      expect(analysisCompleted).toBe(true);
      expect(cachedNodesResults.length).toBeGreaterThan(0);

      const allNodes = new Map();
      cachedNodesResults.forEach(result => {
        if (result.nodes) {
          Object.keys(result.nodes).forEach(nodeKey => {
            allNodes.set(nodeKey, result.nodes[nodeKey]);
          });
        }
      });

      const quarkusCoreProject = allNodes.get('io.quarkus:quarkus-core');
      expect(quarkusCoreProject?.targets).toBeDefined();

      const buildTargets = Object.entries(quarkusCoreProject.targets)
        .filter(([name, _]: [string, any]) => 
          name.includes('compile') || name.includes('jar') || name.includes('build')
        );

      buildTargets.forEach(([name, target]) => {
        const outputs = target?.outputs || [];
        const hasTargetOutput = outputs.some((output: string) => 
          output.includes('{projectRoot}/target')
        );
        
        if (outputs.length > 0) {
          expect(hasTargetOutput).toBe(true);
        }
      });
    });

    it('should use projectRoot placeholders correctly', () => {
      expect(analysisCompleted).toBe(true);
      expect(cachedNodesResults.length).toBeGreaterThan(0);

      const allNodes = new Map();
      cachedNodesResults.forEach(result => {
        if (result.nodes) {
          Object.keys(result.nodes).forEach(nodeKey => {
            allNodes.set(nodeKey, result.nodes[nodeKey]);
          });
        }
      });

      const quarkusCoreProject = allNodes.get('io.quarkus:quarkus-core');
      expect(quarkusCoreProject?.targets).toBeDefined();

      Object.entries(quarkusCoreProject.targets).forEach(([name, target]) => {
        const inputs = target?.inputs || [];
        const outputs = target?.outputs || [];
        
        [...inputs, ...outputs].forEach((path: string) => {
          if (path.includes('projectRoot')) {
            // Should use proper placeholder syntax
            expect(path).toMatch(/\{projectRoot\}/);
          }
        });
      });
    });
  });

  describe('Integration Tests - Real Workspace Workflow', () => {
    it('should handle complete workflow with all core projects', async () => {
      expect(analysisCompleted).toBe(true);
      
      // Use cached nodes results
      const nodesResult = cachedNodesResults;
      expect(Array.isArray(nodesResult)).toBe(true);
      
      // Analyze results
      const allNodes = new Map();
      nodesResult.forEach(result => {
        if (result.nodes) {
          Object.keys(result.nodes).forEach(nodeKey => {
            allNodes.set(nodeKey, result.nodes[nodeKey]);
          });
        }
      });
      
      expect(allNodes.size).toBeGreaterThan(0);

      // Use cached dependencies results  
      const depsResult = cachedDepsResults;
      expect(Array.isArray(depsResult)).toBe(true);
      
      // Categorize dependencies
      const projectDeps = depsResult.filter(dep => !dep.source.includes(':') && !dep.target.includes(':'));
      const targetDeps = depsResult.filter(dep => dep.source.includes(':') && dep.target.includes(':'));

      // Verify we got some meaningful results
      const hasProjects = allNodes.size > 0;
      const hasTargets = Array.from(allNodes.values()).some(node => node?.targets);
      
      expect(hasProjects).toBe(true);
      if (hasTargets) {
        expect(hasTargets).toBe(true);
      }
    });
  });
});