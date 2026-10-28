const { scenario, command, camera, screenshot, mct } = await import(process.env.HARNESS_E2E_LIB);

await scenario(async ({ clients }) => {
  const client = clients[0];
  command(client, 'gamerule sendCommandFeedback false');
  command(client, 'time set noon');
  command(client, 'weather clear');
  const metals = ['dragonsteel_fire', 'dragonsteel_ice', 'dragonsteel_lightning'];
  for (let row = 0; row < metals.length; row++) {
    const metal = metals[row];
    const z = 4 + row * 4;
    const blocks = [
      `metal/block/${metal}`,
      `metal/block/${metal}_slab[type=bottom]`,
      `metal/block/${metal}_slab[type=top]`,
      `metal/block/${metal}_slab[type=double]`,
      `metal/block/${metal}_stairs[facing=south,half=bottom,shape=straight]`,
      `metal/block/${metal}_stairs[facing=east,half=top,shape=straight]`,
      `metal/block/${metal}_stairs[facing=south,half=bottom,shape=inner_left]`,
      `metal/block/${metal}_stairs[facing=south,half=bottom,shape=outer_right]`,
      `metal/anvil/${metal}[facing=south]`,
    ];
    for (let column = 0; column < blocks.length; column++) {
      command(client, `setblock ${-8 + column * 2} -60 ${z} iaftfc:${blocks[column]}`);
    }
  }
  command(client, 'gamemode spectator');
  await camera(client, { x: 0.5, y: -45, z: 31.5, yaw: 180, pitch: 30 }, 5_000);
  mct(client, 'chat', 'clear');
  screenshot(client, 'AC-F007-002', 'three-metals-blocks-slabs-stairs-anvils');
  await camera(client, { x: 0.5, y: -60, z: 31.5, yaw: 180, pitch: 0 });
  command(client, 'gamemode survival');
  for (let slot = 0; slot < metals.length; slot++) {
    command(client, `item replace entity @s hotbar.${slot} with tfc:ceramic/ingot_mold[tfc:fluid={id:"tfc:metal/${metals[slot]}",amount:100}]`);
  }
  mct(client, 'input', 'key', 'press', 'e');
  const layout = mct(client, 'gui', 'layout');
  await new Promise(resolve => setTimeout(resolve, 10_000));
  for (let slot = 0; slot < metals.length; slot++) {
    const position = layout.slots.find(entry => entry.slot === 36 + slot);
    mct(client, 'input', 'mouse-move', String(position.centerX), String(position.centerY));
    await new Promise(resolve => setTimeout(resolve, 500));
    screenshot(client, 'AC-F007-003', `${metals[slot]}-100mb-ingot-mold-tooltip`);
  }
  mct(client, 'gui', 'close');
});
