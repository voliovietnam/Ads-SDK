package com.admob.ads.rewarded

import android.os.CountDownTimer
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.admob.AdFormat
import com.admob.AdType
import com.admob.TAdCallback
import com.admob.ads.AdsSDK
import com.admob.ads.R
import com.admob.getAppCompatActivityOnTop
import com.admob.getPaidTrackingBundle
import com.admob.isEnable
import com.admob.isNetworkAvailable
import com.admob.onNextActionWhenResume
import com.admob.ui.dialogs.DialogShowLoadingAds
import com.admob.ui.dialogs.DialogShowLoadingRewardAds
import com.admob.waitActivityResumed
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback


object AdmobRewarded {


    private var timer: CountDownTimer? = null

    /**
     * @param activity: Show on this activity
     * @param callBack
     * @param onUserEarnedReward
     * @param onFailureUserNotEarn
     */
    fun show(
        activity: AppCompatActivity,
        space: String,
        timeout: Long = 10_000L,
        showLoadingReward: Boolean = true,
        resLoadingAds: Int = R.layout.dialog_loading_inter,
        callBack: TAdCallback? = null,
        onFailureUserNotEarn: () -> Unit = {},
        onUserEarnedReward: () -> Unit
    ) {

        val adChild = AdsSDK.getAdChild(space) ?: return
        val adUnitId = adChild.adsId

        if (!AdsSDK.isEnableRewarded) {
            onUserEarnedReward.invoke()
            return
        }

        if (!AdsSDK.app.isNetworkAvailable()) {
            onFailureUserNotEarn.invoke()
            return
        }

        if (AdsSDK.isPremium || (adChild.adsType != AdFormat.Reward) || !adChild.isEnable()
        ) {
            onUserEarnedReward.invoke()
            return
        }


        var dialog : DialogShowLoadingRewardAds? = null

        if (showLoadingReward) {
            dialog = DialogShowLoadingRewardAds(activity,resLoadingAds).apply { show() }
        }


        AdsSDK.adCallback.onAdStartLoading(adUnitId, AdType.Rewarded)
        callBack?.onAdStartLoading(adUnitId, AdType.Native)

        var loadedReward: RewardedAd? = null

        RewardedAd.load(
            AdsSDK.app,
            adUnitId,
            AdsSDK.defaultAdRequest(),
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e("ThoNH-1", "onAdFailedToLoad")
                    super.onAdFailedToLoad(error)
                    AdsSDK.adCallback.onAdFailedToLoad(adUnitId, AdType.Rewarded, error)
                    callBack?.onAdFailedToLoad(adUnitId, AdType.Rewarded, error)
                    onFailureUserNotEarn.invoke()
                    dialog?.dismiss()
                    timer?.cancel()
                }

                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    Log.e("ThoNH-1", "onAdLoaded")
                    super.onAdLoaded(rewardedAd)
                    AdsSDK.adCallback.onAdLoaded(adUnitId, AdType.Rewarded)
                    callBack?.onAdLoaded(adUnitId, AdType.Rewarded)

                    rewardedAd.setOnPaidEventListener { adValue ->
                        val bundle = getPaidTrackingBundle(
                            adValue,
                            adUnitId,
                            "Rewarded",
                            rewardedAd.responseInfo
                        )
                        AdsSDK.adCallback.onPaidValueListener(bundle)
                        callBack?.onPaidValueListener(bundle)
                    }

                    rewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdClicked() {
                            Log.e("ThoNH-1", "onAdClicked")
                            super.onAdClicked()
                            AdsSDK.adCallback.onAdClicked(adUnitId, AdType.Rewarded)
                            callBack?.onAdClicked(adUnitId, AdType.Rewarded)
                        }

                        override fun onAdDismissedFullScreenContent() {
                            Log.e("ThoNH-1", "onAdDismissedFullScreenContent")
                            super.onAdDismissedFullScreenContent()
                            AdsSDK.adCallback.onAdDismissedFullScreenContent(
                                adUnitId,
                                AdType.Rewarded
                            )
                            callBack?.onAdDismissedFullScreenContent(adUnitId, AdType.Rewarded)
                        }

                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            Log.e("ThoNH-1", "onAdFailedToShowFullScreenContent")
                            super.onAdFailedToShowFullScreenContent(error)
                            AdsSDK.adCallback.onAdFailedToShowFullScreenContent(
                                adUnitId,
                                error.message,
                                AdType.Rewarded
                            )
                            callBack?.onAdFailedToShowFullScreenContent(
                                adUnitId,
                                error.message,
                                AdType.Rewarded
                            )
                            onFailureUserNotEarn.invoke()
                            dialog?.dismiss()
                            timer?.cancel()
                        }

                        override fun onAdImpression() {
                            Log.e("ThoNH-1", "onAdImpression")
                            super.onAdImpression()
                            AdsSDK.adCallback.onAdImpression(adUnitId, AdType.Rewarded)
                            callBack?.onAdImpression(adUnitId, AdType.Rewarded)
                        }

                        override fun onAdShowedFullScreenContent() {
                            Log.e("ThoNH-1", "onAdShowedFullScreenContent")
                            super.onAdShowedFullScreenContent()
                            AdsSDK.adCallback.onAdShowedFullScreenContent(adUnitId, AdType.Rewarded)
                            callBack?.onAdShowedFullScreenContent(adUnitId, AdType.Rewarded)
                        }
                    }

                    loadedReward = rewardedAd
                }
            }
        )


        timer?.cancel()
        timer = object : CountDownTimer(timeout, 500) {
            override fun onTick(millisUntilFinished: Long) {

                if (!AdsSDK.isEnableRewarded) {
                    timer?.cancel()
                    onUserEarnedReward.invoke()
                    return
                }

                loadedReward?.let { reward ->
                    timer?.cancel()
                    activity.waitActivityResumed {
                        dialog?.dismiss()
                        reward.show(activity) { _ ->
                            Log.e("ThoNH-1", "onUserEarnedReward")
                            onUserEarnedReward.invoke()
                        }
                    }

                }
            }

            override fun onFinish() {
                timer?.cancel()
                onFailureUserNotEarn.invoke()
                dialog?.dismiss()
            }
        }.start()
    }

}