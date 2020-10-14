package smart.util

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import smart.base.NN

@BindingAdapter("app:src")
fun setSrc(v: ImageView, src: Any?) {
    when {
        src is String && src.startsWith("/") ->
            Glide.with(v.context).load(NN.getUrl(src)).into(v)
        else ->
            Glide.with(v.context).load(src).into(v)
    }
}
