# KubeJS

Everything Masterful Machinery can do from KubeJS, in one place. Every method is listed, and the
newer features each get a full example. For what a key means in depth, follow the links to
[`reference.md`](reference.md) and the other docs.

## The events

| Event | Script folder | Makes | Reload |
|---|---|---|---|
| `MMEvents.registerControllers` | `kubejs/startup_scripts/` | Controller blocks | Restart |
| `MMEvents.registerPorts` | `kubejs/startup_scripts/` | Port blocks | Restart |
| `MMEvents.registerExtraBlocks` | `kubejs/startup_scripts/` | Decorative blocks | Restart |
| `MMEvents.createStructures` | `kubejs/server_scripts/` | Structures | `/reload` |
| `MMEvents.createProcesses` | `kubejs/server_scripts/` | Recipes | `/reload` |
| `MMEvents.recipeStarted` | `kubejs/server_scripts/` | Runs when a recipe is about to start | `/reload` |
| `MMEvents.recipeFinished` | `kubejs/server_scripts/` | Runs when a recipe finishes | `/reload` |
| `MMEvents.builderStructures` | `kubejs/server_scripts/` | Structure Builder entries | `/reload` |

Rules that catch everyone at least once:

- An event in the wrong folder silently never runs.
- Controllers, ports and extra blocks always register as `mm:<id>`. Write `event.create('alloy_kiln')`,
  then refer to it as `mm:alloy_kiln` everywhere else.
- Structures and processes need the namespace written out: `event.create('kubejs:alloy_kiln')`.
- A misspelled method crashes the game with `TypeError: Cannot find function ... in object ...`. The
  object named tells you which builder you were in. Names are case sensitive.
- One mistake stops the rest of that script file, so everything after it never registers. Check the
  KubeJS log first when something is missing.
- After changing structures with `/reload`, run `/mm reform` so built machines check again.

## Controllers

```js
// kubejs/startup_scripts/
MMEvents.registerControllers(event => {
    event.create('alloy_kiln')
        .type('mm:machine')
        .name('Alloy Kiln')
})
```

| Method | Meaning |
|---|---|
| `.type(id)` | Required. `'mm:machine'`, or `'mm:single'` for a machine in one block |
| `.name(name)` | Display name. Text or a styled object — see [Names](#names) |
| `.texture(id)` | Base texture, e.g. `'kubejs:block/kiln_base'` |
| `.overlay(id)` | Texture drawn on top of the base |
| `.model(id)` | A full block model to use instead |
| `.unformedOverlay(id)`, `.idleOverlay(id)`, `.workingOverlay(id)` | The overlay while unformed, formed and idle, or working |
| `.unformedModel(id)`, `.idleModel(id)`, `.workingModel(id)` | The model while unformed, formed and idle, or working |
| `.geoModel({...})` | An animated GeckoLib model — see [Animated models](#animated-geckolib-models) |
| `.parallelProcessingDefault(bool)` | Recipes run in parallel unless the recipe says otherwise |
| `.maxParallelRecipes(n)` | How many recipes run at once, `0`–`100` |
| `.recipeSelectionMode(mode)` | `'default'`, `'avoid_same_recipe'`, `'round_robin_input_item'` or `'manual'` |
| `.workingSound(id)` | A sound played while working, e.g. `'minecraft:block.blastfurnace.fire_crackle'` |
| `.workingSoundInterval(ticks)` | Ticks between sounds. Default `40` |
| `.workingParticle(id)` | A particle around the controller while working, e.g. `'minecraft:smoke'` |
| `.unformedColor(hex)`, `.idleColor(hex)`, `.workingColor(hex)` | The controller screen's colour per state, as `'#RRGGBB'` |
| `.slot(id, type, input, config)` | Single-block machines only — see [Single-block machines](#single-block-machines) |

### Looks per state

```js
MMEvents.registerControllers(event => {
    event.create('alloy_kiln')
        .type('mm:machine')
        .name('Alloy Kiln')
        .texture('kubejs:block/kiln_base')
        .overlay('kubejs:block/kiln_front')
        .workingOverlay('kubejs:block/kiln_front_lit')
        .workingSound('minecraft:block.blastfurnace.fire_crackle')
        .workingSoundInterval(30)
        .workingParticle('minecraft:smoke')
        .workingColor('#FF8800')
})
```

The kiln shows `kiln_front` while unformed or idle and `kiln_front_lit` while it works. A state with
no method of its own uses `.overlay(...)` or `.model(...)`. Textures go in
`kubejs/assets/kubejs/textures/block/`.

### Animated GeckoLib models

Needs GeckoLib.

```js
MMEvents.registerControllers(event => {
    event.create('star_forge')
        .type('mm:machine')
        .name('Star Forge')
        .geoModel({
            model: 'mypack:geo/star_forge.geo.json',
            texture: 'mypack:textures/block/star_forge.png',
            animations: 'mypack:animations/star_forge.animation.json',
            idle: 'animation.star_forge.idle',
            working: 'animation.star_forge.spin',
            offset: [0, 1, -1],
            scale: 1.0,
            rotate: true,
            glow: false
        })
})
```

Only `model` and `texture` are required. The files go in a resource pack, for example
`kubejs/assets/mypack/geo/star_forge.geo.json`. What each key does is in
[`models.md`](models.md#animated-geckolib-models).

`.geoModel(...)` works on structures too, and a structure's model wins over the controller's.
`.geoModel('none')` on a structure hides the controller's model for that structure.

## Ports

```js
// kubejs/startup_scripts/
MMEvents.registerPorts(event => {
    event.create('kiln_items')
        .name('Kiln Hatch')
        .controllerId('mm:alloy_kiln')
        .config('mm:item', config => {
            config.rows(2)
            config.columns(2)
        })
})
```

This makes `mm:kiln_items_input` and `mm:kiln_items_output`.

| Method | Meaning |
|---|---|
| `.config(type, config => {...})` | Required. The port type and its settings — see [Port config](#port-config) |
| `.controllerId(id)` | Required. Call once per controller the port works with |
| `.name(name)` | Base name. " Input" / " Output" is added |
| `.inputName(name)`, `.outputName(name)` | The full name for one side, instead of `name` |
| `.only(side)` | `'input'`, `'output'` or `'both'` (default) |
| `.texture(id)`, `.overlay(id)`, `.model(id)` | As on controllers |
| `.inputTexture`, `.outputTexture`, `.inputOverlay`, `.outputOverlay`, `.inputModel`, `.outputModel` | The same, for one side only |

`mm:ae2/pattern` and `mm:replication/link` have no sides and make a single block, `mm:<id>`.

### Port config

Every type also takes `config.tierRank(n)`, which structures can ask for with `minTier` / `maxTier`.

| Type | Needs | Config methods |
|---|---|---|
| `mm:item` | — | `rows`, `columns`, `slotCapacity`, `autoPush` |
| `mm:fluid` | — | `rows`, `columns`, `slotCapacity` (required, mB per tank), `fluids(...ids)`, `autoPush` |
| `mm:energy` | — | `capacity`, `maxReceive`, `maxExtract`, `autoPush` |
| `mm:entity` | — | `capacity`, `mode`, `invulnerable`, `immobile`, `silent`, `persistent`, `consume`, `speedPerEntity`, `zone(size)` or `zone(w, h, d)`, `entity(id)`, `tag(id)` — see [`entity.md`](entity.md) |
| `mm:mekanism/chemical` | Mekanism | `capacity`, `autoPush` |
| `mm:mekanism/heat` | Mekanism | `capacity`, `heatCapacity`, `inverseConduction`, `autoPush` — see [`mekanism.md`](mekanism.md) |
| `mm:create/kinetic` | Create | `stress` |
| `mm:pneumaticcraft/air` | PneumaticCraft | `volume`, `danger`, `critical` |
| `mm:botania/mana` | Botania | `capacity` |
| `mm:replication/matter` | Replication | `capacity`, `matter(id)` once per reserved tank, `tanks(n)`, `priority`, `network` — see [`replication.md`](replication.md) |
| `mm:replication/link` | Replication | none |
| `mm:nuclear_radiation/radiation` | Nuclear Radiation | `capacity`, `isotope(id)` once per isotope, `decay`, `shielded`, `carrier(item)`, `loadPerItem` — see [`radiation.md`](radiation.md) |
| `mm:ars_nouveau/source` | Ars Nouveau | `capacity`, `range` |
| `mm:projecte/emc` | ProjectE | `capacity`, `kleinSlot`, `kleinRate` |
| `mm:astralsorcery/starlight` | Astral Sorcery | `capacity` |
| `mm:astralsorcery/lumen` | Astral Sorcery | `capacity`, `lumen(...ids)` |
| `mm:etheria/ether` | Etheria | `capacity`, `vesselRate` |
| `mm:ae2/pattern` | AE2 | `patternPriority`, `exclude(...types)` — see [`ae2.md`](ae2.md) |

`autoPush` only exists on the types that list it. Calling it on any other type crashes the game.

Energy and EMC numbers above 9,007,199,254,740,991 lose precision in a script. Use that number or
less for a "creative" hatch, or write the port in JSON.

### Newer port types

```js
MMEvents.registerPorts(event => {
    // Astral Sorcery: starlight from linked lenses
    event.create('starlight_mount')
        .name('Starlight Lens Mount')
        .controllerId('mm:star_forge')
        .only('input')
        .config('mm:astralsorcery/starlight', c => c.capacity(1000))

    // Astral Sorcery: lumen, limited to two kinds
    event.create('lumen_port')
        .name('Lumen Port')
        .controllerId('mm:star_forge')
        .config('mm:astralsorcery/lumen', c => {
            c.capacity(2000)
            c.lumen('astralsorcery:aevitas', 'astralsorcery:vicio')
        })

    // Etheria: a slot for ether vessels, moving at most 50 ether a tick
    event.create('ether_port')
        .name('Ether Port')
        .controllerId('mm:star_forge')
        .config('mm:etheria/ether', c => {
            c.capacity(10000)
            c.vesselRate(50)
        })

    // A fluid port that only takes lava and oils
    event.create('fuel_tank')
        .name('Fuel Tank')
        .controllerId('mm:star_forge')
        .only('input')
        .config('mm:fluid', c => {
            c.rows(1)
            c.columns(2)
            c.slotCapacity(16000)
            c.fluids('minecraft:lava', '#c:oil')
            c.tierRank(2)
        })
})
```

How each one behaves in game is in [`astral-sorcery.md`](astral-sorcery.md). Leave out `vesselRate`
to move ether as fast as the vessel allows.

## Single-block machines

A `mm:single` controller holds its own slots instead of using ports. Each `.slot(...)` takes the same
config methods as a port of that type:

```js
MMEvents.registerControllers(event => {
    event.create('crusher')
        .type('mm:single')
        .name('Crusher')
        .workingOverlay('kubejs:block/crusher_front_on')
        .slot('ore', 'mm:item', true, c => { c.rows(1); c.columns(1) })
        .slot('power', 'mm:energy', true, c => { c.capacity(100000); c.maxReceive(1000); c.maxExtract(0) })
        .slot('dust', 'mm:item', false, c => { c.rows(2); c.columns(2) })
        .slot('waste', 'mm:fluid', false, c => { c.rows(1); c.columns(1); c.slotCapacity(8000) })
})
```

`.slot(id, type, input, config)` — `input` is `true` for an input slot and `false` for an output.
The config function can be left off. Recipes target a single-block machine like any other; see
[`single-block.md`](single-block.md).

## Extra blocks

```js
MMEvents.registerExtraBlocks(event => {
    event.create('kiln_casing').name('Kiln Casing').type('mm:vent').connectGroup('kiln')
    event.create('kiln_core').name({ text: 'Kiln Core', color: '#FF8800' }).type('mm:circuit').connectGroup('kiln')
})
```

| Method | Meaning |
|---|---|
| `.type(id)` | Required. `'mm:circuit'`, `'mm:gearbox'` or `'mm:vent'` |
| `.name(name)` | Display name. Text or a styled object — see [Names](#names) |
| `.connectGroup(group)` | Blocks sharing a group join into one panel. Without one, a block only joins copies of itself |

## Structures

```js
// kubejs/server_scripts/
MMEvents.createStructures(event => {
    event.create('kubejs:alloy_kiln')
        .name('Alloy Kiln')
        .controllerId('mm:alloy_kiln')
        .layout(layout => {
            layout.layer(['BBB', 'BBB', 'BBB'])
            layout.layer(['ICO', 'BEB', 'BBB'])
            layout.key('B', { block: 'minecraft:bricks' })
            layout.key('I', { port: 'mm:kiln_items', input: true })
            layout.key('O', { port: 'mm:kiln_items', input: false })
            layout.key('E', { portType: 'mm:energy', input: true })
        })
})
```

| Method | Meaning |
|---|---|
| `.name(text)` | Name shown in JEI and on the blueprint. Plain text |
| `.controllerId(id)` | Required. Call once per controller that can build it |
| `.layout(layout => {...})` | Required. The shape — see below |
| `.maxParallelRecipes(n)` | Overrides the controller's parallel limit for this structure |
| `.tier(n)` | The structure's tier, for `.minTier()` / `.maxTier()` recipe conditions |
| `.stateList(name, {...})` | A named set of blocks one spot can take — see [State lists](#state-lists) |
| `.boost(structure, speed, radius, max)` | Faster for every nearby machine of another structure — see [Boosts](#boosts) |
| `.geoModel({...})` or `.geoModel('none')` | An animated model for this structure |
| `.fluidArea(key, port)` | Show a port's fluid filling every spot with that layout key |
| `.fluidVolume([x, y, z], [x, y, z], port)` | Show a port's fluid filling a box |

Inside `.layout(...)`:

| Method | Meaning |
|---|---|
| `layout.layer([...rows])` | One layer. Call from the **top** layer down, as in JSON |
| `layout.key(char, {...})` | What a character matches. Takes every key form from [`reference.md`](reference.md#structure-keys) |
| `layout.portsAnywhere(true)` | Any port can sit in any port spot |

`C` is always the controller and a space matches anything. Key forms you can pass:

```js
layout.key('B', { block: 'minecraft:bricks' })
layout.key('L', { tag: 'minecraft:logs' })
layout.key('A', { block: 'minecraft:oak_log', properties: [{ property: 'axis', value: 'y' }] })
layout.key('I', { port: 'mm:kiln_items', input: true })
layout.key('E', { portType: 'mm:energy', input: true, minTier: 2 })
layout.key('P', { portType: 'mm:item', anywhere: true })
layout.key('F', { anyOf: [
    { portType: 'mm:item', input: true, maxTier: 8 },
    { portType: 'mm:fluid', input: true, maxTier: 8 },
    { block: 'minecraft:iron_block' }
] })
```

Without `.tier(n)`, the tier is read from the name: "Tier 2", "Mk II", "Level 3", or `1`.

### State lists

```js
MMEvents.createStructures(event => {
    event.create('kubejs:alloy_kiln_mk2')
        .name('Alloy Kiln')
        .controllerId('mm:alloy_kiln')
        .tier(2)
        .stateList('casing', {
            iron: { block: 'minecraft:iron_block' },
            gold: { block: 'minecraft:gold_block' }
        })
        .stateList('trim', {
            glass: { block: 'minecraft:glass' },
            defaultIgnored: true
        })
        .layout(layout => {
            layout.layer(['XXX', 'XTX', 'XXX'])
            layout.layer(['XCX', 'XXX', 'XXX'])
            layout.key('X', { stateList: 'casing' })
            layout.key('T', { stateList: 'trim' })
        })
})
```

Each `X` can be iron or gold. `defaultIgnored: true` also accepts any block at all, so `T` can be
glass or anything else. Each option takes the same forms as `layout.key(...)`.

### Boosts

```js
MMEvents.createStructures(event => {
    event.create('kubejs:heat_vent')
        .name('Heat Vent')
        .controllerId('mm:heat_vent')
        .layout(layout => {
            layout.layer(['BCB'])
            layout.key('B', { block: 'minecraft:iron_bars' })
        })

    event.create('kubejs:smelter')
        .name('Smelter')
        .controllerId('mm:smelter')
        .boost('kubejs:heat_vent', 0.25, 16, 4)
        .layout(layout => {
            layout.layer(['BBB', 'BBB'])
            layout.layer(['BCB', 'BBB'])
            layout.key('B', { block: 'minecraft:bricks' })
        })
})
```

`.boost(structure, speed, radius, max)`:

| Argument | Default | Meaning |
|---|---|---|
| `structure` | required | Which structure counts |
| `speed` | required | Added speed per machine found. `0.25` is 25% faster for each one |
| `radius` | `16` | How many blocks from the controller to look, in every direction |
| `max` | no limit | The most machines that count |

`radius` and `max` can be left off from the end: `.boost('kubejs:heat_vent', 0.25)`. The smelter
above runs up to twice as fast with four formed heat vents around it. Call `.boost(...)` again for
another structure; boosts add up.

### Fluid inside the machine

```js
MMEvents.createStructures(event => {
    event.create('kubejs:mixing_vat')
        .name('Mixing Vat')
        .controllerId('mm:mixing_vat')
        .layout(layout => {
            layout.layer(['GGG', 'GAG', 'GGG'])
            layout.layer(['GGG', 'GAG', 'GGG'])
            layout.layer(['FCF', 'GGG', 'GGG'])
            layout.key('G', { block: 'minecraft:glass' })
            layout.key('A', { block: 'minecraft:air' })
            layout.key('F', { port: 'mm:vat_fluid' })
        })
        .fluidArea('A', 'F')
})
```

The two `A` spots fill with whatever is in the `F` port, rising as it fills. Leave out the port to
use any fluid port in the machine. For a box instead of a key, use
`.fluidVolume([1, 1, 1], [1, 2, 1], 'F')`: `x` from the left of a row, `y` the layer counted from the
**bottom** starting at `0`, `z` the row from the front. Spots a volume covers should require
`minecraft:air`, or a block placed there hides the fluid.

## Processes

```js
// kubejs/server_scripts/
MMEvents.createProcesses(event => {
    event.create('kubejs:copper_block')
        .structureId('kubejs:alloy_kiln')
        .ticks(100)
        .input({ type: 'mm:input/consume', ingredient: { type: 'mm:item', item: 'minecraft:copper_ingot', count: 9 } })
        .input({ type: 'mm:input/consume', per_tick: true, ingredient: { type: 'mm:energy', amount: 4000 } })
        .output({ type: 'mm:output/simple', ingredient: { type: 'mm:item', item: 'minecraft:copper_block', count: 1 } })
})
```

| Method | Meaning |
|---|---|
| `.structureId(id)` | Required. The structure that runs it |
| `.structureIds(id, id, ...)` | Several structures that can run it. Can replace `.structureId` |
| `.ticks(n)` | Required. Craft time; 20 ticks is one second |
| `.input({...})` | Add an input entry. Same object as in JSON |
| `.output({...})` | Add an output entry. Same object as in JSON |
| `.parallelProcessing(bool)` | Let it run several times at once |
| `.requestOnly(bool)` | Only run when asked: the Replication Terminal, AE2, or a pick in `manual` mode |

`.input(...)` and `.output(...)` take exactly the JSON entry, so everything in
[`reference.md`](reference.md#processes) works: `chance`, `per_tick`, ranges with `rollGroup`,
`nbt`, `components`, weighted outputs and merge enchantments.

### Conditions

| Method | Passes when |
|---|---|
| `.dimension('minecraft:the_nether')` | The machine is in that dimension |
| `.biome('#minecraft:is_ocean')` | The controller is in that biome or biome tag |
| `.weather('rain')` | `'clear'`, `'rain'` or `'thunder'` |
| `.time('night')` | `'day'` or `'night'` |
| `.minY(-64)`, `.maxY(0)` | The controller's Y is in range. Use either or both |
| `.redstone('powered')` | `'powered'` or `'unpowered'` |
| `.minTier(2)`, `.maxTier(3)` | The structure's tier is in range. Use either or both |
| `.nearbyStructure(structure, min, radius)` | At least `min` formed machines of that structure are within `radius` blocks. `radius` defaults to `16` |
| `.constellation('astralsorcery:aevitas')` | That constellation is in the sky (Astral Sorcery) |
| `.eclipse('solar')` | A `'solar'` or `'lunar'` eclipse is happening (Astral Sorcery) |

Every condition must pass. While one fails the recipe does not start, and a running one waits.

```js
MMEvents.createProcesses(event => {
    // Only runs with three formed cooling towers close by
    event.create('kubejs:reactor_burn')
        .structureId('kubejs:reactor')
        .ticks(200)
        .nearbyStructure('kubejs:cooling_tower', 3, 24)
        .input({ type: 'mm:input/consume', ingredient: { type: 'mm:item', item: 'minecraft:coal_block', count: 1 } })
        .output({ type: 'mm:output/simple', ingredient: { type: 'mm:energy', amount: 200000 } })

    // Underground, at night, in the Overworld
    event.create('kubejs:deep_night')
        .structureId('kubejs:alloy_kiln')
        .ticks(100)
        .dimension('minecraft:overworld')
        .time('night')
        .maxY(0)
        .input({ type: 'mm:input/consume', ingredient: { type: 'mm:item', item: 'minecraft:amethyst_shard', count: 4 } })
        .output({ type: 'mm:output/simple', ingredient: { type: 'mm:item', item: 'minecraft:echo_shard', count: 1 } })
})
```

Unlike a boost, which only makes a machine faster, `.nearbyStructure(...)` stops the recipe
entirely until enough machines are built.

### Merging enchantments

```js
MMEvents.createProcesses(event => {
    // Any gear piece + any enchanted book, like an anvil
    event.create('kubejs:fuse_book')
        .structureId('kubejs:enchant_fuser')
        .ticks(200)
        .input({ type: 'mm:input/consume', ingredient: { type: 'mm:item', tag: 'minecraft:enchantable/armor', count: 1 } })
        .input({ type: 'mm:input/consume', ingredient: { type: 'mm:item', item: 'minecraft:enchanted_book', count: 1 } })
        .output({ type: 'mm:output/merge_enchantments' })

    // Two swords into one, levels capped at 10, anything goes
    event.create('kubejs:overcharge_sword')
        .structureId('kubejs:enchant_fuser')
        .ticks(400)
        .input({ type: 'mm:input/consume', ingredient: { type: 'mm:item', item: 'minecraft:netherite_sword', count: 2 } })
        .input({ type: 'mm:input/consume', per_tick: true, ingredient: { type: 'mm:energy', amount: 1000000 } })
        .output({
            type: 'mm:output/merge_enchantments',
            item: 'minecraft:netherite_sword',
            maxLevel: 10,
            allowConflicting: true,
            allowUnsupported: true,
            leftoverBook: false
        })

    // Up to level 10, except Fortune and Looting, which stop at 3
    event.create('kubejs:overcharge_pickaxe')
        .structureId('kubejs:enchant_fuser')
        .ticks(400)
        .input({ type: 'mm:input/consume', ingredient: { type: 'mm:item', item: 'minecraft:netherite_pickaxe', count: 2 } })
        .output({
            type: 'mm:output/merge_enchantments',
            maxLevel: 10,
            maxLevels: { 'minecraft:fortune': 3, 'minecraft:looting': 3 }
        })
})
```

| Key | Default | Meaning |
|---|---|---|
| `item` | the gear piece that went in | Which input comes back out with the enchantments. An item id or `'#tag'` |
| `maxLevel` | each enchantment's normal max | No enchantment goes above this |
| `maxLevels` | none | A cap for single enchantments, e.g. `{ 'minecraft:fortune': 3 }`. Wins over `maxLevel` for the ones it lists |
| `allowConflicting` | `false` | Let clashing enchantments go together, like all four protections |
| `allowUnsupported` | `false` | Let the item take enchantments it normally can't |
| `leftoverBook` | `true` | Enchantments that were left off come out on a separate book. `false` drops them |

With only books going in and no `item`, the result is an enchanted book. Two of one enchantment at
the same level become one level higher. The full rules are in
[`reference.md`](reference.md#merging-enchantments).

### Item components

```js
// Takes an epic sword with no custom name, gives it back glowing
.input({ type: 'mm:input/consume', ingredient: {
    type: 'mm:item', item: 'minecraft:diamond_sword', count: 1,
    components: { 'minecraft:rarity': 'epic', '!minecraft:custom_name': {} }
} })
.output({ type: 'mm:output/simple', ingredient: {
    type: 'mm:item', item: 'minecraft:diamond_sword', count: 1,
    components: { 'minecraft:rarity': 'epic', 'minecraft:enchantment_glint_override': true }
} })
```

Components use the same ids and values as `/give`. A `!` in front means the item must not have it,
and on an output removes it.

### Weighted, chance and random outputs

```js
.output({ type: 'mm:output/simple', chance: 0.1, ingredient: { type: 'mm:item', item: 'minecraft:diamond', count: 1 } })
.output({ type: 'mm:output/simple', ingredient: { type: 'mm:item', item: 'minecraft:raw_iron', count: { min: 1, max: 3 } } })
.output({
    type: 'mm:output/weighted',
    options: [
        { weight: 6, ingredient: { type: 'mm:item', item: 'minecraft:iron_nugget', count: 3 } },
        { weight: 3, ingredient: { type: 'mm:item', item: 'minecraft:gold_nugget', count: 2 } },
        { weight: 1 }
    ]
})
```

A `chance: 0` input is a catalyst: it must be there but is never used up.

### Ingredients for every port type

| `type` | Keys |
|---|---|
| `mm:item` | `item` or `tag`, `count`, optional `nbt`, `nbt_match`, `components` |
| `mm:fluid` | `fluid`, `amount` (mB) |
| `mm:energy` | `amount` (FE). With `per_tick`, the total over the whole recipe |
| `mm:entity` | `entity` or `tag`, `amount` |
| `mm:mekanism/chemical` | `chemical`, `amount` |
| `mm:mekanism/heat` | see [`mekanism.md`](mekanism.md) |
| `mm:create/kinetic` | `speed` |
| `mm:pneumaticcraft/air` | `air`, optional `pressure` |
| `mm:botania/mana` | `mana` |
| `mm:replication/matter` | `matter`, `amount` |
| `mm:nuclear_radiation/radiation` | optional `isotope`, `amount` (Bq) |
| `mm:ars_nouveau/source` | `source` |
| `mm:projecte/emc` | `emc` |
| `mm:astralsorcery/starlight` | `starlight`, optional `constellation` |
| `mm:astralsorcery/lumen` | `lumen`, `amount` |
| `mm:etheria/ether` | `ether` |

Any `count` or `amount` can be `{ min, max }`, with an optional `rollGroup` to roll several together.

### A full Astral Sorcery and Etheria recipe

```js
MMEvents.createProcesses(event => {
    event.create('kubejs:aevitas_infusion')
        .structureId('kubejs:star_forge')
        .ticks(100)
        .constellation('astralsorcery:aevitas')
        .input({ type: 'mm:input/consume', ingredient: { type: 'mm:astralsorcery/starlight', constellation: 'astralsorcery:aevitas', starlight: 200 } })
        .input({ type: 'mm:input/consume', ingredient: { type: 'mm:etheria/ether', ether: 300 } })
        .output({ type: 'mm:output/simple', ingredient: { type: 'mm:astralsorcery/lumen', lumen: 'astralsorcery:aevitas', amount: 250 } })

    event.create('kubejs:eclipse_ether')
        .structureId('kubejs:star_forge')
        .ticks(200)
        .eclipse('lunar')
        .input({ type: 'mm:input/consume', ingredient: { type: 'mm:item', item: 'minecraft:ender_pearl', count: 1 } })
        .output({ type: 'mm:output/simple', ingredient: { type: 'mm:etheria/ether', ether: 1000 } })
})
```

## Recipe events

```js
// kubejs/server_scripts/
MMEvents.recipeStarted('kubejs:copper_block', event => {
    if (event.level.isRaining()) event.cancel()
})

MMEvents.recipeFinished('kubejs:copper_block', event => {
    event.level.playSound(null, event.pos, 'minecraft:block.anvil.use', 'blocks', 1, 1)
})

MMEvents.recipeFinished(event => {
    console.info(`${event.controllerId} finished ${event.recipeId} at ${event.pos}`)
})
```

With a recipe id, the event only runs for that recipe. Without one, it runs for every recipe.

| Field | Meaning |
|---|---|
| `event.recipeId` | The recipe's id |
| `event.controllerId` | The controller block, e.g. `mm:alloy_kiln` |
| `event.structureId` | The structure the machine is built as |
| `event.level` | The world |
| `event.pos` | The controller's position |
| `event.block` | The controller block, as a KubeJS block |
| `event.controller` | The controller's block entity |

`event.cancel()` in `recipeStarted` stops the recipe from starting; the machine tries again later.
`recipeFinished` can not be cancelled.

## Structure Builder

```js
// kubejs/server_scripts/
MMEvents.builderStructures(event => {
    event.add('mypack:hut', 'mypack:mm_builder_structures/hut.nbt')
    event.remove('mypack:old_hut')
    event.removeNamespace('someothermod')
})
```

| Method | Meaning |
|---|---|
| `event.add(id, file)` | Add a `.nbt` structure file from a datapack under the given id |
| `event.remove(id)` | Hide one builder structure |
| `event.removeNamespace(namespace)` | Hide every builder structure from that namespace |

## Names

`.name(...)` on controllers, ports and extra blocks, and `.inputName(...)` / `.outputName(...)` on
ports, take:

```js
.name('Alloy Kiln')
.name({ translation: 'block.mypack.alloy_kiln' })
.name({ text: 'Alloy Kiln', color: '#FF8800', bold: true })
.name({ text: 'Alloy Kiln', gradient: ['#FFC72C', '#DA291C'] })
.name({ text: 'Alloy Kiln', rainbow: true, speed: 0.5, spread: 0.05 })
.name({ text: 'Alloy Kiln', rainbow: true, colors: ['#00FFFF', '#0000FF'], spread: 0 })
```

The coloured form also takes `italic`, `underlined`, `strikethrough` and `obfuscated`. Structure
names are plain text only.

## Saving a build as a script

Build the machine in a world, right-click two opposite corners with the **Multiblock Saver**, then
right-click the air. A ready-to-edit KubeJS script is written to `config/mm/structures/` next to the
JSON.

## See also

- [`example.md`](example.md) — one machine built start to finish.
- [`reference.md`](reference.md) — every key and option.
- [`models.md`](models.md) — looks, GeckoLib models and fluid volumes.
- [`single-block.md`](single-block.md) — machines in one block.
