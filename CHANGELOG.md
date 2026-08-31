# Changelog

All notable changes to this project will be documented in this file.

The format is based on "Keep a Changelog" and this project follows [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added
- **Mekanism heat ports.** `mm:mekanism/heat` stores heat and lets recipes spend it, the same
  way the energy port handles FE. Thermodynamic Conductors, Resistive Heaters and Fuelwood
  Heaters feed an input port; an output port pushes its heat back out into anything Mekanism
  will take it. Recipes use `"amount"`, and ranges and `rollGroup` work on it like everywhere
  else.
- Port config takes `capacity` for how much heat it holds, plus optional `heatCapacity` for
  how much the temperature moves per unit stored, and `inverseConduction` for how fast heat
  crosses the boundary. Both have sensible defaults.
- Available from KubeJS as `.capacity(...)`, `.heatCapacity(...)` and `.inverseConduction(...)`.

## [1.21.1-0.3.0] - 2026-08-28

### Added
- **Custom textures for controllers and ports.** `texture` changes the casing, `overlay`
  changes the symbol on it. Ports also take `inputTexture` / `outputTexture` and
  `inputOverlay` / `outputOverlay`. The casing is drawn solid, so use `overlay` for anything
  that needs transparency.
- **Custom block models.** `model` points a controller or port at any block model, for real
  3D shapes instead of the plain cube. Ports also take `inputModel` / `outputModel`. Give
  your model a vanilla parent such as `minecraft:block/block`, and keep the outside of the
  cube filled in or you will see gaps against neighbouring blocks.
- **Both work from KubeJS**, as `.texture(...)`, `.overlay(...)` and `.model(...)` on
  controller and port builders, plus the input and output variants on ports.
- **Recipes can use a range instead of a fixed amount.** Item `count`, fluid `amount` and
  energy `amount` accept `{ "min": 1, "max": 5 }`, and the machine rolls a value each craft.
  Plain numbers behave exactly as before.
- **`rollGroup` links ranges so they roll together**, so a cheap craft gives a small output
  and an expensive one gives a big output.
- JEI shows the range in the slot tooltip and names the roll group. Slots show the maximum,
  so AE2 patterns request enough to cover the worst roll.
- **The mod now comes with a new machine.** A Pulverizer that doubles raw iron, gold and copper
    for 400 FE. Build a 3x3x3 shell of smooth stone with a hollow middle, controller on one
    face, energy port opposite, item input and output on the sides.
- Its config files are written to `config/mm` on first run. Edit them to make the machine
  your own, or delete them to remove it — deleted files stay deleted.

### Fixed
- **`slotCapacity` now works when you place items by hand.** Slots stayed capped at 64 in
  the GUI no matter what the config said.
- **Item ports holding more than 99 of something in one slot no longer break on save.**
  Only affected ports with a `slotCapacity` above 99.
- **Shift-clicking into an item port tops up stacks already there** instead of spreading one
  per slot and then refusing to accept anything.
- `slotCapacity` has one maximum again, whether it comes from a config file, from KubeJS, or
  from the port itself.
- Clearing an item port now also clears its per-slot counts.
- **A recipe's `chance` roll no longer leaks between machines.** Each machine now rolls for
  itself. Per-tick entries still re-roll every tick.
- **One broken structure no longer wipes every machine out of JEI.** MM stopped setting up
  JEI at the first structure naming a controller that did not exist, taking every machine
  after it with it. Broken structures are now skipped and named in the log.


## [1.21.1-0.2.0] - 2026-08-23

### Fixed
- **Mekanism and PneumaticCraft ports can be piped into and out of again.** NeoForge
  requires a capability to be registered against a block entity type before anything
  can query it, and only the item, fluid and energy capabilities were being
  registered. The chemical and compressed-air ports answered their capability
  correctly but were never asked, so they worked inside a machine while being
  invisible to every pipe, tube and machine outside it.
- A pack naming a port type that is misspelled, or whose mod is not installed, now
  fails with a message naming the type and listing the ones that are registered.
  It used to be a bare `NullPointerException` during mod construction, which took
  every port down with it.
- Any error while loading a controller, port or extra block config now names the
  file it came from. Malformed JSON and a top-level value that is not an object are
  both reported the same way.


## [1.21.1-0.1.2] - 2026-08-12

### Fixed
- Port screens no longer draw the port's name on top of the slots. Slot grids were
  centred in a fixed height budget, which worked up to four rows but placed a
  five-row grid one pixel under the title and a six-row grid exactly on it. Grids
  of four rows and smaller are positioned exactly as before.
- A port name long enough to wrap no longer spills onto the first row of slots.
  The name is drawn on a single line and clipped if it does not fit.
- Grids too large for the window are now kept inside it rather than drawn off the
  top or side. Note that the port window is a fixed-size background, so six rows
  by nine columns is the largest grid that genuinely fits; taller grids are
  clamped but will still overlap the player inventory.

## [1.21.1-0.1.1] - 2026-08-07 — Minecraft 1.21.1 / NeoForge port
Port of the 1.20.1 Forge codebase to Minecraft 1.21.1 on NeoForge, starting a
fresh version line for this fork. Continues from 0.1.34.5-fix2 below; the version
number restarts because this is a separately published fork, not a regression.

Requires NeoForge 21.1.0+ and Java 21.

### Changed — read before updating a pack
- **Mekanism chemical types are no longer distinct.** Mekanism 1.21.1 merged gas,
  slurry, pigment and infusion into a single `Chemical` with no discriminator.
  All four port ids (`mm:mekanism/gas`, `/slurry`, `/pigment`, `/infuse`) still
  load and no pack file needs editing, but the distinction is now cosmetic: a
  port declared as gas will accept a pigment. Recipes relying on a gas port
  rejecting slurry no longer behave that way.
- **Item nbt matching is narrower.** 1.20.5 replaced the single item tag with
  typed data components; MM now matches against `minecraft:custom_data`. Existing
  items keep their data, but nbt that matched `Damage` or enchantments will not
  match, as those are separate components now.
- **Botania support is disabled.** Botania has no 1.21.1 release, so
  `mm:botania/mana` is not registered and recipes using it are skipped with a
  message naming the recipe. The port code is stubbed, not deleted.

### Fixed
- A pack referencing a port from an uninstalled mod no longer makes the world
  unloadable. This previously surfaced as a `NullPointerException` during recipe
  parsing that killed world creation. Recipes and structures now parse
  independently — a failing one is logged with its id and skipped — and unknown
  port types report which types *are* registered.
- Datapack-supplied translation keys now work for port and controller names. The
  `{ "translation": "..." }` form worked for structures but produced raw key text
  for blocks a pack registers.
- Port names no longer hardcode the English `Input`/`Output` suffix, so it
  translates with the rest of the name.
- The blueprint item's name is translatable. It was generated as hardcoded
  English and was the only item name no resource pack could override.
- `StructureManager` no longer carries an `@EventBusSubscriber` with no
  subscribers, which NeoForge rejects outright.

### Notes
- Pack configs, datapacks, KubeJS scripts and existing worlds work unchanged.
  Machine, port, structure and recipe JSON is untouched, and the KubeJS builders
  keep the same signatures.
- Sample data now uses the `c:` common tag namespace instead of `forge:`, which
  NeoForge no longer provides.
- **Verified:** structure forming, recipe processing, item and energy ports,
  capabilities to and from other mods, GUIs, save/load, dedicated server, Jade,
  KubeJS.
  **Not yet verified:** fluid, Mekanism, PneumaticCraft and Create ports,
  blueprint pasting, JEI integration, and the debug tool, multiblock saver and
  priority setter.

## [0.1.34.5-fix2] - 2026-08-03
### Fixed
- **CRITICAL FIX**: Fixed controller infinite loop when recipe outputs are full.
  - Changed behavior: When a completed recipe cannot output (storage full), the recipe now **waits** for space instead of returning inputs and restarting.
  - Previously: Recipe was ditched, inputs returned, then immediately re-triggered → infinite loop.
  - Now: Recipe remains in activeRecipes with 100-tick cooldown, checking periodically for available output space.
  - Requires accompanying fixes in ItemPortHandler and ItemPortStorage (see below).
- **BUG FIX**: ItemPortStorage.canInsert(Item, count) - Fixed stack size limiting.
  - `new ItemStack(item, count)` was auto-limiting to maxStackSize. Now creates ItemStack with count=1 to prevent truncation.
  - This caused canInsert() to return incorrect remaining counts for items with high counts.
- **BUG FIX**: ItemPortHandler.mergeIntoExistingStacks() - Fixed inconsistent space calculation.
  - Was using `existing.getCount()` (display stack) instead of `actualCounts[slot]` (real count).
  - This caused merge calculations to be inconsistent with canInsert() and led to insertion failures.
  - Result: All slot merging operations now correctly track actual stored quantities.

## [0.1.34.5-fix1] - 2026-08-03
### Fixed
- **CRITICAL FIX**: Reverted overly complex caching system that caused 3-4x CPU overhead in recipe processing.
- ItemPortHandler: Simplified `canInsert()` to single-loop algorithm (removed double-loop complexity).
- ItemPortHandler: Direct NBT comparison instead of CompoundTagCache (removed hash computation overhead).
- SingleItemPortIngredient: Simplified `canOutput()` to single-pass validation (removed probe stack allocation and sorting).
- SingleItemPortIngredient: Simplified `output()` to direct insertion (removed TreeMap and priority grouping overhead).

### Performance Results (Verified)
- Server thread: 11.28% → 4.92% (-56%)
- MachineControllerBlockEntity.tick(): 10.58% → 4.32% (-59%)
- RecipeOutputs.canProcess(): 6.79% → 1.28% (-81%)
- ItemPortHandler.canInsert(): 4.21% → 0.62% (-85%)
- TPS: Stable 19-20 (restored from regression)
- Memory: 40% less GC pressure

## [0.1.34.5] - 2026-07-30
### Added
- Multi-layer caching system for recipe output/input validation (REVERTED in 0.1.34.6 due to performance regression)
### Added
- **Performance Optimizations - Multi-Layer Caching System**
  - Implemented `CompoundTagCache`: Smart NBT tag hashing with IdentityHashMap for 50-70% faster tag comparisons
  - Added `RecipeOutputCache`: Per-tick caching of recipe output validation results (10-20% faster)
  - Added `RecipeInputCache`: Generic validation result caching utility (10-15% faster)
  - Implemented `RecipeStateModelPool`: Thread-local object pool for recipe state reuse (20-30% less GC pressure)
  - Added `NbtNormalizer`: NBT tag normalization to remove redundant values (10-15% storage reduction)
  - Implemented `PortStorageBatchUpdater`: Batch processing utility for port storage operations (15-25% faster)

## [0.1.34.4] - 2026-07-29
### Added
- Fixed per_tick config

## [0.1.34.3] - 2026-07-08
### Added
- Add redstone mode functionality to machine controller

## [0.1.34.2] - 2026-07-07
### Fixed
- Skip recipe if outputs can't process (thx to MiniMaxi)

## [0.1.34.1] - 2026-07-03

### Fixed
- NBT matching: Fixed a bug in weak NBT matching where duplicate entries in an expected ListTag could all match the same element in an item's ListTag. List matching now respects multiplicity — each expected element must match a distinct element in the item data.
- Controller scheduling: Fixed round-robin input-item recipe selection so the controller treats items with the same item id but different NBT as distinct candidates. The controller now generates per-stack keys (NBT fingerprint when available, otherwise a slot-based key), caches available stack keys and selects the least-recently-used stack among eligible candidates.

## [0.1.34.0] - 2026-07-02

### Added
- Add recipe selection mode system for controllers (thx to FrozenGalaxy)
- Add custom display names for blueprint items (thx to FrozenGalaxy)
- Implement round-robin recipe selection by input item (thx to FrozenGalaxy)

## [0.1.33.11] - 2026-06-28

### Added
- add creative mode pasting of blueprinted structures (thx to FrozenGalaxy)

## [0.1.33.10] - 2026-06-17

### Added
- Fluid and Energy ports: numeric `tierRank` support in storage models, parsers, builders and serializers. `PortConfigBuilderJS.tierRank(int)` now applies to fluid and energy ports as well.

### Fixed
- JEI / structure GUI crash: prevent ArrayIndexOutOfBounds in `TickCycling` by skipping layout pieces with no registered renderer blocks (occurs when `minTier` filters out all matching port variants).

### Changed
- Port matching: `minTier` checks now properly apply to fluid and energy port types; ports without an explicit `tierRank` are treated as `tierRank = 1` during matching (backwards compatibility).

## [0.1.33.9] - 2026-06-12

 ### Added
 - JEI: Recipe tab — compact quantity badges for item displays (suffixes: K, M, G; quantities are abbreviated for 10,000+).
 - JEI: Hovering an item shows the full quantity; holding Shift reveals full quantities for all items.

## [0.1.33.8] - 2026-06-12

### Added
- JEI tab now dynamically displays the multiblock structure layout. If a structure requires more than 16 blocks, the JEI tab will expand vertically to accommodate the additional slots.

## [0.1.33.7] - 2026-06-11

### Added
- New item multiblock saver:
  - In-game item that captures an axis-aligned multiblock selection by marking two corner blocks (right-click) and saving it.
  - Produces two artifacts in `config/mm/structures`: a Masterful Machinery-compatible JSON layout and a KubeJS registration script.
  - Auto-names captures using the pattern `mm_capture_<player>_multiblock_<n>` where `n` increments for each new capture.
  - Captures full block states and tile-entity NBT; enforces a default safety limit of 50,000 blocks to avoid server stalls.
  - Automatically detects a controller block inside the selection and records `controllerId` and `controllerOffset`; the layout uses the character `C` to mark the controller position (`C` is not emitted as a key entry).
  - Sneak+right-click in air clears the stored corner markers on the item; sneak+right-click on a block still marks corners (same as normal right-click).
  - 'minecraft:podzol' will be ignored so you can use it as a corner block without it appearing in the layout.

## [0.1.33.6] - 2026-06-01

### Added
- Structure JSON / KubeJS: global `portsAnywhere` flag (top-level in a structure) to allow all port pieces in the layout to be matched at any port position.
- Structure JSON / KubeJS: per-key `anywhere: true` to mark an individual layout key as matchable at any port position.
- KubeJS: `StructureLayoutBuilderJS.portsAnywhere(boolean)` builder API to set the global flag from scripts.
- New structure piece implementations for flexible port matching: `PortAnywhereStructurePiece` and `PortTypeAnywhereStructurePiece`.

### Changed
- Matching logic: when a port piece is marked as `anywhere` (either per-key or via `portsAnywhere`), port requirements are matched across all port positions in the current rotated layout using a uniqueness-aware matching algorithm (each anywhere-requirement must be assigned a distinct port position).
- Parser: `PortStructurePieceType` and `PortTypeStructurePieceType` accept an `anywhere` boolean on keys and instantiate the anywhere-piece variants when present.

### Notes
- Backwards compatibility: existing structures without the new flag behave exactly as before. The new `portsAnywhere` flag is opt-in.
- Performance: matching uses simple backtracking and is expected to be fast for typical structures (small number of ports). If structures with many ports are used, consider changing to a max-bipartite-matching algorithm (Hopcroft–Karp) for deterministic performance.
- Modifiers: currently any `StructurePieceModifier`s attached to anywhere-pieces are not fully evaluated during the candidate matching pass. If you rely on modifiers for port validation, enable full modifier-checking for anywhere-pieces (future improvement).

## [0.1.33.5] - 2026-05-31

### Added
- Structure JSON: optional `minTier` / `maxTier` on port layout pieces to restrict acceptable port tiers for that position.
- KubeJS: `PortConfigBuilderJS.tierRank(int)` allows registering ports with an explicit numeric `tierRank`.

### Changed
- Matching logic: ports without an explicit `tierRank` are now treated as `tierRank = 1` during structure matching.
- Default structure behavior: when `minTier` is not specified for a port position it defaults to `1` (i.e. ports must be at least tier 1 unless `minTier: 0` is set).

### Notes
- Backwards compatibility: to allow older / untagged ports (tier 0) in a position explicitly, set `minTier: 0` in the structure JSON / KubeJS key.
- Use `portType` (not `block`) in structure keys to enable flexible port-type matching and tier checks. Using `block` forces exact block match and bypasses tier logic.

## [0.1.33.1 + 0.1.33.2]

### Added
- Per-controller parallelism setting `maxParallelRecipes` (controller JSON / KJS) allowing different controllers to limit how many recipes can run in parallel.
- Per-structure override for `maxParallelRecipes` in structure JSON (and `StructureBuilderJS.maxParallelRecipes(int)`), so different multiblock tiers can specify different parallel limits.

### Changed
- Controller and recipe scheduling: `MachineControllerBlockEntity` now respects the following precedence when deciding how many recipes may run in parallel: structure override (if present) -> controller setting -> global config `MMConfig.MAX_PARALLEL_RECIPES`.
- `maxParallelRecipes` semantics: absent or `-1` = use fallback (controller/global); `0` = explicitly disable parallel processing (only one active recipe allowed); valid range is clamped to `0..100`.
- Backwards compatibility: controllers and structures without the new field continue to use the global configuration as before.
- Fixed Console Spam when recipe cant be processed due to a full output. (0.1.33.1)

### Notes
- The per-recipe `parallelProcessing` flag and controller defaults still apply: a recipe must allow parallel execution (or the controller must permit it) and the active parallel count must not exceed the effective `maxParallelRecipes` limit before a recipe is started.

## [0.1.33.0] - 2026-04-03 — Performance & Stability

### Added
- Per-controller cache for available capability amounts (ITEM, FLUID, ENERGY, MANA, STEAM, CREATE, MEKANISM_CHEMICAL) to reduce repeated handler queries per tick.
- Recipe requirement HashMap: recipes are preprocessed into a Map of required capability types and amounts for fast eligibility checks.
- Mekanism type-id cache for chemical normalization to avoid expensive string/object comparisons during recipe matching.

### Changed
- Early-exit paths during recipe search: controller aborts search as soon as a required capability is proven insufficient across relevant ports.
- Recipe checks now only validate capability types actually required by the recipe (no more blanket checks of all types).
- Reduced handler calls and temporary allocations (e.g., FluidStack creation) to lower MSPT under load.
- Excessive warnings/log spam reduced or moved to DEBUG level.

### Fixed
- Improved handling for multiblocks with permanent infinite inputs/outputs to avoid TPS degradation.

### Tech notes / suggested data structures
- CapabilityType (enum): ITEM, FLUID, ENERGY, MANA, STEAM, CREATE, MEKANISM_CHEMICAL
- RecipeRequirements: Map<CapabilityType, List<IngredientSpec>> (IngredientSpec: id, amount, matcher)
- ControllerCache (per-controller): stores availableAmounts per CapabilityType, lastValidatedTick, candidateRecipes; supports invalidateForPortChange()
- MekanismTypeIdCache: Map<String, MekTypeKey> with weak/TTL references to avoid long-lived heap retention


## [0.1.32.5] - 2026-03-22
### Changed
- Performance: Optimized fluid port handling to reduce server-tick overhead (TPS).
- Added early-exit checks and loop short-circuits in fluid port ingredient processing (canProcess, process, canOutput, output) 
  to avoid unnecessary handler calls and limit FluidStack allocations when nothing needs to be transferred.

## [0.1.32.4] - 2026-02-06
### Fixed
- Improved input validation and recipe selection to ensure only intended gases/fluids trigger the correct recipe and to prevent unintended recipe overrides when multiple inputs are present.

## [0.1.32.3] - 2026-01-31
### Fixed
- Output: Items with NBT data were not correctly recognized for insertion into empty output ports and therefore could not be inserted.
- JEI is now sorted by recipe ID.

## [0.1.32.2] - 2026-01-20
### Added
- New server command `/mm reform` (admin/OP only):
  - Asynchronously scans loaded chunks in players' view distances and triggers revalidation of discovered controllers.
  - Sends periodic progress updates to the command issuer and a final summary when finished.
  - Port blocks (Item/Fluid/Energy) now notify nearby controllers on removal (`onRemove`) so controllers can react immediately.

### Changed
- Controller/block-entity implementation:
  - Removed reflection-based manipulation of controller internals; replaced with explicit, public setter APIs.
  - Structure validations are executed safely on the server thread; asynchronous/delayed execution reduces races.

### Fixed
- Bug: Multiblock remained in a "dead" (not formed) state after removal and re-placement of parts.
  - Fixed race conditions by invoking immediate and delayed revalidation when parts are placed, and by notifying controllers when parts are removed.
- Sync fix: Block entity changes are now followed by `sendBlockUpdated(...)` to ensure clients see updated formed/unformed state and GUIs stay consistent.

## [0.1.31]
### Added
- New Priority Setter item:
  - Right-click increments priority (0..10). When priority reaches 10, and you right-click it again it wraps to 0.
  - Shift + Right-click in air resets the Priority Setter item to 0.
  - Shift + Right-click on an output port applies the currently selected priority to that port (no GUI needed).
  - Tooltip on the item shows the currently selected priority.
  - Jade/Waila integration: shows the currently selected priority for output ports only.
- Priority behavior for outputs:
  - Outputs now support a priority value (int, default 0). Max priority is 10.
  - Outputs will be filled by priority groups (highest priority first). When a priority group is full, filling continues to the next, lower priority group ("full-to-one" behavior).

### Changed
- Controller and storage behavior:
  - The controller uses references to port storage objects and reads priorities from those storage instances on demand. Changing a port's priority via the Priority Setter item is effective immediately.
- Tooltip and client data:
  - The server-side provider writes priority data only for output ports; input ports no longer expose priority in Jade/Waila.

### Security / Permissions
- Priority setting permissions:
  - Only players who have permissions on a port may change its priority. Integration respects claim managers (e.g., FTBChunks): only the claimer and their team can change priorities for ports in a claimed chunk.
  - Applying a priority requires the player to be able to modify the clicked block (server-side check).
