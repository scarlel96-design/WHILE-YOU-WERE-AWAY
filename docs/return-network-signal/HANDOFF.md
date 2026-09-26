# Return Network First Response — final-integration handoff

## Current

《네가 없는 동안》 0.3.1-dev.1, `work/return-network-integration`. First compound event **PARTIAL**; campaign **PARTIAL**; 0.3.2 not promoted. Save format 5, optional Actor schema 1, Return Network schema 1, city art revision 2. Caveman held. No next subsystem started.

## Baseline and changes

Input commit `2bbef42cad5d35a569ab1744f39e31770d56b7f0`; baseline JAR SHA-256 `9e90a695dfa14ab193849335cb28dffa0d27cb8d212cc87387e260b0f8cb637e`, source ZIP `9b77d81bea39c4cea7673fa604e6f8d0af9e6852943ff3772e2d9567fdef1f17`. Both remain in `evidence/return-network-signal/baseline/`.

`ReturnNetworkEvents` connects a transient SHORT–SHORT–LONG presentation to the existing story event. A 10-tick lead is followed by 12-tick SHORT, 16-tick gap, 12-tick SHORT, 16-tick gap, and 36-tick LONG. The relay's persistent LIT state is not flickered. Vanilla electric-spark particles and quiet bell notes are separate presentation carriers. A complete session plus an explicit interaction by the same player is required before the existing cp3/cp5 observation commits. Emission alone does not fabricate observation. A missed tick, unavailable component, or interrupted session restarts the complete pattern with a fresh runtime session. No pulse progress is saved.

`ReturnNetworkPlayback` holds the runtime playback contract. `ReturnNetworkPlaybackGameTests` adds six GameTests. `ReturnNetworkSignalSmoke` and the Gradle/PowerShell matrix tools provide opt-in actual-client evidence. `ReturnNetworkSmoke` was adjusted only in its smoke assertion: exact single canonical actor identity remains required even if the diagnostic search reports unloaded/unconfirmed during entity readiness. The protected save, actor, transaction, integrity, and navigation classes remain byte-unchanged from the input commit. Existing durable checkpoint meanings 0–6, candidate/integrity/write/readback/hash/publish ordering, and save schemas remain unchanged. Existing old cp3/5/6 saves remain valid without replay.

## Evidence and limits

Final-source `runGameTestServer build`: 108 GameTests PASS, 176,857 core assertions PASS, exit 0. Standalone signal tests: 27/27 PASS, exit 0. Standalone load diagnostics: 53 assertions PASS, exit 0. Final evidence audit: 27 accepted actual-client runs from 31 attempts; four failed command rows remain recorded. The clients used silent audio and right secondary DISPLAY1. Actual runs cover normal cp0–6, signal cuts/restarts, completion replay, NPC/relay availability, environment/path/light loss and recovery, Nether, real chunk unload, death with KeepInventory OFF, and Return-Light regression. See `evidence/return-network-signal/GATES.json`, `commands.jsonl`, `tests-2.log`, and individual client evidence.

The four retained failures were: CutBefore-1 wrapper exit 1 after an intentional hard process cut (boundary and disk state separately confirmed); Reopen-4 stopped before completion at cp4 and Reopen-5 completed the same world; Reopen-10 smoke canonical-count diagnostic failed at cp6, while the story file retained one actor and Reopen-11/13 passed fresh-process reads of that world; Nether-1 stopped at cp4 and Nether-2 passed on a separate copy. The precise transient readiness cause of Reopen-10 remains unproven. No production actor-identity policy was altered to mask it.

No unaided human scene-readability playtest or audible listening was performed. Visual frames and tick logs demonstrate distinct pulse intervals and spatial anchoring, but they do not prove a player recognizes SHORT–SHORT–LONG without explanatory text. This is the seal blocker. First compound event therefore remains **PARTIAL**, and the next subsystem remains **NOT STARTED**.

## Next start point

Run an unaided scene-readability check on the exact packaged JAR, with both visual accessibility and audible rhythm assessed if the silent-client policy is lifted. Reproduce or explain the Reopen-10 entity-readiness diagnostic, then perform the final full-event regression and decide SEALED only if no critical blocker remains. Do not begin Item/Reward/Structure systems or 0.3.2 promotion as part of this closure.
