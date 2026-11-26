package com.cbdog.ptvbox

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI
import java.util.concurrent.TimeUnit

object NetworkPolicy {
    @Volatile private var current=OkHttpClient.Builder().connectTimeout(8,TimeUnit.SECONDS).readTimeout(12,TimeUnit.SECONDS).callTimeout(18,TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false).build()
    @Volatile var userAgent="Mozilla/5.0 Ptvbox/${BuildConfig.VERSION_NAME}";private set
    fun client()=current
    fun directClient()=current.newBuilder().proxy(Proxy.NO_PROXY).build()
    fun validateProxy(text:String):Proxy? {
        if(text.isBlank())return null
        val uri=URI(text);require(uri.scheme in listOf("http","socks5")&&uri.host!=null&&uri.port in 1..65535&&uri.userInfo==null&&uri.path.orEmpty() in listOf("","/")){"代理格式为http://主机:端口或socks5://主机:端口；不接受内嵌账号"}
        return Proxy(if(uri.scheme=="http")Proxy.Type.HTTP else Proxy.Type.SOCKS,InetSocketAddress.createUnresolved(uri.host,uri.port))
    }
    fun apply(context:Context){
        val config=TvPreferences(context);val proxy=validateProxy(SecretStore(context).read("proxy"))
        val ua=config.text("user_agent","Mozilla/5.0 Ptvbox/${BuildConfig.VERSION_NAME}")
        require(ua.length in 1..512&&!ua.contains('\r')&&!ua.contains('\n'))
        val builder=OkHttpClient.Builder().connectTimeout(config.timeout.toLong(),TimeUnit.SECONDS).readTimeout(config.timeout.toLong(),TimeUnit.SECONDS).callTimeout((config.timeout+6).toLong(),TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false).proxy(proxy)
        if(config.flag("doh")){
            // Bootstrap uses numeric resolver addresses, so DoH itself does not require system DNS.
            val bootstrap=OkHttpClient.Builder().proxy(proxy).build()
            builder.dns(DnsOverHttps.Builder().client(bootstrap).url("https://cloudflare-dns.com/dns-query".toHttpUrl()).bootstrapDnsHosts(InetAddress.getByAddress(byteArrayOf(1,1,1,1)),InetAddress.getByAddress(byteArrayOf(1,0,0,1))).build())
        }
        val old=current;current=builder.build();userAgent=ua
        // Active requests retain their original client until completion; applying settings does not abort playback.
        old.connectionPool.evictAll()
    }
}
