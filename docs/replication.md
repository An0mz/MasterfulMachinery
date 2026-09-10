# Replication matter and Masterful Machinery

How to use Replication matter in MM machines, including custom matter types.

## The port

| Port type | What it holds |
|---|---|
| `mm:replication/matter` | Any Replication matter type, built-in or custom |

An **input** port asks the matter network for what it is short of, the way a Replicator does, so it
fills from anywhere on the network rather than waiting for a pipe to push at it. An **output** port
empties itself into your matter tanks the way a Disintegrator does. Matter pipes connect to both.

## Port config

```json
{
  "id": "matter_hatch",
  "controllerIds": "mm:my_machine",
  "name": "Matter Hatch",
  "type": "mm:replication/matter",
  "config": {
    "capacity": 4000,
    "matter": ["replication:metallic", "replication:earth"],
    "priority": 5,
    "network": true
  }
}
```

| Option | Default | Meaning |
|---|---|---|
| `capacity` | `1000` | How much each tank holds |
| `matter` | none | Type each tank is reserved for. One string, or a list for several tanks |
| `tanks` | as many as `matter` | Unreserved tanks, which lock to whatever arrives first |
| `priority` | `0` | MM's own output ordering, clamped to 0–10 |
| `network` | `true` | Set false to keep the port off the matter network entirely |

A port with several tanks shows a divider between them, tints an empty tank with the colour of the
matter it is reserved for, and highlights the one under your cursor.

From KubeJS: `.capacity(...)`, `.matter(...)`, `.tanks(...)`, `.priority(...)`, `.network(...)`.

## Recipe ingredient

```json
{ "type": "mm:replication/matter", "matter": "replication:metallic", "amount": 64 }
```

Ranges and `rollGroup` work on `amount` like everywhere else.

## Built-in matter types

`replication:earth`, `ender`, `living`, `metallic`, `nether`, `organic`, `precious`, `quantum`.

(There is also `replication:empty`, which is the empty marker and not usable in a recipe.)

## Custom matter types

Register into Replication's registry in a **startup script**:

```js
StartupEvents.registry('replication:matter_types', event => {
    event.create('plasma').color(0.2, 0.7, 1.0, 1.0).max(10000)
})
```

- `color(r, g, b, a)` takes floats from 0 to 1, not 0–255. This is the colour the port screen and
  JEI use for the bar.
- `max(n)` is the maximum the network stores of this type.
- KubeJS namespaces the id, so `create('plasma')` registers **`kubejs:plasma`** — that full id is
  what recipes and port configs must use, not the bare name.

Then use it exactly like a built-in:

```json
{ "type": "mm:replication/matter", "matter": "kubejs:plasma", "amount": 200 }
```

```json
"config": { "capacity": 8000, "matter": ["kubejs:plasma"] }
```

### Give it a name

Without a lang entry the game shows the raw translation key, `replication.matter_type.plasma`.
Add it in any lang file — `kubejs/assets/kubejs/lang/en_us.json` works:

```json
{ "replication.matter_type.plasma": "Plasma" }
```

The key uses the **path** of the id, not the namespace, so `kubejs:plasma` becomes
`replication.matter_type.plasma`.

## Notes

- A custom type is a first-class matter type. It travels the network, stores in matter tanks and
  works in any MM matter port with no extra setup.
- Reserving a tank with `matter` is what stops an output port hoarding the wrong type when a
  machine produces more than one.
- `network: false` is for a buffer you want to fill and drain only through recipes, with the
  network unable to see it.

## From KubeJS

```js
// kubejs/startup_scripts/
MMEvents.registerPorts(event => {
    event.create('matter_hatch')
        .name('Matter Hatch')
        .controllerId('mm:my_machine')
        .config('mm:replication/matter', config => {
            config.capacity(4000)
            config.matter('replication:metallic')
            config.matter('kubejs:plasma')
            config.priority(5)
            config.network(true)
        })
})
```

Call `.matter(...)` once per reserved tank, or `.tanks(n)` for unreserved ones.

```js
// kubejs/server_scripts/
MMEvents.createProcesses(event => {
    event.create('kubejs:plasma_to_emerald')
        .structureId('kubejs:my_structure')
        .ticks(40)
        .input({
            type: 'mm:input/consume',
            ingredient: { type: 'mm:replication/matter', matter: 'kubejs:plasma', amount: 200 }
        })
        .output({
            type: 'mm:output/simple',
            ingredient: { type: 'mm:item', item: 'minecraft:emerald', count: 1 }
        })
})
```

### Where the script goes

Registering a port is a **startup** event, so it belongs in `kubejs/startup_scripts/`. Processes are
a **server** event and belong in `kubejs/server_scripts/`. Put either in the wrong folder and it
silently never runs.

Controllers and ports register under `mm:`: `event.create('my_port')` makes `mm:my_port_input` and
`mm:my_port_output`, and `.controllerId(...)` takes `mm:<controller id>`. Structures and processes
need the namespace written out: `event.create('kubejs:my_structure')`.

If you call a method that does not exist, KubeJS does not log a warning — it kills the game on
startup with `TypeError: Cannot find function ... in object <SomeBuilder>`. The builder named in
that message tells you which port type you were configuring. Names are case sensitive.

## See also

- [`mekanism.md`](mekanism.md) — Mekanism chemicals and heat.
- [`entity.md`](entity.md) — entity ports, for machines that work with mobs.
