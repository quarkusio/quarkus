// Show how Maven targets appear in Nx format
const fs = require('fs');

// Mock the target generation process that the TypeScript plugin uses
function createPhaseTarget(phase, phaseDependencies, relevantPhases) {
  const config = {
    executor: '@nx/run-commands:run-commands',
    options: {
      command: `mvn ${phase}`,
      cwd: '{projectRoot}',
    },
    metadata: {
      type: 'phase',
      phase: phase,
      technologies: ['maven'],
      description: `Maven lifecycle phase: ${phase}`,
    },
    inputs: ['{projectRoot}/pom.xml'],
    outputs: [],
  };

  // Add phase-specific configurations
  if (phase === 'compile') {
    config.inputs.push('{projectRoot}/src/main/**/*');
    config.outputs = ['{projectRoot}/target/classes/**/*'];
  } else if (phase === 'test') {
    config.inputs.push('{projectRoot}/src/test/**/*', '{projectRoot}/src/main/**/*');
    config.outputs = ['{projectRoot}/target/surefire-reports/**/*'];
  } else if (phase === 'package') {
    config.inputs.push('{projectRoot}/src/**/*');
    config.outputs = ['{projectRoot}/target/*.jar', '{projectRoot}/target/*.war'];
  }

  // Add dependencies
  const phaseDeps = phaseDependencies[phase] || [];
  const filteredDeps = phaseDeps.filter(dep => relevantPhases.includes(dep));
  if (filteredDeps.length > 0) {
    config.dependsOn = filteredDeps;
  }

  return config;
}

function createPluginGoalTarget(goalInfo) {
  const { pluginKey, goal, targetType, suggestedDependencies } = goalInfo;
  const command = `mvn ${pluginKey}:${goal}`;

  const config = {
    executor: '@nx/run-commands:run-commands',
    options: {
      command,
      cwd: '{projectRoot}',
    },
    metadata: {
      type: 'goal',
      plugin: pluginKey,
      goal: goal,
      targetType: targetType,
      technologies: ['maven'],
      description: `${pluginKey.split(':')[1]}:${goal}`,
    },
    inputs: ['{projectRoot}/pom.xml'],
    outputs: [],
  };

  // Customize based on target type
  if (targetType === 'serve') {
    config.inputs.push('{projectRoot}/src/**/*');
  } else if (targetType === 'build') {
    config.inputs.push('{projectRoot}/src/**/*');
    config.outputs = ['{projectRoot}/target/**/*'];
  } else if (targetType === 'test') {
    config.inputs.push('{projectRoot}/src/test/**/*', '{projectRoot}/src/main/**/*');
    config.outputs = ['{projectRoot}/target/surefire-reports/**/*'];
  }

  if (suggestedDependencies && suggestedDependencies.length > 0) {
    config.dependsOn = suggestedDependencies;
  }

  return config;
}

// Get example project data
const data = JSON.parse(fs.readFileSync('./maven-results.json', 'utf8'));
const projectPath = 'independent-projects/arc/processor';
const project = data[projectPath];

console.log('='.repeat(70));
console.log(`NX TARGET CONFIGURATION FOR: ${project.name}`);
console.log('='.repeat(70));

// Generate example targets
const targets = {};

// Add phase targets
console.log('\n📋 PHASE TARGETS (Maven Lifecycle):');
['compile', 'test', 'package'].forEach(phase => {
  if (project.relevantPhases.includes(phase)) {
    const target = createPhaseTarget(phase, project.phaseDependencies, project.relevantPhases);
    targets[phase] = target;
    
    console.log(`\n  ${phase}:`);
    console.log(`    executor: ${target.executor}`);
    console.log(`    command: ${target.options.command}`);
    console.log(`    inputs: [${target.inputs.join(', ')}]`);
    if (target.outputs.length > 0) {
      console.log(`    outputs: [${target.outputs.join(', ')}]`);
    }
    if (target.dependsOn) {
      console.log(`    dependsOn: [${target.dependsOn.join(', ')}]`);
    }
  }
});

// Add plugin goal targets
console.log('\n🔧 PLUGIN GOAL TARGETS (Framework-specific):');
project.pluginGoals.forEach(goalInfo => {
  const target = createPluginGoalTarget(goalInfo);
  targets[goalInfo.targetName] = target;
  
  console.log(`\n  ${goalInfo.targetName}:`);
  console.log(`    executor: ${target.executor}`);
  console.log(`    command: ${target.options.command}`);
  console.log(`    type: ${goalInfo.targetType}`);
  if (target.dependsOn) {
    console.log(`    dependsOn: [${target.dependsOn.join(', ')}]`);
  }
});

console.log('\n🔗 DEPENDENCY VISUALIZATION:');
console.log('When you run "nx serve", the dependency chain looks like:');

// Find serve target dependencies
const serveTarget = project.pluginGoals.find(g => g.targetType === 'serve');
if (serveTarget && serveTarget.suggestedDependencies) {
  let chain = [];
  let current = serveTarget.suggestedDependencies[0]; // 'compile'
  
  while (current && project.phaseDependencies[current]) {
    const deps = project.phaseDependencies[current];
    if (deps.length > 0) {
      const relevantDep = deps.find(dep => project.relevantPhases.includes(dep));
      if (relevantDep) {
        chain.unshift(relevantDep);
        current = relevantDep;
      } else {
        break;
      }
    } else {
      break;
    }
  }
  
  chain.push('compile');
  chain.push('serve');
  
  console.log(`  ${chain.join(' → ')}`);
  console.log(`  ${'↑'.padStart(chain[0].length + 1)} ${'↑'.padStart(chain[1] ? chain[1].length + 2 : 0)} ${'↑'.padStart('serve'.length + 2)}`);
  console.log(`  First    Compile   Start dev`);
  console.log(`  step     sources   server`);
}

console.log('\n💡 BENEFITS:');
console.log('• Nx only runs targets that have changed or their dependencies changed');
console.log('• Results are cached - if compile hasn\'t changed, it won\'t rerun');
console.log('• Parallel execution - independent targets can run simultaneously');
console.log('• Clear dependency graph - you see exactly what runs when');
console.log('• Framework integration - Quarkus dev mode, Spring Boot run, etc.');