#!/usr/bin/env bash
set -euo pipefail
: "${IRENE_PROJECT_ID:?Set IRENE_PROJECT_ID}"
: "${IRENE_IMAGE:?Set IRENE_IMAGE to the image built by Cloud Build}"
: "${IRENE_ENV_FILE:?Set IRENE_ENV_FILE to your private cloud environment YAML}"
: "${IRENE_SERVICE_ACCOUNT:?Set IRENE_SERVICE_ACCOUNT to the runtime service account email}"
: "${IRENE_DATABASE_SECRET:?Set IRENE_DATABASE_SECRET to a Secret Manager secret:version}"
: "${IRENE_JWT_SECRET:?Set IRENE_JWT_SECRET to a Secret Manager secret:version}"
secret_refs="DATABASE_PASSWORD=$IRENE_DATABASE_SECRET,APP_JWT_SECRET=$IRENE_JWT_SECRET"
if [ -n "${IRENE_ADMIN_SECRET:-}" ]; then secret_refs="$secret_refs,BOOTSTRAP_ADMIN_PASSWORD=$IRENE_ADMIN_SECRET"; fi
# Deploys privately. Public judge access must be a deliberate later step.
gcloud run deploy irene \
  --project "$IRENE_PROJECT_ID" --region "${IRENE_REGION:-asia-south1}" \
  --image "$IRENE_IMAGE" --service-account "$IRENE_SERVICE_ACCOUNT" \
  --env-vars-file "$IRENE_ENV_FILE" \
  --set-secrets "$secret_refs" \
  --no-allow-unauthenticated --min-instances 0 --max-instances 2 \
  --memory 1Gi --cpu 1 --concurrency 20 --timeout 180
