# Masterful Machinery reference

Every file, key and option in one place. For a machine built step by step, see
[`example.md`](example.md).

## Where files go

| What | Where | Reload |
|---|---|---|
| Controllers | `config/mm/controllers/*.json` | Restart |
| Ports | `config/mm/ports/*.json` | Restart |
| Extra blocks | `config/mm/extras/*.json` | Restart |
| Structures | `data/<namespace>/mm/structures/*.json` in a datapack | `/reload` |
| Processes (recipes) | `data/<namespace>/mm/processes/*.json` in a datapack | `/reload` |

Subfolders are fine. With KubeJS installed, `kubejs/data/` works as a datapack, so structures can go
in `kubejs/data/mypack/mm/structures/`.

A structure at `data/mypack/mm/structures/alloy_kiln.json` has the id `mypack:alloy_kiln`.
Processes work the same way.

Controllers, ports and extra blocks register as `mm:<id>`. A port makes two blocks, `mm:<id>_input`
and `mm:<id>_output`, unless `only` limits it to one side.

The same things can be made from KubeJS:

| Event | Script folder |
|---|---|
| `MMEvents.registerControllers` | `kubejs/startup_scripts/` |
| `MMEvents.registerPorts` | `kubejs/startup_scripts/` |
| `MMEvents.registerExtraBlocks` | `kubejs/startup_scripts/` |
| `MMEvents.createStructures` | `kubejs/server_scripts/` |
| `MMEvents.createProcesses` | `kubejs/server_scripts/` |

An event in the wrong folder silently never runs.

## Controllers

```json
{
  "id": "alloy_kiln",
  "type": "mm:machine",
  "name": "Alloy Kiln"
}
```

| Key | Required | Meaning |
|---|---|---|
| `id` | yes | Block id, registered as `mm:<id>` |
| `type` | yes | Always `mm:machine` |
| `name` | yes | Display name — see [Names and colours](#names-and-colours) |
| `texture` | no | Base texture, e.g. `kubejs:block/kiln_base` |
| `overlay` | no | Texture drawn on top of the base |
| `model` | no | A full block model to use instead |
| `parallelProcessingDefault` | no | Whether recipes on this controller run in parallel when the recipe does not say |
| `maxParallelRecipes` | no | How many recipes can run at once, `0`–`100`. Falls back to the global config |
| `recipeSelectionMode` | no | `default`, `avoid_same_recipe` or `round_robin_input_item` |

Recipe selection modes:

- **`default`** — the first recipe that can run, runs.
- **`avoid_same_recipe`** — skips the recipe that just ran when another one can run, so a machine
  with several recipes alternates between them.
- **`round_robin_input_item`** — takes turns between the different input items, so one item cannot
  hog the machine.

KubeJS:

```js
MMEvents.registerControllers(event => {
    event.create('alloy_kiln')
        .type('mm:machine')
        .name('Alloy Kiln')
        .texture('kubejs:block/kiln_base')
        .overlay('kubejs:block/kiln_front')
        .parallelProcessingDefault(true)
        .maxParallelRecipes(4)
        .recipeSelectionMode('avoid_same_recipe')
})
```

## Ports

```json
{
  "id": "kiln_items",
  "controllerIds": "mm:alloy_kiln",
  "name": "Kiln Hatch",
  "type": "mm:item",
  "config": { "rows": 2, "columns": 2 }
}
```

| Key | Required | Meaning |
|---|---|---|
| `id` | yes | Base id. The blocks become `mm:<id>_input` and `mm:<id>_output` |
| `controllerIds` | yes | One controller id, or a list of them |
| `type` | yes | The port type — see below |
| `config` | yes | Settings for that port type |
| `name` | yes | Base name. " Input" / " Output" is added to it |
| `inputName` / `outputName` | no | The full name for one side, replacing `name` for it |
| `only` | no | `input`, `output` or `both` (default) |
| `texture`, `overlay`, `model` | no | As on controllers |
| `inputTexture`, `outputTexture`, `inputOverlay`, `outputOverlay`, `inputModel`, `outputModel` | no | The same, for one side only |

### Port types

| Type | Needs | `config` | Recipe ingredient |
|---|---|---|---|
| `mm:item` | — | `rows`, `columns`, `slotCapacity`, `autoPush` | `item` or `tag`, `count` |
| `mm:fluid` | — | `rows`, `columns`, `slotCapacity`, `autoPush` | `fluid`, `amount` (mB) |
| `mm:energy` | — | `capacity`, `maxReceive`, `maxExtract`, `autoPush` | `amount` (FE) |
| `mm:entity` | — | see [`entity.md`](entity.md) | `entity` or `tag`, `amount` |
| `mm:mekanism/chemical` | Mekanism | see [`mekanism.md`](mekanism.md) | `chemical`, `amount` |
| `mm:mekanism/heat` | Mekanism | see [`mekanism.md`](mekanism.md) | see [`mekanism.md`](mekanism.md) |
| `mm:create/kinetic` | Create | `stress` | `speed` |
| `mm:pneumaticcraft/air` | PneumaticCraft | `volume`, `danger`, `critical` | `air`, `pressure` (optional) |
| `mm:botania/mana` | Botania | `capacity` | `mana` |
| `mm:replication/matter` | Replication | see [`replication.md`](replication.md) | `matter`, `amount` |
| `mm:nuclear_radiation/radiation` | Nuclear Radiation | see [`radiation.md`](radiation.md) | `isotope` (optional), `amount` (Bq) |

Notes on the config keys:

- **`slotCapacity`** — on item ports it is how many items fit in one slot; leave it out for the
  normal stack size. On fluid ports it is required and is the mB each tank holds.
- **`autoPush`** — the port pushes its contents into neighbouring blocks on its own. Off by default;
  the default can be changed in the config.
- **`tierRank`** — any port can have a `tierRank` number in its `config`. Structures can then ask for
  "any energy port of tier 2 or higher" — see [Structure keys](#structure-keys).

KubeJS:

```js
MMEvents.registerPorts(event => {
    event.create('kiln_items')
        .name('Kiln Hatch')
        .controllerId('mm:alloy_kiln')
        .config('mm:item', config => {
            config.rows(2)
            config.columns(2)
            config.slotCapacity(128)
            config.autoPush(true)
        })

    event.create('kiln_energy')
        .name('Kiln Power')
        .controllerId('mm:alloy_kiln')
        .only('input')
        .config('mm:energy', config => {
            config.capacity(100000)
            config.maxReceive(1000)
            config.maxExtract(0)
            config.tierRank(2)
        })
})
```

Config methods per type:

| Type | Methods |
|---|---|
| `mm:item` | `rows`, `columns`, `slotCapacity`, `autoPush`, `tierRank` |
| `mm:fluid` | `rows`, `columns`, `slotCapacity`, `autoPush`, `tierRank` |
| `mm:energy` | `capacity`, `maxReceive`, `maxExtract`, `autoPush`, `tierRank` |
| `mm:create/kinetic` | `stress` |
| `mm:pneumaticcraft/air` | `volume`, `danger`, `critical` |
| `mm:botania/mana` | `capacity` |

The entity, Mekanism and Replication methods are in their own docs. Call `.controllerId(...)` once
per controller to attach a port to several machines.

## Structures

```json
{
  "name": "Alloy Kiln",
  "controllerIds": "mm:alloy_kiln",
  "layout": [
    ["BBB", "BBB", "BBB"],
    ["ICO", "BEB", "BBB"]
  ],
  "key": {
    "B": { "block": "minecraft:bricks" },
    "I": { "port": "mm:kiln_items", "input": true },
    "O": { "port": "mm:kiln_items", "input": false },
    "E": { "portType": "mm:energy", "input": true }
  }
}
```

### Layout

- `layout` is a list of **layers**, from the **top** layer down to the bottom.
- Each layer is a list of **rows**, running front to back.
- Each character in a row is one block, left to right.
- **`C`** is the controller. It must appear exactly once.
- A **space** means "anything goes" — the position is not checked.
- Every other character must be in `key`.

The machine forms in any of the four horizontal rotations, so it does not matter which way the
player builds it.

### Structure keys

| Key form | Matches |
|---|---|
| `{ "block": "minecraft:bricks" }` | That block |
| `{ "tag": "minecraft:logs" }` | Any block in the tag |
| `{ "port": "mm:kiln_items" }` | That port, either side |
| `{ "port": "mm:kiln_items", "input": true }` | That port, input side only |
| `{ "portType": "mm:energy" }` | Any port of that type |
| `{ "portType": "mm:energy", "minTier": 2, "maxTier": 3 }` | Only ports whose `tierRank` is in range |
| `{ "stateList": "casing" }` | Any option from a named state list |

`port` and `portType` also accept `"anywhere": true`. The port then does not have to sit in that
exact position — any of the port positions in the structure will do, as long as every port still
gets a place. Set `"portsAnywhere": true` at the top of the structure to do this for every port.

Add `properties` to a key to also check block states:

```json
"L": {
  "block": "minecraft:oak_log",
  "properties": [{ "property": "axis", "value": "y" }]
}
```

### State lists

A state list is a named set of options that one position can take:

```json
{
  "stateLists": {
    "casing": {
      "iron": { "block": "minecraft:iron_block" },
      "gold": { "block": "minecraft:gold_block" }
    }
  },
  "key": {
    "X": { "stateList": "casing" }
  }
}
```

Add `"defaultIgnored": true` to a list to also accept any block at all. State lists are JSON only.

### Other structure keys

| Key | Meaning |
|---|---|
| `name` | Name shown in JEI and on the blueprint |
| `controllerIds` | One controller id or a list. Several controllers can share one structure |
| `maxParallelRecipes` | Overrides the controller's limit for this structure |
| `portsAnywhere` | As above |

KubeJS:

```js
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

Layers go top to bottom, the same as in JSON. Give the structure id a namespace — `kubejs:alloy_kiln`,
not `alloy_kiln`. `.portsAnywhere(true)` goes on the layout, `.maxParallelRecipes(n)` on the
structure.

## Processes

```json
{
  "structureId": "mypack:alloy_kiln",
  "ticks": 100,
  "inputs": [
    { "type": "mm:input/consume", "ingredient": { "type": "mm:item", "item": "minecraft:copper_ingot", "count": 3 } },
    { "type": "mm:input/consume", "ingredient": { "type": "mm:item", "tag": "c:ingots/tin", "count": 1 } },
    { "type": "mm:input/consume", "per_tick": true, "ingredient": { "type": "mm:energy", "amount": 40 } }
  ],
  "outputs": [
    { "type": "mm:output/simple", "ingredient": { "type": "mm:item", "item": "minecraft:copper_block", "count": 1 } }
  ]
}
```

| Key | Required | Meaning |
|---|---|---|
| `structureId` | yes | Which structure runs this recipe |
| `ticks` | yes | How long one craft takes. 20 ticks is one second |
| `inputs` | yes | List of input entries |
| `outputs` | yes | List of output entries |
| `conditions` | no | Extra requirements — see below |
| `parallelProcessing` | no | Let this recipe run several times at once |

### Entries

Inputs use `mm:input/consume` and outputs use `mm:output/simple`. Both take:

| Key | Meaning |
|---|---|
| `ingredient` | What is taken or made. The `type` is the port type |
| `chance` | `0`–`1`. `0.25` means a 25% chance each craft. Default `1` |
| `per_tick` | Take or make the amount every tick instead of once. Useful for energy |

An item ingredient can also be written as just a string: `"ingredient": "minecraft:diamond"`.

### Random amounts

Any `count` or `amount` can be a range:

```json
"count": { "min": 2, "max": 5 }
```

Each craft rolls a value between the two, inclusive. Ranges that share a **`rollGroup`** roll
together — the input and output below always land on the same point of their ranges, so 1 gold in
always gives 4 nuggets and 5 gold always gives 8:

```json
"inputs":  [{ "type": "mm:input/consume", "ingredient": { "type": "mm:item", "item": "minecraft:gold_ingot", "count": { "min": 1, "max": 5, "rollGroup": "yield" } } }],
"outputs": [{ "type": "mm:output/simple", "ingredient": { "type": "mm:item", "item": "minecraft:gold_nugget", "count": { "min": 4, "max": 8, "rollGroup": "yield" } } }]
```

### Conditions

```json
"conditions": [
  { "type": "mm:dimension", "dimension": "minecraft:the_nether" },
  { "type": "mm:weather", "weather": "rain" }
]
```

`weather` is `clear`, `rain` or `thunder`. Every condition has to pass. Conditions are JSON only.

### Item NBT

Item ingredients can require data on the item:

```json
{ "type": "mm:item", "item": "minecraft:potion", "count": 1, "nbt": "{custom:1b}", "nbt_match": "strong" }
```

`nbt` is SNBT text or a JSON object. By default the item only needs to contain those values;
`"nbt_match": "strong"` makes it match exactly.

KubeJS:

```js
MMEvents.createProcesses(event => {
    event.create('kubejs:bronze')
        .structureId('kubejs:alloy_kiln')
        .ticks(100)
        .parallelProcessing(true)
        .input({ type: 'mm:input/consume', ingredient: { type: 'mm:item', item: 'minecraft:copper_ingot', count: 3 } })
        .input({ type: 'mm:input/consume', per_tick: true, ingredient: { type: 'mm:energy', amount: 40 } })
        .output({ type: 'mm:output/simple', chance: 0.5, ingredient: { type: 'mm:item', item: 'minecraft:copper_block', count: 1 } })
})
```

## Names and colours

`name` on controllers and ports, and `inputName` / `outputName` on ports, take any of these:

| Form | Example |
|---|---|
| Plain text | `"Alloy Kiln"` |
| Translation key | `{ "translation": "block.mypack.alloy_kiln" }` |
| Coloured | `{ "text": "Alloy Kiln", "color": "#FF8800", "bold": true }` |
| Gradient | `{ "text": "Alloy Kiln", "gradient": ["#FFC72C", "#DA291C"] }` |
| Rainbow | `{ "text": "Alloy Kiln", "rainbow": true, "speed": 0.5, "spread": 0.05 }` |

- A gradient takes any number of colours.
- A rainbow's `speed` is how fast it cycles, and `spread` is how much the colour changes from one
  letter to the next. `spread: 0` makes the whole name one colour that cycles together.
- Add `colors` to a rainbow to cycle through only those colours instead of the whole spectrum:
  `{ "text": "Alloy Kiln", "rainbow": true, "colors": ["#00FFFF", "#0000FF"], "spread": 0 }` fades
  the whole name between cyan and blue.
- The coloured form also takes `italic`, `underlined`, `strikethrough` and `obfuscated`.

From KubeJS, `.name()`, `.inputName()` and `.outputName()` take the same forms:

```js
.name({ text: 'Alloy Kiln', rainbow: true })
```

A plain text name can be translated or overridden by a resource pack through the `block.mm.<id>`
lang key.

## Parallel processing

A formed machine can run several crafts of the same recipe at once. A recipe runs in parallel when:

1. the recipe says `"parallelProcessing": true`, or the controller has
   `"parallelProcessingDefault": true` and the recipe does not say otherwise, and
2. the ports have enough inputs and output space for more than one craft.

The limit is the structure's `maxParallelRecipes`, then the controller's, then `maxParallelRecipes`
in the config (default `5`).

## Extra blocks

Decorative blocks for building structures out of:

```json
{ "id": "kiln_casing", "name": "Kiln Casing", "type": "mm:vent" }
```

`type` is `mm:circuit`, `mm:gearbox` or `mm:vent`. The block registers as `mm:<id>`.

```js
MMEvents.registerExtraBlocks(event => {
    event.create('kiln_casing').name('Kiln Casing').type('mm:vent')
})
```

## Items and commands

| Item | Use |
|---|---|
| **Blueprint** | Every structure has one in the **MM Structures** creative tab. In creative, sneak to preview the structure in the world and sneak + right-click to place it |
| **Multiblock Saver** | Right-click two blocks to mark opposite corners, then right-click the air to save what is between them as a structure. The files go to `config/mm/structures/`, as JSON and as a KubeJS script. Sneak + right-click the air to clear the corners |
| **Priority Setter** | Right-click to raise the number, up to 10. Sneak + right-click a port to apply it. Ports with higher priority are used first |
| **Debug Tool** | Right-click a controller to write a report of why it is or is not forming and running |

`/mm reform` (operators only) rechecks every machine near online players. Use it after changing
structures with `/reload`.

JEI shows every structure and its recipes. In the structure preview, left-drag rotates, the scroll
wheel zooms and shift-drag pans. With Jade, looking at a port shows what is inside it.

## Config

`config/mm-common.toml`:

| Option | Default | Meaning |
|---|---|---|
| `portsAutoExtractByDefault` | `false` | The `autoPush` value for ports that do not set it |
| `parallelProcessingDefault` | `false` | Whether recipes run in parallel when nothing else says |
| `maxParallelRecipes` | `5` | The global parallel limit |
| `structureValidationRate` | `10` | How often a controller rechecks its structure, in ticks |
| `asyncValidation` | `true` | Check structures off the main thread. Turn off if machines misbehave |
| `splitRecipesJei` | `true` | One JEI category per structure |
| `showJeiMaxParallel` | `true` | Show the parallel limit in JEI |
| `debugTool` | `true` | Whether the Debug Tool works |

## Troubleshooting

- **The game crashes with `Unknown port type`.** The type is misspelled, or the mod that adds it is
  not installed.
- **The game crashes naming a file under `config/mm/`.** That file has a mistake; the message says
  which key.
- **KubeJS crashes with `TypeError: Cannot find function`.** A method name is wrong. They are case
  sensitive.
- **A block shows a raw name like `block.mm.my_port_input`.** Delete `config/mm/pack/` and
  restart. It is generated again on launch.
- **The machine will not form.** Use the Debug Tool on the controller. Check that each port's
  `controllerIds` includes this controller, and that the layout has exactly one `C`.
- **The machine forms but the recipe never starts.** Check the recipe's `structureId`, that every
  input is in an input port, and that the output ports have room.
