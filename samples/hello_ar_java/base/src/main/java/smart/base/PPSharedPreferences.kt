package smart.base

import dev.eastar.pref.annotation.Pref

//웹에서 엡으로 저장하는 data
@Pref("PUSH_PREFERENCE")
data class PPPSharedPreferences(
        //@formatter:off
        val custNo                        : String,
        val userId                        : String,
        val natvYn                        : String,//외국인,내국인 내국인인경우 Y
        val setNatvYn                     : String,
        val ntltcntycd                    : String,
        //@formatter:on
)
