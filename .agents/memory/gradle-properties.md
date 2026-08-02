---
name: gradle.properties AndroidX
description: Required gradle.properties content for AndroidX-based Android projects
---

# gradle.properties for AndroidX

## Rule
All Android projects using AndroidX dependencies must have a `gradle.properties` file at the project root containing at minimum:

```properties
android.useAndroidX=true
android.enableJetifier=true
```

## Why
Without `android.useAndroidX=true`, the build fails at `checkReleaseAarMetadata` with:
"Configuration contains AndroidX dependencies, but the `android.useAndroidX` property is not enabled"

## How to apply
Create `gradle.properties` at the repo root if missing, or add these two lines to an existing one.
