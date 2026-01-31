package app.counter.controller.caba.adapter;

import android.content.Context;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.Collections;
import java.util.List;

import app.counter.controller.caba.R;
import app.counter.controller.caba.model.CounterModel;

public class CounterAdapter extends RecyclerView.Adapter<CounterAdapter.CounterViewHolder> {
    public interface CounterListener {
        void onIncrement(CounterModel counter);
        void onDecrement(CounterModel counter);
        void onReset(CounterModel counter);
        void onMenu(CounterModel counter, View anchor);
        void onDragStart(RecyclerView.ViewHolder viewHolder);
        void onCardClick(CounterModel counter);
    }

    private List<CounterModel> counters;
    private final CounterListener listener;
    private final Context context;

    public CounterAdapter(Context context, List<CounterModel> counters, CounterListener listener) {
        this.context = context;
        this.counters = counters;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CounterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_counter_native, parent, false);
        return new CounterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CounterViewHolder holder, int position) {
        CounterModel counter = counters.get(position);
        holder.bind(counter);
    }

    @Override
    public int getItemCount() {
        return counters.size();
    }

    public void moveItem(int from, int to) {
        if (from < 0 || to < 0 || from >= counters.size() || to >= counters.size()) return;
        Collections.swap(counters, from, to);
        notifyItemMoved(from, to);
    }

    public void setCounters(List<CounterModel> counters) {
        this.counters = counters;
        notifyDataSetChanged();
    }

    class CounterViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        View colorTagIndicator;
        TextView tvLabel, tvValue, tvTargetValue, tvTargetPercent, tvStep, tvNotes, tvCategory;
        ProgressBar progressTarget;
        ImageButton btnPlus, btnMinus, btnReset, btnMenu;
        ImageView ivLocked, ivDragHandle;
        View targetProgressContainer, addonContainer;

        public CounterViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            colorTagIndicator = itemView.findViewById(R.id.colorTagIndicator);
            tvLabel = itemView.findViewById(R.id.tvLabel);
            tvValue = itemView.findViewById(R.id.tvValue);
            tvTargetValue = itemView.findViewById(R.id.tvTargetValue);
            tvTargetPercent = itemView.findViewById(R.id.tvTargetPercent);
            tvStep = itemView.findViewById(R.id.tvStep);
            tvNotes = itemView.findViewById(R.id.tvNotes);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            progressTarget = itemView.findViewById(R.id.progressTarget);
            btnPlus = itemView.findViewById(R.id.btnPlus);
            btnMinus = itemView.findViewById(R.id.btnMinus);
            btnReset = itemView.findViewById(R.id.btnReset);
            btnMenu = itemView.findViewById(R.id.btnMenu);
            ivLocked = itemView.findViewById(R.id.ivLocked);
            ivDragHandle = itemView.findViewById(R.id.ivDragHandle);
            targetProgressContainer = itemView.findViewById(R.id.targetProgressContainer);
            addonContainer = itemView.findViewById(R.id.addonContainer);
        }

        public void bind(CounterModel counter) {
            tvLabel.setText(counter.getLabel());
            tvValue.setText(String.valueOf(counter.getValue()));
            tvStep.setText("Step: " + counter.getStep());
            tvStep.setVisibility(counter.getStep() > 1 ? View.VISIBLE : View.GONE);
            tvNotes.setText(counter.getNotes());
            tvNotes.setVisibility(counter.getNotes() != null && !counter.getNotes().isEmpty() ? View.VISIBLE : View.GONE);
            tvCategory.setText(counter.getCategory());
            tvCategory.setVisibility(counter.getCategory() != null && !counter.getCategory().isEmpty() ? View.VISIBLE : View.GONE);
            ivLocked.setVisibility(counter.isLocked() ? View.VISIBLE : View.GONE);

            // Color tag
            int color = getColorForTag(counter.getColorTag());
            colorTagIndicator.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_IN);

            // Target progress
            if (counter.getTarget() > 0) {
                targetProgressContainer.setVisibility(View.VISIBLE);
                tvTargetValue.setText(counter.getValue() + "/" + counter.getTarget());
                int percent = counter.getProgressPercent();
                tvTargetPercent.setText(percent + "%");
                progressTarget.setProgress(percent);
            } else {
                targetProgressContainer.setVisibility(View.GONE);
            }

            // Button listeners
            btnPlus.setOnClickListener(v -> listener.onIncrement(counter));
            btnMinus.setOnClickListener(v -> listener.onDecrement(counter));
            btnReset.setOnClickListener(v -> listener.onReset(counter));
            btnMenu.setOnClickListener(v -> listener.onMenu(counter, btnMenu));
            cardView.setOnClickListener(v -> listener.onCardClick(counter));

            // Drag handle
            ivDragHandle.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    listener.onDragStart(this);
                }
                return false;
            });
        }

        private int getColorForTag(String tag) {
            switch (tag) {
                case "red": return context.getResources().getColor(R.color.tag_red);
                case "orange": return context.getResources().getColor(R.color.tag_orange);
                case "yellow": return context.getResources().getColor(R.color.tag_yellow);
                case "green": return context.getResources().getColor(R.color.tag_green);
                case "blue": return context.getResources().getColor(R.color.tag_blue);
                case "purple": return context.getResources().getColor(R.color.tag_purple);
                case "pink": return context.getResources().getColor(R.color.tag_pink);
                case "gray":
                default: return context.getResources().getColor(R.color.tag_gray);
            }
        }
    }

    abstract class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }
}
