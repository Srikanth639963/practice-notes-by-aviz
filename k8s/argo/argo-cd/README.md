# argo-awar05
Argo CD Installation and testing

### Install ArgoCD:

sh```
kubectl create namespace argocd

kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

kubectl patch svc argocd-server -n argocd -p '{"spec": {"type": "LoadBalancer"}}'

kubectl -n argocd get secret argocd-initial-admin-secret \
          -o jsonpath="{.data.password}" | base64 -d; echo
sh```


** Make sure to enable "Self heal" to prevent users to deploy using "kubectl" manually.

** argo app checks git repo for every 3 minutes (180 seconds) and sync the state with repo


## Sync Policy: Manual
You manually trigger syncs - ArgoCD won't auto-deploy changes.

## Set Deletion Finalizer
Prevents ArgoCD from deleting resources when the app is removed - acts as a safety lock.

## Sync Options:

**Skip Schema Validation** - Bypasses Kubernetes schema checks (useful for CRDs or custom resources)

**Prune Last** - Deletes old resources after creating new ones (safer deployment order)

**Respect Ignore Differences** - Honors fields you've marked to ignore during drift detection

**Auto-Create Namespace** - Creates the target namespace if it doesn't exist

**Apply Out of Sync Only** - Only updates resources that have changed (more efficient)

**Server-Side Apply** - Uses Kubernetes server-side apply instead of client-side (better for large resources)

## Prune Propagation Policy: foreground
When deleting resources, waits for dependent resources to be deleted first (parent → children order)

**Replace** - Delete then recreate resources (vs. patch)

**Retry** - Automatically retry failed sync operations


### ArgoCD sync options you'll see when manually syncing an app

## Prune
Deletes resources from the cluster that exist in Kubernetes but are no longer defined in Git. Cleans up orphaned resources.

**Example**: You removed a deployment from Git → Prune will delete it from the cluster too.

## Dry Run
Simulates the sync without actually applying changes. Shows you what *would* happen.

**Use case**: Test before deploying to production - see the diff safely.

## Apply Only
Only creates/updates resources but won't delete anything, even if Prune is enabled elsewhere.

**Use case**: Safe deployments when you want to add/modify but never remove.

## Force
Bypasses sync waves and ignores hooks - applies everything immediately and forcefully.

**Warning**: Use carefully! Can cause disruption by skipping controlled rollout sequences.

		
