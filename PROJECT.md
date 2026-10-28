# Project

Status: draft

Project ID:
Mod ID:

## Platform

Minecraft:
NeoForge:
Java:

## Purpose

## Features

<!-- Add features using the following heading structure. Keep IDs stable.
### F-001: Feature name

#### Description

#### Acceptance Criteria

##### AC-F001-001: Criterion name

Preconditions:

Action:

Expected Result:
-->

## Global Requirements

## Constraints

## Open Questions
# Project

Status: draft

Project ID: iaftfc-reimplementation
Mod ID: iaftfc

## Platform

Minecraft: 1.21.1
NeoForge: 21.1.265
Java: 21

## Purpose

本modは、TerraFirmaCraft（TFC）の地質・気候・食料・金属加工と、Ice and Fire: Community Edition（IaF CE）のドラゴン探索・戦闘・飼育を一つのサバイバル進行として成立させる。ドラゴンの巣や洞窟から得た素材をTFCの作業工程へ取り込み、ドラゴンスティール製の道具・防具およびドラゴン用防具まで到達できることを目的とする。

対象はMinecraft 1.21.1のNeoForge版であり、既存のIaFTFCのアセットに含まれる素材・建材・防具・加工画面・表示名を使用する。新しい画風やゲームシステムを再設計することは目的としない。プレイヤーやサーバーから観測される結果を本書で定義し、その実現手段は指定しない。本書のみで本modの追加・変更仕様と受け入れ条件を判定できることを要件とする。

**用語と展開規則。** 以下の表の属性 `e` と金属識別子 `m` は常に同一行で対応する。`p` は部品形態、`c` は鱗色、`s` は防具部位とする。`<...>` を含む表記は、指定集合の要素を一つずつ展開した完全な公開ID集合を意味し、ゲーム内でそのまま入力する文字列ではない。`#名前空間:...` は該当する材料集合を表すタグである。個数、温度、確率、タグのメンバーは、特記のない限り既定のゲームデータを表す。

| 属性 e | 金属 m | IaFの血 | 炉入力に用いるTFC鋼 | 属性表示 |
| --- | --- | --- | --- | --- |
| fire | dragonsteel_fire | iceandfire:fire_dragon_blood | red_steel | 炎 |
| ice | dragonsteel_ice | iceandfire:ice_dragon_blood | blue_steel | 氷 |
| lightning | dragonsteel_lightning | iceandfire:lightning_dragon_blood | black_steel | 雷 |

属性・材質・色が異なる材料の混用は、該当Featureが明示的に許す場合を除いて成立しない。温度は摂氏、液量はTFCの金属量単位mBとし、100 mBをインゴット1個相当とする。加工図では `#` は材料または残すマス、`.` は空欄または除去したマス、`/` は行境界を表す。図の材料と空欄は、そのFeatureの定義に従う。

## Features

### F-001: 必須環境と導入

#### Description

本modはTFCおよびIaF CEの双方を必要とする。検証・配布・導入説明には、これらが起動時に要求する必須ライブラリだけを含める。以下の数値は2026年10月28日現在のMinecraft 1.21.1／NeoForge向け安定版候補である。版の数値が高くても異なるMinecraft系列やプレリリースは採用対象としない。最新候補の組合せで互換性を確認できない場合、未確認のまま正常動作を宣言しない。

#### Acceptance Criteria

| 導入対象 | 採用候補 | 必要性 |
| --- | --- | --- |
| TerraFirmaCraft / `tfc` | 4.2.12 | 必須のゲーム本体機能 |
| Ice and Fire CE / `iceandfire` | 2.1.4 | 必須のドラゴン・炉・装備機能 |
| Patchouli / `patchouli` | 1.21.1-93-NEOFORGE | TFCが要求する必須ライブラリ |
| Uranus / `uranus` | 2.4.1-bugfix（1.21.1／NeoForge） | IaF CEが要求する必須ライブラリ |
| Jupiter / `jupiter` | 2.3.7（1.21.1／NeoForge） | IaF CEが要求する必須ライブラリ |

開発開始時には公開された正式配布物の必須依存宣言と対象プラットフォームを照合する。`iaftfc` と上表の必須modだけを入れた構成で、シングルプレイおよび専用サーバーの双方を動作対象とする。新たに別の必須依存が確認された場合に限り、理由と対応版を本書へ追記してから検証構成を変更する。必須ライブラリが配布物へ内包されている場合、同一機能を重複導入しない。

##### AC-F001-001: 最小必須構成で起動する

Preconditions:
表の必須modと本modのみを導入した1.21.1／NeoForge環境。

Action:
新規TFCワールドを作成し、同じ構成の専用サーバーへ接続する。

Expected Result:
ロードが完了し、両環境で本modの追加アイテム・加工・ワールド生成の対象データが存在する。実際の読込modは必須集合と一致する。

Verification:
- e2e

##### AC-F001-002: 必須依存の欠落が識別できる

Preconditions:
正常起動する構成を複製する。

Action:
必須modを一つずつ除いた構成で起動する。

Expected Result:
欠落した必須modまたは版の不適合を識別できるロード失敗となり、機能が欠けた状態を正常起動として扱わない。

Verification:
- e2e

##### AC-F001-003: 異なるゲーム系列を採用しない

Preconditions:
公開版候補と対象MC・loaderの対応情報がある。

Action:
採用依存を確定する。

Expected Result:
本書のPlatformに対応する安定版が選ばれ、起動対象のMC系列・loader・Javaに不整合がない。互換性が未確認の場合は合格としない。

Verification:
- unit
- e2e

### F-002: ドラゴンの巣・洞窟とTFCの気候

#### Description

TFCワールドの陸上で、炎・氷・雷のドラゴンの地表の巣と地下の洞窟を探索する。配置の可否はその地点のTFCの平均気温と平均地下水量によって決まり、現在の天候、瞬間気温、季節だけでは決まらない。次の気候条件は配置の**必要条件**であって、適合チャンクに必ず巣が生じるという意味ではない。境界値は両端を含む。

#### Acceptance Criteria

| 属性 | 平均気温 T | 平均地下水量 G |
| --- | --- | --- |
| 炎 | T ≥ 15.0 | G ≤ 299.9 |
| 氷 | T ≤ 5.0 | 制限なし |
| 雷 | T ≥ 5.1 | G ≥ 300.0 |

対象となるTFC陸上バイオームは次の17種とする。

`tfc:plains`, `tfc:plateau`, `tfc:plateau_wide`, `tfc:badlands`, `tfc:canyons`, `tfc:buttes`, `tfc:volcanic_mountains`, `tfc:mountains`, `tfc:old_mountains`, `tfc:oceanic_mountains`, `tfc:glaciated_mountains`, `tfc:glacially_carved_mountains`, `tfc:lowlands`, `tfc:hills`, `tfc:highlands`, `tfc:rolling_hills`, `tfc:whorled_canyons`。海洋・河川のすべてを一律に陸上候補とはしない。

構造物は3属性×地表の巣・地下洞窟の6種類である。地表の巣と地下洞窟はそれぞれIaF CEのドラゴン、地形への組込み、生成可否設定に従い、通常の地形条件や村との排他条件を回避しない。配置間隔と最小間隔の単位はチャンクとする。

| 集合ID | 構造物ID | 配置間隔 | 最小間隔 |
| --- | --- | --- | --- |
| iceandfire:dragon_cave | iceandfire:fire_dragon_cave | 30 | 20 |
| iceandfire:dragon_roost | iceandfire:fire_dragon_roost | 24 | 10 |
| iaftfc:ice_dragon_cave | iceandfire:ice_dragon_cave | 30 | 20 |
| iaftfc:ice_dragon_roost | iceandfire:ice_dragon_roost | 24 | 10 |
| iaftfc:lightning_dragon_cave | iceandfire:lightning_dragon_cave | 30 | 20 |
| iaftfc:lightning_dragon_roost | iceandfire:lightning_dragon_roost | 24 | 10 |

配置方法は通常のランダムな分散配置とし、村 `minecraft:villages` からの除外距離は各集合で10チャンクとする。同じ属性を複数集合へ重複して登録することによる意図しない高頻度生成をしない。配置結果は同一seed・同一必須mod構成・同一設定で再現されるものとする。既に生成したチャンクの自動再生成はしない。

##### AC-F002-001: 気候の境界を判定する

Preconditions:
TおよびGが境界値とその前後となる地点を用意する。

Action:
属性別の構造物候補判定を比較する。

Expected Result:
炎は15.0/299.9を許可し温度不足・地下水過多を除外する。氷は5.0まで、雷は5.1/300.0から許可する。条件の隙間を勝手に埋めない。

Verification:
- unit
- gametest

##### AC-F002-002: 6種類の構造物が生成される

Preconditions:
各属性の気候条件とTFC陸上バイオームを満たす新規ワールド。

Action:
十分な未生成範囲で巣・洞窟の生成と配置集合を調べる。

Expected Result:
3属性それぞれの巣と洞窟が生成候補になり、属性・構造物ID・配置間隔・村との除外関係が表と一致する。

Verification:
- gametest
- e2e

##### AC-F002-003: 不適合地域で強制生成しない

Preconditions:
気候不適合のTFC陸上およびTFCの気候情報がない世界を用意する。

Action:
自然生成の候補を調べる。

Expected Result:
本modが不適合地域へ対象構造物を新たに配置しない。気候情報の欠落を原因とするクラッシュが起こらない。

Verification:
- gametest

##### AC-F002-004: 未生成チャンクで配置を再現する

Preconditions:
同じseedと設定の未生成ワールド2つ。

Action:
同じ範囲を生成し、再起動後も別の未生成範囲を生成する。

Expected Result:
同一構成・seed・地形条件で配置結果が一致し、既生成チャンクへ遡及配置しない。

Verification:
- gametest
- persistence

### F-003: 巣・洞窟の資源と戦利品

#### Description

巣と洞窟の宝箱、岩石、鉱石、金属の山から、TFCの素材と属性に適合した戦利品が得られる。炎の宝箱はクリ、氷の宝箱はホワイトシダー、雷の宝箱はブラックウッドのTFCチェストとする。開閉、向き、未開封時の戦利品、開封後の所持品と残数は通常のチェストと同じく扱う。

#### Acceptance Criteria

| 属性 | 宝箱のブロックID | 属性宝石 |
| --- | --- | --- |
| 炎 | tfc:wood/chest/chestnut | tfc:gem/ruby |
| 氷 | tfc:wood/chest/white_cedar | tfc:gem/sapphire |
| 雷 | tfc:wood/chest/blackwood | tfc:gem/topaz |

洞窟の鉱石の出現量と位置はIaF CEの地形規則に従う。鉱石は周囲のTFC地層に対応する母岩の鉱石となり、単一岩種の地層では必ずその岩種を使う。岩種を判定できない場合の既定母岩はgranite。異なる岩種が交わる境界では同じ地形とseedから決定的に結果を得る。宝石に`rich_`接頭辞を付けない。

| 鉱石階級 | TFC鉱石名 | 出現属性 |
| --- | --- | --- |
| common | rich_native_copper, rich_malachite, rich_tetrahedrite, rich_hematite, rich_magnetite, rich_limonite | 全属性 |
| uncommon | rich_cassiterite, rich_bismuthinite, rich_sphalerite | 全属性 |
| rare | rich_native_gold, rich_native_silver, rich_garnierite | 全属性 |
| attribute | ruby / sapphire / topaz | 炎 / 氷 / 雷に順対応 |

鉱石ブロックIDは金属と宝石を区別せず `tfc:ore/<表の鉱石名>/<母岩>` とする。金・銀・銅の山 `iceandfire:<gold|silver|copper>_pile` の通常破壊報酬はそれぞれ `tfc:ore/small_native_gold`、`tfc:ore/small_native_silver`、`tfc:ore/small_native_copper` とし、積層1～8に応じて1～8個を落とす。バニラのナゲットにはしない。

宝箱の抽選は、属性ごとの地表巣・雌洞窟・雄洞窟の9集合を対象とする。各表の候補は、表にある重みに比例して1回ごとに抽選し、同一候補を複数回引くことを許す。個数範囲は両端を含む一様整数分布とする。`shared/...` は後掲の共有候補集合から1種類を抽選する。各行に書いた個数は抽選されたアイテムに適用する。重量の大きな行を確定報酬に読み替えない。

**`iceandfire:chest/fire_dragon_roost`：抽選回数 1–4、候補総重み 165。**

| 候補 | 個数 | 重み | 追加効果 |
| --- | --- | --- | --- |
| 共有 `iaftfc:shared/gems` | 1–4 | 2 | なし |
| `tfc:gem/ruby` | 1–4 | 3 | なし |
| 共有 `iaftfc:shared/ingots/ordinary` | 10 | 20 | なし |
| `minecraft:bone` | 10 | 20 | なし |
| `minecraft:book` | 1–2 | 10 | なし |
| `tfc:metal/chain/wrought_iron` | 1–2 | 20 | なし |
| `iceandfire:manuscript` | 1–6 | 20 | なし |
| `tfc:metal/ingot/gold` | 10 | 7 | なし |
| `tfc:ore/small_native_gold` | 16 | 12 | なし |
| `minecraft:skeleton_skull` | 1 | 3 | なし |
| `minecraft:cobweb` | 1–6 | 15 | なし |
| 共有 `iaftfc:shared/equipment/ordinary/sword` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/ordinary/helmet` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/ordinary/chestplate` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/ordinary/greaves` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/ordinary/boots` | 1 | 5 | なし |
| 共有 `iaftfc:shared/utility` | 1–6 | 7 | なし |
| `minecraft:experience_bottle` | 1–5 | 1 | なし |

**`iceandfire:chest/fire_dragon_female_cave`：抽選回数 1–4、候補総重み 165。**

| 候補 | 個数 | 重み | 追加効果 |
| --- | --- | --- | --- |
| 共有 `iaftfc:shared/gems` | 1–4 | 2 | なし |
| `tfc:gem/ruby` | 1–4 | 3 | なし |
| 共有 `iaftfc:shared/ingots/dangerous` | 10 | 20 | なし |
| `minecraft:bone` | 10 | 20 | なし |
| `minecraft:book` | 1–2 | 10 | なし |
| `tfc:metal/chain/wrought_iron` | 1–2 | 20 | なし |
| `iceandfire:manuscript` | 1–6 | 20 | なし |
| `tfc:metal/ingot/gold` | 10 | 7 | なし |
| `tfc:ore/small_native_gold` | 16 | 12 | なし |
| `minecraft:skeleton_skull` | 1 | 3 | なし |
| `minecraft:cobweb` | 1–6 | 15 | なし |
| 共有 `iaftfc:shared/equipment/dangerous/sword` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/dangerous/helmet` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/dangerous/chestplate` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/dangerous/greaves` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/dangerous/boots` | 1 | 5 | なし |
| 共有 `iaftfc:shared/utility` | 1–6 | 7 | なし |
| `minecraft:experience_bottle` | 1–5 | 1 | なし |

**`iceandfire:chest/fire_dragon_male_cave`：抽選回数 1–6、候補総重み 212。**

| 候補 | 個数 | 重み | 追加効果 |
| --- | --- | --- | --- |
| `minecraft:book` | 1 | 12 | Lv30エンチャント |
| 共有 `iaftfc:shared/gems` | 1–4 | 2 | なし |
| `tfc:gem/ruby` | 1–4 | 3 | なし |
| 共有 `iaftfc:shared/ingots/lategame` | 1–25 | 20 | なし |
| `minecraft:bone` | 1–25 | 20 | なし |
| `tfc:metal/chain/wrought_iron` | 1–2 | 20 | なし |
| `iceandfire:manuscript` | 1–6 | 20 | なし |
| `tfc:metal/ingot/gold` | 1–25 | 20 | なし |
| `tfc:ore/small_native_gold` | 16 | 30 | なし |
| `minecraft:skeleton_skull` | 1 | 1 | なし |
| `minecraft:cobweb` | 1–6 | 5 | なし |
| 共有 `iaftfc:shared/equipment/lategame/sword` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/lategame/helmet` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/lategame/chestplate` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/lategame/greaves` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/lategame/boots` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/lategame/sword` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/lategame/helmet` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/lategame/chestplate` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/lategame/greaves` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/lategame/boots` | 1 | 5 | なし |
| 共有 `iaftfc:shared/utility` | 1–6 | 7 | なし |
| `minecraft:golden_apple` | 1 | 1 | なし |
| `minecraft:experience_bottle` | 1–5 | 1 | なし |

**`iceandfire:chest/ice_dragon_roost`：抽選回数 1–4、候補総重み 175。**

| 候補 | 個数 | 重み | 追加効果 |
| --- | --- | --- | --- |
| 共有 `iaftfc:shared/gems` | 1–4 | 2 | なし |
| `tfc:gem/sapphire` | 1–4 | 7 | なし |
| 共有 `iaftfc:shared/ingots/ordinary` | 10 | 20 | なし |
| `minecraft:bone` | 10 | 20 | なし |
| `minecraft:book` | 1–2 | 10 | なし |
| `tfc:metal/chain/wrought_iron` | 1–2 | 20 | なし |
| `iceandfire:manuscript` | 1–6 | 20 | なし |
| `tfc:metal/ingot/silver` | 10 | 10 | なし |
| `tfc:ore/small_native_silver` | 16 | 15 | なし |
| `minecraft:skeleton_skull` | 1 | 3 | なし |
| `minecraft:cobweb` | 1–6 | 15 | なし |
| 共有 `iaftfc:shared/equipment/ordinary/sword` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/ordinary/helmet` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/ordinary/chestplate` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/ordinary/greaves` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/ordinary/boots` | 1 | 5 | なし |
| `minecraft:packed_ice` | 1–6 | 7 | なし |
| `minecraft:experience_bottle` | 1–5 | 1 | なし |

**`iceandfire:chest/ice_dragon_female_cave`：抽選回数 1–4、候補総重み 175。**

| 候補 | 個数 | 重み | 追加効果 |
| --- | --- | --- | --- |
| 共有 `iaftfc:shared/gems` | 1–4 | 2 | なし |
| `tfc:gem/sapphire` | 1–4 | 7 | なし |
| 共有 `iaftfc:shared/ingots/dangerous` | 10 | 20 | なし |
| `minecraft:bone` | 10 | 20 | なし |
| `minecraft:book` | 1–2 | 10 | なし |
| `tfc:metal/chain/wrought_iron` | 1–2 | 20 | なし |
| `iceandfire:manuscript` | 1–6 | 20 | なし |
| `tfc:metal/ingot/silver` | 10 | 10 | なし |
| `tfc:ore/small_native_silver` | 16 | 15 | なし |
| `minecraft:skeleton_skull` | 1 | 3 | なし |
| `minecraft:cobweb` | 1–6 | 15 | なし |
| 共有 `iaftfc:shared/equipment/dangerous/sword` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/dangerous/helmet` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/dangerous/chestplate` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/dangerous/greaves` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/dangerous/boots` | 1 | 5 | なし |
| `minecraft:packed_ice` | 1–6 | 7 | なし |
| `minecraft:experience_bottle` | 1–5 | 1 | なし |

**`iceandfire:chest/ice_dragon_male_cave`：抽選回数 1–6、候補総重み 212。**

| 候補 | 個数 | 重み | 追加効果 |
| --- | --- | --- | --- |
| `minecraft:book` | 1 | 12 | Lv30エンチャント |
| 共有 `iaftfc:shared/gems` | 1–4 | 2 | なし |
| `tfc:gem/sapphire` | 1–4 | 3 | なし |
| 共有 `iaftfc:shared/ingots/lategame` | 1–25 | 20 | なし |
| `minecraft:bone` | 1–25 | 20 | なし |
| `tfc:metal/chain/wrought_iron` | 1–2 | 20 | なし |
| `iceandfire:manuscript` | 1–6 | 20 | なし |
| 共有 `iaftfc:shared/ingots/lategame` | 1–25 | 20 | なし |
| `tfc:ore/small_native_gold` | 16 | 30 | なし |
| `minecraft:skeleton_skull` | 1 | 1 | なし |
| `minecraft:cobweb` | 1–6 | 5 | なし |
| 共有 `iaftfc:shared/equipment/lategame/sword` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/lategame/helmet` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/lategame/chestplate` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/lategame/greaves` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/lategame/boots` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/lategame/sword` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/lategame/helmet` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/lategame/chestplate` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/lategame/greaves` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/lategame/boots` | 1 | 5 | なし |
| 共有 `iaftfc:shared/utility` | 1–6 | 7 | なし |
| `minecraft:enchanted_golden_apple` | 1 | 1 | なし |
| `minecraft:experience_bottle` | 1–5 | 1 | なし |

**`iceandfire:chest/lightning_dragon_roost`：抽選回数 1–4、候補総重み 192。**

| 候補 | 個数 | 重み | 追加効果 |
| --- | --- | --- | --- |
| 共有 `iaftfc:shared/gems` | 1–4 | 2 | なし |
| `tfc:gem/topaz` | 1–4 | 7 | なし |
| 共有 `iaftfc:shared/ingots/ordinary` | 10 | 20 | なし |
| `minecraft:bone` | 10 | 20 | なし |
| `minecraft:book` | 1–2 | 10 | なし |
| `tfc:metal/chain/wrought_iron` | 1–2 | 20 | なし |
| `iceandfire:manuscript` | 1–6 | 20 | なし |
| `tfc:metal/ingot/copper` | 10 | 10 | なし |
| `tfc:ore/small_native_copper` | 16 | 15 | なし |
| `minecraft:skeleton_skull` | 1 | 3 | なし |
| `minecraft:cobweb` | 1–6 | 15 | なし |
| 共有 `iaftfc:shared/equipment/ordinary/sword` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/ordinary/helmet` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/ordinary/chestplate` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/ordinary/greaves` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/ordinary/boots` | 1 | 5 | なし |
| `iceandfire:crackled_gravel` | 1–2 | 7 | なし |
| `minecraft:experience_bottle` | 1–5 | 1 | なし |
| `minecraft:flint` | 1–5 | 16 | なし |
| `iceandfire:cannoli` | 1 | 1 | なし |

**`iceandfire:chest/lightning_dragon_female_cave`：抽選回数 1–4、候補総重み 192。**

| 候補 | 個数 | 重み | 追加効果 |
| --- | --- | --- | --- |
| 共有 `iaftfc:shared/gems` | 1–4 | 2 | なし |
| `tfc:gem/topaz` | 1–4 | 7 | なし |
| 共有 `iaftfc:shared/ingots/dangerous` | 10 | 20 | なし |
| `minecraft:bone` | 10 | 20 | なし |
| `minecraft:book` | 1–2 | 10 | なし |
| `tfc:metal/chain/wrought_iron` | 1–2 | 20 | なし |
| `iceandfire:manuscript` | 1–6 | 20 | なし |
| `tfc:metal/ingot/copper` | 10 | 10 | なし |
| `tfc:ore/small_native_copper` | 16 | 15 | なし |
| `minecraft:skeleton_skull` | 1 | 3 | なし |
| `minecraft:cobweb` | 1–6 | 15 | なし |
| 共有 `iaftfc:shared/equipment/dangerous/sword` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/dangerous/helmet` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/dangerous/chestplate` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/dangerous/greaves` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/dangerous/boots` | 1 | 5 | なし |
| `iceandfire:crackled_gravel` | 1–2 | 7 | なし |
| `minecraft:experience_bottle` | 1–5 | 1 | なし |
| `minecraft:flint` | 1–5 | 16 | なし |
| `iceandfire:cannoli` | 1 | 1 | なし |

**`iceandfire:chest/lightning_dragon_male_cave`：抽選回数 1–6、候補総重み 226。**

| 候補 | 個数 | 重み | 追加効果 |
| --- | --- | --- | --- |
| `minecraft:book` | 1 | 12 | Lv30エンチャント |
| 共有 `iaftfc:shared/gems` | 1–4 | 2 | なし |
| `tfc:gem/topaz` | 1–4 | 3 | なし |
| 共有 `iaftfc:shared/ingots/lategame` | 1–25 | 20 | なし |
| `minecraft:bone` | 1–25 | 20 | なし |
| `tfc:metal/chain/wrought_iron` | 1–2 | 20 | なし |
| `iceandfire:manuscript` | 1–6 | 20 | なし |
| `tfc:metal/ingot/copper` | 1–25 | 20 | なし |
| `tfc:ore/small_native_copper` | 16 | 30 | なし |
| `minecraft:skeleton_skull` | 1 | 1 | なし |
| `minecraft:cobweb` | 1–6 | 5 | なし |
| 共有 `iaftfc:shared/equipment/lategame/sword` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/lategame/helmet` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/lategame/chestplate` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/lategame/greaves` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/lategame/boots` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/lategame/sword` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/lategame/helmet` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/lategame/chestplate` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/lategame/greaves` | 1 | 5 | なし |
| 共有 `iaftfc:shared/equipment/lategame/boots` | 1 | 5 | なし |
| 共有 `iaftfc:shared/utility` | 1–6 | 7 | なし |
| `minecraft:enchanted_golden_apple` | 1 | 1 | なし |
| `iceandfire:cannoli` | 1 | 1 | なし |
| `minecraft:flint` | 1–6 | 13 | なし |
| `minecraft:experience_bottle` | 1–5 | 1 | なし |

共有候補集合は、それぞれ独立した1回抽選で1種類を選び、親抽選の個数を適用する。以下の一覧では、特記のない各候補の重みを1とする。装備候補に本mod独自の劣化率、耐久値、エンチャントを加えない。

**`iaftfc:shared/coal_like`**：`minecraft:charcoal`, `tfc:ore/bituminous_coal`, `tfc:ore/lignite`, `tfc:ore/graphite`。

**`iaftfc:shared/gems`**：`tfc:gem/amethyst`, `tfc:gem/diamond`, `tfc:gem/emerald`, `tfc:gem/lapis_lazuli`, `tfc:gem/opal`, `tfc:gem/pyrite`, `tfc:gem/ruby`, `tfc:gem/sapphire`, `tfc:gem/topaz`。

**`iaftfc:shared/utility`**：`tfc:powder/flux`, `minecraft:glass`, `minecraft:glass_pane`, `tfc:kaolin_clay`, `tfc:powder/wood_ash`, `tfc:powder/saltpeter`, `tfc:powder/sylvite`。

**`iaftfc:shared/ingots/ordinary`**：`tfc:metal/ingot/copper`, `tfc:metal/ingot/bronze`, `tfc:metal/ingot/bismuth_bronze`, `tfc:metal/ingot/black_bronze`, `tfc:metal/ingot/wrought_iron`。

**`iaftfc:shared/ingots/dangerous`**：`tfc:metal/ingot/wrought_iron`, `tfc:metal/ingot/steel`。

**`iaftfc:shared/ingots/lategame`**：`tfc:metal/ingot/steel`, `tfc:metal/ingot/black_steel`。

装備共有集合は `iaftfc:shared/equipment/<階級>/<種別>` を用いる。`ordinary` の金属集合は copper, bronze, bismuth_bronze, black_bronze、`dangerous` は wrought_iron、`lategame` は steel とする。各金属について `sword` 集合には `tfc:metal/<sword_blade|javelin_head|mace_head>/<金属>` をそれぞれ重み2、`tfc:metal/<sword|javelin|mace>/<金属>` をそれぞれ重み1で含める。`helmet / chestplate / greaves / boots` 集合には `tfc:metal/unfinished_<部位>/<金属>` を重み2、`tfc:metal/<部位>/<金属>` を重み1で含める。これらの集合は階級3×種別5の15組で、各金属の候補を独立に展開する。

##### AC-F003-001: 属性に合うチェストを利用する

Preconditions:
3属性の巣・洞窟を生成する。

Action:
宝箱の開閉・破壊・保存・再読込を実施する。

Expected Result:
表の樹種のTFCチェストが機能し、内容と残数を再起動後も保持する。他の場所にある一般のチェストを置換しない。

Verification:
- gametest
- persistence
- e2e

##### AC-F003-002: 母岩に適合する鉱石になる

Preconditions:
母岩が単一岩種の洞窟、複数岩種の境界、岩種不明の地形を用意する。

Action:
3属性の洞窟内にある各鉱石階級を調べる。

Expected Result:
金属鉱石はrichのTFC鉱石となり、単一地層では母岩が一致する。属性宝石は対応種類となり、不明地層ではgraniteを使用する。

Verification:
- gametest

##### AC-F003-003: 金属の山のドロップ量

Preconditions:
金銀銅の山を各1～8層用意する。

Action:
適切な方法でそれぞれ破壊する。

Expected Result:
各層数と同数のTFC小鉱石が1種類ずつ得られ、金属ナゲットを代替報酬として生成しない。

Verification:
- gametest

##### AC-F003-004: 9種の戦利品集合を検証する

Preconditions:
9表と共有集合の既定定義が読み込まれた世界。

Action:
全候補の識別、重み、個数範囲、抽選回数、入れ子集合を照合し、固定seedによる抽選を実施する。

Expected Result:
各表は本書の全行と一致し、雄洞窟の重複候補を削って重みを変更しない。報酬は指定候補の範囲内であり、抽選から独立した増殖がない。

Verification:
- unit
- gametest

##### AC-F003-005: チェストの開封状態を保持する

Preconditions:
未開封と開封済みの宝箱を各属性で用意する。

Action:
保存、専用サーバー再起動、別プレイヤーからの再確認を実施する。

Expected Result:
未開封は正常に一度生成され、開封済みは残数のまま復元される。戦利品を再抽選して同じ箱から増殖しない。

Verification:
- persistence
- multiplayer

### F-004: ドラゴンブレスとTFC地形

#### Description

ドラゴンの炎・氷・雷による地形影響は、TFCの草地・土・岩・砂利にも及ぶ。ドラゴンのブレスが地形を変化させる通常の条件が成立した場合に限り、次表の対象を同じ属性のIaF地形ブロックへ変化させる。地形破壊を無効化した設定と保護区域の規則を尊重する。

#### Acceptance Criteria

| 入力ブロックのTFC IDの種類 | 炎の変換先（iceandfire:） | 氷の変換先（iceandfire:） | 雷の変換先（iceandfire:） |
| --- | --- | --- | --- |
| grass/* | chared_grass | frozen_grass | crackled_grass |
| grass_path/* | chared_dirt_path | frozen_dirt_path | crackled_dirt_path |
| dirt/*, coarse_dirt/*, rooted_dirt/*, mud/*, farmland/* | chared_dirt | frozen_dirt | crackled_dirt |
| rock/raw/*, rock/hardened/* | chared_stone | frozen_stone | crackled_stone |
| rock/cobble/*, rock/mossy_cobble/* | chared_cobblestone | frozen_cobblestone | crackled_cobblestone |
| rock/gravel/* | chared_gravel | frozen_gravel | crackled_gravel |

変換先の `chared` はIaF CEの公開IDの綴りであり、`charred` に修正しない。岩種、耕地養分など異なるブロックの専用状態を変換後に誤って持ち越さない。対象外の植物・建物へ本mod固有の変換を拡大しない。変換された3属性の `*_stone` はTFCの崩落起点・崩落可能・崩落誘発の対象に含める。`*_cobblestone` と `*_gravel` は地すべり対象、`*_dirt_path` は地すべりの支持面とする。発生条件・確率・報酬はTFCによる。

##### AC-F004-001: 地形変換の種類を区別する

Preconditions:
各入力種類を2つ以上のTFC土・岩種で準備し、ブレス地形変換を許可する。

Action:
炎・氷・雷それぞれのブレスを対象へ当てる。

Expected Result:
変換後に表の対応IDを確認でき、属性を混同しない。表外のTFC建材に本mod専用の変換は生じない。

Verification:
- gametest
- e2e

##### AC-F004-002: 地形破壊を禁止できる

Preconditions:
ブレス破壊を許可した場所と、設定によって禁止した場所を用意する。

Action:
それぞれ同じ属性のブレスを発生させ、保存して再読み込みする。

Expected Result:
許可側の地形変化は保存され、禁止側で本modの処理が破壊禁止を迂回しない。

Verification:
- gametest
- persistence

##### AC-F004-003: 変換後の地盤がTFCの対象になる

Preconditions:
表で生成される各属性のstone、cobblestone、gravel、dirt_pathを配置する。

Action:
TFCの地盤支持条件を成立させて挙動を確認する。

Expected Result:
stoneは指定崩落判定、cobblestone・gravelは地すべり、dirt_pathは支持の対象となる。地盤の挙動はTFCの標準規則に従う。

Verification:
- gametest

### F-005: TFC食料によるドラゴンの給餌

#### Description

飼い主がドラゴンに渡す食料およびドラゴンが地面から拾う食料に、TFCで新鮮と判定された肉を使用できる。TFCの腐敗判定がある食品は、見かけの種類が肉でも腐敗していれば通常の餌にしない。ドラゴンの所有者・成長段階・体力・空腹に関するIaF CEの通常条件は維持する。ドラゴン専用の特殊な餌の固有効果は通常の肉と混同しない。

#### Acceptance Criteria

##### AC-F005-001: 新鮮な肉を与える

Preconditions:
所有・成長段階・回復可能性が通常の給餌条件を満たすドラゴン。新鮮なTFC肉と腐敗した同種肉を用意する。

Action:
両者を手渡して比較する。

Expected Result:
新鮮な肉は通常給餌として受け付け、上流の回復・消費規則を適用する。腐敗肉は消費せず給餌が成立しない。

Verification:
- gametest
- e2e

##### AC-F005-002: 地面の餌の腐敗を再判定する

Preconditions:
ドラゴンの近くに新鮮な肉と腐敗した肉を落とす。追跡途中に腐敗する肉も用意する。

Action:
ドラゴンが餌を探索し拾うまで観察する。

Expected Result:
新鮮な餌は通常条件で食べられ、腐敗肉と追跡中に腐敗した肉は食べられない。腐敗を固定時刻だけで判定しない。

Verification:
- gametest

##### AC-F005-003: 特殊餌と対象外食品を保護する

Preconditions:
肉以外の一般食品とIaF CEの特殊なドラゴン用食料を用意する。

Action:
通常給餌および特殊給餌を試す。

Expected Result:
一般食品を無条件に餌へ追加しない。特殊餌による成長等の既存効果は、腐敗肉拒否の追加条件によって失われない。

Verification:
- gametest

### F-006: ドラゴン卵の孵化

#### Description

IaF CEのドラゴン卵の通常の孵化条件に、TFCの火源と淡水を加える。炎の卵は通常の火に加え、点火中のTFC焚き火、または熱を持つ木炭炉を卵の位置または直下の有効な位置に置いた場合にも孵化条件を満たす。氷の卵はTFCの淡水の水源または流れる淡水で孵化条件を満たす。TFCの塩水を淡水と同じものとして扱わない。雷の卵には上流にない落雷の必須条件を追加しない。

#### Acceptance Criteria

##### AC-F006-001: 炎の卵とTFCの熱源

Preconditions:
炎の卵と通常の火、点火中・消火済みのTFC焚き火、熱い・冷えた木炭炉を用意する。

Action:
卵の位置・直下・隣接だけの位置を分けて育成を試す。

Expected Result:
指定した位置の有効な熱源では炎の孵化進行が始まり、冷えた設備や無関係な位置では追加孵化条件が成立しない。

Verification:
- gametest
- e2e

##### AC-F006-002: 氷の卵と淡水・塩水

Preconditions:
氷の卵をTFC淡水の水源、流水、塩水、乾燥した場所へ置く。

Action:
孵化の状態を比較する。

Expected Result:
淡水の2条件は有効、塩水と乾燥は本mod追加の淡水条件を満たさない。通常のIaF CE条件は損なわない。

Verification:
- gametest

##### AC-F006-003: 孵化を完了し状態を保存する

Preconditions:
有効な環境の炎・氷の卵と通常条件の雷の卵を用意する。

Action:
孵化まで進め、途中でワールドを保存・再起動し、クライアント2台で確認する。

Expected Result:
孵化時間・段階・所有者等はIaF CEの規則に従い、卵が二重に孵化したり途中経過が不正に消失したりしない。

Verification:
- persistence
- multiplayer
- e2e

### F-007: ドラゴンスティールの材料・金床・建材

#### Description

炎・氷・雷のドラゴンスティールは、それぞれ異なるTFC加工用金属として認識される。素材はインゴット、ダブルインゴット、板、ダブルシート、棒、道具部品5種、未完成防具4種の14形態を用意し、各属性で同じ体系を持つ。すべての形態の外観と名称は現行のアセットと対応させる。完成した道具やプレイヤー用防具はIaF CEの既存アイテムを使用し、同じ用途の新しい完成品IDを重複追加しない。

#### Acceptance Criteria

14形態 `p` は `ingot, double_ingot, sheet, double_sheet, rod, axe_head, hoe_head, pickaxe_head, shovel_head, sword_blade, unfinished_helmet, unfinished_chestplate, unfinished_greaves, unfinished_boots` とする。各属性の素材の公開IDは `iaftfc:metal/<p>/<m>`、計42種類であり、すべてEpic相当の希少度を持つ。

3属性それぞれに、次の建材・作業設備を用意する。ブロックと持ち物としての形を区別し、通常の設置、向き、回収を実行できること。

| 用途 | 公開ID | 観察可能な性質 |
| --- | --- | --- |
| 金属ブロック | `iaftfc:metal/block/<m>` | 立方体、通常回収で1個 |
| ハーフブロック | `iaftfc:metal/block/<m>_slab` | 上・下・二重を区別、単体の回収1個、二重の回収2個 |
| 階段 | `iaftfc:metal/block/<m>_stairs` | 向き・上下・内外角を区別、通常回収1個 |
| 金床 | `iaftfc:metal/anvil/<m>` | TFCのTier 7の金床として使用、通常回収1個 |

追加アイテムの全体数は、42金属素材、12ブロックの所持品形態、5種類のドラゴン骨部品、16種類のドラゴン用防具の計75種類である。建材・金床のブロック登録は別途12種類。追加ブロックを掘る道具はTFCのツルハシ相当とし、独自の風化段階は追加しない。資材の製作方法が本文に存在しないことを根拠なく埋める新レシピは設けない。

素材の公開タグは `#c:ingots/<m>`、`#c:double_ingots/<m>`、`#c:sheets/<m>`、`#c:double_sheets/<m>`、`#c:rods/<m>` とし、対応する形態を含む。対応する全体タグにも加入する。通常金属ブロックは `#c:storage_blocks/<m>` のアイテム・ブロック両方に含め、全体の `#c:storage_blocks` と連結する。追加金床3種はTFCの金床タグへ追加する。これらのタグ追加でTFC/IaF CEの既存メンバーを消さない。

3属性の溶融金属はTFCの金属として認識され、源液体IDを `tfc:metal/<m>`、流動側IDを `tfc:metal/flowing_<m>` とする。対応する `#c:molten_<m>` と `#tfc:molten_metals` に加え、TFCの容器で量と属性を保持する。炎は `#6E211B`、氷は `#6BADC9`、雷は `#4D3A69` を代表色とする。溶融金属は水・淡水として動作しない。ワールドへ置く専用液体ブロックや別種のバケツを新設しない。

##### AC-F007-001: 3属性の全形態が存在する

Preconditions:
1.21.1の正常な必須mod構成。

Action:
追加アイテム75種類、ブロック12種類、液体3属性の公開IDとゲーム内の一覧を確認する。

Expected Result:
各対象が表の展開数だけ存在し、属性を取り違えない。完成道具・防具に重複した新IDが増えず、42金属素材はEpic相当である。

Verification:
- gametest

##### AC-F007-002: 建材を設置し回収する

Preconditions:
3属性のブロック、ハーフ、階段、金床を所持する。

Action:
各形状・向きで設置し、保存してから適切な道具で通常破壊する。

Expected Result:
状態と外見が一致し、指定の個数を回収する。金床の中身はTFCの通常規則で処理され、消失・二重ドロップしない。

Verification:
- gametest
- persistence
- visual

##### AC-F007-003: 溶融金属の属性を維持する

Preconditions:
3属性の金属を100 mBずつTFCの対応容器に入れる。

Action:
容器の移動、容量表示、別容器への移送、保存・再起動を実施する。

Expected Result:
量と属性IDを保持し、混合・勝手な変換・量の増殖がない。色で3属性を識別できる。

Verification:
- gametest
- persistence
- visual

### F-008: ドラゴン炉によるインゴット製造

#### Description

IaF CEの炎・氷・雷のドラゴン炉に、対応するTFC鋼インゴット1個と同属性のドラゴンの血1個を投入し、ドラゴンスティール・インゴット1個を得る。鋼の予熱は不要である。ドラゴン炉の組立、ドラゴンからの供給、スロット、動作停止条件はIaF CEの通常の炉と同じであり、別の炉や独自の画面を追加しない。

#### Acceptance Criteria

| 公開レシピID | 入力インゴット | 血 | 出力 | 既定進捗量 | 完成直後の温度 |
| --- | --- | --- | --- | --- | --- |
| iceandfire:dragonforge/dragonsteel_fire_ingot | #c:ingots/red_steel | iceandfire:fire_dragon_blood | iaftfc:metal/ingot/dragonsteel_fire | 1000 | 1800℃ |
| iceandfire:dragonforge/dragonsteel_ice_ingot | #c:ingots/blue_steel | iceandfire:ice_dragon_blood | iaftfc:metal/ingot/dragonsteel_ice | 1000 | 1800℃ |
| iceandfire:dragonforge/dragonsteel_lightning_ingot | #c:ingots/black_steel | iceandfire:lightning_dragon_blood | iaftfc:metal/ingot/dragonsteel_lightning | 1000 | 1800℃ |

進捗量1000は処理条件の規定値であり、炉の供給状況に関係なく固定の実時間で完成することを保証しない。温度1800℃は**生成が成功した直後の出力スタック**の値である。同じアイテムの既存出力へ加わる場合、その出力スタック全体が1800℃となる。取り出した実物は通常のTFCの熱容量・周囲条件に応じて冷却され、製造していない時間に自動的に1800℃へ戻らない。レシピの完成見本は製造直後の温度を表すものであり、実物と同じ経時冷却はしない。

炉の素材となる属性レンガは、`tfc:fire_bricks` 4個と同属性のドラゴン鱗ブロック5個から4個を得る。配置は `SBS / BSB / SBS`、`B` は耐火レンガ、`S` は属性鱗ブロックである。炉心には属性レンガ8個と対応するドラゴンの心臓1個を `BBB / BHB / BBB` として用い、入力部には属性レンガ4個と鉄インゴット4個を `BIB / I.I / BIB` として用いる。これらの炉部品はIaF CEの既存形態であり、追加固有機械ではない。

出力欄が満杯・別種で占有される場合、正規材料を不正に消費しない。異なる属性の血や鋼からドラゴンスティールを生成しない。IaF CEが無効材料から通常の代替結果を生成する場面では、その結果を改変しない。代替結果は炎・雷が `iceandfire:ash`、氷が `iceandfire:dragon_ice` であり、ドラゴンスティールとして1800℃の熱付与を受けない。

##### AC-F008-001: 全属性で1800℃の出力を得る

Preconditions:
3属性の炉が組み上がり、対応ドラゴンによる稼働条件を満たす。出力欄は空。

Action:
表の入力と血を各1個投入し、完成させる。

Expected Result:
材料を各1個だけ消費し、正しい属性のインゴット1個が完成直後に1800℃となる。温度と進捗は各属性で同じ条件を満たす。

Verification:
- gametest
- e2e

##### AC-F008-002: 製造後は通常の温度変化を受ける

Preconditions:
既に冷えた同属性インゴット1個が炉の出力欄にあり、2個目を生成する。

Action:
完成直後の温度を確認し、取り出して加熱せず時間を経過させる。

Expected Result:
生成時に出力スタック全体が1800℃となり、その後はTFCの規則に従い冷却する。他のスロットや所持品を無関係に加熱しない。

Verification:
- gametest

##### AC-F008-003: 異種入力・出力満杯で不正な製品を生成しない

Preconditions:
出力欄が満杯、異種品で占有、不適合材料の各状態を用意する。

Action:
炉へドラゴン供給を行い、加工結果と材料残数を確認する。

Expected Result:
満杯・異種出力で正規材料を失わず、不適合材料からドラゴンスティールは生成されない。IaF CEの通常代替結果を勝手に消さず、代替結果へ1800℃を付与しない。

Verification:
- gametest

##### AC-F008-004: 炉の進捗を保存・共有する

Preconditions:
専用サーバーでA/Bが稼働途中の炉を観察している。

Action:
サーバーを保存・再起動し、必要な供給を再開して完成品を取り出す。

Expected Result:
炉の材料・出力・IaF CEが保存する進捗が保持される。A/Bで数量と温度が一致し、再起動で完成品が複製されない。

Verification:
- persistence
- multiplayer

##### AC-F008-005: ゲームデータから温度を変更できる

Preconditions:
既定温度1800℃で正常に完成する炉と、温度を1500℃へ変更した管理者設定を用意する。

Action:
設定を適用してレシピの完成見本と次に製造した実物を比較する。

Expected Result:
管理者が指定した1500℃が新規製品と見本に適用され、既に別の場所で保管した製品の温度は一括変更されない。0以下や非有限値によって温度・ゲーム状態が破壊されない。

Verification:
- gametest
- multiplayer

### F-009: TFC金属加工とプレイヤー用装備

#### Description

ドラゴン炉で得た金属を、TFCの加熱・金床・溶接・鋳造工程で工具部品と防具部品にし、IaF CEの完成品へ仕上げる。全属性で鍛造可能温度は1200℃以上、溶接可能温度は1600℃以上、溶解開始温度は2000℃以上とする。各温度の境界値そのものは有効とする。ハンマー、融剤、加工の成功条件、金床の作業はTFCの規則による。

#### Acceptance Criteria

Tier 7の専用金床を初めて製作できるように、同属性インゴット2個からダブルインゴット1個への溶接だけはTier 6の金床で行える。ダブルインゴット7個を `### / .#. / ###` に並べるとTier 7の `iaftfc:metal/anvil/<m>` 1個を得る。混合属性のダブルインゴットは受け付けない。ダブルシートは同属性シート2個をTier 7以上の金床で溶接して1個にする。

Tier 7以上の金床で各形態を鍛造する条件を以下に定める。`last` は最後、`second_last` は最後から2番目、`third_last` は最後から3番目、`not_last` は最後以外、`any` は任意の位置を表す。`hit / punch / upset / draw / bend / shrink` はTFCの既存の加工操作に対応する。加工の最終的な成否はTFCの加工目標と操作履歴に従い、必要な最終操作だけを行えば無条件に成功するわけではない。

| 出力形態 | 入力×数量 | 結果数 | 終了操作条件 | 鍛造品質の継承 |
| --- | --- | --- | --- | --- |
| axe_head | ingot×1 | 1 | punch_last / hit_second_last / upset_third_last | 有 |
| hoe_head | ingot×1 | 1 | punch_last / hit_not_last / bend_not_last | 有 |
| pickaxe_head | ingot×1 | 1 | punch_last / draw_not_last / bend_not_last | 有 |
| shovel_head | ingot×1 | 1 | punch_last / hit_not_last | 有 |
| sword_blade | double_ingot×1 | 1 | hit_last / bend_second_last / bend_third_last | 有 |
| rod | ingot×1 | 2 | hit_last / hit_second_last / hit_third_last | 無 |
| sheet | double_ingot×1 | 1 | hit_third_last / hit_second_last / hit_last | 無 |
| unfinished_helmet | double_sheet×1 | 1 | hit_last / bend_second_last / bend_third_last | 有 |
| unfinished_chestplate | double_sheet×1 | 1 | hit_last / hit_second_last / upset_third_last | 有 |
| unfinished_greaves | double_sheet×1 | 1 | hit_any / draw_any / bend_any | 有 |
| unfinished_boots | sheet×1 | 1 | bend_last / bend_second_last / shrink_third_last | 有 |

道具は、同属性の `iaftfc:metal/<axe_head|hoe_head|pickaxe_head|shovel_head|sword_blade>/<m>` と木製棒 `#c:rods/wooden` を縦に組み合わせ、`iceandfire:dragonsteel_<e>_<axe|hoe|pickaxe|shovel|sword>` を各1個得る。道具部品の鍛造品質は完成道具へ引き継ぐ。完成道具の攻撃力、採掘性能、属性効果は採用したIaF CEの規則に従い、本mod独自の倍率を重複適用しない。

| 防具の完成品 | 必要な未完成部品 | 追加の同属性板材 | 金床 | 出力 |
| --- | --- | --- | --- | --- |
| helmet | unfinished_helmet | sheet×1 | Tier 7 | iceandfire:dragonsteel_<e>_helmet |
| chestplate | unfinished_chestplate | double_sheet×1 | Tier 7 | iceandfire:dragonsteel_<e>_chestplate |
| leggings | unfinished_greaves | sheet×1 | Tier 7 | iceandfire:dragonsteel_<e>_leggings |
| boots | unfinished_boots | sheet×1 | Tier 7 | iceandfire:dragonsteel_<e>_boots |

プレイヤー防具は上表の2入力を溶接して完成させ、より良い方のTFC鍛造品質を引き継ぐ。未完成の `greaves` と完成品の `leggings` の対応を維持する。原料インゴットをバニラの防具形状へ並べるだけで同じ完成品が得られる経路は設けない。

加熱・溶解する金属は属性別の溶融金属 `tfc:metal/<m>` として回収できる。次表は1品目を溶融できた場合の既定の回収mBと熱容量である。液量の差を建材や防具の外観から推測し直さない。

| 対象形態（各属性） | 回収量 mB | 熱容量 | 損傷による減額 |
| --- | --- | --- | --- |
| ingot、axe_head、hoe_head、pickaxe_head、shovel_head | 100 | 2.857143 | なし |
| rod | 50 | 1.428572 | なし |
| double_ingot、sheet、sword_blade、unfinished_boots | 200 | 5.714286 | なし |
| double_sheet、unfinished_helmet、unfinished_chestplate、unfinished_greaves | 400 | 11.428572 | なし |
| block | 100 | 2.857143 | なし |
| block_slab | 50 | 1.428572 | なし |
| block_stairs | 75 | 2.142857 | なし |
| anvil | 1400 | 40.000002 | なし |
| 完成axe、hoe、pickaxe、shovel | 100 | 2.857143 | あり |
| 完成sword | 200 | 5.714286 | あり |
| 完成helmet、leggings | 600 | 17.142858 | なし |
| 完成chestplate | 800 | 22.857144 | なし |
| 完成boots | 400 | 11.428572 | なし |

道具の「損傷による減額あり」は `floor(表の基準量 × 残耐久値÷最大耐久値)` とする。防具は損傷していても表の基準量のままとする。液体の融点は2000℃、比熱設定値は0.00857。再溶融・加熱条件はTFCの通常の熱履歴に従い、入手した全ての炉・設備が必ず2000℃へ到達するという新たな保証はしない。

TFCのインゴット鋳型に同属性の溶融金属100 mBを入れ、通常の冷却・取出し条件を満たすと本modインゴット1個を得る。鋳型破損確率は0.1（10%）。材料が不足した場合、異種の金属や鋳型を用いた場合は同じ製品を得ない。

##### AC-F009-001: Tier 6からTier 7へ到達する

Preconditions:
3属性のインゴット各14個、Tier 6金床、必要なTFC加工用品を用意する。

Action:
各属性でインゴットからダブルインゴット7個を作り、対応金床を製作する。

Expected Result:
Tier 6で同属性ダブルインゴットへ溶接でき、専用金床なしにTier 7金床1個へ到達できる。混合属性からは作れない。

Verification:
- gametest
- e2e

##### AC-F009-002: 加工温度と金床Tierの境界を区別する

Preconditions:
1199.9/1200/1599.9/1600/1999.9/2000℃に相当する材料を用意する。

Action:
鍛造・溶接・溶解を各温度とTier 6/7で試す。

Expected Result:
鍛造は1200℃以上、溶接は1600℃以上、溶解は2000℃以上で有効。ダブルインゴット以外のTier 7作業をTier 6で完了できない。

Verification:
- unit
- gametest

##### AC-F009-003: 全形態をTFC鍛造で得る

Preconditions:
全属性の適温材料とTier 7金床、TFC作業に必要な用品。

Action:
表の11形態×3属性について操作規則を満たす加工を実施する。

Expected Result:
33種類の形成レシピが成立し、出力数、加工終了操作、品質の有無が一致する。属性混合の加工は成立しない。

Verification:
- gametest

##### AC-F009-004: 完成品へ品質を引き継ぐ

Preconditions:
異なる品質の道具部品と未完成防具部品を用意する。

Action:
5種の道具を組み立て、4部位の防具を溶接する。

Expected Result:
15道具と12防具について属性・種類・数量が一致し、道具は部品の品質、防具は2入力中の良い品質を引き継ぐ。

Verification:
- gametest
- e2e

##### AC-F009-005: 金属の再溶解量を一致させる

Preconditions:
3属性の表の全対象と耐久が異なる完成装備を用意する。

Action:
各1品を溶融させ、金属の種類と回収量を確認する。

Expected Result:
対象の回収量は全81品目で表どおり。道具だけが残耐久率で減り、損傷した防具の回収量は表の既定値のままとなる。

Verification:
- unit
- gametest

##### AC-F009-006: 鋳型の入出力条件を守る

Preconditions:
3属性の溶融金属、空のTFCインゴット鋳型、量不足および異種金属の対照を用意する。

Action:
100 mBを鋳込み、通常の冷却を経て取り出す。

Expected Result:
対応インゴットを1個得る。型破損率の既定値は10%。量不足や属性不一致で同じ結果を得ず、再起動を挟んでも複製しない。

Verification:
- gametest
- persistence

### F-010: ドラゴン骨の削り加工

#### Description

ドラゴンの骨をTFCの5×5削り加工で、斧・鍬・ツルハシ・シャベル・剣の部品、およびドラゴンフルートへ加工する。加工を開始するには `iceandfire:dragonbone` が2個必要だが、実際に消費する量は1個である。初めてマスを削った時点で1個消費し、開いただけで閉じると消費しない。完成せず途中終了した場合も、既に削り始めた1個は戻さない。骨の削り画面は石の加工音と粒子を用い、残したマスと削ったマスを区別する。

#### Acceptance Criteria

次の図は5行×5列で、`#`は残す部分、`.`は削除部分を表す。正しい図で完成した場合の結果は各1個とする。

| 出力ID | 図（上から下の5行） |
| --- | --- |
| iaftfc:dragonbone/axe_head | .#... / ####. / ##### / ####. / .#... |
| iaftfc:dragonbone/hoe_head | ..... / ##### / ...## / ..... / ..... |
| iaftfc:dragonbone/pickaxe_head | ..... / .###. / #...# / ..... / ..... |
| iaftfc:dragonbone/shovel_head | .###. / .###. / .###. / .###. / ..#.. |
| iaftfc:dragonbone/sword_blade | ##... / ###.. / .###. / ..##. / ....# |
| iceandfire:dragon_flute | ##### / #.#.# / ##..# / #..## / ##### |

5種類の骨部品はそれぞれTFC式の木柄ではなく `#c:bones/wither` のウィザーの骨1個を縦に組み合わせ、`iceandfire:dragonbone_<axe|hoe|pickaxe|shovel|sword>` 各1個になる。普通の骨や木の棒だけでは、この組立経路を満たさない。

##### AC-F010-001: 開始枚数と消費枚数を区別する

Preconditions:
生存モードで骨1個、2個、3個を用意する。

Action:
1個で開始を試み、2個では開閉だけ、初回削りで終了、完成取得を試す。

Expected Result:
1個では開始できず、2個で開閉だけなら消費0、削りを始めたら消費1、完成時も消費は合計1個となる。

Verification:
- gametest
- e2e

##### AC-F010-002: 骨部品5種類とフルートの図を再現する

Preconditions:
骨が十分あり、5×5画面が使用できる。

Action:
正規6図を完成させ、1マス異なる図も比較する。

Expected Result:
6通りで各1個の対応品を得る。不一致図から誤った品が出ない。

Verification:
- gametest

##### AC-F010-003: ウィザーの骨による道具組立

Preconditions:
骨部品5種、ウィザーの骨、普通の骨、木の棒を所持する。

Action:
各柄を使って5道具の組立を試す。

Expected Result:
ウィザーの骨タグに合う柄だけでIaF CEの完成道具各1個を得る。

Verification:
- gametest

##### AC-F010-004: 加工途中の再接続で増殖しない

Preconditions:
Aが骨2個を持ち、削りを1回行った。

Action:
Aが切断・再接続し、所持品と結果品を確認する。

Expected Result:
消費1個が戻らず、未完成の骨部品や完成品が意図せず生成されない。

Verification:
- multiplayer

### F-011: 12色のドラゴン鱗の削り加工

#### Description

12色のドラゴンの鱗を5×5画面で加工し、それぞれヘルメット、チェストプレート、レギンス、ブーツを作る。同色の鱗を5枚用意して開始し、完成品を取得したときに5枚を消費する。未完成のまま閉じれば消費しない。完成状態で終了して自動的に結果品を受け取った場合は、5枚の消費と防具1個の入手を一組として扱う。

#### Acceptance Criteria

対応色 `c` は `amethyst, black, blue, bronze, copper, electric, gray, green, red, sapphire, silver, white` の12種とする。入力は `iceandfire:dragonscales_<c>`、出力は `iceandfire:armor_<c>_<helmet|chestplate|leggings|boots>`。同じ鱗色だけが一回の加工で使用でき、異なる色の鱗を計5枚集めても同一色の材料としては扱わない。加工画面の各色の模様は既存の鱗のアセットに従う。

| 防具部位 | 残すマスの図（上から下） | 5行に満たない図の領域 |
| --- | --- | --- |
| helmet | ##### / #...# / #...# | 4行目と5行目は削除済み |
| chestplate | #...# / ##### / ##### / ##### / ##### | なし |
| leggings | ##### / ##### / ##.## / ##.## / ##.## | なし |
| boots | ##... / ##... / ##... / ####. / ##### | なし |

公開レシピIDでは `amethyst` の4防具について `iceandfire:armor_amythest_<部位>` の綴りを使用するが、完成品のIDは `iceandfire:armor_amethyst_<部位>` とする。他の11色の公開レシピIDは完成品IDと同じ表記を使う。銀色鱗による `armor_silver_*` と銀金属による `armor_silver_metal_*` は異なる防具である。

##### AC-F011-001: 5枚の開始と完成時の消費を守る

Preconditions:
同色4枚、同色5枚、異なる色が混じった5枚の鱗を用意する。

Action:
加工画面を開き、未完成で閉じる場合と、完成品を取得する場合を比較する。

Expected Result:
同色5枚だけが開始条件を満たす。未完成終了では消費0、完成取得では5枚消費・防具1個入手となる。

Verification:
- gametest
- e2e

##### AC-F011-002: 48通りの防具が正しく完成する

Preconditions:
12色の鱗をそれぞれ必要数所持する。

Action:
各色につき4種の図を完成させ、helmetの図外マスが残る場合も検証する。

Expected Result:
48種類が対応色・部位で各1個出力され、図外の余分なマスは不適合となる。amethystの綴りと銀色防具の区別を保つ。

Verification:
- gametest

##### AC-F011-003: 終了直前の切断で材料を増殖させない

Preconditions:
Aが鱗5枚で加工を完成し、Bが同じサーバーに接続している。

Action:
取得直前または画面終了直後にAの接続を切り、再接続する。

Expected Result:
完成品1個と鱗5枚の消費が対応し、二重取得・原料復元・他プレイヤーへの漏出がない。

Verification:
- multiplayer

### F-012: ドラゴンに装着する金属防具

#### Description

プレイヤー用防具とは別に、ドラゴンの頭・首・胴・尾へ装着する防具をTFCのダブルシートから製作する。製作可能な材質は錬鉄、鋼、黒鋼、赤鋼、青鋼、および炎・氷・雷ドラゴンスティールの8種類であり、各材質に4部位、計32種類の製作結果を持つ。

#### Acceptance Criteria

| 材質 | 原料タグ | 出力ID（部位 `s` を展開） |
| --- | --- | --- |
| iron | #c:double_sheets/wrought_iron | iceandfire:dragonarmor_iron_<s> |
| steel | #c:double_sheets/steel | iaftfc:dragonarmor_steel_<s> |
| black_steel | #c:double_sheets/black_steel | iaftfc:dragonarmor_black_steel_<s> |
| red_steel | #c:double_sheets/red_steel | iaftfc:dragonarmor_red_steel_<s> |
| blue_steel | #c:double_sheets/blue_steel | iaftfc:dragonarmor_blue_steel_<s> |
| dragonsteel_fire | #c:double_sheets/dragonsteel_fire | iceandfire:dragonarmor_dragon_steel_fire_<s> |
| dragonsteel_ice | #c:double_sheets/dragonsteel_ice | iceandfire:dragonarmor_dragon_steel_ice_<s> |
| dragonsteel_lightning | #c:double_sheets/dragonsteel_lightning | iceandfire:dragonarmor_dragon_steel_lightning_<s> |

部位 `s` は `head / neck / body / tail` の4種類である。各部位の図は同じ材質のダブルシートだけで成立する。以下の配置で各1個を製作する。

| 部位 | 材料配置 | 必要なダブルシート | 金属量換算 |
| --- | --- | --- | --- |
| head | ... / .## / ### | 5 | 2000 mB |
| neck | ... / ### / .## | 5 | 2000 mB |
| body | ### / ### / #.# | 8 | 3200 mB |
| tail | ... / ..# / ##. | 3 | 1200 mB |

全4部位で21ダブルシート、計8400 mB相当の原料を消費する。鋼・黒鋼・赤鋼・青鋼の4材質は、本modが各部位の装備を追加する。4材質の保護寄与値は部位1個につき、鋼3.5、黒鋼5.5、赤鋼7.5、青鋼7.5とする。これらの値はTFCの物理ダメージ耐性値やダメージ軽減率の百分率とは異なる。装備の有効性と成長段階による装着制限はIaF CEの通常規則に従う。

##### AC-F012-001: 8材質×4部位を製作する

Preconditions:
8材質のTFCダブルシートとクラフト設備を用意する。

Action:
32通りについて図と投入量を検証し、単シートや別材質を混ぜて試す。

Expected Result:
正しい図から指定IDの防具を各1個得る。頭5、首5、胴8、尾3枚を消費し、不適合な材料では成立しない。

Verification:
- gametest

##### AC-F012-002: 追加した4材質をドラゴンへ装着する

Preconditions:
各属性のドラゴンと追加16種類の防具を用意する。

Action:
頭・首・胴・尾へそれぞれ装着・解除し、別部位への操作も試す。

Expected Result:
正しい部位に装着でき、素材・装着位置に対応した外見が表示される。不適合部位から保護を得ない。

Verification:
- e2e
- visual

##### AC-F012-003: 防具の寄与値と装着状態を保持する

Preconditions:
同じドラゴンに4材質を部位ごとに付け替え、A/Bで観察する。

Action:
寄与値を確認し、保存・再起動・再接続を実施する。

Expected Result:
部位あたり鋼3.5、黒鋼5.5、赤鋼7.5、青鋼7.5と一致する。装着状態と効果はサーバー・A/Bで一致し、再起動後も保持される。

Verification:
- gametest
- persistence
- multiplayer

### F-013: 食料の栄養・腐敗とアイテム収納

#### Description

IaF CEの食料をTFCの栄養・腐敗へ対応させ、特殊な素材、卵、頭蓋骨、武器、防具、設置物にTFCのサイズ・重量区分を適用する。サイズと重量はTFCの分類値であり、独自のkg値や収納計算は追加しない。腐敗した食料を再ログイン時に新鮮へ戻さず、TFCの保存・鮮度規則に従う。

#### Acceptance Criteria

以下は11種類の食品の既定値である。水分および栄養列はTFCの区分別値、腐敗倍率は保存条件と組み合わせる係数を意味し、固定の日数ではない。3属性のドラゴン肉は生肉の食品集合にも含める。

| 食品ID | hunger | saturation | 水分 | 穀物 | 果物 | 野菜 | たんぱく質 | 乳製品 | 腐敗倍率 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `iceandfire:ambrosia` | 5 | 1.5 | 0 | 0 | 0.8 | 0 | 0 | 0.3 | 2.5 |
| `iceandfire:cannoli` | 20 | 5.0 | 0 | 1.5 | 0 | 0 | 0 | 3.0 | 2.5 |
| `iceandfire:cooked_rice_with_fire_dragon_meat` | 4 | 1.5 | 20.0 | 1.7 | 0 | 2.0 | 3.0 | 0 | 3.5 |
| `iceandfire:cooked_rice_with_ice_dragon_meat` | 4 | 1.5 | 20.0 | 1.7 | 0 | 2.0 | 3.0 | 0 | 3.5 |
| `iceandfire:cooked_rice_with_lightning_dragon_meat` | 4 | 1.5 | 20.0 | 1.7 | 0 | 2.0 | 3.0 | 0 | 3.5 |
| `iceandfire:fire_dragon_flesh` | 8 | 2.0 | 0 | 0 | 0 | 0 | 3.0 | 0 | 2.0 |
| `iceandfire:ghost_cream` | 4 | 1.5 | 10.0 | 0 | 0 | 0 | 0 | 2.0 | 2.5 |
| `iceandfire:ice_dragon_flesh` | 8 | 2.0 | 0 | 0 | 0 | 0 | 3.0 | 0 | 2.0 |
| `iceandfire:lightning_dragon_flesh` | 8 | 2.0 | 0 | 0 | 0 | 0 | 3.0 | 0 | 2.0 |
| `iceandfire:pixie_dust` | 1 | 0.8 | 0 | 0 | 0 | 0 | 0 | 0 | 0.5 |
| `iceandfire:pixie_dust_milky_tea` | 4 | 1.5 | 20.0 | 0 | 0 | 0 | 0 | 2.0 | 2.5 |

以下のサイズ・重量表に示すデータ群は、全34群である。`iaftfc:item_size/<群名>` のタグに属するアイテムには対応する1行のサイズ・重量を適用する。`iaftfc:dragonarmor_<材質>_<部位>` は16品すべて `very_large`・`very_heavy`。骨部品5種、金属の27部品、金属ブロック3種、ハーフ3種、階段3種については表の専用行を優先する。

| 定義群 | 対象 | サイズ | 重量 |
| --- | --- | --- | --- |
| dragon_armor | 追加ドラゴン用防具16品 | very_large | very_heavy |
| iaf_armor | `#iaftfc:item_size/iaf_armor` | large | very_heavy |
| iaf_blocks | `#iaftfc:item_size/iaf_blocks` | small | light |
| iaf_bottles | `#iaftfc:item_size/iaf_bottles` | normal | medium |
| iaf_chains | `#iaftfc:item_size/iaf_chains` | normal | heavy |
| iaf_chests | `#iaftfc:item_size/iaf_chests` | large | light |
| iaf_cloth | `#iaftfc:item_size/iaf_cloth` | small | light |
| iaf_default | `#iaftfc:item_size/iaf_default` | very_small | very_light |
| iaf_dragon_armor | `#iaftfc:item_size/iaf_dragon_armor` | very_large | very_heavy |
| iaf_dragon_bones | `#iaftfc:item_size/iaf_dragon_bones` | very_large | medium |
| iaf_dragon_eggs | `#iaftfc:item_size/iaf_dragon_eggs` | very_large | very_heavy |
| iaf_dragon_skulls | `#iaftfc:item_size/iaf_dragon_skulls` | huge | very_heavy |
| iaf_dragonsteel_armor | `#iaftfc:item_size/iaf_dragonsteel_armor` | large | very_heavy |
| iaf_empty_jars | `#iaftfc:item_size/iaf_empty_jars` | normal | medium |
| iaf_filled_jars | `#iaftfc:item_size/iaf_filled_jars` | normal | heavy |
| iaf_foods | `#iaftfc:item_size/iaf_foods` | small | light |
| iaf_furniture | `#iaftfc:item_size/iaf_furniture` | large | light |
| iaf_ingots | `#iaftfc:item_size/iaf_ingots` | large | medium |
| iaf_large_vessels | `#iaftfc:item_size/iaf_large_vessels` | huge | heavy |
| iaf_logs | `#iaftfc:item_size/iaf_logs` | very_large | medium |
| iaf_ores | `#iaftfc:item_size/iaf_ores` | small | medium |
| iaf_plants | `#iaftfc:item_size/iaf_plants` | tiny | very_light |
| iaf_rods | `#iaftfc:item_size/iaf_rods` | normal | light |
| iaf_sheets | `#iaftfc:item_size/iaf_sheets` | large | medium |
| iaf_slabs | `#iaftfc:item_size/iaf_slabs` | small | very_light |
| iaf_small_light | `#iaftfc:item_size/iaf_small_light` | small | light |
| iaf_small_tools | `#iaftfc:item_size/iaf_small_tools` | large | medium |
| iaf_stairs | `#iaftfc:item_size/iaf_stairs` | small | light |
| iaf_tools | `#iaftfc:item_size/iaf_tools` | very_large | very_heavy |
| iaftfc_dragonbone_parts | ドラゴン骨部品5品 | large | medium |
| iaftfc_dragonsteel_parts | 金属部品27品（5刃＋4未完成防具×3属性） | large | medium |
| iaftfc_metal_blocks | 金属ブロック3品 | small | light |
| iaftfc_metal_slabs | 金属ハーフ3品 | small | very_light |
| iaftfc_metal_stairs | 金属階段3品 | small | light |

各タグの対象品も以下に完結して示す。`[存在時]` は採用するIaF CE版に当該公開IDがある場合だけ対象とし、存在しないアイテムを新規追加する意味ではない。以下のリストはすべて同じ `iceandfire:` 名前空間であり、`id` の短縮表記は `iceandfire:<id>` に展開する。

**`#iaftfc:item_size/iaf_armor`（118品）**：

`armor_amethyst_boots`, `armor_amethyst_chestplate`, `armor_amethyst_helmet`, `armor_amethyst_leggings`, `armor_black_boots`, `armor_black_chestplate`, `armor_black_helmet`, `armor_black_leggings`, `armor_blue_boots`, `armor_blue_chestplate`, `armor_blue_helmet`, `armor_blue_leggings`

`armor_bronze_boots`, `armor_bronze_chestplate`, `armor_bronze_helmet`, `armor_bronze_leggings`, `armor_copper_boots`, `armor_copper_chestplate`, `armor_copper_helmet`, `armor_copper_leggings`, `armor_copper_metal_boots`, `armor_copper_metal_chestplate`, `armor_copper_metal_helmet`, `armor_copper_metal_leggings`

`armor_electric_boots`, `armor_electric_chestplate`, `armor_electric_helmet`, `armor_electric_leggings`, `armor_gray_boots`, `armor_gray_chestplate`, `armor_gray_helmet`, `armor_gray_leggings`, `armor_green_boots`, `armor_green_chestplate`, `armor_green_helmet`, `armor_green_leggings`

`armor_red_boots`, `armor_red_chestplate`, `armor_red_helmet`, `armor_red_leggings`, `armor_sapphire_boots`, `armor_sapphire_chestplate`, `armor_sapphire_helmet`, `armor_sapphire_leggings`, `armor_silver_boots`, `armor_silver_chestplate`, `armor_silver_helmet`, `armor_silver_leggings`

`armor_silver_metal_boots`, `armor_silver_metal_chestplate`, `armor_silver_metal_helmet`, `armor_silver_metal_leggings`, `armor_white_boots`, `armor_white_chestplate`, `armor_white_helmet`, `armor_white_leggings`, `blindfold`, `deathworm_red_boots`, `deathworm_red_chestplate`, `deathworm_red_helmet`

`deathworm_red_leggings`, `deathworm_white_boots`, `deathworm_white_chestplate`, `deathworm_white_helmet`, `deathworm_white_leggings`, `deathworm_yellow_boots`, `deathworm_yellow_chestplate`, `deathworm_yellow_helmet`, `deathworm_yellow_leggings`, `diamond_hippogryph_armor`, `earplugs`, `forest_troll_leather_boots`

`forest_troll_leather_chestplate`, `forest_troll_leather_helmet`, `forest_troll_leather_leggings`, `frost_troll_leather_boots`, `frost_troll_leather_chestplate`, `frost_troll_leather_helmet`, `frost_troll_leather_leggings`, `gold_hippogryph_armor`, `iron_hippogryph_armor`, `mountain_troll_leather_boots`, `mountain_troll_leather_chestplate`, `mountain_troll_leather_helmet`

`mountain_troll_leather_leggings`, `netherite_hippogryph_armor`, `sheep_boots`, `sheep_chestplate`, `sheep_helmet`, `sheep_leggings`, `tide_blue_boots`, `tide_blue_chestplate`, `tide_blue_helmet`, `tide_blue_leggings`, `tide_bronze_boots`, `tide_bronze_chestplate`

`tide_bronze_helmet`, `tide_bronze_leggings`, `tide_deepblue_boots`, `tide_deepblue_chestplate`, `tide_deepblue_helmet`, `tide_deepblue_leggings`, `tide_green_boots`, `tide_green_chestplate`, `tide_green_helmet`, `tide_green_leggings`, `tide_purple_boots`, `tide_purple_chestplate`

`tide_purple_helmet`, `tide_purple_leggings`, `tide_red_boots`, `tide_red_chestplate`, `tide_red_helmet`, `tide_red_leggings`, `tide_teal_boots`, `tide_teal_chestplate`, `tide_teal_helmet`, `tide_teal_leggings`。

**`#iaftfc:item_size/iaf_blocks`（89品）**：

`ash`, `burnt_torch`, `chared_cobblestone`, `chared_dirt`, `chared_dirt_path`, `chared_grass`, `chared_gravel`, `chared_stone`, `copper_pile`, `crackled_cobblestone`, `crackled_dirt`, `crackled_dirt_path`

`crackled_grass`, `crackled_gravel`, `crackled_stone`, `deepslate_silver_ore`, `dragon_bone_block`, `dragon_bone_wall`, `dragon_ice`, `dragon_ice_spikes`, `dragonforge_fire_brick`, `dragonforge_fire_core`, `dragonforge_fire_core_disabled`, `dragonforge_fire_input`

`dragonforge_ice_brick`, `dragonforge_ice_core`, `dragonforge_ice_core_disabled`, `dragonforge_ice_input`, `dragonforge_lightning_brick`, `dragonforge_lightning_core`, `dragonforge_lightning_core_disabled`, `dragonforge_lightning_input`, `dragonscale_amethyst`, `dragonscale_black`, `dragonscale_blue`, `dragonscale_bronze`

`dragonscale_copper`, `dragonscale_electric`, `dragonscale_gray`, `dragonscale_green`, `dragonscale_red`, `dragonscale_sapphire`, `dragonscale_silver`, `dragonscale_white`, `dragonsteel_fire_block`, `dragonsteel_ice_block`, `dragonsteel_lightning_block`, `dread_spawner`

`dread_stone`, `dread_stone_bricks`, `dread_stone_bricks_chiseled`, `dread_stone_bricks_cracked`, `dread_stone_bricks_mossy`, `dread_stone_face`, `dread_stone_slab_double`, `dread_stone_tile`, `dread_torch`, `dreadwood_planks`, `dreadwood_planks_lock`, `egginice`

`frozen_cobblestone`, `frozen_dirt`, `frozen_dirt_path`, `frozen_grass`, `frozen_gravel`, `frozen_splinters`, `frozen_stone`, `gold_pile`, `graveyard_soil`, `nest`, `pixie_house_birch`, `pixie_house_dark_oak`

`pixie_house_mushroom_brown`, `pixie_house_mushroom_red`, `pixie_house_oak`, `pixie_house_spruce`, `raw_silver_block`, `sapphire_block`, `sapphire_ore`, `sea_serpent_scale_block_blue`, `sea_serpent_scale_block_bronze`, `sea_serpent_scale_block_deepblue`, `sea_serpent_scale_block_green`, `sea_serpent_scale_block_purple`

`sea_serpent_scale_block_red`, `sea_serpent_scale_block_teal`, `silver_block`, `silver_ore`, `silver_pile`。

**`#iaftfc:item_size/iaf_bottles`（3品）**：

`fire_dragon_blood`, `ice_dragon_blood`, `lightning_dragon_blood`。

**`#iaftfc:item_size/iaf_chains`（3品）**：

`chain`, `chain_link`, `chain_sticky`。

**`#iaftfc:item_size/iaf_chests`（1品）**：

`ghost_chest`。

**`#iaftfc:item_size/iaf_cloth`（7品）**：

`amphithere_feather`, `banner_pattern_feather`, `stymphalian_bird_feather`, `stymphalian_feather_bundle`, `troll_leather_forest`, `troll_leather_frost`, `troll_leather_mountain`。

**`#iaftfc:item_size/iaf_default`（46品）**：

`amythest_gem`, `ancient_stone_stairs`, `banner_pattern_amphithere`, `banner_pattern_bird`, `banner_pattern_dread`, `banner_pattern_eye`, `banner_pattern_fae`, `banner_pattern_fire`, `banner_pattern_fire_head`, `banner_pattern_gorgon`, `banner_pattern_hippocampus`, `banner_pattern_hippogryph_head`

`banner_pattern_ice`, `banner_pattern_ice_head`, `banner_pattern_lightning`, `banner_pattern_lightning_head`, `banner_pattern_mermaid`, `banner_pattern_sea_serpent`, `banner_pattern_troll`, `banner_pattern_weezer`, `bestiary`, `cockatrice_eye`, `copper_block`, `copper_nugget`

`copper_ore`, `dragon_debug_stick`, `dragon_horn_fire`, `dragon_horn_ice`, `dragon_horn_lightning`, `dread_key`, `dread_shard`, `dread_stone_slab_upper`, `ectoplasm`, `manuscript`, `pixie_house`, `sapphire_gem`

`silver_nugget`, `siren_tear`, `summoning_crystal_fire`, `summoning_crystal_fire_active`, `summoning_crystal_ice`, `summoning_crystal_ice_active`, `summoning_crystal_lightning`, `summoning_crystal_lightning_active`, `weezer_blue_album`, `wither_shard`。

**`#iaftfc:item_size/iaf_dragon_armor`（36品）**：

`dragonarmor_copper_body`, `dragonarmor_copper_head`, `dragonarmor_copper_neck`, `dragonarmor_copper_tail`, `dragonarmor_diamond_body`, `dragonarmor_diamond_head`, `dragonarmor_diamond_neck`, `dragonarmor_diamond_tail`, `dragonarmor_dragon_steel_fire_body`, `dragonarmor_dragon_steel_fire_head`, `dragonarmor_dragon_steel_fire_neck`, `dragonarmor_dragon_steel_fire_tail`

`dragonarmor_dragon_steel_ice_body`, `dragonarmor_dragon_steel_ice_head`, `dragonarmor_dragon_steel_ice_neck`, `dragonarmor_dragon_steel_ice_tail`, `dragonarmor_dragon_steel_lightning_body`, `dragonarmor_dragon_steel_lightning_head`, `dragonarmor_dragon_steel_lightning_neck`, `dragonarmor_dragon_steel_lightning_tail`, `dragonarmor_gold_body`, `dragonarmor_gold_head`, `dragonarmor_gold_neck`, `dragonarmor_gold_tail`

`dragonarmor_iron_body`, `dragonarmor_iron_head`, `dragonarmor_iron_neck`, `dragonarmor_iron_tail`, `dragonarmor_netherite_body`, `dragonarmor_netherite_head`, `dragonarmor_netherite_neck`, `dragonarmor_netherite_tail`, `dragonarmor_silver_body`, `dragonarmor_silver_head`, `dragonarmor_silver_neck`, `dragonarmor_silver_tail`。

**`#iaftfc:item_size/iaf_dragon_bones`（1品）**：

`dragonbone`。

**`#iaftfc:item_size/iaf_dragon_eggs`（12品）**：

`dragonegg_amethyst`, `dragonegg_black`, `dragonegg_blue`, `dragonegg_bronze`, `dragonegg_copper`, `dragonegg_electric`, `dragonegg_gray`, `dragonegg_green`, `dragonegg_red`, `dragonegg_sapphire`, `dragonegg_silver`, `dragonegg_white`。

**`#iaftfc:item_size/iaf_dragon_skulls`（3品）**：

`dragon_skull_fire`, `dragon_skull_ice`, `dragon_skull_lightning`。

**`#iaftfc:item_size/iaf_dragonsteel_armor`（12品）**：

`dragonsteel_fire_boots`, `dragonsteel_fire_chestplate`, `dragonsteel_fire_helmet`, `dragonsteel_fire_leggings`, `dragonsteel_ice_boots`, `dragonsteel_ice_chestplate`, `dragonsteel_ice_helmet`, `dragonsteel_ice_leggings`, `dragonsteel_lightning_boots`, `dragonsteel_lightning_chestplate`, `dragonsteel_lightning_helmet`, `dragonsteel_lightning_leggings`。

**`#iaftfc:item_size/iaf_empty_jars`（1品）**：

`pixie_jar_empty`。

**`#iaftfc:item_size/iaf_filled_jars`（6品）**：

`pixie_jar_0`, `pixie_jar_1`, `pixie_jar_2`, `pixie_jar_3`, `pixie_jar_4`, `pixie_jar_5`。

**`#iaftfc:item_size/iaf_foods`（11品）**：

`ambrosia`, `cannoli`, `cooked_rice_with_fire_dragon_meat`, `cooked_rice_with_ice_dragon_meat`, `cooked_rice_with_lightning_dragon_meat`, `fire_dragon_flesh`, `ghost_cream`, `ice_dragon_flesh`, `lightning_dragon_flesh`, `pixie_dust`, `pixie_dust_milky_tea`。

**`#iaftfc:item_size/iaf_furniture`（7品）**：

`lectern`, `podium_acacia`, `podium_birch`, `podium_dark_oak`, `podium_jungle`, `podium_oak`, `podium_spruce`。

**`#iaftfc:item_size/iaf_ingots`（6品）**：

`copper_ingot`, `dragonsteel_fire_ingot`, `dragonsteel_ice_ingot`, `dragonsteel_lightning_ingot`, `ghost_ingot`, `silver_ingot`。

**`#iaftfc:item_size/iaf_large_vessels`（11品）**：

`amphithere_skull`, `cockatrice_skull`, `cyclops_eye`, `cyclops_skull`, `gorgon_head`, `hippogryph_skull`, `hydra_skull`, `seaserpent_skull`, `stone_statue`, `stymphalian_skull`, `troll_skull`。

**`#iaftfc:item_size/iaf_logs`（1品）**：

`dreadwood_log`。

**`#iaftfc:item_size/iaf_ores`（1品）**：

`raw_silver`。

**`#iaftfc:item_size/iaf_plants`（5品）**：

`dreadwood_leaves`, `dreadwood_sapling`, `fire_lily`, `frost_lily`, `lightning_lily`。

**`#iaftfc:item_size/iaf_rods`（14品）**：

`amphithere_arrow`, `deathworm_tounge`, `dragon_horn`, `dragon_stick`, `dragonbone_arrow`, `hippocampus_fin`, `hippogryph_talon`, `hydra_arrow`, `hydra_fang`, `sea_serpent_arrow`, `sea_serpent_fang`, `stymphalian_arrow`

`troll_tusk`, `witherbone`。

**`#iaftfc:item_size/iaf_sheets`（20品）**：

`dragonscales_amethyst`, `dragonscales_black`, `dragonscales_blue`, `dragonscales_bronze`, `dragonscales_copper`, `dragonscales_electric`, `dragonscales_gray`, `dragonscales_green`, `dragonscales_red`, `dragonscales_sapphire`, `dragonscales_silver`, `dragonscales_white`

`sea_serpent_scales_blue`, `sea_serpent_scales_bronze`, `sea_serpent_scales_deepblue`, `sea_serpent_scales_green`, `sea_serpent_scales_purple`, `sea_serpent_scales_red`, `sea_serpent_scales_teal`, `shiny_scales`。

**`#iaftfc:item_size/iaf_slabs`（1品）**：

`dread_stone_slab`。

**`#iaftfc:item_size/iaf_small_light`（42品）**：

`creative_dragon_meal`, `deathworm_chitin_red`, `deathworm_chitin_white`, `deathworm_chitin_yellow`, `deathworm_egg`, `deathworm_egg_giant`, `dragon_meal`, `fire_dragon_heart`, `fire_stew`, `frost_stew`, `hippogryph_egg`, `hydra_heart`

`ice_dragon_heart`, `lightning_dragon_heart`, `lightning_stew`, `pixie_wings`, `rotten_egg`, `sickly_dragon_meal`, `spawn_egg_amphithere`, `spawn_egg_cockatrice`, `spawn_egg_cyclops`, `spawn_egg_death_worm`, `spawn_egg_dread_beast`, `spawn_egg_dread_ghoul`

`spawn_egg_dread_horse`, `spawn_egg_dread_knight`, `spawn_egg_dread_scuttler`, `spawn_egg_dread_thrall`, `spawn_egg_fire_dragon`, `spawn_egg_ghost`, `spawn_egg_gorgon`, `spawn_egg_hippocampus`, `spawn_egg_hippogryph`, `spawn_egg_hydra`, `spawn_egg_ice_dragon`, `spawn_egg_lich`

`spawn_egg_lightning_dragon`, `spawn_egg_pixie`, `spawn_egg_sea_serpent`, `spawn_egg_siren`, `spawn_egg_stymphalian_bird`, `spawn_egg_troll`。

**`#iaftfc:item_size/iaf_small_tools`（10品）**：

`cockatrice_scepter`, `dragon_flute`, `dragon_seeker`, `dread_queen_staff`, `epic_dragon_seeker`, `godly_dragon_seeker`, `legendary_dragon_seeker`, `lich_staff`, `pixie_wand`, `siren_flute`。

**`#iaftfc:item_size/iaf_stairs`（1品）**：

`dread_stone_stairs`。

**`#iaftfc:item_size/iaf_tools`（60品）**：

`amphithere_macuahuitl`, `copper_axe`, `copper_hoe`, `copper_pickaxe`, `copper_shovel`, `copper_sword`, `deathworm_gauntlet_red`, `deathworm_gauntlet_white`, `deathworm_gauntlet_yellow`, `dragonbone_axe`, `dragonbone_bow`, `dragonbone_bow_pulling_0`

`dragonbone_bow_pulling_1`, `dragonbone_bow_pulling_2`, `dragonbone_hoe`, `dragonbone_pickaxe`, `dragonbone_shovel`, `dragonbone_sword`, `dragonbone_sword_fire`, `dragonbone_sword_ice`, `dragonbone_sword_lightning`, `dragonsteel_fire_axe`, `dragonsteel_fire_hoe`, `dragonsteel_fire_pickaxe`

`dragonsteel_fire_shovel`, `dragonsteel_fire_sword`, `dragonsteel_ice_axe`, `dragonsteel_ice_hoe`, `dragonsteel_ice_pickaxe`, `dragonsteel_ice_shovel`, `dragonsteel_ice_sword`, `dragonsteel_lightning_axe`, `dragonsteel_lightning_hoe`, `dragonsteel_lightning_pickaxe`, `dragonsteel_lightning_shovel`, `dragonsteel_lightning_sword`

`dread_knight_sword`, `dread_queen_sword`, `dread_sword`, `fishing_spear`, `ghost_sword`, `hippocampus_slapper`, `hippogryph_sword`, `silver_axe`, `silver_hoe`, `silver_pickaxe`, `silver_shovel`, `silver_sword`

`stymphalian_bird_dagger`, `tide_trident`, `tide_trident_inventory`, `tide_trident_throwing`, `troll_weapon`, `troll_weapon_axe`, `troll_weapon_column`, `troll_weapon_column_forest`, `troll_weapon_column_frost`, `troll_weapon_hammer`, `troll_weapon_trunk`, `troll_weapon_trunk_frost`。

##### AC-F013-001: 11食品の栄養と腐敗値を適用する

Preconditions:
表の11食品の新鮮品および腐敗品を用意する。

Action:
食料の栄養表示、摂食、保存・再ログインを比較する。

Expected Result:
表の各値がTFCの標準栄養・鮮度規則で認識され、保存で腐敗が消えない。IaF CEの独自食品効果も不要に消去しない。

Verification:
- gametest
- persistence

##### AC-F013-002: 34群の収納条件が有効になる

Preconditions:
34群の対象から存在する各1品以上を取得し、大小のTFC容器を用意する。

Action:
TFCのサイズ・重量表示と収納可否を調べる。

Expected Result:
分類は表の値と一致する。卵や大型ドラゴン防具・頭蓋骨を小型素材と同じ収納制約にはしない。

Verification:
- gametest
- e2e

##### AC-F013-003: 存在しない既存IDを誤生成しない

Preconditions:
採用したIaF CEの公開ID一覧とタグ対象一覧を用意する。

Action:
存在する全対象と存在しない対象を区別してサイズを照合する。

Expected Result:
存在する対象にサイズ設定が適用され、対象外のIDを新規追加して空のアイテムを生成したり、タグ全体がロード失敗したりしない。

Verification:
- unit
- gametest

### F-014: TFCの物理攻撃と耐性

#### Description

IaF CEの武器、防具、ドラゴン等は、TFCが区別する刺突・斬撃・打撃の物理攻撃に対応する。数値はTFCが解釈する抵抗値であり、そのままの百分率軽減を表すものではない。TFCの物理攻撃に分類されない効果へ、本modが同じ軽減率を独自に適用しない。

#### Acceptance Criteria

以下の耐性群は計23種類である。長い対象欄で繰り返す色・材質・部位は記載した公開IDを基準とする。対応版に存在しないIDは対象外とし、未登録品の生成を要求しない。

| 耐性群 | 適用する公開アイテムID | 刺突 | 斬撃 | 打撃 |
| --- | --- | --- | --- | --- |
| iaf_copper_metal | `iceandfire:armor_copper_metal_helmet`, `iceandfire:armor_copper_metal_chestplate`, `iceandfire:armor_copper_metal_leggings`, `iceandfire:armor_copper_metal_boots` | 10.0 | 6.25 | 10.0 |
| iaf_dragon_armor_copper | `iceandfire:dragonarmor_copper_head`, `iceandfire:dragonarmor_copper_neck`, `iceandfire:dragonarmor_copper_body`, `iceandfire:dragonarmor_copper_tail` | 10.0 | 6.25 | 10.0 |
| iaf_dragon_armor_diamond | `iceandfire:dragonarmor_diamond_head`, `iceandfire:dragonarmor_diamond_neck`, `iceandfire:dragonarmor_diamond_body`, `iceandfire:dragonarmor_diamond_tail` | 30.0 | 16.5 | 25.0 |
| iaf_dragon_armor_dragonsteel_fire | `iceandfire:dragonarmor_dragon_steel_fire_head`, `iceandfire:dragonarmor_dragon_steel_fire_neck`, `iceandfire:dragonarmor_dragon_steel_fire_body`, `iceandfire:dragonarmor_dragon_steel_fire_tail` | 70.0 | 60.0 | 70.0 |
| iaf_dragon_armor_dragonsteel_ice | `iceandfire:dragonarmor_dragon_steel_ice_head`, `iceandfire:dragonarmor_dragon_steel_ice_neck`, `iceandfire:dragonarmor_dragon_steel_ice_body`, `iceandfire:dragonarmor_dragon_steel_ice_tail` | 70.0 | 60.0 | 70.0 |
| iaf_dragon_armor_dragonsteel_lightning | `iceandfire:dragonarmor_dragon_steel_lightning_head`, `iceandfire:dragonarmor_dragon_steel_lightning_neck`, `iceandfire:dragonarmor_dragon_steel_lightning_body`, `iceandfire:dragonarmor_dragon_steel_lightning_tail` | 70.0 | 60.0 | 70.0 |
| iaf_dragon_armor_gold | `iceandfire:dragonarmor_gold_head`, `iceandfire:dragonarmor_gold_neck`, `iceandfire:dragonarmor_gold_body`, `iceandfire:dragonarmor_gold_tail` | 12.5 | 8.25 | 12.5 |
| iaf_dragon_armor_iron | `iceandfire:dragonarmor_iron_head`, `iceandfire:dragonarmor_iron_neck`, `iceandfire:dragonarmor_iron_body`, `iceandfire:dragonarmor_iron_tail` | 20.0 | 13.2 | 20.0 |
| iaf_dragon_armor_netherite | `iceandfire:dragonarmor_netherite_head`, `iceandfire:dragonarmor_netherite_neck`, `iceandfire:dragonarmor_netherite_body`, `iceandfire:dragonarmor_netherite_tail` | 45.0 | 33.0 | 50.0 |
| iaf_dragon_armor_silver | `iceandfire:dragonarmor_silver_head`, `iceandfire:dragonarmor_silver_neck`, `iceandfire:dragonarmor_silver_body`, `iceandfire:dragonarmor_silver_tail` | 12.5 | 8.25 | 12.5 |
| iaf_dragon_scale | `iceandfire:armor_amethyst_helmet`, `iceandfire:armor_amethyst_chestplate`, `iceandfire:armor_amethyst_leggings`, `iceandfire:armor_amethyst_boots`, `iceandfire:armor_black_helmet`, `iceandfire:armor_black_chestplate`, `iceandfire:armor_black_leggings`, `iceandfire:armor_black_boots`, `iceandfire:armor_blue_helmet`, `iceandfire:armor_blue_chestplate`, `iceandfire:armor_blue_leggings`, `iceandfire:armor_blue_boots`, `iceandfire:armor_bronze_helmet`, `iceandfire:armor_bronze_chestplate`, `iceandfire:armor_bronze_leggings`, `iceandfire:armor_bronze_boots`, `iceandfire:armor_copper_helmet`, `iceandfire:armor_copper_chestplate`, `iceandfire:armor_copper_leggings`, `iceandfire:armor_copper_boots`, `iceandfire:armor_electric_helmet`, `iceandfire:armor_electric_chestplate`, `iceandfire:armor_electric_leggings`, `iceandfire:armor_electric_boots`, `iceandfire:armor_gray_helmet`, `iceandfire:armor_gray_chestplate`, `iceandfire:armor_gray_leggings`, `iceandfire:armor_gray_boots`, `iceandfire:armor_green_helmet`, `iceandfire:armor_green_chestplate`, `iceandfire:armor_green_leggings`, `iceandfire:armor_green_boots`, `iceandfire:armor_red_helmet`, `iceandfire:armor_red_chestplate`, `iceandfire:armor_red_leggings`, `iceandfire:armor_red_boots`, `iceandfire:armor_sapphire_helmet`, `iceandfire:armor_sapphire_chestplate`, `iceandfire:armor_sapphire_leggings`, `iceandfire:armor_sapphire_boots`, `iceandfire:armor_silver_helmet`, `iceandfire:armor_silver_chestplate`, `iceandfire:armor_silver_leggings`, `iceandfire:armor_silver_boots`, `iceandfire:armor_white_helmet`, `iceandfire:armor_white_chestplate`, `iceandfire:armor_white_leggings`, `iceandfire:armor_white_boots`, `iceandfire:tide_blue_helmet`, `iceandfire:tide_blue_chestplate`, `iceandfire:tide_blue_leggings`, `iceandfire:tide_blue_boots`, `iceandfire:tide_bronze_helmet`, `iceandfire:tide_bronze_chestplate`, `iceandfire:tide_bronze_leggings`, `iceandfire:tide_bronze_boots`, `iceandfire:tide_deepblue_helmet`, `iceandfire:tide_deepblue_chestplate`, `iceandfire:tide_deepblue_leggings`, `iceandfire:tide_deepblue_boots`, `iceandfire:tide_green_helmet`, `iceandfire:tide_green_chestplate`, `iceandfire:tide_green_leggings`, `iceandfire:tide_green_boots`, `iceandfire:tide_purple_helmet`, `iceandfire:tide_purple_chestplate`, `iceandfire:tide_purple_leggings`, `iceandfire:tide_purple_boots`, `iceandfire:tide_red_helmet`, `iceandfire:tide_red_chestplate`, `iceandfire:tide_red_leggings`, `iceandfire:tide_red_boots`, `iceandfire:tide_teal_helmet`, `iceandfire:tide_teal_chestplate`, `iceandfire:tide_teal_leggings`, `iceandfire:tide_teal_boots` | 45.0 | 33.0 | 50.0 |
| iaf_dragonsteel | `iceandfire:dragonsteel_fire_helmet`, `iceandfire:dragonsteel_fire_chestplate`, `iceandfire:dragonsteel_fire_leggings`, `iceandfire:dragonsteel_fire_boots`, `iceandfire:dragonsteel_ice_helmet`, `iceandfire:dragonsteel_ice_chestplate`, `iceandfire:dragonsteel_ice_leggings`, `iceandfire:dragonsteel_ice_boots`, `iceandfire:dragonsteel_lightning_helmet`, `iceandfire:dragonsteel_lightning_chestplate`, `iceandfire:dragonsteel_lightning_leggings`, `iceandfire:dragonsteel_lightning_boots` | 70.0 | 60.0 | 70.0 |
| iaf_hippogryph_diamond | `iceandfire:diamond_hippogryph_armor` | 30.0 | 16.5 | 25.0 |
| iaf_hippogryph_gold | `iceandfire:gold_hippogryph_armor` | 12.5 | 8.25 | 12.5 |
| iaf_hippogryph_iron | `iceandfire:iron_hippogryph_armor` | 20.0 | 13.2 | 20.0 |
| iaf_hippogryph_netherite | `iceandfire:netherite_hippogryph_armor` | 45.0 | 33.0 | 50.0 |
| iaf_iaftfc_dragon_armor_black_steel | `iaftfc:dragonarmor_black_steel_head`, `iaftfc:dragonarmor_black_steel_neck`, `iaftfc:dragonarmor_black_steel_body`, `iaftfc:dragonarmor_black_steel_tail` | 45.0 | 33.0 | 50.0 |
| iaf_iaftfc_dragon_armor_blue_steel | `iaftfc:dragonarmor_blue_steel_head`, `iaftfc:dragonarmor_blue_steel_neck`, `iaftfc:dragonarmor_blue_steel_body`, `iaftfc:dragonarmor_blue_steel_tail` | 50.0 | 50.0 | 62.5 |
| iaf_iaftfc_dragon_armor_red_steel | `iaftfc:dragonarmor_red_steel_head`, `iaftfc:dragonarmor_red_steel_neck`, `iaftfc:dragonarmor_red_steel_body`, `iaftfc:dragonarmor_red_steel_tail` | 62.5 | 50.0 | 50.0 |
| iaf_iaftfc_dragon_armor_steel | `iaftfc:dragonarmor_steel_head`, `iaftfc:dragonarmor_steel_neck`, `iaftfc:dragonarmor_steel_body`, `iaftfc:dragonarmor_steel_tail` | 30.0 | 16.5 | 25.0 |
| iaf_leather_like | `iceandfire:blindfold`, `iceandfire:earplugs`, `iceandfire:sheep_helmet`, `iceandfire:sheep_chestplate`, `iceandfire:sheep_leggings`, `iceandfire:sheep_boots` | 0.0 | 3.0 | 0.0 |
| iaf_robust_hide | `iceandfire:deathworm_red_helmet`, `iceandfire:deathworm_red_chestplate`, `iceandfire:deathworm_red_leggings`, `iceandfire:deathworm_red_boots`, `iceandfire:deathworm_white_helmet`, `iceandfire:deathworm_white_chestplate`, `iceandfire:deathworm_white_leggings`, `iceandfire:deathworm_white_boots`, `iceandfire:deathworm_yellow_helmet`, `iceandfire:deathworm_yellow_chestplate`, `iceandfire:deathworm_yellow_leggings`, `iceandfire:deathworm_yellow_boots`, `iceandfire:forest_troll_leather_helmet`, `iceandfire:forest_troll_leather_chestplate`, `iceandfire:forest_troll_leather_leggings`, `iceandfire:forest_troll_leather_boots`, `iceandfire:frost_troll_leather_helmet`, `iceandfire:frost_troll_leather_chestplate`, `iceandfire:frost_troll_leather_leggings`, `iceandfire:frost_troll_leather_boots`, `iceandfire:mountain_troll_leather_helmet`, `iceandfire:mountain_troll_leather_chestplate`, `iceandfire:mountain_troll_leather_leggings`, `iceandfire:mountain_troll_leather_boots` | 20.0 | 13.2 | 20.0 |
| iaf_silver_metal | `iceandfire:armor_silver_metal_helmet`, `iceandfire:armor_silver_metal_chestplate`, `iceandfire:armor_silver_metal_leggings`, `iceandfire:armor_silver_metal_boots` | 12.5 | 8.25 | 12.5 |

3属性のドラゴンエンティティ `iceandfire:fire_dragon`、`iceandfire:ice_dragon`、`iceandfire:lightning_dragon` の物理耐性は、刺突・斬撃・打撃がそれぞれ30.0である。これらをTFCの斬撃を行うエンティティの分類に含める。全てのIaF CE生物やドラゴンのブレスを無条件に斬撃へ分類しない。

武器の攻撃分類は次の各集合に属する公開IDへ適用する。IDはすべて `iceandfire:` 名前空間であり、以下では接頭辞を省略する。採用版で存在するものだけを対象とする。

**刺突 `#tfc:deals_piercing_damage`**：`copper_pickaxe`, `dragonbone_bow`, `dragonbone_bow_pulling_0`, `dragonbone_bow_pulling_1`, `dragonbone_bow_pulling_2`, `dragonbone_pickaxe`, `dragonsteel_fire_pickaxe`, `dragonsteel_ice_pickaxe`, `dragonsteel_lightning_pickaxe`, `fishing_spear`, `silver_pickaxe`, `stymphalian_bird_dagger`, `tide_trident`, `tide_trident_inventory`, `tide_trident_throwing`。

**斬撃 `#tfc:deals_slashing_damage`**：`amphithere_macuahuitl`, `copper_axe`, `copper_hoe`, `copper_sword`, `dragonbone_axe`, `dragonbone_hoe`, `dragonbone_sword`, `dragonbone_sword_fire`, `dragonbone_sword_ice`, `dragonbone_sword_lightning`, `dragonsteel_fire_axe`, `dragonsteel_fire_hoe`, `dragonsteel_fire_sword`, `dragonsteel_ice_axe`, `dragonsteel_ice_hoe`, `dragonsteel_ice_sword`, `dragonsteel_lightning_axe`, `dragonsteel_lightning_hoe`, `dragonsteel_lightning_sword`, `dread_knight_sword`, `dread_queen_sword`, `dread_sword`, `ghost_sword`, `hippogryph_sword`, `silver_axe`, `silver_hoe`, `silver_sword`, `troll_weapon_axe`。

**打撃 `#tfc:deals_crushing_damage`**：`copper_shovel`, `deathworm_gauntlet_red`, `deathworm_gauntlet_white`, `deathworm_gauntlet_yellow`, `dragonbone_shovel`, `dragonsteel_fire_shovel`, `dragonsteel_ice_shovel`, `dragonsteel_lightning_shovel`, `hippocampus_slapper`, `silver_shovel`, `troll_weapon`, `troll_weapon_column`, `troll_weapon_column_forest`, `troll_weapon_column_frost`, `troll_weapon_hammer`, `troll_weapon_trunk`, `troll_weapon_trunk_frost`。

##### AC-F014-001: 防具の物理耐性を適用する

Preconditions:
表の23耐性群の該当防具を取得し、各物理属性の攻撃を準備する。

Action:
対象となる防具を着用・装着し、攻撃結果と基準データを照合する。

Expected Result:
刺突・斬撃・打撃それぞれの表の抵抗値がTFC規則により適用され、属性を入れ替えたり独自の一律%軽減にしたりしない。

Verification:
- unit
- gametest

##### AC-F014-002: ドラゴン本体と武器に属性を設定する

Preconditions:
3属性のドラゴン、各武器集合の有効なアイテムを用意する。

Action:
攻撃分類とTFC物理攻撃の適用結果を調べる。

Expected Result:
ドラゴン3種は30/30/30の耐性を持ち、本書の集合の武器が指定された物理属性として扱われる。他の生物に30を一律適用しない。

Verification:
- gametest

##### AC-F014-003: サーバーの耐性計算を共有する

Preconditions:
A/Bが同一ドラゴンと耐性のある対象を観察する専用サーバー。

Action:
同じ条件で物理攻撃を行い、両クライアントの体力表示とサーバーの結果を比較する。

Expected Result:
サーバー側の耐性計算が正となり、両クライアントが同じ結果を表示する。

Verification:
- multiplayer

### F-015: 旧来の直接製作経路の制限

#### Description

TFCの金属加工を迂回するIaF CEの旧材料と旧製作経路を既定のサバイバル進行から外す。対象の旧アイテムを登録から消去せず、存在する場合はツールチップ末尾に暗赤色の `[DISABLED]` を表示する。この表示は所持禁止・自動消滅・強制削除を意味しない。対応するTFCの銀・宝石や銀色のドラゴン鱗防具を名称の類似だけで無効化しない。

#### Acceptance Criteria

無効表示の対象は42個である。以下のIDにはすべて `iceandfire:` を付ける。

`silver_ingot`, `silver_nugget`, `silver_block`, `raw_silver`, `raw_silver_block`, `silver_ore`, `deepslate_silver_ore`, `silver_axe`, `silver_hoe`, `silver_pickaxe`, `silver_shovel`, `silver_sword`, `armor_silver_metal_helmet`, `armor_silver_metal_chestplate`, `armor_silver_metal_leggings`, `armor_silver_metal_boots`, `sapphire_gem`, `sapphire_block`, `sapphire_ore`, `dragonsteel_fire_ingot`, `dragonsteel_ice_ingot`, `dragonsteel_lightning_ingot`。

さらに `dragonarmor_<材質>_<部位>` の材質 `copper / diamond / gold / netherite / silver` と、部位 `head / neck / body / tail` の全組合せ20個を対象とする。ドラゴン鱗由来の `iceandfire:armor_silver_*` は対象外とする。

旧製作・精錬の無効対象は計48レシピIDである。以下の全公開IDを、既定のゲームにおける成立しないレシピとする。管理者が明示的に上位パックを適用して製作規則を変更する権限は妨げない。

`iceandfire:armor_silver_metal_boots`, `iceandfire:armor_silver_metal_chestplate`, `iceandfire:armor_silver_metal_helmet`, `iceandfire:armor_silver_metal_leggings`, `iceandfire:dragonarmor_copper_body`, `iceandfire:dragonarmor_copper_head`, `iceandfire:dragonarmor_copper_neck`, `iceandfire:dragonarmor_copper_tail`, `iceandfire:dragonarmor_diamond_body`, `iceandfire:dragonarmor_diamond_head`, `iceandfire:dragonarmor_diamond_neck`, `iceandfire:dragonarmor_diamond_tail`, `iceandfire:dragonarmor_gold_body`, `iceandfire:dragonarmor_gold_head`, `iceandfire:dragonarmor_gold_neck`, `iceandfire:dragonarmor_gold_tail`, `iceandfire:dragonarmor_netherite_body`, `iceandfire:dragonarmor_netherite_head`, `iceandfire:dragonarmor_netherite_neck`, `iceandfire:dragonarmor_netherite_tail`, `iceandfire:dragonarmor_silver_body`, `iceandfire:dragonarmor_silver_head`, `iceandfire:dragonarmor_silver_neck`, `iceandfire:dragonarmor_silver_tail`, `iceandfire:furnace/deepslate_silver_ingot`, `iceandfire:furnace/deepslate_silver_ingot_blasting`, `iceandfire:furnace/sapphire`, `iceandfire:furnace/sapphire_blasting`, `iceandfire:furnace/silver_ingot`, `iceandfire:furnace/silver_ingot_blasting`, `iceandfire:furnace/silver_nugget`, `iceandfire:furnace/silver_nugget_blasting`, `iceandfire:raw_silver_block_to_raw_silver`, `iceandfire:raw_silver_to_raw_silver_block`, `iceandfire:sapphire_block_to_sapphire_gem`, `iceandfire:sapphire_gem_to_sapphire_block`, `iceandfire:silver_axe`, `iceandfire:silver_block_to_silver_ingot`, `iceandfire:silver_hoe`, `iceandfire:silver_ingot_from_blasting_raw_silver`, `iceandfire:silver_ingot_from_smelting_raw_silver`, `iceandfire:silver_ingot_to_silver_block`, `iceandfire:silver_ingot_to_silver_nugget`, `iceandfire:silver_nugget_to_silver_ingot`, `iceandfire:silver_pickaxe`, `iceandfire:silver_pile`, `iceandfire:silver_shovel`, `iceandfire:silver_sword`。

既定の正常な製作経路はF-008のドラゴン炉、F-009の鍛造・溶接、F-010の骨加工、F-011の鱗加工、F-012のドラゴン用防具に従う。対応する完成品を、無効にした旧直接クラフト・精錬レシピと別名の同等レシピによって再び解放しない。他方、本書に無効と定めていないIaF CEの一般的な製作・消費・装備効果は維持する。

##### AC-F015-001: 48レシピの無効化範囲を守る

Preconditions:
既定パックを導入し、列挙した48IDと対応旧材料を用意する。

Action:
各レシピと同等の直接製作を試し、製作一覧を確認する。

Expected Result:
48IDは有効なレシピとして利用できず、旧進行が別IDで復活しない。対象以外の一般レシピを一括して削除しない。

Verification:
- gametest

##### AC-F015-002: 42旧アイテムだけに警告を付ける

Preconditions:
42対象、TFCの銀・宝石、銀色鱗防具、ドラゴン炉の未稼働炉心を取得する。

Action:
ツールチップを表示して比較する。

Expected Result:
対象42アイテムに暗赤色 `[DISABLED]` が付き、比較対象へ誤って付かない。名前・説明文は保持される。

Verification:
- e2e
- visual

##### AC-F015-003: 既存所持品を保持する

Preconditions:
旧アイテムを所持品とチェストへ入れて保存する。

Action:
再起動・再ログイン・スロット移動を行う。

Expected Result:
対象アイテムは登録されたまま残り、名称・数量が勝手に変換・削除されない。

Verification:
- persistence
- e2e

### F-016: 名称・グラフィック・ゲーム内表示

#### Description

追加要素には現行アセットのアイコン、モデル、テクスチャと英語名称を使用する。新規作成したアイテムだけでなく、既存のドラゴンへ装着した鋼系防具、各属性の金床・板材・鉱石・溶融金属、骨と鱗の加工画面で、色と形状が製品種別・属性・部位に対応する。欠落テクスチャ、未翻訳キーの表示、見た目だけ異なる加工結果は認めない。

#### Acceptance Criteria

クリエイティブタブの公開IDは `iaftfc:dragonsteel`、英語表示は `IaFTFC Dragonsteel`、アイコンは炎ドラゴンスティールのインゴットとする。追加アイテム75種類はすべて取得・識別できるものとし、他のタブの要素を削除しない。

| 金属 m | 英語名称の前半 | 固体の代表的な色調 | 液体色 |
| --- | --- | --- | --- |
| dragonsteel_fire | Fire Dragonsteel | 橙 #FC9F4E、淡黄白 #FFFED8、赤紫 #A83F56 | #6E211B |
| dragonsteel_ice | Ice Dragonsteel | 青緑 #13C5CF、淡青白 #C6DAED、濃紺 #2B3564 | #6BADC9 |
| dragonsteel_lightning | Lightning Dragonsteel | 桃紫 #EBB9DB、灰紫 #A8A5B2、濃灰紫 #423846 | #4D3A69 |

金属素材の英語表示は金属名称の後ろに、形態順で `Ingot / Double Ingot / Sheet / Double Sheet / Rod / Axe Head / Hoe Head / Pickaxe Head / Shovel Head / Sword Blade / Unfinished Helmet / Unfinished Chestplate / Unfinished Greaves / Unfinished Boots` を付ける。建材と金床はそれぞれ `Plated Block / Plated Slab / Plated Stairs / Anvil`、溶融金属は先頭に `Molten` を付ける。骨部品は `Dragon Bone <Axe Head|Hoe Head|Pickaxe Head|Shovel Head|Sword Blade>` とする。

| 防具材質 | 英語の基本名称 | 代表色 |
| --- | --- | --- |
| steel | Steel Dragon Armor | 青灰 #44575B／#829296 |
| black_steel | Black Steel Dragon Armor | 黒灰 #333333／#878787 |
| red_steel | Red Steel Dragon Armor | 暗赤 #902127／#EE989C |
| blue_steel | Blue Steel Dragon Armor | 濃青 #586A9E／#A0AEC8 |

ドラゴン用防具の装着箇所は、頭 `Head`、首 `Neck`、胴 `Body`、尾 `Tail` で識別する。アイテムの形状と実際のドラゴンへの描画は対応し、異なる部位の装備に見えないこと。金属ブロックには板状の縁取りと斑模様、ハーフ・階段には設置形状を表示し、金床は作業面・胴・台座のある形で表示する。骨部品は灰白・象牙色の骨質、鱗加工画面は選んだ色の鱗の模様とする。

鱗色の表示基準は、`red`=赤、`green`=緑、`bronze`=黄褐、`gray`=灰、`blue`=明青、`white`=白、`sapphire`=濃青、`silver`=銀灰、`electric`=青紫、`amethyst`=紫、`copper`=赤褐、`black`=黒灰とする。骨加工は灰白の骨模様、鱗加工はそれぞれの鱗が重なった模様とする。作業のON/OFF、材料数、完成結果が画面の表示と一致すること。

既定の英語名称を保ち、ほかの言語へ切り替えた場合は存在する翻訳または英語へのフォールバックとする。今回、新規日本語翻訳の製作は必須要件としない。実物の温度表示は現在のTFC温度に従い、完成見本の1800℃と同一視しない。金床はTier 7として識別できる。

##### AC-F016-001: 追加アイテムをクリエイティブタブから探せる

Preconditions:
正常な必須mod環境でクリエイティブモードを使用する。

Action:
専用タブから全75品を取得し、英語名称とアイコンを確認する。

Expected Result:
75品に重複・欠落がなく、名称・形状・属性が本文の規則に合う。翻訳キーや欠落模様が露出しない。

Verification:
- e2e
- visual

##### AC-F016-002: 設置物と装着防具の見た目が一致する

Preconditions:
3属性の金属建材・金床と4材質の追加ドラゴン防具を用意する。

Action:
設置・向き変更・ドラゴンへの装着・取り外しを行う。

Expected Result:
固体の代表色、block/slab/stairs/anvilの形、頭/首/胴/尾への装着位置が一致し、欠落や逆転がない。

Verification:
- e2e
- visual

##### AC-F016-003: 加工画面と熱の表示が製品状態に一致する

Preconditions:
骨・12色の鱗・1800℃完成直後の金属と冷却後の金属を用意する。

Action:
削り画面、金床、炉、所持品の表示を比較する。

Expected Result:
素材の図柄・加工段階・必要数が一致する。完成直後は1800℃、取り出して冷却した実物は現在温度を表示し、表示だけが固定温度へ戻らない。

Verification:
- e2e
- visual

## Global Requirements

### Persistence

同一の対応mod構成において、追加アイテムの公開ID・数量、置いたブロックの種類と状態、TFCの金属温度・加工品質、溶融金属の量、食料の鮮度、ドラゴンの装着防具、孵化の状態、炉の在庫と保存対象の進捗、宝箱の開封状態を保存・停止・再起動後も復元する。熱・鮮度・孵化の経過がある場合には、それぞれの必須modの通常の時間計算に従って変化する。再起動を理由に素材を二重消費したり製品を複製したりしない。

### Multiplayer

製作の可否、資材の消費・返却、炉の進捗・温度、金床の品質、餌の可否、孵化、戦利品の生成、装備の性能、攻撃と耐性はサーバーを正とする。専用サーバーに接続した2クライアントは観測できる状態を共有し、双方が同じ品を同時に取り出しても1回分しか得られない。再接続・画面終了・チャンク再読み込みで不正な変更がない。

### Visual

採用した現行アセットの形、色、模様、名前をF-016の規則に沿って表示する。レシピ結果と手持ち品の熱・品質・種類、加工操作の進行、ドラゴン用防具の装着箇所を表示上で区別する。表示の合否は対象状態と照合し、描画手段や内部ファイル配置は指定しない。

### Performance

本modの追加した機能により、通常の新規チャンク生成、ドラゴンの活動、炉の稼働、金床・削り画面、複数人の通常操作が完了不能になったり、無限処理やクラッシュに陥ったりしない。性能について実測値のない任意のTPSや処理時間の保証値を捏造しない。比較する場合は同じseed、同じ必須mod、同じ設定、同じ計測機器で本mod追加前後を測定する。測定条件と観測値を残さず改善済みと報告しない。

### Compatibility

対象はMinecraft 1.21.1、対応するNeoForge 21.1系列、Java 21、およびF-001の必須依存構成のみとする。TFCの新規ワールドを標準の対象とする。レシピ・タグ・戦利品・食料・熱・耐性・サイズ・構造物配置など、本書で指定した公開IDと結果は、サーバーが使用するゲームデータの既定値として提供する。管理者の正当なデータパック上書きは尊重する。既生成チャンクの自動再生成、異なるゲーム系列への移植、未検証の旧セーブ全体の完全移行を要求しない。

### Verification Environment

ゲームを起動して行うすべての検証には、本modとF-001で定めた真の必須依存modだけを導入する。シングルプレイ・専用サーバー・クライアントA/B・表示検証・保存検証も同条件とし、追加modを導入して材料・レシピ・表示・診断の欠落を補わない。導入されているmodをフォルダー内のファイル名だけで推測せず、実際のロード済み一覧で照合する。必須依存の欠落試験を行う場合は、その対象だけを除外する。ゲーム外のビルド・検証用ツールはゲームに導入するmodではない。

### Acceptance and Data Integrity

各ACの開始条件・操作・期待結果を満たし、指定した検証種別で証拠が得られた場合だけ合格とする。`unit` は値と論理条件、`gametest` はゲーム内の状態、`e2e` は実クライアント上の操作、`visual` は表示、`persistence` は保存・再起動、`multiplayer` は専用サーバーの複数クライアントで検証する。単なるビルド成功、静的解析、アセット一覧の存在だけでは、該当する実ゲームの動作確認を代替しない。属性・色・部位の組合せは本書の展開数を網羅し、1つの代表例のみで全体の合格を宣言しない。

## Constraints

本書は実装アーキテクチャの指定書ではない。旧コードの構成、内部のイベント、登録方法、Mixin、反射、キャッシュ、データ生成方法、内部テストの設計を実装条件としない。公開ID、プレイヤーから見た入出力、温度、数量、生成条件、表示、保存・同期結果だけを契約とする。原料の必要数と消費数が異なる骨加工、製品温度1800℃、Tier 6からの到達経路、防具と道具の溶融量の差、戦利品の重みを、実装都合だけで変更しない。

既存のIaF CEとTFCが担うドラゴンのAI・成長・騎乗・特殊効果、TFCの温度・食料・地質・金床の一般ルールは、それぞれの必須modの仕様に従う。本modに明示していない新しいドラゴン種、ディメンション、装置、クエスト、装備性能、日本語翻訳、装飾建材の新しいサバイバル製作経路を追加しない。

本書で仕様として記載する他modは、真に必要な依存modだけとする。必須ではないmodの名前、版、API、連携方法、追加検証条件を記載しない。ゲーム内に追加modを導入しないことは全ACの共通制約である。

アセットは現行版に存在する画像・モデル・翻訳・加工画面のデザインを再利用し、新しい絵柄へ変更しない。仕様の判定に必要な名称、色、形状、公開ID、加工図は本書内に定義し、本書だけで製品仕様を判断する。アセットおよび依存modの使用・配布は各権利者の許諾範囲に従い、利用許諾が確認できない資産を独自の判断で再ライセンスしない。

依存バージョンは2026年10月28日時点の候補である。導入する実配布物の互換性、必須宣言、版は導入前に確認し、より新しい対応安定版が公開されていれば本書のPlatform・F-001を更新する。バージョンが上がるだけで、ここに定義した製品挙動を黙って変えない。採用版の公開IDや仕様が成立しない場合、対象ACを合格とせず先に本書を改訂する。

Statusがdraftである間は仕様の採用・検証を完了したものと扱わない。

## Open Questions

**必須依存の実互換性。** F-001に示した現行安定版候補を組み合わせ、クライアントと専用サーバーで起動・公開ID・主要な製品挙動まで実証できるか。配布情報から版の存在と必須依存関係を確認した段階であり、この組合せの実起動・統合試験は完了していない。互換性に問題があれば、不要なmodを追加するのではなく、対応する安定版の選択と該当する本書の条件を再検討する。

**既存アセットの利用許諾。** 再利用・改変・配布を行うアセットとコードについて、必要な許諾条件が確定しているか。権利確認を終えるまで、利用・再配布が自由であると推定しない。

