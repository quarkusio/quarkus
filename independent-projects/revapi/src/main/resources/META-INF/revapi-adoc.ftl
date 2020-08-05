<#ftl strip_whitespace=true>
<#if reports?has_content>
Old API: <#list analysis.oldApi.archives as archive>${archive.name}<#sep>, </#list>
New API: <#list analysis.newApi.archives as archive>${archive.name}<#sep>, </#list>
<#list reports as report>
<#list report.differences as diff>

revapi: ${diff.description!} [${diff.code}]
  new: ${report.newElement!"<none>"} (${diff.attachments['newArchive']!})
<#if report.newElement! != report.oldElement!>
  old: ${report.oldElement!"<none>"} (${diff.attachments['oldArchive']!})
</#if>
<#list diff.classification?keys as compat><#if diff.classification?api.get(compat) != "NON_BREAKING">  ${compat?capitalize}: ${diff.classification?api.get(compat)?capitalize?replace("_","")}${'\n'}</#if></#list>
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
<#if !['newArchive', 'oldArchive','package','classQualifiedName','classSimpleName','elementKind','exception','methodName','exampleUseChainInNewApi','exampleUseChainInOldApi','fieldName']?seq_contains(key)>
  ${key} = ${diff.attachments[key]}
</#if>
</#list>
</#list>
</#list>
</#if>