package com.mundosvirtuales.visitorassistant.visitorflow;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.text.TextUtils;

import com.mundosvirtuales.visitorassistant.R;

import java.util.List;

public class VisitorContactAdapter extends ArrayAdapter<ContactListItemViewModel> {

    private final LayoutInflater inflater;

    public VisitorContactAdapter(Context context, List<ContactListItemViewModel> items) {
        super(context, 0, items);
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        ContactListItemViewModel item = getItem(position);
        return item != null && item.isEnabled();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView != null ? convertView : inflater.inflate(R.layout.item_visitor_contact, parent, false);
        ContactListItemViewModel item = getItem(position);

        TextView title = view.findViewById(R.id.contactTitle);
        TextView availability = view.findViewById(R.id.contactAvailability);
        TextView channels = view.findViewById(R.id.contactChannels);

        if (item != null) {
            title.setText(item.getTitle());
            availability.setText(item.getAvailabilityLabel());
            availability.setBackgroundResource(item.isEnabled()
                    ? R.drawable.visitor_badge_ready_background
                    : R.drawable.visitor_badge_background);
            availability.setTextColor(getContext().getResources().getColor(item.isEnabled()
                    ? R.color.visitorBadgeReadyText
                    : R.color.visitorBadgeMutedText));
            String channelsLabel = item.getChannelsLabel();
            if (TextUtils.isEmpty(channelsLabel)) {
                channels.setVisibility(View.GONE);
            } else {
                channels.setVisibility(View.VISIBLE);
                channels.setText(channelsLabel);
            }
            title.setTextColor(getContext().getResources().getColor(item.isEnabled()
                    ? R.color.visitorTextPrimary
                    : R.color.visitorTextSecondary));
            channels.setTextColor(getContext().getResources().getColor(item.isEnabled()
                    ? R.color.visitorTextSecondary
                    : R.color.visitorBadgeMutedText));
            view.setEnabled(item.isEnabled());
            view.setAlpha(item.isEnabled() ? 1f : 0.72f);
        }

        return view;
    }
}
