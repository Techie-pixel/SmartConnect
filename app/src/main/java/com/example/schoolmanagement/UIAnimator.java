package com.example.schoolmanagement;

import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class UIAnimator {

    // Apply slide-up animation to entire layout (used in onCreate)
    public static void animateLayout(View view, int duration) {
        view.setAlpha(0f);
        view.setTranslationY(50f);
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(duration)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .start();
    }

    // Animate toolbar with slide-down effect
    public static void animateToolbar(View toolbar, int delay) {
        toolbar.setAlpha(0f);
        toolbar.setTranslationY(-50f);
        toolbar.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .setStartDelay(delay)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .start();
    }

    // Animate buttons with scale-up effect
    public static void animateButton(Button button, int delay) {
        button.setScaleX(0f);
        button.setScaleY(0f);
        button.setAlpha(0f);
        
        button.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(500)
            .setStartDelay(delay)
            .setInterpolator(new OvershootInterpolator())
            .start();
    }

    // Animate TextViews with fade-in and slide from right
    public static void animateTextView(TextView textView, int delay) {
        textView.setAlpha(0f);
        textView.setTranslationX(150f);
        
        textView.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(500)
            .setStartDelay(delay)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .start();
    }

    // Animate EditText with slide from right effect
    public static void animateEditText(EditText editText, int delay) {
        editText.setAlpha(0f);
        editText.setTranslationX(150f);
        
        editText.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(500)
            .setStartDelay(delay)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .start();
    }

    // Animate Views with scale-up effect
    public static void animateImageView(View imageView, int delay) {
        imageView.setScaleX(0f);
        imageView.setScaleY(0f);
        imageView.setAlpha(0f);
        
        imageView.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(600)
            .setStartDelay(delay)
            .setInterpolator(new OvershootInterpolator())
            .start();
    }

    // Animate LinearLayout items sequentially
    public static void animateLinearLayoutItems(LinearLayout linearLayout, int startDelay, int itemDelay) {
        for (int i = 0; i < linearLayout.getChildCount(); i++) {
            View child = linearLayout.getChildAt(i);
            child.setAlpha(0f);
            child.setTranslationY(30f);
            
            child.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(startDelay + (i * itemDelay))
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
        }
    }

    // Pulse animation for icons (continuous)
    public static void startPulseAnimation(final View view) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1.0f, 1.1f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1.0f, 1.1f, 1.0f);
        
        scaleX.setDuration(1500);
        scaleY.setDuration(1500);
        scaleX.setRepeatCount(ObjectAnimator.INFINITE);
        scaleY.setRepeatCount(ObjectAnimator.INFINITE);
        
        scaleX.start();
        scaleY.start();
    }

    // Ripple effect on click
    public static void animateClick(View view) {
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction(() -> view.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(100)
                .start());
    }

    // Animate dashboard cards with scale + slide-up effect
    public static void animateCard(View card, int delay) {
        card.setAlpha(0f);
        card.setScaleX(0.8f);
        card.setScaleY(0.8f);
        card.setTranslationY(40f);

        card.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .translationY(0f)
            .setDuration(500)
            .setStartDelay(delay)
            .setInterpolator(new OvershootInterpolator(1.2f))
            .start();
    }

    // Comprehensive animation for all UI elements in an activity
    public static void animateActivity(View toolbar, View[] buttons, View[] textViews, View[] imageViews) {
        // Animate toolbar first
        if (toolbar != null) {
            animateToolbar(toolbar, 200);
        }

        // Animate buttons
        if (buttons != null) {
            for (int i = 0; i < buttons.length; i++) {
                if (buttons[i] instanceof Button) {
                    animateButton((Button) buttons[i], 400 + (i * 100));
                } else {
                    animateImageView((View) buttons[i], 400 + (i * 100));
                }
            }
        }

        // Animate text views
        if (textViews != null) {
            for (int i = 0; i < textViews.length; i++) {
                animateTextView((TextView) textViews[i], 300 + (i * 100));
            }
        }

        // Animate image views
        if (imageViews != null) {
            for (int i = 0; i < imageViews.length; i++) {
                animateImageView((View) imageViews[i], 500 + (i * 150));
            }
        }
    }
}
