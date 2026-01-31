package app.counter.controller.caba.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import app.counter.controller.caba.R;
import app.counter.controller.caba.model.CounterModel;

public class DialogAddEditCounter extends DialogFragment {
    public interface Callback {
        void onCounterSaved(CounterModel counter, boolean isEdit);
    }

    private CounterModel counter;
    private Callback callback;
    private boolean isEdit;

    public DialogAddEditCounter(CounterModel counter, Callback callback) {
        this.counter = counter;
        this.callback = callback;
        this.isEdit = counter != null;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Context ctx = requireContext();
        View view = LayoutInflater.from(ctx).inflate(R.layout.dialog_add_edit_counter, null);
        EditText etLabel = view.findViewById(R.id.etLabel);
        EditText etValue = view.findViewById(R.id.etValue);
        EditText etStep = view.findViewById(R.id.etStep);
        EditText etTarget = view.findViewById(R.id.etTarget);
        EditText etNotes = view.findViewById(R.id.etNotes);

        if (isEdit && counter != null) {
            etLabel.setText(counter.getLabel());
            etValue.setText(String.valueOf(counter.getValue()));
            etStep.setText(String.valueOf(counter.getStep()));
            etTarget.setText(String.valueOf(counter.getTarget()));
            etNotes.setText(counter.getNotes());
        }

        return new AlertDialog.Builder(ctx)
                .setTitle(isEdit ? "Edit Counter" : "Tambah Counter")
                .setView(view)
                .setPositiveButton("Simpan", (d, w) -> {
                    String label = etLabel.getText().toString().trim();
                    String valueStr = etValue.getText().toString().trim();
                    String stepStr = etStep.getText().toString().trim();
                    String targetStr = etTarget.getText().toString().trim();
                    String notes = etNotes.getText().toString().trim();
                    if (TextUtils.isEmpty(label)) {
                        Toast.makeText(ctx, "Nama wajib diisi", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int value = valueStr.isEmpty() ? 0 : Integer.parseInt(valueStr);
                    int step = stepStr.isEmpty() ? 1 : Integer.parseInt(stepStr);
                    int target = targetStr.isEmpty() ? 0 : Integer.parseInt(targetStr);
                    if (counter == null) counter = new CounterModel();
                    counter.setLabel(label);
                    counter.setValue(value);
                    counter.setStep(step);
                    counter.setTarget(target);
                    counter.setNotes(notes);
                    if (callback != null) callback.onCounterSaved(counter, isEdit);
                })
                .setNegativeButton("Batal", null)
                .create();
    }
}
