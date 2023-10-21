# Minecraft Mod

Single player mod for Minecraft to emulate the playable character being a King.

At first the goal is to program all roles, then the rouge-like elements will be added.

## Milestones Reached:

- Reimplementation of FakePlayer class from Fabric
  - Enables more realistic simulation of the FakePlayer
- Custom MoveControl for our FakePlayer
  - Enables movement (FakePlayer does not derive from PathAwareEntity, so requires custom MoveControl)
- A* Pathing MovementController for FakePlayer
  - Enables pathing to a specific block with obstacle avoidance with jumping
- Command to spawn a FakePlayer
- Command to assign a role to a FakePlayer
- Reimplementation of Goal system for our FakePlayer
  - Minecraft has a Goal system for MobEntities, but FakePlayer derives from PlayerEntity
- Serializing/Deserializing Kingdoms and Citizens (FakePlayer) with world saves
  - Allows for FakePlayer to have a persistent state
  - Allows for abstract organization of Kingdoms and Citizens to have a persistent state

## Roles

- [x] Lumberjack
  - [x] Chops down trees
  - [ ] Plants trees
- [ ] Guard
    - [ ] Patrols the kingdom
    - [ ] Fights enemies
    - [ ] Can be assigned to a specific spot/gate
    - [ ] Can be assigned to a specific person
    - [ ] Watches for hostile mobs or persons
- [ ] Soldier
  - [ ] Can act as a guard
  - [ ] Can be sent towards another kingdom
  - [ ] Attacks non-citizens
  - [ ] Pillages other kingdoms
  - [ ] Takes best equipment from armory
  - [ ] Can be bribed/blackmailed by spys to go on killing sprees
- [ ] Assassin
  - [ ] Takes money to kill a specific person
    - [ ] Takes money from angry spouses or spys
  - [ ] Can get caught and executed
  - [ ] Must be ex-soldier or ex-guard
- [ ] Military General
  - [ ] Commands soldiers
  - [ ] Can be sent towards another kingdom
  - [ ] Can overthrow the manager
- [ ] Alchemist
  - [ ] Makes potions
  - [ ] Transfers potions to the armory
- [ ] Farmer
- [ ] Miner
  - [ ] Mines resources
- [ ] Banker
  - [ ] Collects taxes
  - [ ] Pays wages to public workers
  - [ ] Safe keeps the kingdom's assets
- [ ] Hunter
- [ ] Cook
- [ ] Baker
  - [ ] Takes wheat from farmer
  - [ ] Makes bread for sale
- [ ] Builder
  - [ ] Will build a road, tunnel, and bridge network
  - [ ] Will build a wall around the kingdom
  - [ ] Will build gates in the wall
- [ ] Manager
  - [ ] Assigns new roles to citizens
- [ ] Blacksmith
- [ ] Trader (buys and sells resources between towns)
- [ ] Beggar
  - [ ] Can be assigned to a specific spot
  - [ ] Hassles citizens for money
  - [ ] Spends money on buying eggs to throw at people
- [ ] Thief
- [ ] Assassin (goes to other kingdoms)
- [ ] Priest (operates domestically, can be used to pacify unhappy citizens)
  - [ ] Holds sermons
- [ ] Religious Prophet
  - [ ] Founds a religion
  - [ ] If killed before reaching 20 followers, religion dies out.
  - [ ] If killed after reaching 20 followers, the religion spreads faster.
  - [ ] Prophets either fork a religion or create a new one
  - [ ] Prophets can be sent to other kingdoms to spread their religion
- [ ] Teacher
  - [ ] Teaches children
- [ ] Drug Dealer (operates domestically, can be used to pacify unhappy citizens)
- [ ] Spy (can be sent to other kingdoms to sell drugs and destabilize that kingdom)

## Traits

- Hates rain (will seek shelter when it rains)
- Murder (chance of murdering someone for fun)
- Age

## Attributes

- Happiness
- Boredom
- Generosity
- Intelligence
- Married

## Role Priorities

Once a new adult citizen joins (either by a child growing up or immigration) they
get assigned a role depending on this order

- Lumberjack
- Guard
- Miner
- Hunter
- Manager
- Cook
- Farmer
- Blacksmith
- 


## Disclaimer

I haven't used Java on a daily basis since 2018. 
I'm sure there are better ways to do things, but I'm just trying to get something working.

---

Based off Fabric Mod Template

## Setup

For setup instructions please see the [fabric wiki page](https://fabricmc.net/wiki/tutorial:setup) that relates to the IDE that you are using.

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.
