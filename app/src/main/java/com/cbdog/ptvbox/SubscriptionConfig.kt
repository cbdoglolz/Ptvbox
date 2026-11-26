package com.cbdog.ptvbox

import org.json.JSONObject

/** Validate before replacing the last working subscription. Keep provider keys unchanged. */
object SubscriptionConfig {
    fun validate(body:String):Int {
        val rows=JSONObject(body).optJSONArray("sites")?:error("订阅没有站点列表")
        require(rows.length() in 1..500){"订阅站点数量无效"}
        val keys=mutableSetOf<String>()
        for(i in 0 until rows.length()) {
            val site=Site.from(rows.getJSONObject(i))
            require(site.key.isNotBlank()&&site.api.isNotBlank()&&site.name.isNotBlank()){"订阅站点缺少必要字段"}
            require(keys.add(site.key)){"订阅包含重复站点标识"}
        }
        return rows.length()
    }
}
