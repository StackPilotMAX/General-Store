# Shiv Shakti Kirana Store 3.0.0

Offline-first Android billing and customer-khata app for **Shiv Shakti Kirana Store**.

## What changed for a general store

The vegetable catalogue has been removed from the billing workflow.

For every bill, the shopkeeper can enter a **temporary item**:
- Item name
- Unit
- Quantity
- Price per unit
- Optional direct item total

There is **no permanent item/master catalogue** to maintain. An item entered while making a bill exists only as that bill line after saving; it is never added to a reusable product list.

Common units are available as quick choices: Pcs, Kg, g, L, ml, Packet, Box, Bottle, Dozen and Set. The unit text is still part of the bill line, so the same system can handle groceries, household goods, stationery, bottles, packets, boxes and other general-store items.

## Billing and khata behavior

The original billing/ledger system remains in place:

- Separate customer IDs with name and phone matching
- Opening debt
- New bill creation
- Bill-level payment
- Customer-level receive payment
- Bill editing using the same bill ID
- Difference-based correction so edited bills do not double-charge
- Bill revision audit trail
- Bill voiding and payment reversal
- Permanent bill/customer deletion with balance recalculation
- Offline Room database
- Bill image sharing with optional UPI QR
- JSON backup/restore with SHA-256 checksum
- Existing Room database version remains compatible with earlier General Store/SabziBill data

## APK update behavior

The package/application ID remains:

`com.stackpilotmax.rameshvegetableshop`

That is intentional. It keeps future APKs in the same Android application identity instead of creating a second app that would require uninstalling the old one.

Version is now **3.0.0 / versionCode 14**. Future releases must use a higher versionCode.

The release workflow:
- Keeps the same application ID
- Verifies package name and version before accepting the APK
- Verifies the APK signature
- Retries Gradle test/lint/build commands for transient dependency or cache failures
- Uses a protected `ANDROID_SIGNING_KEYSTORE_B64` repository secret when configured
- Generates a CI-only fallback key when the protected secret is absent, so tests and build verification stay green without publishing a private key
- Uploads the signed APK plus SHA-256 checksum

For an APK that updates over an already-installed APK without uninstalling, configure `ANDROID_SIGNING_KEYSTORE_B64` with the same signing keystore used for the installed app. Keep that keystore out of the repository. The CI fallback intentionally keeps the workflow green, but it is not a substitute for the production signing key.

## CI verification

GitHub Actions runs:
1. Unit tests
2. Release lint
3. Signed release APK build
4. Package/version verification
5. APK signature verification
6. SHA-256 checksum generation
7. Production artifact upload

Artifact naming:

`ShivShaktiKirana-v3.0.0-production.apk`

## Safe first-device smoke test

Use test customers/data before entering live shop accounts.

1. Create Raju with opening debt ₹500.
2. Add a bill with any temporary items, for example Atta 5 Kg at ₹300 and Soap 4 Pcs at ₹160.
3. Confirm the bill total and customer balance.
4. Create Rajesh and verify Raju's balance never appears in Rajesh's account.
5. Edit one item on Raju's bill and confirm the old amount is replaced, not duplicated.
6. Receive a customer payment and then reverse it.
7. Void a test bill and confirm the bill contribution is removed.
8. Share the bill image and verify the UPI QR amount.
9. Export a backup and restore it on the same test device.
10. Install the next APK over the existing test APK and confirm Android offers an update instead of requiring uninstall.
