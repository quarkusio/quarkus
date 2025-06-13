import {
  CreateDependencies,
  CreateNodesV2,
  CreateNodesContextV2,
  CreateNodesResultV2,
  workspaceRoot,
  readJsonFile,
  writeJsonFile,
} from '@nx/devkit';
import { dirname, join } from 'path';
import { existsSync, readFileSync } from 'fs';
import { spawn } from 'child_process';
import { workspaceDataDirectory } from 'nx/src/utils/cache-directory';
import { hashObject } from 'nx/src/devkit-internals';
import { calculateHashForCreateNodes } from '@nx/devkit/src/utils/calculate-hash-for-create-nodes';

export interface MavenPluginOptions {
  mavenExecutable?: string;
}

const DEFAULT_OPTIONS: MavenPluginOptions = {
  mavenExecutable: 'mvn',
};

// Cache management functions
function readMavenCache(cachePath: string): Record<string, any> {
  try {
    return existsSync(cachePath) ? readJsonFile(cachePath) : {};
  } catch {
    return {};
  }
}

function writeMavenCache(cachePath: string, results: any) {
  try {
    writeJsonFile(cachePath, results);
  } catch (error) {
    console.warn('Failed to write Maven cache:', error.message);
  }
}

/**
 * Maven plugin that delegates to Java for analysis and returns results directly
 */
export const createNodesV2: CreateNodesV2 = [
  '**/pom.xml',
  async (configFiles, options, context): Promise<CreateNodesResultV2> => {
    const opts: MavenPluginOptions = { ...DEFAULT_OPTIONS, ...(options as MavenPluginOptions) };

    if ((options as any)?.verbose !== false) {
      console.log(`Maven plugin found ${configFiles.length} pom.xml files`);
    }

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
      // Generate cache key based on pom.xml files and options
      const projectHash = await calculateHashForCreateNodes(
        workspaceRoot,
        (options as object) ?? {},
        context,
        ['{projectRoot}/pom.xml', '{workspaceRoot}/**/pom.xml']
      );
      const optionsHash = hashObject(opts);
      const cacheKey = `nodes-${projectHash}-${optionsHash}`;
      
      // Set up cache path
      const cachePath = join(workspaceDataDirectory, 'maven-analysis-cache.json');
      const cache = readMavenCache(cachePath);
      
      // Check if we have valid cached results
      if (cache[cacheKey]) {
        if ((options as any)?.verbose !== false) {
          console.log('Using cached Maven analysis results for createNodes');
        }
        return cache[cacheKey];
      }

      // Run analysis if not cached
      const result = await runMavenAnalysis(opts);
      const createNodesResults = result.createNodesResults || [];

      // Cache the results
      cache[cacheKey] = createNodesResults;
      writeMavenCache(cachePath, cache);

      return createNodesResults;

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
  const opts: MavenPluginOptions = { ...DEFAULT_OPTIONS, ...(options as MavenPluginOptions) };

  try {
    // Generate cache key based on pom.xml files and options
    const projectHash = await calculateHashForCreateNodes(
      workspaceRoot,
      (options as object) ?? {},
      context,
      ['{projectRoot}/pom.xml', '{workspaceRoot}/**/pom.xml']
    );
    const optionsHash = hashObject(opts);
    const cacheKey = `deps-${projectHash}-${optionsHash}`;
    
    // Set up cache path
    const cachePath = join(workspaceDataDirectory, 'maven-analysis-cache.json');
    const cache = readMavenCache(cachePath);
    
    // Check if we have valid cached results
    if (cache[cacheKey]) {
      if ((options as any)?.verbose !== false) {
        console.log('Using cached Maven analysis results for createDependencies');
      }
      return cache[cacheKey];
    }

    // Run analysis if not cached
    const result = await runMavenAnalysis(opts);
    const createDependencies = result.createDependencies || [];

    // Cache the results
    cache[cacheKey] = createDependencies;
    writeMavenCache(cachePath, cache);

    return createDependencies;

  } catch (error) {
    console.error(`Maven dependency analysis failed:`, error.message);
    return [];
  }
};

/**
 * Run Maven analysis using Java plugin
 */
async function runMavenAnalysis(options: MavenPluginOptions): Promise<any> {
  const outputFile = join(workspaceDataDirectory, 'maven-analysis.json');

  // Check if verbose mode is enabled
  const isVerbose = process.env.NX_VERBOSE_LOGGING === 'true' || process.argv.includes('--verbose');

  // Check if Java analyzer is available
  if (!findJavaAnalyzer()) {
    throw new Error('Maven analyzer not found. Please ensure maven-plugin is compiled.');
  }

  if (isVerbose) {
    console.log(`Running Maven analysis...`);
  }

  // Build Maven command arguments
  const mavenArgs = [
    'io.quarkus:maven-plugin:analyze',
    `-Dnx.outputFile=${outputFile}`,
    `-Dnx.verbose=${isVerbose}`
  ];

  // Always use quiet mode to suppress expected reactor dependency warnings
  // These warnings are normal in large multi-module projects and don't affect functionality
  mavenArgs.push('-q');

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
    join(workspaceRoot, 'maven-plugin/target/classes'),
    join(workspaceRoot, 'maven-plugin/target/maven-plugin-999-SNAPSHOT.jar'),
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
  name: 'maven-plugin',
  createNodesV2,
  createDependencies,
};