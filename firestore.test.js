const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("Unauthenticated user cannot read or write user accounts, businesses, clients, interactions, or transactions", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.doc(`users/${ALICE_UID}`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/businesses`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/clients`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/interactions`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/transactions`).get());
});

test("Authenticated user can create, read, and update their own UserAccount profile, and Bob cannot read it", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}`).set({
      userId: ALICE_UID,
      displayName: "Alice Vance",
      email: "alice@nasstudio.com",
      studioRole: "Lead Photographer & Owner",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(aliceDb.doc(`users/${ALICE_UID}`).get());
  await assertFails(bobDb.doc(`users/${ALICE_UID}`).get());

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}`).update({
      displayName: "Alice Vance Studio",
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user can create and read their own business profile (including high-value studio pricing), and Bob cannot read it", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/businesses/biz_1`).set({
      userId: ALICE_UID,
      name: "GLOD PHOTO",
      category: "Weddings & Events",
      defaultPricePerPicture: 3000.0,
      monthlyTarget: 150000.0,
      accentColorHex: "#6366F1",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/businesses`).get());
  await assertFails(bobDb.doc(`users/${ALICE_UID}/businesses/biz_1`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/businesses`).get());
});

test("Shadow update with ghost field on business profile is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/businesses/biz_ghost`).set({
      userId: ALICE_UID,
      name: "NAS Studio",
      category: "Portraits",
      defaultPricePerPicture: 10.0,
      monthlyTarget: 3000.0,
      accentColorHex: "#10B981",
      isVerifiedAdmin: true,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Authenticated user can create client, interaction, and transaction with valid schema", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/clients/client_1`).set({
      userId: ALICE_UID,
      businessId: "biz_1",
      name: "Sarah Jenkins",
      phoneNumber: "+1 555-234-5678",
      email: "sarah@example.com",
      notes: "Prefers natural outdoor lighting",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/interactions/int_1`).set({
      userId: ALICE_UID,
      businessId: "biz_1",
      clientId: "client_1",
      clientName: "Sarah Jenkins",
      type: "CALL",
      summary: "Confirmed golden hour shoot package and 30 edited portraits.",
      interactionDateMillis: Date.now(),
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/transactions/tx_1`).set({
      userId: ALICE_UID,
      businessId: "biz_1",
      clientId: "client_1",
      clientName: "Sarah Jenkins",
      customerPhone: "+1 555-234-5678",
      transactionDateMillis: Date.now(),
      yearMonth: "2026-11",
      pictureCount: 5,
      pricePerPicture: 3000.0,
      totalAmount: 15000.0,
      paymentMethod: "BANK_TRANSFER",
      notes: "Full session payment via wire transfer",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/transactions/tx_1`).update({
      pictureCount: 10,
      totalAmount: 30000.0,
      notes: "Adjusted price for 5 additional pictures",
      updatedAt: new Date(),
    })
  );
});

test("Invalid paymentMethod enum or negative pictureCount is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/transactions/tx_bad`).set({
      userId: ALICE_UID,
      businessId: "biz_1",
      clientId: "client_1",
      clientName: "Sarah Jenkins",
      customerPhone: "+1 555-234-5678",
      transactionDateMillis: Date.now(),
      yearMonth: "2026-11",
      pictureCount: 0,
      pricePerPicture: 15.0,
      totalAmount: 0.0,
      paymentMethod: "CRYPTO",
      notes: "",
      createdAt: now,
      updatedAt: now,
    })
  );
});
