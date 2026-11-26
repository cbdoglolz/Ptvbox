package com.cbdog.ptvbox

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import okhttp3.OkHttpClient
import coil3.memory.MemoryCache
import coil3.disk.DiskCache
import okio.Path.Companion.toOkioPath

class PtvApplication:Application(),SingletonImageLoader.Factory {
    override fun onCreate(){super.onCreate();val spider=getProcessName().endsWith(":spider");if(android.os.Build.VERSION.SDK_INT>=28&&!spider)android.webkit.WebView.setDataDirectorySuffix("media052");PluginUi054.install();runCatching{NetworkPolicy.apply(this)}}
    override fun newImageLoader(context:Context):ImageLoader = ImageLoader.Builder(context)
        .memoryCache{MemoryCache.Builder().maxSizePercent(context,TvPreferences(context).budget.imageFraction).build()}
        .diskCache{DiskCache.Builder().directory(context.cacheDir.resolve("posters").toOkioPath()).maxSizeBytes(TvPreferences(context).imageMb*1024L*1024).build()}
        .components { add(OkHttpNetworkFetcherFactory(callFactory={NetworkPolicy.client().newBuilder().followRedirects(true).addInterceptor{chain->
            val builder=chain.request().newBuilder().header("User-Agent",NetworkPolicy.userAgent)
            if(chain.request().url.host.endsWith("doubanio.com"))builder.header("Referer","https://movie.douban.com/")
            chain.proceed(builder.build())
        }.build()})) }.build()
}
