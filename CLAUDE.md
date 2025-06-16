# Maven Plugin Testing Instructions

This guide provides comprehensive testing instructions for the Nx Maven plugin integration, focusing on `nx graph --graph.json` and `nx show projects` commands.

## Prerequisites

- Nx CLI installed (local: v21.1.3)
- Maven installed (3.9.9+)
- Java Development Kit
- Plugin compiled: `/maven-plugin/target/classes/`

## Core Project Goal

- **Main Objective**: Create a Maven plugin that integrates Maven into Nx
  - Plugin should execute Maven commands exactly as Maven does
  - Leverage Maven's official APIs to reflect Maven's workspace understanding into Nx

## Quick Start Testing

### 1. Basic Plugin Verification
```bash
# Verify plugin is loaded and working
nx show projects

# Generate project graph JSON
nx graph --file graph.json

# View project details
nx show projects --verbose
```

[... rest of the existing content remains unchanged ...]