File buildLog = new File( basedir, 'build.log' )

assert buildLog.text.contains( 'quarkus-enforcer-rules-smoketest-ext2-deployment ... SUCCESS' )
assert buildLog.text.contains( 'quarkus-enforcer-rules-smoketest-ext3-deployment ... FAILURE' )
assert buildLog.text.contains( 'BansRuntimeDependency failed with message' )

assert buildLog.text.contains( 'quarkus-enforcer-rules-smoketest-integration-tests-ext1 SUCCESS' )
assert buildLog.text.contains( 'quarkus-enforcer-rules-smoketest-integration-tests-ext2 FAILURE' )
assert buildLog.text.contains( 'RequiresMinimalDeploymentDependency failed with message' )
assert buildLog.text.contains( '<artifactId>quarkus-enforcer-rules-smoketest-ext2-deployment</artifactId>' )
