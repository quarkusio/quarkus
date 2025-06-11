// Test the new goal organization structure
const fs = require('fs');

const data = JSON.parse(fs.readFileSync('./maven-results-fixed.json', 'utf8'));

// Find a project with goals organized by phase
let exampleProject = null;
for (const [path, project] of Object.entries(data)) {
  if (project.goalsByPhase) {
    const totalGoals = Object.values(project.goalsByPhase).flat().length;
    if (totalGoals > 0) {
      exampleProject = { path, project };
      break;
    }
  }
}

if (exampleProject) {
  const { path, project } = exampleProject;
  console.log('='.repeat(70));
  console.log(`EXAMPLE PROJECT WITH NEW STRUCTURE: ${path}`);
  console.log(`Project: ${project.name}`);
  console.log('='.repeat(70));
  
  console.log('\n📋 PHASES:');
  project.relevantPhases.forEach((phase, i) => {
    const goals = project.goalsByPhase[phase] || [];
    console.log(`   ${i + 1}. ${phase} → [${goals.join(', ')}]`);
  });
  
  console.log('\n🔧 PLUGIN GOALS:');
  project.pluginGoals.forEach((goal, i) => {
    console.log(`   ${i + 1}. ${goal.targetName} (${goal.targetType})`);
    console.log(`      Plugin: ${goal.pluginKey}:${goal.goal}`);
    console.log(`      Suggested deps: [${goal.suggestedDependencies?.join(', ') || 'none'}]`);
  });
  
  console.log('\n🔗 GOAL DEPENDENCIES:');
  if (Object.keys(project.goalDependencies).length > 0) {
    for (const [goal, deps] of Object.entries(project.goalDependencies)) {
      console.log(`   ${goal} → depends on: [${deps.join(', ')}]`);
    }
  } else {
    console.log('   (No goal dependencies generated yet)');
  }
  
  console.log('\n💡 NEW TASK GRAPH STRUCTURE:');
  console.log('   Phase targets depend on their own goals:');
  for (const [phase, goals] of Object.entries(project.goalsByPhase)) {
    if (goals.length > 0) {
      console.log(`   • ${phase} → depends on: [${goals.join(', ')}]`);
    }
  }
  
  console.log('\n   Goal targets depend on goals from prerequisite phases:');
  console.log('   (This will be implemented based on goalDependencies)');
  
} else {
  console.log('No project found with organized goals');
}

// Count projects with the new structure
let structureCount = 0;
let goalCount = 0;
for (const project of Object.values(data)) {
  if (project.goalsByPhase) {
    structureCount++;
    goalCount += Object.values(project.goalsByPhase).flat().length;
  }
}

console.log(`\n📊 SUMMARY:`);
console.log(`- Projects with new structure: ${structureCount}`);
console.log(`- Total goals organized: ${goalCount}`);
console.log(`- New fields added: goalsByPhase, goalDependencies`);