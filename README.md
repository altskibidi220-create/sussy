# SoulSMP (Paper 1.21.11)

Infuse-style souls. 12 souls, each with 1 passive + 2 abilities.

## Build
Requires JDK 21 and Maven.

    mvn clean package

The jar is created in `target/SoulSMP-1.0.0.jar`. Drop it into `plugins/` on a Paper 1.21.11 server.

## Resource pack
Use `SoulSMP-ResourcePack.zip` (soul item textures + HUD icon font).
- Put it in `server.properties` (`resource-pack=<url>`, `resource-pack-sha1=<sha1>`) or fill in `resource-pack` in `plugins/SoulSMP/config.yml`.
- Without the pack the souls still work. Set `hud-icon-font: false` in config.yml so the HUD shows a plain symbol instead of the icon.
- Glyph order = registration order in `SoulRegistry`. If you add a soul, add it to `make_pack.py` in the same position.

## Controls
- **F** (offhand swap): ability 1
- **Shift + F**: ability 2
- Right-click a soul item: absorb it (admins can swap souls this way)
- Souls are lost on death; by default a new one is rolled after respawn (`reroll-on-death`).

## Admin
`/soul` opens the test GUI (needs `soulsmp.admin`, default op):
left-click = equip, right-click = get item, plus remove / roll animation / reset cooldowns.
Also: `/soul set|give <player> <soul>`, `/soul clear|roll|cooldown [player]`.

## Souls
| Soul | Passive | F | Shift+F |
|---|---|---|---|
| Vitality | +2 hearts | Heart Burst | Life Bulwark |
| Might | +1 attack dmg | Berserk | Titan Slam |
| Swiftness | +10% speed | Dash | Hyper Drive |
| Inferno | fire immune, ignite attackers | Fireball | Ring of Fire |
| Frost | freeze immune, slow attackers | Frost Nova | Ice Lance |
| Tempest | lightning immune, +10% atk speed | Smite | Chain Lightning |
| Gale | no fall dmg, Jump Boost I | Updraft | Cyclone Blast |
| Terra | +3 armor, +20% KB resist | Stone Guard | Earthquake |
| Tide | water breathing, drown immune, dolphin's grace | Tidal Wave | Whirlpool |
| Shadow | night vision | Shadow Step | Veil of Night |
| Venom | poison immune, poison attackers | Toxic Cloud | Venom Bolt |
| Radiance | regen when low | Holy Beam | Sanctuary |

## Config
Cooldowns (seconds) are in `plugins/SoulSMP/config.yml` under `cooldowns:`. Edit, then run `/soul reload`.
