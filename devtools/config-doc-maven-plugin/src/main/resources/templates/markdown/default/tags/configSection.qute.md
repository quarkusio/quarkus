{configSection.formatTitle}

{#if configSection.nonDeprecatedProperties}
| Configuration property | Type | Default |
|------------------------|------|---------|
{#for property in configSection.nonDeprecatedProperties}
{#configProperty configProperty=property extension=extension additionalAnchorPrefix=additionalAnchorPrefix /}
{/for}
{/if}
{#for subsection in configSection.nonDeprecatedSections}

{#configSection configSection=subsection extension=extension additionalAnchorPrefix=additionalAnchorPrefix /}
{/for}