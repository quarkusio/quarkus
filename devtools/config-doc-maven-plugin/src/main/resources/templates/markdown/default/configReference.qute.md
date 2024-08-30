🔒: Configuration property fixed at build time - All other configuration properties are overridable at runtime

{#if configItemCollection.nonDeprecatedProperties}
| Configuration property | Type | Default |
|------------------------|------|---------|
{#for property in configItemCollection.nonDeprecatedProperties}
{#configProperty configProperty=property extension=extension additionalAnchorPrefix=additionalAnchorPrefix /}
{/for}
{/if}
{#for section in configItemCollection.nonDeprecatedSections}

{#configSection configSection=section extension=extension additionalAnchorPrefix=additionalAnchorPrefix displayConfigRootDescription=false /}
{/for}

{#if includeDurationNote}
{#durationNote summaryTableId /}
{/if}
{#if includeMemorySizeNote}
{#memorySizeNote summaryTableId /}
{/if}
