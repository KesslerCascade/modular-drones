---
layout: default
title: Changes from the Original Mod
---

[← Back to home](index.html)

# Changes from the Original Mod

Modular Drones is a fork of rearth's (of OriTech fame) **Buddy Drones** mod. The original mod hadn't been updated in
some time, so this fork started out fixing some issues and has since grown into its own thing, with some balance
changes that go beyond the original's vision. This page covers what's changed.

## Equip slot integration

- If you have the **Curios**, **Trinkets**, or **Accessories** mod installed, a dedicated Drone slot is added so you
  can activate your drone without giving up your helmet slot.
- The cosmetic head slot is still supported for backwards compatibility with the Accessories mod, though this is
  expected to be removed in a future version.
- If Curios or Trinkets is installed, the drone can no longer be equipped to the head armor slot and must go into the
  dedicated Drone slot instead — the dedicated slot is strictly better, so there's no downside.

## Assembly platform rework

Building and re-editing drones is now much less fiddly:

- The **Drone Assembly Controller** now has a facing direction, set to face you when you place it. This facing
  determines the "front" of the drone you build on it — no more guessing which way your drone will end up facing.
- **Assembly Frame** blocks no longer have an orientation requirement — place them in any arrangement and the
  controller will detect the whole connected platform.
- When re-loading a Pocket Drone onto a platform to edit it, the drone no longer needs to go back in the exact same
  position — the controller automatically finds space for it on the platform.
- Clearer error messages when assembly fails (e.g. platform too small, or the build space is occupied).

<!-- IMAGE: before/after style screenshot or diagram showing the controller's facing and a non-uniform frame layout -->

## Ion Thruster visual rework

The Ion Thruster (the top-tier rotor) got a visual overhaul:

- Occasional ion trail particles while flying, more frequent the faster the drone moves.
- A pulsing glow effect rendered beneath each Ion Thruster while the drone is in flight.
- Thrusters now display connector "arms" when placed next to solid blocks, so they look properly attached to the
  drone's frame instead of floating.

This is a purely visual upgrade — thrust and power values are unchanged.

<!-- IMAGE: screenshot of a drone with Ion Thrusters showing the particle trail and glow effect -->

## Revamped movement

- Eliminated jarring "zips" toward targets — movement feels much more natural.
- Reworked speed/power mechanics so they line up intuitively with the "Speed" stat shown in the drone builder UI.
- Fixed sluggish turning.
- The drone is much better at navigating around obstacles instead of phasing through solid blocks.
- Several flight modes let the drone better handle tight spaces and 2-block-high corridors.

## Arrow attack improvements

- Won't target Endermen, since hitting them with projectiles makes them teleport.
- Arrows now fire from the drone itself, rather than appearing above the player's head.
- Won't target enemies through walls or underground.
- Won't fire if the player is in the way and likely to get hit.
- Flies a bit ahead of the player when engaging enemies, for a better firing angle.
- Added recoil, sound effects, and other polish when firing.
- Fixed targeting of small mobs (e.g. baby zombies) so they're hit reliably.
- Prefers to fight enemies close to the player, to keep the player defended.

## Melee attack improvements

- Won't target Endermen unless they're already aggressive.
- Prefers to fight enemies close to the drone itself, minimizing travel time.

## Item pickup improvements

- Reworked the carry animation to be much smoother.
- Can pick up an entire stack of items and carry it beneath the drone.
- Will gather other items stackable with what it's currently holding before returning to the player.
- Tries to insert collected items directly into the player's inventory, only dropping them in the world if the
  inventory is full.
- Won't immediately re-pick-up an item it just dropped.
- Can hold onto a carried item if interrupted by a higher-priority task (like fighting), and resumes delivery
  afterward.

## Priority system overhaul

- Prefers the arrow/laser launcher over melee when the drone has both, since ranged attacks deal more average DPS.
- Will still use melee against targets that can't be shot (e.g. Endermen) or when it doesn't have a clean line of
  sight.
- Fixed bugs where tasks could be interrupted incorrectly.
- Internal timers prevent the drone from rapidly flip-flopping between tasks.

## Balance tweaks

- Drone weight is no longer based on block hardness — so building a drone out of leaves or carpet for looks doesn't
  make it lighter than a "techy" build.
- Weight is now based purely on the number of blocks.
- Rotors and thrusters are excluded from the weight calculation, so adding one never unintuitively makes your drone
  *slower*.

## Miscellaneous

- Using a Beacon for the laser/sonic attack no longer looks silly — it's rotated to face forward on the drone.
- Fixed a bug that let players break bedrock and other unbreakable blocks with the mining support ability.
- Unified attack cooldowns across all attack types for consistency.
