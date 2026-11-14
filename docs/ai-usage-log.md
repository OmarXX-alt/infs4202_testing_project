# AI usage log

One entry per meaningful use.

| Date | Member | Tool and model | Purpose | Prompt summary | Kept | Modified (how) | Rejected (why) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 2026-11-14 | UniPlace team | OpenCode, DeepSeek V4.1 Flash Free | Read-only reverse-engineering of the Portal and black-box Engine, test/coverage baseline, runtime probing, and test-design hypotheses | Read every Portal source, template, test, and configuration file; run the test suite with failure-ignore; start the delivered system with a reset; probe Portal roles and Engine endpoints; write `docs/reverse-engineering/00`–`09` with evidence references | Yes: all ten documents and this log entry | Claims were checked against source or runtime probes; unconfirmed items are labelled **[INFERRED]** or **[UNKNOWN]**; hypotheses were kept separate from confirmed runtime results; no Portal file was changed | Speculative defect statements without a source reference or a reproduction method were removed; decompiling the Engine and printing secrets were rejected as out of scope |

## Reflection
