#!/bin/bash

echo "Testing verbose logging with Maven plugin..."

# Create a minimal test directory
TEST_DIR="/tmp/maven-test-verbose"
mkdir -p "$TEST_DIR"

# Create a simple pom.xml
cat > "$TEST_DIR/pom.xml" << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>test</groupId>
    <artifactId>test-project</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
</project>
EOF

echo "Created test project at $TEST_DIR"

# Test normal mode
echo ""
echo "=== Testing Normal Mode ==="
cd "$TEST_DIR"
mvn io.quarkus:maven-plugin-v2:analyze -Dnx.outputFile=/tmp/test-normal.json 2>&1 | grep -E "(Starting Nx|Found [0-9]+ projects|SUCCESS|ERROR)"

echo ""
echo "=== Testing Verbose Mode ==="
# Test verbose mode  
mvn io.quarkus:maven-plugin-v2:analyze -Dnx.outputFile=/tmp/test-verbose.json -Dnx.verbose=true 2>&1 | grep -E "(Starting Nx|Found [0-9]+ projects|Verbose mode|Target generation|CreateNodes|Dependencies|SUCCESS|ERROR)"

echo ""
echo "Test complete!"