// Show how Maven targets appear in Nx format
const fs = require('fs');

// Get actual project data from the Maven plugin output
const data = JSON.parse(fs.readFileSync('./maven-results.json', 'utf8'));

// Extract the first project from the createNodesResults
let project = null;
let projectName = null;
let projectRoot = null;

if (data.createNodesResults && data.createNodesResults.length > 0) {
  const [pomPath, createResult] = data.createNodesResults[0];
  const projects = createResult.projects || {};
  const projectKey = Object.keys(projects)[0];
  
  if (projectKey) {
    project = projects[projectKey];
    projectName = project.name;
    projectRoot = project.root || '.';
  }
}

if (!project) {
  console.log('No projects found in maven-results.json');
  process.exit(1);
}

console.log('='.repeat(70));
console.log(`NX TARGET CONFIGURATION FOR: ${projectName}`);
console.log('='.repeat(70));

const targets = project.targets || {};
const targetNames = Object.keys(targets);

// Categorize targets by type
const phaseTargets = [];
const goalTargets = [];

targetNames.forEach(name => {
  const target = targets[name];
  const metadata = target.metadata || {};
  
  if (metadata.type === 'phase') {
    phaseTargets.push({ name, target });
  } else if (metadata.type === 'goal') {
    goalTargets.push({ name, target });
  }
});

// Display phase targets
console.log('\n📋 PHASE TARGETS (Maven Lifecycle):');
if (phaseTargets.length > 0) {
  phaseTargets.forEach(({ name, target }) => {
    console.log(`\n  ${name}:`);
    console.log(`    executor: ${target.executor}`);
    console.log(`    command: ${target.options.command}`);
    if (target.inputs && target.inputs.length > 0) {
      console.log(`    inputs: [${target.inputs.join(', ')}]`);
    }
    if (target.outputs && target.outputs.length > 0) {
      console.log(`    outputs: [${target.outputs.join(', ')}]`);
    }
    if (target.dependsOn && target.dependsOn.length > 0) {
      console.log(`    dependsOn: [${target.dependsOn.join(', ')}]`);
    }
    if (target.metadata.description) {
      console.log(`    description: ${target.metadata.description}`);
    }
  });
} else {
  console.log('  No phase targets found');
}

// Display plugin goal targets
console.log('\n🔧 PLUGIN GOAL TARGETS (Framework-specific):');
if (goalTargets.length > 0) {
  goalTargets.forEach(({ name, target }) => {
    console.log(`\n  ${name}:`);
    console.log(`    executor: ${target.executor}`);
    console.log(`    command: ${target.options.command}`);
    if (target.metadata.plugin) {
      console.log(`    plugin: ${target.metadata.plugin}`);
    }
    if (target.metadata.goal) {
      console.log(`    goal: ${target.metadata.goal}`);
    }
    if (target.dependsOn && target.dependsOn.length > 0) {
      console.log(`    dependsOn: [${target.dependsOn.join(', ')}]`);
    }
    if (target.metadata.description) {
      console.log(`    description: ${target.metadata.description}`);
    }
  });
} else {
  console.log('  No plugin goal targets found');
}

// Show dependency visualization
console.log('\n🔗 DEPENDENCY VISUALIZATION:');
if (targetNames.length > 0) {
  console.log('Target dependency chains:');
  targetNames.forEach(name => {
    const target = targets[name];
    if (target.dependsOn && target.dependsOn.length > 0) {
      console.log(`  ${name} → depends on: [${target.dependsOn.join(', ')}]`);
    }
  });
} else {
  console.log('  No target dependencies found');
}

console.log('\n💡 BENEFITS:');
console.log('• Nx only runs targets that have changed or their dependencies changed');
console.log('• Results are cached - if compile hasn\'t changed, it won\'t rerun');
console.log('• Parallel execution - independent targets can run simultaneously');
console.log('• Clear dependency graph - you see exactly what runs when');
console.log('• Framework integration - Quarkus dev mode, Spring Boot run, etc.');