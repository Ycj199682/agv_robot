package com.reeman.agv.utils

import android.content.Context
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation
import android.widget.EditText
import androidx.appcompat.widget.AppCompatImageButton
import com.reeman.agv.R
import com.warkiz.widget.IndicatorSeekBar

object ViewUtils {

     fun getInputContentToFloat(context: Context, editText: EditText, min: Float, max: Float): Float {
        val inputStr = editText.text.toString()
        if (TextUtils.isEmpty(inputStr)) {
            editText.error =
                context.getString(R.string.text_please_input_within_range, min.toString(), max.toString())
            return Float.MAX_VALUE
        }
        return try {
            val inputFloat = inputStr.toFloat()
            if (inputFloat < min || inputFloat > max) {
                editText.error = context.getString(
                    R.string.text_please_input_within_range,
                    min.toString(),
                    max.toString()
                )
                return Float.MAX_VALUE
            }
            inputFloat
        } catch (e: NumberFormatException) {
            editText.error =
                context.getString(R.string.text_please_input_within_range, min.toString(), max.toString())
            Float.MAX_VALUE
        }
    }


    fun onIntValueChange(v: View, isAdd: Boolean): Int {
        if (v is IndicatorSeekBar) return v.progress
        val parent = v.parent as ViewGroup
        val seekBar = parent.getChildAt(1) as IndicatorSeekBar
        var progress = seekBar.progressFloat
        progress = if (isAdd) progress + 1 else progress - 1
        seekBar.setProgress(progress)
        return seekBar.progress
    }

    fun onFloatValueChange(v: View, isAdd: Boolean): Float {
        if (v is IndicatorSeekBar) return v.progressFloat
        val parent = v.parent as ViewGroup
        val seekBar = parent.getChildAt(1) as IndicatorSeekBar
        var progress = seekBar.progressFloat
        progress = (if (isAdd) progress + 0.1 else progress - 0.1).toFloat()
        seekBar.setProgress(progress)
        return seekBar.progressFloat
    }

    /**
     * 从右渐入
     * @return
     */
    fun getFadeInFromRight(): AnimationSet {
        val fadeAnimation = AnimationSet(true)
        val translateAnimationIn = TranslateAnimation(
            Animation.RELATIVE_TO_PARENT, 1f,
            Animation.RELATIVE_TO_PARENT, 0f,
            Animation.RELATIVE_TO_SELF, 0f,
            Animation.RELATIVE_TO_SELF, 0f
        )
        val alphaAnimationIn = AlphaAnimation(0f, 1f)
        fadeAnimation.addAnimation(translateAnimationIn)
        fadeAnimation.addAnimation(alphaAnimationIn)
        fadeAnimation.duration = 500
        return fadeAnimation
    }

    /**
     * 渐出到左
     * @return
     */
    fun getFadeOutToLeft(): AnimationSet {
        val fadeAnimation = AnimationSet(true)
        val translateAnimationOut = TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 0f,
            Animation.RELATIVE_TO_PARENT, -1f,
            Animation.RELATIVE_TO_SELF, 0f,
            Animation.RELATIVE_TO_SELF, 0f
        )
        val alphaAnimationOut = AlphaAnimation(1f, 0f)
        fadeAnimation.addAnimation(translateAnimationOut)
        fadeAnimation.addAnimation(alphaAnimationOut)
        fadeAnimation.duration = 500
        return fadeAnimation
    }

    /**
     * 从下渐入
     * @return
     */
    fun getFadeInFromBottom(): AnimationSet {
        val fadeAnimation = AnimationSet(true)
        val translateAnimationIn = TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 0f,
            Animation.RELATIVE_TO_SELF, 0f,
            Animation.RELATIVE_TO_PARENT, 1f,
            Animation.RELATIVE_TO_SELF, 0f
        )
        val alphaAnimationIn = AlphaAnimation(0f, 1f)
        fadeAnimation.addAnimation(translateAnimationIn)
        fadeAnimation.addAnimation(alphaAnimationIn)
        fadeAnimation.duration = 500
        return fadeAnimation
    }

    /**
     * 渐出到上
     * @return
     */
    fun getFadeOutToTop(): AnimationSet {
        val fadeAnimation = AnimationSet(true)
        val translateAnimationOut = TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 0f,
            Animation.RELATIVE_TO_SELF, 0f,
            Animation.RELATIVE_TO_SELF, 0f,
            Animation.RELATIVE_TO_PARENT, -1f
        )
        val alphaAnimationOut = AlphaAnimation(1f, 0f)
        fadeAnimation.addAnimation(translateAnimationOut)
        fadeAnimation.addAnimation(alphaAnimationOut)
        fadeAnimation.duration = 500
        return fadeAnimation
    }

    /**
     * 重设view的height与width相等
     * @param viewGroup
     * @param ids
     */
    fun resetViewHeight(viewGroup: ViewGroup, vararg ids: Int) {
        for (id in ids) {
            val imageButton = viewGroup.findViewById<AppCompatImageButton>(id)
            imageButton.post {
                val layoutParams = imageButton.layoutParams
                layoutParams.height = imageButton.width
                imageButton.layoutParams = layoutParams
            }
        }
    }

    /**
     * 切换子view,带渐入渐出动画
     * @param parentView
     * @param newView
     */
    fun showViewWithAnimation(parentView: ViewGroup, newView: View) {
        val child = parentView.getChildAt(0)
        if (child == null) {
            parentView.removeAllViews()
            parentView.addView(newView)
        } else {
            child.startAnimation(getFadeOutToTop())
            parentView.removeView(child)
            parentView.addView(newView)
            newView.startAnimation(getFadeInFromBottom())
        }
    }
}