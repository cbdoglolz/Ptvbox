package com.cbdog.ptvbox

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivateSourceNotice060Test {
    @Test fun privateSubscriptionAdAndScanMessagesStayHiddenDuringPlayback(){
        assertTrue(NoticePolicy057.suppressPluginNoise("饭：已过滤视频中广告(6条)",false,false,true))
        assertTrue(NoticePolicy057.suppressPluginNoise("饭：网盘源需使用百度网盘APP扫码登录",false,false,true))
        assertTrue(NoticePolicy057.suppressPluginNoise("此资源失效",false,false,true))
    }

    @Test fun ordinaryPlayerMessagesAreNotFiltered(){
        assertFalse(NoticePolicy057.suppressPluginNoise("已恢复到 01:23",false,false,true))
    }

    @Test fun oemToastMethodNamesAreStillBlocked(){
        assertTrue(ToastProxyPolicy.shouldBlock("enqueueToast"))
        assertTrue(ToastProxyPolicy.shouldBlock("enqueueToastForWindow"))
        assertTrue(ToastProxyPolicy.shouldBlock("enqueueTextToastAsync"))
        assertTrue(ToastProxyPolicy.shouldBlock("cancelToast"))
        assertFalse(ToastProxyPolicy.shouldBlock("getToastRateLimiterEnabled"))
        assertFalse(ToastProxyPolicy.shouldBlock("notifyPosted"))
    }
}
