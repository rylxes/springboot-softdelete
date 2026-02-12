#!/bin/bash
set -e

# ─────────────────────────────────────────────────────────────
# deploy.sh — Commit to GitHub + Package & Deploy Maven
# ─────────────────────────────────────────────────────────────
# Usage:
#   ./deploy.sh                              # Commits, pushes, installs locally
#   ./deploy.sh "fix: bug in X"              # Custom commit message
#   ./deploy.sh --maven-only                 # Skip git, only Maven install
#   ./deploy.sh --git-only                   # Skip Maven, only git push
#   ./deploy.sh --deploy-central             # Deploy to Maven Central
#   ./deploy.sh "release v1.0" --deploy-central  # Full release
# ─────────────────────────────────────────────────────────────

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# ── Defaults ──────────────────────────────────────────────────
COMMIT_MSG="chore: update and deploy"
DO_GIT=true
DO_MAVEN=true
DEPLOY_CENTRAL=false
BRANCH="master"

# ── Parse args ────────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
    case "$1" in
        --maven-only)
            DO_GIT=false
            shift
            ;;
        --git-only)
            DO_MAVEN=false
            shift
            ;;
        --deploy-central)
            DEPLOY_CENTRAL=true
            shift
            ;;
        --branch)
            BRANCH="$2"
            shift 2
            ;;
        *)
            COMMIT_MSG="$1"
            shift
            ;;
    esac
done

# ── Colors ────────────────────────────────────────────────────
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

info()  { echo -e "${BLUE}▸${NC} $1"; }
ok()    { echo -e "${GREEN}✔${NC} $1"; }
warn()  { echo -e "${YELLOW}⚠${NC} $1"; }
fail()  { echo -e "${RED}✘${NC} $1"; exit 1; }

# ── Resolve Java 17 (always override to ensure correct version) ──
JAVA_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null || echo "")
if [ -z "$JAVA_HOME" ] || [ ! -d "$JAVA_HOME" ]; then
    JAVA_HOME="$HOME/Library/Java/JavaVirtualMachines/azul-17.0.13/Contents/Home"
fi
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"

echo ""
echo "═══════════════════════════════════════════════════════════"
echo "  Spring Boot Soft Delete — Deploy"
echo "═══════════════════════════════════════════════════════════"
echo ""

# ══════════════════════════════════════════════════════════════
# 1. GIT: Commit & Push to GitHub
# ══════════════════════════════════════════════════════════════
if [ "$DO_GIT" = true ]; then
    info "Stage 1: Git — Commit & Push to GitHub"
    echo ""

    git add -A
    ok "Files staged"

    if git diff --cached --quiet; then
        warn "Nothing to commit, working tree clean"
    else
        info "Committing: \"$COMMIT_MSG\""
        git commit -m "$COMMIT_MSG"
        ok "Committed"
    fi

    info "Pushing to origin/$BRANCH..."
    git push -u origin "$BRANCH"
    ok "Pushed to GitHub"
    echo ""
fi

# ══════════════════════════════════════════════════════════════
# 2. MAVEN: Test, Package & Deploy
# ══════════════════════════════════════════════════════════════
if [ "$DO_MAVEN" = true ]; then
    info "Stage 2: Maven — Test & Build"
    echo ""

    # Run tests first
    info "Running tests..."
    ./mvnw clean test
    ok "All tests passed"
    echo ""

    if [ "$DEPLOY_CENTRAL" = true ]; then
        # ── Deploy to Maven Central ──────────────────────────
        info "Deploying to Maven Central..."
        echo ""

        # Pre-flight checks
        if ! command -v gpg &> /dev/null; then
            fail "GPG not found. Install with: brew install gnupg"
        fi

        if ! gpg --list-keys &> /dev/null; then
            fail "No GPG keys found. Generate one with: gpg --gen-key"
        fi

        if [ ! -f "$HOME/.m2/settings.xml" ]; then
            fail "~/.m2/settings.xml not found. See PUBLISHING.md for setup."
        fi

        if ! grep -q '<id>central</id>' "$HOME/.m2/settings.xml" 2>/dev/null; then
            fail "No <server> with id 'central' in ~/.m2/settings.xml. See PUBLISHING.md."
        fi

        ok "Pre-flight checks passed"

        info "Building, signing, and uploading to Maven Central..."
        ./mvnw clean deploy
        ok "Deployed to Maven Central!"
        echo ""
        warn "It may take 10-30 minutes for the artifact to appear on search.maven.org"
    else
        # ── Install locally ──────────────────────────────────
        info "Installing to local Maven repo..."
        ./mvnw clean install -Dgpg.skip=true
        ok "Installed to ~/.m2/repository"
    fi
    echo ""

    # Show version info
    VERSION=$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout 2>/dev/null || echo "1.0.0")
    GROUP_ID=$(./mvnw help:evaluate -Dexpression=project.groupId -q -DforceStdout 2>/dev/null || echo "io.github.rylxes")

    echo "═══════════════════════════════════════════════════════════"
    echo -e "  ${GREEN}All done!${NC}"
    echo ""
    echo "  Add to consumer's pom.xml:"
    echo ""
    echo "    <dependency>"
    echo "        <groupId>$GROUP_ID</groupId>"
    echo "        <artifactId>spring-boot-softdelete</artifactId>"
    echo "        <version>$VERSION</version>"
    echo "    </dependency>"
    echo ""
    echo "═══════════════════════════════════════════════════════════"
fi
