[MAIN GPT-6.1 SOL RESULT — V2-01E]
Current: 0.3.1-dev.1 / work/v2-playable-core / V2-01 PARTIAL / V2-02 PARTIAL.
Save 5; Actor optional 1; Return Network 1; construction plan 3; Caveman HOLD; 0.3.2 NOT PROMOTED.

BASELINE
Local and remote starting HEAD: 1878b6ee8414ddde1a2b3f6291067b4f0fb5ba71.
Production fix inherited from 9b0881a866a1bf85bdcc94da296a86cdff4e2818.
Current tool/package implementation: 1aeb9f164c46efe185904ff3b27edb95e540da9b.
The implementation commit was pushed and remotely verified on work/v2-playable-core.
A subsequent evidence-only commit records this report. Read its final HEAD from the branch.
Baseline JAR: C:\Users\scarl\Documents\Codex\2026-09-08\new-chat\work\return-network-integration\dist\v2-01e\whileaway-0.3.1-v2-01e.jar
JAR SHA256: 5adb5f964d42e1e9355a79fc491db8c32340de5e9a172748a26c8d926c7bcc4a.
Baseline source ZIP SHA256: bcb05cb3ad5b1c93bd4db28d221a823d418bbe3453dba63ddb48940617e1c370.
Earlier original JAR 3c92f0fb7cf1d7981d3bef1b6cc14787c6eff408d8166cdf87a694c8f6050460 remains preserved separately.

GAME CONTENT / ACTUAL PLAY
No new production content or gameplay code was added during this continuation.
The modified fresh test world was resumed at its activated Opening lever.
The player walked through the remaining memory gaps and returned to the Overworld.
Opening completion, movement, normal HUD, health, hunger and inventory were visible.
The player gathered four oak logs and crafted planks, sticks, a table, a wooden pickaxe and a wooden axe.
The player placed, used and recovered the table through normal controls.
A fresh development process retained the completed Opening and both tools.
The player explored forest, plains and a river without debug navigation.
The player mined three stone blocks and collected three cobblestone.
The Stone Age advancement appeared. The player placed dirt and returned to the surface.
Final saved health=20, food=20, difficulty=0, openingStage=COMPLETED.
Final inventory: sapling 1, table 1, wooden pickaxe 1, wooden axe 1, oak log 1, dirt 19, cobblestone 3.
Food acquisition and a finished shelter remain NOT TESTED. Cow attacks produced no confirmed kill or food.
The station, Yeoul, Return Network, NoEvent, Returner and revisit were not reached during this continuation.
Their required normal, crisis, unseen and interrupted client paths remain NOT TESTED here.
No full natural end-to-end completion is claimed.

TEMPORARY TEST PROTECTION
A vanilla creeper damaged the player before the latest user instruction.
The user then authorized easier or protected testing.
The isolated primary world was changed through the GUI from Normal to Peaceful.
The player remained in Survival. No items, coordinates or story facts were injected.
Peaceful restored health and hunger. This does not prove normal-difficulty survival pressure.
Restore Normal on separate copies before threat, warning, crisis or death acceptance tests.
The test difficulty is still Peaceful at handoff.

INFRASTRUCTURE CHANGES
Modified: tools/validation/play-input/java/wwa/input/InputAssist.java.
The PauseScreen branch retains a bounded input request until normal gameplay resumes.
The optional pauseAfter field opens the ordinary pause menu after the input lease ends.
Limits remain two leases, forty ticks per lease and three seconds per lease.
Menus, death, focus loss and dimension changes still release held input.
Modified: tools/ci/v2-01e.init.gradle.
The isolated profile now uses gamma=0.98, matching its tested GUI setting.
The helper remains outside the production JAR.
No src/main code, checkpoint, schema, Actor identity, transaction or write guard changed.

INSTALLED JAR SMOKE
Two separate forgeclient processes loaded the final candidate JAR.
The installed profile contained WhileAway and GeckoLib only. It contained no input-assist mod.
A 37-file copy of the earlier installed test world was used.
The preserved source world still matched all 37 original file hashes after testing.
The memory corridor, HUD and inventory opened normally.
Eight normal W taps moved the saved player from x=2.710139984645783 to x=3.557310519885731.
The second process loaded the same saved position and openingStage=STARTED.
Both installed processes exited normally with status 0.
This is an installation/load/movement/UI/save/reopen smoke. It is not an installed Opening completion.

VALIDATION
New development processes: 2. New installed processes: 2. All four exited normally with status 0.
New compilePlayInputJava: PASS. The development Gradle process ended BUILD SUCCESSFUL, exit 0.
New GameTests: NOT RUN. Full production build/core tests: NOT RERUN.
Inherited evidence only: 120 GameTests PASS; 176858 core assertions PASS; build PASS.
Current runtime/package comparison: 157 classes and 78 resources match, total 235 entries.
The production JAR is byte-identical to the starting candidate.
The source ZIP contains 374 files and matches the selected working-tree files.
Git text normalization differs for some unchanged tools/docs. Normalized content differs from baseline in exactly the two stated files.
HUMAN EXPERIENCE: NOT TESTED. AUDIO READABILITY: NOT TESTED. All clients were muted.
Plan 1/2 compatibility, quiet_city survival and participant/Returner acceptance remain NOT TESTED this continuation.

FAILURES / CORRECTIONS
Creeper damage: ordinary vanilla hazard during input delivery latency. No production defect was established.
Pause/input latency: queued requests and automatic pause were implemented and verified in the actual client.
Menu-close mining: immediate attack leases produced no mining. A six-tick empty lease before attack enabled normal mining.
This is an input-harness limitation. It is not evidence of a production mining defect.
A request omitted turnY and raised NullPointerException. It released input and was corrected with turnY=0.
Short end-frame captures can show a previous frame or pause overlay. Disk checks and later visible frames qualify the evidence.
Installed resource loading briefly showed Not Responding, then recovered and entered the world.
A broad source archive comparison was slow. A source-only comparison then confirmed exactly two normalized changes.

ARTIFACTS
MODIFIED_FILE: C:\Users\scarl\Documents\Codex\2026-09-08\new-chat\work\return-network-integration\dist\v2-01e-continuation\whileaway-0.3.1-v2-01e-continuation-source.zip
DIFF_FILE: C:\Users\scarl\Documents\Codex\2026-09-08\new-chat\work\return-network-integration\dist\v2-01e-continuation\DIFF.patch
VERIFICATION: C:\Users\scarl\Documents\Codex\2026-09-08\new-chat\work\return-network-integration\dist\v2-01e-continuation\VERIFICATION.txt
ROLLBACK: C:\Users\scarl\Documents\Codex\2026-09-08\new-chat\work\return-network-integration\dist\v2-01e-continuation\ROLLBACK.sh
Unchanged production JAR copy: C:\Users\scarl\Documents\Codex\2026-09-08\new-chat\work\return-network-integration\dist\v2-01e-continuation\whileaway-0.3.1-v2-01e-continuation.jar
Source ZIP SHA256: f3ab52c1fa83b896b89a9cc72765ab2019374d7f8e27c58ed2868d678878a77c.
The manifest records sizes and hashes. ROLLBACK.sh has Git executable mode 100755.

EXACT PACKAGE COMMANDS
Run commands from C:\Users\scarl\Documents\Codex\2026-09-08\new-chat\work\return-network-integration.
BASELINE command: python -m zipfile -t dist/v2-01e/whileaway-0.3.1-v2-01e-source.zip
Input: preserved baseline source ZIP. Literal output: Done testing. Exit=0.
MODIFIED command: python -m zipfile -t dist/v2-01e-continuation/whileaway-0.3.1-v2-01e-continuation-source.zip
Input: modified source ZIP. Literal output: Done testing. Exit=0.
ROLLBACK command: & 'C:\Program Files\Git\bin\bash.exe' dist/v2-01e-continuation/ROLLBACK.sh evidence/v2-01e/continuation/rollback-test/source.zip
Input: separate copy of the modified source ZIP. Exit=0.
Literal output:
ROLLBACK_SOURCE_PACKAGE_SHA256=bcb05cb3ad5b1c93bd4db28d221a823d418bbe3453dba63ddb48940617e1c370
WORLD_FILES_TOUCHED=0
Restored status: queue=False; pauseAfter=False. These are source-package checks, not gameplay rollback claims.
The delivered modified ZIP remains changed. Rollback restores the previous source/tool package only.
It does not downgrade saves, replace world files, restore player progress or change the JAR.
The previous JAR-specific rollback remains under dist/v2-01e.

SAFE PAUSE / NEXT
The last pre-closure usage check showed five-hour remaining=14%, weekly remaining=60%.
No further client session was started. All launched games were already saved and stopped.
No usage reset was consumed.
Resume the isolated V2-01E Signal Fix world at the surface with the saved tools and cobblestone.
Do not repeat the Opening. Continue natural survival, food/shelter and station discovery.
Use Normal-difficulty copies for incompatible Returner acceptance paths.
Keep V2-01 and V2-02 PARTIAL until the required continuous path and remaining client gates are actually verified.
