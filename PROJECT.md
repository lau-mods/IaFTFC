# Project

Status: active

Project ID: IaFTFC
Mod ID: iaftfc

## Platform

Minecraft: 1.21.1
NeoForge: 21.1.248
Java: 21

## Purpose

`Ice and Fire and TerraFirma` は、Ice and Fire: Community Edition のドラゴン関連コンテンツを TerraFirmaCraft の地質、気候、金属加工、暦、食料、アイテム物性へ統合する NeoForge 互換 Mod とする。

対象は Fire Dragon、Ice Dragon、Lightning Dragon、本体の成長・繁殖・騎乗・ブレス・卵、およびそれらに直接必要な Roost、Cave、自然骨格、Dragon Forge、Dragonsteel、Dragonbone、Dragon Scale Armor、Dragon Armor、Dragon Meal、Dragon Seeker、Bestiary、Manuscript、Lectern、Dragon Horn、Dragon Flute、Dragon Staff、Summoning Crystal とする。

独立した非ドラゴン Mob、非ドラゴン構造物、非ドラゴン固有装備、生態系は原則として再設計しない。ただし Bestiary は Ice and Fire CE 本来の全ページを維持する。

## Features

### F-001: TFC気候に基づくドラゴン自然生成

#### Description

Fire、Ice、Lightning の Roost と Cave を TFC Overworld で生成可能にする。属性選別にはバイオーム名を使用せず、TFC の実気候値を使用する。

Fire は高温域、Ice は低温域、Lightning は高降水量かつ降水変動の大きい地域を選ぶ。3属性の条件は相互排他的にする。具体的な閾値は固定テスト用の TFC 気候サンプルで校正し、受理された構造物候補に占める3属性の比率が概ね等しくなるよう設定する。閾値は実行時に再学習せず、コードまたはデータとして固定する。

Ice and Fire CE 本来の危険地点距離、構造物生成可否、Dragon 本体生成、性別・年齢等は本仕様で明示的に変更しない限り維持する。自然生成する Dragon Skeleton も維持する。

#### Acceptance Criteria

##### AC-F001-001: TFC気候による属性選別

Preconditions:
Minecraft 1.21.1、TFC 4.2.12 系、Ice and Fire CE 2.1.4 系、IaFTFC を導入した新規 TFC ワールドを使用する。

Action:
Fire、Ice、Lightning の Roost/Cave の配置候補について、TFC の平均気温、降水量、降水変動を取得して属性フィルタを評価する。

Expected Result:
バイオーム名ではなく TFC 気候値だけで属性が選別される。Fire は設定された高温条件、Ice は設定された低温条件、Lightning は設定された高降水量かつ高降水変動条件を満たす。1地点が複数属性の条件を同時に満たさない。

##### AC-F001-002: 3属性の概ね等頻度

Preconditions:
固定 Seed 群から TFC の陸上気候を十分に横断する900件以上の構造物配置候補を決定論的に採取する。

Action:
各候補へ F-001 の属性フィルタを適用し、受理された Fire、Ice、Lightning 候補数を集計する。

Expected Result:
受理候補全体に対する各属性の比率がそれぞれ25%以上42%以下となる。条件変更後も同じ Seed 群で同じ分類結果を再現する。

##### AC-F001-003: RoostとCaveの生成

Preconditions:
各属性の気候条件を満たす未生成チャンクが存在し、Ice and Fire CE 側の構造物生成設定が有効である。

Action:
新規チャンクを生成し、各属性の Roost と Cave を探索する。

Expected Result:
Fire、Ice、Lightning の Roost と Cave がそれぞれ TFC 地形上へ生成される。IaFTFC が同一構造物を二重生成しない。

##### AC-F001-004: 自然Dragon Skeletonの維持

Preconditions:
Ice and Fire CE の Dragon Skeleton 自然生成が有効である。

Action:
Dragon Skeleton の配置条件を満たす新規チャンクを生成する。

Expected Result:
IaFTFC は Dragon Skeleton feature を無効化または別コンテンツへ置換せず、Ice and Fire CE 本来の自然骨格を生成可能な状態に保つ。

### F-002: Dragon CaveとRoostの地質統合

#### Description

Dragon Cave の外殻と Roost の属性地形化は Ice and Fire CE の識別性を維持する。TFC 岩石を属性別の汎用 Charred、Frozen、Crackled 系ブロックへ変換してよく、元の TFC 岩石種を保存する属性別派生ブロックは追加しない。

Cave 内で Ice and Fire CE が直接配置する鉱石枠は、金属鉱石ではなく TFC の9種の宝石鉱石へ置換する。9種は Amethyst、Diamond、Emerald、Lapis Lazuli、Opal、Pyrite、Ruby、Sapphire、Topaz とし、すべて等 weight とする。宝石鉱石の母岩は Fire=`basalt`、Ice=`quartzite`、Lightning=`diorite` に固定する。

Ice and Fire CE 独自の Silver Ore と Sapphire Ore の TFC Overworld 生成は無効化し、ドラゴン統合上の銀とサファイアは TFC の体系を正規経路とする。

#### Acceptance Criteria

##### AC-F002-001: Cave外殻の属性石化

Preconditions:
Cave 生成範囲に TFC の raw rock が存在する。

Action:
Fire、Ice、Lightning の各 Dragon Cave を生成する。

Expected Result:
Cave 外殻は属性に対応する Ice and Fire CE の汎用 Charred、Frozen、Crackled 系石材へ変換される。元の岩石種別を保持する新規属性岩石ブロックは生成されない。

##### AC-F002-002: Cave宝石9種の等weight

Preconditions:
Dragon Cave が鉱石配置処理を実行する。

Action:
Cave の鉱石候補 table または選択処理を検査し、十分な回数の決定論的抽選を実行する。

Expected Result:
候補は `tfc:ore` 系の Amethyst、Diamond、Emerald、Lapis Lazuli、Opal、Pyrite、Ruby、Sapphire、Topaz の9種だけであり、各候補の設定 weight は等しい。Vanilla 金属鉱石または Ice and Fire CE 独自 Silver/Sapphire Ore を Cave 鉱石枠から配置しない。

##### AC-F002-003: 属性固定母岩

Preconditions:
各属性 Cave で宝石鉱石が配置される。

Action:
配置された宝石鉱石の block ID を確認する。

Expected Result:
Fire Cave は `tfc:ore/<gem>/basalt`、Ice Cave は `tfc:ore/<gem>/quartzite`、Lightning Cave は `tfc:ore/<gem>/diorite` を使用する。周囲の元地質によって母岩が変化しない。

##### AC-F002-004: SilverとSapphireのTFC一元化

Preconditions:
TFC Overworld の新規チャンクを生成し、Ice and Fire CE と TFC の worldgen registry が読み込まれている。

Action:
Silver Ore と Sapphire Ore の配置結果および関連 worldgen 定義を検査する。

Expected Result:
Ice and Fire CE 独自 Silver Ore/Sapphire Ore は TFC Overworld に自然生成せず、ドラゴン関連の銀・サファイア報酬と Cave 資源は TFC の Silver/Sapphire を使用する。

### F-003: ドラゴンブレスと地形破壊

#### Description

生存 Dragon のブレス、噛み砕き、ブロック破壊は Ice and Fire CE 本来の危険性を維持する。TFC の raw rock、ore、地形ブロックは原則として破壊・属性変換の対象となり、Anvil、Crucible、Charcoal Forge、Bloomery、Blast Furnace、Barrel、Vessel、Quern 等を IaFTFC 独自に一括保護しない。

Ice and Fire CE 本来の `mobGriefing`、Dragon griefing 設定、爆発耐性、`dragon_block_break_blacklist` 等は維持する。ブレス地形処理だけを変更するために DragonFireDamageWorldEvent 全体を cancel してはならない。Dragon Forge Input への flame hit と Entity damage を維持したまま、必要なブロック変換部分だけを狭い Mixin または同等のフックで変更する。

#### Acceptance Criteria

##### AC-F003-001: TFC岩石と鉱石の破壊許可

Preconditions:
`mobGriefing=true` で、Ice and Fire CE 本来の破壊 blacklist に含まれない TFC raw rock または ore が Dragon の破壊範囲にある。

Action:
十分な Stage の Dragon に対象ブロックを破壊させる。

Expected Result:
対象 TFC block は Ice and Fire CE 本来の強度・Stage・griefing 条件に従って破壊可能であり、IaFTFC が岩石または鉱石であることだけを理由に保護しない。

##### AC-F003-002: TFC設備の原則破壊可能性

Preconditions:
`mobGriefing=true` で、TFC Anvil、Crucible、Charcoal Forge、Bloomery、Blast Furnace、Barrel、Vessel、Quern の各ブロックが Ice and Fire CE 本来の blacklist に追加されていない。

Action:
各設備へ Dragon のブロック破壊判定を適用する。

Expected Result:
IaFTFC 独自の包括的設備 blacklist は存在せず、各設備は Ice and Fire CE 本来の一般破壊判定に従う。

##### AC-F003-003: ブレス処理とDragon Forgeの両立

Preconditions:
組み立て済み Dragon Forge Input と、ブレスによる地形変換対象 TFC block が同一テスト環境にある。

Action:
対応属性 Dragon のブレスを発生させる。

Expected Result:
TFC 地形の属性変換処理が実行され、同時に Dragon Forge Input は本来どおり flame hit を受ける。Entity damage も失われない。IaFTFC は地形変換のためにブレスイベント全体を cancel しない。

### F-004: Dragon構造物LootとMetal Pile

#### Description

3属性 × Roost/Female Cave/Male Cave の9種の Dragon Chest Loot は、元 Ice and Fire CE の報酬量を再調整するのではなく、元 entry を TFC の同等カテゴリへ翻訳する。

元 table の pool 構造、rolls、bonus rolls、entry 数、weight、count provider、conditions、IaF 固有 item は原則維持する。profile は Roost=`ordinary`、Female Cave=`dangerous`、Male Cave=`lategame` とする。

汎用 ingot 候補は次とする。

| Profile | 汎用Ingot候補 |
| --- | --- |
| ordinary | Copper、Bronze、Bismuth Bronze、Black Bronze、Wrought Iron |
| dangerous | Wrought Iron、Steel |
| lategame | Steel、Black Steel |

装備は profile の汎用 ingot 上限より一段低くする。ordinary 装備は Copper/Bronze/Bismuth Bronze/Black Bronze、dangerous 装備は Wrought Iron、lategame 装備は Steel のみとする。Red Steel と Blue Steel はすべての汎用 loot 候補から除外し、Black Steel 装備も出さない。

汎用 Gem は TFC の Amethyst、Diamond、Emerald、Lapis Lazuli、Opal、Pyrite、Ruby、Sapphire、Topaz を等 weight で抽選する。

IaF 属性固有 entry は次へ固定変換する。

| 属性 | Gem | Ingot/Nugget |
| --- | --- | --- |
| Fire | Emerald → TFC Ruby | Gold → TFC Gold / Small Native Gold |
| Ice | Sapphire Gem → TFC Sapphire | Silver → TFC Silver / Small Native Silver |
| Lightning | Amethyst Shard → TFC Topaz | Copper → TFC Copper / Small Native Copper |

IaF の汎用 Diamond は属性報酬とみなさず shared Gem table へ送る。IaF の汎用 Iron Ingot は profile 別 shared Ingot table へ送る。

Tool は元用途の family を保持する。Pickaxe→Pickaxe/Propick、Axe→Axe/Saw、Hoe→Hoe/Scythe、Sword→Sword/Javelin/Mace とし、family 内は等 weight とする。Shovel、Knife、Shears、Shield は同種だけへ変換し、Bow と Crossbow は相互変換しない。

金属 tool/armor entry は加工途中品:完成品の合計 weight を2:1にする。Armor は部位を変更しない。元 enchantment function は shared table 単位に置いてよいが、Tool Head、Blade、Unfinished Armor の実出力に enchantment が付与されてはならない。

Gold/Silver/Copper Pile は Vanilla/IAF Nugget ではなく対応する TFC Small Native Ore をドロップする。元の layer/count provider はカテゴリ変換以外で再調整しない。

#### Acceptance Criteria

##### AC-F004-001: 9種Dragon Chestの構造維持

Preconditions:
対象 Ice and Fire CE 2.1.4 の9種の元 Dragon Chest Loot Table と IaFTFC の置換 table を取得する。

Action:
各 table の pool 数、rolls、bonus rolls、entry 数、entry weight、count provider、conditions、IaF 固有 item を比較する。

Expected Result:
カテゴリ変換に必要な差分以外は元構造と値が一致し、元 IaF 固有 item が意図せず削除されない。

##### AC-F004-002: Profile別金属進行

Preconditions:
ordinary、dangerous、lategame の shared ingot/equipment table が読み込まれている。

Action:
各 table の全候補 item を列挙する。

Expected Result:
ordinary ingot は Copper/Bronze/Bismuth Bronze/Black Bronze/Wrought Iron、dangerous ingot は Wrought Iron/Steel、lategame ingot は Steel/Black Steel の範囲内である。装備は ordinary が Copper/Bronze系、dangerous が Wrought Iron、lategame が Steel のみである。Black Steel 装備、Red Steel loot、Blue Steel loot は存在しない。

##### AC-F004-003: Gemと属性固定報酬

Preconditions:
shared Gem table と Fire/Ice/Lightning の Dragon Chest table が読み込まれている。

Action:
Gem entry と属性固有 Gem/Ingot/Nugget entry を検査する。

Expected Result:
shared Gem table は9種の TFC Gem を等 weight で含む。Fire Emerald/Gold、Ice Sapphire/Silver、Lightning Amethyst/Copper だけが指定された属性固定 TFC 報酬へ変換され、汎用 Diamond/Iron は shared table を使用する。

##### AC-F004-004: Equipment familyと加工途中品比率

Preconditions:
元 Loot に Sword、Pickaxe、Axe、Hoe、Shovel または Armor entry がある。

Action:
対応する shared equipment table の候補と weight を集計する。

Expected Result:
Tool family は Description の対応関係を守り、family 内候補は等 weight である。金属 tool/armor の加工途中品と完成品の weight 合計比は2:1で、Armor 部位は維持される。Tool Head、Blade、Unfinished Armor は enchant されない。

##### AC-F004-005: Metal PileのSmall Ore化

Preconditions:
Gold、Silver、Copper Pile を layer 1 と最大 layer の状態で配置する。

Action:
各 Pile を通常破壊して loot を取得する。

Expected Result:
Gold は `tfc:ore/small_native_gold`、Silver は `tfc:ore/small_native_silver`、Copper は `tfc:ore/small_native_copper` を返す。元の layer/count 関係は維持され、Nugget、Powder、Ingotへ変換されない。

### F-005: Dragon ForgeとDragonsteel生成

#### Description

Dragon Forge 本体の multiblock、Dragon Breath による駆動、cook time、Dragon の誘導等は Ice and Fire CE 本来の挙動を維持する。入力材料と出力 Dragonsteel を TFC 進行へ接続する。

属性と母材の対応は Fire=Red Steel、Ice=Blue Steel、Lightning=Black Steel とする。Dragonsteel recipe は対応母材 Ingot 1個 + 対応 Dragon Blood 1個 → 対応 IaFTFC Dragonsteel Ingot 1個とする。

Dragon Forge Brick は元 IaF の属性別 recipe shape、Dragon Scale Block、出力数を維持し、Stone Bricks 枠を TFC Fire Bricks へ置換する。Dragon Forge Input の Iron 枠は TFC Wrought Iron 系共通タグへ置換する。Core の Dragon Heart 等、ドラゴン固有材料は維持する。

Dragonsteel の正規 metal form は `iaftfc` namespace に置く。Ice and Fire CE 既存 Dragonsteel Ingot/Block の直接圧縮、分解、通常入手 recipe は無効化し、IaFTFC metal form と並行する別経路を作らない。既存 IaF の完成 Tool/Player Armor は最終出力 item として維持する。

#### Acceptance Criteria

##### AC-F005-001: 属性別Dragon Forge入力

Preconditions:
3属性の組み立て済み Dragon Forge と対応 Dragon Blood を用意する。

Action:
Fire Forge に Red Steel Ingot、Ice Forge に Blue Steel Ingot、Lightning Forge に Black Steel Ingot を1個ずつ投入して対応 Dragon Breath で処理する。

Expected Result:
各 Forge は対応する IaFTFC Dragonsteel Ingot を1個出力する。不一致属性の母材または Blood ではその Dragonsteel recipe が成立しない。

##### AC-F005-002: Dragon Forge BrickとInputのTFC素材化

Preconditions:
Dragon Forge Brick/Input の recipe data を読み込む。

Action:
3属性の recipe ingredient と shape を Ice and Fire CE 元 recipe と比較する。

Expected Result:
Brick は Stone Bricks 枠だけが TFC Fire Bricks に置換され、属性 Scale Block と shape と出力数を維持する。Input の Iron 相当材料は Wrought Iron 系 TFC ingredient を使用し、Vanilla Iron Ingot への固定依存が残らない。

##### AC-F005-003: IaFTFC Dragonsteelの正規化

Preconditions:
Ice and Fire CE と IaFTFC の Dragonsteel Ingot/Block recipe が全て読み込まれている。

Action:
Crafting、Dragon Forge、Heating/Casting の入手経路を列挙する。

Expected Result:
正規の Ingot と Metal Block は IaFTFC namespace の Dragonsteel form である。Ice and Fire CE 既存 Dragonsteel Ingot/Block を通常進行で複製・圧縮・分解して別系統として利用できない。

##### AC-F005-004: Dragon Forge本体挙動の維持

Preconditions:
完成済み Dragon Forge、対応属性 Dragon、正しい入力と Blood がある。

Action:
Dragon Forge を通常の Ice and Fire CE 操作で稼働させる。

Expected Result:
Dragon Breath の受信、multiblock 判定、処理進行、Dragon 誘導等の本来の Forge 機構が動作し、IaFTFC の材料統合が Forge 本体の機能を置換しない。

### F-006: Tier 7 Dragonsteel金属加工と装備

#### Description

`dragonsteel_fire`、`dragonsteel_ice`、`dragonsteel_lightning`を IaFTFC の TFC-style metal とする。3属性共通で Tier 7、融点2000℃、鍛造可能温度1200℃、溶接可能温度1600℃とする。Heat Capacity は TFC Red/Blue Steel と同一の計算規則を用い、metal amount に比例させる。

必要 form は Ingot、Double Ingot、Sheet、Double Sheet、Rod、Molten Fluid、Plated Block、Plated Slab、Plated Stairs、Anvil、Axe Head、Hoe Head、Pickaxe Head、Shovel Head、Sword Blade、Unfinished Helmet、Unfinished Chestplate、Unfinished Greaves、Unfinished Boots とする。

IaF に存在しない Dragonsteel Tool は追加しない。完成 Tool は Sword、Pickaxe、Axe、Shovel、Hoe のみとし、既存 Ice and Fire CE の完成 item ID、攻撃性能、耐久、enchantability、属性特殊効果を維持する。Tool part の Forge Rule は対応する TFC metal part と同一にする。完成 Tool は対応 part と `#c:rods/wooden` を用いる TFC advanced crafting 形式とし、Forging Bonus を引き継ぐ。

Player Dragonsteel Armor は TFC metal armor と同じ工程を使用する。Double Sheet から Tier 7 Anvil で Unfinished Armor を鍛造し、Helmet=Unfinished Helmet+Sheet、Chestplate=Unfinished Chestplate+Double Sheet、Leggings=Unfinished Greaves+Sheet、Boots=Unfinished Boots+Sheet を溶接して既存 IaF Dragonsteel Armor を完成させる。Forging Bonus は TFC と同じ `copy_best` 相当で引き継ぐ。

Dragonsteel は Crucible で溶融し、標準 TFC metal amount に従って再鋳造可能とする。

#### Acceptance Criteria

##### AC-F006-001: Dragonsteel熱特性

Preconditions:
3属性 Dragonsteel の item heat と molten fluid heat 定義を読み込む。

Action:
Ingot を加熱して forging、welding、melting の各境界を確認する。

Expected Result:
3属性すべてで forging temperature=1200℃、welding temperature=1600℃、melt temperature=2000℃である。Heat Capacity は同じ metal amount の TFC Red/Blue Steel form と同じ規則で算出される。

##### AC-F006-002: Tier 7導入経路

Preconditions:
Tier 6 の Red/Blue Steel 級 Anvil、Dragonsteel Ingot 2個、Welding Flux を用意する。

Action:
Dragonsteel Ingot 2個を溶接し、その後 Dragonsteel Anvil を成立させて Sheet/Tool Head 等を加工する。

Expected Result:
最初の Ingot+Ingot→Double Ingot の溶接だけは Tier 6 Anvil で実行できる。Dragonsteel Anvil は Tier 7 として機能し、Sheet、Rod、Double Sheet、Tool Head、Sword Blade、Unfinished Armor 等の通常 Dragonsteel 鍛造は Tier 7 を要求する。

##### AC-F006-003: Tool種別とForge Rule

Preconditions:
Tier 7 Dragonsteel Anvil と加工可能温度の Dragonsteel form がある。

Action:
Sword Blade、Pickaxe Head、Axe Head、Shovel Head、Hoe Head を鍛造する。

Expected Result:
各 part は対応する TFC Tool Part と同一 Forge Rule を使用し、Tier 7 を要求する。Propick、Saw、Hammer、Scythe、Knife、Javelin、Chisel、Mace 等、Ice and Fire CE に存在しない Dragonsteel Tool/part は追加されない。

##### AC-F006-004: 既存Dragonsteel Tool性能の維持

Preconditions:
鍛造済み Dragonsteel Tool Part と Wooden Rod がある。

Action:
5種の既存 IaF Dragonsteel Tool を TFC-style recipe で組み立て、同じ Ice and Fire CE 2.1.4 の未改変 Tool と属性・耐久・攻撃値・特殊効果を比較する。

Expected Result:
完成 item は既存 Ice and Fire CE の Sword/Pickaxe/Axe/Shovel/Hoe ID を使用し、本来の性能と属性能力を維持する。Forging Bonus が完成品へ引き継がれる。

##### AC-F006-005: Player Dragonsteel Armor工程

Preconditions:
Tier 7 Dragonsteel Anvil、必要な Double Sheet/Sheet、Welding Flux を用意する。

Action:
4部位の Unfinished Armor を鍛造し、TFC metal armor と同じ材料組合せで溶接する。

Expected Result:
既存 IaF の3属性 Dragonsteel Helmet/Chestplate/Leggings/Boots が完成し、元 IaF の防御性能と特殊能力を維持する。通常 Crafting Table だけで Ingot から完成防具へ直行できない。

##### AC-F006-006: Dragonsteelの溶融と再鋳造

Preconditions:
Dragonsteel form と2000℃以上を扱える TFC Crucible 系設備を用意する。

Action:
Dragonsteel を溶融し、対応する molten fluid を保持して標準 TFC casting route で Ingot へ戻す。

Expected Result:
Dragonsteel は molten fluid として回収され、TFC の対応 form と同じ metal amount 規則で再鋳造できる。溶融によって別属性 Dragonsteel へ変換されない。

### F-007: ドラゴン用Dragon Armor再編

#### Description

Dragon が装備する Dragon Armor は Head、Neck、Body、Tail の4部位を維持する。

有効な系統と Protection 値は次とする。

| Material | Protection |
| --- | ---: |
| Iron | 1.5 |
| Steel | 5 |
| Black Steel | 7 |
| Red Steel | 8 |
| Blue Steel | 8 |
| Fire Dragonsteel | 10 |
| Ice Dragonsteel | 10 |
| Lightning Dragonsteel | 10 |

既存 Iron Dragon Armor item は維持し、材料だけを TFC Wrought Iron Double Sheet へ置換する。Steel、Black Steel、Red Steel、Blue Steel の4系統を IaFTFC で追加する。既存3属性 Dragonsteel Dragon Armor は維持し、材料を対応 IaFTFC Dragonsteel Double Sheet へ置換する。

各部位は Ice and Fire CE 元 recipe の shape を維持し、元の Material Block 1個に相当する各 `#c:double_sheets/<metal>` 1個を同じ位置へ置く。

Copper、Silver、Gold、Diamond、Netherite Dragon Armor は item registration 自体を削除して既存 world を破壊せず、通常取得 recipe を無効化する。

#### Acceptance Criteria

##### AC-F007-001: Dragon Armor系統とProtection

Preconditions:
Dragon Armor registry と IaFTFC 追加 material が読み込まれている。

Action:
有効な全 Dragon Armor material の Protection 値を取得する。

Expected Result:
Iron=1.5、Steel=5、Black Steel=7、Red Steel=8、Blue Steel=8、3属性 Dragonsteel=10 である。Copper/Silver/Gold/Diamond/Netherite の通常取得 recipe は存在しない。

##### AC-F007-002: Double Sheet recipe

Preconditions:
Iron、Steel、Black Steel、Red Steel、Blue Steel、3属性 Dragonsteel の Double Sheet がある。

Action:
Head、Neck、Body、Tail の recipe を実行する。

Expected Result:
各 recipe は Ice and Fire CE 元 Dragon Armor と同じ shape を使用し、Material Block の代わりに対応 Double Sheet を使用する。Iron は Wrought Iron、Dragonsteel は IaFTFC の対応属性 Double Sheet を要求する。

##### AC-F007-003: Dragon装備スロットの互換性

Preconditions:
各有効 material の Head/Neck/Body/Tail を所持し、飼い慣らした Dragon がいる。

Action:
4部位を Dragon へ装備する。

Expected Result:
Ice and Fire CE 本来の Dragon Armor slot 判定と防御計算で正常に装備され、部位が入れ替わらない。

##### AC-F007-004: 追加Dragon Armorの描画

Preconditions:
Steel、Black Steel、Red Steel、Blue Steel Dragon Armor の4部位を装備した Dragon をクライアントで表示する。

Action:
各 material と部位を切り替えて Dragon model を描画する。

Expected Result:
欠落 texture、紫黒 checker、誤った別 material texture がなく、4部位が本来の Dragon Armor layer に正しく描画される。

### F-008: DragonboneとDragon ScaleのKnapping

#### Description

Dragonbone Tool は Sword、Pickaxe、Axe、Shovel、Hoe の Ice and Fire CE 既存5種だけを対象とする。Dragonbone を材料に専用 Knapping Type を追加し、Axe/Hoe/Pickaxe/Shovel Head と Sword Blade を作る。

Knapping pattern は対応する TFC Rock Knapping Tool Part の形状に準拠する。完成 Tool は Knapped Part と `#c:rods/wooden` を用いる TFC-style assembly とし、既存 IaF 完成 item の性能を維持する。

Fire/Ice/Lightning 属性 Dragonbone Weapon は、完成した通常 Dragonbone Sword に対応 Dragon Blood を追加して作る。Tool Part 段階へ Blood を付与しない。IaF に存在しない属性 Dragonbone Tool は追加しない。

Dragon Scale Armor は12色すべてについて、対応色 Scale から Helmet、Chestplate、Leggings、Boots を直接 Knapping する。中間 Scale Plate は追加しない。pattern は TFC Leather Knapping の同部位 pattern に準拠する。

#### Acceptance Criteria

##### AC-F008-001: Dragonbone Tool Part Knapping

Preconditions:
Dragonbone と Knapping を開始できる環境がある。

Action:
Sword、Pickaxe、Axe、Shovel、Hoe に対応する Knapping pattern を完成させる。

Expected Result:
Sword Blade、Pickaxe Head、Axe Head、Shovel Head、Hoe Head が得られ、各 pattern は TFC Rock Knapping の対応形状に準拠する。その他の Dragonbone Tool Part は追加されない。

##### AC-F008-002: Dragonbone Tool組立

Preconditions:
各 Dragonbone Tool Part と `#c:rods/wooden` がある。

Action:
5種の完成 Dragonbone Tool を組み立てる。

Expected Result:
既存 IaF の Dragonbone Sword/Pickaxe/Axe/Shovel/Hoe が得られ、本来の性能を維持する。旧来の直接 Dragonbone 配置だけで完成 Tool を作る recipe は残らない。

##### AC-F008-003: 属性Dragonbone Sword

Preconditions:
完成 Dragonbone Sword と Fire/Ice/Lightning Dragon Blood のいずれかがある。

Action:
対応 Blood を完成 Sword へ追加する。

Expected Result:
対応する既存 IaF 属性 Dragonbone Sword が得られる。Blood は Knapped Part へ直接適用できず、Sword 以外の属性 Dragonbone Tool は新規追加されない。

##### AC-F008-004: Dragon Scale Armor Knapping

Preconditions:
12色いずれかの Dragon Scale を必要数所持する。

Action:
TFC Leather Armor に準拠した Helmet/Chestplate/Leggings/Boots pattern を Knapping する。

Expected Result:
使用した Scale と同色の既存 IaF Dragon Scale Armor が中間 Plate を経ず直接完成する。別色 Scale では同一 recipe が成立せず、既存 IaF Armor の性能を維持する。

### F-009: Dragon Eggと成長時間

#### Description

Dragon Egg の孵化に必要な経過時間は TFC Calendar へ変換せず、Ice and Fire CE の `eggBornTime` と tick-based age をそのまま使用する。

Fire Egg は Ice and Fire CE 本来の burning 条件に加え、Egg の真下1ブロックにある TFC 熱源の実温度が1500℃以上の場合も有効とする。周囲探索は行わない。

Ice Egg は Vanilla Water に加えて TFC Fresh Water を受け入れ、Salt Water を除外する。Spring Water 等は TFC の Fresh Water tag への所属で判定する。

Lightning Egg は Ice and Fire CE 本来の「空が見える + その位置で雨」を維持し、Thunderstorm を追加条件にしない。

孵化後の Dragon Age は TFC Calendar と同期し、TFC Calendar の1日経過を Dragon Age 1日として扱う。

#### Acceptance Criteria

##### AC-F009-001: Egg孵化時間のIaF維持

Preconditions:
Ice and Fire CE の `eggBornTime` を既定値またはテスト値に設定し、各属性 Egg の環境条件を満たす。

Action:
Game tick を進めて Egg age と孵化時点を観測する。

Expected Result:
孵化判定は IaF の `eggBornTime` tick 値を使用し、TFC Calendar の day length を変更しても必要 tick 数自体は変化しない。

##### AC-F009-002: Fire EggのTFC熱源

Preconditions:
Fire Egg と、真下に温度制御可能な TFC 熱源を設置する。

Action:
真下の温度を1499℃以下と1500℃以上でそれぞれ維持して Egg age を比較する。

Expected Result:
IaF 本来の burning 条件を満たさない場合、1499℃以下では TFC 熱源による進行をせず、1500℃以上では進行する。斜め、横、2ブロック下の熱源だけでは TFC 追加条件を満たさない。

##### AC-F009-003: Ice EggのFresh Water

Preconditions:
Vanilla Water、TFC Fresh Water、Fresh Water tag に属する Spring Water、TFC Salt Water を個別に用意する。

Action:
Ice Egg の凍結・孵化条件を各流体で評価する。

Expected Result:
Vanilla Water と TFC Fresh Water 系では有効で、Salt Water では無効である。判定は具体的な Spring Water ID の列挙ではなく Fresh Water tag の所属を尊重する。

##### AC-F009-004: Lightning Eggの雨条件

Preconditions:
天空が見える場所と見えない場所、雨天と晴天を組み合わせて Lightning Egg を配置する。

Action:
各条件で Egg age を進める。

Expected Result:
天空が見え、かつ `isRainingAt` 相当が成立する場合だけ進行する。Thunderstorm であることは要求しない。

##### AC-F009-005: 孵化後Dragon AgeのTFC Calendar同期

Preconditions:
孵化済み Dragon と操作可能な TFC Calendar がある。

Action:
通常 Game tick の進行量とは独立に TFC Calendar をちょうど1日進める。

Expected Result:
Dragon Age が1日分進行する。Calendar が停止している間は通常 tick だけで同量の Age Day を加算しない。

### F-010: Dragon FeedingとTFC FoodData

#### Description

Dragon Meal は Ice and Fire CE 本来の用途を維持し、肉 ingredient として fresh な TFC Meat を使用可能にする。Rotten 判定が真の TFC Food は Dragon Meal recipe、Dragon への手渡し給餌、落下 item を拾う給餌 AI のいずれでも受理しない。AI が対象選択後に item が腐敗した場合も接触時に再確認する。

Fire/Frost/Lightning Stew は Ice and Fire CE 本来の繁殖専用 item のままとし、プレイヤー向け Food component を新規追加しない。Blaze Rod、Prismarine Crystal、Chorus Fruit 等の元 ingredient は維持し、TFC Overdims 非導入環境のための代替 recipe は追加しない。

ドラゴン由来のプレイヤー可食 item には次の TFC FoodData を用いる。3属性間で値は同一とする。

| Item | Hunger | Saturation | Water | Grain | Vegetables | Protein | Decay Modifier |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Fire/Ice/Lightning Dragon Flesh | 8 | 2.0 | 0 | 0 | 0 | 3.0 | 2.0 |
| Cooked Rice with Fire/Ice/Lightning Dragon Meat | 4 | 1.5 | 20 | 1.7 | 2.0 | 3.0 | 3.5 |

#### Acceptance Criteria

##### AC-F010-001: Fresh TFC MeatによるDragon Meal

Preconditions:
同じ種類の fresh TFC Meat と rotten TFC Meat、Dragon Meal のその他の元 ingredient を用意する。

Action:
それぞれを Dragon Meal recipe へ投入する。

Expected Result:
fresh TFC Meat では Dragon Meal を作成でき、rotten TFC Meat では成立しない。Vanilla/IAF の元から有効な fresh meat route を不必要に無効化しない。

##### AC-F010-002: Dragon給餌時の腐敗拒否

Preconditions:
飼育可能な Dragon、fresh TFC Meat、rotten TFC Meat を用意する。

Action:
手渡し給餌と、地面へ投げた meat を探索する AI の両経路を実行する。

Expected Result:
fresh meat は本来の給餌効果を発生させる。rotten meat は手渡しでも AI でも消費されない。AI が fresh item を target にした後で腐敗した場合も、接触時に消費しない。

##### AC-F010-003: Dragon由来FoodData

Preconditions:
3属性 Dragon Flesh と3属性 Cooked Rice with Dragon Meat を用意する。

Action:
TFC Food capability を取得し、栄養、water、decay、hunger、saturation を検査する。

Expected Result:
Description の FoodData 値が3属性で一致して適用され、TFC の decay system に参加する。Ice and Fire CE 本来の item identity と追加効果がある場合はそれを維持する。

##### AC-F010-004: 属性Stewの繁殖専用性

Preconditions:
Fire/Frost/Lightning Stew と対応 Dragon がいる。

Action:
プレイヤーによる摂取と Dragon の繁殖用途をそれぞれ試す。

Expected Result:
Stew はプレイヤー向け通常 Food として新規摂取可能にならず、対応属性 Dragon の Ice and Fire CE 本来の繁殖用途は維持される。

### F-011: Dragon Seekerとドラゴン管理機能

#### Description

Basic Dragon Seeker は Ice and Fire CE 元 recipe の Skull + 2 Metal Ingot の縦形状を維持し、Netherite Ingot を Steel Ingot へ置換する。

Epic Dragon Seeker は Basic Dragon Seeker と Nether Star を維持し、Netherite Upgrade Smithing Template の代わりに高位 TFC Steel を要求する。Red Steel 版と Blue Steel 版を独立した2 recipe とし、1 recipe 内で Red/Blue Steel を共通 tag または混在 ingredient にまとめない。Smithing の template slot に Red Steel または Blue Steel を使用し、base=Basic Dragon Seeker、addition=Nether Star、result=Epic Dragon Seeker とする。

Legendary/Godly Dragon Seeker が Ice and Fire CE 側に存在する場合、本仕様で明示した Basic/Epic の変更に必要な部分以外は元挙動を維持する。

Dragon Flight、Dragon Horn、Dragon Flute、Dragon Staff、Summoning Crystal の能力、距離、収納・召喚挙動は TFC の移動・重量制限に合わせて弱体化しない。

#### Acceptance Criteria

##### AC-F011-001: Basic Dragon SeekerのSteel化

Preconditions:
Dragon Skull と TFC Steel Ingot 2個を用意する。

Action:
Basic Dragon Seeker の recipe を実行する。

Expected Result:
元 IaF recipe と同じ縦形状で Basic Dragon Seeker を作成できる。Netherite Ingot を要求する旧 recipe は通常取得経路として残らない。

##### AC-F011-002: Epic Dragon SeekerのRed/Blue別recipe

Preconditions:
Basic Dragon Seeker、Nether Star、Red Steel Ingot、Blue Steel Ingot を用意する。

Action:
Red Steel を template slot に使う recipe と Blue Steel を template slot に使う recipe を個別に実行する。

Expected Result:
どちらも Epic Dragon Seeker を1個出力する。Red/Blue Steel を単一の共通 ingredient にまとめた recipe はなく、片方の recipe に両色を混在させる必要もない。Netherite Upgrade Smithing Template を要求しない。

##### AC-F011-003: 飛行と管理能力の維持

Preconditions:
飼い慣らした Dragon と Dragon Horn、Flute、Staff、Summoning Crystal 等の対象 item を用意する。

Action:
騎乗飛行、収納、呼び戻し、命令、召喚を Ice and Fire CE 本来の手順で実行する。

Expected Result:
IaFTFC はこれらの本来の性能、距離、能力を追加制限せず、TFC 統合によって機能を失わせない。

### F-012: Bestiary、Manuscript、Lecternの維持

#### Description

Bestiary はドラゴン関連ページだけに制限せず、Ice and Fire CE 2.1.4 が提供する全ページを維持する。Lectern の候補抽選から非ドラゴンページを除外しない。

Manuscript の供給は Ice and Fire CE 本来の構造物、loot、recipe 等に任せ、IaFTFC が Dragon Roost/Cave へ補償用 Manuscript pool を新規追加しない。

#### Acceptance Criteria

##### AC-F012-001: Bestiary全ページ

Preconditions:
Bestiary、Lectern、十分な Manuscript を用意する。

Action:
Lectern でページ解放候補を繰り返し生成し、解放済み Bestiary を閲覧する。

Expected Result:
Ice and Fire CE 2.1.4 のドラゴン関連・非ドラゴン関連を含む本来の全ページが利用可能であり、IaFTFC 独自のドラゴン限定 filter が存在しない。

##### AC-F012-002: Manuscript供給の非追加

Preconditions:
IaFTFC の Dragon Roost/Cave Loot Table を読み込む。

Action:
IaFTFC が追加した Manuscript 専用 entry または pool を検索する。

Expected Result:
本仕様の補償目的だけで追加された Manuscript entry/pool は存在せず、Manuscript の供給元は Ice and Fire CE 本来の経路に任される。

### F-013: TFC Item SizeとWeight

#### Description

本仕様の対象となる Dragon resource、Dragonsteel form、Tool Part、Armor、Food、管理 item には、用途と形状が同等の TFC item の Size/Weight を適用する。名前だけで一括推定せず、同用途の TFC reference を選ぶ。

Dragonsteel の Ingot、Double Ingot、Sheet、Double Sheet、Rod、Tool Part、Unfinished Armor、Anvil、Plated Block/Slab/Stairs は TFC の同 form と同じ Size/Weight とする。

確定例外は次とする。

| Item class | Size | Weight |
| --- | --- | --- |
| Dragon Egg | very_large | very_heavy |
| Dragon Skull | huge | very_heavy |
| Player Dragonsteel Armor | TFC同部位Armor相当 | TFC同部位Armor相当 |
| Dragon-worn Dragon Armor | very_large | very_heavy |
| Dragonsteel Anvil | TFC Anvil相当 | TFC Anvil相当 |

Dragonbone Part、Dragon Scale、Dragon Blood 等は対応する TFC Tool Part、Leather-like material、filled bottle 等の同用途 reference に合わせる。

#### Acceptance Criteria

##### AC-F013-001: Dragonsteel formのTFC物性一致

Preconditions:
全 Dragonsteel metal form と対応する TFC reference form が登録されている。

Action:
Size/Weight data を form ごとに比較する。

Expected Result:
Ingot、Double Ingot、Sheet、Double Sheet、Rod、Tool Part、Unfinished Armor、Anvil、Plated Block/Slab/Stairs が対応 TFC form と同じ Size/Weight を持つ。

##### AC-F013-002: Dragon resourceの明示分類

Preconditions:
Dragon Egg、Dragon Skull、Dragonbone、Dragon Scale、Dragon Blood、Dragon Armor が登録されている。

Action:
TFC Item Size/Weight を取得する。

Expected Result:
Dragon Egg は very_large/very_heavy、Dragon Skull は huge/very_heavy、Dragon-worn Dragon Armor は very_large/very_heavy である。Dragonbone、Scale、Blood は Description で定めた同用途 TFC reference に一致し、未分類 fallback に依存しない。

##### AC-F013-003: 収納制限への反映

Preconditions:
Size 制限を持つ TFC container と、F-013 で分類した大型 Dragon item を用意する。

Action:
Dragon Egg、Dragon Skull、Dragon-worn Dragon Armor を container へ挿入する。

Expected Result:
TFC の Size/Weight 制限が通常どおり適用され、IaFTFC が item identity を理由に制限を迂回しない。

## Global Requirements

- 本 `PROJECT.md` を modding-harness における製品仕様の正本とする。既存 `AGENTS.md`、README、生成 script、resource、旧実装が本書と矛盾する場合は本書を優先し、同じ変更内で矛盾する文書を更新する。
- 公開 API、registry、tag、JSON/data-driven 定義を優先し、Mixin は Ice and Fire CE の Cave/Roost 直接生成、ブレス block transform、Egg の直接 block 比較等、公開フックだけでは実現できない箇所へ限定する。
- ブレスの地形互換化だけを目的に cancellable な world damage event 全体を cancel してはならない。Entity damage と Dragon Forge Input の flame hit を保持する。
- Ice and Fire CE の完成 Dragonsteel Tool/Armor、Dragonbone Tool、Dragon Scale Armor、Dragon 本体、騎乗・管理 item の性能は、本書で明示的に変更した進行・recipe・TFC data 以外では維持する。
- 公開済み Ice and Fire CE item ID と既存 world の item registration を不必要に削除しない。無効化対象は recipe/worldgen を閉じ、save compatibility を優先する。
- 新規 worldgen は新規チャンクで決定論的に動作し、既存チャンクを暗黙に再生成・置換しない。
- Client 専用 class を common/server path から参照しない。Dedicated Server で IaFTFC、TFC、Ice and Fire CE のみを必須依存として正常に load できること。
- JSON/resource は UTF-8、安定した namespace と ID を使用する。IaFTFC 固有 resource は原則 `iaftfc` namespace とする。他 Mod namespace の data override は対象 Mod の既存 ID を置換する場合に限定する。
- 変更された Feature の Acceptance Criterion は GameTest または必要な E2E test へ対応付ける。描画に依存する AC-F007-004 は E2E 対象とし、それ以外の game state/logic は原則 GameTest 対象とする。
- Dependency 更新時は TFC/Ice and Fire CE の対象 source diff を確認し、関連 GameTest/E2E を再実行してから version range を拡張する。未知の将来 minor/major version を無条件に許可しない。
- `./gradlew compileJava` と `./gradlew build` が成功し、変更した JSON が全件 parse 可能であることを各 milestone の共通完成条件とする。

## Constraints

- 必須 dependency は Minecraft `1.21.1`、NeoForge `>=21.1.248,<21.2`、TerraFirmaCraft `>=4.2.12,<4.3`、Ice and Fire: Community Edition `>=2.1.4,<2.2` とする。
- TFC Overdims は必須・compile・runtime dependency に追加しない。導入されている環境では Blaze Rod、Prismarine Crystal、Chorus Fruit、Nether Star 等の既存 Nether/End 入手経路をそのまま利用し、非導入環境向けの代替 recipe は追加しない。
- 独立した非ドラゴン Mob、非ドラゴン構造物、非ドラゴン固有装備の TFC 再設計は対象外とする。Bestiary の全ページ維持だけは明示的な例外とする。
- Dragon Cave/Roost/ブレスによる TFC 岩石種の消失は許容する。属性別に岩石種を保持する追加 block family は実装しない。
- TFC の重要設備を包括的に Dragon 破壊 blacklist へ追加しない。Ice and Fire CE 本来の blacklist、griefing 設定、`mobGriefing` を尊重する。
- IaF に存在しない Dragonsteel Tool family を追加しない。
- Dragon Egg の hatch timer は TFC Calendar 化しない。TFC Calendar と同期するのは孵化後の Dragon Age とする。
- Copper、Silver、Gold、Diamond、Netherite Dragon Armor の通常取得 recipe は無効化する。特に Netherite Dragon Armor を有効系統へ戻さない。
- Red Steel と Blue Steel は Dragon Chest の汎用 loot に含めない。
- IaFTFC 独自の IAF Silver/Sapphire worldgen を復活させず、ドラゴン統合では TFC Silver/Sapphire を正規資源とする。

## Open Questions

None.
