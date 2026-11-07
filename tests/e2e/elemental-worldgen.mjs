const { scenario, command, camera, screenshot } = await import(process.env.HARNESS_E2E_LIB);

await scenario(async ({ clients }) => {
  const client = clients[0];
  command(client, 'time set day');
  command(client, 'weather clear');
  command(client, 'gamemode spectator');
  command(client, 'fill -20 79 -8 20 79 14 minecraft:stone');
  const variants = [
    ['fire', 'fire_lily', 'tfc:sand/yellow'],
    ['ice', 'frost_lily', 'tfc:sea_ice'],
    ['lightning', 'lightning_lily', 'tfc:grass/entisol'],
  ];
  for (const [index, [element, lily, ground]] of variants.entries()) {
    const x = (index - 1) * 9;
    command(client, `fill ${x - 3} 80 -2 ${x + 3} 80 2 ${ground}`);
    for (let offset = -2; offset <= 2; offset++) {
      command(client, `setblock ${x + offset} 81 0 iceandfire:${lily}`);
    }
    command(client, `summon iceandfire:${element}_dragon ${x} 80 11 {NoAI:1b,PersistenceRequired:1b,ModelDead:1b,DeadProg:20f,DeathStage:5,AgeTicks:1200000,Rotation:[0f,0f]}`);
  }
  await camera(client, { x: 0.5, y: 83, z: -18, yaw: 0, pitch: 10 });
  screenshot(client, 'AC-F017-001', 'three-lilies-on-tfc-ground');
  await camera(client, { x: 0.5, y: 85, z: -10, yaw: 0, pitch: 8 });
  screenshot(client, 'AC-F021-001', 'three-skeleton-variants-on-tfc-ground');
});
