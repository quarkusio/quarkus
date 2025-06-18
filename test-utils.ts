import { execSync } from 'child_process';
import { existsSync } from 'fs';

export class TestUtils {
  /**
   * Compile Java components and reset Nx state
   */
  static async setupE2E(): Promise<void> {
    console.log('🔧 Setting up e2e test environment...');
    
    // Compile Java components
    execSync('cd maven-plugin && mvn install -DskipTests -q', { stdio: 'inherit' });
    
    // Reset Nx state
    execSync('npx nx reset', { stdio: 'inherit' });
  }

  /**
   * Run nx command and return output
   */
  static runNxCommand(command: string, options: { encoding?: BufferEncoding; timeout?: number } = {}): string {
    const { encoding = 'utf8', timeout = 60000 } = options;
    return execSync(`npx nx ${command}`, { encoding, timeout });
  }

  /**
   * Check if Maven is available
   */
  static isMavenAvailable(): boolean {
    try {
      execSync('mvn --version', { stdio: 'pipe' });
      return true;
    } catch {
      return false;
    }
  }

  /**
   * Get project count from nx show projects
   */
  static getProjectCount(): number {
    const output = this.runNxCommand('show projects');
    return output.split('\n').filter(line => line.trim()).length;
  }

  /**
   * Generate project graph and return parsed JSON
   */
  static generateProjectGraph(filePath: string): any {
    this.runNxCommand(`graph --file ${filePath}`);
    
    if (!existsSync(filePath)) {
      throw new Error(`Graph file not generated at ${filePath}`);
    }
    
    const fs = require('fs');
    return JSON.parse(fs.readFileSync(filePath, 'utf8'));
  }

  /**
   * Clean up temporary test files
   */
  static cleanup(patterns: string[]): void {
    patterns.forEach(pattern => {
      try {
        const fs = require('fs');
        fs.rmSync(pattern, { recursive: true, force: true });
      } catch {
        // Ignore cleanup errors
      }
    });
  }
}