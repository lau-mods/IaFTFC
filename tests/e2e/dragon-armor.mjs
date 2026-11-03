const { scenario, command, camera, screenshot } = await import(process.env.HARNESS_E2E_LIB);

await scenario(async ({ clients }) => {
  const client = clients[0];
  command(client, 'time set day');
  command(client, 'weather clear');
  command(client, 'gamemode spectator');
  command(client, 'fill -27 79 -17 27 79 20 minecraft:stone');
  const metals = ['steel', 'black_steel', 'red_steel', 'blue_steel'];
  const parts = ['head', 'neck', 'body', 'tail'];
  for (const [metalIndex, metal] of metals.entries()) {
    for (const [partIndex, part] of parts.entries()) {
      const x = (metalIndex - 1.5) * 7;
      const y = 80.5 + partIndex * 1.5;
      command(client, `summon minecraft:item_display ${x} ${y} -16 {item:{id:"iaftfc:dragonarmor_${metal}_${part}",count:1},billboard:"center",transformation:{scale:[1.8f,1.8f,1.8f]}}`);
    }
    const x = (metalIndex - 1.5) * 14;
    command(client, `summon iceandfire:fire_dragon ${x} 80 11 {NoAI:1b,PersistenceRequired:1b,AgeTicks:1200000,Variant:"green",Rotation:[0f,0f]}`);
    const selector = `@e[type=iceandfire:fire_dragon,sort=nearest,limit=1,x=${x},y=80,z=11,distance=..4]`;
    const armor = parts.map(part => `{id:"iaftfc:dragonarmor_${metal}_${part}",count:1}`);
    command(client, `data merge entity ${selector} {Items:[{},${armor.join(',')}]}`);
  }
  await camera(client, { x: 0.5, y: 83, z: -36, yaw: 0, pitch: 0 });
  screenshot(client, 'AC-F014-004', 'sixteen-item-sprites');
  for (const [metalIndex, metal] of metals.entries()) {
    const x = (metalIndex - 1.5) * 14;
    await camera(client, { x: x + 6, y: 84, z: 19, yaw: 145, pitch: 12 });
    screenshot(client, 'AC-F014-004', `${metal}-front-overlay`);
    await camera(client, { x: x - 6, y: 83, z: 19, yaw: 215, pitch: 8 });
    screenshot(client, 'AC-F014-001', `${metal}-opposite-side`);
  }
});
