const { scenario, command, camera, screenshot } = await import(process.env.HARNESS_E2E_LIB);

await scenario(async ({ clients }) => {
  const client = clients[0];
  command(client, 'gamemode spectator');
  command(client, 'time set noon');
  command(client, 'weather clear');
  command(client, 'kill @e[type=iceandfire:fire_dragon]');
  command(client, 'fill -34 79 -8 34 79 22 minecraft:smooth_stone');

  const armors = [
    ['steel', 'iaftfc'],
    ['black_steel', 'iaftfc'],
    ['red_steel', 'iaftfc'],
    ['blue_steel', 'iaftfc'],
  ];
  for (let index = 0; index < armors.length; index++) {
    const [metal, namespace] = armors[index];
    const x = -21 + index * 14;
    const items = ['head', 'neck', 'body', 'tail']
      .map(part => `{id:"${namespace}:dragonarmor_${metal}_${part}",count:1}`);
    command(client, `summon iceandfire:fire_dragon ${x} 80 3 {NoAI:1b,Silent:1b,PersistenceRequired:1b,AgeTicks:1200000,Variant:"green",Rotation:[90f,0f],Items:[{},${items.join(',')}]}`);
  }

  const reference = ['head', 'neck', 'body', 'tail']
    .map(part => `{id:"iceandfire:dragonarmor_dragon_steel_fire_${part}",count:1}`);
  command(client, `summon iceandfire:fire_dragon 0 80 17 {NoAI:1b,Silent:1b,PersistenceRequired:1b,AgeTicks:1200000,Variant:"green",Rotation:[90f,0f],Items:[{},${reference.join(',')}]}`);

  await camera(client, { x: 0.5, y: 87, z: -31, yaw: 0, pitch: 8 }, 6000);
  screenshot(client, 'AC-F012-002', 'four-tfc-metals-side-profile');
  await camera(client, { x: 15, y: 86, z: 31, yaw: 150, pitch: 10 }, 6000);
  screenshot(client, 'AC-F012-002', 'tail-side-and-dragonsteel-reference');
});
