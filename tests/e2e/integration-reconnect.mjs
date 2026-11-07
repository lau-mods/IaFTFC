const { scenario, command, camera, screenshot, waitFor, mct } = await import(process.env.HARNESS_E2E_LIB);
const { readFileSync } = await import('node:fs');

await scenario(async ({ clients }) => {
  const client = clients[0];
  command(client, 'time set day');
  command(client, 'weather clear');
  command(client, 'gamemode spectator');
  command(client, 'fill -9 79 -2 22 79 20 minecraft:stone');
  const display = (x, z, item) => command(client, `summon minecraft:item_display ${x} 82 ${z} {item:{id:"${item}",count:1},billboard:"center",transformation:{translation:[0f,0f,0f],left_rotation:[0f,0f,0f,1f],scale:[1.25f,1.25f,1.25f],right_rotation:[0f,0f,0f,1f]}}`);
  for (const [index, element] of ['fire', 'ice', 'lightning'].entries()) {
    const x = (index - 1) * 5;
    command(client, `setblock ${x} 80 2 iaftfc:metal/anvil/dragonsteel_${element}`);
    command(client, `setblock ${x} 80 4 iaftfc:metal/block/dragonsteel_${element}`);
    display(x, 4, `iaftfc:metal/ingot/dragonsteel_${element}`);
  }
  for (const [x, element] of [[-2.5, 'fire'], [2.5, 'ice'], [8.5, 'lightning']]) {
    command(client, `summon minecraft:armor_stand ${x} 80 8 {NoGravity:1b,ShowArms:1b,ArmorItems:[{id:"iceandfire:dragonsteel_${element}_boots",count:1},{id:"iceandfire:dragonsteel_${element}_leggings",count:1},{id:"iceandfire:dragonsteel_${element}_chestplate",count:1},{id:"iceandfire:dragonsteel_${element}_helmet",count:1}]}`);
  }
  command(client, 'setblock 12 80 4 iceandfire:dragonforge_fire_core_disabled');
  command(client, 'summon iceandfire:fire_dragon 15 80 15 {NoAI:1b,PersistenceRequired:1b,AgeTicks:1200000,Variant:"red",Rotation:[180f,0f]}');
  command(client, 'data merge entity @e[type=iceandfire:fire_dragon,sort=nearest,limit=1,x=15,y=80,z=15,distance=..4] {Items:[{},{id:"iaftfc:dragonarmor_steel_head",count:1},{id:"iaftfc:dragonarmor_steel_neck",count:1},{id:"iaftfc:dragonarmor_steel_body",count:1},{id:"iaftfc:dragonarmor_steel_tail",count:1}]}');
  command(client, 'summon iceandfire:dragon_egg 20 81 5 {NoGravity:1b,Color:"red"}');
  command(client, 'setblock 9 80 2 tfc:crucible');
  display(4, 2, 'iceandfire:fire_dragon_flesh');
  display(7, 2, 'iaftfc:dragonarmor_steel_head');
  display(10, 2, 'iaftfc:metal/ingot/dragonsteel_fire');
  display(13, 2, 'iceandfire:dragon_horn');
  display(16, 2, 'iceandfire:bestiary');
  command(client, 'item replace entity @s armor.chest with iceandfire:dragonsteel_fire_chestplate');
  command(client, 'item replace entity @s hotbar.0 with iaftfc:metal/ingot/dragonsteel_fire[tfc:heat={capacity:-1.0f,temperature:1600.0f,tick:-1L}]');
  command(client, 'item replace entity @s hotbar.1 with iaftfc:dragonarmor_steel_head');
  command(client, 'item replace entity @s hotbar.2 with iceandfire:dragon_horn[iceandfire:dragon_horn={entityType:"iceandfire:fire_dragon",entityUuid:"00000000-0000-0000-0000-000000000001",entityData:{id:"iceandfire:fire_dragon",AgeTicks:240000}}]');
  command(client, 'item replace entity @s hotbar.3 with iceandfire:bestiary[iceandfire:bestiary_pages={pages:["iceandfire:introduction","iceandfire:dragonforge"]}]');
  const scene = await waitFor(() => JSON.stringify(mct(client, 'block', 'get', '0', '80', '2')),
    value => value.includes('iaftfc:metal/anvil/dragonsteel_ice'));
  if (!scene.includes('iaftfc:metal/anvil/dragonsteel_ice')) throw new Error('Integration scene did not load');
  await camera(client, { x: 2, y: 82, z: -9, yaw: 0, pitch: 10 }, 8000);
  screenshot(client, 'AC-F022-001', 'three-metals-and-equipment');
  await camera(client, { x: 11.5, y: 83, z: -5, yaw: 0, pitch: 10 });
  screenshot(client, 'AC-F022-004', 'metal-food-book-and-entity-textures');
  await camera(client, { x: 18.5, y: 82, z: -3, yaw: 0, pitch: 10 });
  screenshot(client, 'AC-F022-004', 'dragon-egg-and-armor');
  await camera(client, { x: 12, y: 83, z: -5, yaw: 0, pitch: 10 });
  screenshot(client, 'AC-F022-002', 'before-reconnect');
  const address = JSON.parse(readFileSync('.harness-config.json', 'utf8')).runtime.server.address;
  mct(client, 'client', 'reconnect', '--address', address);
  await new Promise(resolve => setTimeout(resolve, 3000));
  const ready = mct(client, 'client', 'wait-ready', client, '--timeout', '90');
  if (ready.inWorld !== true) throw new Error('Client did not rejoin the server');
  await camera(client, { x: 2000.5, y: 83, z: 2000.5, yaw: 0, pitch: 10 });
  await camera(client, { x: 12, y: 83, z: -5, yaw: 0, pitch: 10 });
  screenshot(client, 'AC-F022-002', 'after-reconnect');
  await camera(client, { x: 11.5, y: 81.5, z: 0, yaw: 0, pitch: 10 });
  command(client, 'gamemode survival');
  mct(client, 'input', 'key', 'press', 'e');
  mct(client, 'gui', 'wait-open', '--timeout', '15');
  const size = mct(client, 'screen', 'size');
  mct(client, 'input', 'mouse-move', String((size.width ?? size.w) - 10), String((size.height ?? size.h) - 10));
  screenshot(client, 'AC-F022-002', 'reconnected-equipment-and-saved-items');
  mct(client, 'gui', 'close');
  mct(client, 'inventory', 'hotbar', '3');
  mct(client, 'inventory', 'use');
  mct(client, 'gui', 'wait-open', '--timeout', '15');
  const left = ((size.width ?? size.w) - 390) / 2;
  const top = ((size.height ?? size.h) - 245) / 2;
  mct(client, 'input', 'mouse-move', String(Math.floor(left + 380)), String(Math.floor(top + 200)));
  await new Promise(resolve => setTimeout(resolve, 1000));
  screenshot(client, 'AC-F022-002', 'reconnected-bestiary-index');
  mct(client, 'input', 'click', String(Math.floor(left + 280)), String(Math.floor(top + 35)));
  await new Promise(resolve => setTimeout(resolve, 1000));
  screenshot(client, 'AC-F022-002', 'reconnected-bestiary-forge-page');
});
