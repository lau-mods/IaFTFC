const { scenario, command, camera, screenshot, mct } = await import(process.env.HARNESS_E2E_LIB);

await scenario(async ({ clients }) => {
  const client = clients[0];
  await command(client, 'gamerule sendCommandFeedback false');
  await command(client, 'time set noon');
  await command(client, 'weather clear');
  await command(client, 'gamemode survival');
  const items = [
    'iceandfire:pixie_dust',
    'iceandfire:fire_dragon_flesh',
    'iceandfire:dragonegg_red',
    'iceandfire:dragon_skull_fire',
    'iaftfc:dragonarmor_steel_head',
    'iaftfc:dragonbone/axe_head',
    'iaftfc:metal/block/dragonsteel_fire_slab',
  ];
  for (let slot = 0; slot < items.length; slot++) {
    await command(client, `item replace entity @s hotbar.${slot} with ${items[slot]}`);
  }
  await camera(client, { x: 0.5, y: -59, z: 0.5, yaw: 0, pitch: 0 });
  await mct(client, 'input', 'key', 'press', 'e');
  const layout = await mct(client, 'gui', 'layout');
  for (let slot = 0; slot < items.length; slot++) {
    const position = layout.slots.find(entry => entry.slot === 36 + slot);
    await mct(client, 'input', 'mouse-move', String(position.centerX), String(position.centerY));
    await new Promise(resolve => setTimeout(resolve, 400));
    await screenshot(client, 'AC-F013-002', `${items[slot].split(':')[1].replaceAll('/', '-')}-size`);
  }
  await mct(client, 'gui', 'close');

  await command(client, 'setblock 0 -60 2 tfc:ceramic/large_vessel');
  await mct(client, 'block', 'interact', '0', '-60', '2');
  const vessel = await mct(client, 'gui', 'snapshot');
  const dust = vessel.slots.find(entry => entry.item?.type === 'iceandfire:pixie_dust');
  const egg = vessel.slots.find(entry => entry.item?.type === 'iceandfire:dragonegg_red');
  if (!dust || !egg) throw new Error('The vessel inventory is missing the prepared items');
  await mct(client, 'gui', 'click', String(dust.slot), '--button', 'shift-left');
  await mct(client, 'gui', 'click', String(egg.slot), '--button', 'shift-left');
  await screenshot(client, 'AC-F013-002', 'tfc-vessel-small-versus-egg');
  await mct(client, 'gui', 'close');
});
