const { scenario, command, camera, screenshot, mct } = await import(process.env.HARNESS_E2E_LIB);

await scenario(async ({ clients }) => {
  const client = clients[0];
  const materials = ['steel', 'black_steel', 'red_steel', 'blue_steel'];
  const parts = ['head', 'neck', 'body', 'tail'];
  command(client, 'gamerule sendCommandFeedback false');
  command(client, 'time set noon');
  command(client, 'weather clear');
  command(client, 'gamemode spectator');
  mct(client, 'input', 'key', 'press', 'f1');

  for (const [kindIndex, kind] of ['fire', 'ice', 'lightning'].entries()) {
    const z = kindIndex * 70 + 8;
    const variant = { fire: 'red', ice: 'blue', lightning: 'electric' }[kind];
    await camera(client, { x: 0, y: -48, z: z + 16, yaw: 180, pitch: 25 });
    for (let index = 0; index < materials.length; index++) {
      const tag = `iaftfc_armor_${kind}_${index}`;
      command(client, `summon iceandfire:${kind}_dragon ${-45 + index * 30} -60 ${z} {AgeTicks:1200000,Rotation:[0f,0f],Variant:"${variant}",TamedDragon:1b,NoAI:1b,PersistenceRequired:1b,Tags:["${tag}"]}`);
    }
    await camera(client, { x: -37, y: -53, z, yaw: 90, pitch: 16 });
    screenshot(client, 'AC-F012-002', `${kind}-unequipped`);
    for (let index = 0; index < materials.length; index++) {
      const target = `@e[tag=iaftfc_armor_${kind}_${index},limit=1]`;
      const stacks = ['{}', ...parts.map(part => `{id:"iaftfc:dragonarmor_${materials[index]}_${part}",count:1}`)];
      for (let part = 0; part < parts.length; part++) {
        const equipped = ['{}', '{}', '{}', '{}', '{}'];
        equipped[part + 1] = stacks[part + 1];
        command(client, `data merge entity ${target} {Items:[${equipped.join(',')}]}`);
        if (index === 0) {
          await camera(client, { x: -37, y: -53, z, yaw: 90, pitch: 16 });
          screenshot(client, 'AC-F012-002', `${kind}-${parts[part]}-only`);
        }
        command(client, `data merge entity ${target} {Items:[{},{},{},{},{}]}`);
      }
      const wrongSlot = ['{}', '{}', stacks[1], '{}', '{}'];
      command(client, `data merge entity ${target} {Items:[${wrongSlot.join(',')}]}`);
      if (index === 0) {
        await camera(client, { x: -37, y: -53, z, yaw: 90, pitch: 16 });
        screenshot(client, 'AC-F012-002', `${kind}-head-in-neck-slot`);
      }
      command(client, `kill ${target}`);
      command(client, `summon iceandfire:${kind}_dragon ${-45 + index * 30} -60 ${z} {AgeTicks:1200000,Rotation:[0f,0f],Variant:"${variant}",TamedDragon:1b,NoAI:1b,PersistenceRequired:1b,Tags:["iaftfc_armor_${kind}_${index}"],Items:[${stacks.join(',')}]}`);
      await camera(client, { x: -37 + index * 30, y: -53, z, yaw: 90, pitch: 16 });
      screenshot(client, 'AC-F012-002', `${kind}-${materials[index]}-all-parts`);
    }
  }
});
