# V2-01D / V2-02 living-town increment

## Current scope

V2-01 remains PARTIAL. V2-02 begins as a playable-content increment.
Story save format remains 5. Actor schema remains optional 1. Return Network schema remains 1.
Version remains 0.3.1-dev.1. Caveman remains HOLD.

## Content

New districts include a small caretaker home around Yeoul's existing residence anchor.
Its north and east doors preserve the arrival and return-light approaches.
Yeoul retains the same UUID, instance, generation, facts, and residence binding.
She checks the approach, tends the light, visits the workshop, rests, and returns home.
She swings her arm during light work and workshop inspection.
She reacts to changed return-light equipment without deleting or restoring blocks.
Active Return Network navigation still takes priority over ordinary routines.

The family room and boarding-house common room contain usable crafting tables and empty barrels.
Ordinary tables, seats, and bedding provide spaces without story clues.
The city dimension disables bed use. Bedding therefore uses non-explosive carpet mats instead of vanilla beds.
The existing workshop gains a smithing table, furnace, storage, and a real vanilla inspection circuit.

## Two optional discoveries

1. Inspect the soil-marked threshold at city coordinates 60,64,73.
2. Compare the marks outside with the clean indoor floor.
3. Enter the workshop through its existing southern doorway.
4. Read the work sheet at 66,65,46.
5. Operate the vanilla lever at 65,65,46.
6. Observe the floor lamp below the lever.
7. Read the work sheet again.

The second description changes only when the real lever and lamp agree.
Changed or removed equipment produces an uncertain description.
These discoveries grant no item, campaign fact, checkpoint, or automatic deduction.
Repeated visits do not create durable receipts.
Neither discovery uses a quest marker or a mandatory play day.

## Construction compatibility

City construction layout 3 appends the living-space cells to the unchanged revision-2 plan.
This version identifies the district construction cursor. It is separate from story save format 5.
Existing layout-1 and layout-2 saves retain their exact plans and cursor meanings.
Existing cities receive no automatic retrofit or rebuild.
Only newly constructed layout-3 cities receive this increment.
Provisional art remains based on revision 2. No final-art pass occurred.

Added cells accept only their expected generated underlay or an already placed target.
Unexpected blocks remain untouched. The cursor advances past those cells without later forced replacement.
Blocked paths use the existing navigation waiting policy.
The implementation adds no teleport fallback or runtime chunk forcing.
Initial district construction retains its existing bounded chunk-generation path.

Do not load a layout-3 world with older code that supports only layouts 1 and 2.
Package rollback does not downgrade saves or restore player progress.

## Manual launch

Run from the repository root:

```powershell
powershell -NoProfile -File tools/play-v2.ps1
```

The launcher selects the right secondary DISPLAY1 and mutes the isolated profile.
The profile is `../../work/v2-01d-manual-client`.
Create a new survival world for the primary path.
Continue the existing isolated world only when testing its saved Opening resume.
The launcher does not complete or skip story checkpoints.
Keep audio readability NOT TESTED during muted runs.

Use the client validation protocol for the full survival and Returner acceptance path.
The current direct UI run reached a new survival world and initial Opening movement.
It saved and exited normally. It did not reach Overworld survival or the remaining acceptance path.

## Authored-content preview

```powershell
powershell -NoProfile -File tools/play-v2.ps1 -Preview
```

This separate creative fixture prepares a new city and follows Yeoul's actual navigation.
It uses the existing Opening commit API to prepare the fixture.
It changes fixture time and positions the fixture player.
It tests the real switch, captures the workshop, saves, and exits.
Classify its result as SCRIPTED REAL CLIENT.
Do not treat it as natural survival, naive play, or human readability evidence.
The preview remains disabled in normal production launches.

## Support selection

MAIN adopted only the five evidence-tool files and the client protocol from support HEAD 613ff381665d0915ce8c378f1271fb08a3c17615.
MAIN did not adopt the three behavior-neutral production helpers or their sixteen GameTests.
The existing inline production rules already implement those decisions.
MAIN did not merge the support branch or the unverified tier-A bundles.
The support count of 130 GameTests is not MAIN's test count.

## Acceptance and next content

Keep V2-01 PARTIAL until the complete survival, Returner, recovery, revisit, and reload path is observed.
Keep human fear pacing and audio readability separate from technical execution.
Keep V2-02 PARTIAL until the new spaces and daily behavior receive player-experience review.
Expand ordinary residential use and the workshop's travel connections next.
Add Doyun and Miru's present-life behavior after this increment's gameplay review.
