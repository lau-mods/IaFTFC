import { readFileSync } from 'node:fs';

const { scenario, command, camera, screenshot } = await import(process.env.HARNESS_E2E_LIB);

await scenario(async ({ clients }) => {
  const client = clients[0];
  const serverLog = readFileSync('.harness-state/runtime/server/logs/latest.log', 'utf8');
  const clientLog = readFileSync(`.harness-state/runtime/mct-home/clients/${client}/minecraft/logs/latest.log`, 'utf8');
  for (const [side, log] of [['server', serverLog], ['client', clientLog]]) {
    if (!/--fml\.neoForgeVersion, 21\.1\.248/.test(log) || !/NeoForge 21\.1\.248/.test(log)) {
      throw new Error(`${side} did not start with NeoForge 21.1.248`);
    }
    if (!/IaFTFC\s+1\.0\.0\s+\(iaftfc\)/.test(log)) {
      throw new Error(`${side} did not load iaftfc`);
    }
    if (/Registry Object not present|Duplicate registration|ModLoadingException|Exception in thread|Crash report saved/i.test(log)) {
      throw new Error(`${side} reported a load or registry failure`);
    }
  }
  if (!/--launchTarget, forgeserver/.test(serverLog) || !/Env=SERVER/.test(serverLog)
    || !/IaFTFC loaded on DEDICATED_SERVER/.test(serverLog) || !/logged in with entity id/.test(serverLog)) {
    throw new Error('Dedicated server dist or client login is missing');
  }
  const invalidDistLoads = serverLog.split('\n').filter(line =>
    /Attempted to load class .* for invalid dist/.test(line)
    && !/Attempted to load class net\/minecraft\/client\/(?:gui\/components\/Button|player\/LocalPlayer) for invalid dist DEDICATED_SERVER/.test(line));
  if (invalidDistLoads.length > 0) {
    throw new Error(`Dedicated server loaded a client-only class: ${invalidDistLoads.join('\n')}`);
  }
  if (!/IaFTFC loaded on CLIENT/.test(clientLog)) {
    throw new Error('Client did not initialize iaftfc');
  }
  command(client, 'time set day');
  command(client, 'weather clear');
  command(client, 'fill -4 79 -4 4 79 4 minecraft:stone');
  command(client, 'item replace entity @s weapon.mainhand with iceandfire:dragon_horn');
  await camera(client, { x: 0.5, y: 80.6, z: -2.5, yaw: 0, pitch: 10 });
  screenshot(client, 'AC-F001-001', 'joined-world');
});
