File buildLog = new File( basedir, 'build.log' )

// Test should FAIL - dependencies found in <dependencies> section (not <dependencyManagement>)
// This tests the fallback to <dependencies> when not in <dependencyManagement>
assert buildLog.text.contains( 'BUILD FAILURE' )
assert buildLog.text.contains( 'Dependency version alignment check failed' )
// The misaligned dependency should be reported
assert buildLog.text.contains( 'org.geolatte:geolatte-geom' )
assert buildLog.text.contains( 'Version mismatch' )
// The aligned dependency should NOT be reported
assert !buildLog.text.contains( 'org.jboss.logging:jboss-logging' )
// Both dependencies should have been found in the reference artifact
assert !buildLog.text.contains( 'not found in reference artifact' )
