#!/bin/bash
set -e

# ─────────────────────────────────────────────────────────────
# deploy.sh — Commit to GitHub + Package & Install to Maven
# ─────────────────────────────────────────────────────────────
# Usage:
#   ./deploy.sh                     # Commits, pushes, and installs to local Maven
#   ./deploy.sh "fix: bug in X"     # Custom commit message
#   ./deploy.sh --maven-only        # Skip git, only Maven install
#   ./deploy.sh --git-only          # Skip Maven, only git push
# ─────────────────────────────────────────────────────────────

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# ── Defaults ──────────────────────────────────────────────────
COMMIT_MSG="chore: update and deploy"
DO_GIT=true
DO_MAVEN=true
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
NC='\033[0m' # No Color

info()  { echo -e "${BLUE}▸${NC} $1"; }
ok()    { echo -e "${GREEN}✔${NC} $1"; }
warn()  { echo -e "${YELLOW}⚠${NC} $1"; }
fail()  { echo -e "${RED}✘${NC} $1"; exit 1; }

# ── Resolve Java ──────────────────────────────────────────────
if [ -z "$JAVA_HOME" ]; then
    JAVA_HOME=$(/usr/libexec/java_home 2>/dev/null || echo "")
    if [ -z "$JAVA_HOME" ]; then
        # Fallback to known location
        JAVA_HOME="$HOME/Library/Java/JavaVirtualMachines/azul-17.0.13/Contents/Home"
    fi
    export JAVA_HOME
    export PATH="$JAVA_HOME/bin:$PATH"
fi

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

    # Stage all changes
    info "Staging all changes..."
    git add -A
    ok "Files staged"

    # Commit (skip if nothing to commit)
    if git diff --cached --quiet; then
        warn "Nothing to commit, working tree clean"
    else
        info "Committing: \"$COMMIT_MSG\""
        git commit -m "$COMMIT_MSG"
        ok "Committed"
    fi

    # Push
    info "Pushing to origin/$BRANCH..."
    git push -u origin "$BRANCH"
    ok "Pushed to GitHub"
    echo ""
fi

# ══════════════════════════════════════════════════════════════
# 2. MAVEN: Test, Package & Install
# ══════════════════════════════════════════════════════════════
if [ "$DO_MAVEN" = true ]; then
    info "Stage 2: Maven — Test, Package & Install"
    echo ""

    # Run tests
    info "Running tests..."
    ./mvnw clean test
    ok "All tests passed"
    echo ""

    # Package & install to local Maven repo
    info "Packaging & installing to local Maven repo..."
    ./mvnw clean install
    ok "Installed to ~/.m2/repository"
    echo ""

    # Show installed artifacts
    VERSION=$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout 2>/dev/null || echo "1.0.0")
    GROUP_PATH="com/github/rylxes"
    ARTIFACT="spring-boot-softdelete"
    REPO_PATH="$HOME/.m2/repository/$GROUP_PATH/$ARTIFACT/$VERSION"

    info "Installed artifacts:"
    if [ -d "$REPO_PATH" ]; then
        ls -lh "$REPO_PATH"/*.jar 2>/dev/null | awk '{print "    " $NF " (" $5 ")"}'
    fi
    echo ""
fi

# ══════════════════════════════════════════════════════════════
# Done
# ══════════════════════════════════════════════════════════════
echo "═══════════════════════════════════════════════════════════"
echo -e "  ${GREEN}All done!${NC}"
if [ "$DO_MAVEN" = true ]; then
    echo ""
    echo "  To use in another project, add to pom.xml:"
    echo ""
    echo "    <dependency>"
    echo "        <groupId>com.github.rylxes</groupId>"
    echo "        <artifactId>spring-boot-softdelete</artifactId>"
    echo "        <version>$VERSION</version>"
    echo "    </dependency>"
fi
echo ""
echo "═══════════════════════════════════════════════════════════"
