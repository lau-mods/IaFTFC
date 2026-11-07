import { execFileSync } from 'node:child_process';

const { scenario, command, camera, screenshot, mct } = await import(process.env.HARNESS_E2E_LIB);

await scenario(async ({ clients }) => {
  const client = clients[0];
  command(client, 'time set day');
  command(client, 'weather clear');
  command(client, 'gamemode survival');
  const foods = [
    'fire_dragon_flesh', 'ice_dragon_flesh', 'lightning_dragon_flesh',
    'fire_stew', 'frost_stew', 'lightning_stew'
  ];
  for (const [slot, food] of foods.entries()) {
    command(client, `item replace entity @s hotbar.${slot} with iceandfire:${food}`);
  }
  const size = mct(client, 'screen', 'size');
  const width = size.width ?? size.w;
  const height = size.height ?? size.h;
  mct(client, 'input', 'key', 'press', 'e');
  mct(client, 'gui', 'wait-open', '--timeout', '15');
  screenshot(client, 'AC-F016-002', 'six-food-items');
  const shiftKey = down => {
    if (process.platform === 'darwin') {
      const instance = mct(client, 'client', 'list').clients.find(entry => entry.name === client);
      if (!instance?.pid) throw new Error(`Minecraft client process is unavailable: ${client}`);
      // MC Pilot reports its Node launcher; the Java child owns the GLFW window.
      const pid = Number(execFileSync('pgrep', ['-P', String(instance.pid)], { encoding: 'utf8' }).trim().split(/\s+/)[0]);
      execFileSync('osascript', ['-l', 'JavaScript', '-e',
        `ObjC.import('ApplicationServices'); ObjC.import('AppKit');
         const pid = ${pid};
         const app = $.NSRunningApplication.runningApplicationWithProcessIdentifier(pid);
         if (typeof app.activateWithOptions === 'function') app.activateWithOptions(1);
         const event = $.CGEventCreateKeyboardEvent(null, 56, ${down});
         $.CGEventSetFlags(event, ${down} ? $.kCGEventFlagMaskShift : 0);
         $.CGEventPostToPid(pid, event);`]);
    } else {
      mct(client, 'input', 'key', down ? 'down' : 'up', 'shift');
    }
  };
  for (const label of ['flesh-nutrition', 'stew-nutrition']) {
    if (label === 'stew-nutrition') {
      mct(client, 'gui', 'close');
      command(client, 'item replace entity @s hotbar.0 with iceandfire:fire_stew');
      mct(client, 'input', 'key', 'press', 'e');
      mct(client, 'gui', 'wait-open', '--timeout', '15');
    }
    mct(client, 'input', 'mouse-move', String(Math.floor(width / 2 - 72)),
      String(Math.floor(height / 2 + 72)));
    shiftKey(true);
    try {
      await new Promise(resolve => setTimeout(resolve, 500));
      screenshot(client, 'AC-F016-002', label);
    } finally {
      shiftKey(false);
    }
  }
  mct(client, 'gui', 'close');

  command(client, 'gamemode spectator');
  command(client, 'fill -7 79 0 7 79 8 minecraft:stone');
  const seekers = ['dragon_seeker', 'epic_dragon_seeker', 'legendary_dragon_seeker', 'godly_dragon_seeker'];
  for (const [index, seeker] of seekers.entries()) {
    const x = (index - 1.5) * 3;
    command(client, `summon minecraft:item_display ${x} 81 4 {item:{id:"iceandfire:${seeker}",count:1},transformation:{translation:[0f,0f,0f],left_rotation:[0f,0f,0f,1f],scale:[2f,2f,2f],right_rotation:[0f,0f,0f,1f]}}`);
  }
  command(client, 'summon iceandfire:fire_dragon 0 80 23 {NoAI:1b,PersistenceRequired:1b,AgeTicks:1200000,Tame:1b}');
  command(client, 'data modify entity @e[type=iceandfire:fire_dragon,sort=nearest,limit=1] Owner set from entity @s UUID');
  await camera(client, { x: 0.5, y: 83, z: -9.5, yaw: 0, pitch: 10 });
  screenshot(client, 'AC-F018-002', 'seeker-tiers-and-dragon');
  command(client, 'clear @s');
  await camera(client, { x: 0.5, y: 81, z: 18, yaw: 0, pitch: 10 });
  command(client, 'gamemode survival');
  mct(client, 'input', 'click', String(Math.floor(width / 2)), String(Math.floor(height / 2)), '--button', 'right');
  mct(client, 'chat', 'clear');
  command(client, 'execute as @s on vehicle if entity @s[type=iceandfire:fire_dragon] run say dragon-mounted');
  mct(client, 'chat', 'history', '--match', 'dragon-mounted', '--wait', '5');
  command(client, 'data merge entity @e[type=iceandfire:fire_dragon,sort=nearest,limit=1] {NoAI:0b}');
  mct(client, 'input', 'key', 'press', 'f5');
  mct(client, 'input', 'key', 'press', 'f5');
  for (let view = 0; view < 3; view++) mct(client, 'input', 'key', 'press', 'f7');
  mct(client, 'look', 'set', '--yaw', '0', '--pitch', '20');
  mct(client, 'input', 'key', 'press', 'f1');
  await new Promise(resolve => setTimeout(resolve, 1000));
  screenshot(client, 'AC-F018-002', 'mounted-dragon');
  mct(client, 'input', 'key', 'hold', 'space', '--duration', '5000');
  mct(client, 'chat', 'clear');
  command(client, 'execute as @s on vehicle if entity @s[type=iceandfire:fire_dragon,nbt={Hovering:1b}] run say dragon-airborne');
  command(client, 'execute as @s on vehicle if entity @s[type=iceandfire:fire_dragon,nbt={Flying:1b}] run say dragon-airborne');
  mct(client, 'chat', 'history', '--match', 'dragon-airborne', '--wait', '5');
  await new Promise(resolve => setTimeout(resolve, 1000));
  screenshot(client, 'AC-F018-002', 'dragon-flight');
});
