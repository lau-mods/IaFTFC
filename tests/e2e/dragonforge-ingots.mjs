const { scenario, command, camera, screenshot, mct } = await import(process.env.HARNESS_E2E_LIB);

await scenario(async ({ clients }) => {
  const client = clients[0];
  command(client, 'gamerule sendCommandFeedback false');
  command(client, 'time set noon');
  command(client, 'weather clear');
  const elements = ['fire', 'ice', 'lightning'];
  const steels = ['red_steel', 'blue_steel', 'black_steel'];
  for (let index = 0; index < elements.length; index++) {
    const element = elements[index];
    const x = -6 + index * 6;
    const z = 5;
    for (const y of [-61, -59]) {
      command(client, `setblock ${x} ${y} ${z} iceandfire:dragonforge_${element}_brick`);
      for (const offset of [-1, 1]) {
        command(client, `setblock ${x + offset} ${y} ${z} iceandfire:dragonforge_${element}_brick`);
        command(client, `setblock ${x} ${y} ${z + offset} iceandfire:dragonforge_${element}_brick`);
        for (const corner of [-1, 1]) command(client, `setblock ${x + offset} ${y} ${z + corner} iceandfire:dragon_bone_block`);
      }
    }
    for (const offset of [-1, 1]) {
      for (const corner of [-1, 1]) command(client, `setblock ${x + offset} -60 ${z + corner} iceandfire:dragonforge_${element}_brick`);
      command(client, `setblock ${x} -60 ${z + offset} iceandfire:dragonforge_${element}_brick`);
    }
    command(client, `setblock ${x - 1} -60 ${z} iceandfire:dragonforge_${element}_brick`);
    command(client, `setblock ${x + 1} -60 ${z} iceandfire:dragonforge_${element}_input`);
    command(client, `setblock ${x} -60 ${z} iceandfire:dragonforge_${element}_core_disabled`);
  }
  await new Promise(resolve => setTimeout(resolve, 1000));
  for (let index = 0; index < elements.length; index++) {
    const element = elements[index];
    const x = -6 + index * 6;
    const z = 5;
    command(client, `item replace block ${x} -60 ${z} container.0 with tfc:metal/ingot/${steels[index]}`);
    command(client, `item replace block ${x} -60 ${z} container.1 with iceandfire:${element}_dragon_blood`);
    command(client, `data merge block ${x} -60 ${z} {CookTime:1000}`);
  }
  await new Promise(resolve => setTimeout(resolve, 2000));
  command(client, 'gamemode survival');
  for (let index = 0; index < elements.length; index++) {
    const x = -6 + index * 6;
    command(client, `item replace entity @s hotbar.${index} from block ${x} -60 5 container.2`);
  }
  await camera(client, { x: 0.5, y: -59, z: 13.5, yaw: 180, pitch: 0 });
  mct(client, 'chat', 'clear');
  mct(client, 'input', 'key', 'press', 'e');
  const layout = mct(client, 'gui', 'layout');
  for (let index = 0; index < elements.length; index++) {
    const slot = layout.slots.find(entry => entry.slot === 36 + index);
    mct(client, 'input', 'mouse-move', String(slot.centerX), String(slot.centerY));
    await new Promise(resolve => setTimeout(resolve, 400));
    screenshot(client, 'AC-F008-001', `${elements[index]}-forge-output`);
  }
  mct(client, 'gui', 'close');
});
