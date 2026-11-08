const { scenario, command, camera, screenshot, mct, waitFor } = await import(process.env.HARNESS_E2E_LIB);

await scenario(async ({ clients }) => {
  const client = clients[0];
  command(client, 'gamemode creative');
  command(client, 'time set noon');
  command(client, 'weather clear');
  await camera(client, { x: 0.5, y: -53, z: -15, yaw: 0, pitch: 30 });
  command(client, 'fill -9 -61 -2 9 -61 6 minecraft:smooth_stone');

  const metals = ['dragonsteel_fire', 'dragonsteel_ice', 'dragonsteel_lightning'];
  const placements = [
    [-7, 'block', ''],
    [-5, 'block', '_slab[type=bottom]'],
    [-3, 'block', '_slab[type=top]'],
    [-1, 'block', '_stairs[facing=north,half=bottom,shape=straight]'],
    [1, 'block', '_stairs[facing=north,half=bottom,shape=inner_left]'],
    [3, 'block', '_stairs[facing=north,half=bottom,shape=outer_left]'],
    [5, 'anvil', ''],
  ];
  for (let row = 0; row < metals.length; row++) {
    const z = row * 2;
    command(client, `setblock 7 -60 ${z} iceandfire:${metals[row]}_block replace`);
    for (const [x, family, suffix] of placements) {
      const path = family === 'anvil' ? `metal/anvil/${metals[row]}` : `metal/block/${metals[row]}${suffix}`;
      command(client, `setblock ${x} -60 ${z} iaftfc:${path} replace`);
      await waitFor(
        () => mct(client, 'block', 'get', String(x), '-60', String(z)),
        state => state.type === `iaftfc:${path.split('[')[0]}`,
      );
    }
  }

  await camera(client, { x: 0.5, y: -53, z: -15, yaw: 0, pitch: 30 }, 6_000);
  screenshot(client, 'AC-F010-003', 'plated-blocks-slabs-stairs-and-anvils');
});
