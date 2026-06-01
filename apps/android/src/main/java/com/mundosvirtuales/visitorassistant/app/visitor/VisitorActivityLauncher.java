package com.mundosvirtuales.visitorassistant.app.visitor;

import android.content.Context;
import android.content.Intent;

import com.mundosvirtuales.visitorassistant.MyBaseActivity;
import com.mundosvirtuales.visitorassistant.MyDialogActivity;
import com.mundosvirtuales.visitorassistant.features.visitor.domain.VisitorEntryPlan;
import com.mundosvirtuales.visitorassistant.features.visitor.presentation.start.VisitorStartUiEvent;
import com.mundosvirtuales.visitorassistant.visitorflow.VisitorContactActivity;
import com.mundosvirtuales.visitorassistant.visitorflow.VisitorInformationActivity;
import com.mundosvirtuales.visitorassistant.visitorflow.VisitorLeaveMessageActivity;
import com.mundosvirtuales.visitorassistant.visitorflow.VisitorStartActivity;

public final class VisitorActivityLauncher {

    private VisitorActivityLauncher() {
    }

    public static Intent createInteractionIntent(Context context, VisitorEntryPlan.Target target, String visitorName) {
        if (VisitorActivityDestinationResolver.resolveEntryTarget(target) == VisitorActivityDestinationResolver.Destination.VISITOR_START) {
            return VisitorStartActivity.createIntent(context, visitorName);
        }
        return MyDialogActivity.createLegacyIntent(context, visitorName);
    }

    public static Intent createIntent(Context context, VisitorStartUiEvent event) {
        VisitorActivityDestinationResolver.Destination destination = VisitorActivityDestinationResolver.resolveStartTarget(event);
        if (destination == VisitorActivityDestinationResolver.Destination.VISITOR_CONTACT) {
            return VisitorContactActivity.createIntent(context, event.getVisitorName());
        }
        if (destination == VisitorActivityDestinationResolver.Destination.VISITOR_LEAVE_MESSAGE) {
            return VisitorLeaveMessageActivity.createIntent(context, event.getVisitorName());
        }
        if (destination == VisitorActivityDestinationResolver.Destination.VISITOR_INFORMATION) {
            return VisitorInformationActivity.createIntent(context);
        }
        return MyBaseActivity.createIntentFromVisitorStart(context);
    }
}
