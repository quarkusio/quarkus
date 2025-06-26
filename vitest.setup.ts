import { execSync } from 'child_process';

export async function setup() {
  const globalSetupStart = Date.now();
  console.log('🔥 Setting up Maven Plugin E2E Tests (global setup)...');

  // Step 1: Clean previous Maven build
  console.log('🧹 Cleaning previous Maven build...');
  const cleanStart = Date.now();
  execSync('rm -rf maven-plugin/target', { stdio: 'inherit' });
  execSync('cd maven-plugin && mvn clean', { stdio: 'inherit' });
  const cleanDuration = Date.now() - cleanStart;
  console.log(`⏱️  Maven clean completed in ${cleanDuration}ms`);

  // Step 2: Recompile Java components (with cache disabled to ensure fresh compilation)
  console.log('📦 Recompiling Java components with fresh build...');
  const javaCompileStart = Date.now();
  // Disable Develocity build cache to ensure source changes are compiled
  const env = {
    ...process.env,
    GRADLE_ENTERPRISE_BUILD_CACHE_ENABLED: 'false'
  };
  execSync('npm run compile-java:fresh', {
    stdio: 'inherit',
    env
  });
  const javaCompileDuration = Date.now() - javaCompileStart;
  console.log(`⏱️  Java compilation completed in ${javaCompileDuration}ms`);

  // Step 3: Reset Nx state
  console.log('🔄 Resetting Nx state...');
  const nxResetStart = Date.now();
  execSync('npx nx reset', { stdio: 'inherit' });
  const nxResetDuration = Date.now() - nxResetStart;
  console.log(`⏱️  Nx reset completed in ${nxResetDuration}ms`);

  const globalSetupDuration = Date.now() - globalSetupStart;
  console.log(`✅ Maven plugin global setup complete in ${globalSetupDuration}ms`);
}
