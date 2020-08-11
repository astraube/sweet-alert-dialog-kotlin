package com.github.astraube

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationSet
import android.view.animation.Transformation
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.github.astraube.OptAnimationLoader.loadAnimation
import com.github.astraube.extensions.isNotNullOrBlank
import com.github.astraube.extensions.visible

class SweetAlertDialog @JvmOverloads constructor(
    context: Context,
    alertType: Int = NORMAL_TYPE
) : Dialog(context, R.style.alert_dialog), View.OnClickListener {
    private var mDialogView: View? = null
    private val mModalInAnim: AnimationSet?
    private val mModalOutAnim: AnimationSet?
    private val mOverlayOutAnim: Animation
    private val mErrorInAnim: Animation?
    private val mErrorXInAnim: AnimationSet?
    private val mSuccessLayoutAnimSet: AnimationSet?
    private val mSuccessBowAnim: Animation?
    private var mTitleTextView: TextView? = null
    private var mContentTextView: TextView? = null
    private var mErrorFrame: FrameLayout? = null
    private var mSuccessFrame: FrameLayout? = null
    private var mProgressFrame: FrameLayout? = null
    private var mSuccessTick: SuccessTickView? = null
    private var mErrorX: ImageView? = null
    private var mSuccessLeftMask: View? = null
    private var mSuccessRightMask: View? = null
    private var mCustomImgDrawable: Drawable? = null
    private var mCustomImage: ImageView? = null
    private var mConfirmButton: Button? = null
    private var mCancelButton: Button? = null
    private var mWarningFrame: FrameLayout? = null
    private var mCancelClickListener: OnSweetListener? = null
    private var mConfirmClickListener: OnSweetListener? = null
    private var mCloseFromCancel = false
    var titleText: String? = null
    var contentText: String? = null
    var isShowCancelButton = false
    var isShowContentText = false
    var cancelText: String? = null
    var confirmText: String? = null
    var alerType: Int
    val progressHelper: ProgressHelper


    interface OnSweetListener {
        fun onClick(dialog: SweetAlertDialog)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sweet_alert_dialog)
        mDialogView = window!!.decorView.findViewById(android.R.id.content)
        mTitleTextView = findViewById<View>(R.id.title_text) as TextView
        mContentTextView = findViewById<View>(R.id.content_text) as TextView
        mErrorFrame = findViewById<View>(R.id.error_frame) as FrameLayout
        mErrorX = mErrorFrame!!.findViewById<View>(R.id.error_x) as ImageView
        mSuccessFrame = findViewById<View>(R.id.success_frame) as FrameLayout
        mProgressFrame = findViewById<View>(R.id.progress_dialog) as FrameLayout
        mSuccessTick = mSuccessFrame!!.findViewById<View>(R.id.success_tick) as SuccessTickView
        mSuccessLeftMask = mSuccessFrame!!.findViewById(R.id.mask_left)
        mSuccessRightMask = mSuccessFrame!!.findViewById(R.id.mask_right)
        mCustomImage = findViewById<View>(R.id.custom_image) as ImageView
        mWarningFrame = findViewById<View>(R.id.warning_frame) as FrameLayout
        mConfirmButton = findViewById<View>(R.id.confirm_button) as Button
        mCancelButton = findViewById<View>(R.id.cancel_button) as Button
        progressHelper.progressWheel = findViewById<View>(R.id.progressWheel) as ProgressWheel
        mConfirmButton?.setOnClickListener(this)
        mCancelButton?.setOnClickListener(this)
        setTitleText(titleText)
        setContentText(contentText)
        setCancelText(cancelText)
        setConfirmText(confirmText)
        changeAlertType(alerType, true)
    }

    private fun restore() {
        mCustomImage!!.visibility = View.GONE
        mErrorFrame!!.visibility = View.GONE
        mSuccessFrame!!.visibility = View.GONE
        mWarningFrame!!.visibility = View.GONE
        mProgressFrame!!.visibility = View.GONE
        mConfirmButton!!.visibility = View.VISIBLE
        mConfirmButton!!.setBackgroundResource(R.drawable.sweet_blue_button_background)
        mErrorFrame!!.clearAnimation()
        mErrorX!!.clearAnimation()
        mSuccessTick!!.clearAnimation()
        mSuccessLeftMask!!.clearAnimation()
        mSuccessRightMask!!.clearAnimation()
    }

    private fun playAnimation() {
        if (alerType == ERROR_TYPE) {
            mErrorFrame!!.startAnimation(mErrorInAnim)
            mErrorX!!.startAnimation(mErrorXInAnim)
        } else if (alerType == SUCCESS_TYPE) {
            mSuccessTick!!.startTickAnim()
            mSuccessRightMask!!.startAnimation(mSuccessBowAnim)
        }
    }

    private fun changeAlertType(alertType: Int, fromCreate: Boolean) {
        alerType = alertType
        // call after created views
        if (mDialogView != null) {
            if (!fromCreate) {
                // restore all of views state before switching alert type
                restore()
            }
            when (alerType) {
                ERROR_TYPE -> mErrorFrame!!.visibility = View.VISIBLE
                SUCCESS_TYPE -> {
                    mSuccessFrame!!.visibility = View.VISIBLE
                    // initial rotate layout of success mask
                    mSuccessLeftMask!!.startAnimation(mSuccessLayoutAnimSet!!.animations[0])
                    mSuccessRightMask!!.startAnimation(mSuccessLayoutAnimSet.animations[1])
                }
                WARNING_TYPE -> {
                    mConfirmButton!!.setBackgroundResource(R.drawable.sweet_red_button_background)
                    mWarningFrame!!.visibility = View.VISIBLE
                }
                CUSTOM_IMAGE_TYPE -> setCustomImage(mCustomImgDrawable)
                PROGRESS_TYPE -> {
                    mProgressFrame!!.visibility = View.VISIBLE
                    mConfirmButton!!.visibility = View.GONE
                }
            }
            if (!fromCreate) {
                playAnimation()
            }
        }
    }

    fun changeAlertType(alertType: Int) {
        changeAlertType(alertType, false)
    }

    fun setTitleText(text: String?): SweetAlertDialog {
        titleText = text
        mTitleTextView?.let {
            if (text.isNotNullOrBlank()) {
                it.text = text
            }
        }
        return this
    }

    fun setCustomImage(drawable: Drawable?): SweetAlertDialog {
        mCustomImgDrawable = drawable
        mCustomImage?.let { imgView ->
            mCustomImgDrawable?.let {
                imgView.visibility = View.VISIBLE
                imgView.setImageDrawable(it)
            }
        }
        return this
    }

    fun setCustomImage(resourceId: Int): SweetAlertDialog {
        return setCustomImage(ContextCompat.getDrawable(context, resourceId))
    }

    fun setContentText(text: String?): SweetAlertDialog {
        contentText = text
        mContentTextView?.let {
            if (text.isNotNullOrBlank()) {
                showContentText(true)
                it.text = text
            }
        }
        return this
    }

    fun showCancelButton(isShow: Boolean): SweetAlertDialog {
        isShowCancelButton = isShow
        mCancelButton?.let {
            it.visible = isShow
        }
        return this
    }

    fun showContentText(isShow: Boolean): SweetAlertDialog {
        isShowContentText = isShow
        mContentTextView?.let {
            it.visible = isShow
        }
        return this
    }

    fun setCancelText(text: String?): SweetAlertDialog {
        cancelText = text
        mCancelButton?.let {
            if (text.isNotNullOrBlank()) {
                showCancelButton(true)
                it.text = text
            }
        }
        return this
    }

    fun setConfirmText(text: String?): SweetAlertDialog {
        confirmText = text
        mConfirmButton?.let {
            if (confirmText.isNotNullOrBlank())
                it.text = confirmText
        }
        return this
    }

    fun setCancelClickListener(listener: OnSweetListener?): SweetAlertDialog {
        mCancelClickListener = listener
        return this
    }

    fun setConfirmClickListener(listener: OnSweetListener?): SweetAlertDialog {
        mConfirmClickListener = listener
        return this
    }

    override fun onStart() {
        mDialogView!!.startAnimation(mModalInAnim)
        playAnimation()
    }

    /**
     * The real Dialog.cancel() will be invoked async-ly after the animation finishes.
     */
    override fun cancel() {
        dismissWithAnimation(true)
    }

    /**
     * The real Dialog.dismiss() will be invoked async-ly after the animation finishes.
     */
    fun dismissWithAnimation() {
        dismissWithAnimation(false)
    }

    private fun dismissWithAnimation(fromCancel: Boolean) {
        mCloseFromCancel = fromCancel
        mConfirmButton!!.startAnimation(mOverlayOutAnim)
        mDialogView!!.startAnimation(mModalOutAnim)
    }

    override fun onClick(v: View) {
        if (v.id == R.id.cancel_button) {
            if (mCancelClickListener != null) {
                mCancelClickListener?.onClick(this@SweetAlertDialog)
            } else {
                dismissWithAnimation()
            }
        } else if (v.id == R.id.confirm_button) {
            if (mConfirmClickListener != null) {
                mConfirmClickListener?.onClick(this@SweetAlertDialog)
            } else {
                dismissWithAnimation()
            }
        }
    }

    companion object {
        const val NORMAL_TYPE = 0
        const val ERROR_TYPE = 1
        const val SUCCESS_TYPE = 2
        const val WARNING_TYPE = 3
        const val CUSTOM_IMAGE_TYPE = 4
        const val PROGRESS_TYPE = 5
    }

    init {
        setCancelable(true)
        setCanceledOnTouchOutside(false)
        progressHelper = ProgressHelper(context)
        alerType = alertType
        mErrorInAnim = loadAnimation(getContext(), R.anim.sweet_error_frame_in)
        mErrorXInAnim = loadAnimation(getContext(), R.anim.sweet_error_x_in) as AnimationSet?
        // 2.3.x system don't support alpha-animation on layer-list drawable
        // remove it from animation set
        @SuppressLint("ObsoleteSdkInt")
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1) {
            val childAnims = mErrorXInAnim!!.animations
            var idx = 0
            while (idx < childAnims.size) {
                if (childAnims[idx] is AlphaAnimation) {
                    break
                }
                idx++
            }
            if (idx < childAnims.size) {
                childAnims.removeAt(idx)
            }
        }
        mSuccessBowAnim = loadAnimation(getContext(), R.anim.sweet_success_bow_roate)
        mSuccessLayoutAnimSet = loadAnimation(getContext(), R.anim.sweet_success_mask_layout) as AnimationSet
        mModalInAnim = loadAnimation(getContext(), R.anim.sweet_modal_in) as AnimationSet
        mModalOutAnim = loadAnimation(getContext(), R.anim.sweet_modal_out) as AnimationSet
        mModalOutAnim.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                mDialogView?.let {
                   it. visibility = View.GONE
                    it.post {
                        if (mCloseFromCancel) {
                            super@SweetAlertDialog.cancel()
                        } else {
                            super@SweetAlertDialog.dismiss()
                        }
                    }
                }
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
        // dialog overlay fade out
        mOverlayOutAnim = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                window?.let {
                    val wlp = it.attributes
                    wlp.alpha = 1 - interpolatedTime
                    it.attributes = wlp
                }
            }
        }
        mOverlayOutAnim.setDuration(120)
    }
}