package com.mundosvirtuales.visitorassistant.visitorflow;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class VisitorFlowLoggerTest {

    @Test
    public void summarizeContactUsesPlainJavaEmptyHandling() {
        VisitorDtos.ContactSummary contact = new VisitorDtos.ContactSummary("ventas", "", true, false, true);

        String summary = VisitorFlowLogger.summarizeContact(contact);

        assertEquals("contactId=ventas, displayName=<empty>, available=true", summary);
    }

    @Test
    public void summarizeNotificationDetectsMissingAndPresentMessagesWithoutAndroidFrameworkHelpers() {
        VisitorDtos.NotificationRequestDto emptyMessageRequest = new VisitorDtos.NotificationRequestDto(
                "ventas",
                "robot-01",
                "Ada",
                "Hall"
        );
        VisitorDtos.NotificationRequestDto populatedMessageRequest = new VisitorDtos.NotificationRequestDto(
                "ventas",
                "robot-01",
                "Ada",
                "Hall",
                "leave_message",
                "Necesito ayuda"
        );

        String emptySummary = VisitorFlowLogger.summarizeNotification(emptyMessageRequest);
        String populatedSummary = VisitorFlowLogger.summarizeNotification(populatedMessageRequest);

        assertTrue(emptySummary.endsWith("hasMessage=false"));
        assertTrue(populatedSummary.endsWith("hasMessage=true"));
    }
}
