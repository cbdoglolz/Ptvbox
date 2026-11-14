# TV delivery rules

- Future automatic builds/releases must integrate the owner's WeCom channel through WECHAT_WEBHOOK_URL stored as an Actions secret.
- Notify only when a real new client version with actual feature/fix content is ready; never for unchanged checks, tests or build-only timestamps.
- Message bodies contain only version changes and update details. No links of any kind: repository, workflow, download, website or other addresses. Do not send test messages without an actual requested update.
- APK signing secrets and personal source credentials never enter code, logs or packages. Releases remain drafts for the owner to publish.
