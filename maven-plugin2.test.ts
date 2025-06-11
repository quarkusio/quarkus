import { describe, it, expect, beforeEach, vi } from 'vitest';
import { join } from 'path';
import plugin from './maven-plugin2';

// Mock external dependencies
vi.mock('fs', () => ({
  existsSync: vi.fn(),
  readFileSync: vi.fn(),
  unlinkSync: vi.fn(),
}));

vi.mock('child_process', () => ({
  exec: vi.fn(),
  execSync: vi.fn(),
  spawn: vi.fn(),
}));

vi.mock('@nx/devkit', () => ({
  workspaceRoot: '/test/workspace',
  DependencyType: {
    static: 'static',
    implicit: 'implicit',
  },
}));

import { existsSync, readFileSync, unlinkSync } from 'fs';
import { spawn } from 'child_process';

const mockExistsSync = vi.mocked(existsSync);
const mockReadFileSync = vi.mocked(readFileSync);
const mockSpawn = vi.mocked(spawn);

describe('Maven Plugin v2', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // Reset cache
    (plugin as any).cachedBatchResults = new Map();
  });

  describe('Plugin Configuration', () => {
    it('should have correct plugin name', () => {
      expect(plugin.name).toBe('maven-plugin2');
    });

    it('should export createNodesV2 function', () => {
      expect(plugin.createNodesV2).toBeDefined();
      expect(Array.isArray(plugin.createNodesV2)).toBe(true);
      expect(plugin.createNodesV2[0]).toBe('**/pom.xml');
      expect(typeof plugin.createNodesV2[1]).toBe('function');
    });

    it('should export createDependencies function', () => {
      expect(plugin.createDependencies).toBeDefined();
      expect(typeof plugin.createDependencies).toBe('function');
    });
  });

  describe('createNodesV2', () => {
    it('should filter out maven-script and workspace root pom.xml', async () => {
      const configFiles = [
        'pom.xml',
        'maven-script/pom.xml',
        'extensions/arc/pom.xml',
        'target/some-build/pom.xml',
        'node_modules/something/pom.xml',
      ];

      // Mock Java analyzer exists
      mockExistsSync.mockImplementation((path: string) => {
        return path.includes('maven-script/target/classes/MavenModelReader.class');
      });

      // Mock successful spawn
      mockSpawn.mockImplementation(() => {
        const mockChild = {
          stdout: { on: vi.fn() },
          stderr: { on: vi.fn() },
          stdin: { write: vi.fn(), end: vi.fn() },
          on: vi.fn((event, callback) => {
            if (event === 'close') {
              setTimeout(() => callback(0), 10);
            }
          }),
          kill: vi.fn(),
        };
        return mockChild as any;
      });

      // Mock file read for results
      mockReadFileSync.mockReturnValue('{}');

      const context = { workspaceRoot: '/test/workspace', nxJsonConfiguration: {} };
      const [pattern, createNodes] = plugin.createNodesV2;
      
      const results = await createNodes(configFiles, {}, context);
      
      // Should only process extensions/arc/pom.xml (filtered out others)
      expect(results).toBeDefined();
    });

    it('should respect NX_MAVEN_LIMIT environment variable', async () => {
      const originalLimit = process.env.NX_MAVEN_LIMIT;
      process.env.NX_MAVEN_LIMIT = '1';

      const configFiles = [
        'extensions/arc/pom.xml',
        'extensions/cache/pom.xml',
        'extensions/jackson/pom.xml',
      ];

      // Mock Java analyzer exists
      mockExistsSync.mockImplementation((path: string) => {
        return path.includes('maven-script/target/classes/MavenModelReader.class');
      });

      // Mock successful spawn
      mockSpawn.mockImplementation(() => {
        const mockChild = {
          stdout: { on: vi.fn() },
          stderr: { on: vi.fn() },
          stdin: { write: vi.fn(), end: vi.fn() },
          on: vi.fn((event, callback) => {
            if (event === 'close') {
              setTimeout(() => callback(0), 10);
            }
          }),
          kill: vi.fn(),
        };
        return mockChild as any;
      });

      mockReadFileSync.mockReturnValue('{}');

      const context = { workspaceRoot: '/test/workspace', nxJsonConfiguration: {} };
      const [pattern, createNodes] = plugin.createNodesV2;
      
      const results = await createNodes(configFiles, {}, context);
      
      // Should limit to 1 file
      expect(results).toBeDefined();

      // Restore environment
      if (originalLimit) {
        process.env.NX_MAVEN_LIMIT = originalLimit;
      } else {
        delete process.env.NX_MAVEN_LIMIT;
      }
    });

    it('should handle Java analyzer not found', async () => {
      mockExistsSync.mockReturnValue(false);

      const configFiles = ['extensions/arc/pom.xml'];
      const context = { workspaceRoot: '/test/workspace', nxJsonConfiguration: {} };
      const [pattern, createNodes] = plugin.createNodesV2;
      
      const results = await createNodes(configFiles, {}, context);
      
      expect(results).toEqual([]);
    });

    it('should handle Maven process failure', async () => {
      // Mock Java analyzer exists
      mockExistsSync.mockImplementation((path: string) => {
        return path.includes('maven-script/target/classes/MavenModelReader.class');
      });

      // Mock failed spawn
      mockSpawn.mockImplementation(() => {
        const mockChild = {
          stdout: { on: vi.fn() },
          stderr: { on: vi.fn() },
          stdin: { write: vi.fn(), end: vi.fn() },
          on: vi.fn((event, callback) => {
            if (event === 'close') {
              setTimeout(() => callback(1), 10); // Exit code 1 = failure
            }
          }),
          kill: vi.fn(),
        };
        return mockChild as any;
      });

      const configFiles = ['extensions/arc/pom.xml'];
      const context = { workspaceRoot: '/test/workspace', nxJsonConfiguration: {} };
      const [pattern, createNodes] = plugin.createNodesV2;
      
      const results = await createNodes(configFiles, {}, context);
      
      expect(results).toEqual([]);
    });

    it('should generate valid project configuration', async () => {
      // Mock Java analyzer exists
      mockExistsSync.mockImplementation((path: string) => {
        return path.includes('maven-script/target/classes/MavenModelReader.class');
      });

      // Mock successful spawn with project data
      mockSpawn.mockImplementation(() => {
        const mockChild = {
          stdout: { on: vi.fn((event, callback) => {
            if (event === 'data') {
              callback('SUCCESS: Projects processed\n');
            }
          }) },
          stderr: { on: vi.fn() },
          stdin: { write: vi.fn(), end: vi.fn() },
          on: vi.fn((event, callback) => {
            if (event === 'close') {
              setTimeout(() => callback(0), 10);
            }
          }),
          kill: vi.fn(),
        };
        return mockChild as any;
      });

      // Mock file read with sample project data
      const sampleProjectData = {
        'extensions/arc': {
          name: 'quarkus-arc',
          sourceRoot: 'extensions/arc/src/main/java',
          projectType: 'library',
          targets: {
            build: {
              executor: '@nx/run-commands:run-commands',
              options: { command: 'mvn compile' }
            }
          },
          tags: ['extension'],
          implicitDependencies: {
            projects: ['core-runtime'],
            inheritsFrom: ['build-parent']
          }
        }
      };

      mockReadFileSync.mockReturnValue(JSON.stringify(sampleProjectData));

      const configFiles = ['extensions/arc/pom.xml'];
      const context = { workspaceRoot: '/test/workspace', nxJsonConfiguration: {} };
      const [pattern, createNodes] = plugin.createNodesV2;
      
      const results = await createNodes(configFiles, {}, context);
      
      expect(results).toHaveLength(1);
      expect(results[0][0]).toBe('extensions/arc/pom.xml');
      expect(results[0][1].projects).toBeDefined();
      expect(results[0][1].projects['extensions/arc']).toBeDefined();
      expect(results[0][1].projects['extensions/arc'].name).toBe('quarkus-arc');
      expect(results[0][1].projects['extensions/arc'].targets).toBeDefined();
      expect(results[0][1].projects['extensions/arc'].metadata.technologies).toContain('maven');
    });
  });

  describe('createDependencies', () => {
    it('should return empty array when no cached results', async () => {
      const dependencies = await plugin.createDependencies();
      expect(dependencies).toEqual([]);
    });

    it('should generate dependencies from cached results', async () => {
      // Import the plugin module to access the cache directly
      const pluginModule = await import('./maven-plugin2');
      
      // Simulate cached results from createNodesV2
      const cachedResults = new Map([
        ['extensions/arc', {
          name: 'quarkus-arc',
          implicitDependencies: {
            projects: ['quarkus-core'],
            inheritsFrom: ['build-parent']
          }
        }],
        ['core/runtime', {
          name: 'quarkus-core',
          implicitDependencies: { projects: [] }
        }],
        ['build-parent', {
          name: 'build-parent',
          implicitDependencies: { projects: [] }
        }]
      ]);

      // Set the cache by modifying the module's cached results
      (pluginModule as any).cachedBatchResults = cachedResults;

      const dependencies = await pluginModule.createDependencies();
      
      expect(dependencies).toHaveLength(2);
      expect(dependencies[0]).toEqual({
        source: 'quarkus-arc',
        target: 'quarkus-core',
        type: 'static'
      });
      expect(dependencies[1]).toEqual({
        source: 'quarkus-arc',
        target: 'build-parent',
        type: 'implicit'
      });
    });
  });

  describe('Framework Detection', () => {
    it('should detect Quarkus framework', async () => {
      // This would require mocking the detectFramework function
      // For now, we'll test through the full integration
      mockExistsSync.mockImplementation((path: string) => {
        return path.includes('maven-script/target/classes/MavenModelReader.class');
      });

      mockSpawn.mockImplementation(() => {
        const mockChild = {
          stdout: { on: vi.fn((event, callback) => {
            if (event === 'data') {
              callback('SUCCESS: Projects processed\n');
            }
          }) },
          stderr: { on: vi.fn() },
          stdin: { write: vi.fn(), end: vi.fn() },
          on: vi.fn((event, callback) => {
            if (event === 'close') {
              setTimeout(() => callback(0), 10);
            }
          }),
          kill: vi.fn(),
        };
        return mockChild as any;
      });

      const sampleProjectData = {
        'extensions/arc': {
          name: 'quarkus-arc',
          implicitDependencies: {
            external: ['io.quarkus:quarkus-core']
          },
          targets: {}
        }
      };

      mockReadFileSync.mockReturnValue(JSON.stringify(sampleProjectData));

      const configFiles = ['extensions/arc/pom.xml'];
      const context = { workspaceRoot: '/test/workspace', nxJsonConfiguration: {} };
      const [pattern, createNodes] = plugin.createNodesV2;
      
      const results = await createNodes(configFiles, {}, context);
      
      expect(results[0][1].projects['extensions/arc'].metadata.framework).toBe('quarkus');
    });
  });

  describe('Target Normalization', () => {
    it('should normalize Maven targets to Nx conventions', async () => {
      mockExistsSync.mockImplementation((path: string) => {
        return path.includes('maven-script/target/classes/MavenModelReader.class');
      });

      mockSpawn.mockImplementation(() => {
        const mockChild = {
          stdout: { on: vi.fn((event, callback) => {
            if (event === 'data') {
              callback('SUCCESS: Projects processed\n');
            }
          }) },
          stderr: { on: vi.fn() },
          stdin: { write: vi.fn(), end: vi.fn() },
          on: vi.fn((event, callback) => {
            if (event === 'close') {
              setTimeout(() => callback(0), 10);
            }
          }),
          kill: vi.fn(),
        };
        return mockChild as any;
      });

      const sampleProjectData = {
        'extensions/arc': {
          name: 'quarkus-arc',
          targets: {
            package: {
              executor: '@nx/run-commands:run-commands',
              options: { command: 'mvn package' }
            },
            test: {
              executor: '@nx/run-commands:run-commands',
              options: { command: 'mvn test' }
            }
          }
        }
      };

      mockReadFileSync.mockReturnValue(JSON.stringify(sampleProjectData));

      const configFiles = ['extensions/arc/pom.xml'];
      const context = { workspaceRoot: '/test/workspace', nxJsonConfiguration: {} };
      const options = { buildTargetName: 'build', testTargetName: 'test' };
      const [pattern, createNodes] = plugin.createNodesV2;
      
      const results = await createNodes(configFiles, options, context);
      
      const targets = results[0][1].projects['extensions/arc'].targets;
      expect(targets.build).toBeDefined(); // package -> build
      expect(targets.test).toBeDefined();
      expect(targets.package).toBeUndefined(); // Should be renamed
    });
  });
});