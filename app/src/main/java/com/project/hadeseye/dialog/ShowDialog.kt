package com.project.hadeseye.dialog


import android.content.Context
import androidx.core.graphics.green
import com.developer.kalert.KAlertDialog
import com.project.hadeseye.R

class ShowDialog(private val context: Context) {

    var isTrue = false

    fun invalidDialog(title: String, message: String) {
        KAlertDialog(context, KAlertDialog.ERROR_TYPE, isTrue)
            .setTitleText(title)
            .setContentText(message)
            .setConfirmText("OK")
            .confirmButtonColor(R.color.red)
            .setConfirmClickListener { dialog ->
                dialog.dismissWithAnimation()
            }
            .show()
    }

    fun successDialog(title: String, message: String) {
        KAlertDialog(context, KAlertDialog.SUCCESS_TYPE, isTrue)
            .setTitleText(title)
            .setContentText(message)
            .setConfirmText("OK")
            .confirmButtonColor(R.color.green)
            .setConfirmClickListener { dialog ->
                dialog.dismissWithAnimation()
            }
            .show()
    }

    fun successDialog(title: String, message: String, button: String, onConfirm: Runnable?) {
        KAlertDialog(context, KAlertDialog.SUCCESS_TYPE, isTrue)
            .setTitleText(title)
            .setContentText(message)
            .setConfirmText(button)
            .confirmButtonColor(R.color.green)
            .setConfirmClickListener { dialog ->
                dialog.dismissWithAnimation()
                onConfirm?.run()
            }
            .show()
    }

    fun loadingDialog(title: String): KAlertDialog {
        val pDialog = KAlertDialog(context, KAlertDialog.PROGRESS_TYPE, isTrue)
        pDialog.setTitleText(title)
        pDialog.show()
        return pDialog
    }

    fun logoutDialog(onConfirm: Runnable?) {
        KAlertDialog(context, KAlertDialog.WARNING_TYPE, isTrue)
            .setTitleText("Logout Confirmation")
            .setContentText("Are you sure you want to log out of your account?")
            .setCancelText("No")
            .setConfirmText("Yes")
            .confirmButtonColor(R.color.green)
            .cancelButtonColor(R.color.gray)
            .setConfirmClickListener { dialog ->
                dialog.dismissWithAnimation()
                onConfirm?.run()
            }
            .setCancelClickListener { dialog ->
                dialog.dismissWithAnimation()
            }
            .show()
    }



}