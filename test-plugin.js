// Quick test of Maven plugin target generation
const fs = require('fs');

// Test that the maven-results.json now has target data
const data = JSON.parse(fs.readFileSync('./maven-results.json', 'utf8'));

// Find a project with plugin goals (like Quarkus projects)
let projectWithGoals = null;
for (const [path, project] of Object.entries(data)) {
  if (project.pluginGoals && project.pluginGoals.length > 0) {
    projectWithGoals = { path, project };
    break;
  }
}

if (projectWithGoals) {
  const { path, project } = projectWithGoals;
  console.log('✓ Found project with plugin goals:', path);
  console.log('  - Phases:', project.relevantPhases?.length || 0);
  console.log('  - Plugin goals:', project.pluginGoals?.length || 0);
  console.log('  - Phase dependencies:', Object.keys(project.phaseDependencies || {}).length);
  
  // Test a few plugin goals
  if (project.pluginGoals.length > 0) {
    const goal = project.pluginGoals[0];
    console.log('  - First goal:', goal.pluginKey + ':' + goal.goal);
    console.log('  - Target name:', goal.targetName);
    console.log('  - Target type:', goal.targetType);
    console.log('  - Dependencies:', goal.suggestedDependencies || []);
  }
  
  console.log('✓ Target data is now available for Maven plugin processing');
} else {
  console.log('✗ No projects found with plugin goals');
}

// Count total projects with various types of target data
let phaseCounts = 0, goalCounts = 0, depCounts = 0;
for (const project of Object.values(data)) {
  if (project.relevantPhases?.length > 0) phaseCounts++;
  if (project.pluginGoals?.length > 0) goalCounts++;
  if (Object.keys(project.phaseDependencies || {}).length > 0) depCounts++;
}

console.log(`\nSummary:`);
console.log(`- Total projects: ${Object.keys(data).length}`);
console.log(`- Projects with phases: ${phaseCounts}`);
console.log(`- Projects with plugin goals: ${goalCounts}`);
console.log(`- Projects with phase dependencies: ${depCounts}`);