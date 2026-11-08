const { scenario, command, camera, screenshot, mct, waitFor } = await import(process.env.HARNESS_E2E_LIB);

await scenario(async ({ clients }) => {
  const client = clients[0];
  command(client, 'gamemode creative');
  command(client, 'time set dayLength disabled');
  command(client, 'fill -5 7 -3 5 7 3 minecraft:bedrock');
  command(client, 'fill -5 8 -3 5 8 3 tfc:dirt/entisol');
  command(client, 'fill -1 11 -6 1 11 -4 minecraft:bedrock');
  const lilies = [[-2, 'fire_lily'], [0, 'frost_lily'], [2, 'lightning_lily']];
  for (let slot = 0; slot < lilies.length; slot++) {
    command(client, `item replace entity @s hotbar.${slot} with iaftfc:plant/${lilies[slot][1]}`);
  }
  for (const [x, id] of lilies) {
    command(client, `setblock ${x} 9 0 iaftfc:plant/${id} replace`);
  }
  await camera(client, { x: 0.5, y: 12, z: -5, yaw: 0, pitch: 40 });
  const cameraFloor = await mct(client, 'block', 'get', '0', '11', '-5');
  if (cameraFloor.type !== 'minecraft:bedrock') {
    throw new Error(`Expected camera floor at 0 11 -5: ${JSON.stringify(cameraFloor)}`);
  }
  for (let month = 0; month < 12; month++) {
    if (month) {
      command(client, 'time add months 1');
    }
    command(client, 'time set noon');
    for (const [x, id] of lilies) {
      const ground = await mct(client, 'block', 'get', String(x), '8', '0');
      if (ground.type !== 'tfc:dirt/entisol') {
        throw new Error(`Expected TFC dirt at ${x} 8 0: ${JSON.stringify(ground)}`);
      }
      const block = await waitFor(
        () => mct(client, 'block', 'get', String(x), '9', '0'),
        state => state.type === `iaftfc:plant/${id}`,
      );
      if (block.type !== `iaftfc:plant/${id}`) {
        throw new Error(`Expected iaftfc:plant/${id} at ${x} 9 0: ${JSON.stringify(block)}`);
      }
    }
    await camera(client, { x: 0.5, y: 12, z: -5, yaw: 0, pitch: 40 });
    screenshot(client, 'AC-F005-004', `month-${month}`);
  }
});
