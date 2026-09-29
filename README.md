# SabziBill 2.2.1

Offline-first Android billing and customer-khata app for **Ramesh Vegetable Shop**.

## Production capabilities

- Scroll World-inspired native Compose journey with a reduced-motion option
- Large, senior-friendly controls and clear Hindi/Hinglish guidance
- Separate permanent customer IDs, normalized names and phone matching
- Opening debt, bill charges, bill-level payments and customer payments
- Difference-based bill correction: the old bill charge is replaced, never duplicated
- Bill revision audit trail and reversible bill voiding
- Reversible customer payment entries with overpayment protection
- Custom quantity, rate or direct item total for every vegetable
- Branded Canvas bill image with bill debt snapshot and optional UPI QR
- WhatsApp/general sharing from private temporary cache; no permanent Gallery image
- Checksummed JSON backup and confirmed full restore through Android's file picker
- Safe Room v1 → v2 migration without destructive fallback
- Same package ID and stable direct-distribution signature for future data-preserving APK updates

## Version 2.2.1 payment + voice reliability fix

- Customer cards reserve proper width for **Receive Payment** and **Payments**, so the Payments label no longer collapses one letter per line on narrow phones.
- Edit/Delete controls sit on a separate compact row, leaving both payment actions readable.
- Voice now has a three-stage recovery path: **on-device recognizer → normal Android recognizer → system speech input dialog**.
- Missing Hindi offline models and common vendor speech-service failures automatically move to the next available recognition path instead of leaving voice unusable.
- Results from the system speech fallback return to SabziBill and still open the matched vegetable's **Rate & Qty** dialog automatically.
- Version code 7 uses the same package ID and stable signing identity so this APK installs as an update over prior direct-download versions without clearing local data.

## Version 2.2 voice selection

- **Bolkar Sabzi** requests microphone permission when needed and listens inside the billing screen.
- On-device recognition is preferred on supported phones; the regular Android speech service is used as fallback.
- Hindi, Marathi, Hinglish and common English shop names are matched across multiple recognition hypotheses.
- Maharashtra aliases include examples such as **शेपू**, **हिरवा कांदा**, **कोथिंबीर**, **ढोबळी मिरची**, **लिंबू**, **काकडी** and **खीरा**.
- Minor speech-to-text spelling mistakes are handled with conservative fuzzy matching.
- After a vegetable matches, its unit filter is selected and its **Rate & Qty** dialog opens automatically.
- Partial speech and actionable permission, timeout, language-pack and recognizer error states are shown below the mic button.
- Android 13+ receives catalogue biasing phrases so known vegetable names are ranked higher when the speech engine supports biasing.

A compatible speech path is still required on the phone. SabziBill 2.2.1 can fall back to the phone's system speech-input activity when the embedded recognizers cannot complete the request; manual vegetable selection always remains available.

## Version 2.1 controls

- Customer cards include **Delete Customer** with a confirmation dialog.
- Deleting a customer removes that customer account and payment history. Historical bills remain in Bill Archive but are detached from the deleted khata.
- Every archived bill includes **Delete Bill**. Permanent deletion also removes its items and revision history, then recalculates the linked customer's balance.
- The Android system Back button returns to the previous app page. On the Home page, Back exits normally.

## Customer-ledger invariant

Every bill belongs to exactly one customer ID and contributes one active outstanding charge.

Example:

```text
Raju previous debt: ₹500
Raju vegetable bill: ₹250
Raju total: ₹750

Rajesh is a separate customer.
Rajesh never inherits Raju's ₹500.
```

Editing a ₹250 bill to ₹300 replaces the original ₹250 contribution. It changes the customer's balance by ₹50; it does not add another ₹300.

## Backup and restore

Before using the app for live shop accounts:

1. Open **Settings → Backup**.
2. Export a backup to a private folder or trusted drive.
3. Keep at least one recent copy outside the phone.

Restore validates the backup format and SHA-256 checksum before replacing local data. After restore, close and reopen the app so every screen reloads the restored database and settings.

## Vegetable catalogue

**Bunch:** Methi, Palak, Soya, Hara Kanda, China Kothmir, Pudina, Chaulai, Mooli ke Patte, Desi Kothmir and Mooli.

**Kilogram:** Hari Mirch, Naya Adrak, Purana Adrak, Kadi Patta, Nimbu, Gobhi, Shimla Mirch, Chota Lahsun, Bada Lahsun and Kakdi.

## Production build gates

GitHub Actions must pass all of these before publishing the APK:

- Ledger and multilingual voice-matching regression unit tests
- Release Android lint
- Signed release compilation
- APK signature verification
- SHA-256 checksum generation

The artifact is named **SabziBill-v2.2.1-production** under the successful **SabziBill Production Build** workflow.

## First-device smoke test

Use sample customers before entering real shop accounts:

1. Create Raju with opening debt ₹500 and save a ₹250 bill. Confirm ₹750.
2. Start a fresh bill for Rajesh. Confirm that Raju's ₹500 does not appear.
3. Edit Raju's ₹250 bill to ₹300. Confirm Raju becomes ₹800, not ₹1,050.
4. Open Customer Khata and confirm **Receive Payment** and **Payments** are readable on one line.
5. Tap **Bolkar Sabzi**, allow microphone permission and say “मेथी”. Confirm the Methi Rate & Qty dialog opens.
6. Test “शेपू”, “ढोबळी मिरची”, “हरी मिर्च” and “खीरा”. Confirm the correct menus open.
7. Receive and reverse a payment.
8. Void a sample bill and confirm its outstanding contribution is removed.
9. Share the Canvas bill image and scan the configured UPI QR.
10. Export a backup, add sample data, restore the backup and reopen the app.

Only move real shop records into the app after this device-level smoke test passes.

## Version 2.3 Diwali Edition

- Ultra-animated Diwali festival frame across Home, Billing, Khata, Archive and Settings/QR, while respecting the app motion toggle.
- Warm saffron, maroon, purple and gold Material palette plus festive world-card gradients.
- Billing now includes typo-tolerant vegetable search. Wrong names such as `meti`, `shimla mirh`, `kakadi` and `hara knda` surface the nearest catalogue matches; tapping a result opens Rate & Qty.
- UPI QR preview is Diwali-themed and includes **QR Phone Mein Save Karein**, using Android's system file picker so no broad storage permission is required.
- WhatsApp/general bill sharing sends only the generated bill image. No duplicate caption, bill number or extra message is attached.
- The generated bill artwork also uses the Diwali palette, without changing totals, debt snapshots, bill IDs or ledger calculations.
- Version code 8 keeps the same application ID and stable direct-distribution signing identity, so v2.3.0 updates prior direct-download APKs without clearing Room data.
