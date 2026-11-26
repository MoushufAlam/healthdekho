// ============================================================================
// OPTIONAL: FR-5 (not evaluated: only FR-1 to FR-3 are assessed)
// What this file does: FULLY_PAID / OVERDUE / ON_TRACK shown in search results.
// Only FR-5 uses this file. To remove the feature, delete every piece tagged
// "OPTIONAL: FR-5" (search the project for it). Theory and flow: docs/steps/step8.readme.md
// ============================================================================
package com.healthdekho.dto.search;

// Policy-level payment health shown in FR-5 search results
public enum PremiumPaymentStatus {
    FULLY_PAID,
    OVERDUE,
    ON_TRACK
}
