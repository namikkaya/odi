package com.odi.beranet.beraodi.odiLib

import android.os.AsyncTask

abstract class BaseAsyncTask(private val listener:odiInterface) : AsyncTask<Void, Void, String?>(){
    override fun onPreExecute() {
        super.onPreExecute()
    }

    override fun onPostExecute(result: String?) {
        super.onPostExecute(result)
        if (null != result) {
            listener.onError(result)
        }else {
            listener.onCompleted()
        }
    }
}

class LoadMediaTask(listener:odiInterface):BaseAsyncTask(listener) {
    override fun doInBackground(vararg params: Void?): String? {
        return "boş dön"
    }
}