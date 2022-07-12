<#ftl strip_whitespace=true>
<#if reports?has_content>
# API Change analysis Results

The summary of the API changes between artifacts <#list analysis.oldApi.archives as archive>`${archive.name}`<#sep>, </#list> and
<#list analysis.newApi.archives as archive>`${archive.name}`<#sep>, </#list>


<#assign count = 0>
<#compress>
[cols="1,1"]
|===
|*Total API Changes* |


|Classes Changed
|<#list reports as report>
<#list report.differences as diff>
<#if diff.code?contains("class")>
<#assign count++>
</#if>
</#list>
</#list>
xref:classes[${count}]

<#assign count = 0>
|Methods Changed
|<#list reports as report>
<#list report.differences as diff>
<#if diff.code?contains("method")><#assign count++></#if>
</#list>
</#list>
xref:methods[${count}]

<#assign count = 0>
|Fields Changed
|<#list reports as report>
<#list report.differences as diff>
<#if diff.code?contains("field")><#assign count++></#if>
</#list>
</#list>
xref:fields[${count}]

<#assign count = 0>
|Other Changes
|<#list reports as report>
<#list report.differences as diff>
<#if !diff.code?contains("field") && !diff.code?contains("class") && !diff.code?contains("method") ><#assign count++></#if>
</#list>
</#list>
xref:others[${count}]

|===
</#compress>


=== [[classes]]Classes Changed
<#list reports as report>
<#list report.differences as diff>
<#if diff.code?contains("class")>
===============================
[.lead]
*${diff.code}* : <#if report.newElement??>
<#assign temp = report.newElement?split(" ")[0]>
${report.newElement?replace(temp, "")} +
</#if>
<#if report.oldElement??>
<#assign temp = report.oldElement?split(" ")[0]>
${report.oldElement?replace(temp, "")}
</#if>

${diff.description} +
<#list diff.attachments?keys as key>
<#if !['newArchive', 'newArchiveRole', 'oldArchive', 'oldArchiveRole','package','classQualifiedName','classSimpleName','elementKind','exception','methodName','exampleUseChainInNewApi','exampleUseChainInOldApi','fieldName']?seq_contains(key)>
  ${key} = ${diff.attachments[key]}
</#if>
</#list> +
<#list diff.classification?keys as compat>
<#if diff.classification?api.get(compat) != "NON_BREAKING">
*${compat?capitalize} Compatibility*: ${diff.classification?api.get(compat)?capitalize?replace("_","")},
</#if>
</#list>
${diff.justification!""}
===============================
</#if>
</#list>
</#list>

=== [[fields]]Fields Changed
<#list reports as report>
<#list report.differences as diff>
<#if diff.code?contains("field")>
===============================
[.lead]
*${diff.code}* : <#if report.newElement??>
<#assign temp = report.newElement?split(" ")[0]>
${report.newElement?replace(temp, "")} +
</#if>
<#if report.oldElement??>
<#assign temp = report.oldElement?split(" ")[0]>
${report.oldElement?replace(temp, "")}
</#if>

${diff.description} +
<#list diff.attachments?keys as key>
<#if !['newArchive', 'newArchiveRole', 'oldArchive', 'oldArchiveRole','package','classQualifiedName','classSimpleName','elementKind','exception','methodName','exampleUseChainInNewApi','exampleUseChainInOldApi','fieldName']?seq_contains(key)>
  ${key} = ${diff.attachments[key]}
</#if>
</#list> +
<#list diff.classification?keys as compat>
<#if diff.classification?api.get(compat) != "NON_BREAKING">
*${compat?capitalize} Compatibility*: ${diff.classification?api.get(compat)?capitalize?replace("_","")},
</#if>
</#list>
${diff.justification!""}
===============================
</#if>
</#list>
</#list>

=== [[methods]]Methods Changed
<#list reports as report>
<#list report.differences as diff>
<#if diff.code?contains("method")>
===============================
[.lead]
*${diff.code}* : <#if report.newElement??>
<#assign temp = report.newElement?split(" ")[0]>
${report.newElement?replace(temp, "")} +
</#if>
<#if report.oldElement??>
<#assign temp = report.oldElement?split(" ")[0]>
${report.oldElement?replace(temp, "")}
</#if>

${diff.description} +
<#list diff.attachments?keys as key>
<#if !['newArchive', 'newArchiveRole', 'oldArchive', 'oldArchiveRole','package','classQualifiedName','classSimpleName','elementKind','exception','methodName','exampleUseChainInNewApi','exampleUseChainInOldApi','fieldName']?seq_contains(key)>
  ${key} = ${diff.attachments[key]}
</#if>
</#list> +
<#list diff.classification?keys as compat>
<#if diff.classification?api.get(compat) != "NON_BREAKING">
*${compat?capitalize} Compatibility*: ${diff.classification?api.get(compat)?capitalize?replace("_","")},
</#if>
</#list>
${diff.justification!""}
===============================
</#if>
</#list>
</#list>

=== [[others]]Other Changes
<#list reports as report>
<#list report.differences as diff>
<#if !diff.code?contains("field") && !diff.code?contains("class") && !diff.code?contains("method")>
===============================
[.lead]
*${diff.code}* : <#if report.newElement??>
<#assign temp = report.newElement?split(" ")[0]>
${report.newElement?replace(temp, "")} +
</#if>
<#if report.oldElement??>
<#assign temp = report.oldElement?split(" ")[0]>
${report.oldElement?replace(temp, "")}
</#if>

${diff.description} +
<#list diff.attachments?keys as key>
<#if !['newArchive', 'newArchiveRole', 'oldArchive', 'oldArchiveRole','package','classQualifiedName','classSimpleName','elementKind','exception','methodName','exampleUseChainInNewApi','exampleUseChainInOldApi','fieldName']?seq_contains(key)>
  ${key} = ${diff.attachments[key]}
</#if>
</#list> +
<#list diff.classification?keys as compat>
<#if diff.classification?api.get(compat) != "NON_BREAKING">
*${compat?capitalize} Compatibility*: ${diff.classification?api.get(compat)?capitalize?replace("_","")},
</#if>
</#list>
${diff.justification!""}
===============================
</#if>
</#list>
</#list>

</#if>