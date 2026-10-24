# Astral Sorcery and Masterful Machinery

How to make machines that run on starlight and lumen from
[Astral Sorcery](https://www.curseforge.com/minecraft/mc-mods/astral-sorcery). The port types and
conditions on this page only exist when Astral Sorcery is installed.

Liquid starlight is a normal fluid, so it goes through `mm:fluid` ports like any other.

## The ports

| Port type | What it holds | Sides |
|---|---|---|
| `mm:astralsorcery/starlight` | Starlight, kept per constellation | Input |
| `mm:astralsorcery/lumen` | Lumen of any type | Input and output |

## Starlight

### Port config

```json
{
  "id": "starlight_port",
  "controllerIds": "mm:star_forge",
  "name": "Starlight Lens Mount",
  "type": "mm:astralsorcery/starlight",
  "only": "input",
  "config": {
    "capacity": 1000
  }
}
```

| Option | Default | Meaning |
|---|---|---|
| `capacity` | required | The most starlight the port holds, all constellations together |

### Getting starlight in

Link a lens to the port with the Linking Tool, the same way you would link it to an altar. Starlight
beamed into the port is stored under the constellation it came from. Daylight, the moon phase and
the focus crystal all change how much arrives, just as they do for Astral's own blocks. A good
setup at night gives about 1 starlight per tick.

### Recipe ingredient

```json
{ "type": "mm:astralsorcery/starlight", "starlight": 50, "constellation": "astralsorcery:aevitas" }
```

| Key | Meaning |
|---|---|
| `starlight` | How much to take. A number, or `{ "min": 20, "max": 50 }` |
| `constellation` | Optional. Only starlight of this constellation counts. Leave it out to take any |

Starlight is input only. With `"per_tick": true` the amount is taken every tick.

## Lumen

### Port config

```json
{
  "id": "lumen_port",
  "controllerIds": "mm:star_forge",
  "name": "Lumen Port",
  "type": "mm:astralsorcery/lumen",
  "config": {
    "capacity": 2000,
    "lumen": ["astralsorcery:aevitas", "astralsorcery:vicio"]
  }
}
```

| Option | Default | Meaning |
|---|---|---|
| `capacity` | required | The most lumen the port holds, all types together |
| `lumen` | any | One lumen id or a list. The port only takes these types |

### How lumen gets in and out

An **input** port pulls lumen from Astral's lumen network. It reaches lumen sources within 16 blocks
that it can see, and every second it asks for the lumen types its machine's recipes use. Astral
draws the transfer beam as usual. With `lumen` set, it pulls only those types.

An **output** port joins the lumen network as a source. Astral blocks that need lumen, like the
Lumen Crystallizer or the Tree Beacon, take from it when it is in range and in sight.

### Recipe ingredient

```json
{ "type": "mm:astralsorcery/lumen", "lumen": "astralsorcery:aevitas", "amount": 100 }
```

| Key | Meaning |
|---|---|
| `lumen` | The lumen type, e.g. `astralsorcery:aevitas` |
| `amount` | How much. A number, or `{ "min": 50, "max": 100 }` |

Works as an input and as an output. JEI shows it with Astral's own lumen icon.

## Conditions

| Type | Keys | Passes when |
|---|---|---|
| `mm:astralsorcery/constellation` | `constellation` | That constellation is in the sky |
| `mm:astralsorcery/eclipse` | `eclipse`: `solar` or `lunar` | That eclipse is happening |

```json
"conditions": [
  { "type": "mm:astralsorcery/constellation", "constellation": "astralsorcery:discidia" },
  { "type": "mm:astralsorcery/eclipse", "eclipse": "lunar" }
]
```

## A full recipe

Starlight from Aevitas and a dose of Vicio lumen make Aevitas lumen, only while Aevitas is up:

```json
{
  "structureId": "mypack:star_forge",
  "ticks": 100,
  "conditions": [
    { "type": "mm:astralsorcery/constellation", "constellation": "astralsorcery:aevitas" }
  ],
  "inputs": [
    { "type": "mm:input/consume", "ingredient": { "type": "mm:astralsorcery/starlight", "constellation": "astralsorcery:aevitas", "starlight": 200 } },
    { "type": "mm:input/consume", "ingredient": { "type": "mm:astralsorcery/lumen", "lumen": "astralsorcery:vicio", "amount": 50 } }
  ],
  "outputs": [
    { "type": "mm:output/simple", "ingredient": { "type": "mm:astralsorcery/lumen", "lumen": "astralsorcery:aevitas", "amount": 250 } }
  ]
}
```

## Seeing what is inside

Right-click a port for a bar, and hover it to see the amount of each constellation or lumen type.
Jade shows the same bar.

## From KubeJS

```js
// kubejs/startup_scripts/
MMEvents.registerPorts(event => {
    event.create('starlight_port')
        .name('Starlight Lens Mount')
        .controllerId('mm:star_forge')
        .only('input')
        .config('mm:astralsorcery/starlight', config => {
            config.capacity(1000)
        })

    event.create('lumen_port')
        .name('Lumen Port')
        .controllerId('mm:star_forge')
        .config('mm:astralsorcery/lumen', config => {
            config.capacity(2000)
            config.lumen('astralsorcery:aevitas', 'astralsorcery:vicio')
        })
})
```

```js
// kubejs/server_scripts/
MMEvents.createProcesses(event => {
    event.create('kubejs:aevitas_lumen')
        .structureId('mypack:star_forge')
        .ticks(100)
        .constellation('astralsorcery:aevitas')
        .input({ type: 'mm:input/consume', ingredient: { type: 'mm:astralsorcery/starlight', constellation: 'astralsorcery:aevitas', starlight: 200 } })
        .output({ type: 'mm:output/simple', ingredient: { type: 'mm:astralsorcery/lumen', lumen: 'astralsorcery:aevitas', amount: 250 } })
})
```

`.eclipse('solar')` adds the eclipse condition.

## See also

- [`reference.md`](reference.md) — every key and option.
- [`ars-nouveau.md`](ars-nouveau.md) — Ars Nouveau Source.
- [`projecte.md`](projecte.md) — ProjectE EMC.
