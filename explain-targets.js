// Explain Maven target dependencies with examples
const fs = require('fs');

const data = JSON.parse(fs.readFileSync('./maven-results.json', 'utf8'));

// Find a good example project with multiple types of targets
let exampleProject = null;
for (const [path, project] of Object.entries(data)) {
  if (project.pluginGoals?.length > 2 && project.relevantPhases?.length > 8) {
    exampleProject = { path, project };
    break;
  }
}

if (!exampleProject) {
  console.log('No suitable example project found');
  process.exit(1);
}

const { path, project } = exampleProject;
console.log('='.repeat(60));
console.log(`EXAMPLE PROJECT: ${path}`);
console.log(`Project: ${project.name}`);
console.log('='.repeat(60));

console.log('\n1. MAVEN LIFECYCLE PHASES:');
console.log('These are the standard Maven build phases that run in order:');
project.relevantPhases.forEach((phase, i) => {
  console.log(`   ${i + 1}. ${phase}`);
});

console.log('\n2. PHASE DEPENDENCIES:');
console.log('Each phase depends on previous phases completing first:');
for (const [phase, deps] of Object.entries(project.phaseDependencies || {})) {
  if (deps.length > 0) {
    console.log(`   ${phase} → depends on: [${deps.join(', ')}]`);
  } else {
    console.log(`   ${phase} → no dependencies (runs first)`);
  }
}

console.log('\n3. PLUGIN GOALS:');
console.log('These are specific tasks that plugins can execute:');
project.pluginGoals.forEach((goal, i) => {
  console.log(`   ${i + 1}. Target: "${goal.targetName}"`);
  console.log(`      Plugin: ${goal.pluginKey}`);
  console.log(`      Goal: ${goal.goal}`);
  console.log(`      Type: ${goal.targetType}`);
  console.log(`      Dependencies: [${goal.suggestedDependencies?.join(', ') || 'none'}]`);
  console.log('');
});

console.log('\n4. TARGET TYPES EXPLAINED:');
console.log('   • build    - Compiles/packages the code (mvn compile, mvn package)');
console.log('   • test     - Runs tests (mvn test, mvn integration-test)');
console.log('   • serve    - Starts development server (quarkus:dev, spring-boot:run)');
console.log('   • deploy   - Deploys artifacts (mvn deploy)');
console.log('   • utility  - Helper tasks (clean, validate)');

if (Object.keys(project.crossProjectDependencies || {}).length > 0) {
  console.log('\n5. CROSS-PROJECT DEPENDENCIES:');
  console.log('Some targets depend on targets in other projects:');
  for (const [target, deps] of Object.entries(project.crossProjectDependencies)) {
    console.log(`   ${target} → depends on: [${deps.join(', ')}]`);
  }
}

console.log('\n6. HOW NX USES THESE:');
console.log('When you run "nx build my-project", Nx will:');
console.log('   1. Look at the build target dependencies');
console.log('   2. Run any prerequisite targets first (like compile)');
console.log('   3. Run dependent project targets if needed');
console.log('   4. Then run the actual build target');
console.log('   5. Cache results for faster subsequent runs');

console.log('\n7. EXAMPLE DEPENDENCY CHAIN:');
console.log('For a typical Java project, running "nx test" might execute:');
console.log('   validate → compile → test-compile → test');
console.log('   ↑         ↑        ↑              ↑');
console.log('   Check     Compile  Compile test   Run');
console.log('   project   sources  sources        tests');