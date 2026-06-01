package com.mundosvirtuales.visitorassistant.visitorflow;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VisitorFlowEntryDeciderTest {

    @Test
    public void guidedFlowTakesPrecedenceWhenFeatureAndBackendAreConfigured() {
        VisitorFlowEntryDecider.EntryTarget entryTarget = VisitorFlowEntryDecider.resolve(true, "https://backend.example/api");

        assertEquals(VisitorFlowEntryDecider.EntryTarget.GUIDED_VISITOR_FLOW, entryTarget);
    }

    @Test
    public void legacyDialogRemainsAvailableWhenGuidedFlowIsDisabledOrUnconfigured() {
        assertEquals(
                VisitorFlowEntryDecider.EntryTarget.LEGACY_DIALOG,
                VisitorFlowEntryDecider.resolve(false, "https://backend.example/api")
        );
        assertEquals(
                VisitorFlowEntryDecider.EntryTarget.LEGACY_DIALOG,
                VisitorFlowEntryDecider.resolve(true, "   ")
        );
        assertEquals(
                VisitorFlowEntryDecider.EntryTarget.LEGACY_DIALOG,
                VisitorFlowEntryDecider.resolve(true, null)
        );
    }
}
