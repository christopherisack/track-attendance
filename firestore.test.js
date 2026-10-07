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

test("Unauthenticated user: cannot read user profile or workers", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("workers").get());
});

test("Authenticated user: Alice can create and read her worker", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const workerDoc = aliceDb.collection("users").doc(ALICE_UID).collection("workers").doc("w1");
  
  await assertSucceeds(
    workerDoc.set({
      id: "w1",
      userId: ALICE_UID,
      empCode: "EMP-101",
      fullName: "Marcus Vance",
      role: "Lead Machinist",
      department: "Operations",
      phone: "+1 555-0100",
      shiftName: "Morning Shift",
      hourlyRate: 25.0,
      avatarColorHex: "#1E3A8A",
      isActive: true,
      createdAt: new Date(),
    })
  );

  await assertSucceeds(workerDoc.get());
});

test("Cross-user isolation: Bob cannot read or write Alice's workers", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const workerDoc = aliceDb.collection("users").doc(ALICE_UID).collection("workers").doc("w1");
  await workerDoc.set({
    id: "w1",
    userId: ALICE_UID,
    empCode: "EMP-101",
    fullName: "Marcus Vance",
    role: "Lead Machinist",
    department: "Operations",
    createdAt: new Date(),
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("workers").doc("w1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("workers").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("workers").doc("w2").set({
      id: "w2",
      userId: BOB_UID,
      empCode: "EMP-999",
      fullName: "Hacker",
      role: "Intruder",
      department: "None",
      createdAt: new Date(),
    })
  );
});

test("Authenticated user: Alice can record attendance and Bob cannot access it", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const attDoc = aliceDb.collection("users").doc(ALICE_UID).collection("attendance_records").doc("att1");

  await assertSucceeds(
    attDoc.set({
      id: "att1",
      userId: ALICE_UID,
      workerId: "w1",
      date: "2026-10-07",
      status: "PRESENT",
      checkInTime: "08:00 AM",
      checkOutTime: "04:30 PM",
      hoursWorked: 8.0,
      overtimeHours: 0.0,
      notes: "On time",
      createdAt: new Date(),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("attendance_records").doc("att1").get());
});
