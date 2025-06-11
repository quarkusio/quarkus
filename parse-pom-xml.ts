import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { parseString } from 'xml2js';
import { promisify } from 'node:util';
import { EffectivePomResolver } from './effective-pom-resolver';

// Performance optimizations
const parseXml = promisify(parseString);
const XML_PARSER_OPTIONS = {
    explicitArray: true,
    ignoreAttrs: true,
    trim: true,
    normalize: true
};

// Cache for parsed POMs to avoid re-parsing
const pomCache = new Map<string, MavenProjectInfo>();
const coordinateCache = new Map<string, string>();

export interface MavenPlugin {
  groupId: string;
  artifactId: string;
  version?: string;
  executions?: Array<{
    id?: string;
    phase?: string;
    goals?: string[];
  }>;
  configuration?: any;
}

export interface MavenProjectInfo {
  groupId: string;
  artifactId: string;
  version: string;
  packaging: string;
  modules?: string[];
  dependencies?: Array<{
    groupId: string;
    artifactId: string;
    version?: string;
    scope?: string;
    type?: string;
  }>;
  parent?: {
    groupId: string;
    version: string;
  };
  plugins?: MavenPlugin[];
  properties?: Record<string, string>;
}

export interface MavenReport {
  projects: Map<string, MavenProjectInfo>;
  projectToModules: Map<string, string[]>;
}

export async function parsePomXml(
  workspaceRoot: string,
  pomFiles: string[],
  useEffectivePom: boolean = false
): Promise<MavenReport> {
  const report: MavenReport = {
    projects: new Map(),
    projectToModules: new Map(),
  };

  // Process POMs in batches for better memory management
  const BATCH_SIZE = 100;
  
  for (let i = 0; i < pomFiles.length; i += BATCH_SIZE) {
    const batch = pomFiles.slice(i, i + BATCH_SIZE);
    
    // Process batch in parallel
    const batchPromises = batch.map(async (pomFile) => {
      try {
        // Check cache first
        const fullPath = pomFile.startsWith('/') ? pomFile : join(workspaceRoot, pomFile);
        const cacheKey = useEffectivePom ? `${fullPath}:effective` : fullPath;
        if (pomCache.has(cacheKey)) {
          return { pomFile, projectInfo: pomCache.get(cacheKey)! };
        }

        let pomContent: string;
        if (useEffectivePom) {
          try {
            pomContent = await getEffectivePomContent(fullPath, workspaceRoot);
            console.log(`[Maven Plugin] Successfully resolved effective POM for ${pomFile}`);
          } catch (error: any) {
            console.warn(`[Maven Plugin] Failed to resolve effective POM for ${pomFile}, falling back to raw POM:`, error.message);
            pomContent = readFileSync(fullPath, 'utf-8');
          }
        } else {
          pomContent = readFileSync(fullPath, 'utf-8');
        }
        
        const parsed = await parseXml(pomContent) as any;
        const project = parsed.project;

        if (!project) {
          return null;
        }

        const projectInfo: MavenProjectInfo = {
          groupId: project.groupId?.[0] || project.parent?.[0]?.groupId?.[0] || '',
          artifactId: project.artifactId?.[0] || '',
          version: project.version?.[0] || project.parent?.[0]?.version?.[0] || '',
          packaging: project.packaging?.[0] || 'jar',
        };

        // Add parent info if present
        if (project.parent?.[0]) {
          projectInfo.parent = {
            groupId: project.parent[0].groupId?.[0] || '',
            version: project.parent[0].version?.[0] || '',
          };
        }

        // Parse modules if present
        if (project.modules?.[0]?.module) {
          projectInfo.modules = project.modules[0].module;
        }

        // Parse dependencies if present - only extract essential info
        if (project.dependencies?.[0]?.dependency) {
          projectInfo.dependencies = project.dependencies[0].dependency
            .filter((dep: any) => dep.groupId?.[0] && dep.artifactId?.[0]) // Only valid deps
            .map((dep: any) => ({
              groupId: dep.groupId[0],
              artifactId: dep.artifactId[0],
              version: dep.version?.[0],
              scope: dep.scope?.[0] || 'compile',
              type: dep.type?.[0] || 'jar',
            }));
        }

        // Parse plugins only for essential info - skip heavy configuration parsing
        if (project.build?.[0]?.plugins?.[0]?.plugin) {
          projectInfo.plugins = project.build[0].plugins[0].plugin
            .filter((plugin: any) => plugin.groupId?.[0] && plugin.artifactId?.[0])
            .map((plugin: any) => ({
              groupId: plugin.groupId[0],
              artifactId: plugin.artifactId[0],
              version: plugin.version?.[0],
            }));
        }

        // Cache the result
        pomCache.set(cacheKey, projectInfo);
        
        // Cache coordinate lookup for faster dependency resolution
        const coordinate = `${projectInfo.groupId || projectInfo.parent?.groupId}.${projectInfo.artifactId}`;
        coordinateCache.set(coordinate, pomFile);

        return { pomFile, projectInfo };
      } catch (e) {
        console.error(`Error parsing POM file ${pomFile}:`, e);
        return null;
      }
    });

    // Wait for batch to complete
    const batchResults = await Promise.all(batchPromises);
    
    // Process results
    for (const result of batchResults) {
      if (result) {
        const { pomFile, projectInfo } = result;
        report.projects.set(pomFile, projectInfo);
        
        if (projectInfo.modules) {
          report.projectToModules.set(pomFile, projectInfo.modules);
        }
      }
    }
  }

  return report;
}

// Fast coordinate lookup using cache
export function findProjectByCoordinatesFromCache(
  groupId: string, 
  artifactId: string
): string | null {
  const coordinate = `${groupId}.${artifactId}`;
  return coordinateCache.get(coordinate) || null;
}

// Clear caches for testing/reset
export function clearParsingCaches(): void {
  pomCache.clear();
  coordinateCache.clear();
  effectivePomResolver.clearCache();
}

// Global effective POM resolver instance
const effectivePomResolver = new EffectivePomResolver();

// Get effective POM content using TypeScript implementation
async function getEffectivePomContent(pomPath: string, workspaceRoot: string): Promise<string> {
  try {
    const resolved = await effectivePomResolver.resolveEffectivePom(pomPath, workspaceRoot);
    
    // Convert back to XML format that our existing parser expects
    const xmlContent = `<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>${resolved.groupId}</groupId>
  <artifactId>${resolved.artifactId}</artifactId>
  <version>${resolved.version}</version>
  <packaging>${resolved.packaging}</packaging>
  ${resolved.parent ? `<parent>
    <groupId>${resolved.parent.groupId}</groupId>
    <artifactId>${resolved.parent.artifactId}</artifactId>
    <version>${resolved.parent.version}</version>
  </parent>` : ''}
  ${resolved.modules && resolved.modules.length > 0 ? `<modules>
    ${resolved.modules.map(m => `<module>${m}</module>`).join('\n    ')}
  </modules>` : ''}
  ${resolved.dependencies.length > 0 ? `<dependencies>
    ${resolved.dependencies.map(dep => `<dependency>
      <groupId>${dep.groupId}</groupId>
      <artifactId>${dep.artifactId}</artifactId>
      <version>${dep.version}</version>
      <scope>${dep.scope}</scope>
      <type>${dep.type}</type>
    </dependency>`).join('\n    ')}
  </dependencies>` : ''}
  ${resolved.plugins.length > 0 ? `<build>
    <plugins>
      ${resolved.plugins.map(plugin => `<plugin>
        <groupId>${plugin.groupId}</groupId>
        <artifactId>${plugin.artifactId}</artifactId>
        <version>${plugin.version}</version>
      </plugin>`).join('\n      ')}
    </plugins>
  </build>` : ''}
</project>`;
    
    return xmlContent;
  } catch (error: any) {
    throw new Error(`Failed to resolve effective POM for ${pomPath}: ${error.message}`);
  }
}

