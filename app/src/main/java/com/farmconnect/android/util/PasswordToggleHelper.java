package com.farmconnect.android.util;

import android.text.InputType;
import android.view.MotionEvent;
import android.widget.EditText;

import com.farmconnect.android.R;

/**
 * Adds a show/hide (eye) icon to a plain EditText password field.
 *
 * Login's password field uses a Material TextInputLayout, which already
 * has a built-in app:endIconMode="password_toggle" for this. Register's
 * password fields are plain EditTexts (see RegisterField style), so this
 * gives them the same show/hide behavior via a compound drawable + touch
 * listener instead, without changing their existing look/background.
 */
public final class PasswordToggleHelper {

    private PasswordToggleHelper() {
    }

    public static void attach(EditText editText) {
        setDrawable(editText, false);

        editText.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                android.graphics.drawable.Drawable end = editText.getCompoundDrawables()[2];
                if (end != null) {
                    int touchX = (int) event.getX();
                    int iconStart = editText.getWidth() - editText.getPaddingEnd() - end.getIntrinsicWidth();
                    if (touchX >= iconStart) {
                        boolean currentlyHidden = isPasswordHidden(editText);
                        setDrawable(editText, currentlyHidden);
                        editText.setInputType(currentlyHidden
                                ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        editText.setSelection(editText.getText().length());
                        return true;
                    }
                }
            }
            return false;
        });
    }

    private static boolean isPasswordHidden(EditText editText) {
        int variation = editText.getInputType() & InputType.TYPE_MASK_VARIATION;
        return variation == InputType.TYPE_TEXT_VARIATION_PASSWORD;
    }

    private static void setDrawable(EditText editText, boolean showingPassword) {
        editText.setCompoundDrawablesWithIntrinsicBounds(
                0, 0, showingPassword ? R.drawable.ic_eye_off : R.drawable.ic_eye, 0);
        editText.setCompoundDrawablePadding(12);
    }
}
