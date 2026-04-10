package com.mundosvirtuales.visitorassistant.visitorflow;

import android.content.Context;

import com.mundosvirtuales.visitorassistant.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class VisitorInformationCatalog {

    public static final class Option {
        private final String id;
        private final String title;
        private final String summary;
        private final String detail;

        Option(String id, String title, String summary, String detail) {
            this.id = id;
            this.title = title;
            this.summary = summary;
            this.detail = detail;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getSummary() {
            return summary;
        }

        public String getDetail() {
            return detail;
        }
    }

    private VisitorInformationCatalog() {
    }

    public static List<Option> buildDefault(Context context) {
        List<Option> options = new ArrayList<>();
        options.add(new Option(
                "spaces",
                context.getString(R.string.visitor_information_option_spaces),
                context.getString(R.string.visitor_information_option_spaces_summary),
                context.getString(R.string.visitor_information_option_spaces_detail)
        ));
        options.add(new Option(
                "robot",
                context.getString(R.string.visitor_information_option_robot),
                context.getString(R.string.visitor_information_option_robot_summary),
                context.getString(R.string.visitor_information_option_robot_detail)
        ));
        options.add(new Option(
                "contact",
                context.getString(R.string.visitor_information_option_contact),
                context.getString(R.string.visitor_information_option_contact_summary),
                context.getString(R.string.visitor_information_option_contact_detail)
        ));
        return Collections.unmodifiableList(options);
    }
}
