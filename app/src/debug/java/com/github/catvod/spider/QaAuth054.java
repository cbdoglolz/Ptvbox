package com.github.catvod.spider;
import com.github.catvod.crawler.Spider;
import com.cbdog.ptvbox.PluginUi054;
import android.app.AlertDialog;
import android.widget.*;
import android.graphics.Bitmap;
import android.os.*;
import java.util.*;
public class QaAuth054 extends Spider {
 public String searchContent(String key,boolean quick,String page){return "{\"list\":[{\"vod_id\":\"qa054\",\"vod_name\":\"QA授权影片\",\"vod_year\":\"2026\"}]}";}
 public String detailContent(List<String> ids){return "{\"list\":[{\"vod_id\":\"qa054\",\"vod_name\":\"QA授权影片\",\"vod_year\":\"2026\",\"vod_play_from\":\"测试网盘\",\"vod_play_url\":\"正片$qa-token\"}]}";}
 public String playerContent(String flag,String id,List<String> vip){new Handler(Looper.getMainLooper()).post(()->{var activity=PluginUi054.INSTANCE.getAuthActivity();if(activity==null)return;Toast.makeText(activity,"广告过滤5条，扫码失败（仅QA）",Toast.LENGTH_LONG).show();LinearLayout outer=new LinearLayout(activity);outer.setOrientation(LinearLayout.VERTICAL);LinearLayout row=new LinearLayout(activity);outer.addView(row);List<ImageView> images=new ArrayList<>();String[] names={"夸克网盘","阿里网盘","UC网盘"};for(int k=0;k<3;k++){ImageView image=new ImageView(activity);image.setContentDescription(names[k]);image.setImageBitmap(code(k,0));row.addView(image,new LinearLayout.LayoutParams(200,200));images.add(image);}Button refresh=new Button(activity);refresh.setText("刷新二维码");refresh.setOnClickListener(v->{for(int k=0;k<3;k++)images.get(k).setImageBitmap(code(k,(int)System.currentTimeMillis()));});outer.addView(refresh);new AlertDialog.Builder(activity).setTitle("网盘授权测试").setView(outer).setCancelable(true).show();});return "{\"url\":\"\"}";}
 private Bitmap code(int index,int version){try{var m=new com.google.zxing.qrcode.QRCodeWriter().encode("https://example.invalid/qa054/"+index+"/"+version,com.google.zxing.BarcodeFormat.QR_CODE,240,240);Bitmap b=Bitmap.createBitmap(240,240,Bitmap.Config.ARGB_8888);for(int y=0;y<240;y++)for(int x=0;x<240;x++)b.setPixel(x,y,m.get(x,y)?0xff000000:0xffffffff);return b;}catch(Exception e){throw new RuntimeException(e);}}
}
