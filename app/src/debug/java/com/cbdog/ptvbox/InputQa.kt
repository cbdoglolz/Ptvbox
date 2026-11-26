package com.cbdog.ptvbox

import android.app.Instrumentation
import android.graphics.Rect
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import org.json.JSONObject

/** Inject actual pointer/key events; never use accessibility ACTION_CLICK as a tap substitute. */
object InputQa {
    fun clickLabel(i:Instrumentation,label:String,mouse:Boolean=false,idle:Boolean=true)=tap(i,target(i,label),mouse,idle)
    fun hasLabel(i:Instrumentation,label:String)=rect(i,label)?.isEmpty==false
    fun labelFocused(i:Instrumentation,label:String):Boolean{fun walk(n:AccessibilityNodeInfo?):Boolean{if(n==null)return false;if(n.contentDescription?.toString()==label&&n.isFocused)return true;for(k in 0 until n.childCount)if(walk(n.getChild(k)))return true;return false};return walk(i.uiAutomation.rootInActiveWindow)||i.uiAutomation.windows.any{walk(it.root)}}

    fun player(i:Instrumentation,m:PtvModel):JSONObject {
        rect(i,"显示控制")?.let{tap(i,it)};Thread.sleep(200)
        tap(i,target(i,"暂停"));i.runOnMainSync{check(!m.player.playWhenReady)}
        tap(i,target(i,"播放"),true);i.runOnMainSync{check(m.player.playWhenReady)}
        tap(i,target(i,"音量"));tap(i,target(i,"音量 +"));i.runOnMainSync{check(m.player.volume>0f);m.player.volume=0f}
        i.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);Thread.sleep(250)
        tap(i,target(i,"音轨"));tap(i,target(i,"关闭"));i.runOnMainSync{check(m.screen=="player")}
        tap(i,target(i,"字幕"));tap(i,target(i,"关闭"))
        tap(i,target(i,"全屏"));tap(i,target(i,"全屏"));
        tap(i,target(i,"隐藏控制"));tap(i,target(i,"显示控制"))
        return JSONObject().put("touchPause",true).put("mousePlay",true).put("volumeMenu",true).put("audioMenu",true).put("subtitleMenu",true).put("fullscreenToggle",true).put("hideShowControls",true)
    }
    private fun rect(i:Instrumentation,label:String):Rect? {
        fun walk(n:AccessibilityNodeInfo?):Rect? {
            if(n==null)return null
            if(n.text?.toString()==label||n.contentDescription?.toString()==label)return Rect().also{n.getBoundsInScreen(it)}
            for(index in 0 until n.childCount)walk(n.getChild(index))?.let{return it}
            return null
        }
        return walk(i.uiAutomation.rootInActiveWindow)?:i.uiAutomation.windows.firstNotNullOfOrNull{walk(it.root)}
    }
    private fun target(i:Instrumentation,label:String):Rect {repeat(25){rect(i,label)?.takeIf{!it.isEmpty}?.let{return it};Thread.sleep(120)};run{val labels=mutableListOf<String>();fun visit(n:AccessibilityNodeInfo?){if(n==null)return;n.text?.let{labels.add(it.toString())};n.contentDescription?.let{labels.add(it.toString())};for(k in 0 until n.childCount)visit(n.getChild(k))};visit(i.uiAutomation.rootInActiveWindow);error("Missing UI target: $label; present=${labels.joinToString("|").take(1500)}")}}
    private fun event(i:Instrumentation,x:Float,y:Float,action:Int,source:Int,down:Long){
        val props=arrayOf(MotionEvent.PointerProperties().apply{id=0;toolType=if(source==InputDevice.SOURCE_MOUSE)MotionEvent.TOOL_TYPE_MOUSE else MotionEvent.TOOL_TYPE_FINGER})
        val coords=arrayOf(MotionEvent.PointerCoords().apply{this.x=x;this.y=y;pressure=if(action==MotionEvent.ACTION_UP)0f else 1f;size=1f})
        val button=if(source==InputDevice.SOURCE_MOUSE&&action!=MotionEvent.ACTION_UP)MotionEvent.BUTTON_PRIMARY else 0
        val e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,1,props,coords,0,button,1f,1f,0,0,source,0)
        try{check(i.uiAutomation.injectInputEvent(e,true))}finally{e.recycle()}
    }
    private fun tap(i:Instrumentation,r:Rect,mouse:Boolean=false,idle:Boolean=true){val source=if(mouse)InputDevice.SOURCE_MOUSE else InputDevice.SOURCE_TOUCHSCREEN;val t=SystemClock.uptimeMillis();event(i,r.exactCenterX(),r.exactCenterY(),MotionEvent.ACTION_DOWN,source,t);Thread.sleep(70);event(i,r.exactCenterX(),r.exactCenterY(),MotionEvent.ACTION_UP,source,t);if(idle)i.waitForIdleSync();Thread.sleep(250)}
    private fun swipe(i:Instrumentation,r:Rect){val t=SystemClock.uptimeMillis();val y=r.exactCenterY();val x=r.exactCenterX();event(i,x,y,MotionEvent.ACTION_DOWN,InputDevice.SOURCE_TOUCHSCREEN,t);for(n in 1..12){Thread.sleep(20);event(i,x-n*15f,y,MotionEvent.ACTION_MOVE,InputDevice.SOURCE_TOUCHSCREEN,t)};event(i,x-180,y,MotionEvent.ACTION_UP,InputDevice.SOURCE_TOUCHSCREEN,t);Thread.sleep(350)}
    private fun scrollMenu(i:Instrumentation){val d=i.targetContext.resources.displayMetrics;val x=130*d.density;val y=d.heightPixels-65*d.density;val t=SystemClock.uptimeMillis();val delta=(y-90*d.density)/12;event(i,x,y,MotionEvent.ACTION_DOWN,InputDevice.SOURCE_TOUCHSCREEN,t);for(n in 1..12){Thread.sleep(20);event(i,x,y-n*delta,MotionEvent.ACTION_MOVE,InputDevice.SOURCE_TOUCHSCREEN,t)};event(i,x,y-12*delta,MotionEvent.ACTION_UP,InputDevice.SOURCE_TOUCHSCREEN,t);Thread.sleep(400)}
    fun run(i:Instrumentation,a:MainActivity,m:PtvModel):JSONObject {
        val result=JSONObject()
        tap(i,target(i,"动画"));i.runOnMainSync{check(m.screen=="anime");result.put("touchNavigatesToAnime",true)}
        tap(i,target(i,"首页"),true);i.runOnMainSync{check(m.screen=="home");result.put("mouseNavigatesHome",true)}
        Thread.sleep(700)
        var hero:Work?=null;i.runOnMainSync{hero=m.feed.firstOrNull()};val work=hero?:error("No home hero for pointer test")
        tap(i,target(i,"查看资料与片源"));i.runOnMainSync{check(m.screen=="detail"&&m.detail?.key==work.key)};i.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);Thread.sleep(250);i.runOnMainSync{check(m.screen=="home")};result.put("touchPosterAndSystemBackToHome",true)
        repeat(4){if(!hasLabel(i,"搜索"))scrollMenu(i)};tap(i,target(i,"搜索"),true)
        i.runOnMainSync{check(m.screen=="search");result.put("railMouseSearch",true)}
        val entry=target(i,"片名 / 已缓存片名的拼音首字母");tap(i,entry);i.sendStringSync("pointerqa");Thread.sleep(400)
        i.runOnMainSync{check(m.query=="pointerqa");result.put("touchKeyboardEntry",true)}
        i.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);Thread.sleep(250)
        var clicks=0;var disabled=0;var latest=0;var callbackVersion by mutableIntStateOf(1)
        val focus=FocusRequester()
        i.runOnMainSync{a.setContent{MaterialTheme{Column(Modifier.fillMaxSize().padding(28.dp)){
            InputButton(onClick={clicks++;latest=callbackVersion},modifier=Modifier.focusRequester(focus)){Text("Pointer probe")}
            InputButton(onClick={disabled++},enabled=false){Text("Disabled probe")}
            LazyRow(Modifier.width(380.dp)){items((0..9).toList()){n->InputButton(onClick={clicks++},modifier=Modifier.width(150.dp).height(90.dp)){Text("Drag $n")}}}
        }}}};i.waitForIdleSync();Thread.sleep(500)
        tap(i,target(i,"Pointer probe"));check(clicks==1);result.put("touchOneTapOneAction",true)
        i.runOnMainSync{callbackVersion=2};Thread.sleep(200);tap(i,target(i,"Pointer probe"),true);check(clicks==2&&latest==2);result.put("mouseOneClickLatestCallback",true)
        tap(i,target(i,"Disabled probe"));tap(i,target(i,"Disabled probe"),true);check(disabled==0);result.put("disabledIgnoresPointer",true)
        val before=target(i,"Drag 0");swipe(i,before);check(clicks==2);val after=rect(i,"Drag 0");check(after==null||after.left<before.left-10);result.put("dragScrollDoesNotClick",true)
        i.runOnMainSync{focus.requestFocus()};i.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_DPAD_CENTER);Thread.sleep(300);check(clicks==3);result.put("dpadOnePressOneAction",true)
        i.uiAutomation.takeScreenshot()?.let{b->java.io.File(i.targetContext.filesDir,"qa-input.png").outputStream().use{b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}}
        return result
    }
}
