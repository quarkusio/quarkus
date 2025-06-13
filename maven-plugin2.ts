import {
  CreateDependencies,
  CreateNodesV2,
  CreateNodesContextV2,
  CreateNodesResultV2,
  workspaceRoot,
} from '@nx/devkit';
import { dirname, join } from 'path';
import { existsSync, readFileSync } from 'fs';
import { spawn } from 'child_process';

export interface MavenPluginOptions {
  mavenExecutable?: string;
}

const DEFAULT_OPTIONS: MavenPluginOptions = {
  mavenExecutable: 'mvn',
};

/**
 * Maven plugin that delegates to Java for analysis and returns results directly
 */
export const createNodesV2: CreateNodesV2 = [
  '**/pom.xml',
  async (configFiles, options, context): Promise<CreateNodesResultV2> => {
    const opts: MavenPluginOptions = { ...DEFAULT_OPTIONS, ...options };

    console.log(`Maven plugin found ${configFiles.length} pom.xml files`);

    // Filter out unwanted pom.xml files
    const filteredFiles = configFiles.filter(file =>
      !file.includes('maven-script/') &&
      !file.includes('target/') &&
      !file.includes('node_modules/')
    );

    if (filteredFiles.length === 0) {
      return [];
    }

    try {
      const result = await runMavenAnalysis(opts);

      // Java returns exactly what we need - just return it directly
      return result.createNodesResults || [];

    } catch (error) {
      console.error(`Maven analysis failed:`, error.message);
      return [];
    }
  },
];

/**
 * Create dependencies using Java analysis results
 */
export const createDependencies: CreateDependencies = async (options, context) => {
  const opts: MavenPluginOptions = { ...DEFAULT_OPTIONS, ...options };

  try {
    const result = await runMavenAnalysis(opts);

    // Java returns exactly what we need - just return it directly
    return result.createDependencies || [];

  } catch (error) {
    console.error(`Maven dependency analysis failed:`, error.message);
    return [];
  }
};

/**
 * Run Maven analysis using Java plugin
 */
async function runMavenAnalysis(options: MavenPluginOptions): Promise<any> {
  const outputFile = join(workspaceRoot, 'maven-plugin-v2/maven-analysis.json');

  // Check if Java analyzer is available
  if (!findJavaAnalyzer()) {
    throw new Error('Maven analyzer not found. Please ensure maven-plugin-v2 is compiled.');
  }

  console.log(`Running Maven analysis...`);

  // Check if verbose mode is enabled
  const isVerbose = process.env.NX_VERBOSE_LOGGING === 'true' || process.argv.includes('--verbose');

  // Build Maven command arguments
  const mavenArgs = [
    'io.quarkus:maven-plugin-v2:analyze',
    `-Dnx.outputFile=${outputFile}`,
    `-Dnx.verbose=${isVerbose}`
  ];

  // Only add quiet flag if not in verbose mode
  if (!isVerbose) {
    mavenArgs.push('-q');
  }

  // Run Maven plugin
  await new Promise<void>((resolve, reject) => {
    const child = spawn(options.mavenExecutable, mavenArgs, {
      cwd: workspaceRoot,
      stdio: 'inherit'
    });

    // No timeout for very large codebases like Quarkus
    child.on('close', (code) => {
      if (code === 0) {
        resolve();
      } else {
        reject(new Error(`Maven process exited with code ${code}`));
      }
    });

    child.on('error', (error) => {
      reject(new Error(`Failed to spawn Maven process: ${error.message}`));
    });
  });

  // Read and return JSON result
  if (!existsSync(outputFile)) {
    throw new Error(`Output file not found: ${outputFile}`);
  }

  const jsonContent = readFileSync(outputFile, 'utf8');
  return JSON.parse(jsonContent);
}

/**
 * Find the compiled Java Maven analyzer
 */
function findJavaAnalyzer(): string | null {
  const possiblePaths = [
    join(workspaceRoot, 'maven-plugin-v2/target/classes'),
    join(workspaceRoot, 'maven-plugin-v2/target/maven-plugin-v2-999-SNAPSHOT.jar'),
  ];

  for (const path of possiblePaths) {
    if (existsSync(path)) {
      return path;
    }
  }

  return null;
}


/**
 * Plugin configuration
 */
export default {
  name: 'maven-plugin2',
  createNodesV2,
  createDependencies,
};
