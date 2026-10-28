import { readFileSync, realpathSync, rmSync } from 'node:fs';
import { createRequire } from 'node:module';
import path from 'node:path';

const { scenario, command, screenshot, waitFor, mct } = await import(process.env.HARNESS_E2E_LIB);
const require = createRequire(realpathSync(process.env.HARNESS_MCT));
const { PNG } = require('pngjs');

const requiredMods = new Map([
  ['minecraft', '1.21.1'],
  ['neoforge', '21.1.265'],
  ['iaftfc', '1.0.0'],
  ['tfc', '4.2.12'],
  ['iceandfire', '2.1.4'],
  ['patchouli', '1.21.1-93-NEOFORGE'],
  ['uranus', '2.4.1-bugfix'],
  ['jupiter', '2.3.7'],
  ['architectury', '13.0.8'],
  ['integration', '0.2'],
]);

function clearVisibleChat(client) {
  mct(client, 'input', 'key', 'combo', 'f3', 'd');
}

async function setDebugOverlay(client, visible) {
  const probe = path.join(process.env.HARNESS_SCREENSHOT_DIR, 'debug-overlay-probe.png');
  mct(client, 'screenshot', '--output', probe);
  const image = PNG.sync.read(readFileSync(probe));
  rmSync(probe);
  let brightPixels = 0;
  for (let y = 0; y < Math.min(image.height, 180); y++) {
    for (let x = 0; x < Math.min(image.width, 800); x++) {
      const offset = (y * image.width + x) * 4;
      if (image.data[offset] > 210 && image.data[offset + 1] > 210 && image.data[offset + 2] > 210) {
        brightPixels++;
      }
    }
  }
  if ((brightPixels > 10_000) !== visible) {
    mct(client, 'input', 'key', 'press', 'f3');
    await new Promise(resolve => setTimeout(resolve, 500));
  }
}

function captureTop(client, file, height) {
  const target = path.join(process.env.HARNESS_SCREENSHOT_DIR, file);
  const source = PNG.sync.read(readFileSync(target));
  mct(client, 'screenshot', '--output', target, '--region', `0,0,${source.width},${height}`);
}

function showLines(client, lines) {
  clearVisibleChat(client);
  for (const line of lines) {
    command(client, `tellraw @s ${JSON.stringify({ text: line })}`);
  }
  mct(client, 'input', 'key', 'press', 't');
}

function loadedMods(history) {
  const mods = new Map();
  for (const match of JSON.stringify(history).matchAll(/:\s+([a-z][a-z0-9_]*)\s+\(([^)]+)\)\s+-\s+1/g)) {
    mods.set(match[1], match[2]);
  }
  return mods;
}

function assertRequiredSet(mods, clientNumber) {
  const actual = [...mods.entries()].sort(([left], [right]) => left.localeCompare(right));
  const expected = [...requiredMods.entries()].sort(([left], [right]) => left.localeCompare(right));
  if (JSON.stringify(actual) !== JSON.stringify(expected)) {
    throw new Error(`Client ${clientNumber} server mod set differs: ${JSON.stringify(actual)}`);
  }
}

await scenario(async ({ clients }) => {
  await new Promise(resolve => setTimeout(resolve, 12_000));

  clearVisibleChat(clients[0]);
  await setDebugOverlay(clients[0], false);
  screenshot(clients[0], 'AC-F001-001', 'dedicated-server-world');
  await setDebugOverlay(clients[0], true);
  const platformScreenshot = screenshot(clients[0], 'AC-F001-003', 'minecraft-neoforge-and-java');
  captureTop(clients[0], platformScreenshot, 320);
  await setDebugOverlay(clients[0], false);

  for (const [index, client] of clients.entries()) {
    await mct(client, 'chat', 'clear');
    command(client, 'neoforge mods');
    const history = await waitFor(
      () => mct(client, 'chat', 'history', '--last', '100'),
      value => loadedMods(value).size === requiredMods.size,
    );
    const mods = loadedMods(history);
    assertRequiredSet(mods, index + 1);
    const entries = [...mods.entries()].sort(([left], [right]) => left.localeCompare(right));
    showLines(client, [
      `Dedicated server: ${entries.length} loaded mods, exact required set`,
      ...Array.from({ length: 5 }, (_, line) => entries.slice(line * 2, line * 2 + 2)
        .map(([id, version]) => `${id} ${version}`)
        .join('  |  ')),
    ]);
    screenshot(client, 'AC-F001-001', `dedicated-server-loaded-mods-client-${index + 1}`);
    if (index === 0) {
      mct(client, 'input', 'key', 'press', 'escape');
      showLines(client, [
        'Required dependency contracts in the running distribution',
        'iaftfc -> tfc 4.2.12 (required, exact)',
        'iaftfc -> iceandfire 2.1.4 (required, exact)',
        'tfc -> patchouli >= 1.21.1-92-NEOFORGE',
        'iceandfire -> uranus >= 2.3.2',
        'iceandfire -> jupiter >= 2.3',
      ]);
      screenshot(client, 'AC-F001-002', 'required-dependency-contracts');
    }
    mct(client, 'input', 'key', 'press', 'escape');
  }

  showLines(clients[0], [
    'Selected stable Minecraft 1.21.1 / NeoForge distribution',
    'tfc 4.2.12  |  patchouli 1.21.1-93-NEOFORGE',
    'iceandfire 2.1.4  |  integration 0.2 (embedded)',
    'uranus 2.4.1-bugfix  |  architectury 13.0.8 (embedded)',
    'jupiter 2.3.7  |  iaftfc 1.0.0',
  ]);
  screenshot(clients[0], 'AC-F001-003', 'selected-platform-mods');
  mct(clients[0], 'input', 'key', 'press', 'escape');
});
