package com.example

import com.example.data.model.AlapConstants
import com.example.data.model.Announcement
import com.example.util.ApkUpdateManager
import com.example.util.WebsiteUpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun appVersion_isUpdatedTo101Build2() {
    assertEquals("1.0.1", AlapConstants.CURRENT_APP_VERSION)
    assertEquals(2L, AlapConstants.CURRENT_APP_VERSION_CODE)
    assertEquals("https://alap-chat.netlify.app", AlapConstants.PUBLIC_WEBSITE_URL)
    assertEquals("https://alap-updates.netlify.app/version.json", AlapConstants.UPDATE_VERSION_JSON_URL)
  }

  @Test
  fun websiteUpdateChecker_versionComparison_isAccurate() {
    assertTrue(WebsiteUpdateChecker.isVersionLower("1.0", "1.0.1"))
    assertTrue(WebsiteUpdateChecker.isVersionLower("1.0.0", "v1.0.1"))
    assertFalse(WebsiteUpdateChecker.isVersionLower("1.0.1", "1.0.1"))
    assertFalse(WebsiteUpdateChecker.isVersionLower("1.1.0", "1.0.1"))
    assertEquals("1.0.1", WebsiteUpdateChecker.maxVersionString("1.0", "1.0.1"))
    assertEquals("1.2.0", WebsiteUpdateChecker.maxVersionString("1.2.0", "1.0.1"))
  }

  @Test
  fun announcementBanner_hidesWhenAppVersionMatches_andShowsWhenVersionDoesNotMatch() {
    val matchingUpdate = Announcement(
      id = "ann1",
      title = "Alap v1.0.1 Update",
      message = "Bug fixes",
      versionTag = "v1.0.1",
      priority = "update"
    )
    assertFalse(
      WebsiteUpdateChecker.shouldShowAnnouncement(
        announcement = matchingUpdate,
        currentVersionName = "1.0.1",
        currentVersionCode = 2L
      )
    )

    val matchingUrgent = matchingUpdate.copy(priority = "urgent", versionTag = "1.0.1")
    assertFalse(
      WebsiteUpdateChecker.shouldShowAnnouncement(
        announcement = matchingUrgent,
        currentVersionName = "1.0.1",
        currentVersionCode = 2L
      )
    )

    val olderUpdate = matchingUpdate.copy(versionTag = "v1.0.0")
    assertFalse(
      WebsiteUpdateChecker.shouldShowAnnouncement(
        announcement = olderUpdate,
        currentVersionName = "1.0.1",
        currentVersionCode = 2L
      )
    )

    val newerUpdate = matchingUpdate.copy(title = "Alap v1.0.2 Ready", versionTag = "v1.0.2")
    assertTrue(
      WebsiteUpdateChecker.shouldShowAnnouncement(
        announcement = newerUpdate,
        currentVersionName = "1.0.1",
        currentVersionCode = 2L
      )
    )

    val generalNotice = Announcement(
      id = "ann2",
      title = "Community Notice",
      message = "Welcome to Alap",
      versionTag = "",
      priority = "info"
    )
    assertTrue(
      WebsiteUpdateChecker.shouldShowAnnouncement(
        announcement = generalNotice,
        currentVersionName = "1.0.1",
        currentVersionCode = 2L
      )
    )
  }

  @Test
  fun apkUpdateManager_normalizesGithubBlobUrls() {
    assertEquals(
      "https://github.com/user/repo/raw/main/alap-latest.apk",
      ApkUpdateManager.normalizeDownloadUrl("  https://github.com/user/repo/blob/main/alap-latest.apk  ")
    )
    assertEquals(
      "https://github.com/user/repo/releases/download/v1.0.1/alap-latest.apk",
      ApkUpdateManager.normalizeDownloadUrl("https://github.com/user/repo/releases/download/v1.0.1/alap-latest.apk")
    )
  }
}
