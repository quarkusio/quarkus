File buildLog = new File( basedir, 'build.log' )

// Test BansRuntimeDependency failure
assert buildLog.text.contains( 'quarkus-enforcer-rules-smoketest-ext2-deployment ... SUCCESS' )
assert buildLog.text.contains( 'quarkus-enforcer-rules-smoketest-ext3-deployment ... FAILURE' )
assert buildLog.text.contains( 'BansRuntimeDependency failed with message' )

// Test RequiresMinimalDeploymentDependency failure: missing dependency
assert buildLog.text.contains( 'quarkus-enforcer-rules-smoketest-integration-tests-ext1 SUCCESS' )
assert buildLog.text.contains( 'quarkus-enforcer-rules-smoketest-integration-tests-ext2 FAILURE' )
assert buildLog.text.contains( 'RequiresMinimalDeploymentDependency failed with message' )
assert buildLog.text.contains( '1 minimal *-deployment dependencies are missing' )
assert buildLog.text.contains( '<artifactId>quarkus-enforcer-rules-smoketest-ext2-deployment</artifactId>' )

// Test RequiresMinimalDeploymentDependency failure: superfluous dependency
assert buildLog.text.contains( 'quarkus-enforcer-rules-smoketest-integration-tests-ext1 FAILURE' )
assert buildLog.text.contains( 'RequiresMinimalDeploymentDependency failed with message' )
assert buildLog.text.contains( '1 minimal *-deployment dependencies are superfluous' )
assert buildLog.text.contains( '    io.quarkus:quarkus-enforcer-rules-smoketest-ext2-deployment:1.0-SNAPSHOT' )
