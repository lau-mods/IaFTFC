const { scenario, command, camera, screenshot, waitFor, mct } = await import(process.env.HARNESS_E2E_LIB);

await scenario(async ({ clients }) => {
  const client = clients[0];
  command(client, 'gamemode spectator');
  command(client, 'time set day');
  command(client, 'weather rain 600');
  command(client, 'kill @e[type=iceandfire:fire_dragon]');
  command(client, 'kill @e[type=iceandfire:ice_dragon]');
  command(client, 'kill @e[type=iceandfire:lightning_dragon]');
  command(client, 'fill -13 79 -3 13 79 7 minecraft:stone');
  command(client, 'setblock -8 80 3 minecraft:netherrack');
  command(client, 'setblock -8 81 3 minecraft:fire');
  command(client, 'setblock 0 80 3 minecraft:packed_ice');
  command(client, 'setblock 0 81 3 iceandfire:egginice');
  command(client, 'data merge block 0 81 3 {Color:"blue",Age:7000}');
  command(client, 'summon iceandfire:dragon_egg -7.5 81 3.5 {Color:"red",DragonAge:7000}');
  command(client, 'summon iceandfire:dragon_egg 8.5 81 3.5 {Color:"electric",DragonAge:7000}');
  await camera(client, { x: 0.5, y: 83, z: -15, yaw: 0, pitch: 8 });
  screenshot(client, 'AC-F015-004', 'three-eggs-before-hatching');

  command(client, 'tick freeze');
  command(client, 'data merge block 0 81 3 {Age:7199}');
  command(client, 'data merge entity @e[type=iceandfire:dragon_egg,sort=nearest,limit=1,x=-8,y=81,z=3,distance=..3] {DragonAge:7201}');
  command(client, 'data merge entity @e[type=iceandfire:dragon_egg,sort=nearest,limit=1,x=8,y=81,z=3,distance=..3] {DragonAge:7201}');
  command(client, 'tick step 3');
  const water = await waitFor(() => JSON.stringify(mct(client, 'block', 'get', '0', '81', '3')),
    result => result.includes('minecraft:water'), 20000);
  if (!water.includes('minecraft:water')) throw new Error('Egg in Ice did not hatch on the client');
  command(client, 'data merge entity @e[type=iceandfire:fire_dragon,sort=nearest,limit=1,x=-8,y=81,z=3,distance=..4] {NoAI:1b}');
  command(client, 'data merge entity @e[type=iceandfire:lightning_dragon,sort=nearest,limit=1,x=8,y=81,z=3,distance=..4] {NoAI:1b}');
  command(client, 'data merge entity @e[type=iceandfire:ice_dragon,sort=nearest,limit=1,x=0,y=82,z=3,distance=..16] {NoAI:1b}');
  command(client, 'tp @e[type=iceandfire:ice_dragon,sort=nearest,limit=1,x=0,y=82,z=3,distance=..16] 0.5 82 3.5');
  await camera(client, { x: 0.5, y: 83, z: -15, yaw: 0, pitch: 8 });
  screenshot(client, 'AC-F015-004', 'fire-ice-lightning-hatching');
  command(client, 'tick unfreeze');
});
