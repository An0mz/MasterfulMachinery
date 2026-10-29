# Tiers

There are two kinds of tier, and they work separately:

- **Structure tier** — a number on a structure, like "Mk II". Recipes can require a tier with
  `minTier` / `maxTier`.
- **Port tier** — a `tierRank` number on a port. A structure can ask for "any energy port of tier 2
  or higher" in one spot.

## Tiered machines

A tiered machine is one controller with several structures, one per tier. Each structure lists the
same `controllerId` and its own `tier`:

```js
// kubejs/server_scripts/
MMEvents.createStructures(event => {
    event.create('kubejs:smelter_mk1')
        .name('Smelter Mk I')
        .controllerId('mm:smelter')
        .tier(1)
        .layout(layout => {
            layout.layer(['BBB', 'BBB', 'BBB'])
            layout.layer(['ICO', 'BEB', 'BBB'])
            layout.key('B', { block: 'minecraft:bricks' })
            layout.key('I', { portType: 'mm:item', input: true })
            layout.key('O', { portType: 'mm:item', input: false })
            layout.key('E', { portType: 'mm:energy', input: true })
        })

    event.create('kubejs:smelter_mk2')
        .name('Smelter Mk II')
        .controllerId('mm:smelter')
        .tier(2)
        .maxParallelRecipes(4)
        .layout(layout => {
            layout.layer(['NNN', 'NNN', 'NNN'])
            layout.layer(['ICO', 'NEN', 'NNN'])
            layout.key('N', { block: 'minecraft:nether_bricks' })
            layout.key('I', { portType: 'mm:item', input: true })
            layout.key('O', { portType: 'mm:item', input: false })
            layout.key('E', { portType: 'mm:energy', input: true, minTier: 2 })
        })
})
```

The same in JSON, one file per structure:

```json
{
  "name": "Smelter Mk II",
  "controllerIds": ["mm:smelter"],
  "tier": 2,
  "maxParallelRecipes": 4,
  "layout": { ... }
}
```

The controller forms as whichever structure is built around it, and its screen shows the tier.

**Make each tier differ in at least one spot.** If Mk II is just Mk I with more blocks around it, a
Mk II build also matches Mk I, and the machine can form as either. A machine that is already formed
keeps its structure for as long as that structure still matches, so an upgraded Mk I can stay Mk I.
Use a different casing, as above, or put a block in a spot that Mk I leaves empty and requires to be
`minecraft:air`.

### Tier from the name

Without `tier`, the number is read from the structure's name: "Tier 2", "Level 3", "Lvl 2",
"Mk II" and "Mk 2.5" all work, as do Roman numerals made of I, V and X. A name with none of these is
tier `1`. Setting `tier` is clearer and lets you name the structure anything.

Tiers can be decimals, like `1.5`.

## Recipes for tiers

A recipe runs on the structures in `structureId` and `structureIds`. Without a tier condition, it
runs on all of them. Add `minTier` / `maxTier` to limit it:

```js
// kubejs/server_scripts/
MMEvents.createProcesses(event => {
    // Every tier
    event.create('kubejs:smelt_iron')
        .structureIds('kubejs:smelter_mk1', 'kubejs:smelter_mk2', 'kubejs:smelter_mk3')
        .ticks(100)
        .input({ type: 'mm:input/consume', ingredient: { type: 'mm:item', item: 'minecraft:raw_iron', count: 1 } })
        .output({ type: 'mm:output/simple', ingredient: { type: 'mm:item', item: 'minecraft:iron_ingot', count: 1 } })

    // Mk II and up
    event.create('kubejs:smelt_netherite')
        .structureIds('kubejs:smelter_mk1', 'kubejs:smelter_mk2', 'kubejs:smelter_mk3')
        .minTier(2)
        .ticks(400)
        .input({ type: 'mm:input/consume', ingredient: { type: 'mm:item', item: 'minecraft:ancient_debris', count: 1 } })
        .output({ type: 'mm:output/simple', ingredient: { type: 'mm:item', item: 'minecraft:netherite_scrap', count: 1 } })
})
```

The JSON form is a `mm:tier` condition:

```json
{
  "structureId": "mypack:smelter_mk1",
  "structureIds": ["mypack:smelter_mk2", "mypack:smelter_mk3"],
  "ticks": 400,
  "conditions": [
    { "type": "mm:tier", "minTier": 2 }
  ],
  "inputs": [ ... ],
  "outputs": [ ... ]
}
```

| Key | Passes when |
|---|---|
| `minTier` | The structure's tier is this or higher |
| `maxTier` | The structure's tier is this or lower |

Use either or both. JEI shows the condition under the recipe, like "Structure tier 2 or higher".

Listing every tier in `structureIds` and limiting with `minTier` means a Mk IV added later only needs
adding to the list. Leaving a structure out of the list is just as good a way to keep a recipe off it.

### Better recipes at higher tiers

Give each tier its own copy of the recipe, with ranges that don't overlap:

```js
MMEvents.createProcesses(event => {
    const tiers = ['kubejs:smelter_mk1', 'kubejs:smelter_mk2', 'kubejs:smelter_mk3']

    event.create('kubejs:gold_slow')
        .structureIds(...tiers)
        .maxTier(1)
        .ticks(200)
        .input({ type: 'mm:input/consume', ingredient: { type: 'mm:item', item: 'minecraft:raw_gold', count: 1 } })
        .output({ type: 'mm:output/simple', ingredient: { type: 'mm:item', item: 'minecraft:gold_ingot', count: 1 } })

    event.create('kubejs:gold_fast')
        .structureIds(...tiers)
        .minTier(2)
        .ticks(80)
        .input({ type: 'mm:input/consume', ingredient: { type: 'mm:item', item: 'minecraft:raw_gold', count: 1 } })
        .output({ type: 'mm:output/simple', ingredient: { type: 'mm:item', item: 'minecraft:gold_ingot', count: 2 } })
})
```

If the ranges overlapped, a Mk II would have both recipes to pick from. Other ways to make a higher
tier better, without extra recipes:

- `.maxParallelRecipes(n)` on the structure, to run more recipes at once.
- `.boost(...)` on the structure, to speed up with nearby machines — see
  [`kubejs.md`](kubejs.md#boosts).
- Better ports, using port tiers below.

## Port tiers

Give a port a `tierRank` in its config. A port without one is tier `1`.

```js
// kubejs/startup_scripts/
MMEvents.registerPorts(event => {
    event.create('basic_power')
        .name('Basic Power Port')
        .controllerId('mm:smelter')
        .only('input')
        .config('mm:energy', c => { c.capacity(10000); c.maxReceive(100); c.maxExtract(0); c.tierRank(1) })

    event.create('advanced_power')
        .name('Advanced Power Port')
        .controllerId('mm:smelter')
        .only('input')
        .config('mm:energy', c => { c.capacity(100000); c.maxReceive(1000); c.maxExtract(0); c.tierRank(2) })
})
```

```json
{ "id": "advanced_power", "type": "mm:energy", "controllerIds": ["mm:smelter"], "config": { "capacity": 100000, "maxReceive": 1000, "maxExtract": 0, "tierRank": 2 } }
```

A structure key asks for a range of port tiers with `portType`:

```js
layout.key('E', { portType: 'mm:energy', input: true, minTier: 2 })
layout.key('F', { portType: 'mm:fluid', input: true, minTier: 1, maxTier: 3 })
```

```json
"E": { "portType": "mm:energy", "input": true, "minTier": 2 }
```

`minTier` / `maxTier` only work with `portType`. A key with `port` asks for one exact port, so tiers
don't apply.

Port tiers don't change the structure's tier, so a recipe's `minTier` can't see them. To make better
ports unlock recipes, give the higher tier structure a key that needs them, as `smelter_mk2` does
above.

### Building with tiers

When a structure accepts several tiers of a port, the controller's **Assemble** button and the
Multiblock Tool let you pick which tier to place for each kind of part. Where a structure doesn't
take the tier you picked, the nearest tier it allows is used.

## See also

- [`reference.md`](reference.md#structure-keys) — every structure key.
- [`reference.md`](reference.md#conditions) — every recipe condition.
- [`kubejs.md`](kubejs.md) — every KubeJS method.
