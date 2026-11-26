# Project continuity gate

- Before starting development, read `C:\Users\cbdog\.codex\PROJECT-CONTINUITY.md` and `D:\Ptvbox\PROJECT-CONTINUITY.md` completely.
- Check `Revision`, `Last updated`, `git status`, recent commits and the latest release before editing. Treat changes by other people as user work until proven otherwise.
- Update `PROJECT-CONTINUITY.md` after every meaningful session and before finishing every version, including implementation state, tests, release evidence, blockers and the exact next resume point.
- Never rely on chat history alone when it conflicts with the project continuity file, source, tests or deployed state.

# TV delivery rules

- Future automatic builds/releases must integrate the owner's WeCom channel through WECHAT_WEBHOOK_URL stored as an Actions secret.
- Notify only when a real new client version with actual feature/fix content is ready; never for unchanged checks, tests or build-only timestamps.
- Message bodies contain only version changes and update details. No links of any kind: repository, workflow, download, website or other addresses. Do not send test messages without an actual requested update.
- APK signing secrets and personal source credentials never enter code, logs or packages. Releases remain drafts for the owner to publish.
- Use an isolated emulator by default. A box the user is currently watching must never be connected, installed on, launched on or controlled without renewed explicit authorization. Local protected-device addresses belong only in ignored .tools configuration, never source or release artifacts.
- Each delivery includes a Markdown report distinguishing implemented features, emulator evidence, physical-device evidence and the PC parity backlog. Do not call an incomplete feature matrix full parity.
- For every new version not directly tested on the owner's TV box, deliver a signed APK for the owner to transfer to a phone and test manually. Explicitly label unverified device behavior; a supplied APK does not imply physical-device validation. Keep APKs under artifacts and provide the exact local file link. For documentation-only work, reuse and verify the existing signed APK rather than manufacturing a new version. Distinguish an ATV APK from the future native phone app.
- Every version delivery must include a requirement checklist against the owner's original development dossier: mark completed items with [x] and unfinished/partially implemented/unverified items with [ ]. State what changed in this version, what was not developed this time, and the specific next-version scope. Do not equate a runnable APK or passing automated tests with stage acceptance. Support pointer/touch and D-pad input together so the ATV test APK can be operated on Android phones and PC Android emulators.
