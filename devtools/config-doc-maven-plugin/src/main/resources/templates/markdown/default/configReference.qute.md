🔒: Configuration property fixed at build time - All other configuration properties are overridable at runtime

🔑: Secret configuration property - Its value is masked and cannot be looked up by name with the `Config` API

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
{#configProperty context=context configProperty=property extension=extension additionalAnchorPrefix=additionalAnchorPrefix /}
{/for}
{#for section in configItemCollection.nonDeprecatedSections}
{#configSection context=context configSection=section extension=extension additionalAnchorPrefix=additionalAnchorPrefix displayConfigRootDescription=false /}
{/for}
</tbody>
</table>

{#if includeDurationNote}
{#durationNote context.summaryTableId /}
{/if}
{#if includeMemorySizeNote}
{#memorySizeNote context.summaryTableId /}
{/if}
