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

## See also

- [`mekanism.md`](mekanism.md) — Mekanism chemicals and heat.
