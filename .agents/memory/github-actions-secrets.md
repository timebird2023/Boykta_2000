---
name: GitHub Actions secrets setup
description: How to set GitHub Actions secrets via the API from Replit
---

# GitHub Actions secrets setup via API

## Rule
Use Python + PyNaCl (available on Replit) to encrypt secret values with the repo's public key before uploading via GitHub API.

## How to apply
1. GET `https://api.github.com/repos/{owner}/{repo}/actions/secrets/public-key` → get `key_id` and `key`
2. Encrypt each secret value with `nacl.public.SealedBox(PublicKey(key, Base64Encoder)).encrypt(value)`
3. PUT `https://api.github.com/repos/{owner}/{repo}/actions/secrets/{name}` with `{"encrypted_value": base64_encrypted, "key_id": key_id}`

PyNaCl is available via `pip install PyNaCl`.

## PKCS12 keystore note
When generating a keystore with `keytool`, PKCS12 format (default in JDK 17+) ignores the `-keypass` value if it differs from `-storepass`. Both KEY_PASSWORD and STORE_PASSWORD should be set to the same value (storepass) in GitHub Actions secrets.
