# V2-01 support rebase report

## Verified baseline

Main branch: `work/v2-playable-core`.
Local and remote main SHA: `062797f54ee63a65b2cc79870a619d1eeea0daa1`.
Support branch: `gpt-support/v2-01-observation-client-gate`.
Support checkout is separate from the main checkout. Main and master source were not modified.

## Archive availability

`whileaway-gpt-support-tier-a.zip` and `gpt-support-return-network-tier-a.zip` were not found in the workspace or attachment filename inventory.
Their contents were not supplied in this directive. No content-specific compatibility claim is made for those two archives.
Disposition: **NEEDS REWORK / provenance review pending**. Do not apply either archive wholesale.
No matching support paths were found in the tracked tree at `570c4f821c21aa98e4f2ab479dca50a688d7b6d5`.

The available later reference is `dist/return-network-support-v1/whileaway-gpt-support-return-network-closure-v1.zip`.
Its source, tools, and report are already present in the current main tree. They were inspected in place.

## Capability reconciliation

| Previous capability or file | Decision | Current evidence or required action |
| --- | --- | --- |
| Legacy save-format assertions that require format 4 | SUPERSEDED | Main uses format 5 and `ReturnNetworkSchema5GameTests`. Do not reintroduce old constants. |
| Transaction failure and preservation tests | SUPERSEDED | `ReturnNetworkIntegrationGameTests`, `ReturnNetworkTransactionGameTests`, and `ReturnNetworkPreservationGameTests` already cover stronger boundaries. |
| Signal semantic model and tests | SUPERSEDED as an incoming patch | `ReturnNetworkSignal`, `ReturnNetworkPlayback`, and their tests are already integrated. Preserve them. |
| `ReturnNetworkLoadAssessment` | STILL USEFUL | Existing read-only diagnostics distinguish readiness observations. They do not prove live updater execution or actual unload. |
| `tools/ci/return-network-support.init.gradle` | NEEDS REWORK for this support line | Evidence root is pinned to the old support track. Reuse the idea only with a new evidence root. |
| `tools/ci/probe-return-network-support.py` | NEEDS REWORK | Hardcoded old hashes and byte-identical runtime assumptions do not describe current production. Preserve as historical evidence. |
| `tools/ci/package-return-network-support.py` | NEEDS REWORK | Hardcoded `b5d26787` baseline and add-only ZIP logic are unsuitable for behavior-neutral extraction. |
| Old add-only lexical audit as the new acceptance gate | DROP | It rejects every existing-file change. This directive explicitly permits reviewed extraction. Do not weaken or rewrite the historical audit. |
| Duplicate persistence runner or schema framework | DROP | Current main already owns durable transactions, readback, quarantine, and migration. No replacement is needed. |

## Current adoption

No old archive was merged, copied over production, or handed off as a current patch.
This support line adds only current-API helpers, tests, client evidence tooling, and documentation.
Save format 5, optional Actor schema 1, Return Network schema 1, and checkpoint meanings remain unchanged.

## Remaining provenance gap

The two named tier-A archives remain unavailable for byte-level inspection.
If they appear later, inspect their manifests against the then-current main SHA before selecting any file.
Their absence does not authorize speculative patch reconstruction.
