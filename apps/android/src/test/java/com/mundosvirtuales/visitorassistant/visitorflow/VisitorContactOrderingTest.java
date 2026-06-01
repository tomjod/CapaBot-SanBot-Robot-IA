package com.mundosvirtuales.visitorassistant.visitorflow;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class VisitorContactOrderingTest {

    @Test
    public void availableContactsStayFirstAndThenSortAlphabetically() {
        List<VisitorDtos.ContactSummary> sorted = VisitorContactOrdering.sort(Arrays.asList(
                new VisitorDtos.ContactSummary("3", "Zeta", false, false, false),
                new VisitorDtos.ContactSummary("2", "beta", true, false, true),
                new VisitorDtos.ContactSummary("1", "Alpha", true, true, true),
                new VisitorDtos.ContactSummary("4", "delta", false, true, false)
        ));

        assertEquals("1", sorted.get(0).getId());
        assertEquals("2", sorted.get(1).getId());
        assertEquals("4", sorted.get(2).getId());
        assertEquals("3", sorted.get(3).getId());
    }
}
