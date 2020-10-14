package smart.base

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences

/** web에서 값을 저장해서 사용하는것*/
object Pref {
    const val __PUSH_TOKEN = "__PUSH_TOKEN"
    const val __PUSH_NOTI_ENABLE = "__PUSH_NOTI_ENABLE"
    const val __RATE_STORAGE_SAVE = "__RATE_STORAGE_SAVE"
    const val __USER_TYPE = "__USER_TYPE"
    const val __FAVORITE_BRANCH = "__FAVORITE_BRANCH"
    const val __AUTO_LOGIN = "__AUTO_LOGIN"
    const val custNo = "custNo"
    const val userId = "userId"
    const val setNatvYn = "setNatvYn"            //외국인,내국인 내국인인경true??
    const val natvYn = "natvYn"                  //외국인,내국인 내국인인경true
    const val ntltcntycd = "ntltcntycd"


    private const val NAME = "PUSH_PREFERENCE"
    private var PREFERENCES: SharedPreferences? = null
    fun CREATE(context: Context) {
        PREFERENCES = context.getSharedPreferences(NAME, Activity.MODE_PRIVATE)
    }

    @JvmStatic
    operator fun set(key: String?, value: String?) {
        PREFERENCES!!.edit().putString(key, value).apply()
    }

    @JvmStatic
    operator fun get(key: String?): String? {
        return PREFERENCES!!.getString(key, "")
    }
}

/** 회원가입여부 */
val Pref.isJoin: Boolean get() = !this["custNo"].isNullOrEmpty()

/**회원 가입시 저장한 내국인(N), 외국인(F) 여부 코드 전달값 안드 1.06 이전에는 해당값이 없으므로 외국인으로 판단*/
val Pref.nationalCd: String
    get() = when {
        !isJoin -> "" //모름
        Pref["natvYn"] == "Y" -> "N"
        else -> "F"
    }

