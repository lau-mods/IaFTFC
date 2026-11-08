const { scenario, command, screenshot, mct } = await import(process.env.HARNESS_E2E_LIB);

await scenario(async ({ clients }) => {
  const client = clients[0];
  command(client, 'gamemode survival');
  command(client, 'item replace entity @s hotbar.0 with iceandfire:silver_ingot');
  command(client, 'item replace entity @s hotbar.1 with iceandfire:dragonsteel_fire_sword');
  command(client, 'item replace entity @s hotbar.2 with tfc:ore/small_native_silver');

  await mct(client, 'input', 'key', 'press', 'e');
  await mct(client, 'gui', 'wait-open', '--timeout', '15');
  const layout = await mct(client, 'gui', 'layout');
  const hover = async (slot, label) => {
    const bounds = layout.slots.find(entry => entry.slot === 36 + slot);
    if (!bounds) throw new Error(`Missing hotbar slot ${slot}`);
    await mct(client, 'input', 'mouse-move', String(bounds.centerX), String(bounds.centerY));
    await new Promise(resolve => setTimeout(resolve, 500));
    screenshot(client, 'AC-F014-002', label);
  };
  await hover(0, 'disabled-silver-ingot');
  await hover(1, 'enabled-dragonsteel-sword');
  await hover(2, 'enabled-pile-output');
});
