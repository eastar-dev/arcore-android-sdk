package smart.auth

import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.base.CActivity
import android.content.Context
import android.content.Intent
import android.log.Log
import android.os.Bundle
import android.os.Looper
import android.volley.Net
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import dev.eastar.base.R
import smart.base.BActivity
import smart.base.PP
import smart.net.SmartEnty
import java.util.concurrent.TimeUnit

fun Application.session() = registerActivityLifecycleCallbacks(Session)

object Session : Application.ActivityLifecycleCallbacks {
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityDestroyed(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {
        (activity as? AppCompatActivity)?.let { mLastActivity = it }
    }

    private var mLastActivity: AppCompatActivity? = null


    private const val MAX_SESSION_MINUTES: Long = 10
    private const val TIMEWORN_WORKER = "timeWarnWorker"
    private const val TIMEOUT_WORKER = "timeoutWorker"


    private var mAliveRefCount: Int = 0
//    private val timeoutWarnWorker by lazy {
//        OneTimeWorkRequest.Builder(TimeoutWarnWorker::class.java)
//                .setInitialDelay(MAX_SESSION_MINUTES - 1, TimeUnit.MINUTES)
//                .addTag(TIMEWORN_WORKER)
//                .build()
//    }
//    private val timeoutWorker by lazy {
//        OneTimeWorkRequest.Builder(TimeoutWorker::class.java)
//                .setInitialDelay(MAX_SESSION_MINUTES, TimeUnit.MINUTES)
//                .addTag(TIMEOUT_WORKER)
//                .build()
//    }

    @JvmStatic
    fun startSessionTimeout() {
        stopSessionTimeout()
        if (!AA.isLogin)
            return
        //사용시 마다 생성해서 써야 2번째 등록시 정상 호출됨.
        val timeoutWarnWorker1 by lazy {
            OneTimeWorkRequest.Builder(TimeoutWarnWorker::class.java)
                .setInitialDelay(MAX_SESSION_MINUTES - 1, TimeUnit.MINUTES)
                .addTag(TIMEWORN_WORKER)
                .build()
        }
        val timeoutWorker1 by lazy {
            OneTimeWorkRequest.Builder(TimeoutWorker::class.java)
                .setInitialDelay(MAX_SESSION_MINUTES, TimeUnit.MINUTES)
                .addTag(TIMEOUT_WORKER)
                .build()
        }
        WorkManager.getInstance().enqueue(listOf(timeoutWarnWorker1, timeoutWorker1))
        Log.w("SS", "startSessionTimeout ", mAliveRefCount);
    }

    @JvmStatic
    fun stopSessionTimeout() {
        Log.w("SS", "stopSessionTimeout ", mAliveRefCount);
        try {
            WorkManager.getInstance().cancelAllWorkByTag(TIMEWORN_WORKER)
        } catch (e: Exception) {
            Log.printStackTrace(e)
        }

        try {
            WorkManager.getInstance().cancelAllWorkByTag(TIMEOUT_WORKER)
        } catch (e: Exception) {
            Log.printStackTrace(e)
        }
    }

    @JvmStatic
    fun updateSession() {
        //        Log.w("SS", "updateSession ", mAliveRefCount);
        stopSessionTimeout()
        if (!AA.isLogin) {
            Log.w("!SS", "로그인이 안되서 안감 updateSession ", mAliveRefCount)
            return
        }
        Net.asyncKt(SessionUpdate())
        //SessionUpdate 성공시 startSessionTimeout 호출됨.
        //startSessionTimeout()
    }

    //다이얼로그가 2개 떠있는 문제 수정
    var mTimeOutCheckDialog: AlertDialog? = null
    var mTimeOutedDialog: AlertDialog? = null

    class TimeoutWarnWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
        override fun doWork(): Result {
            if (mAliveRefCount > 0) {
                updateSession()
                return Result.success()
            }
            Log.w("SS", "TimeoutWarnWorker ", mAliveRefCount)
            mLastActivity?.let {
                if (it.lifecycle.currentState == Lifecycle.State.DESTROYED)
                    return Result.success()

                it.runOnUiThread {
                    Log.e("SS", it.javaClass.simpleName, it.lifecycle.currentState, "1분 후 로그아웃 됩니다. 연장하시겠습니까?")
                    if (mTimeOutedDialog != null) { //sleep상태에 장기간 빠진후 wakeup시 TimeoutWorker가 먼저 실행될수 있음
                        Log.e("SS", it.javaClass.simpleName, it.lifecycle.currentState, "이미 종료 다이얼로그가 호츨")
                        return@runOnUiThread
                    }
                    var strMsg = it.getString(R.string.ngn_30_007_001)//"1분 후 로그아웃 됩니다. 연장하시겠습니까?"
                    var strConfirm = it.getString(R.string.ngn_30_007_002)//
                    var strCancel = it.getString(R.string.etc_10_002_002)//
                    if (it is BActivity) {
                        strMsg = it.getString(R.string.ngn_30_007_001)
                        strConfirm = it.getString(R.string.ngn_30_007_002)
                        strCancel = it.getString(R.string.etc_10_002_002)
                    }
                    mTimeOutCheckDialog = AlertDialog.Builder(it).setMessage(strMsg)
                        .setPositiveButton(strConfirm) { _, _ -> updateSession() }
                        .setNegativeButton(strCancel, null)
                        .setCancelable(false)
                        .setOnDismissListener { d -> mTimeOutCheckDialog = null }
                        .show()
                }
            }
            return Result.success()
        }
    }

    class TimeoutWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
        override fun doWork(): Result {
            android.os.Handler(Looper.getMainLooper()).post { AA.logout() }

            Log.w("SS", "TimeoutWarnWorker ", mAliveRefCount)
            mLastActivity?.run {
                if (lifecycle.currentState == Lifecycle.State.DESTROYED)
                    return Result.success()

                runOnUiThread {
                    //기존 체크 다이얼로그가 떠있으면...닫는다...
                    mTimeOutCheckDialog?.dismiss()

                    Log.e("SS", javaClass.simpleName, lifecycle.currentState, "10분이상 사용하지 않아 로그아웃 합니다.")
                    var strMsg = getString(R.string.ngn_30_008_001)//"10분이상 사용하지 않아 로그아웃 합니다."
                    var strConfirm = getString(R.string.etc_10_003_009)//
                    if (this is BActivity ) {
                        strMsg = getString(R.string.ngn_30_008_001)
                        strConfirm = getString(R.string.etc_10_003_009)
                    }

                    mTimeOutedDialog = AlertDialog.Builder(this).setMessage(strMsg)
                        .setPositiveButton(strConfirm) { _, _ -> main() }
                        .setCancelable(false)
                        .setOnDismissListener { d -> mTimeOutedDialog = null }
                        .show()
                }
            }
            return Result.success()
        }
    }

    private fun main() {
        mLastActivity?.run { startActivity(packageManager.getLaunchIntentForPackage(packageName)?.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)) }
    }

    @JvmStatic
    fun addRef() {
        mAliveRefCount++
        Log.w("SS", "addRef ", mAliveRefCount)
    }

    @JvmStatic
    fun removeRef() {
        mAliveRefCount--
        Log.w("SS", "removeRef ", mAliveRefCount)
        if (mAliveRefCount < 0)
            mAliveRefCount = 0
    }


    //--------------------------------------------------------------------------------------------
    class SessionUpdate : SmartEnty() {
        private val data: Data? = null

        init {
            setUrl("/api/cmn/sessionContinue.do")
        }

        override fun parseData(json: String) {
            super.parseData(json)
            data?.result?.publicKey?.takeUnless { it.isBlank() }?.let {
                PP.publicKey = it
            }
        }

        private inner class Data {
            internal var status: String? = null
            internal var result: Result? = null

            inner class Result {
                var publicKey: String? = null
            }
        }
    }

}
