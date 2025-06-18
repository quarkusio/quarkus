# Cache Unification for TypeScript Plugin

## Problem
The TypeScript plugin was using separate cache entries for nodes and dependencies data, causing potential conflicts and inefficient caching.

## Solution
Unified the cache structure to store both nodes and dependencies in a single cache object per cache key.

## Changes Made

### 1. Cache Data Structure
- Added `CacheData` interface with optional `nodes` and `dependencies` properties
- Updated cache functions to use typed cache structure

### 2. createNodesV2 Function
- Changed cache check from `cache[cacheKey]` to `cache[cacheKey]?.nodes`
- Modified cache storage to use `cache[cacheKey].nodes = createNodesResults`
- Ensures cache object is initialized before setting nodes data

### 3. createDependencies Function  
- Changed cache check from `cache[cacheKey]` to `cache[cacheKey]?.dependencies`
- Modified cache storage to use `cache[cacheKey].dependencies = createDependencies`
- Ensures cache object is initialized before setting dependencies data

## Benefits
- Single cache file stores both nodes and dependencies data
- No cache conflicts between the two functions
- Better cache utilization and performance
- Cleaner cache management