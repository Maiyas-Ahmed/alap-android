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

test("Student profile fields: valid studentClass/studentSubject allowed; oversized fields rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@gmail.com", email_verified: true }).firestore();
  const profile = {
    ...makeUserProfile(ALICE_UID, "Alice"),
    isStudent: true,
    studentClass: "HSC-26",
    studentSubject: "Physics, Higher Math",
  };
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(profile));

  // Valid update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      studentClass: "CSE 2nd Year",
      studentSubject: "Algorithms, OS",
      updatedAt: nowTs(),
    })
  );

  // Oversized studentClass (>40 chars) rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      studentClass: "A".repeat(41),
      updatedAt: nowTs(),
    })
  );
});

test("Quiz & Flashcard messages: participants can create and vote; outsiders blocked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = nowTs();
  const chatId = "dm_study_quiz";

  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).set({
      chatType: "direct",
      title: "",
      description: "",
      avatarId: "avatar_1",
      participantUids: [ALICE_UID, BOB_UID],
      adminUids: [],
      createdBy: ALICE_UID,
      lastMessageText: "Study started",
      lastMessageSenderId: ALICE_UID,
      lastMessageType: "text",
      lastMessageAt: ts,
      typingUids: [],
      pinnedByUids: [],
      mutedByUids: [],
      archivedByUids: [],
      unreadCounts: {},
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).collection("messages").doc("quiz_1").set({
      chatId,
      senderId: ALICE_UID,
      senderName: "Alice",
      text: "Quiz: Speed of light in vacuum?",
      messageType: "quiz",
      pollQuestion: "Speed of light in vacuum?",
      pollOptions: ["3 x 10^8 m/s", "3 x 10^6 m/s"],
      pollVotes: {},
      correctOptionIndex: 0,
      quizExplanation: "c is approximately 300,000 km/s",
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

  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).collection("messages").doc("card_1").set({
      chatId,
      senderId: ALICE_UID,
      senderName: "Alice",
      text: "Flashcard: Newton's 2nd Law",
      messageType: "flashcard",
      flashcardFront: "State Newton's 2nd Law",
      flashcardBack: "F = ma (Rate of change of momentum is proportional to applied force)",
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

  // Bob votes on quiz
  await assertSucceeds(
    bobDb.collection("chats").doc(chatId).collection("messages").doc("quiz_1").update({
      pollVotes: { [BOB_UID]: 0 },
      updatedAt: nowTs(),
    })
  );

  // Charlie cannot vote or read
  await assertFails(
    charlieDb.collection("chats").doc(chatId).collection("messages").doc("quiz_1").update({
      pollVotes: { [CHARLIE_UID]: 1 },
      updatedAt: nowTs(),
    })
  );
});

test("Study Group Invites: group admin creates invite code, outsider joins safely, non-admin spoof denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = nowTs();
  const chatId = "study_group_hsc";
  const inviteCode = "HSC26A";

  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).set({
      chatType: "group",
      title: "HSC Physics Batch",
      description: "Daily notes and quizzes",
      avatarId: "group_default",
      participantUids: [ALICE_UID, BOB_UID],
      adminUids: [ALICE_UID],
      createdBy: ALICE_UID,
      lastMessageText: "Created group",
      lastMessageSenderId: ALICE_UID,
      lastMessageType: "system",
      lastMessageAt: ts,
      isStudyGroup: true,
      inviteCode,
      typingUids: [],
      pinnedByUids: [],
      mutedByUids: [],
      archivedByUids: [],
      unreadCounts: {},
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Non-admin Bob cannot create a study invite for Alice's group
  await assertFails(
    bobDb.collection("study_invites").doc("FAKE99").set({
      inviteCode: "FAKE99",
      chatId,
      title: "Fake Batch",
      description: "",
      createdBy: BOB_UID,
      isActive: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Admin Alice creates valid study invite
  await assertSucceeds(
    aliceDb.collection("study_invites").doc(inviteCode).set({
      inviteCode,
      chatId,
      title: "HSC Physics Batch",
      description: "Daily notes and quizzes",
      createdBy: ALICE_UID,
      isActive: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Outsider Charlie can read invite code and join group
  await assertSucceeds(charlieDb.collection("study_invites").doc(inviteCode).get());
  await assertSucceeds(
    charlieDb.collection("chats").doc(chatId).update({
      participantUids: [ALICE_UID, BOB_UID, CHARLIE_UID],
      updatedAt: nowTs(),
    })
  );
});

test("Group Study Board: participants create/read notes & exams, creator/admin deletes, outsider & unauthorized member denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = nowTs();
  const chatId = "study_board_chat";

  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).set({
      chatType: "group",
      title: "Math Study Group",
      description: "",
      avatarId: "group_default",
      participantUids: [ALICE_UID, BOB_UID],
      adminUids: [ALICE_UID],
      createdBy: ALICE_UID,
      lastMessageText: "Welcome",
      lastMessageSenderId: ALICE_UID,
      lastMessageType: "system",
      lastMessageAt: ts,
      isStudyGroup: true,
      inviteCode: "MATH26",
      typingUids: [],
      pinnedByUids: [],
      mutedByUids: [],
      archivedByUids: [],
      unreadCounts: {},
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Alice adds an exam countdown item
  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).collection("study_items").doc("exam_1").set({
      chatId,
      itemType: "exam",
      title: "Calculus Midterm",
      details: "Chapters 1 to 4",
      scheduleText: "10:00 AM Room 204",
      examDateEpochMs: Date.now() + 86400000 * 5,
      createdBy: ALICE_UID,
      createdByName: "Alice",
      participantUids: [ALICE_UID, BOB_UID],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Participant Bob can read and add a note
  await assertSucceeds(bobDb.collection("chats").doc(chatId).collection("study_items").get());
  await assertSucceeds(
    bobDb.collection("chats").doc(chatId).collection("study_items").doc("note_1").set({
      chatId,
      itemType: "note",
      title: "Integration Formulas",
      details: "Remember by-parts rule",
      scheduleText: "",
      examDateEpochMs: 0,
      createdBy: BOB_UID,
      createdByName: "Bob",
      participantUids: [ALICE_UID, BOB_UID],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Non-creator & non-admin Bob cannot delete Alice's exam item
  await assertFails(bobDb.collection("chats").doc(chatId).collection("study_items").doc("exam_1").delete());

  // Outsider Charlie cannot read or write study items
  await assertFails(charlieDb.collection("chats").doc(chatId).collection("study_items").get());
  await assertFails(
    charlieDb.collection("chats").doc(chatId).collection("study_items").doc("intrude_1").set({
      chatId,
      itemType: "note",
      title: "Spam",
      details: "",
      scheduleText: "",
      examDateEpochMs: 0,
      createdBy: CHARLIE_UID,
      createdByName: "Charlie",
      participantUids: [ALICE_UID, BOB_UID],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Group admin Alice can delete Bob's note, and creator Bob can delete own note if recreated
  await assertSucceeds(aliceDb.collection("chats").doc(chatId).collection("study_items").doc("note_1").delete());
});

test("Account Roles & Age-Lock: student account locked from switching to personal; personal can toggle isStudent; only Admin grants Teacher", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@gmail.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@gmail.com", email_verified: true }).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true }).firestore();

  // Regular user cannot self-grant isTeacher or accountType = 'teacher'
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      ...makeUserProfile(ALICE_UID, "Alice"),
      accountType: "teacher",
      isTeacher: true,
    })
  );

  // Alice completes profile as a Locked Student Account
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      ...makeUserProfile(ALICE_UID, "Alice Student"),
      accountType: "student",
      isStudent: true,
      isTeacher: false,
      teacherSubject: "",
    })
  );

  // Alice cannot switch locked student account back to 'personal' or turn off isStudent
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      accountType: "personal",
      isStudent: false,
      updatedAt: nowTs(),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isStudent: false,
      updatedAt: nowTs(),
    })
  );

  // Alice cannot self-promote to teacher on update either
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isTeacher: true,
      accountType: "teacher",
      updatedAt: nowTs(),
    })
  );

  // Verified Admin CAN grant Teacher badge and status
  await assertSucceeds(
    adminDb.collection("users").doc(ALICE_UID).update({
      isTeacher: true,
      accountType: "teacher",
      teacherSubject: "Higher Math",
      updatedAt: nowTs(),
    })
  );

  // Adult Bob creates a Personal account and can freely toggle isStudent ON and OFF
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).set({
      ...makeUserProfile(BOB_UID, "Bob Personal"),
      accountType: "personal",
      isStudent: false,
      isTeacher: false,
      teacherSubject: "",
    })
  );
  await assertSucceeds(bobDb.collection("users").doc(BOB_UID).update({ isStudent: true, updatedAt: nowTs() }));
  await assertSucceeds(bobDb.collection("users").doc(BOB_UID).update({ isStudent: false, updatedAt: nowTs() }));
});

test("Group studyLockToAdmin: blocks non-admin from adding study_items, quizzes, or flashcards when locked; allows Group Admin", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = nowTs();
  const chatId = "locked_study_group";

  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).set({
      chatType: "group",
      title: "Teacher Physics Class",
      description: "Admin locked class",
      avatarId: "group_default",
      participantUids: [ALICE_UID, BOB_UID],
      adminUids: [ALICE_UID],
      createdBy: ALICE_UID,
      lastMessageText: "Class opened",
      lastMessageSenderId: ALICE_UID,
      lastMessageType: "system",
      lastMessageAt: ts,
      isStudyGroup: true,
      studyLockToAdmin: true,
      inviteCode: "PHY101",
      typingUids: [],
      pinnedByUids: [],
      mutedByUids: [],
      archivedByUids: [],
      unreadCounts: {},
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Non-admin student Bob CANNOT add study_items, quiz, or flashcard
  await assertFails(
    bobDb.collection("chats").doc(chatId).collection("study_items").doc("bob_note").set({
      chatId,
      itemType: "note",
      title: "Unauthorized note",
      details: "",
      scheduleText: "",
      examDateEpochMs: 0,
      createdBy: BOB_UID,
      createdByName: "Bob",
      participantUids: [ALICE_UID, BOB_UID],
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    bobDb.collection("chats").doc(chatId).collection("messages").doc("bob_quiz").set({
      chatId,
      senderId: BOB_UID,
      senderName: "Bob",
      text: "Quiz attempt",
      messageType: "quiz",
      pollQuestion: "Q?",
      pollOptions: ["A", "B"],
      pollVotes: {},
      correctOptionIndex: 0,
      quizExplanation: "",
      isForwarded: false,
      isEdited: false,
      isDeleted: false,
      participantUids: [ALICE_UID, BOB_UID],
      readByUids: [BOB_UID],
      starredByUids: [],
      reactions: {},
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    bobDb.collection("chats").doc(chatId).collection("messages").doc("bob_card").set({
      chatId,
      senderId: BOB_UID,
      senderName: "Bob",
      text: "Flashcard attempt",
      messageType: "flashcard",
      flashcardFront: "Front",
      flashcardBack: "Back",
      isForwarded: false,
      isEdited: false,
      isDeleted: false,
      participantUids: [ALICE_UID, BOB_UID],
      readByUids: [BOB_UID],
      starredByUids: [],
      reactions: {},
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Non-admin Bob CAN still send normal chat messages
  await assertSucceeds(
    bobDb.collection("chats").doc(chatId).collection("messages").doc("bob_text").set({
      chatId,
      senderId: BOB_UID,
      senderName: "Bob",
      text: "Sir, when is the next test?",
      messageType: "text",
      isForwarded: false,
      isEdited: false,
      isDeleted: false,
      participantUids: [ALICE_UID, BOB_UID],
      readByUids: [BOB_UID],
      starredByUids: [],
      reactions: {},
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Group Admin Alice CAN add study_items and post quiz
  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).collection("study_items").doc("teacher_note").set({
      chatId,
      itemType: "note",
      title: "Chapter 1 Formulas",
      details: "Official notes",
      scheduleText: "",
      examDateEpochMs: 0,
      createdBy: ALICE_UID,
      createdByName: "Alice",
      participantUids: [ALICE_UID, BOB_UID],
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).collection("messages").doc("teacher_quiz").set({
      chatId,
      senderId: ALICE_UID,
      senderName: "Alice",
      text: "Quiz: G?",
      messageType: "quiz",
      pollQuestion: "Value of g?",
      pollOptions: ["9.8 m/s^2", "10.8 m/s^2"],
      pollVotes: {},
      correctOptionIndex: 0,
      quizExplanation: "Standard gravity",
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
});

test("Teacher Plans & Billing Ledger: admin manages plans and assigns teacher plans; regular users cannot self-assign", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@gmail.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@gmail.com", email_verified: true }).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true }).firestore();
  const ts = nowTs();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      ...makeUserProfile(ALICE_UID, "Alice"),
      accountType: "personal",
      isTeacher: false,
    })
  );

  // Regular user cannot create a teacher plan or billing record
  await assertFails(
    aliceDb.collection("teacher_plans").doc("monthly_pro").set({
      planId: "monthly_pro",
      name: "Teacher Pro Monthly",
      billingCycle: "monthly",
      priceBdt: 499,
      maxStudyGroups: 20,
      maxStudentsPerGroup: 200,
      features: ["TR Badge", "Up to 20 Study Groups"],
      isActive: true,
      sortOrder: 1,
      createdBy: ALICE_UID,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Admin creates monthly teacher plan
  await assertSucceeds(
    adminDb.collection("teacher_plans").doc("monthly_pro").set({
      planId: "monthly_pro",
      name: "Teacher Pro Monthly",
      billingCycle: "monthly",
      priceBdt: 499,
      priceUsd: 5,
      maxStudyGroups: 20,
      maxStudentsPerGroup: 200,
      features: ["Verified TR Badge", "Up to 20 Study Groups", "Admin-Locked Quizzes"],
      isActive: true,
      sortOrder: 1,
      createdBy: ADMIN_UID,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Signed-in teacher/user can read plans, only admin can assign plan to user
  await assertSucceeds(aliceDb.collection("teacher_plans").doc("monthly_pro").get());
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      teacherPlanId: "monthly_pro",
      teacherSubscriptionStatus: "active",
      updatedAt: nowTs(),
    })
  );

  await assertSucceeds(
    adminDb.collection("users").doc(ALICE_UID).update({
      isTeacher: true,
      accountType: "teacher",
      teacherSubject: "Physics",
      teacherPlanId: "monthly_pro",
      teacherPlanName: "Teacher Pro Monthly",
      teacherBillingCycle: "monthly",
      teacherSubscriptionStatus: "free_launch",
      teacherPlanExpiresAtEpochMs: 0,
      updatedAt: nowTs(),
    })
  );

  // Admin writes manual billing ledger record; Alice (owner) can read hers, Bob cannot read Alice's
  await assertSucceeds(
    adminDb.collection("teacher_billing_records").doc("rec_1").set({
      teacherUid: ALICE_UID,
      teacherName: "Alice",
      planId: "monthly_pro",
      planName: "Teacher Pro Monthly",
      billingCycle: "monthly",
      amountBdt: 0,
      paymentMethod: "free_launch",
      referenceNote: "Free early creator phase",
      status: "waived",
      periodStartEpochMs: Date.now(),
      periodEndEpochMs: Date.now() + 86400000 * 30,
      recordedBy: ADMIN_UID,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(aliceDb.collection("teacher_billing_records").doc("rec_1").get());
  await assertFails(bobDb.collection("teacher_billing_records").doc("rec_1").get());
});

test("Guest accounts: anti-fraud transparency enforced and Guests can only chat with other Guests", async () => {
  const GUEST_1_UID = "guest_uid_001";
  const GUEST_2_UID = "guest_uid_002";
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@gmail.com", email_verified: true }).firestore();
  const guest1Db = testEnv.authenticatedContext(GUEST_1_UID, { email: "guest1@gmail.com", email_verified: true }).firestore();
  const guest2Db = testEnv.authenticatedContext(GUEST_2_UID, { email: "guest2@gmail.com", email_verified: true }).firestore();
  const ts = nowTs();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      ...makeUserProfile(ALICE_UID, "Alice"),
      accountType: "personal",
    })
  );

  // Guest cannot spoof another Gmail in guestEmail
  await assertFails(
    guest1Db.collection("users").doc(GUEST_1_UID).set({
      ...makeUserProfile(GUEST_1_UID, "Guest"),
      handle: "",
      publicUserId: "",
      hidePhoneNumber: false,
      hideUserId: false,
      hideOnlineStatus: false,
      hideAbout: false,
      accountType: "guest",
      guestEmail: "spoofed@gmail.com",
      guestDeviceInfo: "Pixel 8 • Android 14",
      guestJoinedAtEpochMs: Date.now(),
    })
  );

  // Guest cannot hide online status or about info
  await assertFails(
    guest1Db.collection("users").doc(GUEST_1_UID).set({
      ...makeUserProfile(GUEST_1_UID, "Guest"),
      handle: "",
      publicUserId: "",
      hidePhoneNumber: false,
      hideUserId: false,
      hideOnlineStatus: true,
      hideAbout: false,
      accountType: "guest",
      guestEmail: "guest1@gmail.com",
      guestDeviceInfo: "Pixel 8 • Android 14",
      guestJoinedAtEpochMs: Date.now(),
    })
  );

  // Valid Guest 1 & Guest 2 profiles succeed with empty handle & full transparency
  await assertSucceeds(
    guest1Db.collection("users").doc(GUEST_1_UID).set({
      ...makeUserProfile(GUEST_1_UID, "Guest"),
      handle: "",
      publicUserId: "",
      hidePhoneNumber: false,
      hideUserId: false,
      hideOnlineStatus: false,
      hideAbout: false,
      accountType: "guest",
      guestEmail: "guest1@gmail.com",
      guestDeviceInfo: "Pixel 8 • Android 14",
      guestJoinedAtEpochMs: Date.now(),
    })
  );
  await assertSucceeds(
    guest2Db.collection("users").doc(GUEST_2_UID).set({
      ...makeUserProfile(GUEST_2_UID, "Guest"),
      handle: "",
      publicUserId: "",
      hidePhoneNumber: false,
      hideUserId: false,
      hideOnlineStatus: false,
      hideAbout: false,
      accountType: "guest",
      guestEmail: "guest2@gmail.com",
      guestDeviceInfo: "Samsung S24 • Android 14",
      guestJoinedAtEpochMs: Date.now(),
    })
  );

  // Regular user Alice can read Guest 1's full transparency info
  await assertSucceeds(aliceDb.collection("users").doc(GUEST_1_UID).get());

  // Guest 1 CANNOT start a direct chat with regular user Alice (and Alice cannot start one with Guest 1)
  await assertFails(
    guest1Db.collection("chats").doc("dm_guest1_alice").set({
      chatType: "direct",
      title: "",
      description: "",
      avatarId: "avatar_emerald",
      participantUids: [GUEST_1_UID, ALICE_UID],
      adminUids: [],
      createdBy: GUEST_1_UID,
      lastMessageText: "Hi Alice",
      lastMessageSenderId: GUEST_1_UID,
      lastMessageType: "text",
      lastMessageAt: ts,
      typingUids: [],
      pinnedByUids: [],
      mutedByUids: [],
      archivedByUids: [],
      unreadCounts: {},
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    aliceDb.collection("chats").doc("dm_alice_guest1").set({
      chatType: "direct",
      title: "",
      description: "",
      avatarId: "avatar_emerald",
      participantUids: [ALICE_UID, GUEST_1_UID],
      adminUids: [],
      createdBy: ALICE_UID,
      lastMessageText: "Hi Guest",
      lastMessageSenderId: ALICE_UID,
      lastMessageType: "text",
      lastMessageAt: ts,
      typingUids: [],
      pinnedByUids: [],
      mutedByUids: [],
      archivedByUids: [],
      unreadCounts: {},
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Guest 1 CANNOT create a group chat or post a status
  await assertFails(
    guest1Db.collection("chats").doc("grp_guest").set({
      chatType: "group",
      title: "Guest Group",
      description: "",
      avatarId: "group_default",
      participantUids: [GUEST_1_UID, GUEST_2_UID],
      adminUids: [GUEST_1_UID],
      createdBy: GUEST_1_UID,
      lastMessageText: "Hi",
      lastMessageSenderId: GUEST_1_UID,
      lastMessageType: "system",
      lastMessageAt: ts,
      typingUids: [],
      pinnedByUids: [],
      mutedByUids: [],
      archivedByUids: [],
      unreadCounts: {},
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    guest1Db.collection("statuses").doc("st_guest").set({
      userId: GUEST_1_UID,
      authorName: "Guest",
      authorAvatarId: "avatar_emerald",
      text: "Hello",
      styleKey: "emerald",
      isPublic: true,
      viewerUids: [],
      expiresAtEpochMs: Date.now() + 86400000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Guest 1 CAN chat and exchange messages with Guest 2
  await assertSucceeds(
    guest1Db.collection("chats").doc("dm_guest1_guest2").set({
      chatType: "direct",
      title: "",
      description: "",
      avatarId: "avatar_emerald",
      participantUids: [GUEST_1_UID, GUEST_2_UID],
      adminUids: [],
      createdBy: GUEST_1_UID,
      lastMessageText: "Hello Guest 2!",
      lastMessageSenderId: GUEST_1_UID,
      lastMessageType: "text",
      lastMessageAt: ts,
      typingUids: [],
      pinnedByUids: [],
      mutedByUids: [],
      archivedByUids: [],
      unreadCounts: { [GUEST_2_UID]: 1 },
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    guest1Db.collection("chats").doc("dm_guest1_guest2").collection("messages").doc("m1").set({
      chatId: "dm_guest1_guest2",
      senderId: GUEST_1_UID,
      senderName: "Guest",
      text: "Hello Guest 2!",
      messageType: "text",
      isForwarded: false,
      isEdited: false,
      isDeleted: false,
      participantUids: [GUEST_1_UID, GUEST_2_UID],
      readByUids: [GUEST_1_UID],
      starredByUids: [],
      reactions: {},
      createdAt: ts,
      updatedAt: ts,
    })
  );
});



