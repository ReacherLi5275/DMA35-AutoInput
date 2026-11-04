# DMA35 Bridge v3.3 – Bluetooth diagnostics field build

Changes: close active RFCOMM client on Stop, show connection count, received byte count, last connection, parse result, and errors. Keep secure SPP server UUID, CSV, IME, automatic input, DOWN/RIGHT navigation. Existing measurement number de-duplication remains in place.

**Important:** `Listening` is not `Connected`. `Printing finished` on the instrument does not prove that Android received the data. A system pairing prompt cannot be automatically dismissed by this application. This is an unvalidated field diagnostic build, not a production release. Build using GitHub Actions `Build DMA35 APK` and inspect the new diagnostics after two prints. Do not delete pairing again before recording diagnostics.
