const { scenario, command, camera, screenshot, mct } = await import(process.env.HARNESS_E2E_LIB);

await scenario(async ({ clients }) => {
  const client = clients[0];
  command(client, 'time set day');
  command(client, 'weather clear');
  command(client, 'gamemode spectator');
  const elements = ['fire', 'ice', 'lightning'];
  const steels = ['red_steel', 'blue_steel', 'black_steel'];
  for (const [index, element] of elements.entries()) {
    const x = (index - 1) * 12;
    command(client, `fill ${x - 6} 79 0 ${x + 2} 79 6 minecraft:stone`);
    for (const dy of [-1, 0, 1]) {
      for (const dx of [-1, 0, 1]) {
        for (const dz of [-1, 0, 1]) {
          if (dy === 0 && dx === 0 && dz === 0) continue;
          const block = dy === 0 && dx === 0 && dz === -1
            ? `iceandfire:dragonforge_${element}_input`
            : dy !== 0 && dx !== 0 && dz !== 0
              ? 'iceandfire:dragon_bone_block'
              : `iceandfire:dragonforge_${element}_brick`;
          command(client, `setblock ${x + dx} ${81 + dy} ${4 + dz} ${block}`);
        }
      }
    }
    command(client, `setblock ${x} 81 4 iceandfire:dragonforge_${element}_core_disabled`);
    command(client, `item replace block ${x} 81 4 container.0 with tfc:metal/ingot/${steels[index]}`);
    command(client, `item replace block ${x} 81 4 container.1 with iceandfire:${element}_dragon_blood`);
    command(client, `summon iceandfire:${element}_dragon ${x - 4} 80 1 {AgeTicks:720000,NoAI:1b,PersistenceRequired:1b}`);
    command(client, `execute if entity @e[type=iceandfire:${element}_dragon,x=${x - 4},y=80,z=1,distance=..4] run setblock ${x} 78 4 minecraft:emerald_block`);
    const marker = JSON.stringify(mct(client, 'block', 'get', String(x), '78', '4'));
    if (!marker.includes('minecraft:emerald_block')) throw new Error(`${element} dragon did not remain beside its forge`);
  }
  await camera(client, { x: 0.5, y: 84, z: -22, yaw: 0, pitch: 9 }, 5000);
  screenshot(client, 'AC-F009-002', 'three-assembled-forges');
  command(client, 'gamemode survival');
  await camera(client, { x: 0.5, y: 81.6, z: 1, yaw: 0, pitch: 10 });
  mct(client, 'block', 'interact', '0', '81', '4');
  mct(client, 'gui', 'wait-open', '--timeout', '15');
  screenshot(client, 'AC-F009-002', 'ice-forge-gui');
});
