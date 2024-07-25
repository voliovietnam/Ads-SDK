package com.admob.app_update

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.IntentSender
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.admob.ads.AdsSDK
import com.admob.getAppCompatActivityOnTop
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings

object Update {
    private val TAG = "Update"
    fun checkShowUpdate() {


        val remoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 0
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val versionRemote = remoteConfig.getLong("version_force_update")
                var currentCode = 1000
                Log.e(TAG, "checkShowUpdate: " + versionRemote)
                kotlin.runCatching {
                    val manager = AdsSDK.app.packageManager
                    val info = manager.getPackageInfo(
                        AdsSDK.app.packageName, PackageManager.GET_ACTIVITIES
                    )
                    currentCode = info.versionCode
                }
                if (currentCode <= versionRemote) {
                    Log.e(TAG, "checkShowUpdate: currentCode <= versionRemote")

                    val appUpdateManager = AppUpdateManagerFactory.create(AdsSDK.app)
                    val appUpdateInfoTask = appUpdateManager.appUpdateInfo
                    appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
                        // This example applies an flexible update. To apply a immediate update
                        // instead, pass in AppUpdateType.IMMEDIATE

                        if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                            // Request the update
                            try {
                                val activity =
                                    AdsSDK.activities.lastOrNull() ?: return@addOnSuccessListener

                                appUpdateManager.startUpdateFlow(
                                    appUpdateInfo,
                                    activity,
                                    AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                                )

                                appUpdateManager.registerListener { state ->
                                    when (state.installStatus()) {
                                        InstallStatus.CANCELED -> {
                                            Log.e(TAG, " CANCELED")
                                        }

                                        InstallStatus.INSTALLED -> {
                                            Log.e(TAG, " INSTALLED")
                                        }

                                        InstallStatus.INSTALLING -> {
                                            Log.e(TAG, "INSTALLING")
                                        }
                                    }
                                }

                            } catch (_: IntentSender.SendIntentException) {
                            }
                        }
                    }.addOnFailureListener {
                        Log.e(TAG, "checkShowUpdate: " + "appUpdateInfoTask- fail " + it.message)
                    }

                }
            }
        }
    }

    fun checkShowUpdateForResult(activity: AppCompatActivity) {

        val remoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 0
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val versionRemote = remoteConfig.getLong("version_force_update")
                var currentCode = 1000
                Log.e(TAG, "checkShowUpdate: " + versionRemote)
                kotlin.runCatching {
                    val manager = AdsSDK.app.packageManager
                    val info = manager.getPackageInfo(
                        AdsSDK.app.packageName, PackageManager.GET_ACTIVITIES
                    )
                    currentCode = info.versionCode
                }
                if (currentCode <= versionRemote) {
                    Log.e(TAG, "checkShowUpdate: currentCode <= versionRemote")

                    val appUpdateManager = AppUpdateManagerFactory.create(AdsSDK.app)
                    val appUpdateInfoTask = appUpdateManager.appUpdateInfo
                    appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
                        // This example applies an flexible update. To apply a immediate update
                        // instead, pass in AppUpdateType.IMMEDIATE
                        Log.e(
                            TAG,
                            "appUpdateInfo.updateAvailability(): " + appUpdateInfo.updateAvailability()
                        )
                        if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                            // Request the update
                            try {
                                val resultLauncher =
                                    activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result: ActivityResult ->
                                        // handle callback
                                        //RESULT_OK: Người dùng đã chấp nhận bản cập nhật. Đối với các bản cập nhật ngay lập tức, bạn có thể không nhận được lệnh gọi lại này vì bản cập nhật phải đã hoàn tất vào thời điểm quyền kiểm soát được trả lại cho ứng dụng của bạn.
                                        //RESULT_CANCELED:Người dùng đã từ chối hoặc hủy cập nhật.
                                        //ActivityResult.RESULT_IN_APP_UPDATE_FAILED: Một số lỗi khác đã ngăn cản người dùng cung cấp sự đồng ý hoặc ngăn cản quá trình cập nhật.
                                        if (result.resultCode == RESULT_OK) {
                                            Log.e(
                                                TAG,
                                                "checkShowUpdateForResult: + result.resultCode == RESULT_OK "
                                            )
                                        }
                                        if (result.resultCode == RESULT_CANCELED) {
                                            Log.e(
                                                TAG,
                                                "checkShowUpdateForResult: + result.resultCode == RESULT_CANCELED "
                                            )

                                        }
                                        if (result.resultCode == com.google.android.play.core.install.model.ActivityResult.RESULT_IN_APP_UPDATE_FAILED) {
                                            Log.e(
                                                TAG,
                                                "checkShowUpdateForResult: + result.resultCode == RESULT_IN_APP_UPDATE_FAILED "
                                            )
                                        }

                                    }
                                appUpdateManager.startUpdateFlowForResult(
                                    appUpdateInfo,
                                    resultLauncher,
                                    AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                                )
                            } catch (_: IntentSender.SendIntentException) {
                            }
                        }
                    }.addOnFailureListener {
                        Log.e(TAG, "checkShowUpdate: " + "appUpdateInfoTask- fail " + it.message)
                    }

                }
            }
        }
    }


    fun checkUpdateAfterShowAd(nextAction: () -> Unit) {
        nextAction.invoke()
        checkShowUpdate()
    }

}