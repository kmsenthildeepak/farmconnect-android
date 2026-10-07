package com.farmconnect.android.util;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Single source of truth for formatting money as Indian Rupees across the
 * app (product list, cart, orders, farmer orders, sales report, ...).
 *
 * Uses NumberFormat.getCurrencyInstance(Locale) rather than a literal "₹"
 * string in source, so the symbol comes from the JDK's locale data at
 * runtime and can never be corrupted by a source file being saved/compiled
 * with the wrong charset - the exact bug behind the "â‚¹3,300.00"-style
 * corruption seen elsewhere in this project.
 */
public final class CurrencyFormatter {

    private static final NumberFormat FORMAT =
            NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    static {
        FORMAT.setMinimumFractionDigits(2);
        FORMAT.setMaximumFractionDigits(2);
    }

    private CurrencyFormatter() {
    }

    /** e.g. 1160.0 -> "₹1,160.00" */
    public static synchronized String format(double amount) {
        return FORMAT.format(amount);
    }

    public static synchronized String format(java.math.BigDecimal amount) {
        return FORMAT.format(amount == null ? 0 : amount);
    }
}
