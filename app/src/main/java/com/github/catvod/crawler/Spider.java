package com.github.catvod.crawler;

import android.content.Context;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.Dns;
import okhttp3.OkHttpClient;

/** Binary host interface for owner-supplied TVBox plugins. No site implementation is bundled. */
public abstract class Spider {
    public String siteKey = "";
    public static Dns safeDns() { return Dns.SYSTEM; }
    public static OkHttpClient client() { return new OkHttpClient(); }
    public void init(Context context) throws Exception { }
    public void init(Context context, String ext) throws Exception { init(context); }
    public String homeContent(boolean filter) throws Exception { return ""; }
    public String homeVideoContent() throws Exception { return ""; }
    public String categoryContent(String tid, String page, boolean filter, HashMap<String,String> ext) throws Exception { return ""; }
    public String detailContent(List<String> ids) throws Exception { return ""; }
    public String searchContent(String key, boolean quick) throws Exception { return ""; }
    public String searchContent(String key, boolean quick, String page) throws Exception { return searchContent(key, quick); }
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception { return ""; }
    public String liveContent(String url) throws Exception { return ""; }
    public boolean manualVideoCheck() throws Exception { return false; }
    public boolean isVideoFormat(String url) throws Exception { return false; }
    public Object[] proxy(Map<String,String> params) throws Exception { return null; }
    public String action(String action) throws Exception { return null; }
    public void destroy() { }
}
