#!/usr/bin/env bash
# Build only; never uploads to Firebase.
set -euo pipefail
repo_dir="$(cd "$(dirname "$0")/../.." && pwd)"
: "${VITE_API_BASE:?Set VITE_API_BASE to https://YOUR_SERVICE.run.app/api/v1}"
node -e 'const u=new URL(process.env.VITE_API_BASE); if(u.protocol!=="https:" || !u.hostname.endsWith(".run.app") || u.pathname!=="/api/v1" || u.search || u.hash || u.username || u.password) { console.error("Use the exact HTTPS Cloud Run URL followed by /api/v1, without credentials, query or trailing slash."); process.exit(1); }'
npm ci --prefix "$repo_dir/frontend"
npm run build --prefix "$repo_dir/frontend"
