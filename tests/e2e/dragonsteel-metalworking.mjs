const { scenario, command, camera, screenshot, mct } = await import(process.env.HARNESS_E2E_LIB);

await scenario(async ({ clients }) => {
  const client = clients[0];
  command(client, 'gamerule sendCommandFeedback false');
  command(client, 'time set noon');
  command(client, 'weather clear');
  command(client, 'setblock 0 -60 3 minecraft:crafting_table');
  for (const metal of ['dragonsteel_fire', 'dragonsteel_ice', 'dragonsteel_lightning']) {
    command(client, `give @s iaftfc:metal/double_ingot/${metal} 7`);
  }
  await camera(client, { x: 0.5, y: -59, z: 1.5, yaw: 180, pitch: 10 });
  await mct(client, 'block', 'interact', '0', '-60', '3');
  const metal = 'iaftfc:metal/double_ingot/dragonsteel_fire';
  await mct(client, 'craft', '--recipe', JSON.stringify([[metal, metal, metal], [null, metal, null], [metal, metal, metal]]));
  await mct(client, 'gui', 'close');
  await mct(client, 'input', 'key', 'press', 'e');
  screenshot(client, 'AC-F009-001', 'crafted-tier-seven-anvil');
  await mct(client, 'gui', 'close');
});
