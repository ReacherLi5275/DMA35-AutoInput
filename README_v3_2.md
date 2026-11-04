# DMA35 Bridge v3.2 — Field Trial

- Real DMA35 Bluetooth Classic SPP receiver; no simulation or local test field.
- Parses **Density@25°C**, not raw density.
- Stores CSV measurements; exports through Android document picker.
- Persistent one-value pending input with explicit INSERT PENDING fallback.
- Live auto-input requires DMA35 Keyboard selected and an editable field focused.
- Default DOWN, optional RIGHT. Excel and Augmentir navigation require device-level verification.
- Re-listens after DMA35 disconnects. Pairing prompts are controlled by Android and cannot be suppressed by the app.
- Duplicate measurement number ignored when present. Equal numeric values are NOT automatically duplicates.
- Never automatically submits Augmentir forms.

## Upgrade / build
Project root is `DMA35AutoInput/`. In GitHub Actions use `working-directory: DMA35AutoInput` for Gradle or run `cd DMA35AutoInput` before `gradle assembleDebug`.

## Production qualification
This is **not validated for controlled production records**. Verify repeated connections, pairing persistence, CSV correctness, field focus, value insertion, navigation, missed measurements, background restrictions and data retention on each target Android/Excel/Augmentir combination. "INSERTED" means Android accepted the text insertion command, not that the target saved it.
