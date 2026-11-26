# Machine effects

Animated effects drawn on a formed machine: liquid streams, drips, falling drops, pools and ripples,
liquid seeping down block sides, particles like smoke, steam and sparks, beams and arcs, and glowing
faces and halos. Effects follow the machine's state, fading up when it starts working and back down
to idle when it stops.

The easy way to make one is the **Effect Studio**, in game. You can also write the files by hand.

## The Effect Studio

Hold the **Effect Wand** and right-click any block of a formed machine. Only operators can use it.

- **Library.** Drag a preset (Tesla coil, Reactor core, Foundry pour, Bleeding block and more) or a
  single part (drips, smoke, sparks, a glowing block...) onto the machine.
- **Spots.** Drag the points where the effect sits, holding Alt to change their height. Ctrl-click or drag a box to select several and
  move, centre or delete them together. Right-click a point for more options, like centring it on a
  block face.
- **Layers.** Each effect is a stack of layers. Turn a layer off, turn it off for one spot only, or
  give it a spot of its own with the **+** next to its spot setting. Click a fluid port to make a
  layer use that port's fluid colour and look.
- **Look.** Amount, size and speed, separately for idle and working, plus colour. **Reroll** gives a
  new random look.
- **Advanced.** Every setting in plain words, with a hover explanation for each. The **Expert**
  switch shows the raw setting names used in the files below.
- **Glowing blocks.** Click **Pick** on a glow, then click blocks in the view to choose which ones
  glow. **Clear** unselects them all.
- **Arcs.** Connect two points to draw an arc between them. **Curve** bends it, and **Curve
  direction** turns the bend: 0 is up, 90 sideways, 180 down.

**Save** writes the effect into `kubejs/`:

- the effect itself to `kubejs/assets/<namespace>/mm_fx/<name>.json`
- where it sits on the machine to `kubejs/data/<namespace>/mm/structure_effects/<structure>.json`

Players need the `mm_fx` files on their side, so ship `kubejs/assets` with the pack. On a dedicated
server, copy the `structure_effects` file to the server as well.

Save a preset to reuse an effect on other machines. Presets go to `config/mm/effect_presets/`.

## Effect files

An effect is a list of layers. Put it in a resource pack or `kubejs/assets` at
`assets/<namespace>/mm_fx/<name>.json`:

```json
{
  "rise": 2.4,
  "fall": 3.6,
  "layers": [
    {
      "type": "drip",
      "at": "spout",
      "to": "floor",
      "texture": "mm:textures/fx/liquid_flow.png",
      "color": "#ff7a1a",
      "rate": { "idle": 0, "working": 1.5 },
      "glow": true
    },
    {
      "type": "pool",
      "at": "spout",
      "to": "floor",
      "texture": "mm:textures/fx/liquid_pool.png",
      "color": "#ff7a1a"
    }
  ]
}
```

This one is `kubejs/assets/mypack/mm_fx/kiln_drip.json`, so its id is `mypack:kiln_drip`.

Words like `"spout"` and `"floor"` are **anchor names**. The effect file only names them; the
structure says where they are (see below). That way one effect can sit on many machines.

| Key | Default | Meaning |
|---|---|---|
| `layers` | | The layers, drawn in order |
| `rise` | `2.4` | Seconds to fade from idle to working |
| `fall` | `3.6` | Seconds to fade from working back to idle |
| `hold` | `1.25` | Seconds to stay at working after the machine stops, so short gaps between recipes don't flicker |
| `view_distance` | `96` | How far away the effect is drawn, in blocks. The player's own setting can lower it |
| `seed` | `0` | Change it to get a different random look |

### Idle and working values

Settings that can differ between idle and working take either one number, or both:

```json
"rate": 2
"rate": { "idle": 0.5, "working": 16 }
```

### Settings every layer has

| Key | Meaning |
|---|---|
| `type` | `stream`, `drip`, `drops`, `pool`, `ripple`, `seep`, `emitter`, `beam` or `glow` |
| `name` | A label shown in the Effect Studio |
| `off` | `true` turns the layer off |
| `skip` | Point numbers to leave out, counting from `0`, such as `[1, 3]` |
| `texture` | The texture, such as `mm:textures/fx/spark.png` |
| `color` | A colour such as `"#9fd8ff"`, tinting the texture |
| `alpha` | Opacity from `0` to `1` |
| `glow` | `true` ignores light, so it shines in the dark |
| `port_color` | `true` takes the colour of the machine's fluid port |
| `port_texture` | `true` draws the port fluid's own texture (stream, drip, drops, pool, ripple, seep) |

Textures that come with the mod, in `mm:textures/fx/`: `beam`, `blood_flow`, `blood_pool`,
`blood_white`, `glow_face`, `liquid_flow`, `liquid_pool`, `molten_flow`, `molten_pool`, `soft`,
`spark`.

### Layer types

| Type | Anchors | What it draws | Main settings |
|---|---|---|---|
| `stream` | `from` points, `to` height | A falling column of liquid | `radius`, `gravity`, `sway`, `breakup`, `droplets` |
| `drip` | `at` points, `to` height | Beads that swell and drop | `rate`, `size`, `drop_size` |
| `drops` | | Single drops that fall and splash, from the stream and drip layers | `gravity`, `max`, `splash` |
| `pool` | `at` points, `to` height | A puddle that grows while it works and drains when it stops | `size`, `fill`, `drain`, `swirl` |
| `ripple` | | Rings where falling drops land | `life`, `speed`, `max` |
| `seep` | `at` points, `top` height | Liquid crawling down the sides of the blocks around each point | `streams`, `faces`, `side_streams`, `chance`, `speed`, `stains` |
| `emitter` | `at` points | Particles such as smoke, steam, sparks and embers | `rate`, `life`, `size`, `velocity`, `velocity_spread`, `box`, `gravity`, `drag`, `blend` |
| `beam` | `from` and `to` points | A beam or arc between points | `width`, `segments`, `arc`, `arc_angle`, `jitter`, `flicker`, `links` |
| `glow` | `at` blocks or points | Glowing block faces, or a soft halo with `"mode": "halo"` | `mode`, `size`, `pulse`, `faces` |

The Effect Studio's **Advanced** tab lists every setting of every layer with its range and an
explanation. Turn on **Expert** there to see the names to use in a file.

A few settings worth knowing:

- **Seep.** `faces` picks the sides that bleed, such as `["north", "east"]`. `side_streams` sets how
  many lines run down each side, from 1 to 4: `{ "north": 4, "west": 2 }`. A point on the corner
  between blocks, such as the middle of a 2x2 pillar, makes every block around it seep. Seeping
  follows stairs, slabs and other partial blocks.
- **Beam.** With several points, `links` says which connect, as pairs of point numbers counting from
  `0`: `[[0, 1], [1, 2]]`. `arc` bends the beam and `arc_angle` turns the bend around it, in degrees.
  `jitter` makes it crackle like lightning.
- **Emitter.** Colours, sizes, lives and alphas take two values, start and end:
  `"alpha": [1, 0]` fades each particle out.

## Putting an effect on a structure

### KubeJS

```js
MMEvents.createStructures(event => {
    event.create('kubejs:alloy_kiln')
        // ...layout as usual...
        .effect({
            fx: 'mypack:kiln_drip',
            anchors: {
                spout: [[1.5, 2, 1.5]],
                floor: 0.05
            }
        })
})
```

### JSON

Add it to the structure's `visuals` list:

```json
"visuals": [
  {
    "type": "effect",
    "fx": "mypack:kiln_drip",
    "anchors": { "spout": [[1.5, 2, 1.5]], "floor": 0.05 }
  }
]
```

Or put it in its own file at `data/<namespace>/mm/structure_effects/<name>.json`, which is what the
Effect Studio writes. This works for any structure, including ones from other packs:

```json
{
  "structure": "kubejs:alloy_kiln",
  "replace": true,
  "effects": [
    { "fx": "mypack:kiln_drip", "anchors": { "spout": [[1.5, 2, 1.5]], "floor": 0.05 } }
  ]
}
```

`"replace": true` replaces the effects the structure already has. Without it, these are added.

| Key | Meaning |
|---|---|
| `fx` | The effect's id |
| `anchors` | Where each anchor name in the effect sits on this structure |
| `port` | Optional. The layout key of the fluid port that `port_color` and `port_texture` use. Without it, layers keep their own `color` and `texture` |

### Anchors

Positions use the structure's layout: `x` is the position in a row from the left, `y` is the layer
counted from the **bottom** starting at `0`, and `z` is the row from the front. Whole numbers are
block edges, so `[1.5, 2, 1.5]` is the middle of the top of the block at `[1, 1, 1]`. Effects turn
with the machine when it is built facing another way.

| Anchor | Example | Used for |
|---|---|---|
| A list of points | `[[1.5, 2, 1.5], [3.5, 2, 1.5]]` | Where streams, drips, particles, seeping and beams start. One copy of the layer per point |
| A height | `0.05` | Floors and tops, such as where a stream lands or where seeping starts |
| A layout key | `"G"` | Every block with that key, for glowing blocks |
| A block list | `{ "blocks": [[1, 0, 1], [2, 0, 1]] }` | Just those blocks |
| A box | `{ "from": [0, 0, 0], "to": [2, 1, 2] }` | Every block in the box, corners included |

## Settings

Each player can tune effects in the client config, or on the Client page of the mod's config screen.
Under `[effects]`:

| Setting | Default | Meaning |
|---|---|---|
| `quality` | `MEDIUM` | `LOW`, `MEDIUM` or `HIGH`. Lower draws simpler effects from closer up and is faster |
| `viewDistance` | `64` | How far away effects are drawn, in blocks |
| `particleAmount` | `100` | Amount of effect particles, in percent |
| `maxParticles` | `1500` | Most effect particles alive at once across all machines |

`machineEffects` under `[controller]` turns all effects off. Bind **Cycle machine effect quality** in
Controls to switch quality in game, and open F3 to see how much frame time machine effects take.

## See also

- [`models.md`](models.md) — controller looks, GeckoLib models and fluid volumes.
- [`kubejs.md`](kubejs.md) — structures and recipes from KubeJS.
- [`reference.md`](reference.md) — every key and option.
