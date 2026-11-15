package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.AppConfig
import com.example.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class AlapRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureUserProfile_authenticatedOwner_createsProfileAndPrivateEmail() = runBlocking {
        val freshEmail = "alice.${UUID.randomUUID()}@test.com"
        val uid = signInTestUser(freshEmail)
        val repo = AlapRepository(firestore)

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureUserProfileAndPrivateInfo(uid, "Alice Alap", freshEmail)
        }
        assertTrue(result.isSuccess)

        val profile = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeMyProfile(uid).first { it != null }
        }
        assertEquals("Alice Alap", profile?.displayName)
    }

    @Test
    fun directChatAndMessages_participantsCanChat_nonParticipantRejected() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = AlapRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureUserProfileAndPrivateInfo(aliceUid, "Alice", ALICE_EMAIL).getOrThrow()
        }

        val bobUid = signInTestUser(BOB_EMAIL)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureUserProfileAndPrivateInfo(bobUid, "Bob", BOB_EMAIL).getOrThrow()
        }

        val aliceProfile = UserProfile(userId = aliceUid, displayName = "Alice", avatarId = "avatar_emerald")
        val bobProfile = UserProfile(userId = bobUid, displayName = "Bob", avatarId = "avatar_slate")

        // Switch back to Alice to create DM and send message
        signInTestUser(ALICE_EMAIL)
        val chatId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.getOrCreateDirectChat(aliceProfile, bobProfile).getOrThrow()
        }
        val chat = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeChat(chatId).first { it != null }
        }!!

        val msgId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.sendMessage(chat, "Alice", "Salam Bob! ${UUID.randomUUID()}").getOrThrow()
        }
        assertTrue(msgId.isNotBlank())

        val messages = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeMessages(chatId, aliceUid).first { it.isNotEmpty() }
        }
        assertTrue(messages.any { it.message.id == msgId })

        // Switch to Charlie (non-participant) and verify isolation
        val charlieUid = signInTestUser(CHARLIE_EMAIL)
        val charlieFilteredMessages = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeMessages(chatId, charlieUid).first()
        }
        assertTrue("Filtered query must leak zero messages to non-participant", charlieFilteredMessages.isEmpty())

        var chatReadDenied = false
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeChat(chatId).first()
            }
        } catch (e: Exception) {
            val ffe = e.unwrapFirestoreException()
            chatReadDenied = ffe?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        }
        assertTrue("Expected PERMISSION_DENIED for non-participant direct chat read", chatReadDenied)

        var msgReadDenied = false
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("chats").document(chatId).collection("messages").document(msgId).get().await()
            }
        } catch (e: Exception) {
            val ffe = e.unwrapFirestoreException()
            msgReadDenied = ffe?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        }
        assertTrue("Expected PERMISSION_DENIED for non-participant direct message read", msgReadDenied)
    }

    @Test
    fun observeMyChats_unauthenticated_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = AlapRepository(firestore)
        var denied = false
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeMyChats("some_uid").first()
            }
        } catch (e: Exception) {
            val ffe = e.unwrapFirestoreException()
            denied = ffe?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        }
        assertTrue("Expected PERMISSION_DENIED when unauthenticated", denied)
    }

    @Test
    fun updateAppConfig_regularUser_failsWithPermissionDenied() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repo = AlapRepository(firestore)
        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.updateAppConfig(AppConfig(appTagline = "Unauthorized"))
        }
        assertTrue(result.isFailure)
        val ffe = result.exceptionOrNull()?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ffe?.code)
    }

    @Test
    fun postStatus_authenticatedOwner_publishesAndAppearsInFeed() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = AlapRepository(firestore)
        val aliceProfile = UserProfile(userId = aliceUid, displayName = "Alice", avatarId = "avatar_emerald")

        val statusId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.postStatus(aliceProfile, "Using Alap offline!", "emerald").getOrThrow()
        }
        val feed = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeStatuses().first { list -> list.any { it.id == statusId } }
        }
        assertTrue(feed.any { it.id == statusId })
    }

    @Test
    fun emailOtpVerification_ownerIssuesAndVerifies_otherUserDenied() = runBlocking {
        val freshEmail = "otp.${UUID.randomUUID()}@gmail.com"
        val aliceUid = signInTestUser(freshEmail)
        val repo = AlapRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureUserProfileAndPrivateInfo(aliceUid, "Otp User", freshEmail).getOrThrow()
        }

        val issuedCode = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.issueEmailVerificationCode(aliceUid, freshEmail).getOrThrow()
        }
        assertEquals(6, issuedCode.length)
        assertTrue(issuedCode.all { it.isDigit() })

        val wrongResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.verifyEmailCode(aliceUid, if (issuedCode == "000000") "111111" else "000000")
        }
        assertTrue("Wrong code must fail", wrongResult.isFailure)

        val verified = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.verifyEmailCode(aliceUid, issuedCode).getOrThrow()
        }
        assertTrue("Matching 6-digit code must succeed", verified)

        // Bob cannot read Alice's OTP verification document
        signInTestUser(BOB_EMAIL)
        var denied = false
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid)
                    .collection("private").document("verification").get().await()
            }
        } catch (e: Exception) {
            val ffe = e.unwrapFirestoreException()
            denied = ffe?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        }
        assertTrue("Other user must receive PERMISSION_DENIED reading verification OTP", denied)
    }

    @Test
    fun updateProfileAndPrivacy_hidesPhoneAndUserIdOnPublicDoc_keepsInPrivateInfo() = runBlocking {
        val freshEmail = "privacy.${UUID.randomUUID()}@gmail.com"
        val uid = signInTestUser(freshEmail)
        val repo = AlapRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureUserProfileAndPrivateInfo(uid, "Privacy User", freshEmail).getOrThrow()
        }

        // First hide both phone number and user ID
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.updateProfileAndPrivacy(
                displayName = "Privacy User",
                handle = "privacy_user",
                phoneNumber = "+8801712345678",
                alapId = "ALAP-998877",
                about = "Privacy first",
                avatarId = "avatar_emerald",
                hidePhoneNumber = true,
                hideUserId = true,
                hideOnlineStatus = true,
                hideAbout = true,
                isDiscoverable = true,
                markProfileCompleted = true
            ).getOrThrow()
        }

        val hiddenProfile = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeMyProfile(uid).first { it != null && it.isProfileCompleted }
        }!!
        assertEquals("", hiddenProfile.publicPhoneNumber)
        assertEquals("", hiddenProfile.publicUserId)
        assertTrue(hiddenProfile.hidePhoneNumber)
        assertTrue(hiddenProfile.hideUserId)
        assertTrue(hiddenProfile.hideOnlineStatus)
        assertTrue(hiddenProfile.hideAbout)
        assertTrue(!hiddenProfile.isOnline)

        val privateInfo = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeMyPrivateInfo(uid).first { it != null && it.phoneNumber == "+8801712345678" }
        }!!
        assertEquals("+8801712345678", privateInfo.phoneNumber)
        assertEquals("ALAP-998877", privateInfo.alapId)

        // Now unhide phone number and user ID
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.updateProfileAndPrivacy(
                displayName = "Privacy User",
                handle = "privacy_user",
                phoneNumber = "+8801712345678",
                alapId = "ALAP-998877",
                about = "Privacy first",
                avatarId = "avatar_emerald",
                hidePhoneNumber = false,
                hideUserId = false,
                hideOnlineStatus = false,
                hideAbout = false,
                isDiscoverable = true,
                markProfileCompleted = true
            ).getOrThrow()
        }

        val visibleProfile = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeMyProfile(uid).first { it != null && it.publicPhoneNumber == "+8801712345678" }
        }!!
        assertEquals("+8801712345678", visibleProfile.publicPhoneNumber)
        assertEquals("ALAP-998877", visibleProfile.publicUserId)
    }

    @Test
    fun pollAndHideReadReceipts_workInChat_whileReadReceiptsRespectPrivacy() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = AlapRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureUserProfileAndPrivateInfo(aliceUid, "Alice", ALICE_EMAIL).getOrThrow()
        }
        val bobUid = signInTestUser(BOB_EMAIL)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureUserProfileAndPrivateInfo(bobUid, "Bob", BOB_EMAIL).getOrThrow()
        }

        val aliceProfile = UserProfile(userId = aliceUid, displayName = "Alice")
        val bobProfile = UserProfile(userId = bobUid, displayName = "Bob")

        signInTestUser(ALICE_EMAIL)
        val chatId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.getOrCreateDirectChat(aliceProfile, bobProfile).getOrThrow()
        }
        val chat = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeChat(chatId).first { it != null }
        }!!

        val pollId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.sendMessage(
                chat = chat,
                senderName = "Alice",
                text = "Best meeting time?",
                messageType = "poll",
                pollQuestion = "Best meeting time?",
                pollOptions = listOf("10 AM", "4 PM")
            ).getOrThrow()
        }
        val pollMsg = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeMessages(chatId, aliceUid).first { list -> list.any { it.message.id == pollId } }
        }.first { it.message.id == pollId }.message

        // Bob opens chat with hideReadReceipts = true -> readByUids must NOT add bobUid, yet Bob can vote on poll
        signInTestUser(BOB_EMAIL)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.markChatAndMessagesRead(chat, listOf(pollMsg), hideReadReceipts = true)
            repo.votePoll(chatId, pollMsg, 1).getOrThrow()
        }

        val afterBob = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeMessages(chatId, bobUid).first { list ->
                val target = list.firstOrNull { it.message.id == pollId }?.message
                target?.pollVotes?.containsKey(bobUid) == true
            }
        }.first { it.message.id == pollId }.message

        assertEquals(1L, (afterBob.pollVotes?.get(bobUid) as? Number)?.toLong())
        assertTrue("Bob must not be added to readByUids when hideReadReceipts=true", bobUid !in afterBob.readByUids)
    }

    @Test
    fun userReportAndAuditLogSecurity_regularUserCannotReadAuditLogsOrReports() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = AlapRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureUserProfileAndPrivateInfo(aliceUid, "Alice", ALICE_EMAIL).getOrThrow()
        }

        val reportRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.submitUserReport(
                targetUid = "target_uid_123",
                targetName = "Spammer",
                chatId = "dm_test",
                reason = "Spam links"
            )
        }
        assertTrue("Non-suspended user can submit report", reportRes.isSuccess)

        var auditReadDenied = false
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("audit_logs").get().await()
            }
        } catch (e: Exception) {
            auditReadDenied = e.unwrapFirestoreException()?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        }
        assertTrue("Regular user must receive PERMISSION_DENIED reading audit_logs", auditReadDenied)

        var reportsListDenied = false
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("reports").get().await()
            }
        } catch (e: Exception) {
            reportsListDenied = e.unwrapFirestoreException()?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        }
        assertTrue("Regular user must receive PERMISSION_DENIED listing reports", reportsListDenied)
    }

    @Test
    fun devicePushTokenAndApiKeySecurity_enforcedInRepository() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = AlapRepository(firestore)

        // Alice can register her own FCM device token
        val tokenSave = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveDevicePushToken("fcm_sample_token_alice_123456")
        }
        assertTrue("Owner must be allowed to save device push token", tokenSave.isSuccess)

        // Regular user Alice cannot create Primary Admin API keys
        val apiKeyRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createAdminApiKey("Unauthorized Website Key", listOf("push:send"))
        }
        assertTrue("Regular user must fail creating Admin API key", apiKeyRes.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            apiKeyRes.exceptionOrNull()?.unwrapFirestoreException()?.code
        )

        // Bob cannot read Alice's device token
        signInTestUser(BOB_EMAIL)
        var tokenReadDenied = false
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("device_tokens").document(aliceUid).get().await()
            }
        } catch (e: Exception) {
            tokenReadDenied = e.unwrapFirestoreException()?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        }
        assertTrue("Other user must receive PERMISSION_DENIED reading another user's push token", tokenReadDenied)
    }

    @Test
    fun purgeDeliveredRelayMessages_deletesOnlyAfterAllParticipantsRead() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = AlapRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureUserProfileAndPrivateInfo(aliceUid, "Alice", ALICE_EMAIL).getOrThrow()
        }

        val bobUid = signInTestUser(BOB_EMAIL)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureUserProfileAndPrivateInfo(bobUid, "Bob", BOB_EMAIL).getOrThrow()
        }

        signInTestUser(ALICE_EMAIL)
        val chatId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.getOrCreateDirectChat(
                UserProfile(userId = aliceUid, displayName = "Alice"),
                UserProfile(userId = bobUid, displayName = "Bob")
            ).getOrThrow()
        }
        val chat = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeChat(chatId).first { it != null }
        }!!

        val msgId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.sendMessage(chat, "Alice", "Relay payload").getOrThrow()
        }

        // Switch to Bob, mark read so both participants are in readByUids, then purge
        signInTestUser(BOB_EMAIL)
        val beforeRead = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeMessages(chatId, bobUid).first { list -> list.any { it.message.id == msgId } }
        }.map { it.message }

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.markChatAndMessagesRead(chat, beforeRead, hideReadReceipts = false)
        }

        val afterRead = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeMessages(chatId, bobUid).first { list ->
                val target = list.firstOrNull { it.message.id == msgId }?.message
                target != null && target.readByUids.containsAll(listOf(aliceUid, bobUid))
            }
        }.map { it.message }

        val purgedCount = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.purgeDeliveredRelayMessages(chat, afterRead)
        }
        assertTrue("Expected at least 1 delivered relay message to be purged", purgedCount >= 1)

        val remaining = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeMessages(chatId, bobUid).first { list -> list.none { it.message.id == msgId } }
        }
        assertTrue("Delivered relay message should be purged from Firestore", remaining.none { it.message.id == msgId })
    }

    private companion object {
        const val ALICE_EMAIL = "alice.alap@test.com"
        const val BOB_EMAIL = "bob.alap@test.com"
        const val CHARLIE_EMAIL = "charlie.alap@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
