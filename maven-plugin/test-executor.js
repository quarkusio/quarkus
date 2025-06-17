const { default: executor } = require('./dist/executors/maven-batch/executor');

async function testExecutor() {
  console.log('Testing Maven Batch Executor...');
  
  const context = {
    root: '/home/jason/projects/triage/java/quarkus',
    projectName: 'maven-plugin',
    targetName: 'test-batch'
  };

  // Test 1: Single goal execution
  console.log('\n=== Test 1: Single Compile Goal ===');
  const result1 = await executor({
    goals: ['org.apache.maven.plugins:maven-compiler-plugin:compile'],
    projectRoot: '.',
    verbose: true,
    mavenPluginPath: 'maven-plugin'
  }, context);
  
  console.log('Result 1:', result1.success ? 'SUCCESS' : 'FAILED');
  if (result1.error) console.log('Error:', result1.error);

  // Test 2: Batch execution (jar + install)
  console.log('\n=== Test 2: JAR + Install Batch ===');
  const result2 = await executor({
    goals: [
      'org.apache.maven.plugins:maven-jar-plugin:jar',
      'org.apache.maven.plugins:maven-install-plugin:install'
    ],
    projectRoot: '.',
    verbose: true,
    mavenPluginPath: 'maven-plugin',
    outputFile: 'test-batch-results.json'
  }, context);
  
  console.log('Result 2:', result2.success ? 'SUCCESS' : 'FAILED');
  if (result2.error) console.log('Error:', result2.error);
  if (result2.output) {
    console.log('Overall Success:', result2.output.overallSuccess);
    console.log('Total Duration:', result2.output.totalDurationMs + 'ms');
    console.log('Goals Executed:', result2.output.goalResults.length);
  }

  console.log('\n=== Test Complete ===');
}

testExecutor().catch(console.error);