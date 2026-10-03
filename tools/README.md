# Command-line tools

All of them run with Gradle from the project root, against the real router. They need:

1. A text file with the user's password on **a single line** (for example `C:\path\router_pwd.txt`).
   Keep it **outside the repository**: `router_pwd.txt` is already in `.gitignore`, but it is better not to store it inside the project.
2. To be on the router's network (`192.168.1.1` by default; the third argument allows another URL, for example `http://192.168.0.1`).
3. **No other administration session open** (browser, app): the router only allows one.

Argument format: `--args="<passwordFile> [user] [routerUrl]"`. The default user is `user`.

## Read-only (safe)

| Task | Purpose |
|---|---|
| `:protocol:run` | Discovery: logs in, downloads the authenticated web UI with GET requests (main page, menu, scripts) to `tools/discovery-out/` (ignored by git) and logs out. Used to build `docs/router_features.md`. |
| `:protocol:probe` | Reads specific data-model objects: `--args="<file> user OID1,OID2"`. Writes `tools/discovery-out/probe/<OID>.json`. |
| `:protocol:pagesCheck` | Loads every section of the catalog (`Catalog.kt`) and reports declared fields the router does not return. |
| `:protocol:dashCheck` | Runs the reads behind the "Summary" and "Devices" screens and tells you which one fails. |

```powershell
.\gradlew.bat :protocol:pagesCheck --args="C:\path\router_pwd.txt user"
```

> `tools/discovery-out/` contains data from the router (serial number, WPS PIN, PPPoE and DDNS credentials…). **Do not publish it or push it to any repository.**

## They write to the router (with automatic rollback)

They change a value, read it back, restore it and verify it. Even so, they **temporarily modify your configuration**: use them only if you know what you are doing, and with your computer connected by cable.

| Task | What it tests |
|---|---|
| `:protocol:writeTest` | Renames the guest SSID and restores it. |
| `:protocol:addDeleteTest` | Creates and deletes a disabled port-forwarding rule. |
| `:protocol:safeTests` | A batch of writes that do not cut the connection (DHCP reservation, port triggering, guest security, LED, NTP, ALG, DMZ, etc.). |

None of these tools perform reboots, factory resets, or changes to the main Wi-Fi or the router's IP.
