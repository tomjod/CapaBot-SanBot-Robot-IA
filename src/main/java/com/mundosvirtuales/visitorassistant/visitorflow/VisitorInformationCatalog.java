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
        private final int logoResId;
        private final String summary;
        private final String detail;

        Option(String id, String title, int logoResId, String summary, String detail) {
            this.id = id;
            this.title = title;
            this.logoResId = logoResId;
            this.summary = summary;
            this.detail = detail;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public int getLogoResId() {
            return logoResId;
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
                "transformapp",
                context.getString(R.string.visitor_information_option_transformapp),
                R.drawable.busso_transformapp_logo,
                context.getString(R.string.visitor_information_option_transformapp_summary),
                context.getString(R.string.visitor_information_option_transformapp_detail)
        ));
        options.add(new Option(
                "tps",
                context.getString(R.string.visitor_information_option_tps),
                R.drawable.busso_tps_logo,
                context.getString(R.string.visitor_information_option_tps_summary),
                context.getString(R.string.visitor_information_option_tps_detail)
        ));
        options.add(new Option(
                "quimica_mavar",
                context.getString(R.string.visitor_information_option_quimica_mavar),
                R.drawable.busso_quimica_mavar_logo,
                context.getString(R.string.visitor_information_option_quimica_mavar_summary),
                context.getString(R.string.visitor_information_option_quimica_mavar_detail)
        ));
        options.add(new Option(
                "mundos_virtuales",
                context.getString(R.string.visitor_information_option_mundos_virtuales),
                R.drawable.busso_mundos_virtuales_logo,
                context.getString(R.string.visitor_information_option_mundos_virtuales_summary),
                context.getString(R.string.visitor_information_option_mundos_virtuales_detail)
        ));
        options.add(new Option(
                "tra",
                context.getString(R.string.visitor_information_option_tra),
                R.drawable.busso_tra_logo,
                context.getString(R.string.visitor_information_option_tra_summary),
                context.getString(R.string.visitor_information_option_tra_detail)
        ));
        options.add(new Option(
                "data_center",
                context.getString(R.string.visitor_information_option_data_center),
                R.drawable.busso_data_center_logo,
                context.getString(R.string.visitor_information_option_data_center_summary),
                context.getString(R.string.visitor_information_option_data_center_detail)
        ));
        options.add(new Option(
                "yerbas_buenas",
                context.getString(R.string.visitor_information_option_yerbas_buenas),
                R.drawable.busso_yerbas_buenas_logo,
                context.getString(R.string.visitor_information_option_yerbas_buenas_summary),
                context.getString(R.string.visitor_information_option_yerbas_buenas_detail)
        ));
        options.add(new Option(
                "lb",
                context.getString(R.string.visitor_information_option_lb),
                R.drawable.busso_lb_logo,
                context.getString(R.string.visitor_information_option_lb_summary),
                context.getString(R.string.visitor_information_option_lb_detail)
        ));
        options.add(new Option(
                "micro_renta",
                context.getString(R.string.visitor_information_option_micro_renta),
                R.drawable.busso_micro_renta_logo,
                context.getString(R.string.visitor_information_option_micro_renta_summary),
                context.getString(R.string.visitor_information_option_micro_renta_detail)
        ));
        return Collections.unmodifiableList(options);
    }
}
