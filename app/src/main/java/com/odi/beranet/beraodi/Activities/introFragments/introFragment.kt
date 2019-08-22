package com.odi.beranet.beraodi.Activities.introFragments

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast

import com.odi.beranet.beraodi.R
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.lang.Exception


private const val IMAGE_PARAM = "param1"


class introFragment : Fragment() {

    private var imagePath: String? = null
    private lateinit var imageView:ImageView
    private lateinit var slideProgressBar: RelativeLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            imagePath = it.getString(IMAGE_PARAM)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_intro, container, false)
        imageView = view.findViewById(R.id.imageView)
        slideProgressBar = view.findViewById(R.id.slideProgressBar)

        val newListener = object: Callback{
            override fun onSuccess() {
                slideProgressBar.visibility = View.GONE
            }

            override fun onError(e: Exception?) {
                Toast.makeText(activity!!.applicationContext, "Fotoğraf yüklenirken bir hata oldu", Toast.LENGTH_LONG)
                    .show()
                slideProgressBar.visibility = View.GONE
            }

        }
        Picasso.get().load(imagePath).into(imageView, newListener)
        return view
    }

    companion object {
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String) =
            introFragment().apply {
                arguments = Bundle().apply {
                    putString(IMAGE_PARAM, param1)
                }
            }
    }
}
