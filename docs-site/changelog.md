---
layout: default
title: Changelog
---

[← Back to home](index.html)

# Changelog

A version-by-version history of notable changes since this fork began (Buddy Drones 1.1.0 was the last release of
the original mod; this fork's history starts at 1.1.1). For a full summary of how this fork differs from the
original mod overall, see [Changes from the Original Mod](changes.html).

## 1.3.2

- Drones no longer attack piglins or zombie piglins that are currently passive. They will engage them once they become
  hostile, just like Endermen.

## 1.3.1

*Released for Minecraft 1.21.11 and 26.1.2 only, as the underlying bugs are related to Minecraft changes in those versions.*

- Fixed arrows frequently glancing off or passing through the target's hitbox without registering a hit.
- Fixed arrows sometimes launching in the wrong direction due to incorrect initial orientation when spawned.

## 1.3.0

### Ion Thruster visual rework

- Ion Thrusters now spawn occasional ion trail particles while flying, more frequently the faster the drone moves.
- A pulsing glow effect renders beneath each Ion Thruster while the drone is in flight.
- Ion Thrusters display connector "arms" when placed next to solid blocks, so they look properly attached to the
  drone's frame.

### Rotor placement flexibility

- Wooden Rotors and Advanced Rotors can now be placed at the top or bottom of a block, like a slab. This gives more
  flexibility in positioning rotors when constructing drones.

### Other fixes and improvements

- The controller now remembers a drone's name when you place it back on the platform for re-editing.
- Fixed a bug where tall drones could not be placed back on the platform for editing.
- Fixed the drone spinning unexpectedly when its rotation was near the wraparound point (e.g. crossing from 359° to 0°).

## 1.2.5

- **Assembly platform rework**: the Drone Assembly Controller now faces you when placed, and that facing determines
  the front of the drone. Assembly Frame blocks no longer need a specific orientation, and re-loading a Pocket Drone
  onto a platform no longer requires precise repositioning — the controller finds space for it automatically.
- New mod icon.

## 1.2.2

- **Added Trinkets (Fabric) and Curios (NeoForge) support**, providing a dedicated Drone slot alongside the existing
  Accessories integration.
- Fixed a bug that allowed breaking unbreakable blocks (like bedrock) by assembling them as part of a drone.
- Fixed a pathfinding bug.
- Arrow attacks now only adjust the drone's position horizontally toward the target, not vertically.
- Converted the codebase to Mojang mappings (internal change, no gameplay impact).
- Updated in-game documentation to reflect recent changes.

## 1.2.1

- Added rudimentary pathfinding so drones better navigate around obstacles.
- Improved drone acceleration and deceleration for smoother movement.
- Tuned the arrow attack's startup cooldown, recoil, and hover positioning while firing.
- Added a new mod icon and a unique icon for the drone accessory slot.
- Updated author and project URL information for the fork.

## 1.2.0

- **Item pickup revamp**: the drone now visibly carries picked-up items, syncs this to clients, and plays a sound
  when depositing items into the player's inventory.
- **Movement overhaul**: more natural, frame-rate-independent rotation interpolation, tweaks to hovering, and the
  drone snaps to its desired rotation more responsively.
- **Priority system rework** with a unified attack cooldown across attack types.
- Rebalanced the thrust-to-weight ratio and added a speed limit for drones.
- Added recoil to arrow attacks, plus a dispenser firing sound.
- The Beacon block now rotates to face forward when rendered on a drone.
- Fixed iron trapdoor rotors not working correctly.
- Fixed a deadlock that could occur in singleplayer worlds.
- Drones no longer stay locked onto targets they've lost line of sight to, and targeting of small mobs (e.g. baby
  zombies) was improved.

## 1.1.1

The first release of this fork, building on top of Buddy Drones 1.1.0:

- Drones no longer target Endermen with ranged attacks (avoids triggering their teleport).
- Arrows now fire from the drone itself rather than appearing above the player's head.
- Various arrow attack safety and cooldown robustness improvements.
- General drone movement improvements.
- Added a dedicated drone slot via the Accessories mod.
- Other small bug fixes.
