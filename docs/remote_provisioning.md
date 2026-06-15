# IPTV Smarters — Firebase Remote Provisioning Architecture

This document provides a comprehensive technical overview of the **Firebase Remote Provisioning and Device-to-Database Synchronization Engine** implemented in this IPTV Smarters application.

---

## 1. Overview & The TV UX Challenge

Typing multi-character servers, usernames, and 20+ character password hashes into a media player using a standard handheld infrared remote control or an on-screen D-pad keyboard is one of the most frustrating experiences for Smart TV, Android TV, and Fire TV users. 

To solve this, the application features an **automated, zero-manual-input remote provisioning mechanism**. 
* **The Receiver Device (e.g., Android TV / TV Dongle)** generates a short, highly readable **8-digit pairing code**.
* **The Sender Device (e.g., Companion Web app, smartphone, tablet, or Administrator console)** inputs the 8-digit grouping code, enters the subscription billing credentials (Xtream Codes API server details or M3U Playlist URL), and triggers a push.
* **The Cloud Relay (Firebase Realtime Database)** matches the code and writes the encrypted portal details directly to the TV's user profile node.
* **The Target TV Device** instantly detects the remote payload, synchronizes the database, and begins loading channels and stream metadata.

---

## 2. System Architecture

The provisioning engine is designed as a secure, decoupled, cross-device message relay.

```
+-----------------------------------+             +----------------------------------+
|           SENDER DEV              |             |           RECEIVER DEV           |
| (Mobile App / Admin Dashboard)    |             |       (Android TV / Tablet)      |
+-----------------+-----------------+             +-----------------+----------------+
                  |                                                 |
                  | 1. Input 8-Digit Pairing Code                   | Generates 8-Digit Code
                  |    & Portal Credentials                         | & Registers Listener
                  v                                                 v
+-----------------+-------------------------------------------------+----------------+
|                                                                                    |
|                           FIREBASE REALTIME DATABASE                               |
|                                                                                    |
|    /devices/$code                                                                  |
|    {                                                                               |
|       "uid": "user_112233",                                                        |
|       "email": "receiver@tv.com",                      2. Matches Code & Resolves  |
|       "timestamp": 1781466816                          Target UID                  |
|    }                                                   --------------------------->|
|                                                                                    |
|    /users/$uid/portals/$portalId                                                   |
|    {                                                                               |
|       "name": "HQ Premium IPTV",                                                   |
|       "type": "XTREAM",                                3. Writes Portal Details    |
|       "hostUrl": "http://stream.host",                 (Instantly Merged)          |
|       "username": "customer_99",                       --------------------------->|
|       "password": "pass"                                                           |
|    }                                                                               |
|                                                                                    |
+------------------------------------------------------------------------------------+
```

### Protocol Interaction Flow

1. **Token Allocation & Generation**: Upon startup, the receiving device requests an 8-digit unique code via `FirebaseManager.getProvisioningCode()`. This is stored locally in device-level `SharedPreferences` so that it persists across screen refreshes.
2. **Device Registration Hook**: The receiving device executes `registerDeviceForProvisioning()`, posting its active Firebase Authentication User Profile `uid` and verified account email inside the `/devices/$code` bucket.
3. **Remote Administration lookup**: When the provisioning sender executes a command, it requests lookup on `/devices/$targetCode.json`. Firebase resolves the association, verifying the existence of the pairing token and returning the bound `targetUid`.
4. **Relay Payload Insertion**: The sender formats the credentials payload under the matching user account path `/users/$targetUid/portals/$portalId.json` and pushes the data.
5. **Real-Time Client Merging**: The receiver’s active flow observer loads and syncs the new playlist, updating local state variables dynamically.

---

## 3. Database Schema Reference

The Firebase Realtime Database runs a clean, lightning-fast key-value tree layout:

### `devices` Nodes (Pairing Relay Directory)
Keyed on the unique 8-digit numeric pairing string (`10000000..99999999`).
```json
{
  "devices": {
    "48151623": {
      "uid": "8bF2h7Jw89LkPzQxRst99W3Yv9Za",
      "email": "livingroom-tv@gmail.com",
      "timestamp": 1781466816123
    }
  }
}
```

### `users` Nodes (Profiles & Provisioned Portals)
Stores the list of IPTV playlists and stream portal profiles bound to each account.
```json
{
  "users": {
    "8bF2h7Jw89LkPzQxRst99W3Yv9Za": {
      "portals": {
        "portal_1718384210000": {
          "id": "portal_1718384210000",
          "name": "Premium Live Sports & VOD",
          "type": "XTREAM",
          "hostUrl": "http://premium-iptv-relay.net",
          "port": "8080",
          "username": "iptv_user_992",
          "password": "secureStreamPasswordXYZ"
        },
        "portal_1718384225000": {
          "id": "portal_1718384225000",
          "name": "Open-Source backup playlist",
          "type": "M3U",
          "url": "https://iptv-org.github.io/iptv/categories/movies.m3u"
        }
      }
    }
  }
}
```

---

## 4. Cryptographic Encryption & REST API Integration

To prevent unauthorized entities from intercepting sensitive subscription details (such as server credentials, usernames, and passwords) or sniffing pairing associations, the relay uses **symmetric AES-256 encryption with SHA-256 key-derivation**. 

### Cryptographic Specification
* **Algorithm**: `AES` (Advanced Encryption Standard).
* **Mode & Padding**: `AES/CBC/PKCS5Padding` with a deterministic standard Initialization Vector (IV).
* **Key Derivation**: Keys are derived by passing the raw dynamic secret string through `SHA-256` hash function to produce a secure, stable 256-bit (32-byte) binary key.
* **Payload Wrapper**: Encrypted payloads are enveloped inside a standard JSON object containing:
  * `"encrypted": true` — A boolean flag informing the recipient that decryption is required.
  * `"data": "<Base64_Ciphertext>"` — The secure Base64-encoded binary block.

---

### Endpoints and Payload Layouts

#### A. Code & Hardware Identifier Registration (Device Binding)
The system supports dual registering bindings. A target device is reachable either via its temporary **8-digit dynamic numeric code** OR via its persistent hardware **`ANDROID_ID`**. 

The lookup key itself is stored unencrypted in the Realtime Database (as keys under the `/devices/$code` or `/devices/$androidId` node), but the structural user details mapped within are fully encrypted. When registering under the dynamic code, the secret is the code itself; when registering under the native hardware identifier, the secret is that specific `ANDROID_ID` string.

* **Endpoints**: 
  * `PUT https://$projectId-rtdb.firebaseio.com/devices/$code.json`
  * `PUT https://$projectId-rtdb.firebaseio.com/devices/$androidId.json`
* **Plaintext Inner Metadata**:
  ```json
  {
    "uid": "8bF2h7Jw89LkPzQxRst99W3Yv9Za",
    "email": "receiver@tv.com",
    "timestamp": 1781466816123
  }
  ```
* **Realtime Database JSON Value**:
  ```json
  {
    "encrypted": true,
    "data": "A9kXv1+8bLM3pQv...[Base64 AES Ciphertext]"
  }
  ```

#### B. Device Discovery (Lookup & Resolve)
When looking up a target device, the sender executes a standard `GET` request matching the user-input registration string (which can be either the 8-digit grouping code or the hardware `ANDROID_ID`). It receives the encrypted payload and decrypts `"data"` using that exact input string as the symmetric key, resolving the target subscriber's recipient `uid`.

* **Endpoint**: `GET https://$projectId-rtdb.firebaseio.com/devices/$targetCode.json`

#### C. Subscription Portal Provision Payload
When provisioning or saving subscription details, the portal payload JSON is encrypted using the recipient's secure **Firebase User UID** (`targetUid`) as the encryption secret. This guarantees that only the sender (who performed the verification lookup) and the recipient (who owns the account) can decrypt the IPTV login details.

* **Endpoint**: `PUT https://$projectId-rtdb.firebaseio.com/users/$targetUid/portals/$portalId.json`
* **Plaintext Inner Portal Details**:
  ```json
  {
    "id": "portal_1718384210000",
    "name": "Live Premium Stream",
    "hostUrl": "http://main-server.com",
    "port": "80",
    "username": "customer_abc",
    "password": "mySecurePassword123",
    "type": "XTREAM",
    "url": ""
  }
  ```
* **Realtime Database JSON Value**:
  ```json
  {
    "encrypted": true,
    "data": "mWz9Hq9v18BPlMxQv...[Base64 AES-256 Ciphertext]"
  }
  ```

---

## 5. Built-In Simulated Emulator Fallback (No-Config Safe Mode)

Developers and engineers frequently need to iterate, build, and test features without having access to an active Firebase Client cloud project, or in environments where the internet may be highly restricted.

The provisioning engine has a **robust fallback system** that kicks in automatically if no `FIREBASE_PROJECT_ID` or `FIREBASE_API_KEY` are provided in `.env` / `BuildConfig`:

1. **Simulated Registers**: When calling `registerDeviceForProvisioning()`, the app saves registration details inside a local device-level database wrapper (`simulated_remote_db` SharedPreferences directory) mapped to keys:
   `code_email_$code` -> email  
   `code_uid_$code` -> uid
2. **Local Messaging Relay Queue**: Triggering `remoteProvisionPortal` validates mock codes and places the portal payload payload inside `portals_$targetUid`.
3. **Simulated Deliveries**: When the app starts up or refreshes, `loadProvisionedPortals()` checks if a mock project configuration is running. It checks this local payload queue, merges the profiles seamlessly, and clears the mock delivery queue. 

This guarantees **100% feature functional testability** on isolated emulators and development pipelines out-of-the-box, with zero credentials pre-configured.

---

## 6. How to Use & Operating Guidelines

### Option A: From the IPTV Smarters App Built-In Admin Portal
1. On the receiving screen (e.g., your Android TV app), note the **8-Digit Code** prominently displayed on the `LoginScreen`.
2. On any other device (or simulated companion context), click the **Secret Admin Console** (accessible under the hidden profile trigger or developer settings).
3. Select **Xtream Codes** or **M3U Link** mode.
4. Enter the **8-Digit Target Device Code** shown on the TV.
5. Provide a descriptive **Playlist Profile Name** and details (Host, Username, Password, or direct `.m3u` link).
6. Tap **SEND & REMOTE PROVISION DEVICE**.
7. The receiving device will instantly sync and import the new portal, adding it to the **Your Configured Playlists & Profiles** overview list.

### Option B: Cloud or Web Panel Integration
Service operators or subscription managers can build custom web interfaces calling the Firebase Realtime Database directly. Since the REST API keys are standard, any web-based billing or billing automation integration can write a portal entry under `/users/$targetUid/portals/` pointing to the customer's target device matching their generated 8-digit pairing token.

---

## 7. Server-Side Diagnostics & Decryption Playbook

When auditing the Firebase Realtime Database, subscription logs, or debugging user support tickets, administrators might find values containing raw Base64 payloads inside the `"data"` parameter marked with `"encrypted": true`.

To decrypt these values for diagnostic review, use one of the standard utility scripts below.

### Standard Inputs Required:
1. **The Ciphertext**: The string found inside the `"data"` field of the Firebase node.
2. **The Shared Secret / Key String**:
   * For `/devices/$code` entries: The secret is the **8-digit pairing code** (e.g., `"48151623"`).
   * For `/users/$targetUid/portals/$portalId` entries: The secret is the recipient's **Firebase User UID** (`targetUid`).

---

### Node.js Script (Standard Library - Zero Dependencies)

Create a file named `decrypt_diagnostic.js` and execute it with `node decrypt_diagnostic.js`:

```javascript
/**
 * IPTV Smarters - Server-Side Decryption Tool (Node.js)
 * Zero dependencies required. Uses the built-in 'crypto' module.
 */
const crypto = require('crypto');

function decryptPayload(ciphertextBase64, keyString) {
    try {
        // 1. Derive the 256-bit AES Key via SHA-256
        const key = crypto.createHash('sha256').update(keyString, 'utf-8').digest();
        
        // 2. Setup the Deterministic Zero IV (16 bytes)
        const iv = Buffer.alloc(16, 0);

        // 3. Initialize AES-256-CBC Decipher
        const decipher = crypto.createDecipheriv('aes-256-cbc', key, iv);
        
        // 4. Decrypt and output UTF-8 string
        let decrypted = decipher.update(ciphertextBase64, 'base64', 'utf8');
        decrypted += decipher.final('utf8');
        
        return JSON.parse(decrypted);
    } catch (error) {
        console.error("[-] Decryption failed. Please verify your Key String / Pairing Code or Ciphertext structure.");
        console.error(error.message);
        return null;
    }
}

// ==========================================
// DIAGNOSTIC INPUTS FOR COPY-PASTE TESTING
// ==========================================
const sampleBase64Cipher = "PASTE_THE_BASE64_DATA_STRING_HERE";
const sampleSecretKey = "PASTE_PAIRING_CODE_OR_UID_HERE";

console.log("[*] Attempting decryption...");
const result = decryptPayload(sampleBase64Cipher, sampleSecretKey);
if (result) {
    console.log("[+] Decryption Successful! Resolved Payload:");
    console.log(JSON.stringify(result, null, 2));
}
```

---

### Python Script (Requires `pycryptodome`)

Install requirements: `pip install pycryptodome`  
Create a file named `decrypt_diagnostic.py` and run it:

```python
"""
IPTV Smarters - Server-Side Decryption Tool (Python)
Prerequisites: pip install pycryptodome
"""
import base64
import json
import hashlib
from Crypto.Cipher import AES
from Crypto.Util.Padding import unpad

def decrypt_payload(ciphertext_base64, key_string):
    try:
        # 1. Derive 256-bit AES Key via SHA-256 of the token string
        key_bytes = hashlib.sha256(key_string.encode('utf-8')).digest()
        
        # 2. Configured Deterministic Zero IV (16 bytes)
        iv_bytes = bytes([0] * 16)
        
        # 3. Decode Base64 ciphertext
        ciphertext_bytes = base64.b64decode(ciphertext_base64)
        
        # 4. Decrypt via AES-256-CBC
        cipher = AES.new(key_bytes, AES.MODE_CBC, iv=iv_bytes)
        decrypted_padded = cipher.decrypt(ciphertext_bytes)
        
        # 5. Strip PKCS7 / PKCS5 padding
        decrypted_bytes = unpad(decrypted_padded, AES.block_size)
        
        # 6. Parse JSON output
        return json.loads(decrypted_bytes.decode('utf-8'))
        
    except Exception as e:
        print("[-] Decryption failed. Verify your secret key / pairing code and payload correctness.")
        print(f"Error details: {e}")
        return None

# ==========================================
# DIAGNOSTIC INPUTS FOR COPY-PASTE TESTING
# ==========================================
sample_base64_cipher = "PASTE_THE_BASE64_DATA_STRING_HERE"
sample_secret_key = "PASTE_PAIRING_CODE_OR_UID_HERE"

print("[*] Attempting decryption...")
result = decrypt_payload(sample_base64_cipher, sample_secret_key)
if result:
    print("[+] Decryption Successful! Resolved Payload:")
    print(json.dumps(result, indent=2))
```

