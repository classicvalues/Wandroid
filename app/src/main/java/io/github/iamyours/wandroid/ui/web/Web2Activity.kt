package io.github.iamyours.wandroid.ui.web

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebView
import androidx.lifecycle.Observer
import io.github.iamyours.router.ARouter
import io.github.iamyours.router.annotation.Route
import io.github.iamyours.wandroid.BuildConfig
import io.github.iamyours.wandroid.R
import io.github.iamyours.wandroid.base.BaseActivity
import io.github.iamyours.wandroid.databinding.ActivityWeb2Binding
import io.github.iamyours.wandroid.db.AppDataBase
import io.github.iamyours.wandroid.extension.arg
import io.github.iamyours.wandroid.extension.copy
import io.github.iamyours.wandroid.extension.openBrowser
import io.github.iamyours.wandroid.extension.viewModel
import io.github.iamyours.wandroid.util.Constants
import io.github.iamyours.wandroid.widget.BottomStyleDialog
import io.github.iamyours.wandroid.widget.WanWebView
import kotlinx.android.synthetic.main.dialog_more.view.*

@Route(path = "/web2")
class Web2Activity : BaseActivity<ActivityWeb2Binding>() {
    override val layoutId: Int
        get() = R.layout.activity_web2
    val link by arg<String>("link")
    var navTitle = ""
    val cacheDao = AppDataBase.get().cacheDao()

    val vm by viewModel<Web2VM> {
        collect.value = intent.getBooleanExtra("collect", false)
        articleId.value = intent.getIntExtra("articleId", 0)
        articleUrl.value = link
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.vm = vm
        vm.attachLoading(loadingState)
        vm.collect.observe(this, Observer {
            val data = Intent()
            data.putExtra("collect", vm.collect.value ?: false)
            setResult(Constants.RESULT_COLLECT_CHANGED, data)
        })
        vm.toLogin.observe(this, Observer {
            ARouter.getInstance()
                .build("/login")
                .navigation(this) { _, resultCode, _ ->
                    if (resultCode == Constants.RESULT_LOGIN) {
                        vm.isLogin.value = true
                    }
                }
        })
        vm.showMore.observe(this, Observer {
            showMoreDialog()
        })
        initWebView()
    }

    private fun initWebView() {
        binding.webView.run {
            settings.run {
                javaScriptEnabled = true
            }
            setBackgroundColor(0)
            scrollListener = object : WanWebView.OnScrollChangedListener {
                override fun onScroll(dx: Int, dy: Int, oldX: Int, oldY: Int) {
                    vm.title.value = if (dy < 10) "" else navTitle
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
            }
        }
        vm.html.observe(this, Observer {
            binding.webView.loadData(
                it,
                "text/html",
                "UTF-8"
            )
            vm.loaded.value = true
        })
    }

    private fun showMoreDialog() {
        val v = LayoutInflater.from(this).inflate(R.layout.dialog_more, null)
        val dialog = BottomStyleDialog(this)
        dialog.setContentView(v)
        val cached = cacheDao.hasCache(link ?: "")
        if (vm.articleId.value == 0) {
            v.dv_collect.visibility = View.GONE
        }
        v.dv_download.isSelected = cached
        v.dtv_download.text = if (cached) "已下载" else "下载"
//        v.dv_download.setOnClickListener {
//            downHtml(cached)
//            saveCacheOrNot(link, navTitle, cached)
//            dialog.dismiss()
//        }
        v.dv_link.setOnClickListener {
            link?.copy(it.context)
            dialog.dismiss()
        }
        v.dv_open_link.setOnClickListener {
            link?.openBrowser(it.context)
            dialog.dismiss()
        }
        v.dv_collect.isSelected = vm.collect.value ?: false
        v.dv_collect.setOnClickListener {
            vm.collectOrNot()
            dialog.dismiss()
        }
        v.dtv_cancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}