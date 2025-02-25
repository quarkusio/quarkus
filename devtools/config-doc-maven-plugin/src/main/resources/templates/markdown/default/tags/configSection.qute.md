<thead>
<tr>
<th align="left" colspan="3">
{configSection.formatTitle}
</th>
</tr>
</thead>

{#for property in configSection.nonDeprecatedProperties}
{#configProperty context=context configProperty=property extension=extension additionalAnchorPrefix=additionalAnchorPrefix /}
{/for}
{#for subsection in configSection.nonDeprecatedSections}
{#configSection context=context configSection=subsection extension=extension additionalAnchorPrefix=additionalAnchorPrefix /}
{/for}
