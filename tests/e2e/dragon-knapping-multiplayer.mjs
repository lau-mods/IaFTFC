const { scenario, command, screenshot, waitFor, mct } = await import(process.env.HARNESS_E2E_LIB);

await scenario(async ({ clients }) => {
  const [a, b] = clients;
  if (!a || !b) throw new Error('Two clients are required');
  command(a, 'gamerule sendCommandFeedback false');
  command(a, 'gamemode survival');
  command(b, 'gamemode survival');
  command(a, 'clear @s');
  command(b, 'clear @s');

  const showInventory = (client, acId, label) => {
    mct(client, 'input', 'key', 'press', 'e');
    screenshot(client, acId, label);
    mct(client, 'gui', 'close');
  };
  const itemCount = (client, id) => mct(client, 'inventory', 'get').slots
    .filter(slot => slot.item.type === id)
    .reduce((count, slot) => count + slot.item.count, 0);
  const expect = (actual, expected, label) => {
    if (actual !== expected) throw new Error(`${label}: expected ${expected}, got ${actual}`);
  };
  const disconnect = () => {
    command(b, 'kick iaftfc_a knapping-check');
    mct(a, 'client', 'reconnect');
    mct(a, 'client', 'wait-ready', a, '--timeout', '60');
  };
  const cut = async (pattern, result, firstOnly = false) => {
    const layout = mct(a, 'gui', 'layout');
    const output = layout.slots.find(slot => slot.slot === 0);
    if (!output) throw new Error('Knapping output slot is absent');
    for (let attempt = 0; attempt < 3; attempt++) {
      for (let row = 0; row < 5; row++) for (let col = 0; col < 5; col++) {
        if (pattern[row][col] === '.') {
          mct(a, 'input', 'click', String(output.centerX - 116 + 16 * col), String(output.centerY - 34 + 16 * row));
          if (firstOnly) {
            const count = await waitFor(() => mct(a, 'inventory', 'held').item.count, count => count === 1, 5_000, 200);
            expect(count, 1, 'Bone first cut before disconnect');
            return;
          }
        }
      }
      if ((await waitFor(() => mct(a, 'gui', 'snapshot').slots[0].item?.type, type => type === result, 2_000, 200)) === result) return;
    }
    throw new Error(`Knapping result ${result} did not appear`);
  };

  command(a, 'item replace entity @s weapon.mainhand with iceandfire:dragonbone 2');
  mct(a, 'inventory', 'use');
  await cut(['.#...', '####.', '#####', '####.', '.#...'], null, true);
  screenshot(a, 'AC-F010-004', 'bone-after-first-cut-before-disconnect');
  disconnect();
  expect(mct(a, 'inventory', 'held').item.count, 1, 'Bone after reconnect');
  expect(itemCount(a, 'iaftfc:dragonbone/axe_head'), 0, 'Bone part after reconnect');
  expect(itemCount(b, 'iaftfc:dragonbone/axe_head'), 0, 'Bone part leaked to observer');
  showInventory(a, 'AC-F010-004', 'bone-after-reconnect');
  showInventory(b, 'AC-F010-004', 'observer-after-bone-disconnect');

  command(a, 'item replace entity @s weapon.mainhand with iceandfire:dragonscales_red 5');
  mct(a, 'inventory', 'use');
  await cut(['#####', '#...#', '#...#', '.....', '.....'], 'iceandfire:armor_red_helmet');
  screenshot(a, 'AC-F011-003', 'helmet-ready-before-disconnect');
  disconnect();
  expect(itemCount(a, 'iceandfire:dragonscales_red'), 0, 'Scales after reconnect');
  expect(itemCount(a, 'iceandfire:armor_red_helmet'), 1, 'Helmet after reconnect');
  expect(itemCount(b, 'iceandfire:armor_red_helmet'), 0, 'Helmet leaked to observer');
  showInventory(a, 'AC-F011-003', 'helmet-after-reconnect');
  showInventory(b, 'AC-F011-003', 'observer-after-scale-disconnect');
});
