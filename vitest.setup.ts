import { execSync } from 'child_process';

export async function setup() {
  console.log('🔥 Setting up Maven Plugin E2E Tests (global setup)...');
  
  // Step 1: Recompile Java components
  console.log('📦 Recompiling Java components...');
  execSync('cd maven-plugin && mvn install -DskipTests -q', { stdio: 'inherit' });
  
  // Step 2: Reset Nx state
  console.log('🔄 Resetting Nx state...');
  execSync('npx nx reset', { stdio: 'inherit' });
  
  console.log('✅ Maven plugin global setup complete');
}