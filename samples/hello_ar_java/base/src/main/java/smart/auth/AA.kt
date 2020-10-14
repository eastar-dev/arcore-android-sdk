package smart.auth

import android.log.Log
import android.util.SDF
import android.webkit.CookieManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.annotations.SerializedName
import dev.eastar.ktx.lower
import dev.eastar.operaxinterceptor.event.OperaXEventObservable
import dev.eastar.operaxinterceptor.event.OperaXEvents
import smart.base.NN
import smart.base.PP
import java.util.*

object AA {

    @JvmStatic
    val isAutoLogin: String
        get() = if (info?.isAutoLogin == true) "Y" else "N"

    private var info: Info? = null

    //    @JvmStatic
    val isLogin: Boolean
        get() {
            return info != null
        }

    //    @JvmStatic
    val custNm: String
        get() {
            return info?.custNm ?: ""
        }

    //    @JvmStatic
    val lastLogin: String
        get() = when {
            !isLogin -> ""
            info?.lstLginDt.isNullOrBlank() -> ""
            PP.appLanguage == Locale.KOREA.language -> "최근 접속일시 " + SDF.yyyymmddhhmmss_2.format(SDF.yyyymmddhhmmss.opt_parse(info!!.lstLginDt + info!!.lstLginTm))
            else -> "Last visited on " + SDF.yyyymmddhhmmss_2.format(SDF.yyyymmddhhmmss.opt_parse(info!!.lstLginDt + info!!.lstLginTm))
        }

    @JvmStatic
    var isOpbkUser: Boolean
        get() = info?.opbkYn ?: false
        set(value) {
            info?.run { opbkYn = value }
        }


    @JvmStatic
    fun login(json: String?) {
        Log.e("login", json)
        if (isLogin) Log.w("!already login")


        val gson = GsonBuilder()
            .registerTypeAdapter(Boolean::class.java, JsonDeserializer { jo, _, _ -> jo.asString.lower == "y" || jo.asString.lower == "true" })
            .registerTypeAdapter(Boolean::class.javaPrimitiveType, JsonDeserializer { jo, _, _ -> jo.asString.lower == "y" || jo.asString.lower == "true" })
            .create()

        info = gson.fromJson(json, Info::class.java)
        FirebaseCrashlytics.getInstance().setUserId(info?.custNo ?: "")
        FirebaseCrashlytics.getInstance().setCustomKey("last_UI_action", "logged_in");
        Session.startSessionTimeout()
        OperaXEventObservable.notify(OperaXEvents.Logined)
    }

    @JvmStatic
    fun logout() {
        FirebaseCrashlytics.getInstance().setCustomKey("last_UI_action", "logged_out");
        Log.w("logout")
        if (!isLogin) Log.w("!already logouted")
        info = null
        Session.stopSessionTimeout()
        CookieManager.getInstance().run {
            setCookie(NN.HOST, "")
            flush()
            removeAllCookies(null)
        }
        OperaXEventObservable.notify(OperaXEvents.Logouted)
    }
}

class Info {
    @SerializedName("custNm", alternate = ["custnm"])
    var custNm: String = ""
    var custNo: String = ""
    var lginMdclCd: String = ""

    @SerializedName("custGrdCd", alternate = ["custgrdcd"])
    var custGrdCd: String = ""

    @SerializedName("lstLginDt", alternate = ["lstlgindt"])
    var lstLginDt: String = ""

    @SerializedName("lstLginTm", alternate = ["lstlgintm"])
    var lstLginTm: String = ""
    var fastTrnsYnForNative: Boolean = false
    var opbkYn: Boolean = false //YN

    var isAutoLogin: Boolean = false //자동로그인여부
}

//회원가입후
//login, {
//  "lginMdclCd": "D",
//  "custnm": "",
//  "trnsPossYn": "",
//  "custgrdcd": "3",
//  "mbReserve": "",
//  "custNo": "",
//  "exhgRtImgSvcNtryYn": "",
//  "curCd": "",
//  "exhgRtCd": "",
//  "pbldExhgRt": "",
//  "imgNm": "",
//  "colrCtt": "",
//  "udeCtt": "",
//  "rejectDesc": "",
//  "stampCnt": "",
//  "userType": "",
//  "opbkYn": "Y"
//}

//로그인후
//login, {
//  "lginMdclCd": "D",
//  "custnm": "정동진",
//  "trnsPossYn": "",
//  "custgrdcd": "3",
//  "lstlgindt": "20200907",
//  "lstlgintm": "023500",
//  "mbReserve": "",
//  "custNo": "037518264",
//  "exhgRtImgSvcNtryYn": "",
//  "curCd": "",
//  "exhgRtCd": "",
//  "pbldExhgRt": "",
//  "imgNm": "",
//  "colrCtt": "",
//  "udeCtt": "",
//  "rejectDesc": "",
//  "stampCnt": "",
//  "userType": "",
//  "opbkYn": "Y"
//}
//자동로그인
//login, {
//  "custGrdCd": "3",
//  "custNm": "정동진",
//  "custNo": "037518264",
//  "fastTrnsYnForNative": "Y",
//  "lginMdclCd": "H",
//  "lstLginDt": "20200907",
//  "lstLginTm": "030858",
//  "opbkYn": "Y",
//  "resultCode": "0000"
//}