# AE2 autocrafting and Masterful Machinery

How to make a machine's recipes autocraftable from an ME terminal, using
[Applied Energistics 2](https://www.curseforge.com/minecraft/mc-mods/applied-energistics-2). The
port type only exists when AE2 is installed.

## The port

| Port type | What it does |
|---|---|
| `mm:ae2/pattern` | Puts the machine's recipes in the ME crafting terminal and runs the orders |

## Port config

```json
{
  "id": "me_port",
  "controllerIds": "mm:pulverizer",
  "name": "ME Pattern Port",
  "type": "mm:ae2/pattern",
  "config": {
    "patternPriority": 0
  }
}
```

| Option | Default | Meaning |
|---|---|---|
| `patternPriority` | `0` | Which machine AE2 prefers when several can make the same item |

## Setting one up

1. Put the port block in the machine's structure, like any other port.
2. Run an ME cable to it. The port is a normal machine to AE2: it needs a **channel** and power.
3. Give the machine its usual **input hatches** and **output hatches**. The port fills the input
   hatches and takes results out of the output hatches; it does not store anything itself.

Order the item in the crafting terminal. AE2 takes the ingredients out of storage, puts them in the
machine, the machine runs, and the result goes back into storage.

## Which recipes appear

A recipe is only offered when it has an **item or fluid on both sides**. AE2 cannot carry energy,
Source or radiation, so it cannot supply them or accept them as a result.

| Recipe | In the terminal? |
|---|---|
| 2 clay → 1 brick | Yes |
| 1 bucket of lava + 1 iron → 1 steel | Yes |
| 30,000 FE → 1 diamond | No — nothing AE2 can supply |
| 1 clay → 1,000 Source | No — nothing AE2 can accept |

Recipes that do not appear still run normally in the machine. Energy, Source and the rest are
supplied through their own ports as usual.

## Matter as an ingredient

Recipes that use Replication matter can be autocrafted when
[Replication AE2 Bridge](https://www.curseforge.com/minecraft/mc-mods/replication-ae2-bridge) is
installed and its block connects your matter network to ME. The matter has to be visible in the ME
terminal first — if it is not there, the bridge is not set up, and nothing MM does can help.

## Notes

- The port runs **one craft at a time**. AE2 queues the rest.
- It is not a storage bus. The machine's contents are not visible in the terminal; only finished
  results are pushed back.
- Several machines can offer the same recipe. AE2 shares the work between them, and
  `patternPriority` decides who is asked first.

## From KubeJS

```js
// kubejs/startup_scripts/
MMEvents.registerPorts(event => {
    event.create('me_port')
        .name('ME Pattern Port')
        .controllerId('mm:pulverizer')
        .config('mm:ae2/pattern', config => {
            config.patternPriority(0)
        })
})
```

## See also

- [`reference.md`](reference.md) — every key and option.
- [`replication.md`](replication.md) — matter, and ordering from the Replication Terminal.
