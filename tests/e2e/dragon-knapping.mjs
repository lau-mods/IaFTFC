const { scenario, command, screenshot, waitFor, mct } = await import(process.env.HARNESS_E2E_LIB);

await scenario(async ({ clients }) => {
  const client = clients[0];
  command(client, 'gamerule sendCommandFeedback false');
  command(client, 'gamemode survival');
  command(client, 'clear @s');

  const hold = (id, count) => command(client, `item replace entity @s weapon.mainhand with ${id} ${count}`);
  const open = () => mct(client, 'inventory', 'use');
  const close = () => mct(client, 'gui', 'close');
  const heldCount = () => mct(client, 'inventory', 'held').item.count;
  const itemCount = id => mct(client, 'inventory', 'get').slots
    .filter(slot => slot.item.type === id)
    .reduce((count, slot) => count + slot.item.count, 0);
  const expect = (actual, expected, label) => {
    if (actual !== expected) throw new Error(`${label}: expected ${expected}, got ${actual}`);
  };
  const inventory = (acId, label) => {
    mct(client, 'input', 'key', 'press', 'e');
    screenshot(client, acId, label);
    close();
  };
  const cut = async (pattern, result, firstOnly = false) => {
    const layout = mct(client, 'gui', 'layout');
    const output = layout.slots.find(slot => slot.slot === 0);
    if (!output) throw new Error('Knapping output slot is absent');
    for (let attempt = 0; attempt < 3; attempt++) {
      for (let row = 0; row < 5; row++) for (let col = 0; col < 5; col++) {
        if (pattern[row][col] !== '.') continue;
        // TFC places the 5x5 buttons 116 px left and 34 px above the output slot.
        mct(client, 'input', 'click', String(output.centerX - 116 + 16 * col), String(output.centerY - 34 + 16 * row));
        if (firstOnly) {
          await new Promise(resolve => setTimeout(resolve, 500));
          return;
        }
      }
      if (result && (await waitFor(() => mct(client, 'gui', 'snapshot').slots[0].item?.type, type => type === result, 2_000, 200)) === result) return;
    }
    throw new Error(`Knapping result ${result} did not appear`);
  };

  hold('iceandfire:dragonbone', 1);
  open();
  screenshot(client, 'AC-F010-001', 'one-bone-cannot-open');
  hold('iceandfire:dragonbone', 2);
  open();
  screenshot(client, 'AC-F010-001', 'two-bones-open');
  close();
  expect(heldCount(), 2, 'Bone open and close');
  inventory('AC-F010-001', 'two-bones-unchanged-after-close');
  open();
  await cut(['.#...', '####.', '#####', '####.', '.#...'], null, true);
  expect(await waitFor(heldCount, count => count === 1, 5_000, 200), 1, 'Bone first cut before close');
  screenshot(client, 'AC-F010-001', 'first-cut');
  close();
  expect(heldCount(), 1, 'Bone first cut');
  inventory('AC-F010-001', 'one-bone-after-cut');
  hold('iceandfire:dragonbone', 3);
  open();
  await cut(['.....', '#####', '...##', '.....', '.....'], 'iaftfc:dragonbone/hoe_head');
  screenshot(client, 'AC-F010-001', 'completed-bone-hoe');
  mct(client, 'gui', 'click', '0', '--button', 'shift-left');
  close();
  expect(heldCount(), 2, 'Completed bone consumption');
  expect(itemCount('iaftfc:dragonbone/hoe_head'), 1, 'Completed bone output');
  inventory('AC-F010-001', 'two-bones-and-one-hoe-head');

  hold('iceandfire:dragonscales_red', 4);
  open();
  screenshot(client, 'AC-F011-001', 'four-scales-cannot-open');
  hold('iceandfire:dragonscales_red', 5);
  open();
  screenshot(client, 'AC-F011-001', 'five-red-scales-open');
  close();
  expect(heldCount(), 5, 'Scale open and close');
  inventory('AC-F011-001', 'five-scales-unchanged-after-close');
  open();
  await cut(['#####', '#...#', '#...#', '.....', '.....'], null, true);
  close();
  expect(heldCount(), 5, 'Unfinished scales');
  inventory('AC-F011-001', 'five-scales-after-unfinished-close');
  command(client, 'item replace entity @s weapon.offhand with iceandfire:dragonscales_blue 1');
  hold('iceandfire:dragonscales_red', 4);
  open();
  screenshot(client, 'AC-F011-001', 'mixed-colors-cannot-open');
  command(client, 'item replace entity @s weapon.offhand with minecraft:air');
  hold('iceandfire:dragonscales_red', 5);
  open();
  await cut(['#####', '#...#', '#...#', '.....', '.....'], 'iceandfire:armor_red_helmet');
  screenshot(client, 'AC-F011-001', 'completed-red-helmet');
  mct(client, 'gui', 'click', '0', '--button', 'shift-left');
  close();
  expect(heldCount(), 0, 'Completed scale consumption');
  expect(itemCount('iceandfire:armor_red_helmet'), 1, 'Completed scale output');
  inventory('AC-F011-001', 'five-scales-consumed-one-helmet');
});
