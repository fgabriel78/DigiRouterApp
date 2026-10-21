package es.routerapp.protocol.parental

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParentalModelsTest {

    @Test
    fun testTimeFormatting() {
        assertEquals("22:00", TimeLimitConfig.formatTime(1320))
        assertEquals("06:00", TimeLimitConfig.formatTime(360))
        assertEquals("00:00", TimeLimitConfig.formatTime(0))
        assertEquals("00:15", TimeLimitConfig.formatTime(15))
        assertEquals("23:59", TimeLimitConfig.formatTime(1439))

        val config = TimeLimitConfig(bedTimeBeginMinutes = 1260, bedTimeEndMinutes = 420)
        assertEquals("21:00", config.bedTimeBeginFormatted)
        assertEquals("07:00", config.bedTimeEndFormatted)
    }

    @Test
    fun testFilterLevelConversion() {
        assertEquals(FilterLevel.CHILD, FilterLevel.fromCode(0))
        assertEquals(FilterLevel.PRE_TEEN, FilterLevel.fromCode(1))
        assertEquals(FilterLevel.TEEN, FilterLevel.fromCode(2))
        assertEquals(FilterLevel.ADULT, FilterLevel.fromCode(3))
        assertEquals(FilterLevel.CUSTOM, FilterLevel.fromCode(99))

        assertEquals(FilterLevel.TEEN, FilterLevel.fromString("2"))
        assertEquals(FilterLevel.CUSTOM, FilterLevel.fromString("invalid"))
    }

    @Test
    fun testFilterPresets() {
        val child = FilterCategories.forLevel(FilterLevel.CHILD)
        assertTrue(child.adultContentEnable)
        assertTrue(child.gamblingEnable)
        assertTrue(child.gamesEnable)
        assertTrue(child.socialNetworkingEnable)
        assertTrue(child.downLoadEnable)

        val teen = FilterCategories.forLevel(FilterLevel.TEEN)
        assertTrue(teen.adultContentEnable)
        assertTrue(teen.gamblingEnable)
        assertFalse(teen.gamesEnable)
        assertFalse(teen.socialNetworkingEnable)

        val adult = FilterCategories.forLevel(FilterLevel.ADULT)
        assertFalse(adult.adultContentEnable)
        assertFalse(adult.gamblingEnable)
    }

    @Test
    fun testFilterCategoryAttributeMap() {
        val custom = FilterCategories(
            adultContentEnable = true,
            gamblingEnable = true,
            gamesEnable = false,
            socialNetworkingEnable = true,
        )
        val map = custom.toAttributeMap()
        assertEquals("1", map["adultContentEnable"])
        assertEquals("1", map["gambleEnable"])
        assertEquals("0", map["gamesEnable"])
        assertEquals("1", map["socialNetworkingEnable"])
    }

    @Test
    fun testProfileStatus() {
        assertEquals(ProfileStatus.ACTIVE, ProfileStatus.fromInt(1))
        assertEquals(ProfileStatus.PAUSED, ProfileStatus.fromInt(0))
        assertEquals(ProfileStatus.PAUSED, ProfileStatus.fromInt(-1))
    }
}
