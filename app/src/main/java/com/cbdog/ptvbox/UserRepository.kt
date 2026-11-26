package com.cbdog.ptvbox

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/** One writer queue; Compose only consumes immutable snapshots. */
class UserRepository(private val db:Library){
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO.limitedParallelism(1))
    private val mutable=MutableStateFlow(UserSnapshot())
    val state:StateFlow<UserSnapshot> = mutable.asStateFlow()
    private val loaded=MutableStateFlow(false)
    val ready=loaded.asStateFlow()
    private val problem=MutableStateFlow("")
    val loadError=problem.asStateFlow()
    private val failures=MutableSharedFlow<String>(extraBufferCapacity=1)
    val errors=failures.asSharedFlow()
    init{scope.launch{db.changes.collect{try{mutable.value=db.snapshot();problem.value=""}catch(e:Exception){problem.value="用户资料读取失败，原数据保留"}finally{loaded.value=true}}}}
    fun write(action:Library.()->Unit){scope.launch{try{db.action();mutable.value=db.snapshot()}catch(e:Exception){failures.tryEmit("资料保存失败，请重试；原数据保留")}}}
    fun progress(w:Watch)=write{save(w)}
    fun close(){scope.launch{db.close();scope.cancel()}}
}
