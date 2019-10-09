package com.odi.beranet.beraodi.odiLib

import android.content.Context
import android.content.SharedPreferences
import com.odi.beranet.beraodi.ApplicationClass

class Prefs {
    companion object {
        var sharedData:sharedObject?? = sharedObject(ApplicationClass.applicationContext())
    }
}


class sharedObject(context: Context){
    private val PREFS_FILENAME = "user"

    private val user_id_FileName = "user_id"
    private val user_first_look = "user_first_look"
    private val user_cameratooltip_look = "user_cameratooltip_look"
    private val user_previewtooltip_look = "user_previewtooltip_look"

    val prefs: SharedPreferences = context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)

    private var user_id:String
        get() = prefs.getString(user_id_FileName, "")
        set(value) = prefs.edit().putString(user_id_FileName, value).apply()

    private var userFirstLook:Boolean
        get() = prefs.getBoolean(user_first_look, false)
        set(value) = prefs.edit().putBoolean(user_first_look, value).apply()


    private var userFirstCameraToolTip:Boolean
        get() = prefs.getBoolean(user_cameratooltip_look, false)
        set(value) = prefs.edit().putBoolean(user_cameratooltip_look, value).apply()

    private var userFirstPreviewToolTip:Boolean
        get() = prefs.getBoolean(user_previewtooltip_look, false)
        set(value) = prefs.edit().putBoolean(user_previewtooltip_look, value).apply()


    /////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////
    // <user information>
    fun getFirstLookPreviewTooltip():Boolean? {
        return userFirstPreviewToolTip
    }

    fun setFirstLookPreviewTooltip(status:Boolean) {
        userFirstPreviewToolTip = true
    }



    fun getFirstLookCameraTooltip():Boolean? {
        return userFirstCameraToolTip
    }

    fun setFirstLookCameraTooltip(status:Boolean) {
        userFirstCameraToolTip = true
    }

    /**
     * video slayt ilk açıldığında 1 kez gösterilmesi için tutulur
     */
    fun getFirstLook():Boolean? {
        return userFirstLook
    }

    /**
     * true yapıldığında video slide bir daha gösterilmez
     */
    fun setFirstLook(status:Boolean) {
        userFirstLook = status
    }

    /**
     * @param userId : kullanıcı id si set eder
     */
    fun setUserId(userId:String?){
        user_id = userId!!
    }

    /**
     * @return userId: Kullanıcı id si döndürür.
     */
    fun getUserId():String?{
        var myUserId:String? = user_id
        return myUserId
    }

    /**
     * Kullanıcı bilgisini sıfılar
     */
    fun clearUserData(){
        prefs.edit().clear().apply()
    }

    /**
     * @return kullanıcı bilgisi mevcut ise true
     */
    fun getStatus():Boolean{
        return !(user_id == null || user_id == "")
    }

    //-------------------------------------------------------------------

}