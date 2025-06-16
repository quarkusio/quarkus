// Explain Maven target dependencies with examples
const fs = require('fs');

const data = JSON.parse(fs.readFileSync('./maven-results.json', 'utf8'));

// Extract the first project from the createNodesResults
let project = null;
let projectName = null;
let projectRoot = null;
let path = null;

if (data.createNodesResults && data.createNodesResults.length > 0) {
  const [pomPath, createResult] = data.createNodesResults[0];
  const projects = createResult.projects || {};
  const projectKey = Object.keys(projects)[0];
  
  if (projectKey) {
    project = projects[projectKey];
    projectName = project.name;
    projectRoot = project.root || '.';
    path = pomPath;
  }
}

if (!project) {
  console.log('No projects found in maven-results.json');
  process.exit(1);
}
console.log('='.repeat(60));
console.log(`EXAMPLE PROJECT: ${path}`);
console.log(`Project: ${projectName}`);
console.log('='.repeat(60));

const targets = project.targets || {};
const targetNames = Object.keys(targets);

// Categorize targets
const phaseTargets = [];
const goalTargets = [];
const allPhases = new Set();
const allPlugins = new Set();

targetNames.forEach(name => {
  const target = targets[name];
  const metadata = target.metadata || {};
  
  if (metadata.type === 'phase') {
    phaseTargets.push({ name, target });
    if (metadata.phase) allPhases.add(metadata.phase);
  } else if (metadata.type === 'goal') {
    goalTargets.push({ name, target });
    if (metadata.plugin) allPlugins.add(metadata.plugin);
  }
});

console.log('\n1. MAVEN LIFECYCLE PHASES:');
console.log('These are the Maven build phases discovered in this project:');
if (allPhases.size > 0) {
  Array.from(allPhases).forEach((phase, i) => {
    console.log(`   ${i + 1}. ${phase}`);
  });
} else {
  console.log('   No phase targets found');
}

console.log('\n2. PHASE DEPENDENCIES:');
console.log('Each phase target depends on other targets completing first:');
phaseTargets.forEach(({ name, target }) => {
  if (target.dependsOn && target.dependsOn.length > 0) {
    console.log(`   ${name} → depends on: [${target.dependsOn.join(', ')}]`);
  } else {
    console.log(`   ${name} → no dependencies (runs independently)`);
  }
});

console.log('\n3. PLUGIN GOALS:');
console.log('These are specific tasks that plugins can execute:');
goalTargets.forEach((goalInfo, i) => {
  const { name, target } = goalInfo;
  const metadata = target.metadata || {};
  console.log(`   ${i + 1}. Target: "${name}"`);
  console.log(`      Plugin: ${metadata.plugin || 'unknown'}`);
  console.log(`      Goal: ${metadata.goal || 'unknown'}`);
  console.log(`      Phase: ${metadata.phase || 'none'}`);
  console.log(`      Dependencies: [${target.dependsOn?.join(', ') || 'none'}]`);
  if (metadata.description) {
    console.log(`      Description: ${metadata.description}`);
  }
  console.log('');
});

console.log('\n4. TARGET TYPES EXPLAINED:');
console.log('   • goal     - Specific plugin execution (maven-compiler:compile)');
console.log('   • phase    - Maven lifecycle phase (compile, test, package)');
console.log('   • build    - Compiles/packages the code');
console.log('   • test     - Runs tests');
console.log('   • utility  - Helper tasks (clean, validate)');

console.log('\n5. DISCOVERED PLUGINS:');
console.log('Maven plugins found in this project:');
if (allPlugins.size > 0) {
  Array.from(allPlugins).forEach((plugin, i) => {
    console.log(`   ${i + 1}. ${plugin}`);
  });
} else {
  console.log('   No plugin goals found');
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