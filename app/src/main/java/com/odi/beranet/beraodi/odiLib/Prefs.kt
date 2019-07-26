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

    val prefs: SharedPreferences = context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)

    private var user_id:String
        get() = prefs.getString(user_id_FileName, "")
        set(value) = prefs.edit().putString(user_id_FileName, value).apply()


    /////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////
    // <user information>


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