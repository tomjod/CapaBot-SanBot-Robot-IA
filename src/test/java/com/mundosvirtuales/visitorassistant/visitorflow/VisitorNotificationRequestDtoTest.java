package com.mundosvirtuales.visitorassistant.visitorflow;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class VisitorNotificationRequestDtoTest {

    @Test
    public void leaveMessageRequestIncludesNormalizedOptionalFields() {
        VisitorDtos.NotificationRequestDto dto = new VisitorDtos.NotificationRequestDto(
                "ventas",
                "robot-01",
                "  María  ",
                "  Hall principal ",
                " leave_message ",
                "  Necesito que me contacten mañana.  "
        );

        assertEquals("ventas", dto.contactId);
        assertEquals("robot-01", dto.deviceId);
        assertEquals("María", dto.visitorName);
        assertEquals("Hall principal", dto.location);
        assertEquals("leave_message", dto.reason);
        assertEquals("Necesito que me contacten mañana.", dto.message);
    }

    @Test
    public void talkToPersonRequestKeepsOptionalMessageFieldsNull() {
        VisitorDtos.NotificationRequestDto dto = new VisitorDtos.NotificationRequestDto("ventas", "robot-01", "  Ada  ", "  ");

        assertEquals("Ada", dto.visitorName);
        assertNull(dto.location);
        assertNull(dto.reason);
        assertNull(dto.message);
    }
}
