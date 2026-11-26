package com.cbdog.ptvbox

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text

/** One saved value outside, with all edit/delete/test operations isolated in a dialog. */
@Composable internal fun ConfigValueCard(
    title:String,
    value:String,
    emptyText:String="配置",
    note:String="",
    history:List<String> = emptyList(),
    onSave:(String)->Unit,
    onClear:()->Unit,
    onDeleteHistory:(String)->Unit = {},
    onTest:((String)->Unit)?=null
){
    val c=LocalCinema.current
    val context=LocalContext.current
    var open by remember(title){mutableStateOf(false)}
    var input by remember(title){mutableStateOf("")}
    var restore by remember(title){mutableStateOf(false)}
    var editFocused by remember(title){mutableStateOf(false)}
    var showHistory by remember(title){mutableStateOf(false)}
    val editFocus=remember(title){FocusRequester()}
    LaunchedEffect(open,restore){
        if(!open&&restore){androidx.compose.runtime.withFrameNanos{};runCatching{editFocus.requestFocus()};restore=false}
    }
    fun close(){open=false;restore=true}
    Row(
        Modifier.fillMaxWidth().background(c.raised,RoundedCornerShape(8.dp)).padding(12.dp),
        verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(12.dp)
    ){
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)){
            Row(horizontalArrangement=Arrangement.spacedBy(7.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){
                Text(title,color=c.text,fontSize=14.sp)
                if(value.isNotBlank())Text("✓ 已配置",color=PcGreen,fontSize=11.sp)
            }
            Text(value.ifBlank{emptyText},color=if(value.isBlank())c.muted else PcGreen,fontSize=13.sp,maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            if(note.isNotBlank())Text(note,color=c.muted,fontSize=11.sp,maxLines=2)
        }
        InputButton(
            {input="";open=true},
            Modifier.focusRequester(editFocus).onFocusChanged{editFocused=it.isFocused}.border(1.dp,c.border,RoundedCornerShape(8.dp)),
            shape=androidx.tv.material3.ButtonDefaults.shape(RoundedCornerShape(8.dp)),
            colors=ptvButtonColors(androidx.compose.ui.graphics.Color(0x663e405b),c.text),
            contentPadding=PaddingValues(14.dp,10.dp)
        ){Text("配置",fontSize=14.sp,modifier=Modifier.semantics{contentDescription="配置 $title"+(if(editFocused)" · 当前焦点" else "")})}
    }
    if(open)Dialog({close()},properties=DialogProperties(usePlatformDefaultWidth=false)){
        Column(
            Modifier.widthIn(max=620.dp).fillMaxWidth(.9f).heightIn(max=520.dp)
                .background(c.surface,RoundedCornerShape(12.dp)).padding(18.dp),
            verticalArrangement=Arrangement.spacedBy(10.dp)
        ){
            Text(title,color=c.text,fontSize=20.sp)
            if(note.isNotBlank())Text(note,color=c.muted,fontSize=12.sp,maxLines=3)
            Entry(input,{input=it},"输入新内容；留空不会覆盖现有配置")
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                Action("复制",{
                    val text=input.ifBlank{value}
                    if(text.isNotBlank()){
                        context.getSystemService(android.content.ClipboardManager::class.java)
                            .setPrimaryClip(android.content.ClipData.newPlainText(title,text))
                    }
                })
                onTest?.let{test->Action("测试",{val text=input.ifBlank{value};if(text.isNotBlank())test(text)})}
                if(history.isNotEmpty())Action(if(showHistory)"收起历史" else "历史",{showHistory=!showHistory})
            }
            if(showHistory&&history.isNotEmpty()){
                Text("历史记录",color=c.text,fontSize=14.sp)
                LazyColumn(Modifier.fillMaxWidth().heightIn(max=190.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                    items(history,key={it}){old->
                        Row(Modifier.fillMaxWidth().background(c.raised,RoundedCornerShape(7.dp)).padding(8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){
                            Text(old,color=c.text,fontSize=11.sp,maxLines=2,modifier=Modifier.weight(1f))
                            Action("使用",{input=old})
                            Action("删除",{onDeleteHistory(old)})
                        }
                    }
                }
            }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                Action("保存",{if(input.isNotBlank())onSave(input.trim());close()})
                if(value.isNotBlank())Action("删除当前",{onClear();close()})
                Action("取消",{close()})
            }
        }
    }
}
