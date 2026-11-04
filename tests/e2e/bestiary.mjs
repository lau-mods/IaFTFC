const { scenario, command, screenshot, mct } = await import(process.env.HARNESS_E2E_LIB);

await scenario(async ({ clients }) => {
  const client = clients[0];
  command(client, 'gamemode survival');
  command(client, 'item replace entity @s weapon.mainhand with iceandfire:bestiary[iceandfire:bestiary_pages={pages:["iceandfire:introduction","iceandfire:tameddragons","iceandfire:dragonforge"]}]');
  mct(client, 'inventory', 'use');
  mct(client, 'gui', 'wait-open', '--timeout', '15');
  const size = mct(client, 'screen', 'size');
  const width = size.width ?? size.w;
  const height = size.height ?? size.h;
  const left = (width - 390) / 2;
  const top = (height - 245) / 2;
  const click = (x, y) => mct(client, 'input', 'click', String(Math.floor(x)), String(Math.floor(y)));

  click(left + 80, top + 55);
  screenshot(client, 'AC-F019-002', 'forge-bricks-and-layout');
  click(left + 365, top + 225);
  click(left + 365, top + 225);
  screenshot(client, 'AC-F019-002', 'steel-ingots-and-dragon-blood');
  click(left + 365, top + 225);
  screenshot(client, 'AC-F019-002', 'dragonsteel-forging');

  mct(client, 'gui', 'close');
  mct(client, 'inventory', 'use');
  mct(client, 'gui', 'wait-open', '--timeout', '15');
  click(left + 280, top + 35);
  click(left + 365, top + 225);
  click(left + 365, top + 225);
  click(left + 365, top + 225);
  screenshot(client, 'AC-F019-002', 'dragon-armor-double-sheets');
});
