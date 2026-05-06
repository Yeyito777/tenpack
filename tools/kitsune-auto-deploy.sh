#!/usr/bin/env bash
set -euo pipefail

# Poll/update/deploy Tenpack on kitsune.
# Intended to be run by systemd timer as user `yeyito`.

REPO="${TENPACK_REPO:-/home/yeyito/Workspace/tenpack}"
BRANCH="${TENPACK_BRANCH:-main}"
PUBLIC_TARGET="${TENPACK_PUBLIC_TARGET:-/srv/http/tenpack}"

log() {
  printf '[%s] %s\n' "$(date -Is)" "$*"
}

deploy_current() {
  if [[ ! -f "$REPO/public/client-manifest.json" || ! -f "$REPO/public/server-manifest.json" ]]; then
    echo "public manifests missing; run ./tools/tenpack-build-public.py --out public before committing" >&2
    return 1
  fi

  log "deploying public/ to ${PUBLIC_TARGET}"
  sudo -n mkdir -p "$PUBLIC_TARGET"
  sudo -n rsync -a --delete "$REPO/public/" "$PUBLIC_TARGET/"
  sudo -n chown -R root:root "$PUBLIC_TARGET"
  sudo -n find "$PUBLIC_TARGET" -type d -exec chmod 755 {} +
  sudo -n find "$PUBLIC_TARGET" -type f -exec chmod 644 {} +

  log "restarting Tenpack services"
  sudo -n systemctl restart tenpack-minecraft.service
  sudo -n systemctl restart tenpack-upnp.service || true
}

if [[ ! -d "$REPO/.git" ]]; then
  echo "repo is not a git checkout: $REPO" >&2
  exit 1
fi

cd "$REPO"

old_head="$(git rev-parse HEAD)"
log "checking origin/${BRANCH} from ${old_head}"
git fetch --quiet origin "$BRANCH"
new_head="$(git rev-parse "origin/${BRANCH}")"

if [[ "$old_head" == "$new_head" ]]; then
  log "no update"
  exit 0
fi

log "update detected: ${old_head} -> ${new_head}"
git reset --hard --quiet "origin/${BRANCH}"
deploy_current
log "deployed ${new_head}"
