# How machines look

Ways to change what a machine looks like in the world: a controller that changes when it works,
animated GeckoLib models, and fluid that fills up inside the machine.

## Controller looks per state

A controller is in one of three states: `unformed`, `idle` (formed, nothing running) or `working`.
Give any state its own model or overlay:

```json
{
  "id": "alloy_kiln",
  "type": "mm:machine",
  "name": "Alloy Kiln",
  "overlay": "kubejs:block/kiln_front",
  "workingOverlay": "kubejs:block/kiln_front_lit"
}
```

| Key | Meaning |
|---|---|
| `unformedModel`, `idleModel`, `workingModel` | A full block model for that state |
| `unformedOverlay`, `idleOverlay`, `workingOverlay` | The overlay for that state |

A state without its own key uses `model` or `overlay`. Single-block machines (`mm:single`) take the
same keys.

KubeJS:

```js
MMEvents.registerControllers(event => {
    event.create('alloy_kiln')
        .type('mm:machine')
        .name('Alloy Kiln')
        .overlay('kubejs:block/kiln_front')
        .workingOverlay('kubejs:block/kiln_front_lit')
})
```

`.idleModel(...)`, `.workingModel(...)`, `.idleOverlay(...)` and `.workingOverlay(...)` work the
same way.

## Animated GeckoLib models

With [GeckoLib](https://www.curseforge.com/minecraft/mc-mods/geckolib) installed, a formed machine
can show an animated model drawn from its controller. The blocks of the machine stay where they are;
the model is drawn on top of them. It only shows while the machine is formed.

Put `geoModel` on a controller, or on a structure:

```json
"geoModel": {
  "model": "mypack:geo/star_forge.geo.json",
  "texture": "mypack:textures/block/star_forge.png",
  "animations": "mypack:animations/star_forge.animation.json",
  "idle": "animation.star_forge.idle",
  "working": "animation.star_forge.spin",
  "offset": [0, 1, -1],
  "scale": 1.0,
  "rotate": true,
  "glow": false
}
```

| Key | Default | Meaning |
|---|---|---|
| `model` | required | The `.geo.json` file, as a full path in a resource pack |
| `texture` | required | The texture, as a full path |
| `animations` | none | The `.animation.json` file |
| `idle` | none | Animation that loops while the machine is formed and idle |
| `working` | none | Animation that loops while the machine works |
| `offset` | `[0, 0, 0]` | Moves the model, in blocks: right, up, and towards the controller's front |
| `scale` | `1` | Size of the model |
| `rotate` | `true` | Turn the model with the controller. With `false`, `offset` is in world directions |
| `glow` | `false` | Draw the model at full brightness |

A structure's `geoModel` wins over the controller's, so one controller can look different for each
structure it builds. `"geoModel": "none"` on a structure turns the controller's model off for it.

The files go in a resource pack, for example `kubejs/assets/mypack/geo/star_forge.geo.json`.

KubeJS takes the same object:

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
            working: 'animation.star_forge.spin'
        })
})
```

`.geoModel({...})` also works on a structure in `MMEvents.createStructures`.

## Fluid volumes

A fluid volume shows the fluid in one of the machine's ports inside the machine, rising as the port
fills. Add a `visuals` list to a structure:

```json
{
  "name": "Mixing Vat",
  "controllerIds": "mm:mixing_vat",
  "layout": [
    ["GGG", "GAG", "GGG"],
    ["GGG", "GAG", "GGG"],
    ["FCF", "GGG", "GGG"]
  ],
  "key": {
    "G": { "block": "minecraft:glass" },
    "A": { "block": "minecraft:air" },
    "F": { "port": "mm:vat_fluid" }
  },
  "visuals": [
    { "type": "fluid_volume", "area": "A", "port": "F" }
  ]
}
```

| Key | Meaning |
|---|---|
| `area` | A layout key. Every spot with that key is filled |
| `from`, `to` | Instead of `area`, two corners of a box: `[x, y, z]` |
| `port` | Optional. The layout key of the fluid port to show. Without it, any fluid port in the machine |

For `from` and `to`, `x` is the position in a row from the left, `y` is the layer counted from the
**bottom** starting at `0`, and `z` is the row from the front. Both corners are included.

The spots a volume covers should be a key that requires `minecraft:air`. A space in the layout lets
any block sit there and hide the fluid; the log warns when a volume covers spaces.

KubeJS:

```js
MMEvents.createStructures(event => {
    event.create('kubejs:mixing_vat')
        // ...layout as usual...
        .fluidArea('A', 'F')
        .fluidVolume([1, 1, 1], [1, 2, 1], 'F')
})
```

`.fluidArea(key)` and `.fluidVolume(from, to)` work without a port too.

## See also

- [`reference.md`](reference.md) — every key and option.
- [`single-block.md`](single-block.md) — single-block machines.
