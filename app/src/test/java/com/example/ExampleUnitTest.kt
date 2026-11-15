package com.example

import com.example.data.model.AlapConstants
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
