File buildLog = new File( basedir, 'build.log' )

// Test DependencyAlignmentRule failure - should report ALL misalignments
assert buildLog.text.contains( 'Dependency version alignment check failed' )
assert buildLog.text.contains( 'Version mismatch' )
// Both misaligned dependencies should be reported
assert buildLog.text.contains( 'net.bytebuddy:byte-buddy' )
assert buildLog.text.contains( 'org.antlr:antlr4' )
// The aligned dependency should NOT be reported
assert !buildLog.text.contains( 'jakarta.persistence:jakarta.persistence-api' )
