const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";
const CHARLIE_UID = "charlie_uid_789";
const ADMIN_UID = "admin_uid_000";
const ADMIN_EMAIL = "mourshedahmed00@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function nowTs() {
  return new Date();
}

function makeUserProfile(uid, name = "Alice") {
  const ts = nowTs();
  return {
    userId: uid,
    displayName: name,
    handle: name.toLowerCase(),
    about: "Hey there! I am using Alap.",
    avatarId: "avatar_emerald",
    publicPhoneNumber: "",
    publicUserId: "ALAP-" + uid.slice(0, 6).toUpperCase(),
    hidePhoneNumber: true,
    hideUserId: false,
    hideOnlineStatus: false,
    hideAbout: false,
    isProfileCompleted: true,
    isDiscoverable: true,
    isOnline: true,
    isVerified: false,
    isSuspended: false,
    lastSeenAt: ts,
    createdAt: ts,
    updatedAt: ts,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read users, chats, announcements, or statuses", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").where("isDiscoverable", "==", true).get());
  await assertFails(unauthDb.collection("chats").where("participantUids", "array-contains", ALICE_UID).get());
  await assertFails(unauthDb.collection("announcements").where("isActive", "==", true).get());
  await assertFails(unauthDb.collection("statuses").where("isPublic", "==", true).get());
  await assertFails(unauthDb.collection("app_config").doc("global").get());
});

test("User profile: owner can create and update own profile, cannot self-verify or self-suspend", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@gmail.com", email_verified: true }).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(makeUserProfile(ALICE_UID, "Alice")));

  // Cannot update isVerified
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).update({ isVerified: true, updatedAt: nowTs() }));
  // Cannot inject ghost field (Shadow Update)
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).update({ role: "admin", updatedAt: nowTs() }));
  // Valid update
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).update({ about: "Offline ready!", updatedAt: nowTs() }));
});

test("PII isolation: Bob cannot read Alice's private info or OTP verification code, only owner/admin where allowed", async () => {
  const ts = nowTs();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@gmail.com", email_verified: true }).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("private").doc("info").set({
      userId: ALICE_UID,
      email: "alice@gmail.com",
      phoneNumber: "+8801700000000",
      alapId: "ALAP-ALICE1",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("private").doc("verification").set({
      userId: ALICE_UID,
      email: "alice@gmail.com",
      code: "482915",
      expiresAtEpochMs: Date.now() + 600000,
      attempts: 0,
      verified: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@gmail.com", email_verified: true }).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("private").doc("info").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("private").doc("verification").get());

  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true }).firestore();
  await assertSucceeds(adminDb.collection("users").doc(ALICE_UID).collection("private").doc("info").get());
  await assertFails(adminDb.collection("users").doc(ALICE_UID).collection("private").doc("verification").get());
});

test("Email spoofing test: unverified admin email is rejected for admin writes", async () => {
  const spoofedDb = testEnv.authenticatedContext("spoof_uid", { email: ADMIN_EMAIL, email_verified: false }).firestore();
  const ts = nowTs();
  await assertFails(
    spoofedDb.collection("announcements").doc("ann_1").set({
      title: "Fake Update",
      message: "Spoof attempt",
      versionTag: "v9.9",
      priority: "urgent",
      isActive: true,
      createdBy: "spoof_uid",
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Admin powers: verified admin can publish announcements and update app_config; regular user cannot", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@gmail.com", email_verified: true }).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true }).firestore();
  const ts = nowTs();

  await assertFails(
    aliceDb.collection("app_config").doc("global").set({
      appTagline: "Hacked",
      maintenanceBanner: "",
      isReadOnlyMode: true,
      allowStatusUpdates: false,
      allowGroupCreation: false,
      maxMessageLength: 500,
      minAppVersion: "1.0",
      updatedBy: ALICE_UID,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    adminDb.collection("app_config").doc("global").set({
      appTagline: "Fast, private messaging",
      maintenanceBanner: "",
      isReadOnlyMode: false,
      allowStatusUpdates: true,
      allowGroupCreation: true,
      maxMessageLength: 1000,
      minAppVersion: "1.0",
      updatedBy: ADMIN_UID,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    adminDb.collection("announcements").doc("ann_1").set({
      title: "Alap v1.1 Update",
      message: "Offline message queueing is now live.",
      versionTag: "v1.1",
      priority: "update",
      isActive: true,
      createdBy: ADMIN_UID,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(aliceDb.collection("announcements").where("isActive", "==", true).get());
  await assertFails(aliceDb.collection("announcements").get()); // unfiltered list denied
});

test("Chats and messages: participants can chat and react, non-participants are blocked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = nowTs();
  const chatId = "dm_alice_bob";

  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).set({
      chatType: "direct",
      title: "",
      description: "",
      avatarId: "avatar_1",
      participantUids: [ALICE_UID, BOB_UID],
      adminUids: [],
      createdBy: ALICE_UID,
      lastMessageText: "Hello Bob!",
      lastMessageSenderId: ALICE_UID,
      lastMessageType: "text",
      lastMessageAt: ts,
      typingUids: [],
      pinnedByUids: [],
      mutedByUids: [],
      archivedByUids: [],
      unreadCounts: { [BOB_UID]: 1 },
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).collection("messages").doc("msg_1").set({
      chatId,
      senderId: ALICE_UID,
      senderName: "Alice",
      text: "Hello Bob!",
      messageType: "text",
      isForwarded: false,
      isEdited: false,
      isDeleted: false,
      participantUids: [ALICE_UID, BOB_UID],
      readByUids: [ALICE_UID],
      starredByUids: [],
      reactions: {},
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bob can query and react
  await assertSucceeds(
    bobDb.collection("chats").doc(chatId).collection("messages").where("participantUids", "array-contains", BOB_UID).get()
  );
  await assertSucceeds(
    bobDb.collection("chats").doc(chatId).collection("messages").doc("msg_1").update({
      reactions: { [BOB_UID]: "👍" },
      readByUids: [ALICE_UID, BOB_UID],
      updatedAt: nowTs(),
    })
  );

  // Pre-create existence check on a non-existent deterministic DM doc should succeed cleanly
  await assertSucceeds(aliceDb.collection("chats").doc("dm_alice_bob_nonexistent").get());

  // Charlie cannot read or write in existing Alice-Bob chat
  await assertFails(charlieDb.collection("chats").doc(chatId).get());
  await assertFails(charlieDb.collection("chats").doc(chatId).collection("messages").doc("msg_1").get());
  await assertFails(
    charlieDb.collection("chats").doc(chatId).collection("messages").doc("msg_2").set({
      chatId,
      senderId: CHARLIE_UID,
      senderName: "Charlie",
      text: "Intruder",
      messageType: "text",
      isForwarded: false,
      isEdited: false,
      isDeleted: false,
      participantUids: [ALICE_UID, BOB_UID],
      readByUids: [CHARLIE_UID],
      starredByUids: [],
      reactions: {},
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Statuses: author can post and delete, signed-in users can view and record view", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = nowTs();

  await assertSucceeds(
    aliceDb.collection("statuses").doc("status_1").set({
      userId: ALICE_UID,
      authorName: "Alice",
      authorAvatarId: "avatar_1",
      text: "Enjoying Alap offline!",
      styleKey: "emerald",
      isPublic: true,
      viewerUids: [],
      expiresAtEpochMs: Date.now() + 86400000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(bobDb.collection("statuses").where("isPublic", "==", true).get());
  await assertSucceeds(
    bobDb.collection("statuses").doc("status_1").update({
      viewerUids: [BOB_UID],
      updatedAt: nowTs(),
    })
  );
  // Bob cannot delete Alice's status
  await assertFails(bobDb.collection("statuses").doc("status_1").delete());
  // Alice can delete own status
  await assertSucceeds(aliceDb.collection("statuses").doc("status_1").delete());
});

test("Hardened security: suspended user blocked from messaging, audit logs immutable, reports admin-moderated", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@gmail.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@gmail.com", email_verified: true }).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true }).firestore();
  const ts = nowTs();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(makeUserProfile(ALICE_UID, "Alice")));
  await assertSucceeds(bobDb.collection("users").doc(BOB_UID).set(makeUserProfile(BOB_UID, "Bob")));

  // Admin writes an immutable audit log
  await assertSucceeds(
    adminDb.collection("audit_logs").doc("log_1").set({
      adminUid: ADMIN_UID,
      adminEmail: ADMIN_EMAIL,
      action: "SUSPEND_USER",
      targetId: BOB_UID,
      details: "Test suspension",
      createdAt: ts,
    })
  );
  // Even admin cannot edit or delete an audit log
  await assertFails(adminDb.collection("audit_logs").doc("log_1").update({ details: "Tampered" }));
  await assertFails(adminDb.collection("audit_logs").doc("log_1").delete());
  // Regular user cannot read or write audit logs
  await assertFails(aliceDb.collection("audit_logs").doc("log_1").get());

  // Alice submits a user report against Bob
  await assertSucceeds(
    aliceDb.collection("reports").doc("rep_1").set({
      reporterUid: ALICE_UID,
      targetUid: BOB_UID,
      targetName: "Bob",
      chatId: "dm_alice_bob",
      reason: "Spam links",
      status: "open",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(bobDb.collection("reports").doc("rep_1").get());
  await assertSucceeds(adminDb.collection("reports").doc("rep_1").update({ status: "resolved", updatedAt: nowTs() }));

  // Suspend Bob -> Bob can no longer create a status or chat
  await assertSucceeds(adminDb.collection("users").doc(BOB_UID).update({ isSuspended: true, updatedAt: nowTs() }));
  await assertFails(
    bobDb.collection("statuses").doc("status_bob").set({
      userId: BOB_UID,
      authorName: "Bob",
      authorAvatarId: "avatar_1",
      text: "Should be blocked",
      styleKey: "emerald",
      isPublic: true,
      viewerUids: [],
      expiresAtEpochMs: Date.now() + 86400000,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );
});

test("Admin API Keys: exclusive to verified Primary Admin; regular users and delegated admins denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@gmail.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@gmail.com", email_verified: true }).firestore();
  const primaryAdminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true }).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const ts = nowTs();

  // Grant Bob secondary delegated admin status
  await assertSucceeds(
    primaryAdminDb.collection("admins").doc(BOB_UID).set({
      uid: BOB_UID,
      email: "bob@gmail.com",
      addedBy: ADMIN_UID,
      createdAt: ts,
    })
  );

  // Regular user & delegated secondary admin & unauthenticated user cannot read or create admin_api_keys
  await assertFails(unauthDb.collection("admin_api_keys").get());
  await assertFails(aliceDb.collection("admin_api_keys").get());
  await assertFails(bobDb.collection("admin_api_keys").get());
  await assertFails(
    bobDb.collection("admin_api_keys").doc("key_bob").set({
      label: "Secondary Attempt",
      apiKey: "alap_live_secret_1234567890",
      scopes: ["push:send"],
      isActive: true,
      createdBy: BOB_UID,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Primary Admin can create, list, toggle active, and delete API keys
  await assertSucceeds(
    primaryAdminDb.collection("admin_api_keys").doc("key_netlify_1").set({
      label: "Netlify Admin Panel",
      apiKey: "alap_live_primary_key_9876543210",
      scopes: ["push:send", "config:write"],
      isActive: true,
      createdBy: ADMIN_UID,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(primaryAdminDb.collection("admin_api_keys").get());
  await assertSucceeds(
    primaryAdminDb.collection("admin_api_keys").doc("key_netlify_1").update({
      isActive: false,
      updatedAt: nowTs(),
    })
  );
  await assertSucceeds(primaryAdminDb.collection("admin_api_keys").doc("key_netlify_1").delete());
});

test("Device Push Tokens & AppConfig APK Update fields: owner writes token, admin updates latestApkUrl", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@gmail.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@gmail.com", email_verified: true }).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true }).firestore();
  const ts = nowTs();

  await assertSucceeds(
    aliceDb.collection("device_tokens").doc(ALICE_UID).set({
      userId: ALICE_UID,
      token: "fcm_token_example_abcdef123456",
      platform: "android",
      updatedAt: ts,
    })
  );
  // Bob cannot read or overwrite Alice's push token
  await assertFails(bobDb.collection("device_tokens").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("device_tokens").doc(ALICE_UID).set({
      userId: ALICE_UID,
      token: "hijacked_token_123456789",
      platform: "android",
      updatedAt: ts,
    })
  );
  // Admin can list push tokens for broadcast delivery
  await assertSucceeds(adminDb.collection("device_tokens").get());

  // Admin can publish force update APK URL and version code
  await assertSucceeds(
    adminDb.collection("app_config").doc("global").set({
      appTagline: "Fast, private messaging",
      maintenanceBanner: "",
      isReadOnlyMode: false,
      allowStatusUpdates: true,
      allowGroupCreation: true,
      forceUpdateRequired: true,
      maxMessageLength: 1000,
      minAppVersion: "1.1",
      latestApkUrl: "https://alap-updates.netlify.app/alap-latest.apk",
      latestVersionCode: 2,
      updatedBy: ADMIN_UID,
      updatedAt: ts,
    })
  );
});

test("Local-First Relay purge: delivered message cannot be deleted before read by all, succeeds after read by all", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = nowTs();
  const chatId = "dm_relay_test";

  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).set({
      chatType: "direct",
      title: "",
      description: "",
      avatarId: "avatar_1",
      participantUids: [ALICE_UID, BOB_UID],
      adminUids: [],
      createdBy: ALICE_UID,
      lastMessageText: "Ephemeral relay test",
      lastMessageSenderId: ALICE_UID,
      lastMessageType: "text",
      lastMessageAt: ts,
      typingUids: [],
      pinnedByUids: [],
      mutedByUids: [],
      archivedByUids: [],
      unreadCounts: { [BOB_UID]: 1 },
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).collection("messages").doc("relay_msg_1").set({
      chatId,
      senderId: ALICE_UID,
      senderName: "Alice",
      text: "Save me to Room then delete from server!",
      messageType: "text",
      isForwarded: false,
      isEdited: false,
      isDeleted: false,
      participantUids: [ALICE_UID, BOB_UID],
      readByUids: [ALICE_UID],
      starredByUids: [],
      reactions: {},
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bob cannot purge before Bob has marked the message read
  await assertFails(bobDb.collection("chats").doc(chatId).collection("messages").doc("relay_msg_1").delete());

  // Bob marks read -> all participants have read
  await assertSucceeds(
    bobDb.collection("chats").doc(chatId).collection("messages").doc("relay_msg_1").update({
      readByUids: [ALICE_UID, BOB_UID],
      updatedAt: nowTs(),
    })
  );

  // Outsider Charlie still cannot delete
  await assertFails(charlieDb.collection("chats").doc(chatId).collection("messages").doc("relay_msg_1").delete());

  // Recipient Bob can now purge the delivered relay message after persisting to Room
  await assertSucceeds(bobDb.collection("chats").doc(chatId).collection("messages").doc("relay_msg_1").delete());
});
