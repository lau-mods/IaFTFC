const { scenario, command, camera, screenshot, mct } = await import(process.env.HARNESS_E2E_LIB);

await scenario(async ({ clients }) => {
  const client = clients[0];
  command(client, 'gamerule sendCommandFeedback false');
  command(client, 'time set noon');
  command(client, 'weather clear');
  command(client, 'setblock 0 -60 3 minecraft:crafting_table');
  command(client, 'give @s iaftfc:metal/axe_head/dragonsteel_fire');
  command(client, 'give @s tfc:wood/twig/oak');
  await camera(client, { x: 0.5, y: -59, z: 1.5, yaw: 180, pitch: 10 });
  await mct(client, 'block', 'interact', '0', '-60', '3');
  await mct(client, 'craft', '--recipe', JSON.stringify([
    ['iaftfc:metal/axe_head/dragonsteel_fire', null, null],
    ['tfc:wood/twig/oak', null, null],
    [null, null, null],
  ]));
  await mct(client, 'gui', 'close');
  await mct(client, 'input', 'key', 'press', 'e');
  screenshot(client, 'AC-F009-004', 'crafted-dragonsteel-axe');
  await mct(client, 'gui', 'close');
});
