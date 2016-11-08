package com.gooeybar.readycheck.listener;

import android.app.AlertDialog;
import android.content.res.Resources;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import com.gooeybar.readycheck.R;

public class DialogValidInputTextWatcher implements TextWatcher {

    private AlertDialog dialog;
    private Resources resources;
    private EditText[] otherEditTexts;

    public DialogValidInputTextWatcher(AlertDialog dialog, Resources resources, EditText... editTexts) {
        this.dialog = dialog;
        this.resources = resources;
        this.otherEditTexts = editTexts;
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        String value = charSequence.toString().trim();

        boolean shouldDisableButton = value.isEmpty() || value.contains("\\") || value.contains("/");

        for (EditText editText : otherEditTexts) {
            if (shouldDisableButton) {
                break;
            }
            String editTextValue = editText.getText().toString().trim();
            shouldDisableButton = editTextValue.isEmpty() || editTextValue.contains("\\") || editTextValue.contains("/");
        }
        if (shouldDisableButton) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        } else {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
        }
    }

    @Override
    public void afterTextChanged(Editable editable) {

    }
}
