<tr>
<td>

{#if configProperty.phase.fixedAtBuildTime}🔒 {/if}`{configProperty.path.property}`
{#for additionalPath in configProperty.additionalPaths}
`{additionalPath.property}`
{/for}

{configProperty.formatDescription.escapeCellContent.or("")}

{#envVar configProperty /}
</td>
<td>

{configProperty.formatTypeDescription(context).escapeCellContent.or("")}
</td>
<td>

{#if configProperty.defaultValue}{configProperty.formatDefaultValue.escapeCellContent}{/if}
</td>
</tr>
