# Project

Status: active

Project ID: iaftfc
Mod ID: iaftfc

## Platform

Minecraft: 1.21.1
NeoForge: 21.1.248
Java: 21

## Purpose

`iaftfc` は、Ice and Fire: Community Edition 2.1.4 のドラゴン関連コンテンツを TerraFirmaCraft 4.2.12 の世界生成、鉱物、製錬、鍛造、食料、重量および暦へ接続する NeoForge アドオンである。既存の IaFTFC 実装・仕様・アセットは一切引き継がず、`iaftfc` という名称と Mod ID だけを使用する。必須のゲーム内依存関係は `tfc` および `iceandfire` とする。IaF CE がそれ自体の動作に必要とする Jupiter / Uranus 等は IaF CE の既存依存関係に従って配置し、本アドオン独自の前提 Mod にはしない。TFC Overdims、JEI、EMI、Jade 等への依存は設けない。

本書は実装の唯一の製品仕様である。**指示のないゲームデザイン上の変更を禁止する**。元 Mod の挙動を維持する箇所は、指定したリリースに含まれるクラス・データを規範とし、その仕組みに対する互換差分だけを実装する。実装言語、Mixin とイベントの選択などの内部構造は任意とするが、登録 ID、入力、出力、データ、確率、温度、進行段階、表示、セーブ結果および以下の AC は一致しなければならない。

**固定基準版**：IaF CE の 1.21.1 NeoForge 公開リリース `iceandfire-2.1.4.jar`、TFC の 1.21.1 公開リリース `4.2.12`。参照ソースはそれぞれ `https://github.com/IAFEnvoy/IceAndFire-CE/tree/1.21.1` と `https://github.com/TerraFirmaCraft/TerraFirmaCraft/tree/1.21.x`。ブランチ先頭がリリース後に変わった場合は公開 JAR の内容を優先する。`PROJECT.md` の変更なしに他版へ自動追随して仕様を変えない。

## Features

### F-001: 起動、登録および互換処理の適用範囲

#### Description

TFC と IaF CE の両方が存在する Minecraft 1.21.1 環境でロードする。新規登録物の名前空間はすべて `iaftfc` とする。既存 IaF のドラゴン、卵、Dragon Forge、Dragon Armor、Dragonbone、Dragonscale、防具・工具、Bestiary、各種ユーティリティは、明記された例外を除いて登録 ID と基本能力を維持する。IaF の非ドラゴン Mob・構造物の生成、戦利品、食料、装備は変更しない。

対応ワールドは TFC Climate が存在する Overworld とする。TFC 特有の世界生成・自然物変換は Overworld のみ行う。別ディメンションでドラゴンを使用した場合、元 IaF の戦闘・移動・保存・呼び戻しを妨げない。Overdims への必須依存、特別な Nether/End レシピ、代替資源生成は追加しない。

別途明示する元 IaF のレシピ、銀・サファイア生成、無効化対象の装備ルート以外の IaF 内容を包括的に無効化しない。削除するのは**取得経路**であり、既存セーブ上の IaF アイテム ID 自体を消去・登録解除しない。

#### Acceptance Criteria

##### AC-F001-001: 必須依存とロード

Preconditions:
Minecraft 1.21.1、NeoForge 21.1.248、TFC 4.2.12、IaF CE 2.1.4 と IaF CE 自身の必須依存を両側に配置する。

Action:
専用サーバーとクライアントを起動してログインする。

Expected Result:
クラッシュやレジストリ衝突なく起動し、`iaftfc` が読み込まれる。専用サーバーは client-only クラスをロードしない。

##### AC-F001-002: 適用範囲

Preconditions:
ドラゴン3属性と、IaFの非ドラゴン Mob・構造物が存在する。

Action:
属性ドラゴンと非ドラゴンの生成・ドロップ・レシピを検査する。

Expected Result:
変更対象は本書で明記したドラゴン関連機能に限られ、非ドラゴンの機能に独自の変更を加えない。

##### AC-F001-003: 既存セーブの登録 ID

Preconditions:
IaF既存の龍素材、装備、Dragon Horn、Bestiaryを保持するセーブを使用する。

Action:
本アドオンを導入してセーブを開く。

Expected Result:
既存IDのアイテムは消失しない。ただし本書で無効化した製法による新規作成は成立しない。

### F-002: 気候と属性別ドラゴン生成

#### Description

既存の `iceandfire:dragon_cave` および `iceandfire:dragon_roost` Structure Set の random-spread 配置規則、salt、spacing、separation、試行頻度、スポーン地点制限、元 IaF 設定、村回避規則、地形サイズ、ドラゴン年齢・性別・カラーバリエーション選択を維持する。`dragon_cave` の spacing 18、separation 12、salt 9821379、`dragon_roost` の spacing 15、separation 6 は変更しない。1候補から生成する3属性の重みだけをTFC気候へ対応させる。各候補から選ばれる属性は Fire・Ice・Lightning の3種であり、いずれも長期平均約1/3とする。地上Roostと地下Caveは別々の元Structure Set候補を使用する。

**属性の気候選択仕様**：3属性の選択は、季節瞬間温度ではなく TFC Climate の**年間平均気温 T（℃）**、年間降水量 R（mm）、降水量変動 V を使用する。寒いほど Ice、暖かいほど Fire、降雨が多く変動が強いほど Lightning が選ばれやすくする。固定のBiome名で選択しない。平均出現率の補正と確率の再現性を保証するため、次の手順を使用する。

1. ワールドシードごとに一度、Overworld の TFC 気候ノイズ取得 API から、チャンクを実際に生成・ロードせず 4096 地点を標本抽出する。添字 `i=0..4095` ごとに `h=SplitMix64(worldSeed + i*0x9E3779B97F4A7C15)` を計算し、`chunkX=(short)(h & 0xffff)`、`chunkZ=(short)((h >>> 16)&0xffff)`、`x=chunkX*16+8`、`z=chunkZ*16+8` とする。SplitMix64は一般的な固定の64-bitアルゴリズム（加算、二段のxor-shift/multiply、最後のxor-shift）を使用し、Javaの符号付きlongのオーバーフローをそのまま使用する。気候照会にチャンク生成を伴うAPIは使わない。
2. 標本の T、R、V について母平均と母標準偏差を算出し、`zT=(T-meanT)/max(sdT,1e-6)`、`zR=(R-meanR)/max(sdR,1e-6)`、`zV=(V-meanV)/max(sdV,1e-6)` とする。Lightning は `zR+0.5*zV-0.5*abs(zT)`、Fire は `zT`、Ice は `-zT` を適性scoreとする。
3. 3つの補正値 `bF=bI=bL=0` で始め、全標本で `p[k]=exp(clamp(score[k]+b[k],-15,15))/Σexp(...)` を計算する。全標本における各属性の平均確率 `q[k]` に対し、64回、`b[k]+=ln((1/3)/max(q[k],1e-9))` を更新し、毎回 `b` の3値平均を引く。補正値はワールドシード・気候分布ごとにキャッシュする。64-bit double を使用し、iteration順と属性順は Fire, Ice, Lightning とする。
4. 実生成候補の T/R/V で同じ score・softmax を計算し、**候補チャンクとStructure Setのsaltから確定する別個の64-bit SplitMix64乱数**の上位53bitを `[0,1)` の一様値へ変換する。累積確率 Fire→Ice→Lightning の順に1種を選ぶ。これにより属性の偏りが生じても、固定の入力に対する属性は再起動後も変化しない。
5. 各属性専用の元 IaF `Structure` をそのまま生成する。元配置候補自体を増減させず、既存の年齢・性別・戦利品・地形操作を引き継ぐ。標本気候や候補周辺が通常生成可能なTFC Overworldでない場合は元IaFの配置処理を維持する。

標本からの補正は**気候適性を維持しつつ3属性の期待出現率を等しくする**ためのものであり、地形の局所的な不均衡を理由として個々の属性を強制生成しない。測定上の許容範囲は、標本に基づく3属性の平均確率がそれぞれ `[0.32,0.35]` とする。実生成数の統計検査では500以上の生成成功例を対象に各属性比率 `[0.27,0.40]` を許容する。IaF元設定で自然生成が無効のときは無効のままとする。

#### Acceptance Criteria

##### AC-F002-001: 気候選択と再現性

Preconditions:
TFC Overworldの同じシードで、構造物候補座標と T/R/V が既知である。

Action:
属性選択を2回実施し、Fire / Ice / Lightningのscore、確率、選択値を取得する。

Expected Result:
同じ候補から必ず同じ属性を返す。温度を増やすとFireの相対確率が上昇し、温度を下げるとIceが上昇し、降水量・変動を増やすとLightningの相対確率が上昇する。

##### AC-F002-002: 頻度とStructure Set

Preconditions:
固定シード群のTFCワールドで候補配置を列挙できる。

Action:
各Structure Setの配置候補と3属性比率を測定する。

Expected Result:
元のspacing、separation、salt、候補総数、村回避、設定による制限は不変であり、確率補正の標本平均は各種0.32～0.35、十分な成功構造物数では各種0.27～0.40に入る。

##### AC-F002-003: 3属性のRoostとCave

Preconditions:
TFC Overworldで、元IaFの生成設定が有効である。

Action:
指定したシード・座標にFire、Ice、LightningのRoostとCaveを生成する。

Expected Result:
6種が正常生成され、各属性のドラゴン・固有装飾・色・宝箱・年齢とRoost/Caveの別が保たれる。

### F-003: Dragon RoostのTFC地形処理

#### Description

Fire / Ice / LightningのRoostによる既存地形置換を保持する。`#c:stones` は各属性の Charred / Frozen / Crackled Stone、`#c:gravels` は対応Gravel、土壌は対応Dirt、植物・葉は元IaFと同じ除去規則、木材は元IaFのAshまたはFrozen Splintersへ変更する。**岩石ごとの新たな属性ブロックは作らず、元岩石IDの喪失を許容する**。変換はRoost生成対象範囲に限る。TFCの岩石、砂利、土壌を変換できるようにタグ・比較ロジックを補う。巨礫・装飾の地表判定はTFCの地面を判定できるよう一般化するが、元の個数・配置半径は変えない。ブロックエンティティ、破壊不可ブロックおよび保護対象を構造物生成時に無差別に破壊しない。

#### Acceptance Criteria

##### AC-F003-001: 各属性の地表置換

Preconditions:
各属性のRoost範囲にTFC生岩・砂利・土・植物・原木を配置する。

Action:
Roostを生成する。

Expected Result:
TFC生岩・砂利・土は属性別IaFブロックへ置換され、元IaFの木・植物処理が成立し、岩石ごとの属性ブロックは追加されない。

##### AC-F003-002: 生成装飾と地表判定

Preconditions:
草地・TFC石地表・TFC砂利地表にRoostを生成する。

Action:
生成後の巨礫、巣、装飾、ドラゴン、Pile、Chestを確認する。

Expected Result:
元IaFと同数・同規則で配置され、Vanilla `grass_block` 等に限定した判定による地中貫通や未生成が起こらない。

### F-004: Dragon Caveの地質、鉱石および宝物

#### Description

Caveの球状外殻、属性石、空洞掘削、床装飾、Chest、Pile、ドラゴン生成、Y値とConfig offsetはIaFを維持する。地質を元へ復元しない。Caveの「鉱石ブロックを選択・設置する枝」だけを置換し、Vanilla 鉄・銅・金・石炭・Redstone・Lapis・Diamond・Emerald鉱石、IaF Silver / Sapphire鉱石および Budding Amethyst を**Cave内部の配置結果として残さない**。

Caveで鉱石配置を行うたび、次の9種類を**整数0～8の一様抽選**で選ぶ：Amethyst, Diamond, Emerald, Lapis Lazuli, Opal, Pyrite, Ruby, Sapphire, Topaz。各確率はちょうど1/9である。各GemはTFCで定義される原石鉱石の**normal品位**とし、属性ごとの母岩を Fire=`basalt`、Ice=`quartzite`、Lightning=`diorite` に固定する。すなわち、TFCの岩石別鉱石ブロックIDを使用し、生成位置の元岩石・周囲の地質・IaFのCommon/Uncommon/Rare分類によって母岩、Gem候補、確率を変えない。配置**回数・位置・生成確率の元IaF処理は変更しない**。9種から選ぶ乱数は、位置と洞窟シードに基づき安定に導出する。

外殻に直接埋める鉱石は通常のTFC鉱石ブロックの採掘・Loot仕様を使用する。TFC内にそのGemの指定母岩・品位IDが存在しない場合、独自の偽物ブロックで代用せず、対象TFC版のレジストリが示す正規normal鉱石IDを選ぶ。指定3岩石はTFC公式の岩石種である。

#### Acceptance Criteria

##### AC-F004-001: 属性別母岩

Preconditions:
各属性Caveの鉱石配置位置を十分数記録する。

Action:
生成結果の鉱石ブロックとGem名を検査する。

Expected Result:
FireはBasalt、IceはQuartzite、LightningはDiorite母岩のTFC Normal Gem鉱石のみとなる。9種以外とVanilla/IaF鉱石は生成されない。

##### AC-F004-002: 等確率と独立性

Preconditions:
9種それぞれの選択枝を固定乱数で注入できる。

Action:
0～8の値を順に適用し、3属性、元鉱石class、元地層を変えて検証する。

Expected Result:
9種の選択結果が1対1に対応し、選択は均等で元分類や周囲の地質に影響されない。

##### AC-F004-003: Cave本体の保存

Preconditions:
TFCの岩盤と通常のIaF Caveを比較できる。

Action:
両者の洞窟形状、属性外殻、チェスト数、Pile数、ドラゴン年齢・性別を確認する。

Expected Result:
鉱石の種類を除いて元IaFのCave仕様を維持し、空洞と外殻は生成される。Cave生成時にBlock Entityを消去しない。

### F-005: 独立Silver・Sapphire生成の禁止

#### Description

IaF CE の Silver Ore と Sapphire Ore の Overworld自然鉱脈を、`neoforge:biome_modifier` とfeature参照の削除・置換によって**新規生成しない**。IaFの銀・サファイア鉱石ブロック／アイテムはレジストリから除外しないが、アドオンの通常進行では使用しない。SilverはTFCのNative Silver、SapphireはTFCのGem/Oreを使用する。IaFの古いSilver Ingot、Nugget、Raw Silver、Sapphire Gemを材料にするドラゴン関連レシピはTFC側へ置換し、逆変換製法を残さない。

#### Acceptance Criteria

##### AC-F005-001: 鉱脈と素材の単一化

Preconditions:
TFC Overworldの十分なサンプルチャンクを生成する。

Action:
IaF Silver/SapphireのBiome Modifierが適用されていないことと、ドラゴン関連レシピの入力を検査する。

Expected Result:
IaF固有Silver/Sapphire鉱脈は新規生成されず、TFC Silver/Sapphireは元の仕組みで生成する。ドラゴン関連製法はIaF固有銀・サファイアを要求しない。

### F-006: Roost/CaveのLoot Table再設計

#### Description

対象は `iceandfire:chest/{fire|ice|lightning}_dragon_{roost|female_cave|male_cave}` の**9テーブルだけ**とする。TFCのAncient City、YUNG's構造物等、本アドオン対象外のLootは変更しない。IaF CE 2.1.4 JAR の9つの元テーブルを変換元として使用し、元テーブルを全面置換する。各元poolのrolls/bonus_rolls、entryの数、weight、conditions、count provider、元Mod固有ドラゴンアイテム、ドロップ用途を保つ。既存の1entryを同一weightのnested table参照に変えることは許可する。1entryを複数の親entryへ増やして重みを変えない。

Profileは `roost=ordinary`、`female_cave=dangerous`、`male_cave=lategame`。汎用Ingot候補は `ordinary=[copper,bronze,bismuth_bronze,black_bronze,wrought_iron]`、`dangerous=[wrought_iron,steel]`、`lategame=[steel,black_steel]`。各候補は等weight。汎用装備の素材は `ordinary=[copper,bronze,bismuth_bronze,black_bronze]`、`dangerous=[wrought_iron]`、`lategame=[steel]`。Red/Blue SteelやDragonsteelは**汎用Lootに含めない**。

属性を表す元entryだけは固定置換する：FireのEmerald→`tfc:gem/ruby`、IceのIaF Sapphire Gem→`tfc:gem/sapphire`、LightningのAmethyst Shard→`tfc:gem/topaz`。同様にFire Gold Ingot→`tfc:metal/ingot/gold`、Ice Silver Ingot→`tfc:metal/ingot/silver`、Lightning Copper Ingot→`tfc:metal/ingot/copper`。属性Nuggetはそれぞれ `tfc:ore/small_native_gold`、`tfc:ore/small_native_silver`、`tfc:ore/small_native_copper`。それ以外のDiamond等の汎用宝石は9種から等確率の`shared/gems`へ、Iron Ingotは対応Profileの`shared/ingots`へ送る。元の固定×10、×16、`uniform 1..25`等はそのまま保持する。

汎用Gem候補は正確に `amethyst,diamond,emerald,lapis_lazuli,opal,pyrite,ruby,sapphire,topaz` のTFC Gemアイテム9種で各weight=1。原Gemの品位変更、連続抽選、追加金属はしない。元のCoal系は `shared/coal_like`（`minecraft:charcoal`, `tfc:ore/bituminous_coal`, `tfc:ore/lignite`, `tfc:ore/graphite` の対応する採掘時アイテムの正規IDをレジストリから参照。粉末Graphiteは禁止）とし4候補等weight。元の一般的なUtility・Obsidian等に限り `shared/utility`（`tfc:powder/flux`, `minecraft:glass`, `minecraft:glass_pane`, `tfc:kaolin_clay`, `tfc:powder/wood_ash`, `tfc:powder/saltpeter`, `tfc:powder/sylvite`）へ置換する。Utilityを新しい独立poolとして追加しない。Coal系とGraphiteはUtilityに含めない。元アイテムが指定カテゴリにない場合、ドラゴン固有物は維持し、汎用物はカテゴリ一致の変換表に従う。独自に数量や総報酬額を調整しない。

汎用Tool familyは `pickaxe↔{pickaxe,propick}`、`axe↔{axe,saw}`、`hoe↔{hoe,scythe}`、`sword↔{sword,javelin,mace}`、`shovel↔{shovel}`、`knife↔{knife}`、`shears↔{shears}`、`shield↔{shield}`、`bow↔{bow}`、`crossbow↔{crossbow}`。family内候補は等確率。TFCに中間部品のないBow/Crossbowは完成品のみとする。金属Toolは加工途中品 : 完成品=2:1、Armorは部位固定でUnfinished : 完成品=2:1。例：Pickaxe = Pickaxe Head 1/3 + Propick Head 1/3 + Pickaxe 1/6 + Propick 1/6。Sword = Sword Blade・Javelin Head・Mace Head各2/9、完成Sword・Javelin・Mace各1/9。HelmetをBootsへ置換するような部位変更は禁止する。元Enchantは親参照tableに置いてよいが、TFCでEnchant不能なHead/Unfinishedには実際の付与を行わず、完成品へだけ元のEnchant仕様を作用させる。元装備Enchantのレベル・数値は維持する。

**詳細変換規則は本書のF-006およびGR-005だけで完結する**。原IaFの9テーブルとそのcount/weight/conditionsを固定版JARから読み取る。他ModのLootや旧IaFTFCの変換スクリプトを参照・変更しない。Enchantは「tableに置いてよいが未完成品へ効果を付けない」規則を採用する。共有Loot Tableは `iaftfc:shared/gems`, `iaftfc:shared/ingots/{profile}`, `iaftfc:shared/coal_like`, `iaftfc:shared/utility`, `iaftfc:shared/equipment/{profile}/{family}` に登録する。

#### Acceptance Criteria

##### AC-F006-001: 9テーブルの完全性

Preconditions:
IaF CE 2.1.4の9つの元Loot JSONと生成後の9つの置換JSONがある。

Action:
正規化したJSON ASTでpool数、rolls、bonus_rolls、元entry数、weight、count provider、conditions、固有アイテムを比較する。

Expected Result:
カテゴリ置換を除いて各値は一致し、元の固定数量と範囲は維持され、新しいpoolや意図しない固有アイテム削除がない。

##### AC-F006-002: Profileと装備進行

Preconditions:
各Profileで対象Ingot/Tool/Armorのnested tableを呼び出す。

Action:
候補集合、等weight、未完成と完成の比率を検査する。

Expected Result:
Ingotは上記候補集合だけ、装備はordinaryがCopper/Bronze系、dangerousがWrought Iron、lategameがSteelだけとなる。各金属装備では2:1を満たし、部位とTool familyを維持する。

##### AC-F006-003: 属性宝石・Ingot・Nugget

Preconditions:
Fire/Ice/LightningのRoost・Female Cave・Male Caveに該当entryがある。

Action:
置換結果と元countを検査する。

Expected Result:
属性固定entryは指定されたTFC素材、一般Gemは9種等確率、Iron IngotはProfileへ置換され、Red/Blue Steel・Dragonsteel・Vanilla Iron Ingot・IaF旧Silver/Sapphireが出ない。

##### AC-F006-004: Enchantと形式

Preconditions:
装備にEnchant functionのあるCave元entryを利用する。

Action:
未完成装備と完成装備の抽選を実行する。

Expected Result:
Head/UnfinishedにEnchantが付かず、完成装備には元設定が適用される。BowとCrossbowは混在せず、Graphite Powderは出現しない。

### F-007: Gold/Silver/Copper Pile

#### Description

IaFの`gold_pile`、`silver_pile`、`copper_pile`を引き続きRoostとCaveに生成する。各1 layerあたり、従来の金属Nugget 2個の代わりに対応するTFC Small Native Ore 2個をドロップする。対象は`gold→tfc:ore/small_native_gold`、`silver→tfc:ore/small_native_silver`、`copper→tfc:ore/small_native_copper`。layer 1～8と元の破壊・Silk Touchその他の状態を維持する。層数に応じて最大16個を返す。IaF Silver/Copper Nuggetからのインゴット変換をTFC資源生成に利用しない。

#### Acceptance Criteria

##### AC-F007-001: 1～8層のドロップ

Preconditions:
3属性Pileの各layer数1～8を生成する。

Action:
生存モードで破壊してドロップを数える。

Expected Result:
各Pileは対応するSmall Native Oreを`2*layers`個ドロップし、NuggetやIngotを直接出さない。layerごとの元仕様を維持する。

### F-008: Dragonsteel金属の新規実装

#### Description

Dragonsteel Fire / Ice / Lightning はすべて`iaftfc`名前空間の**独立したTFC式金属**とする。正式な金属キーは `dragonsteel_fire`, `dragonsteel_ice`, `dragonsteel_lightning`。旧IaF `iceandfire:dragonsteel_{fire|ice|lightning}_ingot` と同Blockを新規取得・製錬の正規出力にせず、旧IDを維持したまま取得経路を無効化する。独自に登録するIDは各金属について次のとおりとする。

| 用途 | 登録IDパターン |
|---|---|
| Ingot | `iaftfc:metal/ingot/<metal>` |
| Double Ingot | `iaftfc:metal/double_ingot/<metal>` |
| Sheet | `iaftfc:metal/sheet/<metal>` |
| Double Sheet | `iaftfc:metal/double_sheet/<metal>` |
| Rod | `iaftfc:metal/rod/<metal>` |
| Molten Fluid | `iaftfc:metal/<metal>`（Source/Flowing、TFC標準Fluid Container連携） |
| TFC式Metal Block | `iaftfc:metal/block/<metal>` |
| Anvil Block | `iaftfc:metal/anvil/<metal>` |
| Sword Blade | `iaftfc:metal/sword_blade/<metal>` |
| Pickaxe Head | `iaftfc:metal/pickaxe_head/<metal>` |
| Axe Head | `iaftfc:metal/axe_head/<metal>` |
| Shovel Head | `iaftfc:metal/shovel_head/<metal>` |
| Hoe Head | `iaftfc:metal/hoe_head/<metal>` |
| Unfinished Helmet/Chestplate/Greaves/Boots | `iaftfc:metal/unfinished_<part>/<metal>` |

`<metal>`は上記3キーのいずれか。金属のTierは**7**、融点は**2000 ℃**、鍛造可能温度は**1200 ℃以上かつ溶融前**、溶接可能温度は**1600 ℃以上かつ溶融前**。Heat CapacityはTFC Red Steel Ingotと同じ `2.857143` をIngot基準とし、Double Ingot / Sheet / Double Sheet / Rod / 頭部 / 未完成防具をTFCの対応部品と同じ金属量比で計算する。元IaFのDragonsteel技術値・属性能力は、最終製品IDを通じて引き続き使用する。3金属の合金混合・相互変換レシピは作らない。

TFC単位は**Ingot=100 mB**, Double Ingot=200 mB, Sheet=200 mB, Double Sheet=400 mB, Rod=50 mB、Metal Block=100 mB、Anvil=1400 mBとする。Source/Flowingのフルバケツ・Mold容量はTFCの既存金属Fluid実装と一致させる。溶融→鋳造→再溶融の物質保存を必須とする。金属再鋳造は既存TFC Ingot Moldを使用できるようにする。TFC標準のCoal燃料は最高1415℃、Bellowsの目標温度上昇は最大600℃で、雨天でなければCharcoal Forgeの到達目標は2015℃となる。従って、最高温度近傍でBellowsを継続操作することでTFC Charcoal Forge→上部Crucibleによる2000℃の再溶融を実現する。単独Charcoal Forge（Bellowsなし）やCharcoal燃料（1350+600=1950℃）からDragonsteelが溶融するルートは追加しない。温度の上限自体を変えたり、Dragon ForgeがTFC Crucibleに独自の無償熱を与える機能を追加したりしない。`tfc:item_heat`, `tfc:fluid_heat`など実際のデータ機構・タグに登録する。Item/Fluidの温度状態は再ログイン後も維持する。

Metal BlockはTFC Red Steelと同型の装飾金属ブロックとし、`tfc:advanced_shaped_crafting`で`[" SH","SWS"," S "]`（`S=対応Dragonsteel Sheet`,`W=#minecraft:planks`,`H=#c:tools/hammer`）から**Metal Block8個**を得る。工具の損傷Remainder、`input_column=2`などはTFC Red Steel金属Blockに一致させる。Sheet4枚（計800mB）→Metal Block8個（計800mB）で物量保存する。Metal Block1個の溶融は100mB、Dragonsteel Anvil1個は1400mBへ戻る。Block↔Ingotの9個収納レシピは作らない。

追加しないもの：IaFに存在しないPropick、Saw、Hammer、Scythe、Knife、Javelin、Mace、Chisel、Shield、Tuyere、Lamp、Bars、Chain、Trapdoor等のDragonsteel専用品。TFC式金属加工に必要な上表の部品と設備だけを追加する。上述の対象外型を生成する包括的なRegistryMetal自動展開は禁止する。

#### Acceptance Criteria

##### AC-F008-001: 登録集合とTier

Preconditions:
3属性のレジストリを列挙する。

Action:
全指定Item/Block/FluidとTier・温度を調査する。

Expected Result:
指定した3金属×上表の登録が存在し、TFCのMetal/Heat/AnvilシステムでTier 7・融点2000 ℃・鍛造1200 ℃・溶接1600 ℃として扱われ、未指定のTFC専用工具は登録されない。

##### AC-F008-002: 溶融・再鋳造

Preconditions:
Fire/Ice/LightningのIngotとTFC Crucible、Moldを用意する。

Action:
Coal燃料のTFC Charcoal ForgeへBellowsで継続送風し、上部Crucibleの実測温度が2000℃を越えた状態で各Ingotを溶融して100 mBを確認し、鋳造・冷却・再溶融する。Charcoal燃料およびBellowsなしのCoal燃料では溶融しないことも検証する。

Expected Result:
同属性のIngot1個へ戻る。Metal Block8個の製作・800mBへの溶融、Anvil1個の1400mBへの溶融も成立し、流体量が保存され、交差属性・Vanilla Iron Ingot・旧IaF Ingotへ変換されない。Coal+Bellows条件で溶融が成立し、Charcoal+Bellows（目標1950℃）やCoal単独（1415℃）では溶融しない。

##### AC-F008-003: Item Heatと表示

Preconditions:
各金属のIngot・Double Ingot・Sheet・Double Sheet・Rod・Blade・Head・未完成防具を加熱する。

Action:
温度tooltip、鍛造・溶接状態、インベントリ保存を検査する。

Expected Result:
TFC標準と同じ区分で温度が表示され、1200/1600/2000 ℃の境界が正しく処理され、再ログイン後に熱・金属量が維持される。

### F-009: Dragon Forgeと素材レシピ

#### Description

IaFのFire / Ice / Lightning Dragon Forge（Brick、Input、Core、Input BEの龍息供給、Core BEの3スロット、マルチブロック判定、GUI、cookTime、属性一致）を既存IDと挙動のまま使用する。変更するのは入力レシピとDragonsteel正規出力だけである。

各属性Dragonsteelのレシピは `iceandfire:dragonforge` typeで `input=1 tfc:metal/ingot/{red_steel|blue_steel|black_steel}`、`blood=1 iceandfire:{fire|ice|lightning}_dragon_blood`、`cookTime=1000`、`result=1 iaftfc:metal/ingot/dragonsteel_{fire|ice|lightning}` とする。順序対応は Fire←Red Steel、Ice←Blue Steel、Lightning←Black Steel。元の `minecraft:iron_ingot + blood → iceandfire:dragonsteel_*_ingot` を**同一recipe IDで上書き**する。

属性Dragon BloodをScaleと空Glass Bottleから製造するDragon Forgeレシピは元IaFの `cookTime=750` を維持し、Scaleタグ・個数・Bottle個数・出力個数を変えない。Dragon Forge Brickの元レシピは属性Dragon Scale Block `S` とStone Bricks `B` の`SBS / BSB / SBS`で結果4個。`B`だけを `#minecraft:stone_bricks`（TFCのStone Bricks登録を含む既存タグ）へ変更し、`minecraft:stone_bricks`の直接要求を外す。Inputレシピは`BIB / I I / BIB`を維持し、`I`は`#c:ingots/wrought_iron`、Coreは元レシピの`BBB / BHB / BBB`で`H`は対応Dragon Heartを維持する。属性Forgeのレンガなどに任意の新素材を要求しない。Ice/Lightningにも同じ材料置換を適用する。

#### Acceptance Criteria

##### AC-F009-001: Forgeによる属性金属変換

Preconditions:
各属性の完成Forgeと、対応する基礎Steel Ingot、Dragon Blood、Dragonを用意する。

Action:
DragonのブレスをInputへ当て、1000 cookTimeを消費する。

Expected Result:
基礎IngotとBlood各1個から対応する`iaftfc` Dragonsteel Ingot1個ができる。異属性組み合わせ、Vanilla Iron、旧IaF Ingotは成立しない。

##### AC-F009-002: 設備の製作と動作

Preconditions:
TFCの各種Stone Bricks、Wrought Iron Ingot、Dragon Scales Block、Dragon Heartがある。

Action:
3属性のBrick・Input・Coreを作り、Forgeを組み立てる。

Expected Result:
Brickは1回4個、Input・Coreは1回1個であり、元IaFの3属性構造、GUI、Dragonの誘導・ブレス検知が動作する。

##### AC-F009-003: Bloodの代替製法

Preconditions:
対応属性のDragon ScalesとGlass BottleをForgeへ投入する。

Action:
750 cookTime完了させる。

Expected Result:
IaF本来の属性Blood1個が得られ、3属性それぞれで成立する。

### F-010: Dragonsteel鍛造、溶接および金床

#### Description

DragonsteelにTFC既存のAnvil/Welding規則を**入力部品を変える以外そのまま**実装する。Anvil生成は `Double Ingot ×7` を `### /  #  / ###` で配置し、`iaftfc:metal/anvil/<metal>`を1個出力する。金床はTFCの通常Anvil UI、Hammer、作業progress、鍛造ルール、溶接に利用できるTier 7金床である。TFCの作業順序と既存Anvil/Heat APIで実装し、独自GUI・独自ハンマーは追加しない。

Ingot×2→Double IngotのWeldingは**Tier 6金床**（`max(0,tier-1)`）かつ両入力1600℃以上が必要。Sheet×2→Double SheetもTFC標準と同じTier 6。Double Ingotの鍛造によるSheet、IngotのRod×2などの通常Anvil工程は**Tier 7**。Sword BladeはDouble Ingot×1、その他4種類のHeadはIngot×1を入力し、Forge Ruleは同型のTFC Red Steelレシピのルール配列を同順で使用する。下表を厳密に適用する。

| 出力 | 入力 | Anvil Tier | Forge Rules（TFC JSON上の順） |
|---|---|---:|---|
| Pickaxe Head | Ingot | 7 | `punch_last`, `draw_not_last`, `bend_not_last` |
| Shovel Head | Ingot | 7 | `punch_last`, `hit_not_last` |
| Axe Head | Ingot | 7 | `punch_last`, `hit_second_last`, `upset_third_last` |
| Hoe Head | Ingot | 7 | `punch_last`, `hit_not_last`, `bend_not_last` |
| Sword Blade | Double Ingot | 7 | `hit_last`, `bend_second_last`, `bend_third_last` |
| Unfinished Helmet | Double Sheet | 7 | `hit_last`, `bend_second_last`, `bend_third_last` |
| Unfinished Chestplate | Double Sheet | 7 | `hit_last`, `hit_second_last`, `upset_third_last` |
| Unfinished Greaves | Double Sheet | 7 | `hit_any`, `draw_any`, `bend_any` |
| Unfinished Boots | Sheet | 7 | `bend_last`, `bend_second_last`, `shrink_third_last` |

TFC本体のData Generatorで出力されたJSONのルールを正本とし、実装時にこの表と一致することを機械的に確認する。TFCと同じForge Bonus適用フラグを設定する。完成プレイヤー防具のWeldingはすべてTier 6（TFC標準）とし、`Unfinished Helmet+Sheet → Helmet`、`Unfinished Chestplate+Double Sheet → Chestplate`、`Unfinished Greaves+Sheet → Leggings`、`Unfinished Boots+Sheet → Boots`。完成品の出力IDはF-011で固定した**IaF既存アイテム**を使用し、Forge Bonusは`copy_best`で転写する。

**重要**：Tier 6金床で作れるのはIngot→Double Ingot、Sheet→Double Sheet、上記Unfinished防具とのWelding等、TFC元規則がTier-1である工程だけである。Double Ingot→Sword Blade、Head、Sheet、未完成防具等の鍛造はTier 7金床を必要とする。この順序で最初のDragonsteel Anvilを製作できる。

#### Acceptance Criteria

##### AC-F010-001: Tier 6からTier 7への移行

Preconditions:
1600℃以上のDragonsteel Ingot14個とTier 6 Red/Blue Steel金床がある。

Action:
7回WeldingしてDouble Ingot7個を作り、`### / # / ###`で金床をクラフトする。

Expected Result:
Tier 6だけで最初のTier 7 Dragonsteel金床1個ができ、その金床でTier 7鍛造が行える。

##### AC-F010-002: Tool Head鍛造ルール

Preconditions:
Tier 6とTier 7金床、各属性の熱いIngotとDouble Ingotを用意する。

Action:
5種類のBlade/Headを表のルールで鍛造する。

Expected Result:
Tier 6では鍛造が不成立、Tier 7では指定数の金属とForge Rule・Forge Bonusを用いて各1個ができる。

##### AC-F010-003: 未完成防具と溶接

Preconditions:
各部位に必要な熱いSheet/Double Sheet、Tier 7金床を用意する。

Action:
4種類のUnfinished Armorを鍛造し、対応SheetをTier 6以上で溶接する。

Expected Result:
各部位の入力数量・作業ルールがTFC標準と一致し、最終的にIaFの対応Dragonsteel Armorを1個得る。熱・ボーナス・材料を不正に複製しない。

### F-011: Dragonsteel武器・道具・プレイヤー防具

#### Description

完成品はIaF CEの**既存登録アイテムID**を使用し、攻撃力、耐久値、採掘速度、Enchantability、属性攻撃・属性防護能力、各種Config倍率、特殊効果をそのまま維持する。既存アイテムを複製した同等品は新規登録しない。各属性の対象は `iceandfire:dragonsteel_<element>_{sword,pickaxe,axe,shovel,hoe,helmet,chestplate,leggings,boots}` の9種である。計27種を対象とする。IaFにない専用Toolを追加しない。

旧IaFのDragonsteel Ingotを直接並べる通常Craftingレシピ（ToolとArmor、Dragonsteel Block相互変換）はすべて無効化する。Sword/Pickaxe/Axe/Shovel/HoeはF-010の鍛造済みBlade/Headに**TFCの同種Toolの標準棒材**を組み合わせる1個出力のレシピだけを採用する。SwordはSword Blade、PickaxeはPickaxe Head、AxeはAxe Head、ShovelはShovel Head、HoeはHoe Headが入力。棒材はTFCの `#c:rods/wooden` 等、TFC同用途が利用する正規Ingredientを同一条件で使用し、金属棒の代用を許さない。完成品のForge Bonusは出力時に正しく移す。プレイヤー防具はF-010の未完成部品+溶接だけで得られる。ドラゴン本体用ArmorはF-014の**別の製法**であり混同しない。

完成済みの各IaF Dragonsteel Tool・Player ArmorもTFCの加熱・再溶融対象とする。固定金属量はSword200mB、Pickaxe/Axe/Shovel/Hoe各100mB、Helmet600mB、Chestplate800mB、Leggings600mB、Boots400mBとする。いずれもTFC Red Steel同品目の溶融量に一致し、融点2000℃で対応属性のMolten Dragonsteelへ変換する。耐久の減少で回収金属量を増やしたり、鍛造BonusやEnchantを金属流体へ引き継いだりしない。

TFC側の採掘Tier・道具用途タグおよび適切な攻撃ダメージ種別に対応させるが、元IaFの属性Abilityを失わせない。採掘可能範囲は少なくともTFC Red/Blue Steelの上限を満たす。既存のIaF特殊効果によってワールドの土壌や金属工程を不正に迂回する追加レシピは設けない。

#### Acceptance Criteria

##### AC-F011-001: 5種の工具の製作

Preconditions:
3属性×5種のHead/Bladeと適合するTFC棒材を用意する。

Action:
それぞれの組み立てレシピを実行する。

Expected Result:
15種の既存IaF完成Toolが1個ずつ生成され、通常Craftingによる旧Ingot直結製法では生成されない。

##### AC-F011-002: 4部位の防具の製作

Preconditions:
F-010の各Unfinished Armorと溶接材料がある。

Action:
3属性×4部位の溶接を実行する。

Expected Result:
12種の既存IaF完成Player Armorが1個ずつ生成され、元IaFの通常Craftingでは生成されない。

##### AC-F011-003: 性能・属性・採掘互換

Preconditions:
同版IaF CE単体の完成装備と本アドオン導入環境の同ID完成装備を用意する。

Action:
耐久値、攻撃・防御値、属性Ability、ドラゴン戦闘、TFC採掘を比較する。

Expected Result:
IaF固有数値と属性Abilityが一致し、TFC金属Tierに整合する対象ブロックを採掘できる。IaFにないToolは増えない。

### F-012: DragonboneのKnappingおよび属性武器

#### Description

既存の `iceandfire:dragonbone` を独自Knapping材料として扱う。Knapping Typeは `iaftfc:dragonbone`、5×5グリッド、初期25セルすべて保持、押下したセルを除去し、TFC Rock Knappingと同じ操作・UI規則を採用する。1回のKnappingにつきDragonbone原料**1個**を消費し、部品1個を生成する。未消費・中断時の挙動はTFC Rock Knappingに準ずる。可能な道具はSword、Pickaxe、Axe、Shovel、Hoeの5種のみ。部品は `iaftfc:dragonbone/{sword_blade,pickaxe_head,axe_head,shovel_head,hoe_head}` とし、元IaF完成品へTFC標準の棒材1個で組み立てる。

5種類の**完成パターン**は以下のとおりである。`#`は残すセル、空白は除去するセル。短い行は右側の空白で5文字まで埋め、5行未満は下側を空白で埋める。Axe、Hoe、ShovelはTFC Rock Knappingの同名原型を採用する。TFCに石のPickaxeとSwordがないため、この2種だけTFC陶器のPickaxe Head Mold、Sword Blade Moldの形状をKnapping操作の完成形として用いる。これによって「Rock Knappingと同型」を5種について一意に定める。

```text
AXE                     PICKAXE                 SHOVEL
 #                      #####                   ###
####                     #   #                   ###
#####                     ###                    ###
####                     #####                   ###
 #                                              #

HOE                     SWORD
#####                     ###
   ##                      ##
                            #
                            #
                          ####
```

上記のSWORDは次の厳密な行列を採用する：`["  ###","   ##","#   #","##  #","#### "]`。PICKAXEは `["#####","#   #"," ### ","#####","     "]`。AXEは `[" #   ","#### ","#####","#### "," #   "]`。SHOVELは `["###  ","###  ","###  ","###  "," #   "]`。HOEは `["#####","   ##","     ","     ","     "]`。**このJSON風の行列を図示の余白より優先する。**パターンの鏡映・回転はTFCの該当Knapping recipeが許す場合に限り許可し、新しい簡単な代替パターンは追加しない。

完成品はIaF既存の `iceandfire:dragonbone_{sword,pickaxe,axe,shovel,hoe}`。元のDragonbone直接Craftingは無効化する。属性付きDragonbone Swordは従来どおり**完成Sword 1個 + 対応Dragon Blood 1個 → `iceandfire:dragonbone_sword_{fire|ice|lightning}` 1個**のShapelessとし、Sword BladeやKnapping画面にはBloodを使用しない。IaFの属性武器性能と効果は維持する。Dragonbone BowとArrow、他のドラゴン依存アイテムは元IaFの仕様を維持し、無指定のTFC式Knappingを追加しない。

#### Acceptance Criteria

##### AC-F012-001: 5種類のKnapping

Preconditions:
Dragonbone1個と対応5×5パターンを用意する。

Action:
各パターンでセルを除去して完成させる。

Expected Result:
対応する部品が各1個生成され、Dragonbone1個が消費される。不一致形状、未設定の道具種別、別素材では生成しない。

##### AC-F012-002: 組み立てと属性化

Preconditions:
5種類のDragonbone部品、木製棒材、完成Sword、3属性Dragon Bloodを用意する。

Action:
各部品を組み立て、Swordだけを属性付与する。

Expected Result:
既存IaFの5Toolを得る。属性Swordは完成Sword+Bloodだけで生成され、未完成Blade+Bloodでは不成立となる。

### F-013: Dragonscaleプレイヤー防具

#### Description

既存Dragon Scaleの12色（Fire: Red/Green/Bronze/Gray、Ice: Blue/White/Sapphire/Silver、Lightning: Electric/Amethyst/Copper/Black）から、既存IaFの同色プレイヤー防具4部位を直接Knappingする。Knapping Type `iaftfc:dragonscale` はTFC Leather Knappingと同じ5×5のUI・セル削除を使用する。Scale Plateのような中間アイテムを追加しない。1回のKnapping入力は**同色Dragon Scales 1個**、結果は対応完成防具1個とし、通常Craftingの元レシピを無効化する。元IaF防具の性能、属性耐性、Set効果、名前、Texture、IDを維持する。

各部位の正確な完成パターン（`#`は残存セル）は次のとおり。TFC Leather Knapping 1.21.1 のレシピをそのまま使用する。

```text
helmet:     ["#####","#   #","#   #","     ","     "]
chestplate: ["#   #","#####","#####","#####","#####"]
leggings:   ["#####","#####","## ##","## ##","## ##"]
boots:      ["##   ","##   ","##   ","#### ","#####"]
```

色によってKnappingパターンと素材消費量を変更しない。防具の色は入力ScaleのIDで決定する。TFC Leather素材からDragon Scale Armorを生成しない。

#### Acceptance Criteria

##### AC-F013-001: 12色×4部位

Preconditions:
12色のScalesと4つの部位パターンを用意する。

Action:
各色・各部位でKnappingを行う。

Expected Result:
同色の既存IaF防具48種をすべて生成でき、Scaleが各1個消費される。中間Scale Plateは存在せず、Leatherや別色Scalesは該当色の入力にならない。

##### AC-F013-002: 元性能と経路の封鎖

Preconditions:
完成したScales Armor4部位をEquipする。

Action:
元IaFの装備効果と旧Craftingレシピを検査する。

Expected Result:
元IaFと同じ効果・Textureが維持され、従来の通常Craftingでは直接製作できない。

### F-014: Dragon本体用Dragon Armorの素材体系

#### Description

装着部位はIaF標準の `head` / `neck` / `body` / `tail`、装着・耐久・レンダリング・防御計算・ドラゴン成長との関係はIaFを維持する。有効な素材とProtection値は次のとおりである（Protectionは**各素材のIaF DragonArmorMaterial相当の値**であり、プレイヤー防具のarmor pointではない）。

| 素材キー | Protection | 完成アイテムID | 素材入力 |
|---|---:|---|---|
| iron | 1.5 | `iceandfire:dragonarmor_iron_<part>` | TFC Wrought Iron Double Sheet |
| steel | 5 | `iaftfc:dragonarmor_steel_<part>` | TFC Steel Double Sheet |
| black_steel | 7 | `iaftfc:dragonarmor_black_steel_<part>` | TFC Black Steel Double Sheet |
| red_steel | 8 | `iaftfc:dragonarmor_red_steel_<part>` | TFC Red Steel Double Sheet |
| blue_steel | 8 | `iaftfc:dragonarmor_blue_steel_<part>` | TFC Blue Steel Double Sheet |
| dragon_steel_fire | 10 | `iceandfire:dragonarmor_dragon_steel_fire_<part>` | IaFTFC Fire Dragonsteel Double Sheet |
| dragon_steel_ice | 10 | `iceandfire:dragonarmor_dragon_steel_ice_<part>` | IaFTFC Ice Dragonsteel Double Sheet |
| dragon_steel_lightning | 10 | `iceandfire:dragonarmor_dragon_steel_lightning_<part>` | IaFTFC Lightning Dragonsteel Double Sheet |

ここで`<part>`は`head,neck,body,tail`。3属性Dragonsteelの既存IaF IDには`dragon_steel`のアンダースコアを含むことに注意する。元IaFのIron、3属性Dragonsteelの4部位は既存Itemを再利用し、Steel/Black/Red/Blueの**新規16個だけ**を本アドオンで登録する。

全素材・全部位で元IaF Dragon ArmorのCrafting Shaped配置を保持し、素材として用いる元`minecraft:iron_block`、`iceandfire:dragonsteel_*_block`など**Blockの各個所を対応するDouble Sheet 1個に置換する**。各4部位の厳密な形状は次のとおり。`#`=同一素材のDouble Sheet。

```text
head: ["   "," ##","###"]
neck: ["   ","###"," ##"]
body: ["###","###","# #"]
tail: ["   ","  #","## "]
```

頭部は5枚、首5枚、胴8枚、尾3枚のDouble Sheetを使用する（各行の空白は素材不使用）。材料混在は禁止する。原IaFのCopper、Silver、Gold、Diamond、NetheriteのDragon Armorは**通常レシピ、Loot、新規入手経路、クリエイティブでの展示を無効化**し、本アドオンの有効装備候補から除外する。ただし既存アイテムIDは登録解除しない。既存セーブでそれらを保持する場合も、新規Equip・能力適用は禁止する（既装着分は読み込み時にその部位の防御0として扱い、手動取り外しは可能とする）。旧Diamond→NetheriteのSmithing Recipeも消す。IaFの既存Helmet等と別系列である点に注意する。

新規DragonArmorMaterialは`name={steel,black_steel,red_steel,blue_steel}`、`fireProof=false`、`dragonSteel=false`とし、Protectionのみ表の値とする。既存3属性Dragonsteelは元IaFの`dragonSteel=true`を維持する。IaFの`DragonArmorFeatureRenderer.getArmorTexture`はnamespaceを`iceandfire`へ固定するので、新規4素材の本体表示用Overlayは**`assets/iceandfire/textures/entity/dragon_armor/armor_<part>_<metal>.png`** に追加する。既存同名ファイルは上書きしない。新規ItemのModelは**`assets/iaftfc/items/dragonarmor_<metal>_<part>.json`**および`assets/iaftfc/models/item/dragonarmor_<metal>_<part>.json`に、画像は`assets/iaftfc/textures/item/dragonarmor_<metal>_<part>.png`に置く。IaF標準`DragonArmorItem.getDescriptionId()`が`iceandfire`名前空間へ固定されているため、新規16Itemは説明キーを`item.iaftfc.dragonarmor_<metal>_<part>`に上書きし、部位名と各素材名を含むローカライズを登録する。

新規4素材のDragonArmorItemはIaF既存のDragon Armorスロット、Synced Data、Rendererに完全互換とする。新規4素材×4部位のItem spriteとドラゴン本体表示overlayを用意する。素材ごとの色はSteel=`#A7ADB6`、Black Steel=`#3B414B`、Red Steel=`#B13A46`、Blue Steel=`#397DAE`とする。**Assetの生成方法**は、IaFの同部位Iron Dragon Armor item spriteおよびentity overlayを色変換する方式に固定する。原画像の各画素alphaは保持し、RGBは `gray=(77R+150G+29B+128)>>8`、`factor=(115+140*gray/255)/255` とし、各出力channelを `round(baseChannel*factor)` にして0～255へclampする。完全透明画素はRGBAすべて0。ソース画像の解像度・UV・透明領域・部位別shapeは変更しない。生成物は静的PNGとしてリポジトリへ含め、実行時の動的生成にしない。

#### Acceptance Criteria

##### AC-F014-001: 新規16装備と数値

Preconditions:
4種類のTFC金属Double Sheetとドラゴンを用意する。

Action:
各部位を所定のShapedパターンで製作・装着する。

Expected Result:
新規16アイテムを取得でき、各素材のProtectionが表の5/7/8/8となり、部位ごとの重複・装着不能・描画欠落がない。

##### AC-F014-002: 既存の有効装備

Preconditions:
TFC Wrought IronとIaFTFC3種のDragonsteel Double Sheetを用意する。

Action:
Ironと3属性DragonsteelのDragon Armorを全部位製作する。

Expected Result:
既存IaFの4素材×4部位のItem IDが得られ、Protectionは1.5または10となる。旧金属Block製法は不成立。

##### AC-F014-003: 無効素材の封鎖

Preconditions:
Copper/Silver/Gold/Diamond/Netheriteの元IaF Dragon Armorレシピ・アイテムを列挙する。

Action:
旧Crafting/Smithing、Loot、Creative表示、装着を確認する。

Expected Result:
通常入手・新規装着・防御適用ができない一方、旧ID保持セーブを破損しない。

##### AC-F014-004: アセット

Preconditions:
新規16アイテムと装備したドラゴンをクライアントで描画する。

Action:
4素材×4部位のItem sprite・Dragon overlayをスクリーンショットで検査する。

Expected Result:
定義したRGBA変換の画像が正しく描画され、missing texture、UVずれ、部位不一致がない。

### F-015: Egg孵化とDragon成長

#### Description

Fire/Ice/Lightningの卵12色、Spawner/Entity、孵化演出、生成Dragonの色・Owner・性別はIaF CEを維持する。**Eggの孵化進行にはTFC Calendarを使用せず、IaF `eggBornTime`（標準7200ゲームtick）を保持する**。IaFの孵化有効条件と年齢進行規則を保持し、TFC固有環境を条件に追加するだけとする。

- Fire Egg：IaF既存の炎判定を保持し、追加で**卵がある座標の真下1ブロック**のTFC温度提供Block Entity（Crucible等）が1500℃以上のとき孵化進行を許す。真下以外の熱源・周囲の熱放射は合算しない。1500℃未満はTFC側の条件を満たさない。温度はTFCが実際に保持する値を読む。元炎判定が成立した場合はそのまま有効。
- Ice Egg：IaFの水中で`egg_in_ice`へ遷移する処理（確率1/500 tick）を保持し、TFCのFresh WaterをVanilla水と同等に判定する。Salt Waterは認めない。Spring WaterはTFCの淡水タグに所属する場合だけ認める。Egg in Ice Block Entity内の孵化timerはIaF元tick方式を維持する。
- Lightning Egg：空が見えることとその場所が雨であることの**両方**が必要。Thunderstormは必須にしない。IaF元の雨判定、誕生時の視覚的稲妻等を保持する。

**孵化後のドラゴン成長**だけをTFC Calendarに連動し、`Dragon Age 1日 = TFC Calendar 1日`とする。TFC Calendarの経過tick/1日長を利用し、シーズンやカレンダー設定によって成長に必要な実game tickが変わる。残余のCalendar tickを蓄積し、小数日が生じた場合に二重加算をしない。ワールド保存時に年齢と最後のCalendar経過値を保存し、ロード・非ロード期間を含むTFC Calendar時間差に対応する。既存IaF成長停止、成長アイテム、卵入手・配置・孵化後Ownerなどの仕組みは維持する。

#### Acceptance Criteria

##### AC-F015-001: Fire Egg直下温度

Preconditions:
Fire EggをTFC熱源の真上に置く。温度を1499℃、1500℃、2000℃と変える。

Action:
各温度で一定tick進行させる。

Expected Result:
TFC熱源による進行は1500℃以上の場合のみ発生し、斜め下や隣の熱源からは進行しない。元IaF炎条件でも進行する。

##### AC-F015-002: Ice Eggの淡水

Preconditions:
Ice EggとTFC Fresh Water、Salt Water、Vanilla Waterを準備する。

Action:
確率処理を固定乱数で発生させる。

Expected Result:
Fresh WaterとVanilla WaterではEgg in Iceへ変化し、Salt Waterでは変化しない。孵化確率1/500とEgg in Iceのtimerは元IaFと一致する。

##### AC-F015-003: Lightning Eggの降雨

Preconditions:
Sky-visible/遮蔽とRain/No Rainの4条件を用意する。

Action:
Lightning Eggの年齢進行を調査する。

Expected Result:
Sky-visibleかつRainの場合だけ進み、Thunderstormは要求しない。

##### AC-F015-004: Egg timerとDragon成長の分離

Preconditions:
IaF標準eggBornTimeとTFCの2種のCalendar day lengthを用意する。

Action:
Egg孵化時間と孵化後Dragon Ageの増加を比較する。

Expected Result:
Eggはどちらの暦設定でも元IaF指定tickに従い、Dragon成長だけが各TFC日長に従って1日ずつ進む。再ログイン・アンロード後も二重成長しない。

### F-016: Dragon Meal、肉、StewおよびTFC食料

#### Description

IaFのDragon Meal、Sickly/Creative Dragon Meal、Fire/Frost/Lightning Stewの効果とドラゴンに対する作用はそのまま維持する。Dragon Mealの既存Shaped `BMB/MBM/BMB`（`B=bone`,`M=meat`）を保持し、`B`を`#c:bones`に、`M`を本アドオンが定義する`iaftfc:dragon_food_meat`タグへ変更する。このタグはTFC 4.2.12の**生の陸上動物肉**（Beef、Pork、Mutton、Chicken、Venison、Rabbit、Horse Meat、Bear、Bison、Turkey、Duck、Quail、Grouse、Pheasant、Goat/Chevon、Camelidae等の存在ID）だけを含み、調理肉・腐敗肉・魚介・IaFドラゴン肉・食事完成品を含めない。実際のTFC登録一覧に存在しない識別子は出力しない。レシピは入力ごとに肉4個、Bone5個でDragon Meal1個を返す（パターン中の`B`5個、`M`4個）。Vanilla肉だけを必要とする経路は置換する。

元IaFの3属性Dragon FleshはそれぞれTFC FoodDataに登録し、食べることで発生するIaF本来の状態効果を変更しない。3属性ともFoodData値は正確に `hunger=4, saturation=0, protein=2.0, water=0, decay_modifier=2.0`、その他栄養0とする。属性Stew3種は `hunger=4, saturation=2.0, protein=2.5, water=5.0, vegetables=1.0, grain=0, fruit=0, dairy=0, decay_modifier=1.5` とする。これはTFCの生肉／調理肉の栄養と腐敗率に準じる確定値である。FoodDataが付いたことによって元IaF特殊効果や既存レシピを失わせない。

属性Stewの製法は元IaFの各レシピ・ボウル・属性Lily・固有素材をそのまま使用し、TFCに未導入のNether/End素材について代替手段を作らない。TFC Overdimsの有無は判定しない。Dragon Meatの食料としての腐敗はTFC FoodCapabilityの元標準挙動に従い、永久保存・腐敗無効を設定しない。

#### Acceptance Criteria

##### AC-F016-001: TFC MeatによるDragon Meal

Preconditions:
複数種の新鮮なTFC生肉、Bone、腐敗肉、調理肉を用意する。

Action:
Dragon Mealレシピを試す。

Expected Result:
生肉4個+Bone5個で1個が生成され、腐敗肉・調理肉・魚介では生成されない。Dragon Mealの元IaF成長効果を維持する。

##### AC-F016-002: FoodDataと腐敗

Preconditions:
3属性Dragon Fleshと3属性Stewを用意する。

Action:
栄養表示、腐敗、食事効果、保存を検査する。

Expected Result:
6食品すべてが定義したFoodDataとTFC腐敗を持ち、元IaFの食用効果とStewの機能を維持する。

### F-017: 属性Lily、繁殖とDragon関連植物

#### Description

Fire Lily、Frost Lily、Lightning Lilyの自然生成、配置後の挙動、属性Stewでの用途はIaF CEの元仕様を維持し、TFC Overworldにおける配置先Biomeと設置地面の判定だけを修正する。Fire LilyはTFCの砂・高温に対応する土壌、Frost LilyはTFCの雪/氷・寒冷地面、Lightning LilyはTFCの土・草/高降水量に対応する地面を適格とする。IaFのConfigured/Placed Featureの個数・試行分布を維持し、3属性をClimate平均値により適合性チェックする。適合性はF-002の属性scoreが3種中で最大または同値最高である地点とし、自然生成時のみ判定する。プレイヤーの手動植栽は元IaFの植栽判定を基本とする。繁殖・交配・Stew使用後のドラゴン挙動は元IaFに従う。

#### Acceptance Criteria

##### AC-F017-001: TFC地表でのLily生成

Preconditions:
各属性scoreの高いTFC地域と適格地表を用意する。

Action:
自然生成と手動植栽を確認する。

Expected Result:
属性に対応するLilyがTFC地表に生成され、異属性環境で新規自然生成しない。Stewレシピ・繁殖効果が元IaFと一致する。

### F-018: Dragon Seeker、収納、飛行と制御

#### Description

IaFのDragon Horn、Dragon Flute、Dragon Staff、属性Summoning Crystal、Dragon Seeker各段階、騎乗・飛行・召喚・収納・Owner管理・Config制限は元IaFの実装を維持する。SeekerのうちNormalとEpicだけ材料を置換する。

- `iceandfire:dragon_seeker`：元Shapedの`a/x/x`を維持し、`a=#iceandfire:dragon_skulls`、`x=tfc:metal/ingot/steel`×2、結果1個。元Netherite Ingot経路を無効化する。
- `iceandfire:epic_dragon_seeker`：Shapelessではなく**別のSmithing Transformレシピ2本**で、Base=`iceandfire:dragon_seeker`、TemplateとAdditionはIaF元Recipeに定義されたNether Starなどの構成を維持せず、`Template`をTFC `tfc:metal/ingot/red_steel` とするRed版、および `tfc:metal/ingot/blue_steel` とするBlue版とし、Additionはそれぞれ**同じ色のIngot**とする。すなわちRed版は`base Normal Seeker + red_steel ×2`、Blue版は`base Normal Seeker + blue_steel ×2`で、Resultは同じEpic Seeker1個。1 recipeでRed/Blueの共通Ingredientを使わず、異色混合は不成立。元IaFのNetherite Smithing TemplateとNether Star経路を無効化する。
- Legendary Seekerは元IaFのドラゴン頭骨＋Dragonsteel素材という要求を維持し、`#iceandfire:dragon_steels`を`#iaftfc:dragonsteel_ingots`へ置換して本アドオン3金属を認める。最上位Godlyは元IaFの入手条件を変更しない。

Seekerの元性能はNormal追跡範囲150、Epic200、Legendary300、Godly500であり、追跡可能な死亡/飼い慣らし状態、出力表示、Admin能力は元IaFを維持する。Seekerの表示や使用によって未生成チャンクを強制生成する追加機能を作らない。

#### Acceptance Criteria

##### AC-F018-001: NormalとEpicの製作

Preconditions:
Dragon Skull、Steel Ingot、Red/Blue Steel Ingot、元IaFの旧Netherite素材を用意する。

Action:
NormalレシピとEpic2本を試す。

Expected Result:
Skull1+Steel2でNormal1個、Normal1+Red Steel2またはBlue Steel2でEpic1個を作る。色違い混合、旧Netheriteレシピでは作れない。

##### AC-F018-002: 探索範囲とドラゴン制御

Preconditions:
距離150/200/300/500付近にDragonを配置し、制御用アイテムを用意する。

Action:
Seeker、Dragon Horn、Flute、Staff、Crystal、騎乗を操作する。

Expected Result:
元IaFと同じ追跡範囲・対象種別・制御・保存・飛行を維持する。LegendaryはIaFTFC Dragonsteel Ingotを材料とする。

### F-019: Bestiary、Manuscript、Lectern

#### Description

Bestiaryの**全ページ**、非ドラゴンページを含む解禁候補、初期Introduction、解禁進行、Manuscript、Lectern、開閉UI、ページ配置、説明・画像、3候補の選択、必要Manuscript数（1ページ3個）、3 ManuscriptsからBestiary1個のCraftingをIaF CEのまま維持する。ドラゴン専用ページへのフィルタは行わない。Lecternの板材IngredientはTFC木材を含む共通`#minecraft:planks`タグを使用する。Manuscriptのドロップ元や生成率は元IaFに従い、本アドオン固有の新しいManuscript入手経路は追加しない。本書による金属/装備製法の変更により本にある古いレシピ図が事実と異なる場合、少なくとも Dragon Forge / Dragonsteel / Dragon Armor の該当説明部分は本書の新製法へ更新する。ただし全ページ数、ページ解禁順、非ドラゴンページの表示は変更しない。

#### Acceptance Criteria

##### AC-F019-001: 全ページと解禁

Preconditions:
Bestiary、Lectern、Manuscriptを用意する。

Action:
Bestiary作成、Lecternでページを解禁、全ページを開く。

Expected Result:
非ドラゴンを含む全IaFページが存在し、解禁候補のフィルタ・削除がなく、各ページ3 Manuscriptsの必要数を維持する。TFC PlanksでLecternレシピが成立する。

##### AC-F019-002: 新製法の案内

Preconditions:
DragonsteelおよびDragon Forgeのページを開く。

Action:
金属製造、装備工程、Dragon Armorの材料を表示する。

Expected Result:
元IaFのDragon Forgeと情報を残しつつ、旧Iron Ingot直接変換や旧Block製作を誤って正規製法として示さない。

### F-020: ブレス、Dragonの地形破壊および保護境界

#### Description

Fire/Ice/Lightningのブレス・飛翔攻撃・地形破壊による属性石への変換・燃焼・凍結・電撃、被弾Entitiesへの攻撃、Dragon Forge Inputへのブレス供給はIaF CEの元挙動を維持する。TFCの原石、土壌、砂利、原木、作物を元IaFの同等タグ/カテゴリとして認識させる。変換後の石はIaFの単一Charred/Frozen/Crackled Stoneとし、岩石種ごとに新規Blockは設けない。TFC Fresh Waterの凍結は元IaFのWater凍結と同等にするが、Salt WaterはFresh Waterとして扱わない。

生きているドラゴンはTFC岩石・鉱石・設備・金床・Crucible・Bloomery等を原則破壊し得る。無条件のTFC全設備保護タグを作らない。`mobGriefing`、IaFの`dragonGriefing`等の設定、DragonBlockBreakBlacklist（元IaFタグ）の判定を元どおり維持する。元IaFがBlock Entity保護する場面はそのまま保護する。`DragonFireDamageWorldEvent`全体をCancelしてはいけない。CancelするとEntity DamageやDragon Forgeへのブレス供給も抑止するため、TFC地形変換に必要な狭い箇所だけを修正する。

TFCの `can_collapse` など落盤に関するTagを恣意的に追加して属性石をTFC生岩と同等に落盤させることはしない。属性石の落盤は元IaFブロック性質のままにする。Cave・Roost生成時の属性化と、生きたDragonのブレス変換を区別する。

#### Acceptance Criteria

##### AC-F020-001: 3属性の地形変換

Preconditions:
TFC生岩、土壌、砂利、木材、Fresh/Salt Water、作物を配置する。

Action:
各属性Dragonがブレスを放つ。

Expected Result:
IAF元カテゴリに従ってCharred/Frozen/Crackled/灰化/凍結/除去が発生し、個別母岩の属性石を追加しない。IceがFresh Waterを凍結でき、Salt Waterは特別な淡水として扱われない。

##### AC-F020-002: Dragon ForgeとEntity Damage

Preconditions:
Dragon Forge InputとターゲットEntityをDragonのブレス範囲に配置する。

Action:
TFC地形を含む状態でブレスを発射する。

Expected Result:
地形変換を行いながら元IaFのEntity DamageとForgeへのブレス供給が両立し、WorldEvent全体のCancelによる副作用がない。

##### AC-F020-003: TFC設備と保護設定

Preconditions:
TFC Anvil、Crucible、Bloomery、Ore、Chestを置き、IAF griefingとMinecraft mobGriefingを切り替える。

Action:
Dragonがブロック破壊行動を行う。

Expected Result:
通常設定ではTFC設備・鉱石も元IaFの破壊条件に従い破壊される。既定Blacklistおよびgriefing無効設定では元IaF相当の保護が働き、追加のTFC全面保護はない。

### F-021: 自然Dragon Skeletonと資源進行

#### Description

Fire/Ice/Lightningの自然Dragon Skeleton（死骸）生成、配置、骨・頭骨の回収、色・バリエーション・LootはIaF CE本来の方式を維持する。TFC OverworldのClimate条件によって対象外にならないよう、3属性の自然死骸生成バイオームをF-002と同じClimateの適性scoreに従って広域TFCバイオームへ接続する。ただしIaFの元Skeleton Featureの出現頻度とLoot数量は変更しない。Skeletonがあることを理由としてDragonbone装備をTFC金属技術へ追加制限することはせず、Dragonbone Knappingによって加工する。

#### Acceptance Criteria

##### AC-F021-001: 自然死骸

Preconditions:
TFC Overworldを生成し、Dragon Skeletonの元IaF自然生成が有効である。

Action:
3属性Skeletonとその破壊/回収を検査する。

Expected Result:
全3属性がTFC地域に自然生成し、元IaFと同じ骨・頭骨・Lootの供給を維持する。Knapping以外の新しい加工経路は作らない。


### F-022: 進行経路とバージョン固定の総合検証

#### Description

すべての新規製法・既存製法の置換・ワールド生成・Loot・保存・表示は相互に矛盾してはならない。完全な新規Worldで、Dragon Skeleton→Dragonbone Knapping、Dragon Scales→Scale Armor、TFC Red/Blue/Black Steel→Dragon Forge→Dragonsteel→Tier 6 Double Ingot→Tier 7 Anvil→Tool/Armor、Dragon Egg→成長→Dragon Forge燃料供給というゲーム内経路を、Creativeによる中間素材補給なしで通過できることを確認する。元IaF JARの指定済み製法・温度・頻度・ドロップ数から逸脱する場合は、該当Featureに明示した変更だけを適用する。

本Featureは `PROJECT.md` と HarnessのAcceptance Criterion対応を含む。GameTestでワールド生成の結果を決定論的なシード・構造物座標で検査し、E2Eで装備、GUI、金属、ドラゴンおよびBestiaryの表示を実画面から検査する。ゲームプレイに関するACを「コンパイル成功」で代用することは認めない。

#### Acceptance Criteria

##### AC-F022-001: サバイバル進行の完結性

Preconditions:
本書の必須依存のみを導入し、新しいTFC Overworldを用意する。

Action:
自然生成資源、ドラゴン素材、TFC金属加工、Dragon Forge、Dragonsteel金床、装備、孵化を順番に実行する。

Expected Result:
本書で許可した素材・装置・レシピだけで、Fire/Ice/LightningのDragonsteel装備と全有効Dragon Armorが製作できる。該当しないVanilla/IaF素材のショートカット、入手不能な必須中間アイテム、Gold/Silver/Sapphireの二重進行経路は存在しない。

##### AC-F022-002: 再起動、保存、専用サーバー

Preconditions:
Dragon Forge、熱いDragonsteel、Crucible内の溶融金属、卵、育成中のドラゴン、Dragon Horn収納個体、装着中のDragon Armor、解禁済みBestiaryを用意する。

Action:
セーブ、完全なserver停止、再起動、client再接続、チャンクアンロードと再ロードを実行する。

Expected Result:
金属種・量・熱、卵tick、Dragon Age・Owner・収納情報、各Armor、Bestiaryのページ解禁、TFC食料の腐敗状態が破損・重複・巻き戻りなく引き継がれ、client/server間の視覚状態が一致する。

##### AC-F022-003: 不正レシピおよび元IaF経路の不成立

Preconditions:
旧Vanilla Iron/Netherite/Silver/Sapphire、旧IaF Dragonsteel Ingot/Block、旧Dragon Armor、元IaF Dragonbone/Dragonscale完成品の全レシピ、指定したTFC置換レシピを列挙する。

Action:
Recipe Manager、JEI等に依存しない実クラフト、Dragon Forge、Anvil、Welding、Mold、Lootを検証する。

Expected Result:
許可した新経路だけが成立し、元IaFの無効な金属ブロック逆変換、Netherite Dragon Armor、Diamond Dragon Armor、旧Iron直接Dragonsteel、Knapping迂回、属性混合Epic Seekerの取得経路が存在しない。元IaFの無効化対象以外のドラゴン専用レシピは存続する。

##### AC-F022-004: データ生成の再現性とテスト対応

Preconditions:
同一TFC/IaF/NeoForgeバージョン、同一SourceおよびWorld Seedの2つの独立Workspaceを用意する。

Action:
Data Generationを各々実行し、生成されたレシピ、タグ、Loot、モデルJSON、テクスチャ、自然生成選択を比較する。またすべてのACのGameTest/E2E対応を検査する。

Expected Result:
生成物はバイト列またはPNGピクセル単位で一致し、抽選出力は同座標・同シードで一致する。全ACが `tests/acceptance.json` に存在し、検証不能のAC、単なるskip、意図しないmissing textureを残さない。

## Global Requirements

### GR-001: 仕様の正本と参照版

- 本書 `PROJECT.md` だけを製品仕様の正本とする。初回開発の固定依存はMinecraft 1.21.1、NeoForge 21.1.248、TFC 4.2.12、IaF CE 2.1.4、Java 21とする。固定版を用いたテスト結果だけを合格判定に用いる。ユーザーから改版の指定があるまで新規リリースへ自動更新しない。
- 上流の仕様をそのまま使用すると明記した部分は、上記リリースの**登録済みデータ、NeoForge jarおよびゲーム内挙動**を参照する。開発ブランチ上の変更を参照した際、上記リリースと異なる場合はリリースを優先する。元IaFTFCの既存リポジトリや未検証の実装方法を設計上の根拠にしない。
- 全新規コンテンツは `iaftfc` 名前空間。元IaFのドラゴン3属性・装備・工具・Mob・UI・特殊能力を変更しない場合は元IDを使用し、同一品の複製を避ける。
- ソースの基準URL：`https://github.com/IAFEnvoy/IceAndFire-CE/tree/1.21.1`、`https://github.com/TerraFirmaCraft/TerraFirmaCraft/tree/1.21.x`、Harness `https://github.com/lau-mods/modding-harness`。本書とソースが矛盾した場合は本書の変更点を優先し、変更点以外は固定リリースを優先する。

### GR-002: 数値・レシピの共通解釈

- `>=`、`<`、整数個数、確率、温度、重み、ID、5×5 Knappingパターンは本書の数値を厳密に使用する。浮動小数点の境界は小数点以下の丸めによりTFC/IaFの既存Recipe判定より緩くしてはならない。製錬・熱輸送はserver authorityで管理する。
- AnvilのForge Ruleは実際のTFC JSON `rules` 順で指定する。鍛造Tierは7、標準的なTier-1 Welding工程は6とする。鍛造不可能な低Tier装置や温度不足状態の加工を防止する。Sheet、Double Sheet、Double Ingot、Rodなどの金属量と熱容量は同重量のTFC Red Steelアイテムに一致させる。
- 「無効化」は既存IDのレジストリ登録解除を意味しない。指定の入手経路を遮断し、指定された場合だけ装備・能力適用を停止する。`neoforge:false`などNeoForgeのレシピ条件および正確なRecipe ID差し替えを使用する。無効化に必要なデータの置換は本書対象のレシピ・Loot・Biome Modifierのみとし、無関係なIaFコンテンツを巻き込まない。
- TFC素材への置換はcategoryを保つ。Nugget→Small Native Ore、Ingot→Ingot、Sheet→Sheet、Head→Head、Helmet→Helmetなど、ゲーム内の素材段階を上げる不正な置換や部位を変える置換を禁止する。Lootのcount functionは元entryの個数を変えない。エンチャント対象外の部品にEnchantを付与したことにして進行を迂回しない。

### GR-003: TFCのサイズと重量

TFCのItem Size/Weightを、次の同用途TFCカテゴリと同じ仕様で付与する。既存のTFC共通タグでサイズ・重量が継承される場合は重複定義せず、継承した結果を検査する。専用サイズ指定が重複する場合は指定素材の正しいサイズ・重量が優先されるようにデータを調整する。

| IaFTFCまたはIaF対象 | サイズ/重量基準 |
|---|---|
| Dragonsteel Ingot、Double Ingot、Sheet、Double Sheet | `data/tfc/tfc/item_size/{ingots,double_ingots,sheets,double_sheets}.json` の `large/medium` |
| Dragonsteel Rod | TFC金属Rodの `normal/light` |
| Dragonsteel Anvil | TFC Anvilの `huge/very_heavy` |
| Dragonsteel Blade/Head | TFC Red Steelの同種Tool Head/Blade |
| Dragonsteel Unfinished Armor | TFC Red Steelの同部位Unfinished Armor |
| 完成Dragonsteel工具・プレイヤー防具 | TFC Red Steelの同じ工具・防具部位 |
| DragonboneとDragonbone Blade/Head | TFCの骨、およびStone/Metalの対応Tool Head/Bladeのうち同用途で使用されるSize/Weight |
| Dragon ScalesとScales製完成防具 | TFCのLeather系原料、および完成Leather Armorの同部位 |
| Dragon Blood | TFCのボトル入り液体/Glass Bottleと同一 |
| Dragon Flesh・3属性Stew | TFC `c:foods` の `small/light`、食料腐敗APIを有効にする |
| Dragon Armor Head/Neck/Body/Tail | IaF既存4部位のサイズ/重量をTFC容量制約に合わせ、同じ部位は素材によらず同一値とする |

特にDouble Sheetは1個400mBであり、Dragon Armorレシピの各セルにDouble Sheet1個を使用する。DragonsteelのRodは50mB。完成装備の保存・熱処理・収納はTFCの同種アイテムに従う。

### GR-004: レジストリ、タグ、データ配置

- 主要登録IDは各Feature表に従う。データパスはNeoForge 1.21.1に従い`data/iaftfc/recipe/`、`data/iaftfc/tfc/`、`data/iaftfc/tags/`、`assets/iaftfc/`、元IaF上書きは`data/iceandfire/recipe/`、`data/iceandfire/loot_table/`などの正規パスを使用する。内部Java package名は`iaftfc`を含む固有のものとし、元IaFTFCのコードを転用しない。
- 金属タグは少なくとも `c:ingots/<metal>`、`c:double_ingots/<metal>`、`c:sheets/<metal>`、`c:double_sheets/<metal>`、`c:rods/<metal>` と各共通上位タグ、TFCの必要な金属/加熱タグへ追加する。3金属をまとめるタグ `iaftfc:dragonsteel_ingots` は新規正規Ingot3種**だけ**を含め、IaF旧Ingotを含めない。既存のTFC Red/Blue/Black Steelタグや別Modの同名アイテムは再定義しない。
- GemはTFC Gem 9種類のItem IDを参照する。Rockは`basalt,quartzite,diorite`だけをCave用に指定する。動的なRock判定を経由して他のRockIDのGem鉱石を生成しない。
- Resource Packは元IaFやTFCの既存Assetを壊さない。新規装備PNGはF-014のピクセル演算で固定生成し、生成Scriptと生成済み画像を同梱する。Texture Atlas、モデル、表示用言語ファイルは`ja_jp`、`en_us`の双方を用意し、Missing Translationが残らないようにする。IaFのライセンスと再配布条件を遵守し、参照元をリポジトリの著作権表示に記載する。

### GR-005: Lootとデータ生成

- Loot変換の正本はF-006と本GR-005である。変換元の正本はIaF CE 2.1.4のJARに格納された9Chest JSONであり、生成スクリプトは当該固定入力から生成する。旧IaFTFCの仕様書や外部Markdownを必要としない。別Mod向けルールは本アドオンに実装しない。
- データ生成時に元IaF JARから9Chestの元JSONを取り出して読込み、対象のentryだけをカテゴリ単位に翻訳する。元pool構造、rolls、bonus_rolls、weight、conditions、count provider、元IaF固有アイテムを保存する。元JSONが固定版から異なる場合は失敗として停止し、何も黙って補間しない。
- 必須の機械的バリデーション：9テーブルの存在、元と同一pool数、同一rolls/bonus_rolls、親entry数・weight、count provider、条件、NuggetのSmall Ore化、IngotのIngot化、属性Gem/金属だけの固定変換、非属性Gemの共通表、装備部位維持、Tool Family内重み等価、2:1割合、Player装備のTier上限、Red/Blue Steel Lootなし、Black Steel完成装備なし、Graphite Powderなし、Bow/Crossbow分離、Enchant未完成品不可、1entryで二重set_countなし、IaF固有Loot保存。
- データ生成とPNG色変換は決定論的であること。生成される識別子・確率・配列順序は安定させ、Git差分に乱数、ファイル時刻、実行環境依存値を含めない。

### GR-006: ワールド生成と他Modへの影響

- IaFのドラゴン本体、Roost、Cave、自然Skeleton、属性Lilyだけを対象とする。IaFのその他Mob、その他構造物、他Modの自然生成、TFCの岩石・鉱脈・水文・地形生成ルールを全域で変更しない。
- 適性計算はTFC Overworldに限定し、Climate取得のためのチャンク生成や永久チャンクロードをしてはならない。シード・候補座標に対する再計算結果は同一とする。Climate APIの呼び出しはserver側で行い、client側でWorldgen適性を独自判定しない。
- Dragon Caveの鉱石以外の外殻変換は元IaFのままにする。RoostのTFC地表対応も元IaF装飾アルゴリズムと同数量に限る。Dragonが自然破壊するTFCの設備・鉱石を追加の全体保護リストで無条件に保護しない。
- 地形に対するMixin/Eventは狭い変更箇所に限る。ブレス全体をCancelしてEntity DamageやDragon Forge入力を失わせない。EntityやBlockEntityの`SavedData`、Chunk NBT、同期Packetは固定版IaFとTFCの既存保存と共存する。

### GR-007: Harness Workspace構成

`iaftfc`は `lau-mods/modding-harness` の**別のMod Workspace**であり、Harness自身のソースを変更してはならない。Workspaceのルートに本`PROJECT.md`を置き、`.harness`へHarnessをGit submoduleとして追加する。`gradlew`、Gradle ModDevGradle、`src/main/java`、`src/main/resources`、`src/test/java`、`tests/acceptance.json`、`tests/e2e`、`.harness-config.json`を揃える。Workspaceは必ず独立したGit repositoryとする。

新規作成はHarness READMEの `create --template-repo https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle.git` を使用する。`init` が本書を上書きしないことを確認する。`.harness-config.json` には少なくとも下記タスク・モデル設定形式を設定する。Agent名と推論モデルはHarness既定設定のまま変更してよいが、製品仕様の数値・アイテム群は変更してはならない。

```json
{
  "gradle": {
    "compile": "classes",
    "build": "build",
    "gameTest": "runGameTestServer"
  },
  "agents": {
    "implementation": { "command": "codex", "model": null, "effort": "medium", "fast": false, "provider": null },
    "codeReview": { "command": "claude", "model": null, "effort": "medium" },
    "e2eReview": { "command": "claude", "model": null, "effort": "high" }
  },
  "runtime": {
    "command": "mct",
    "server": null,
    "clients": [],
    "deploy": [],
    "logs": []
  }
}
```

`runtime.server`と`runtime.clients`は実際のNeoForge Dedicated ServerおよびMC Pilot clientの環境に合わせ、`doctor`が全項目`ok`を返すよう使用者が設定する。`runtime`の空欄をそのままE2E完了状態とは見なさない。GameTest実行でJUnit XMLが`build/test-results/gametest/`以下に出力されるようGradleを設定する。`tests/acceptance.json`には**本書の全AC ID**をGameTestまたはE2Eの実在するtestcase/scenarioへ対応付ける。描画を伴うACには実際にPNGを保存するE2Eを少なくとも1個対応させる。専用serverおよびclientの双方へTFC/IaFとIaF自身の必要依存を導入する。

各milestoneでは、実装と共にJavaのGameTest、必要なNode E2Eシナリオ、Acceptance Mapを作り、HarnessのCompile、Build、Review、GameTest、E2Eすべてを成功させる。実装後に`node .harness/dist/cli/main.js validate`、`doctor`、`plan`、`develop`が正規の操作手順で実行できること。Harness管理中の`PROJECT.md`、`.harness`サブモジュール、`.harness-state/plan.json`を開発Agentが書き換えて進行を迂回することは禁止する。

### GR-008: クライアント、セーブ、後方互換

- Modはサーバー側判定を正本とする。温度、Recipe、Dragon Age、卵年齢、DragonのOwner、Dragon Armor装着・Protection、Structure配置、Loot抽選はサーバーで確定し、clientへ必要な状態だけ同期する。描画専用クラスをDedicated Serverへロードしない。
- 旧IaFの持ち物/ドラゴン/卵/Bestiary/Forge設備を含むセーブをクラッシュさせずロードする。IaF旧IDを欠番にしない。無効化したItemのセーブ保持は許すが、新規取得ルート・能力は本書どおりに制限する。IaFTFC過去版のセーブ・独自データの移行は実装せず、過去の実装アセットを流用しない。
- IaFのDragonsteel Toolの耐久・特殊能力やプレイヤー防具の属性耐性は元実装を参照する。新しいDragonsteel TFC金属、Weapon/Armorの製作経路を作るだけで、元クラスを二重登録しない。
- ローカライズの英語・日本語、Model/Texture、Loot、Recipe、GameTest、E2Eの各仕様を同時に完成させる。未実装ACを省略する、Testを常時skipする、テストの成功条件を成果に合わせて緩める、GameTest/E2Eの結果を偽造することは禁止する。

## Constraints

- **対象機能**：IaF CEのFire/Ice/Lightning Dragon、その卵、自然死骸、Roost/Cave、Dragon Forge、Dragonsteel、Dragonbone/Dragonscale、防具、Manuscript/Bestiary、Dragon Meal/Flesh/Stew、属性Lily、ドラゴン用ユーティリティ、およびそれらの最低限の基盤のみ。その他のIaF Mob・構造物・装備などには手を加えない。
- **名前**：以前のIaFTFCから引き継ぐのは `iaftfc` の名前・Mod IDのみであり、既存のソースコード、仕様、アセット、生成成果物を流用しない。
- **バージョン**：上記固定版を正本とし、将来のIaF/TFCの変更は本書に基づき互換性を再検証してから採用する。必要依存以外のModのサポートを暗黙に要求しない。
- **追加コンテンツ**：Dragonsteelは3属性の金属加工、Forge、TFC用金属フォームおよびIaF既存の5Tool/4PlayerArmorに限定する。未存在のTFC専用Tool、未指定の魔法設備、新鉱石種、新ドラゴン種、独自ゲームプレイ機能を追加しない。
- **進行**：Tier 7加工・元素別Dragon Forgeの要件を緩和しない。異属性混合や旧Vanilla生産経路を復活させない。Lootの変換で高Tier装備を直接報酬に追加しない。
- **ワールド**：TFC世界生成と元IaF世界生成を尊重し、標準のDragon構造物頻度・数・素材ドロップ量を変更しない。TFCの他環境を無条件にドラゴン生成可能としない。
- **表示**：既存IaFのアニメーションや3D形状、TFC GUI操作感を保持し、新規アセットは本文の変換手順だけを使用する。解像度変更による品質差を発生させない。
- **実装手段**：上流API/タグ/データパックによる解決を優先する。Mixinが必要な場合は対象メソッドの特定の呼び出しを狭く変更し、関連しない元IaF/TFC処理に副作用を与えない。
- **ライセンス**：IaF CEとTFCそれぞれのライセンスを順守し、リリースのAssetsや参考ソースを使用する場合は許諾条件に従う。

## Open Questions

None.
