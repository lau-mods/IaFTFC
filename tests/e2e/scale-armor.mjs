const { scenario, command, camera, screenshot } = await import(process.env.HARNESS_E2E_LIB);

await scenario(async ({ clients }) => {
  const client = clients[0];
  command(client, 'time set day');
  command(client, 'weather clear');
  command(client, 'gamemode spectator');
  command(client, 'fill -14 79 0 14 79 7 minecraft:stone');
  const colors = [
    'red', 'green', 'bronze', 'gray', 'blue', 'white', 'sapphire', 'silver',
    'electric', 'amethyst', 'copper', 'black'
  ];
  for (const [index, color] of colors.entries()) {
    const x = (index - 5.5) * 2.2;
    const armor = ['boots', 'leggings', 'chestplate', 'helmet']
      .map(part => `{id:"iceandfire:armor_${color}_${part}",count:1}`);
    command(client, `summon minecraft:armor_stand ${x} 80 3 {NoGravity:1b,ShowArms:1b,ArmorItems:[${armor.join(',')}]}`);
  }
  await camera(client, { x: 0, y: 82.5, z: -14, yaw: 0, pitch: 5 });
  screenshot(client, 'AC-F013-002', 'twelve-scale-armor-colors');
});
