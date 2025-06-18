import { execSync } from 'child_process';

export async function setup() {
  const globalSetupStart = Date.now();
  console.log('🔥 Setting up Maven Plugin E2E Tests (global setup)...');

  // Step 1: Recompile Java components
  console.log('📦 Recompiling Java components...');
  const javaCompileStart = Date.now();
  execSync('cd maven-plugin && mvn clean compile kotlin:compile org.apache.maven.plugins:maven-plugin-plugin:descriptor install -Dmaven.test.skip=true', { stdio: 'inherit' });
  const javaCompileDuration = Date.now() - javaCompileStart;
  console.log(`⏱️  Java compilation completed in ${javaCompileDuration}ms`);

  // Step 2: Reset Nx state
  console.log('🔄 Resetting Nx state...');
  const nxResetStart = Date.now();
  execSync('npx nx reset', { stdio: 'inherit' });
  const nxResetDuration = Date.now() - nxResetStart;
  console.log(`⏱️  Nx reset completed in ${nxResetDuration}ms`);

  const globalSetupDuration = Date.now() - globalSetupStart;
  console.log(`✅ Maven plugin global setup complete in ${globalSetupDuration}ms`);
}
