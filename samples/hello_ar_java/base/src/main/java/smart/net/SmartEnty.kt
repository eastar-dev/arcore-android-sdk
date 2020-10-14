package smart.net

import android.volley.NetEnty
import smart.base.NN

open class SmartEnty : NetEnty() {
    fun setUrl(url: String) {
        _url = NN.getUrl(url)
    }
}
