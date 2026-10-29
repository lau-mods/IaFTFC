const { scenario, command, camera, screenshot, mct } = await import(process.env.HARNESS_E2E_LIB);
const { spawn } = await import('node:child_process');
const { openSync, closeSync, readFileSync } = await import('node:fs');
const { connect } = await import('node:net');
const path = await import('node:path');

const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));
const running = pid => {
  try { process.kill(-pid, 0); return true; } catch { return false; }
};
const listening = (host, port) => new Promise(resolve => {
  const socket = connect(port, host);
  socket.once('connect', () => { socket.destroy(); resolve(true); });
  socket.once('error', () => resolve(false));
});
const until = async (check, message, timeout = 120_000) => {
  const deadline = Date.now() + timeout;
  while (Date.now() < deadline) {
    if (await check()) return;
    await sleep(500);
  }
  throw new Error(message);
};

await scenario(async ({ clients }) => {
  const [a, b] = clients;
  if (!a || !b) throw new Error('Two clients are required');
  command(a, 'gamerule sendCommandFeedback false');
  command(a, 'time set noon');
  command(a, 'weather clear');
  command(a, 'gamemode spectator');
  command(b, 'gamemode spectator');
  mct(a, 'input', 'key', 'press', 'f1');
  mct(b, 'input', 'key', 'press', 'f1');
  const parts = ['head', 'neck', 'body', 'tail'];
  const view = async label => {
    await camera(a, { x: 8, y: -53, z: 8, yaw: 90, pitch: 16 });
    await camera(b, { x: 8, y: -53, z: 8, yaw: 90, pitch: 16 });
    screenshot(a, 'AC-F012-003', `${label}-client-a`);
    screenshot(b, 'AC-F012-003', `${label}-client-b`);
  };
  for (const material of ['steel', 'black_steel', 'red_steel', 'blue_steel']) {
    const items = ['{}', ...parts.map(part => `{id:"iaftfc:dragonarmor_${material}_${part}",count:1}`)].join(',');
    if (material !== 'steel') command(a, 'kill @e[tag=iaftfc_armor_shared]');
    command(a, `summon iceandfire:fire_dragon 0 -60 8 {AgeTicks:1200000,Rotation:[0f,0f],TamedDragon:1b,NoAI:1b,PersistenceRequired:1b,Tags:["iaftfc_armor_shared"],Items:[${items}]}`);
    await view(material);
  }

  command(b, 'kick iaftfc_a armor-reconnect-check');
  mct(a, 'client', 'reconnect');
  mct(a, 'client', 'wait-ready', a, '--timeout', '60');
  await view('after-reconnect');

  const config = JSON.parse(readFileSync('.harness-config.json', 'utf8')).runtime.server;
  const { pid } = JSON.parse(readFileSync('.harness-state/runtime/server.json', 'utf8'));
  const [host, portText] = config.address.split(':');
  const port = Number(portText);
  let restarted;
  try {
    command(a, 'save-all flush');
    process.kill(-pid, 'SIGTERM');
    await until(async () => !running(pid) && !await listening(host, port), 'Original server did not stop');

    const logFile = path.join(process.env.HARNESS_SCREENSHOT_DIR, 'server-restart.log');
    const log = openSync(logFile, 'a');
    try {
      restarted = spawn(config.command[0], config.command.slice(1), {
        cwd: path.resolve(config.directory),
        detached: true,
        stdio: ['ignore', log, log],
      });
      await new Promise((resolve, reject) => {
        restarted.once('spawn', resolve);
        restarted.once('error', reject);
      });
    } finally {
      closeSync(log);
    }
    await until(async () => /Done \([\d.,]+s\)!/.test(readFileSync(logFile, 'utf8')) && await listening(host, port), 'Restarted server did not become ready', 600_000);
    for (const client of [a, b]) {
      mct(client, 'client', 'reconnect');
      mct(client, 'client', 'wait-ready', client, '--timeout', '180');
    }
    await view('after-server-restart');
  } finally {
    if (restarted?.pid && running(restarted.pid)) {
      process.kill(-restarted.pid, 'SIGTERM');
      await until(() => !running(restarted.pid), 'Restarted server did not stop');
    }
  }
});
