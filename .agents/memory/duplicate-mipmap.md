---
name: duplicate mipmap resources
description: Avoid duplicate resource errors from XML + image files with same name in mipmap dirs
---

# Duplicate mipmap resources

## Rule
Never have both an `.xml` and a `.jpg`/`.png` file with the same resource name in a density-specific mipmap directory (e.g. `mipmap-hdpi`).

## Why
AAPT fails with "Duplicate resources" error during `mergeReleaseResources`. The density-specific dir can only have one file per resource name.

## How to apply
- Keep `.jpg`/`.png` bitmap files in each density-specific dir (`mipmap-hdpi`, `mipmap-mdpi`, etc.)
- Keep adaptive icon XML definitions only in `mipmap-anydpi-v26/`
- Delete any `.xml` bitmap wrappers from density-specific mipmap dirs that conflict with same-named image files
