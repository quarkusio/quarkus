h|[[{configSection.toAnchor(extension, additionalAnchorPrefix)}]] [.section-name.section-level{configSection.level}]##{configSection.formatTitle.escapeCellContent}##
h|Type
h|Default

{#for item in configSection.items}
{#if !item.deprecated}
{#if item.isSection}
{#configSection configSection=item extension=extension additionalAnchorPrefix=additionalAnchorPrefix /}

{#else}
{#configProperty configProperty=item extension=extension additionalAnchorPrefix=additionalAnchorPrefix /}

{/if}
{/if}
{/for}