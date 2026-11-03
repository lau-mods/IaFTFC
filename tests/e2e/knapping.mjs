const { scenario, command, screenshot, mct } = await import(process.env.HARNESS_E2E_LIB);

await scenario(async ({ clients }) => {
  const client = clients[0];
  command(client, 'gamemode survival');
  command(client, 'item replace entity @s weapon.mainhand with iceandfire:dragonbone 2');
  mct(client, 'inventory', 'use');
  mct(client, 'gui', 'wait-open', '--timeout', '15');
  screenshot(client, 'AC-F012-001', 'dragonbone-knapping-grid');
  mct(client, 'gui', 'close');

  command(client, 'item replace entity @s weapon.mainhand with iceandfire:dragonscales_red 2');
  mct(client, 'inventory', 'use');
  mct(client, 'gui', 'wait-open', '--timeout', '15');
  screenshot(client, 'AC-F013-001', 'red-scale-knapping-grid');
  mct(client, 'gui', 'close');
});
