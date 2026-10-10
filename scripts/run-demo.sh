#!/usr/bin/env bash
set -euo pipefail
repo_dir="$(cd "$(dirname "$0")/.." && pwd)"
cd "$repo_dir"
if ! command -v java >/dev/null || ! command -v mvn >/dev/null || ! command -v npm >/dev/null; then
  echo 'Install Java 17+, Maven and Node.js 20+ to run the local demo.' >&2
  exit 1
fi
if [ ! -d frontend/node_modules ]; then npm ci --prefix frontend; fi
npm run build --prefix frontend
mkdir -p backend/target/classes/static
cp -R frontend/dist/. backend/target/classes/static/
maven_args=()
if [ -n "${IRENE_MAVEN_REPO:-}" ]; then maven_args+=("-Dmaven.repo.local=$IRENE_MAVEN_REPO"); fi
mvn "${maven_args[@]}" -B -s backend/.mvn/public-settings.xml -gs backend/.mvn/public-settings.xml -f backend/pom.xml package -DskipTests
printf '\nOpen http://127.0.0.1:%s — fictional demo accounts appear on the login screen.\n' "${PORT:-8080}"
exec java -jar backend/target/irene-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=demo --server.address=127.0.0.1
