#!/usr/bin/env node

// Simple test script that limits Maven processing to just a few files
const { execSync } = require('child_process');

try {
  console.log('Testing with limited files (first 5 pom.xml files)...');
  
  // Find just the first 5 pom.xml files
  const pomFiles = execSync('find . -name "pom.xml" | grep -v maven-script | head -5', {
    cwd: process.cwd(),
    encoding: 'utf8'
  }).trim().split('\n').filter(Boolean);
  
  console.log(`Found ${pomFiles.length} files to test:`, pomFiles);
  
  // Set environment variable to limit processing
  process.env.NX_MAVEN_LIMIT = '5';
  
  // Run nx graph with timeout
  const timeout = 60; // 60 seconds
  console.log(`Running nx graph with ${timeout}s timeout...`);
  
  const output = execSync(`timeout ${timeout}s nx graph --file test-graph.json`, {
    cwd: process.cwd(),
    encoding: 'utf8',
    env: {
      ...process.env,
      NX_DAEMON: 'false',
      NX_CACHE_PROJECT_GRAPH: 'false'
    }
  });
  
  console.log('Success! Graph generated.');
  
  // Check if graph file was created
  const fs = require('fs');
  if (fs.existsSync('./test-graph.json')) {
    const graph = JSON.parse(fs.readFileSync('./test-graph.json', 'utf8'));
    const nodeCount = Object.keys(graph.graph?.nodes || {}).length;
    const depCount = Object.keys(graph.graph?.dependencies || {}).length;
    console.log(`Graph contains ${nodeCount} nodes and ${depCount} dependencies`);
  }
  
} catch (error) {
  console.error('Test failed:', error.message);
  
  // Check for stuck processes
  try {
    const processes = execSync('ps aux | grep -i maven | grep -v grep', { encoding: 'utf8' });
    if (processes.trim()) {
      console.warn('Found running Maven processes:', processes);
    }
  } catch (e) {
    console.log('No Maven processes found running');
  }
}