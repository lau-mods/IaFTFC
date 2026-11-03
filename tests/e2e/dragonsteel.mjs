const { scenario, command, camera, screenshot, waitFor, mct } = await import(process.env.HARNESS_E2E_LIB);

await scenario(async ({ clients }) => {
  const client = clients[0];
  command(client, 'time set day');
  command(client, 'weather clear');
  command(client, 'gamemode spectator');
  command(client, 'fill -5 79 0 5 79 8 minecraft:stone');
  for (const [index, metal] of ['dragonsteel_fire', 'dragonsteel_ice', 'dragonsteel_lightning'].entries()) {
    const x = (index - 1) * 3;
    command(client, `setblock ${x} 80 3 iaftfc:metal/block/${metal}`);
    command(client, `setblock ${x} 80 5 iaftfc:metal/anvil/${metal}`);
    for (const [dx, dz] of [[-1, 0], [1, 0], [0, -1], [0, 1]]) {
      command(client, `setblock ${x + dx} 80 ${7 + dz} minecraft:glass`);
    }
    command(client, `setblock ${x} 80 7 iaftfc:fluid/metal/${metal}`);
    command(client, `summon minecraft:item_display ${x} 81 3 {item:{id:"iaftfc:metal/ingot/${metal}",count:1},transformation:{scale:[1.5f,1.5f,1.5f]}}`);
  }
  const iceBlock = await waitFor(
    () => JSON.stringify(mct(client, 'block', 'get', '0', '80', '3')),
    value => value.includes('iaftfc:metal/block/dragonsteel_ice')
  );
  if (!iceBlock.includes('iaftfc:metal/block/dragonsteel_ice')) throw new Error('Dragonsteel scene did not load');
  await camera(client, { x: 0.5, y: 82, z: 3, yaw: 0, pitch: 20 });
  await camera(client, { x: 0.5, y: 82, z: -3.5, yaw: 0, pitch: 16 }, 8000);
  const position = await waitFor(() => mct(client, 'position', 'get'), value =>
    Math.abs(value.x - 0.5) < 1 && Math.abs(value.y - 82) < 1 && Math.abs(value.z + 3.5) < 1);
  if (Math.abs(position.x - 0.5) >= 1 || Math.abs(position.y - 82) >= 1 || Math.abs(position.z + 3.5) >= 1) {
    throw new Error('Dragonsteel camera did not reach the scene');
  }
  screenshot(client, 'AC-F008-001', 'three-metals-and-anvils');
  await camera(client, { x: 0.5, y: 82, z: 3, yaw: 0, pitch: 20 });
  screenshot(client, 'AC-F008-001', 'molten-fluid-sources');
  command(client, 'gamemode survival');
  const size = mct(client, 'screen', 'size');
  const width = size.width ?? size.w;
  const height = size.height ?? size.h;
  const captureHeat = (part, metal, temperature, label) => {
    command(client, `item replace entity @s hotbar.0 with iaftfc:metal/${part}/${metal}[tfc:heat={capacity:-1.0f,temperature:${temperature}.0f,tick:-1L}]`);
    mct(client, 'input', 'key', 'press', 'e');
    mct(client, 'gui', 'wait-open', '--timeout', '15');
    mct(client, 'input', 'mouse-move', String(Math.floor(width / 2 - 72)), String(Math.floor(height / 2 + 72)));
    screenshot(client, 'AC-F008-003', label);
    mct(client, 'gui', 'close');
  };
  for (const temperature of [1199, 1200, 1599, 1600]) {
    captureHeat('ingot', 'dragonsteel_fire', temperature, `ingot-${temperature}`);
  }
  for (const [part, metal] of [
    ['sheet', 'dragonsteel_fire'], ['rod', 'dragonsteel_ice'],
    ['pickaxe_head', 'dragonsteel_lightning'], ['unfinished_helmet', 'dragonsteel_fire']
  ]) {
    captureHeat(part, metal, 1700, `${part}-heated`);
  }
  command(client, 'item replace entity @s weapon.mainhand with minecraft:air');
  await camera(client, { x: -2.5, y: 81.6, z: 3, yaw: 0, pitch: 15 });
  mct(client, 'block', 'interact', '-3', '80', '5');
  mct(client, 'gui', 'wait-open', '--timeout', '15');
  screenshot(client, 'AC-F010-001', 'dragonsteel-anvil-ui');
});
