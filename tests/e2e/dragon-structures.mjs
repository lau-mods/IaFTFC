const { scenario, command, camera, screenshot, waitFor, mct } = await import(process.env.HARNESS_E2E_LIB);

await scenario(async ({ clients }) => {
  const client = clients[0];
  command(client, 'gamemode spectator');
  command(client, 'scoreboard objectives add iaftfcPlace dummy');

  function block(x, y, z) {
    return JSON.stringify(mct(client, 'block', 'get', String(x), String(y), String(z)));
  }

  async function view(position, settleMs = 3000) {
    await camera(client, position, settleMs);
    const current = await waitFor(() => mct(client, 'position', 'get'), value =>
      Math.abs(value.x - position.x) < 1 && Math.abs(value.y - position.y) < 1
        && Math.abs(value.z - position.z) < 1, 30000);
    if (Math.abs(current.x - position.x) >= 1 || Math.abs(current.y - position.y) >= 1
      || Math.abs(current.z - position.z) >= 1) throw new Error('Camera teleport did not finish');
  }

  async function completed(x, z, action) {
    const marker = [x + 35, 40, z + 35];
    command(client, `setblock ${marker.join(' ')} minecraft:air`);
    const cleared = await waitFor(() => block(...marker), value => value.includes('minecraft:air'), 30000);
    if (!cleared.includes('minecraft:air')) throw new Error('Scene marker did not clear');
    action();
    command(client, `setblock ${marker.join(' ')} minecraft:obsidian`);
    const result = await waitFor(() => block(...marker), value => value.includes('minecraft:obsidian'), 120000);
    if (!result.includes('minecraft:obsidian')) throw new Error(`Scene command did not finish near ${x}, ${z}`);
  }

  async function place(element, kind, x, y, z) {
    for (const [dx, dz] of [[0, 0], [16, 0], [0, 16], [-16, 0], [0, -16],
      [16, 16], [-16, 16], [16, -16], [-16, -16]]) {
      const marker = [x + 35, 41, z + 35];
      command(client, `setblock ${marker.join(' ')} minecraft:air`);
      const cleared = await waitFor(() => block(...marker), value => value.includes('minecraft:air'), 30000);
      if (!cleared.includes('minecraft:air')) throw new Error('Placement marker did not clear');
      command(client, `execute store success score #place iaftfcPlace run place structure iceandfire:${element}_dragon_${kind} ${x + dx} ${y} ${z + dz}`);
      command(client, `execute if score #place iaftfcPlace matches 1 run setblock ${marker.join(' ')} minecraft:obsidian`);
      command(client, `execute if score #place iaftfcPlace matches 0 run setblock ${marker.join(' ')} minecraft:redstone_block`);
      const result = await waitFor(() => block(...marker), value =>
        value.includes('minecraft:obsidian') || value.includes('minecraft:redstone_block'), 120000);
      if (result.includes('minecraft:obsidian')) return { x: x + dx, z: z + dz };
      if (!result.includes('minecraft:redstone_block')) throw new Error(`Placement did not finish: ${element} ${kind}`);
    }
    throw new Error(`Could not place ${element} dragon ${kind} near ${x}, ${z}`);
  }

  for (const [index, element] of ['fire', 'ice', 'lightning'].entries()) {
    const x = 3000 + index * 80;
    await view({ x, y: -30, z: 2960, yaw: 0, pitch: 20 });
    for (const [y, left, right, material] of [
      [-60, -20, 20, 'tfc:rock/raw/basalt'],
      [-59, -20, 20, 'tfc:dirt/entisol'],
      [-58, -20, -7, 'tfc:grass/entisol'],
      [-58, -6, 6, 'tfc:rock/raw/basalt'],
      [-58, 7, 20, 'tfc:rock/gravel/basalt'],
    ]) {
      await completed(x, 3000, () => command(client,
        `fill ${x + left} ${y} 2980 ${x + right} ${y} 3020 ${material}`));
    }
    await completed(x, 3000, () => command(client, `setblock ${x - 12} -57 3000 tfc:plant/hibiscus`));
    await completed(x, 3000, () => command(client, `setblock ${x + 12} -57 3000 tfc:wood/log/oak`));
    const placed = await place(element, 'roost', x, -58, 3000);
    await view({ x: placed.x, y: -35, z: placed.z - 34, yaw: 0, pitch: 24 }, 5000);
    screenshot(client, 'AC-F002-003', `${element}-roost`);
    screenshot(client, 'AC-F003-001', `${element}-terrain`);
    screenshot(client, 'AC-F003-002', `${element}-decoration`);
  }

  for (const [index, element] of ['fire', 'ice', 'lightning'].entries()) {
    const x = 3000 + index * 80;
    await view({ x, y: -12, z: 3560, yaw: 0, pitch: 0 });
    for (const [bottom, top] of [[-30, -16], [-15, -1]]) {
      await completed(x, 3600, () => command(client,
        `fill ${x - 18} ${bottom} 3582 ${x + 18} ${top} 3618 tfc:rock/raw/basalt`));
    }
    const placed = await place(element, 'cave', x, -35, 3600);
    const air = [];
    for (let y = -24; y <= -1; y++) {
      if (block(placed.x, y, placed.z).includes('minecraft:air')) air.push(y);
    }
    const interiorY = air[Math.floor(air.length / 2)];
    if (interiorY === undefined) throw new Error(`No cave cavity found for ${element}`);
    await view({ x: placed.x + 0.5, y: interiorY + 1, z: placed.z + 0.5, yaw: 0, pitch: 10 }, 5000);
    screenshot(client, 'AC-F002-003', `${element}-cave`);
    screenshot(client, 'AC-F004-003', `${element}-cave-body`);
  }
});
