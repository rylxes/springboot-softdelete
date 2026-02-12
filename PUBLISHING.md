# Publishing to Maven Central — Setup Guide

## Prerequisites Checklist

- [x] Sonatype Central account created at [central.sonatype.com](https://central.sonatype.com)
- [ ] Namespace `io.github.rylxes` verified
- [ ] GPG key generated and published
- [ ] Maven `settings.xml` configured with Central Portal token

---

## Step 1: Verify Your Namespace

1. Go to [central.sonatype.com](https://central.sonatype.com) → **Namespaces**
2. Click **Add Namespace** → enter `io.github.rylxes`
3. Central will ask you to prove GitHub ownership by creating a **temporary public repo** with a specific name (they'll tell you the exact name, something like `OSSRH-verification-XXXXX`)
4. Create that repo on GitHub, then click **Verify**
5. Once verified ✅, you can delete the verification repo

> **Why `io.github.rylxes` instead of `com.github.rylxes`?**
> Maven Central requires `io.github.<username>` for GitHub-based namespaces. `com.github.*` is reserved.

---

## Step 2: Generate a GPG Key

Maven Central requires all artifacts to be GPG signed.

```bash
# Install GPG (if not already installed)
brew install gnupg

# Generate a key (use your real name and email)
gpg --gen-key

# List your keys to get the key ID
gpg --list-keys --keyid-format short
# Output will show something like:
# pub   ed25519/ABCD1234 2026-02-12 [SC]
#       ← ABCD1234 is your KEY_ID

# Publish your public key to a key server (required for Central verification)
gpg --keyserver keyserver.ubuntu.com --send-keys ABCD1234
gpg --keyserver keys.openpgp.org --send-keys ABCD1234
```

---

## Step 3: Generate a Central Portal Token

1. Go to [central.sonatype.com](https://central.sonatype.com)
2. Click your profile (top right) → **View Account**
3. Click **Generate User Token**
4. It will show you a username and password — copy them

---

## Step 4: Configure Maven `settings.xml`

Edit (or create) `~/.m2/settings.xml`:

```xml
<settings>
    <servers>
        <server>
            <id>central</id>
            <username>YOUR_TOKEN_USERNAME</username>
            <password>YOUR_TOKEN_PASSWORD</password>
        </server>
    </servers>
</settings>
```

Replace `YOUR_TOKEN_USERNAME` and `YOUR_TOKEN_PASSWORD` with the values from Step 3.

---

## Step 5: Deploy! 🚀

```bash
./deploy.sh "feat: initial Maven Central release" --deploy-central
```

Or manually:

```bash
./mvnw clean deploy
```

This will:
1. Run tests
2. Build JARs (main + sources + javadoc)
3. Sign everything with GPG
4. Upload to Central Portal
5. Wait for validation and auto-publish

First deployment takes ~10–30 minutes to appear on Maven Central.

---

## Troubleshooting

| Issue | Fix |
|---|---|
| `No GPG key found` | Run `gpg --list-keys` to verify the key exists |
| `gpg: signing failed: No pinentry` | `brew install pinentry-mac` then add `pinentry-program /opt/homebrew/bin/pinentry-mac` to `~/.gnupg/gpg-agent.conf` |
| `401 Unauthorized` | Check `~/.m2/settings.xml` server id matches `central` |
| `Namespace not verified` | Go to central.sonatype.com → Namespaces and complete verification |
