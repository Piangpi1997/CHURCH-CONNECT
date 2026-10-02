package org.cmf.churchconnect

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLocaleTest {
    @Test fun unsetOrUnsupportedChoiceUsesSystemLanguage() {
        assertEquals(AppLocale.SYSTEM, AppLocale.normalizeSelection(null))
        assertEquals(AppLocale.SYSTEM, AppLocale.normalizeSelection("fr"))
        assertEquals(AppLocale.SYSTEM, AppLocale.normalizeSelection(""))
    }

    @Test fun supportedLanguagesRemainSelectableIncludingReviewedLaterTedimFallback() {
        assertEquals("en", AppLocale.normalizeSelection("en"))
        assertEquals("my", AppLocale.normalizeSelection("my"))
        assertEquals("ctd", AppLocale.normalizeSelection("ctd"))
        assertEquals(AppLocale.SYSTEM, AppLocale.normalizeSelection(AppLocale.SYSTEM))
    }
}
