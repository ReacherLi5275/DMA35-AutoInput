# DMA35 Bridge v3.1 — Pilot validation build

## Purpose
Receive Anton Paar DMA35 Bluetooth Classic RFCOMM SPP printer output; parse `Density@25°C`, append local CSV records and optionally insert the number into the focused Android text field using the DMA35 input method.

## Operational setup
1. Install the APK on an authorized Android test device.
2. Grant Bluetooth permission. Open the app and tap **Start Bluetooth receiver** once. Confirm foreground notification.
3. In Android keyboard settings, enable **DMA35 Keyboard** and select it as the current input method.
4. In DMA35 printer setup, choose Bluetooth Printer, connect to this device's SPP receiver. Ensure the phone is discoverable while pairing.
5. Open an editable cell or Augmentir density field and keep it focused. Set **Automatic input** on, **Advance after input** on, and **DOWN** (default) or **RIGHT**.
6. Send one real measurement. Check parsed density, entered value, destination field, and the next focus location. Confirm no form submission occurs.
7. **Export CSV for Excel** saves a snapshot using the Android document picker. It does not write directly to an open Excel workbook.

## Important limitations
- The instrument must actually transmit a record. The app cannot make the DMA35 Save button transmit if the firmware has no save-to-print option.
- Automatic input into another app depends on Android InputConnection and that app's field implementation. The keyboard must be the selected IME and an editable field must be focused; hiding the keyboard view does not intentionally disable auto mode, but Android may disconnect the input session when no field is focused.
- DOWN uses DPAD_DOWN; RIGHT uses TAB. These are *navigation requests*, not guaranteed spatial row/column selection. Confirm with the actual Augmentir form or Excel sheet. No automatic form submission.
- Simulation has been removed. Only incoming DMA35 transmissions are accepted.
- This is a pilot validation build, **not approved for production**. Site QA/IT must validate accuracy, traceability, field ordering, repeated transmissions, interruptions, MDM permissions, and failure recovery before authorizing controlled production records.
- CSV is local app storage; export periodically under an approved retention and backup procedure. No direct Augmentir API or live XLSX editing.

## GitHub Actions
Keep your existing `.github/workflows/build-apk.yml` and replace the `DMA35AutoInput` directory with the directory from this archive. Commit and push. The workflow should build the APK as before.
