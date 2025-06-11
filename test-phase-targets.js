// Quick test to see if phase targets are working
const { createNodesV2 } = require('./maven-plugin2.ts');

async function testPhaseTargets() {
  console.log('Testing Maven phase target detection...');
  
  try {
    const results = await createNodesV2[1](['maven-script/pom.xml'], {}, {});
    console.log('Results:', JSON.stringify(results, null, 2));
  } catch (error) {
    console.error('Error:', error);
  }
}

testPhaseTargets();