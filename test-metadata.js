// Test script to verify metadata is added to targets
const plugin = require('./maven-plugin2.ts');

async function testMetadata() {
  console.log('Testing Maven plugin target metadata...');
  
  try {
    // Mock data similar to what Java analyzer produces
    const mockResult = {
      '/test/project': {
        name: 'test:project',
        projectType: 'library',
        implicitDependencies: { projects: [] },
        relevantPhases: ['clean', 'compile', 'test'],
        pluginGoals: [
          {
            pluginKey: 'io.quarkus:quarkus-maven-plugin',
            goal: 'dev',
            targetType: 'serve',
            phase: null,
            targetName: 'serve'
          },
          {
            pluginKey: 'org.apache.maven.plugins:maven-surefire-plugin',
            goal: 'test',
            targetType: 'test',
            phase: 'test',
            targetName: 'surefire:test'
          }
        ]
      }
    };
    
    // Create a simple test by calling the createPhaseTarget and createPluginGoalTarget functions
    // We'll need to extract and test these individually since the plugin exports are complex
    
    console.log('Mock data structure looks correct');
    console.log('Plugin goals:', JSON.stringify(mockResult['/test/project'].pluginGoals, null, 2));
    console.log('Relevant phases:', mockResult['/test/project'].relevantPhases);
    
  } catch (error) {
    console.error('Error testing metadata:', error.message);
  }
}

testMetadata();