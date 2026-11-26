package com.cbdog.ptvbox
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
/** Media bodies may remain open for hours. Retain connect/read stall limits and proxy policy,
 * but never apply the short metadata request's total-call deadline to playback. */
object StreamHttp054 {
 fun client(base:OkHttpClient):OkHttpClient=base.newBuilder().callTimeout(0,TimeUnit.MILLISECONDS).followRedirects(true).build()
}
