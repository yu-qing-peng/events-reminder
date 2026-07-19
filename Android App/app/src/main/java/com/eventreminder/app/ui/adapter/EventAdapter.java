package com.eventreminder.app.ui.adapter;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.eventreminder.app.R;
import com.eventreminder.app.data.api.ApiModels;
import com.eventreminder.app.util.CountdownFormatter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    public interface EventListener {
        void onDeleteEvent(int eventId);
    }

    private final List<ApiModels.ServerEvent> events;
    private final EventListener listener;

    public EventAdapter(List<ApiModels.ServerEvent> events, EventListener listener) {
        this.events = events;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        ApiModels.ServerEvent event = events.get(position);

        long now = System.currentTimeMillis();
        long diff = 0;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US);
            Date evDate = sdf.parse(event.eventDate);
            if (evDate != null) {
                diff = evDate.getTime() - now;
            }
        } catch (Exception ignored) {}

        boolean isPast = CountdownFormatter.isPast(diff);
        boolean isSoon = CountdownFormatter.isSoon(diff);

        // Dot color
        int dotRes;
        if (isPast) {
            dotRes = R.drawable.dot_muted;
        } else if (isSoon) {
            dotRes = R.drawable.dot_danger;
        } else {
            dotRes = R.drawable.dot_accent;
        }
        holder.ivDot.setBackgroundResource(dotRes);

        // Title
        holder.tvTitle.setText(event.title);

        // Time label
        String timeLabel = "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US);
            Date evDate = sdf.parse(event.eventDate);
            if (evDate != null) {
                SimpleDateFormat displayFmt = new SimpleDateFormat("EEE, MMM d, HH:mm", Locale.US);
                timeLabel = displayFmt.format(evDate);
            }
        } catch (Exception ignored) {}

        if (event.description != null && !event.description.isEmpty()) {
            holder.tvTime.setText(timeLabel + " \u00B7 " + event.description);
        } else {
            holder.tvTime.setText(timeLabel);
        }

        // Countdown
        int cdColor;
        String countdownText;
        if (isPast) {
            cdColor = R.color.text_muted;
            countdownText = "Passed";
        } else if (isSoon) {
            cdColor = R.color.danger;
            long diffMinutes = diff / 60000;
            countdownText = "In " + diffMinutes + "m";
        } else {
            cdColor = R.color.accent;
            countdownText = CountdownFormatter.format(diff);
        }
        holder.tvCountdown.setText(countdownText);
        holder.tvCountdown.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), cdColor));

        // Delete
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteEvent(event.id));
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        ImageView ivDot;
        TextView tvTitle, tvTime, tvCountdown;
        TextView btnDelete;

        EventViewHolder(View itemView) {
            super(itemView);
            ivDot = itemView.findViewById(R.id.ivEventDot);
            tvTitle = itemView.findViewById(R.id.tvEventTitle);
            tvTime = itemView.findViewById(R.id.tvEventTime);
            tvCountdown = itemView.findViewById(R.id.tvEventCountdown);
            btnDelete = itemView.findViewById(R.id.btnDeleteEvent);
        }
    }
}
