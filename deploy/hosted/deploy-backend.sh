#!/usr/bin/env bash
# Default is a local command preview. Only --apply performs deployment.
set -euo pipefail
mode=${1:---plan}
[[ "$mode" = --plan || "$mode" = --apply ]] || { echo 'Usage: deploy-backend.sh [--plan|--apply]' >&2; exit 1; }
: "${IRENE_PROJECT_ID:?Set the Cloud Run project ID (not the Firebase/Gemini project)}"
: "${IRENE_IMAGE:?Set the immutable Artifact Registry image digest, ending @sha256:...}"
: "${IRENE_SERVICE_ACCOUNT:?Set the dedicated runtime service account email}"
: "${IRENE_ENV_FILE:?Set the private deploy/hosted/env.yaml path}"
: "${IRENE_DATABASE_SECRET:?Set database-password-secret:VERSION}"
: "${IRENE_JWT_SECRET:?Set jwt-secret:VERSION}"
: "${IRENE_GEMINI_SECRET:?Set gemini-key-secret:VERSION}"
: "${IRENE_CA_SECRET:?Set supabase-ca-secret:VERSION}"
[[ "$IRENE_IMAGE" =~ @sha256:[a-f0-9]{64}$ ]] || { echo 'Use a pinned sha256 image digest for reproducible rollback.' >&2; exit 1; }
for ref in "$IRENE_DATABASE_SECRET" "$IRENE_JWT_SECRET" "$IRENE_GEMINI_SECRET" "$IRENE_CA_SECRET" "${IRENE_ADMIN_SECRET:-unused:1}"; do
  [[ "$ref" =~ ^[A-Za-z0-9_-]+:[1-9][0-9]*$ ]] || { echo 'Secret references must use a secret name and numeric version, never a secret value or latest.' >&2; exit 1; }
done
[ -r "$IRENE_ENV_FILE" ] || { echo 'Private environment file not found.' >&2; exit 1; }
secret_refs="DATABASE_PASSWORD=$IRENE_DATABASE_SECRET,APP_JWT_SECRET=$IRENE_JWT_SECRET,GEMINI_API_KEY=$IRENE_GEMINI_SECRET,/var/run/secrets/supabase/ca.crt=$IRENE_CA_SECRET"
if [ -n "${IRENE_ADMIN_SECRET:-}" ]; then secret_refs+=",BOOTSTRAP_ADMIN_PASSWORD=$IRENE_ADMIN_SECRET"; fi
cmd=(gcloud run deploy irene-api --project "$IRENE_PROJECT_ID" --region "${IRENE_REGION:-asia-south1}"
  --image "$IRENE_IMAGE" --service-account "$IRENE_SERVICE_ACCOUNT" --env-vars-file "$IRENE_ENV_FILE"
  --set-secrets "$secret_refs" --no-allow-unauthenticated --min 0 --max 2 --min-instances 0 --max-instances 2
  --cpu-throttling --cpu 1 --memory 1Gi --concurrency 10 --timeout 180 --port 8080
  --startup-probe 'httpGet.path=/actuator/health/readiness,httpGet.port=8080,periodSeconds=5,timeoutSeconds=3,failureThreshold=40'
  --liveness-probe 'httpGet.path=/actuator/health/liveness,httpGet.port=8080,periodSeconds=30,timeoutSeconds=3,failureThreshold=3')
if [ "$mode" = --plan ]; then
  printf 'Plan only. Nothing is sent to Google.\n'
  printf '%q ' "${cmd[@]}"; printf '\n'
else
  command -v gcloud >/dev/null || { echo 'Install Google Cloud CLI or use Cloud Shell.' >&2; exit 1; }
  "${cmd[@]}"
fi
