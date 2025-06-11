import {
  CreateNodesV2,
  CreateNodesContextV2,
  CreateNodesResult,
  TargetConfiguration,
  workspaceRoot,
} from '@nx/devkit';
import { dirname, join } from 'path';
import { spawn } from 'child_process';

export interface MavenPluginOptions {
  buildTargetName?: string;
  testTargetName?: string;
  mavenExecutable?: string;
}

const DEFAULT_OPTIONS: MavenPluginOptions = {
  buildTargetName: 'build',
  testTargetName: 'test',
  mavenExecutable: 'mvn',
};

/**
 * Simplified Maven plugin that processes files one by one
 */
export const createNodesV2: CreateNodesV2 = [
  '**/pom.xml',
  async (configFiles, options, context) => {
    const opts: MavenPluginOptions = Object.assign({}, DEFAULT_OPTIONS, options || {});

    console.log(`Found ${configFiles.length} pom.xml files`);

    // Filter out unwanted files
    const filteredConfigFiles = configFiles.filter(file =>
      !file.includes('maven-script/pom.xml') &&
      !file.includes('target/') &&
      !file.includes('node_modules/') &&
      file !== 'pom.xml'
    );

    // Apply strict limit for testing
    const maxFiles = 10; // Very conservative limit
    if (filteredConfigFiles.length > maxFiles) {
      console.log(`Limiting to first ${maxFiles} files to prevent system overload`);
      filteredConfigFiles.splice(maxFiles);
    }

    console.log(`Processing ${filteredConfigFiles.length} files individually...`);

    const results: Array<[string, CreateNodesResult]> = [];

    // Process files one by one with error handling
    for (let i = 0; i < filteredConfigFiles.length; i++) {
      const configFile = filteredConfigFiles[i];
      console.log(`Processing file ${i + 1}/${filteredConfigFiles.length}: ${configFile}`);

      try {
        const nxConfig = await processSingleMavenFile(configFile, opts);
        if (nxConfig) {
          const projectRoot = dirname(configFile);
          const result = {
            projects: {
              [projectRoot]: {
                name: nxConfig.name,
                root: projectRoot,
                sourceRoot: join(projectRoot, 'src/main/java'),
                projectType: 'library' as const,
                targets: {
                  [opts.buildTargetName || 'build']: {
                    executor: '@nx/maven:build',
                    options: { command: 'compile' }
                  },
                  [opts.testTargetName || 'test']: {
                    executor: '@nx/maven:test',
                    options: { command: 'test' }
                  }
                },
                tags: ['maven'],
                metadata: { technologies: ['maven', 'java'] }
              }
            }
          };
          results.push([configFile, result]);
        }
      } catch (error) {
        console.warn(`Failed to process ${configFile}: ${error.message}`);
      }
    }

    console.log(`Successfully processed ${results.length} projects`);
    return results;
  },
];

/**
 * Process a single Maven file using a simple approach
 */
async function processSingleMavenFile(
  pomPath: string,
  options: MavenPluginOptions
): Promise<{ name: string } | null> {
  
  const mavenScriptDir = join(workspaceRoot, 'maven-script');
  const absolutePomPath = pomPath.startsWith('/') ? pomPath : join(workspaceRoot, pomPath);
  
  return new Promise((resolve, reject) => {
    const timeout = setTimeout(() => {
      child.kill('SIGTERM');
      reject(new Error('Process timeout after 30 seconds'));
    }, 30000);

    const child = spawn(options.mavenExecutable, [
      'exec:java',
      '-Dexec.mainClass=MavenModelReader',
      '-Dexec.args=--stdin --nx',
      '-q'
    ], {
      cwd: mavenScriptDir,
      stdio: ['pipe', 'pipe', 'pipe']
    });

    let stdout = '';
    let stderr = '';

    child.stdout?.on('data', (data) => {
      stdout += data.toString();
    });

    child.stderr?.on('data', (data) => {
      stderr += data.toString();
    });

    child.on('close', (code) => {
      clearTimeout(timeout);
      if (code === 0) {
        try {
          const result = JSON.parse(stdout);
          const firstProject = Object.values(result)[0] as any;
          resolve(firstProject || null);
        } catch (error) {
          reject(new Error(`JSON parse error: ${error.message}`));
        }
      } else {
        reject(new Error(`Maven process failed with code ${code}`));
      }
    });

    child.on('error', (error) => {
      clearTimeout(timeout);
      reject(error);
    });

    // Send single file path
    child.stdin?.write(absolutePomPath + '\n');
    child.stdin?.end();
  });
}

export default {
  name: 'maven-plugin-simple',
  createNodesV2,
};