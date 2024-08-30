🔒: Configuration property fixed at build time - All other configuration properties are overridable at runtime

{#for extensionConfigRootsEntry in configRootsByExtensions}

# {extensionConfigRootsEntry.key.formatName}

| Configuration property | Type | Default |
|------------------------|------|---------|
{#for configRoot in extensionConfigRootsEntry.value.values}
{#for item in configRoot.items}
{#if !item.deprecated}
{#if !item.isSection}
{#configProperty configProperty=item extension=extensionConfigRootsEntry.key additionalAnchorPrefix=additionalAnchorPrefix /}
{#else}
{#configSection configSection=item extension=extensionConfigRootsEntry.key additionalAnchorPrefix=additionalAnchorPrefix /}
{/if}
{/if}
{/for}
{/for}
{/for}

{#if includeDurationNote}
{#durationNote summaryTableId /}
{/if}
{#if includeMemorySizeNote}
{#memorySizeNote summaryTableId /}
{/if}
