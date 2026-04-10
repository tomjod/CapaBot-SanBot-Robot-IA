package com.mundosvirtuales.visitorassistant.visitorflow;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class VisitorContactCacheSerializerTest {

    @Test
    public void serializeAndDeserializeRoundTripsContacts() {
        List<VisitorDtos.ContactSummary> contacts = Arrays.asList(
                new VisitorDtos.ContactSummary("ventas", "Ventas", true, false, true),
                new VisitorDtos.ContactSummary("demo", "Demo", false, true, false)
        );

        String raw = VisitorContactCacheSerializer.serialize(contacts);
        List<VisitorDtos.ContactSummary> restored = VisitorContactCacheSerializer.deserialize(raw);

        assertEquals(2, restored.size());
        assertEquals("ventas", restored.get(0).getId());
        assertEquals("Demo", restored.get(1).getDisplayName());
        assertTrue(restored.get(0).isTelegramAvailable());
    }

    @Test
    public void deserializeReturnsEmptyListForInvalidPayload() {
        List<VisitorDtos.ContactSummary> restored = VisitorContactCacheSerializer.deserialize("{not-json");

        assertTrue(restored.isEmpty());
    }
}
