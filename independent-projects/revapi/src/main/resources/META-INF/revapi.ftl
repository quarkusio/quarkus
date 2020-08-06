<#ftl strip_whitespace=true>
<#if reports?has_content>
# API Change analysis Results

The summary of the API changes between artifacts <#list analysis.oldApi.archives as archive>`${archive.name}`<#sep>, </#list> and 
<#list analysis.newApi.archives as archive>`${archive.name}`<#sep>, </#list>

[cols="1,1,1,1,1", options="header"]
.Changes
|===
|Code
|Element
|Classification
|Description
|Justification

<#list reports as report>

<#list report.differences as diff>

|${diff.code}
|<#if report.newElement??>*${report.newElement}*</#if>
<#if report.oldElement??>*${report.oldElement}*</#if>
<#if diff.attachments['exampleUseChainInNewApi']??>
  Example use chain in new api:
<#list diff.attachments['exampleUseChainInNewApi']?split(" <- ") as e>
  <-${e}
</#list>
</#if>
<#if diff.attachments['exampleUseChainInOldApi']?? && diff.attachments['exampleUseChainInNewApi']! != diff.attachments['exampleUseChainInOldApi']>
  Example use chain in old api:
<#list diff.attachments['exampleUseChainInOldApi']?split(" <- ") as e>
  <-${e}
</#list>
</#if>

<#list diff.attachments?keys as key>
<#if !['newArchive', 'newArchiveRole', 'oldArchive', 'oldArchiveRole','package','classQualifiedName','classSimpleName','elementKind','exception','methodName','exampleUseChainInNewApi','exampleUseChainInOldApi','fieldName']?seq_contains(key)>
  ${key} = ${diff.attachments[key]}
</#if>
</#list>
|<#list diff.classification?keys as compat><#if diff.classification?api.get(compat) != "NON_BREAKING">  ${compat?capitalize}: ${diff.classification?api.get(compat)?capitalize?replace("_","")}${'\n'}</#if></#list>
|${diff.description}
|${diff.justification!""}

</#list>
</#list>
|===
</#if>