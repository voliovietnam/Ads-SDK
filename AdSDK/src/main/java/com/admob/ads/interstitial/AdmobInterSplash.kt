package com.admob.ads.interstitial

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import com.admob.AdFormat
import com.admob.AdType
import com.admob.TAdCallback
import com.admob.ads.AdsSDK
import com.admob.ads.R
import com.admob.app_update.Update
import com.admob.getActivityOnTop
import com.admob.getAppCompatActivityOnTop
import com.admob.isEnable
import com.admob.isNetworkAvailable
import com.admob.onNextActionWhenResume
import com.admob.waitActivityResumed
import com.google.android.gms.ads.LoadAdError


object AdmobInterSplash {

    private var timer: CountDownTimer? = null

    /**
     * @param adUnitId: adUnit
     * @param timeout: timeout to wait ad show
     * @param nextAction
     */
    fun show(
        space: String,
        timeout: Long = 15_000,
        showLoadingInter: Boolean = true,
        resLoadingAds: Int = R.layout.dialog_loading_inter,
        showAdCallback: TAdCallback? = null,
        nextAction: () -> Unit
    ) {

        fun checkUpdateAndNext(){
            Update.checkUpdateAfterShowAd(nextAction)
        }

        val adChild = AdsSDK.getAdChild(space)

        if (adChild == null){
            checkUpdateAndNext()
            return
        }

        if (!AdsSDK.isEnableInter || AdsSDK.isPremium || (adChild.adsType != AdFormat.Interstitial) || !AdsSDK.app.isNetworkAvailable() || !adChild.isEnable()) {
            checkUpdateAndNext()
            return
        }

        val callback = object : TAdCallback {
            override fun onAdFailedToLoad(adUnit: String, adType: AdType, error: LoadAdError) {
                super.onAdFailedToLoad(adUnit, adType, error)
                Log.e("DucLH--inter", "Failload: $error $adUnit ${adType.name}")
                timer?.cancel()
                onNextActionWhenResume(::checkUpdateAndNext)
            }

            override fun onAdLoaded(adUnit: String, adType: AdType) {
                super.onAdLoaded(adUnit, adType)
                Log.e("DucLH--inter", "onAdLoaded: $adUnit ${adType.name}")

            }

            override fun onAdFailedToShowFullScreenContent(
                error: String,
                adUnit: String,
                adType: AdType
            ) {
                super.onAdFailedToShowFullScreenContent(error, adUnit, adType)
                Log.e("DucLH--inter", "FailShow: $error $adUnit ${adType.name}")
                timer?.cancel()
                onNextActionWhenResume(::checkUpdateAndNext)
            }
        }

        AdmobInter.load(adChild.spaceName, callback)

        timer?.cancel()
        timer = object : CountDownTimer(timeout, 1000) {
            override fun onTick(millisUntilFinished: Long) {

                if (!AdsSDK.isEnableInter) {
                    timer?.cancel()
                    checkUpdateAndNext()
                    return
                }

                if (AdmobInter.checkShowInterCondition(adChild.spaceName, false)) {
                    timer?.cancel()
                    onNextActionWhenResume {
                        AdsSDK.getAppCompatActivityOnTop()?.waitActivityResumed {
                            AdmobInter.show(
                                space = space,
                                showLoadingInter = showLoadingInter,
                                forceShow = true,
                                nextActionBeforeDismiss = false,
                                loadAfterDismiss = false,
                                loadIfNotAvailable = false,
                                resLoadingAds = resLoadingAds,
                                callback = showAdCallback,
                                nextAction = ::checkUpdateAndNext
                            )
                        }
                    }
                }
            }

            override fun onFinish() {
                timer?.cancel()
                onNextActionWhenResume(::checkUpdateAndNext)
            }
        }.start()
    }

}
