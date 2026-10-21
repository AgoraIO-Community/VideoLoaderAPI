# Video Loader API Sample

## Overview

Preload live rooms and implement quick join and quick switching for audience video feeds using VideoLoaderAPI.

Repository: [AgoraIO-Community/VideoLoaderAPI](https://github.com/AgoraIO-Community/VideoLoaderAPI)

## Prerequisites and Setup

1. Obtain a test project's App ID and certificate (when token authentication is enabled). The sample's default robot rooms additionally require rte-cloudplayer access and REST credentials.
2. For Android, open `Android/` and set `AGORA_APP_ID`, `AGORA_APP_CERTIFICATE`, `CLOUD_PLAYER_KEY` and `CLOUD_PLAYER_SECRET` in its `gradle.properties`; sync and run.
3. For iOS, fill the corresponding values in `iOS/Example/VideoLoaderAPI/KeyCenter.swift`, run `pod install` in `iOS/Example/`, and open `VideoLoaderAPI.xcworkspace` with valid signing.
4. Select the demo's preload/render mode and exercise the room-list, quick-join and quick-switch screens with active test streams.

## What To Check

- Confirm the first remote frame appears when entering a live room.
- Scroll between rooms and verify video/audio follow the selected room.
- Leave the feed and confirm preloaded subscriptions and rendering resources are released.

## Integration Notes

This is an audience-side live-feed switching demo, not a local camera publishing quickstart. Default robot streams need service entitlement; keep cloud-player secrets on a backend in production.

Keep App Certificates, customer secrets and privileged token generation on a trusted backend for production. Use only your own test credentials while configuring historical demos; do not commit them. Preserve the sample's existing dependency versions and lifecycle until a separate migration is tested.

## Source References

- [README.md](../../README.md)
- [Android/README.md](../../Android/README.md)
- [iOS/README.md](../../iOS/README.md)

## Validation Status

This recipe was checked against the repository's source and setup documents. The platform build and live service/device flow have not been run as part of this documentation change. Complete the checks above in an environment with the required SDKs, hardware and service access before treating it as a validated deployment.

## Implementation Prompt

```text
You are working in AgoraIO-Community/VideoLoaderAPI. Use docs/ai/RECIPE.md and its linked source files to adapt the Video Loader API Sample reference to this project.

Start by inspecting the actual sample directory and configuration described above. Resolve its listed missing prerequisites before claiming it is runnable. Preserve the existing architecture and compatible dependency versions. Implement the recipe's feature-specific flow, permissions and resource cleanup. Use a trusted backend for privileged credentials and tokens in production.

Run the repository-specific build or launch flow, then perform each What To Check item. Report the commands run, the observed results and anything blocked by service access, hardware or incomplete sample code. Do not claim live validation from a documentation or catalog build alone.
```
