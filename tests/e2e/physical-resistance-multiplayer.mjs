const { scenario, command, camera, screenshot, waitFor, mct } = await import(process.env.HARNESS_E2E_LIB);

await scenario(async ({ clients }) => {
  const [a, b] = clients;
  if (!a || !b) throw new Error('Two clients are required');
  const healthTolerance = 0.1;
  const serverHealth = async selector => {
    mct(a, 'chat', 'clear');
    command(a, `data get entity ${selector} Health`);
    const result = await waitFor(
      () => JSON.stringify(mct(a, 'chat', 'history', '--last', '5')).match(/[\d.]+f\b/g),
      matches => matches?.length,
      5_000,
      200,
    );
    if (!result?.length) throw new Error(`No server health for ${selector}`);
    return Number(result.at(-1).slice(0, -1));
  };
  const dragonHealth = client => {
    const nearby = mct(client, 'entity', 'list', '--radius', '48');
    const dragon = nearby.entities.find(entity => entity.type === 'iceandfire:fire_dragon');
    if (!dragon) throw new Error(`Dragon is not visible to ${client}: ${JSON.stringify(nearby)}`);
    return Number(mct(client, 'entity', 'info', '--id', String(dragon.id)).health);
  };
  const equal = (actual, expected, label) => {
    if (!Number.isFinite(actual) || Math.abs(actual - expected) > healthTolerance) {
      throw new Error(`${label}: client ${actual}, server ${expected}`);
    }
  };
  for (const client of [a, b]) {
    command(client, 'gamemode survival');
    command(client, 'effect clear @s');
    command(client, 'effect give @s minecraft:instant_health 1 10 true');
    command(client, 'item replace entity @s armor.head with iceandfire:dragonsteel_fire_helmet');
  }
  command(a, 'gamerule sendCommandFeedback true');
  command(a, 'gamerule naturalRegeneration false');
  command(a, 'item replace entity @s weapon.mainhand with iceandfire:dragonsteel_fire_sword');
  command(a, 'summon iceandfire:fire_dragon 0 -60 4 {AgeTicks:1200000,NoAI:1b,PersistenceRequired:1b,Rotation:[180.0f,0.0f],Tags:["iaftfc_resistance_target"]}');
  await camera(a, { x: 0, y: -59, z: 1, yaw: 0, pitch: 10 });
  await camera(b, { x: 3, y: -59, z: 1, yaw: -45, pitch: 10 });

  const dragonSelector = '@e[tag=iaftfc_resistance_target,limit=1]';
  const dragonBefore = await serverHealth(dragonSelector);
  mct(a, 'entity', 'attack', '--nearest', '--type', 'iceandfire:fire_dragon');
  const dragonAfter = await waitFor(() => serverHealth(dragonSelector), health => health < dragonBefore, 10_000);
  if (dragonAfter >= dragonBefore) throw new Error('The classified sword did not damage the dragon');
  equal(await waitFor(() => dragonHealth(a), health => Math.abs(health - dragonAfter) <= healthTolerance, 5_000), dragonAfter, 'Dragon observed by A');
  equal(await waitFor(() => dragonHealth(b), health => Math.abs(health - dragonAfter) <= healthTolerance, 5_000), dragonAfter, 'Dragon observed by B');

  const playerBefore = await serverHealth('iaftfc_b');
  command(a, 'item replace entity @s weapon.mainhand with iceandfire:copper_sword');
  mct(a, 'entity', 'attack', '--name', 'iaftfc_b');
  const playerAfter = await waitFor(() => serverHealth('iaftfc_b'), health => health < playerBefore, 10_000);
  if (playerAfter >= playerBefore) throw new Error('The classified sword did not damage the armored player');
  const observedByB = await waitFor(async () => ({
    client: Number(mct(b, 'status', 'health').health),
    server: await serverHealth('iaftfc_b'),
  }), health => Math.abs(health.client - health.server) <= healthTolerance, 5_000);
  equal(observedByB.client, observedByB.server, 'Armored player observed by B');
  const player = mct(a, 'entity', 'list', '--radius', '24').entities.find(entity => entity.name === 'iaftfc_b');
  if (!player) throw new Error('Armored player is not visible to A');
  const observedByA = await waitFor(async () => ({
    client: Number(mct(a, 'entity', 'info', '--id', String(player.id)).health),
    server: await serverHealth('iaftfc_b'),
  }), health => Math.abs(health.client - health.server) <= healthTolerance, 5_000);
  equal(observedByA.client, observedByA.server, 'Armored player observed by A');

  await camera(a, { x: -5, y: -59, z: -22, yaw: -11, pitch: -10 });
  await camera(b, { x: 5, y: -59, z: -22, yaw: 11, pitch: -10 });
  equal(dragonHealth(a), dragonAfter, 'Dragon framed by A');
  equal(dragonHealth(b), dragonAfter, 'Dragon framed by B');
  mct(a, 'chat', 'clear');
  screenshot(a, 'AC-F014-003', 'client-a');
  screenshot(b, 'AC-F014-003', 'client-b');
});
