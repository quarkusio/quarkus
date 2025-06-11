import { readFileSync, existsSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { parseString } from 'xml2js';
import { promisify } from 'node:util';

const parseXml = promisify(parseString);

interface ResolvedMavenProject {
  groupId: string;
  artifactId: string;
  version: string;
  packaging: string;
  properties: Record<string, string>;
  dependencies: Array<{
    groupId: string;
    artifactId: string;
    version: string;
    scope: string;
    type: string;
  }>;
  dependencyManagement: Array<{
    groupId: string;
    artifactId: string;
    version: string;
    scope?: string;
    type?: string;
  }>;
  modules?: string[];
  plugins: Array<{
    groupId: string;
    artifactId: string;
    version: string;
  }>;
  parent?: {
    groupId: string;
    artifactId: string;
    version: string;
    relativePath?: string;
  };
}

export class EffectivePomResolver {
  private pomCache = new Map<string, any>();
  private resolvedCache = new Map<string, ResolvedMavenProject>();
  
  async resolveEffectivePom(
    pomPath: string, 
    workspaceRoot: string
  ): Promise<ResolvedMavenProject> {
    const absolutePath = resolve(pomPath);
    
    if (this.resolvedCache.has(absolutePath)) {
      return this.resolvedCache.get(absolutePath)!;
    }
    
    console.log(`[Effective POM] Resolving ${pomPath}`);
    
    const rawPom = await this.parsePom(absolutePath);
    const resolved = await this.buildEffectivePom(rawPom, absolutePath, workspaceRoot);
    
    this.resolvedCache.set(absolutePath, resolved);
    return resolved;
  }
  
  private async parsePom(pomPath: string): Promise<any> {
    if (this.pomCache.has(pomPath)) {
      return this.pomCache.get(pomPath);
    }
    
    const content = readFileSync(pomPath, 'utf-8');
    const parsed = await parseXml(content) as any;
    this.pomCache.set(pomPath, parsed);
    return parsed;
  }
  
  private async buildEffectivePom(
    rawPom: any, 
    pomPath: string, 
    workspaceRoot: string
  ): Promise<ResolvedMavenProject> {
    const project = rawPom.project;
    
    // Start with properties from this POM
    const properties = this.extractProperties(project);
    
    // Resolve parent if present
    let parentResolved: ResolvedMavenProject | null = null;
    if (project.parent?.[0]) {
      const parentPath = await this.resolveParentPath(project.parent[0], pomPath, workspaceRoot);
      if (parentPath && existsSync(parentPath)) {
        parentResolved = await this.resolveEffectivePom(parentPath, workspaceRoot);
        // Inherit parent properties
        Object.assign(properties, parentResolved.properties, properties);
      }
    }
    
    // Resolve all placeholders in properties first
    this.resolvePropertyPlaceholders(properties);
    
    // Build effective POM
    const effective: ResolvedMavenProject = {
      groupId: this.resolveValue(project.groupId?.[0], properties) || 
               parentResolved?.groupId || '',
      artifactId: this.resolveValue(project.artifactId?.[0], properties) || '',
      version: this.resolveValue(project.version?.[0], properties) || 
               parentResolved?.version || '',
      packaging: this.resolveValue(project.packaging?.[0], properties) || 'jar',
      properties,
      dependencies: [],
      dependencyManagement: [],
      plugins: [],
      modules: project.modules?.[0]?.module || []
    };
    
    if (project.parent?.[0]) {
      effective.parent = {
        groupId: this.resolveValue(project.parent[0].groupId?.[0], properties) || '',
        artifactId: this.resolveValue(project.parent[0].artifactId?.[0], properties) || '',
        version: this.resolveValue(project.parent[0].version?.[0], properties) || '',
        relativePath: project.parent[0].relativePath?.[0]
      };
    }
    
    // Merge dependency management from parent
    if (parentResolved?.dependencyManagement) {
      effective.dependencyManagement.push(...parentResolved.dependencyManagement);
    }
    
    // Add dependency management from current POM
    if (project.dependencyManagement?.[0]?.dependencies?.[0]?.dependency) {
      for (const dep of project.dependencyManagement[0].dependencies[0].dependency) {
        effective.dependencyManagement.push({
          groupId: this.resolveValue(dep.groupId?.[0], properties) || '',
          artifactId: this.resolveValue(dep.artifactId?.[0], properties) || '',
          version: this.resolveValue(dep.version?.[0], properties) || '',
          scope: this.resolveValue(dep.scope?.[0], properties) || 'compile',
          type: this.resolveValue(dep.type?.[0], properties) || 'jar'
        });
      }
    }
    
    // Resolve dependencies with version management
    if (project.dependencies?.[0]?.dependency) {
      for (const dep of project.dependencies[0].dependency) {
        const resolvedDep = this.resolveDependency(dep, effective.dependencyManagement, properties);
        if (resolvedDep.groupId && resolvedDep.artifactId) {
          effective.dependencies.push(resolvedDep);
        }
      }
    }
    
    // Merge plugins from parent
    if (parentResolved?.plugins) {
      effective.plugins.push(...parentResolved.plugins);
    }
    
    // Add plugins from current POM
    if (project.build?.[0]?.plugins?.[0]?.plugin) {
      for (const plugin of project.build[0].plugins[0].plugin) {
        effective.plugins.push({
          groupId: this.resolveValue(plugin.groupId?.[0], properties) || '',
          artifactId: this.resolveValue(plugin.artifactId?.[0], properties) || '',
          version: this.resolveValue(plugin.version?.[0], properties) || ''
        });
      }
    }
    
    console.log(`[Effective POM] Resolved ${effective.groupId}.${effective.artifactId} with ${effective.dependencies.length} dependencies`);
    
    return effective;
  }
  
  private extractProperties(project: any): Record<string, string> {
    const properties: Record<string, string> = {
      // Built-in Maven properties
      'project.groupId': project.groupId?.[0] || project.parent?.[0]?.groupId?.[0] || '',
      'project.artifactId': project.artifactId?.[0] || '',
      'project.version': project.version?.[0] || project.parent?.[0]?.version?.[0] || '',
      'project.packaging': project.packaging?.[0] || 'jar',
      // Add standard Maven properties
      'maven.compiler.source': '17',
      'maven.compiler.target': '17',
      'project.build.sourceEncoding': 'UTF-8'
    };
    
    // Extract custom properties
    if (project.properties?.[0]) {
      for (const [key, value] of Object.entries(project.properties[0])) {
        if (Array.isArray(value) && value.length > 0) {
          properties[key] = String(value[0]);
        }
      }
    }
    
    return properties;
  }
  
  private resolvePropertyPlaceholders(properties: Record<string, string>): void {
    // Multiple passes to resolve nested placeholders
    let changed = true;
    let passes = 0;
    const maxPasses = 10;
    
    while (changed && passes < maxPasses) {
      changed = false;
      passes++;
      
      for (const [key, value] of Object.entries(properties)) {
        const resolved = this.resolveValue(value, properties);
        if (resolved !== value) {
          properties[key] = resolved;
          changed = true;
        }
      }
    }
    
    if (passes >= maxPasses) {
      console.warn('[Effective POM] Property resolution may have circular references');
    }
  }
  
  private resolveValue(value: string | undefined, properties: Record<string, string>): string {
    if (!value) return '';
    
    return value.replace(/\$\{([^}]+)\}/g, (match, propName) => {
      // Handle environment variables
      if (propName.startsWith('env.')) {
        const envVar = propName.substring(4);
        return process.env[envVar] || match;
      }
      
      // Handle system properties
      if (propName.startsWith('user.')) {
        return process.env[propName.replace('user.', '')] || match;
      }
      
      // Handle regular properties
      return properties[propName] || match;
    });
  }
  
  private resolveDependency(
    dep: any, 
    dependencyManagement: Array<any>, 
    properties: Record<string, string>
  ): {
    groupId: string;
    artifactId: string;
    version: string;
    scope: string;
    type: string;
  } {
    const groupId = this.resolveValue(dep.groupId?.[0], properties) || '';
    const artifactId = this.resolveValue(dep.artifactId?.[0], properties) || '';
    let version = this.resolveValue(dep.version?.[0], properties) || '';
    const scope = this.resolveValue(dep.scope?.[0], properties) || 'compile';
    const type = this.resolveValue(dep.type?.[0], properties) || 'jar';
    
    // If no version specified, look in dependency management
    if (!version) {
      const managed = dependencyManagement.find(
        m => m.groupId === groupId && m.artifactId === artifactId
      );
      if (managed) {
        version = managed.version;
      }
    }
    
    return { groupId, artifactId, version, scope, type };
  }
  
  private async resolveParentPath(
    parent: any, 
    currentPomPath: string, 
    workspaceRoot: string
  ): Promise<string | null> {
    const relativePath = parent.relativePath?.[0] || '../pom.xml';
    const currentDir = dirname(currentPomPath);
    
    // Try relative path first
    let parentPath = resolve(currentDir, relativePath);
    if (existsSync(parentPath)) {
      return parentPath;
    }
    
    // If relative path points to a directory, append pom.xml
    if (existsSync(parentPath) && !parentPath.endsWith('.xml')) {
      parentPath = join(parentPath, 'pom.xml');
      if (existsSync(parentPath)) {
        return parentPath;
      }
    }
    
    // Fallback: search in workspace for matching parent
    const parentGroupId = parent.groupId?.[0];
    const parentArtifactId = parent.artifactId?.[0];
    
    if (parentGroupId && parentArtifactId) {
      // This would require a more sophisticated search
      // For now, return null and let caller handle it
    }
    
    return null;
  }
  
  clearCache(): void {
    this.pomCache.clear();
    this.resolvedCache.clear();
  }
}