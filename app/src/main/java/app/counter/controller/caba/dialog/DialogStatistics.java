package app.counter.controller.caba.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import app.counter.controller.caba.R;
import app.counter.controller.caba.model.AppState;
import app.counter.controller.caba.model.CounterModel;

public class DialogStatistics extends DialogFragment {
    private final AppState appState;

    public DialogStatistics(AppState appState) {
        this.appState = appState;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_statistics, null);
        TextView tvTotalCounters = view.findViewById(R.id.tvTotalCounters);
        TextView tvTotalValue = view.findViewById(R.id.tvTotalValue);
        TextView tvAverageValue = view.findViewById(R.id.tvAverageValue);
        TextView tvTotalIncrements = view.findViewById(R.id.tvTotalIncrements);
        TextView tvTotalDecrements = view.findViewById(R.id.tvTotalDecrements);

        int totalCounters = appState.getCards().size();
        int totalValue = 0;
        int totalIncrements = 0;
        int totalDecrements = 0;
        for (CounterModel c : appState.getCards()) {
            totalValue += c.getValue();
            totalIncrements += c.getTotalIncrements();
            totalDecrements += c.getTotalDecrements();
        }
        int avgValue = totalCounters > 0 ? totalValue / totalCounters : 0;

        tvTotalCounters.setText("Total Counter: " + totalCounters);
        tvTotalValue.setText("Total Nilai: " + totalValue);
        tvAverageValue.setText("Rata-rata: " + avgValue);
        tvTotalIncrements.setText("Total Increment: " + totalIncrements);
        tvTotalDecrements.setText("Total Decrement: " + totalDecrements);

        return new AlertDialog.Builder(requireContext())
                .setTitle("Statistik")
                .setView(view)
                .setPositiveButton("Tutup", null)
                .create();
    }
}
