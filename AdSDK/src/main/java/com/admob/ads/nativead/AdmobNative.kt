package com.admob.ads.nativead

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.RatingBar
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.admob.AdFormat
import com.admob.AdType
import com.admob.Constant
import com.admob.TAdCallback
import com.admob.ads.AdsSDK
import com.admob.ads.R
import com.admob.ads.databinding.AdLoadingViewBinding
import com.admob.getActivityOnTop
import com.admob.getAppCompatActivityOnTop
import com.admob.getPaidTrackingBundle
import com.admob.isEnable
import com.admob.isNetworkAvailable
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.firebase.crashlytics.FirebaseCrashlytics

object AdmobNative {

    interface INativeLoadCallback {
        fun forNativeAd(space: String, nativeAd: NativeAd) {}
    }

    private const val TAG = "AdmobNative"

    private val natives = mutableMapOf<String, NativeAd?>()
    private val nativesCallback = mutableMapOf<String, TAdCallback?>()
    private val nativeWithViewGroup = mutableMapOf<String, ViewGroup?>()
    private val nativesLoading = mutableMapOf<String, INativeLoadCallback>()
    private val nativesCollapsiblePopupWindow = mutableMapOf<String, PopupWindow>()

    fun loadOnly(adUnitId: String) {
        if (!AdsSDK.isEnableNative) {
            return
        }

        load(adUnitId)
    }

    /**
     * @param adContainer: ViewGroup contain this Native
     * @param nativeContentLayoutId LayoutRes for Native
     * @param forceRefresh always load new ad then fill to ViewGroup
     * @param callback callback
     */
    fun show(
        adContainer: ViewGroup,
        space: String,
        @LayoutRes nativeContentLayoutId: Int ,
        forceRefresh: Boolean = false,
        callback: TAdCallback? = null
    ) {

        val adChild = AdsSDK.getAdChild(space) ?: return

        if (!AdsSDK.isEnableNative || AdsSDK.isPremium || (adChild.adsType != AdFormat.Native) || !AdsSDK.app.isNetworkAvailable() || !adChild.isEnable()) {
            adContainer.removeAllViews()
            callback?.onDisable()
            adContainer.isVisible = false
            return
        }

        addLoadingLayout(adContainer)

        if (!adContainer.context.isNetworkAvailable()) {
            return
        }

        val nativeAd = natives[space]

        val nativeIsStillLoading = nativesLoading.contains(space)

        // Native vẫn tiếp tục loading
        if (nativeIsStillLoading) {
            nativesLoading[space] = object : INativeLoadCallback {
                override fun forNativeAd(space: String, nativeAd: NativeAd) {
                    fillNative(
                        adContainer,
                        nativeAd,
                        nativeContentLayoutId,
                        null,
                        null,
                        space,
                        callback
                    )
                }
            }
        } else {
            // Native đang ko loading và (đang null hoặc forceRefresh) thì load lại quảng cáo
            if (nativeAd == null) {
                load(space, callback, object : INativeLoadCallback {
                    override fun forNativeAd(space: String, nativeAd: NativeAd) {
                        fillNative(
                            adContainer,
                            nativeAd,
                            nativeContentLayoutId,
                            null,
                            null,
                            space,
                            callback
                        )
                    }
                })
            } else {
                // Native đang ko loading  và có sẵn quảng cáo thì fill luôn
                fillNative(
                    adContainer,
                    nativeAd,
                    nativeContentLayoutId,
                    null,
                    null,
                    space,
                    callback
                )

                // Nếu forceRefresh thì load quảng cáo mới
                if (forceRefresh) {
                    load(space, callback, object : INativeLoadCallback {
                        override fun forNativeAd(space: String, nativeAd: NativeAd) {
                            fillNative(
                                adContainer,
                                nativeAd,
                                nativeContentLayoutId,
                                null,
                                null,
                                space,
                                callback
                            )
                        }
                    })
                }
            }
        }
    }


    fun showCollapsible(
        adContainer: ViewGroup,
        space: String,
        @LayoutRes nativeContentLayoutId: Int ,
        @LayoutRes nativeCollapsibleLayoutId: Int ,
        lifecycle: Lifecycle,
        forceRefresh: Boolean = false,
        callback: TAdCallback? = null
    ) {

        val adChild = AdsSDK.getAdChild(space) ?: return

        if (!AdsSDK.isEnableNative || AdsSDK.isPremium || (adChild.adsType != AdFormat.Native) || !AdsSDK.app.isNetworkAvailable() || !adChild.isEnable()) {
            adContainer.removeAllViews()
            callback?.onDisable()
            adContainer.isVisible = false
            return
        }

        addLoadingLayout(adContainer)

        if (!adContainer.context.isNetworkAvailable()) {
            return
        }

        val nativeAd = natives[space]
        val nativeIsStillLoading = nativesLoading.contains(space)

        // Native vẫn tiếp tục loading
        if (nativeIsStillLoading) {
            nativesLoading[space] = object : INativeLoadCallback {
                override fun forNativeAd(space: String, nativeAd: NativeAd) {
                    fillNative(
                        adContainer,
                        nativeAd,
                        nativeContentLayoutId,
                        nativeCollapsibleLayoutId, lifecycle, space, callback)

                }
            }
        } else {
            // Native đang ko loading và (đang null hoặc forceRefresh) thì load lại quảng cáo
            if (nativeAd == null) {
                load(space, callback, object : INativeLoadCallback {
                    override fun forNativeAd(space: String, nativeAd: NativeAd) {
                        fillNative(
                            adContainer,
                            nativeAd,
                            nativeContentLayoutId,
                            nativeCollapsibleLayoutId, lifecycle, space, callback)
                    }
                })
            } else {
                // Native đang ko loading  và có sẵn quảng cáo thì fill luôn
                fillNative(
                    adContainer,
                    nativeAd,
                    nativeContentLayoutId,
                    null, null, space, callback)

                // Nếu forceRefresh thì load quảng cáo mới
                if (forceRefresh) {
                    dismissCollapsible(space)
                    load(space, callback, object : INativeLoadCallback {
                        override fun forNativeAd(space: String, nativeAd: NativeAd) {
                            fillNative(
                                adContainer,
                                nativeAd,
                                nativeContentLayoutId,
                                nativeCollapsibleLayoutId, lifecycle, space, callback)
                        }
                    })
                }
            }
        }
    }

    private fun load(
        space: String,
        callback: TAdCallback? = null,
        nativeLoadCallback: INativeLoadCallback = object : INativeLoadCallback {}
    ) {

        val adChild = AdsSDK.getAdChild(space) ?: return

        if (!AdsSDK.app.isNetworkAvailable()) {
            return
        }

        if (adChild.adsType != AdFormat.Native) return

        if (!adChild.isEnable()) return

        val id = if (AdsSDK.isDebugging) Constant.ID_ADMOB_NATIVE_TEST else adChild.adsId
        val activity = AdsSDK.getAppCompatActivityOnTop() ?: AdsSDK.getActivityOnTop() ?: AdsSDK.app
        val adLoader = AdLoader.Builder(activity, id)
            .forNativeAd { ad: NativeAd ->
                natives[space]?.destroy()
                natives[space] = ad
                nativesLoading[space]?.forNativeAd(space, ad)

            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    nativesLoading.remove(space)
                    natives[space] = null
                    runCatching { Throwable(adError.message) }
                    AdsSDK.adCallback.onAdFailedToLoad(adChild.adsId, AdType.Native, adError)
                    getNativeCallback(space)?.onAdFailedToLoad(
                        adChild.adsId,
                        AdType.Native,
                        adError
                    )
                }

                override fun onAdClicked() {
                    AdsSDK.adCallback.onAdClicked(adChild.adsId, AdType.Native)
                    getNativeCallback(space)?.onAdClicked(adChild.adsId, AdType.Native)
                }

                override fun onAdClosed() {
                    AdsSDK.adCallback.onAdClosed(adChild.adsId, AdType.Native)
                    getNativeCallback(space)?.onAdClosed(adChild.adsId, AdType.Native)
                }


                override fun onAdImpression() {
                    AdsSDK.adCallback.onAdImpression(adChild.adsId, AdType.Native)
                    getNativeCallback(space)?.onAdImpression(adChild.adsId, AdType.Native)
                }

                override fun onAdLoaded() {
                    nativesLoading.remove(space)
                    AdsSDK.adCallback.onAdLoaded(adChild.adsId, AdType.Native)
                    getNativeCallback(space)?.onAdLoaded(adChild.adsId, AdType.Native)
                }

                override fun onAdOpened() {
                    AdsSDK.adCallback.onAdOpened(adChild.adsId, AdType.Native)
                    getNativeCallback(space)?.onAdOpened(adChild.adsId, AdType.Native)
                }

            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setVideoOptions(VideoOptions.Builder().setStartMuted(true).build())
                    .build()
            )
            .build()
        nativesLoading[space] = nativeLoadCallback
        adLoader.loadAd(AdRequest.Builder().build())

        AdsSDK.adCallback.onAdStartLoading(adChild.adsId, AdType.Native)
        callback?.onAdStartLoading(adChild.adsId, AdType.Native)
    }

    private fun fillNative(
        viewGroup: ViewGroup,
        nativeAd: NativeAd,
        @LayoutRes nativeContentLayoutId: Int,
        @LayoutRes nativeCollapsibleLayoutId: Int? = null,
        lifecycle: Lifecycle? = null,
        space: String,
        callback: TAdCallback?
    ) {
        if (!AdsSDK.isEnableNative) return

        val adUnitId = AdsSDK.getAdChild(space)?.adsId ?: return
        setNativeCallback(space, callback)
        try {
            nativeAd.setOnPaidEventListener(null)
            nativeAd.setOnPaidEventListener { adValue ->
                val bundle =
                    getPaidTrackingBundle(adValue, adUnitId, "Native", nativeAd.responseInfo)
                AdsSDK.adCallback.onPaidValueListener(bundle)
                callback?.onPaidValueListener(bundle)
            }


//            val contentNativeView = LayoutInflater
//                .from(viewGroup.context)
//                .inflate(nativeContentLayoutId, null, false)
//
//            val unifiedNativeAdView = NativeAdView(AdsSDK.app)
//
//            unifiedNativeAdView.layoutParams = ViewGroup.LayoutParams(
//                ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.MATCH_PARENT
//            )
//
//            contentNativeView.parent?.let {
//                (it as ViewGroup).removeView(contentNativeView)
//            }
            val isViewSaveEnable = try {
                if (nativeAd.responseInfo?.adapterResponses?.find { it.adapterClassName == "com.google.ads.mediation.facebook.FacebookMediationAdapter" } != null) {
                    viewGroup.isSaveEnabled = false
                    viewGroup.isSaveFromParentEnabled = false
                    false
                } else {
                    true
                }
            } catch (e: Exception) {
                true
            }

//            unifiedNativeAdView.addView(contentNativeView)
//            populateUnifiedNativeAdView(nativeAd, unifiedNativeAdView)
//            viewGroup.removeAllViews()
//            viewGroup.addView(unifiedNativeAdView)
            showNativeAdView(
                space,
                lifecycle = lifecycle,
                nativeContentLayoutId,
                nativeAd,
                viewGroup,
                isViewSaveEnable,
                false,
                callback
            )
            if (nativeCollapsibleLayoutId != null){
                showNativeAdView(
                    space,
                    lifecycle = lifecycle,
                    nativeCollapsibleLayoutId,
                    nativeAd,
                    viewGroup,
                    isViewSaveEnable,
                    true,
                    callback
                )
            }

            nativeWithViewGroup[space] = viewGroup

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showNativeAdView(
        space: String,
        lifecycle: Lifecycle?,
        @LayoutRes nativeLayoutId: Int,
        nativeAd: NativeAd,
        viewGroup: ViewGroup,
        isViewSaveEnable: Boolean,
        showOnPopup: Boolean,
        callback: TAdCallback? = null
    ) {
        if (!AdsSDK.isEnableNative) return

        val contentNativeView = LayoutInflater
            .from(viewGroup.context)
            .inflate(nativeLayoutId, null, false)

        val unifiedNativeAdView = NativeAdView(AdsSDK.app)

        unifiedNativeAdView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        contentNativeView.parent?.let {
            (it as ViewGroup).removeView(contentNativeView)
        }

        contentNativeView.isSaveEnabled = isViewSaveEnable
        contentNativeView.isSaveFromParentEnabled = isViewSaveEnable

        unifiedNativeAdView.isSaveEnabled = isViewSaveEnable
        unifiedNativeAdView.isSaveFromParentEnabled = isViewSaveEnable

        unifiedNativeAdView.addView(contentNativeView)
        populateUnifiedNativeAdView(nativeAd, unifiedNativeAdView)

        if (showOnPopup) {
            Log.e(TAG, "showNativeAdView: popup", )
            val popupWindow = PopupWindow(
                unifiedNativeAdView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            popupWindow.setOnDismissListener {
                nativesCollapsiblePopupWindow.remove(space)
                callback?.onCollapsibleDismiss()
                AdsSDK.adCallback.onCollapsibleDismiss()
            }
            nativesCollapsiblePopupWindow[space] =  popupWindow

            popupWindow.showAsDropDown(viewGroup, 0, -contentNativeView.minimumHeight)
            contentNativeView.findViewById<View>(R.id.btnCloseNative)?.setOnClickListener {
                popupWindow.dismiss()
            }

            lifecycle?.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(
                    source: LifecycleOwner, event: Lifecycle.Event
                ) {
                    if (event == Lifecycle.Event.ON_DESTROY) {
                        popupWindow.dismiss()
                    }
                }
            })
        } else {
            viewGroup.removeAllViews()
            viewGroup.addView(unifiedNativeAdView)
        }

    }

    private fun dismissCollapsible(space: String){
        kotlin.runCatching {
            nativesCollapsiblePopupWindow[space]?.dismiss()
        }

    }


    private fun populateUnifiedNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        // Set the media view.
        val viewGroup = adView.findViewById<ViewGroup>(R.id.ad_media)
        if (viewGroup != null) {
            val mediaView = MediaView(adView.context)
            viewGroup.addView(
                mediaView,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            adView.mediaView = mediaView
        }

        try {
            val viewGroupIcon = adView.findViewById<View>(R.id.ad_app_icon)
            if (viewGroupIcon != null) {
                if (viewGroupIcon is ViewGroup) {
                    val nativeAdIcon = ImageView(adView.context)
                    viewGroupIcon.addView(
                        nativeAdIcon,
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    )
                    adView.iconView = nativeAdIcon
                } else {
                    adView.iconView = viewGroupIcon
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.priceView = adView.findViewById(R.id.ad_price)
        adView.starRatingView = adView.findViewById(R.id.ad_stars)
        adView.storeView = adView.findViewById(R.id.ad_store)
        adView.advertiserView = adView.findViewById(R.id.ad_advertiser)

        try {
            (adView.headlineView as TextView).text = nativeAd.headline
            if (adView.mediaView != null && nativeAd.mediaContent != null) {
                adView.mediaView!!.mediaContent = nativeAd.mediaContent!!
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            if (nativeAd.body == null) {
                adView.bodyView!!.visibility = View.INVISIBLE
            } else {
                adView.bodyView!!.visibility = View.VISIBLE
                (adView.bodyView as TextView).text = nativeAd.body
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            if (adView.callToActionView != null) {
                if (adView.callToActionView != null) {
                    if (nativeAd.callToAction == null) {
                        adView.callToActionView!!.visibility = View.INVISIBLE
                    } else {
                        adView.callToActionView!!.visibility = View.VISIBLE
                        if (adView.callToActionView is Button) {
                            (adView.callToActionView as Button).text = nativeAd.callToAction
                        } else {
                            (adView.callToActionView as TextView).text = nativeAd.callToAction
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            if (adView.iconView != null) {
                if (nativeAd.icon == null) {
                    adView.iconView!!.visibility = View.INVISIBLE
                } else {
                    (adView.iconView as ImageView).setImageDrawable(
                        nativeAd.icon!!.drawable
                    )
                    adView.iconView!!.visibility = View.VISIBLE
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            if (adView.priceView != null) {
                if (nativeAd.price == null) {
                    adView.priceView!!.visibility = View.INVISIBLE
                } else {
                    adView.priceView!!.visibility = View.VISIBLE
                    (adView.priceView as TextView).text = nativeAd.price
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            if (adView.storeView != null) {
                if (nativeAd.store == null) {
                    adView.storeView!!.visibility = View.INVISIBLE
                } else {
                    adView.storeView!!.visibility = View.VISIBLE
                    (adView.storeView as TextView).text = nativeAd.store
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            if (adView.starRatingView != null) {
                if (nativeAd.starRating == null) {
                    adView.starRatingView!!.visibility = View.GONE
                } else {
                    (adView.starRatingView as RatingBar).rating = nativeAd.starRating!!.toFloat()
                    adView.starRatingView!!.visibility = View.VISIBLE
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            if (adView.advertiserView != null) {
                if (nativeAd.advertiser == null) {
                    adView.advertiserView!!.visibility = View.INVISIBLE
                } else {
                    (adView.advertiserView as TextView).text = nativeAd.advertiser
                    adView.advertiserView!!.visibility = View.VISIBLE
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        adView.setNativeAd(nativeAd)
    }


    private fun addLoadingLayout(viewGroup: ViewGroup) {
        val view = AdLoadingViewBinding
            .inflate(LayoutInflater.from(viewGroup.context))
            .root

        viewGroup.removeAllViews()
        viewGroup.addView(view, ViewGroup.LayoutParams(-1, -1))
        view.requestLayout()
    }

    fun setEnableNative(isEnable: Boolean) {
        if (!isEnable) {
            try {
                nativeWithViewGroup.forEach { (_, viewGroup) ->
                    viewGroup?.removeAllViews()
                    viewGroup?.isVisible = false
                }
                nativesCollapsiblePopupWindow.forEach { t, popup ->
                    popup.dismiss()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                FirebaseCrashlytics.getInstance().recordException(e)
            }
        }
    }

    private fun setNativeCallback(space: String, callback: TAdCallback?) {
        nativesCallback.set(space, callback)
    }

    private fun getNativeCallback(space: String): TAdCallback? {
        return nativesCallback.get(space)
    }
}