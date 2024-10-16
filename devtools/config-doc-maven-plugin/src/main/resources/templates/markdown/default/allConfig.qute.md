🔒: Configuration property fixed at build time - All other configuration properties are overridable at runtime

{#for extensionConfigRootsEntry in configRootsByExtensions}

# {extensionConfigRootsEntry.key.formatName}

<table>
<thead>
<tr>
<th align="left">Configuration property</th>
<th>Type</th>
<th>Default</th>
</tr>
</thead>
<tbody>
{#for configRoot in extensionConfigRootsEntry.value.values}
{#for item in configRoot.items}
{#if !item.deprecated}
{#if !item.isSection}
{#configProperty context=context configProperty=item extension=extensionConfigRootsEntry.key additionalAnchorPrefix=additionalAnchorPrefix /}
{#else}
{#configSection context=context configSection=item extension=extensionConfigRootsEntry.key additionalAnchorPrefix=additionalAnchorPrefix /}
{/if}
{/if}
{/for}
{/for}
</tbody>
</table>
{/for}

{#if includeDurationNote}
{#durationNote context.summaryTableId /}
{/if}
{#if includeMemorySizeNote}
{#memorySizeNote context.summaryTableId /}
{/if}
