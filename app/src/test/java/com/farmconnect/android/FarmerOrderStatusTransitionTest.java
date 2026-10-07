package com.farmconnect.android;

import com.farmconnect.android.adapter.FarmerOrderAdapter;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FarmerOrderStatusTransitionTest {

    @Test
    public void testPendingTransitions() {
        List<String> allowed = FarmerOrderAdapter.getAllowedTransitions("PENDING");
        assertEquals(5, allowed.size());
        assertTrue(allowed.contains("ACCEPTED"));
        assertTrue(allowed.contains("REJECTED"));
        assertTrue(allowed.contains("PACKED"));
        assertTrue(allowed.contains("OUT_FOR_DELIVERY"));
        assertTrue(allowed.contains("DELIVERED"));
        assertFalse(allowed.contains("CANCELLED"));

        assertTrue(FarmerOrderAdapter.isValidTransition("PENDING", "ACCEPTED"));
        assertTrue(FarmerOrderAdapter.isValidTransition("PENDING", "REJECTED"));
        assertTrue(FarmerOrderAdapter.isValidTransition("PENDING", "PACKED"));
        assertTrue(FarmerOrderAdapter.isValidTransition("PENDING", "OUT_FOR_DELIVERY"));
        assertTrue(FarmerOrderAdapter.isValidTransition("PENDING", "DELIVERED"));

        assertFalse(FarmerOrderAdapter.isValidTransition("PENDING", "PENDING"));
        assertFalse(FarmerOrderAdapter.isValidTransition("PENDING", "CANCELLED"));
    }

    @Test
    public void testAcceptedTransitions() {
        List<String> allowed = FarmerOrderAdapter.getAllowedTransitions("ACCEPTED");
        assertEquals(4, allowed.size());
        assertTrue(allowed.contains("REJECTED"));
        assertTrue(allowed.contains("PACKED"));
        assertTrue(allowed.contains("OUT_FOR_DELIVERY"));
        assertTrue(allowed.contains("DELIVERED"));
        assertFalse(allowed.contains("PENDING"));
        assertFalse(allowed.contains("CANCELLED"));

        assertTrue(FarmerOrderAdapter.isValidTransition("ACCEPTED", "REJECTED"));
        assertTrue(FarmerOrderAdapter.isValidTransition("ACCEPTED", "PACKED"));
        assertTrue(FarmerOrderAdapter.isValidTransition("ACCEPTED", "OUT_FOR_DELIVERY"));
        assertTrue(FarmerOrderAdapter.isValidTransition("ACCEPTED", "DELIVERED"));

        assertFalse(FarmerOrderAdapter.isValidTransition("ACCEPTED", "PENDING"));
        assertFalse(FarmerOrderAdapter.isValidTransition("ACCEPTED", "ACCEPTED"));
        assertFalse(FarmerOrderAdapter.isValidTransition("ACCEPTED", "CANCELLED"));
    }

    @Test
    public void testPackedTransitions() {
        List<String> allowed = FarmerOrderAdapter.getAllowedTransitions("PACKED");
        assertEquals(3, allowed.size());
        assertTrue(allowed.contains("REJECTED"));
        assertTrue(allowed.contains("OUT_FOR_DELIVERY"));
        assertTrue(allowed.contains("DELIVERED"));
        assertFalse(allowed.contains("PENDING"));
        assertFalse(allowed.contains("ACCEPTED"));
        assertFalse(allowed.contains("CANCELLED"));

        assertTrue(FarmerOrderAdapter.isValidTransition("PACKED", "REJECTED"));
        assertTrue(FarmerOrderAdapter.isValidTransition("PACKED", "OUT_FOR_DELIVERY"));
        assertTrue(FarmerOrderAdapter.isValidTransition("PACKED", "DELIVERED"));

        assertFalse(FarmerOrderAdapter.isValidTransition("PACKED", "PENDING"));
        assertFalse(FarmerOrderAdapter.isValidTransition("PACKED", "ACCEPTED"));
        assertFalse(FarmerOrderAdapter.isValidTransition("PACKED", "PACKED"));
        assertFalse(FarmerOrderAdapter.isValidTransition("PACKED", "CANCELLED"));
    }

    @Test
    public void testOutForDeliveryTransitions() {
        List<String> allowed = FarmerOrderAdapter.getAllowedTransitions("OUT_FOR_DELIVERY");
        assertEquals(1, allowed.size());
        assertTrue(allowed.contains("DELIVERED"));

        assertTrue(FarmerOrderAdapter.isValidTransition("OUT_FOR_DELIVERY", "DELIVERED"));

        assertFalse(FarmerOrderAdapter.isValidTransition("OUT_FOR_DELIVERY", "PENDING"));
        assertFalse(FarmerOrderAdapter.isValidTransition("OUT_FOR_DELIVERY", "ACCEPTED"));
        assertFalse(FarmerOrderAdapter.isValidTransition("OUT_FOR_DELIVERY", "REJECTED"));
        assertFalse(FarmerOrderAdapter.isValidTransition("OUT_FOR_DELIVERY", "PACKED"));
        assertFalse(FarmerOrderAdapter.isValidTransition("OUT_FOR_DELIVERY", "OUT_FOR_DELIVERY"));
        assertFalse(FarmerOrderAdapter.isValidTransition("OUT_FOR_DELIVERY", "CANCELLED"));
    }

    @Test
    public void testRejectedTerminal() {
        List<String> allowed = FarmerOrderAdapter.getAllowedTransitions("REJECTED");
        assertTrue(allowed.isEmpty());

        String[] targets = {"PENDING", "ACCEPTED", "REJECTED", "PACKED", "OUT_FOR_DELIVERY", "DELIVERED", "CANCELLED"};
        for (String target : targets) {
            assertFalse(FarmerOrderAdapter.isValidTransition("REJECTED", target));
        }
    }

    @Test
    public void testDeliveredTerminal() {
        List<String> allowed = FarmerOrderAdapter.getAllowedTransitions("DELIVERED");
        assertTrue(allowed.isEmpty());

        String[] targets = {"PENDING", "ACCEPTED", "REJECTED", "PACKED", "OUT_FOR_DELIVERY", "DELIVERED", "CANCELLED"};
        for (String target : targets) {
            assertFalse(FarmerOrderAdapter.isValidTransition("DELIVERED", target));
        }
    }

    @Test
    public void testCancelledTerminal() {
        List<String> allowed = FarmerOrderAdapter.getAllowedTransitions("CANCELLED");
        assertTrue(allowed.isEmpty());

        String[] targets = {"PENDING", "ACCEPTED", "REJECTED", "PACKED", "OUT_FOR_DELIVERY", "DELIVERED", "CANCELLED"};
        for (String target : targets) {
            assertFalse(FarmerOrderAdapter.isValidTransition("CANCELLED", target));
        }
    }
}
