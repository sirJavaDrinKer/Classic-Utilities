# Classic Utilities

I couldn't find a mod for 1.4.7 that added freecam or fullbright :p
+ Now with Light Overlay!

Supported Versions: 
- Forge 1.3.2
- Forge 1.4.7
- Forge 1.5.2
Planned:
- Forge 1.6.4
- Forge 1.7.10
Possible:
- Forge 1.2.5 (depending on how much of a pain in the rear it is)
- Legacy fabric versions. (haven't looked into)
- Beta versions + Indev/Infdev (haven't looked into)

## Controls

| Function   | Key  | Functionality |
|------------|------|---------------|
| Freecam    | `V` | Detaches the camera from the player and allows them to fly their camera around |
| Fullbright | `F` | Maxes out the gamma so the player can see fine in dark areas |
| Light Overlay | `L` | Shows areas where hostile mobs are capable of spawning |

Freecam should automatically switch off when your character is interrupted (killed, switch dimensions, etc).

## Building

```
./gradlew build
./collect.sh
```

## Credits

Project layout and Gradle/Voldeloom scaffolding adapted from
[AutoThirdPerson](https://github.com/quat1024/AutoThirdPerson) by quaternary (LGPL-3.0-or-later).
