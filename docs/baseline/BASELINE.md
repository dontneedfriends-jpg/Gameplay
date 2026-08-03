# Baseline: pre-runtime-hardening

commit: 33bdb679145273d1904f81a398963ea5b440fbe6
date: 2026-08-03

## Components
catalog: remote manifest (see ManifestRepository.kt)
defaults: DefaultVersion + bundled assets in app assets (sha256 in native-so-sha256.txt where ELF)

## Native .so inventory
see native-so-sha256.txt (26 files)

## Notes
Old containers/sessions/library DB/logs snapshots are device-local; capture per plan 0.1 during first device pass.
