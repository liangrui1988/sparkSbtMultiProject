package com.cd.core.constant
/**
  * create by roy 2019/09/10
  */
object LogConstant {
  val dataExtractRegexStr = "(https?://[^\"]*=\\{)"
  val pattern: String = "\\[(?<createTime>\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}.\\d{3})\\] \\[%{DATA:level}\\] \\[%{DATA:className}\\] \\[%{DATA:methodName}\\] \\[%{DATA:thread}\\] \\[%{GREEDYDATA:msg}\\] \\[%{NUMBER:clientType:int}\\] \\[%{NUMBER:step:int}\\] \\[%{NUMBER:flag:int}\\] \\[%{DATA:ip}\\] \\[%{DATA:clientIp}\\] \\[%{NUMBER:costTime:int}\\] \\[%{DATA:isErr}\\] \\[%{DATA:errName}\\] \\[%{DATA:logId}\\] \\[%{DATA:sysName}\\] \\[%{DATA:actionName}\\] \\[%{DATA:apiName}\\] \\[%{DATA:platformKey}\\]"

}
