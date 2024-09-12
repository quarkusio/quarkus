🔒: Configuration property fixed at build time - All other configuration properties are overridable at runtime

<table>
<thead>
<tr>
<th align="left">Configuration property</th>
<th>Type</th>
<th>Default</th>
</tr>
</thead>
<tbody>
{#for property in configItemCollection.nonDeprecatedProperties}
{#configProperty configProperty=property extension=extension additionalAnchorPrefix=additionalAnchorPrefix /}
{/for}
{#for section in configItemCollection.nonDeprecatedSections}
{#configSection configSection=section extension=extension additionalAnchorPrefix=additionalAnchorPrefix displayConfigRootDescription=false /}
{/for}
</tbody>
</table>

{#if includeDurationNote}
{#durationNote summaryTableId /}
{/if}
{#if includeMemorySizeNote}
{#memorySizeNote summaryTableId /}
{/if}
