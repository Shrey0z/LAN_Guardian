# LAN GUARDIAN - USERS GUIDE

A beginner's guide to understanding, running, and using LAN Guardian: a
two-part system that watches your local network for rogue devices and
ARP-spoofing attacks, and alerts you in real time on your phone.

---

## 1. What This App Actually Is

LAN Guardian has **two separate parts that talk to each other**:

| Part | What it is | Where it runs |
|---|---|---|
| **Detection Engine** | A Python program that watches network traffic and uses a machine learning model to spot suspicious devices | Your PC/laptop |
| **Android Dashboard** | The app on your phone (or emulator) that displays alerts as they happen | Your phone/emulator |

They connect to each other over Wi-Fi using something called a
**WebSocket** — think of it as a live phone line between your PC and your
phone. The PC does the detective work; the phone just shows you what it found.

**Neither half works alone.** The Python engine has no screen — it just
prints to a terminal. The Android app has no detection logic — it just
displays whatever it's told. You need both running to see anything happen.

---

## 2. What It's For

Real intrusion detection systems (IDS) usually watch internet traffic.
LAN Guardian instead watches your **local network** — the same Wi-Fi your
other devices are on — for two specific red flags:

- **A device you've never seen before joins the network**
- **ARP spoofing** — a technique where a malicious device pretends to be a
  device it isn't (e.g., pretending to be your router, to intercept traffic)

When either happens, you get an alert on your phone within seconds.

---

## 3. Before You Start: Two Ways to Run It

### Option A — Demo Mode (recommended for your first run)
No real network monitoring. The Python engine fakes a rogue-device attack
every ~10 seconds, purely so you can see the whole pipeline work without
needing a real attack to test against. **Start here.**

### Option B — Live Mode
Real ARP sniffing on your actual Wi-Fi network. Requires admin/root
privileges and a bit more setup. Move to this only after Demo Mode works.

---

## 4. Starting the Detection Engine (PC side)

1. Open a terminal/Command Prompt on your PC.
2. Navigate to the `detection-engine` folder.
3. Install dependencies (only needed once):
   ```
   pip install -r requirements.txt
   ```
4. Start it in demo mode:
   ```
   python3 alert_server.py --demo
   ```
5. You should see something like:
   ```
   [server] Training baseline model on simulated normal traffic...
   [server] Model trained.
   [server] WebSocket server starting on ws://0.0.0.0:8765
   ```
   **Leave this terminal window open.** Closing it stops the whole system.

---

## 5. Connecting the Android App (phone/emulator side)

The app needs to know *where* to find your PC on the network. This is the
single most common thing that goes wrong, so pay attention to which
situation applies to you:

| You're running the app on... | Use this server address in `MainActivity.kt` |
|---|---|
| An **emulator** in Android Studio, on the same PC running the Python engine | `ws://10.0.2.2:8765` |
| A **real physical phone**, connected to the same Wi-Fi as your PC | `ws://<your PC's LAN IP>:8765` (find it with `ipconfig` on Windows, `ifconfig` on Mac/Linux) |

After changing this line, rebuild and re-run the app.

**Once connected correctly**, the app header will change from:
- 🔴 Disconnected - retrying... → **🟢 Connected to detection engine**

If it stays red for more than 10-15 seconds, see the Troubleshooting
section below.

---

## 6. Using the App Day-to-Day

Once both sides are running and connected, using it is simple — there's
nothing to configure or click. It's a passive monitor:

- **New alert cards appear at the top of the list** as they happen, newest first.
- Each card shows:
  - **Severity** — WARNING (yellow) or CRITICAL (red)
  - **MAC address → IP address** — which device triggered the alert
  - **Explanation** — a plain-English reason (e.g. "IP address claimed by multiple MAC addresses")
  - **Timestamp** — when it happened
- **CRITICAL alerts also trigger a phone notification**, even if the app
  isn't open — so you'll know even with your phone in your pocket.
- You don't need to do anything with an alert to dismiss it — it just
  stays in the scrollable list as a log.

In Demo Mode, expect one new alert roughly every 10 seconds — that's
intentional, not a bug, so you can watch it work.

---

## 7. Switching to Live Mode (real detection)

Once Demo Mode is working end-to-end, you can point it at your real
network:

```
sudo python3 alert_server.py --live --interface wlan0
```

Replace `wlan0` with your actual network interface name (find it with
`ip a` on Linux or your OS's equivalent). This requires admin/root
privileges because raw packet sniffing is an OS-restricted capability.

**⚠️ Only run this against a network you own or have explicit permission
to monitor.** Running network sniffing tools on networks you don't control
can be illegal, even for a security project. Test it on your home Wi-Fi,
not a college/public/work network without permission.

---

## 8. Troubleshooting

| Symptom | Likely Cause | Fix |
|---|---|---|
| App stuck on 🔴 Disconnected | Python server isn't running | Start `alert_server.py --demo` on your PC first |
| Stuck on 🔴 even with server running | Wrong server address for emulator vs. real phone | Re-check the table in Section 5 |
| Stuck on 🔴, address looks correct | Windows Firewall blocking the connection | Allow Python through Firewall (Private networks) when prompted, or manually in Windows Security settings |
| Build error: "Content is not allowed in prolog/trailing section" | A layout XML file has duplicated or leftover content from a partial paste | Select all (Ctrl+A), delete, and paste the correct file content fresh, once |
| App won't build, red squiggly on `package` line | Project package name doesn't match `com.shrey.languardian` exactly | Fix the mismatch — every file's `package` line must match |
| Real phone can't connect but emulator could | Using `10.0.2.2` on a real phone (that only works for emulators) | Switch to your PC's actual `192.168.x.x` LAN IP |
| No alerts ever appear, even though 🟢 Connected | In `--live` mode, no real suspicious traffic has actually occurred yet | This is correct behavior — live mode only alerts on real anomalies. Use `--demo` to verify the pipeline works first |

---

## 9. Quick Glossary (for your own understanding, and for interviews)

- **ARP (Address Resolution Protocol):** how devices on a LAN figure out
  which physical device (MAC address) owns which IP address. Attackers
  exploit this by lying about which MAC owns an IP — that's ARP spoofing.
- **WebSocket:** a persistent, two-way connection between two programs —
  used here so the PC can push alerts to the phone instantly, instead of
  the phone having to keep asking "anything new?" repeatedly.
- **Isolation Forest:** the machine learning model used to score how
  "normal" or "unusual" a network event looks, based on patterns learned
  from a baseline of normal traffic.
- **Baseline:** the "what does normal look like" data the model is
  trained on before it can recognize what's abnormal.

---

## 10. What to Do If Something Breaks

1. Read the exact error message — don't paraphrase it in your head, note
   it word-for-word.
2. Check the Troubleshooting table above first.
3. If it's still unclear, take a screenshot of the exact error (terminal
   output or Android Studio Build panel) and bring it back for help.
