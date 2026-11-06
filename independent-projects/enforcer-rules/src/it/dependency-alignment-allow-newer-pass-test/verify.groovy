File buildLog = new File( basedir, 'build.log' )

// Test should pass - allowNewer allows versions equal to or newer than reference
assert buildLog.text.contains( 'BUILD SUCCESS' )
assert !buildLog.text.contains( 'Dependency version alignment check failed' )
assert !buildLog.text.contains( 'Version mismatch' )
