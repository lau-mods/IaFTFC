# Project

Status: active

Project ID: iaftfc
Mod ID: iaftfc

## Platform

Minecraft: 1.21.1
NeoForge: 21.1.264
Java: 21

## Purpose

IaFTFC v0.1は、TerraFirmaCraft（TFC）のワールドにIce and Fire: Community Edition（CE）のFire・Ice・Lightning Dragonを統合するNeoForge Addonである。ドラゴンに直接関係する地形生成、戦利品、植物、食料、孵化、鍛冶、装備、およびそれらを成立させるTFCの共通機構のみを対象とする。

原作者が公開した旧IaFTFCの後継版として、Mod ID `iaftfc`、必要な既存レジストリID、完成品の挙動を継承する。一方、開発基盤はMinecraft 1.21.1対応のNeoForge MDKから新規構築する。旧版のGit履歴は仕様と既存IDの調査資料であり、旧コード、JSON、PNG、Gradleファイルをそのまま移植しない。

本ファイルをHarnessに渡す唯一の製品仕様とする。外部の調査報告書、旧仕様書、ソース調査台帳、個別の実装計画等をHarnessへの入力要件としない。必要に応じて公開Gitリポジトリや公式文書を自ら参照してよい。

## Features

### F-001: ドラゴンの巣と地下洞窟の気候生成

#### Description

CEのFire・Ice・Lightning Dragonの既存構造物を、TFCの陸地と実際の気候条件に従って生成する。CEのドラゴン、雌雄、年齢、巣・洞窟の内部処理を再実装しない。CEの `iceandfire:structure_gen/fire`、`ice`、`lightning` にTFC陸上バイオームを接続し、気候の判定にはTFCの `tfc:climate` を使用する。

旧IaFTFCはFire用の `iceandfire:dragon_roost`、`iceandfire:dragon_cave` を上書きし、Ice・Lightningについて `iaftfc` 名前空間で各1セットを追加していた。旧版の構造物配置は巣がspacing=24／separation=10、洞窟がspacing=30／separation=20である。CE 2.2の単体既定値は巣15／6、洞窟18／12である。旧版の各属性で使われていた異なるsaltと、村の構造物セットからの10チャンク除外条件を継承する。

属性の適地はIceが寒冷、Fireが高温・低地下水量、Lightningが温暖・多地下水量とする。旧版の実際の条件はIceが気温5.0℃以下、Fireが15.0℃以上かつ地下水量299.9以下、Lightningが5.1℃以上かつ地下水量300.0以上であった。**これらは調査済みの旧値であり、v0.1の最終閾値ではない。** v0.1では気候条件を再設計するが、3属性のどれにも適合しない地域の存在を許容する。空白地帯へ補完生成・属性再抽選・強制スポーンを行わない。各属性が生成可能な気候における基礎抽選は概ね等確率とするが、ワールド全体の最終生成数を1:1:1へ補正しない。

巣・洞窟それぞれの生成確率はCE 2.2の属性別Configを使用する。6項目の既定値はいずれも0.5である。配置間隔はCEに該当Configがあればそれを優先し、存在しない場合はIaFTFC側の `dragonRoostSpacing`（既定24チャンク）と `dragonCaveSpacing`（既定30チャンク）を使用する。CE 2.2には配置間隔Configがなく、JSONに書かれたspacingとIaFTFCの設定値を接続する仕組みが必要である。CEの `dangerousDistanceLimit`（既定1000ブロック）を尊重する。

CEのドラゴン骨格は巣・洞窟と別のPlaced Featureであり、旧IaFTFCでは骨格に属性別のTFC気候制限を追加していない。したがって、巣・洞窟の空白地帯に骨格が存在する可能性は維持する。ただしTFC陸上バイオームは3属性すべての `structure_gen` タグに属し、3属性の骨格Featureが同じチャンクで重ねて抽選されるため、CEの `iceandfire:spawn_dragon_skeleton_fire`、`_ice`、`_lightning` の各Placed Featureへ `minecraft:rarity_filter` chance=3を追加し、チャンクあたりの合計生成確率をCEの `generateSkeletonChance`（既定1/300）相当に抑える。CEの骨格Config自体は変更しない。

#### Acceptance Criteria

##### AC-F001-001: 気候と地形に従う構造物生成

Preconditions:
TFCの新規ワールド内に、3属性それぞれの適格気候および全属性に不適格な気候を再現した生成候補がある。CE側の生成確率は正値である。

Action:
同一シード・固定設定の構造物候補について生成を評価する。

Expected Result:
適格なTFC陸上気候でのみ対応属性の巣・洞窟が候補となる。全属性が不適格な地点ではいずれの巣・洞窟も生成されず、別属性への振替も起こらない。同じ入力条件で結果が決定的であり、CEの同一構造物を二重に配置しない。

##### AC-F001-002: 生成確率と配置間隔の設定反映

Preconditions:
CEの属性別生成確率と、IaFTFCの2つのspacing設定を変更できる。CE 2.2にはspacingの独自設定が存在しない。

Action:
同一シード・同一気候のワールド生成を、生成確率0と正値、または既定spacingと変更後spacingで比較する。

Expected Result:
CEの対象属性の生成確率を0にするとその属性の構造物は生成されない。IaFTFCのspacingを変更すると実際の候補配置が変化する。村との除外領域、separationの下限、CEのスポーン地点安全距離が失われない。CE側にspacing設定が追加された場合はCE設定が優先される。

##### AC-F001-003: 非ドラゴンの自然生成を無効化

Preconditions:
IaFTFCとCE 2.2を導入し、新規チャンクを生成する。

Action:
構造物配置候補、BiomeのMobスポーン表・生成Feature、およびデータ上書きの適用結果を確認する。

Expected Result:
Cyclops Cave、Gorgon Temple、Graveyard、Hydra Cave、Mausoleum、Pixie Village、Siren Islandの7種は自然生成されない。CEの非ドラゴンMobはBiomeの自然スポーンおよびMob生成Featureから出現しない。プレイヤー死亡イベントによるGhostの自動生成も無効化する。3属性のDragonの巣・洞窟・骨格と属性Lily、バニラおよび他Modの生成経路を維持する。この制限は全ディメンションで同じCEデータに適用し、既存個体・既存構造物・スポーンエッグ・コマンド召喚・既存Spawnerには介入しない。

### F-002: ドラゴン構造物のチェスト・鉱石・戦利品

#### Description

CEが巣・地下洞窟内へ配置するチェストを、Fire=Chestnut、Ice=White Cedar、Lightning=BlackwoodのTFC木製チェストへ置換する。対象はドラゴン構造物piece内のチェストに限定し、元CEのLootTable IDを維持する。

CEの巣3種、雌洞窟3種、雄洞窟3種の計9つのチェストLoot TableをTFC素材へ置換する。巣はordinary、雌洞窟はdangerous、雄洞窟はlategameとする。装備候補の中心はordinaryがCopper／Bronze／Bismuth Bronze／Black Bronze、dangerousがWrought Iron、lategameがSteelである。加工途中品と完成品の重みの比は2:1とし、エンチャントは完成品にのみ付ける。各元テーブルのpool、roll、weight、count provider、condition、CE固有報酬を原則維持し、同じentryを異なる共有テーブルで重複計上しない。

属性固有の資源はFire=Ruby／Gold、Ice=Sapphire／Silver、Lightning=Topaz／Copperとする。Vanilla Nuggetは対応するTFC Small Oreへ、Chainは `tfc:metal/chain/wrought_iron` へ置換する。汎用レア報酬にRed／Blue Steelを追加しない。

洞窟外殻の鉱石を、周囲のTFC岩石に適合する属性固有のTFC鉱石へ置換する。外殻ブロックが鉱石になる確率はCEの `generateOreRatio` を維持し、鉱石枠の50%を属性宝石、50%を属性金属のRich鉱石とする。属性宝石はFire=Ruby、Ice=Sapphire、Lightning=Topaz、属性金属はFire=Rich Native Gold、Ice=Rich Native Silver、Lightning=Rich Native Copperとする。CEのCommon／Uncommon／Rare鉱石枠とその他の金属鉱石は生成しない。近傍岩石を探索し、存在しない岩石・鉱石の組合せを生成しない。

CEのGold／Silver／Copper Pileは装飾として残し、layers=1〜8に対して対応するTFC Small Native Oreをlayersの1/2（端数切り上げ）、すなわち1〜4個ドロップする。

#### Acceptance Criteria

##### AC-F002-001: 属性別のTFCチェストと戦利品

Preconditions:
各属性の巣・雌洞窟・雄洞窟を生成したワールドがある。

Action:
構造物内のチェスト種別と、9つのLoot Tableから得られる報酬定義を調べる。

Expected Result:
チェストの木材は属性別の指定値と一致する。Loot TableはCEの元構造物から正しく参照され、指定profileのTFC素材が出現する。置換対象以外のCE報酬の構造と数量分布を維持し、加工途中品対完成品の重み比が2:1になる。

##### AC-F002-002: 地質に適合する洞窟鉱石

Preconditions:
異なるTFC岩石種の地下に属性別のドラゴン洞窟を生成する。

Action:
洞窟装飾の鉱石・宝石ブロックを調べる。

Expected Result:
鉱石はTFCで登録された岩石種・鉱石の組合せとして生成される。鉱石は属性別宝石（Fire=Ruby、Ice=Sapphire、Lightning=Topaz）または属性別Rich金属鉱石（Fire=Native Gold、Ice=Native Silver、Lightning=Native Copper）のみで、両方が出現する。他属性の資源、その他の金属鉱石、未登録の組合せ、CE旧鉱石を生成しない。

##### AC-F002-003: PileからのTFC鉱石回収

Preconditions:
CEのGold／Silver／Copper Pileがlayers=1〜8の各状態で存在する。

Action:
各Pileを通常採掘する。

Expected Result:
対応するTFC Small Native Oreがlayersの1/2（端数切り上げ）個、すなわちlayers=1で1個、8で4個得られる。CE元Nugget等が重複してドロップしない。

### F-003: ドラゴンブレスとTFC地形の変化

#### Description

CEの移動時接触破壊、引っ掛かり時の掘削、ブレス・チャージによる破壊、属性変換、設定に応じた爆発を維持する。CEのGriefing設定、飼育済み個体の設定、mobGriefing、DragonProof、ブロック破壊禁止タグと既存のブロック耐性に従う。IaFTFC側で設備の一律保護や直接破壊の全面禁止は導入しない。

旧IaFTFCのTFC地形変換を再現する。Grass→grass、Dirt／Coarse Dirt／Rooted Dirt／Mud／Farmland→dirt、Grass Path→dirt_path、Raw／Hardened Rock→stone、Cobblestone／Mossy Cobblestone→cobblestone、Rock Gravel→gravelという分類を用い、FireはCEのchared系列、Iceはfrozen系列、Lightningはcrackled系列へ変換する。TFCから変換したCEブロックのrevert属性はfalseとし、元のTFC岩石種は保持しない。

属性変換後のStoneはTFCの崩落系タグへ接続せず、崩落させない。CobblestoneとGravelを土砂移動系タグ、Dirt Pathを支持系タグに接続する。CEの標準破壊や属性効果が処理されるブロックを重複して変換しない。

#### Acceptance Criteria

##### AC-F003-001: TFC自然地形の属性別変換

Preconditions:
上記6分類のTFC自然ブロックを配置し、CEの既定設定でドラゴンの属性ブレスを受ける。

Action:
Fire・Ice・Lightningそれぞれの地形変換を実行する。

Expected Result:
各ブロックは指定されたCEのchared／frozen／crackled系列に変化し、TFC地形からの変換結果はrevert=falseとなる。IaFTFCによる追加変換がCEの破壊許可判定を上書きしない。

##### AC-F003-002: 変換された地形のTFC物理挙動

Preconditions:
属性変換済みStone、Cobblestone、Gravel、Dirt PathがTFCの土砂移動・崩落を生じる配置にある。

Action:
TFCの崩落・土砂移動判定を起こす。

Expected Result:
Stoneは崩落せず、Cobblestone、Gravel、Dirt Pathがそれぞれ定義したTFC物理タグに従って処理される。通常のGrassやDirtを不要な崩落対象に追加していない。

### F-004: TFC肉によるドラゴン給餌

#### Description

Fire・LightningにTFCの非腐敗肉・加熱肉、IceにこれらとCE本来の魚食対象を給餌できるようにする。CEの食料ポイント計算を利用するUranusはバニラFoodProperties.nutritionの10倍を返すが、TFCがバニラFOODへ設定するnutritionは0である。TFC食品についてのみFoodCapability／IFoodの実際のhunger値を使う変換処理を導入し、CE元の食品判定を変更しない。

腐敗したTFC肉は手渡し、地面に落ちた肉の標的選択、追跡中、接触直前、消費時のすべてで拒否する。TFCのmeatとfishの分類を混同しない。Dragon Mealの肉素材もTFCの適格な肉に接続する。ドラゴンの捕食対象Entity TypeにはTFC家畜・野生生物を新規追加しない。

#### Acceptance Criteria

##### AC-F004-001: 非腐敗肉の通常給餌

Preconditions:
ドラゴンが空腹で、腐敗していないTFCの生肉・加熱肉がある。対象肉のバニラnutritionは0である。

Action:
肉をドラゴンへ直接与える。

Expected Result:
TFCのFoodDataに基づく正の食料ポイントが評価され、CE本来の給餌処理で肉が消費され、空腹度が回復する。腐敗肉は消費されず、Fire・LightningとIceの肉・魚の区別を維持する。

##### AC-F004-002: 地面の肉の探索と腐敗拒否

Preconditions:
非腐敗TFC肉を地面へ落とし、それを探索可能なドラゴンがいる。比較用に腐敗肉、または追跡途中で腐敗する肉を用意する。

Action:
ドラゴンの落下アイテム探索・接近・消費を進める。

Expected Result:
非腐敗肉はCEの食料ポイントに変換されて摂食対象となる。腐敗肉は標的にならず、追跡開始後に腐敗した肉も摂食されない。

### F-005: TFC属性植物と繁殖用Stew

#### Description

CEのFire Lily、Frost Lily、Lightning Lilyに相当する植物を、新しい `iaftfc` 名前空間のTFC PlantBlockとして実装する。TFC 4.2.14のRegistryPlantとPlantBlockの仕組みに従い、季節、年齢、土壌、花の採取、見た目、移動速度の設定を行う。原則としてTFCの花の収穫ルールを継承し、独自の開花時限定収穫や新農耕機構は設けない。

植物のTFC4段階テクスチャはTFCのLilyではなくCEの各Lilyテクスチャから作成する。blooming=CE原画、seeding=花弁を除き萎れた色の花芯、dying／dormant／sprouting=茎のみ、budding=閉じた花芯とする。

各植物の気候分布は対応するドラゴンの巣・洞窟の適格気候に概ね一致させる。CEの元Lilyはrarity_filter chance=32でBiome Modifierから生成されるが、この元Featureは停止し、IaFTFCの新しいTFC PlantのConfigured／Placed Featureに置換する。TFCの気候空白地帯に対応するLilyが存在しなくてもよい。

CEの元Lilyをサバイバルで入手不能とし、CEのfire_stew、frost_stew、lightning_lily_mixtureの3レシピが新Plantを材料として使用するよう変更する。その他の材料のTFCでの入手性を維持する。出力は既存CEのFire Stew、Frost Stew、Lightning Stewであり、プレイヤー用食品ではなくドラゴン繁殖専用品とする。

#### Acceptance Criteria

##### AC-F005-001: 属性植物の気候生成

Preconditions:
TFC新規ワールドに、各属性の適格気候と全属性の不適格気候がある。

Action:
属性植物のPlaced Featureを生成する。

Expected Result:
IaFTFC製Fire／Frost／Lightning Lilyが対応気候とTFC植物の設置可能ブロックに生成される。CE元LilyのWorldgen Featureは追加生成されない。

##### AC-F005-002: TFCの花としての採取

Preconditions:
各新PlantがTFCの植物として成長・表示されている。

Action:
TFCの通常の花に対して適用される採取操作と、適用されない操作を行う。

Expected Result:
採取制限、回収形態、植物アイテムの設置と季節状態がTFCの花の一般規則と一致する。新植物のRegistryPlant情報が4.2.14のAPIと整合する。

##### AC-F005-003: 新植物による繁殖用Stew

Preconditions:
3属性の新植物と、対応Stewの残りの有効な材料を所持している。

Action:
CEの3つの繁殖用Stewレシピを実行する。

Expected Result:
CE元Lilyを要求せず、それぞれCEの既存Stewを1個生成する。ドラゴン繁殖に使用するCE既存の効果と対象属性を維持する。

##### AC-F005-004: 属性植物のモデルと季節表現

Preconditions:
新植物を3属性とも設置し、TFCの季節表示を切り替える。

Action:
ゲーム画面で各植物のモデル・テクスチャ・状態を撮影する。

Expected Result:
3属性が視覚的に判別でき、ブロック形状と季節状態に応じた表示に欠損や誤参照がない。TFCの一般的な花の外観規則を維持する。

### F-006: ドラゴン肉と料理のTFC食料化

#### Description

CEのFire・Ice・Lightning Dragon Fleshの3品および各属性のドラゴン肉入り炒飯3品を、TFC Foodとして登録する。腐敗、栄養、満腹度、保存条件に対応させる。旧IaFTFCの数値を基準とし、肉はhunger=8、protein=3.0、saturation=2.0、decay_modifier=2.0、炒飯はhunger=4、water=20.0、saturation=1.5、grain=1.7、vegetables=2.0、protein=3.0、decay_modifier=3.5とする。非ドラゴン食品のTFC化は行わない。

#### Acceptance Criteria

##### AC-F006-001: ドラゴン関連食品6品のTFC栄養

Preconditions:
CEの3属性のDragon Fleshとドラゴン肉入り炒飯を各1品入手する。

Action:
TFCのFoodデータと腐敗処理を適用し、摂食時の栄養と腐敗状態を確認する。

Expected Result:
6品に上記の食品値が適用され、TFCの栄養・腐敗が作用する。CEが元々持つ食品の特殊効果を変更せず、非ドラゴン食品には追加変更がない。

### F-007: ドラゴンの卵とTFC熱源・淡水

#### Description

CEの卵の孵化ロジックをTFCの設備・流体へ接続する。Fire EggはCE本来の燃焼判定に加え、点火されたTFC Firepit、稼働中のTFC Charcoal Forgeの上でも加温される。Ice EggはCE本来のminecraft:waterに加え、`#tfc:any_fresh_water`の流体でも氷内の卵へ移行する。Lightning Eggの天空・降雨条件と時間経過はCEのままとする。停止した熱源、Salt Waterを追加の有効条件としない。

#### Acceptance Criteria

##### AC-F007-001: TFC火熱設備によるFire Egg孵化

Preconditions:
Fire EggをLitのFirepitまたは稼働中のCharcoal Forgeの直上に配置する。比較用に停止状態の設備も用意する。

Action:
CEの通常の孵化更新を進める。

Expected Result:
稼働中のTFC設備をCEの有効な火熱条件として認識し、停止中は追加の加温条件とならない。CE元来の火熱による孵化条件は変わらない。

##### AC-F007-002: TFC淡水によるIce Egg孵化

Preconditions:
Ice EggをTFC Fresh WaterおよびSalt Waterにそれぞれ配置する。

Action:
CEの氷内卵への変換判定を進める。

Expected Result:
Fresh Waterがminecraft:waterと同様に有効となり、CEの氷内卵へ変換する。Salt Waterは新しい許可条件にならない。

### F-008: Dragon Scale ArmorのKnapping

#### Description

CE既存のDragon Scale Armor全12色について、Helmet、Chestplate、Leggings、Bootsの計48完成品をTFC Leather Knapping相当の5×5パターンで加工する。1回の完成に対応色のDragon Scaleを5枚消費する。元CEの作業台レシピはKnapping経路へ置換する。同じ色の鱗だけを有効とし、完成品のCE元レジストリID・性能は維持する。JEI等へ登録するカテゴリは共通のDragon Scale Knappingとして扱う。

#### Acceptance Criteria

##### AC-F008-001: 鱗の色とKnapping完成品

Preconditions:
12色それぞれのDragon Scaleを5枚以上所持し、TFC式Knappingを開始できる。

Action:
対応する部位のパターンで加工し、異なる色の鱗でも同じパターンを試す。

Expected Result:
指定色の4部位のCE元Armorが完成し、1回につき鱗5枚を消費する。異なる色の鱗から別色の装備は得られない。CE元の作業台レシピでこの工程を迂回できない。

### F-009: Dragon BoneのKnappingと工具

#### Description

Dragon BoneをTFC Rock Knapping相当の5×5パターンで成形する。骨2本を操作開始条件とし、初回のKnapping操作で1本を消費する。Axe Head、Hoe Head、Pickaxe Head、Shovel Head、Sword Bladeを新規に成形し、対応するCE完成工具の組立材料とする。完成工具はCE元IDを使用する。組立時には旧IaFTFCに倣い `#c:bones/wither` を使用する。Dragon Fluteも専用パターンから製作する。Dragonbone Arrow・Bow、属性別Dragon Bone Sword等の既存用途は必要なTFC素材適合以外変更しない。

#### Acceptance Criteria

##### AC-F009-001: Dragon Boneの成形と工具組立

Preconditions:
Dragon Boneを2本以上所持し、対応する工具のKnappingパターンとWither Boneを用意する。

Action:
Knappingを実行して5種類のパーツを作成し、対応する完成工具を組み立てる。

Expected Result:
初回操作で骨1本を消費する。正しいパターンから各パーツが完成し、TFC式組立工程からCE元IDの工具が得られる。元の作業台レシピで成形工程を迂回できない。

##### AC-F009-002: Dragon FluteのKnapping

Preconditions:
Dragon Flute用の骨とKnapping入力条件が揃っている。

Action:
Flute専用の5×5パターンを完成させる。

Expected Result:
追加の中間Tool Headを必要とせず、CE元IDのDragon Fluteが完成する。

### F-010: TFC Dragonsteel MetalとDragon Forge

#### Description

Fire・Ice・Lightningの3属性Dragonsteelを新規のTFC RegistryMetalとして登録する。FireはRed Steel、IceはBlue Steel、LightningはBlack Steelのインゴットと、対応するCE Dragon BloodをDragon Forgeへ投入して作る。CE元のDragon Forgeの属性判定とcookTime=1000を維持する。出力は新規IaFTFCのDragonsteel Ingotであり、完成直後のTFC温度は1800℃とする。

各金属にIngot、Double Ingot、Sheet、Double Sheet、Rod、各Tool Head、Sword Blade、Unfinished Armor、Plated Block／Slab／Stairs、Anvil、Molten Fluidを実装する。溶融温度は2000℃、鍛造可能温度は1200℃、溶接可能温度は1600℃とする。旧レジストリIDを維持するため、流体だけは例外的に `tfc:metal/dragonsteel_fire`、`tfc:metal/dragonsteel_ice`、`tfc:metal/dragonsteel_lightning` のIDを使用し、対応するTFC・共通タグ、熱データ、鋳造・加熱レシピへ接続する。

Dragon ForgeのRecipe Codec、Network Codec、実際のItemStack出力、表示用ItemStackの温度情報を一貫させる。Dragon Forge BrickのCE既存レシピは、石レンガ相当の材料をTFCのFire Bricksへ置換する。既存CE Dragonsteel Ingot・Blockの重複品はF-014で入手不能にする。

#### Acceptance Criteria

##### AC-F010-001: 属性別Dragon Forgeの熱いインゴット

Preconditions:
3属性それぞれのDragon Forge、対応するRed／Blue／Black SteelとDragon Bloodを用意する。

Action:
CEのDragon Forgeで各属性のDragonsteel Ingotを製作する。

Expected Result:
各Forgeから属性の一致するIaFTFC Ingotが生成され、TFCの熱データに1800℃が反映される。ForgeのRecipe保存・読込・ネットワーク同期により出力温度が欠落しない。

##### AC-F010-002: TFC金属と溶融流体の加工経路

Preconditions:
IaFTFC製の3属性Dragonsteel Ingotと、TFCの溶融・鋳造・加熱設備がある。

Action:
金属を熱処理・溶融・鋳造し、対応するTFC素材形態へ加工する。

Expected Result:
3属性の流体が旧公開IDで参照され、温度と鋳造・加熱レシピが対応するIaFTFC製品を生成する。流体・アイテム・ブロック・Recipeの参照に欠落がない。

##### AC-F010-003: Dragonsteel建材と金床の表示

Preconditions:
3属性のPlated Block／Slab／StairsとAnvilをワールドへ設置する。

Action:
形状とテクスチャを画面上で確認する。

Expected Result:
3属性それぞれの形状、材質の配色、ブロックモデルが正しく表示される。CE元のDragonsteel金属色との識別性を保ち、未定義テクスチャや不正なブロックステートを表示しない。

### F-011: Dragonsteelの鍛冶と完成装備

#### Description

Dragonsteel AnvilをTFCの実際のAnvil Block Entityとして登録し、鍛造・溶接UIと加工Tierを機能させる。Anvil Tierは7とし、最初のDouble Ingotの溶接だけはTier 6を許可して、Tier 7 Anvil製作前に進行不能となる循環を防ぐ。その後のSheet、Double Sheet、Rod、Tool Head、Sword Blade、Unfinished Armor等にはTier 7を要求する。

各Tool HeadはIngot1個、Sword BladeはDouble Ingot1個から製作する。完成工具は対応パーツとTFC素材を使って組み立て、鍛造ボーナスを維持する。完成したCEのDragonsteel Tool／ArmorのID・特性を引き継ぎ、素材側はTFC金属の新実装を使用する。ArmorはUnfinished ArmorとSheet等を使う溶接工程へ移す。旧IaFTFCのTier・性能・加工段階を基準とし、CEの完成品を無用に複製しない。

#### Acceptance Criteria

##### AC-F011-001: Tier 6からTier 7への製作進行

Preconditions:
Tier 6のTFC AnvilとDragonsteel Ingotがあり、Tier 7の金床はまだ所持していない。

Action:
初回Double Ingotを製作し、それを使ってDragonsteel Anvilを完成させ、その後のTier 7レシピを実行する。

Expected Result:
初回Double Ingotの溶接がTier 6で成立する。新Dragonsteel AnvilがTFCの実際の鍛冶設備として動作し、その後の指定レシピではTier 7を要求する。Tier 6でTier 7専用加工を迂回できない。

##### AC-F011-002: TFC鍛造からCE完成工具・防具

Preconditions:
Dragonsteelの工具パーツ・Sheet・Unfinished Armorと、Tier 7の設備がある。

Action:
工具とプレイヤーArmorを加工・組立てる。

Expected Result:
CE元IDの対応する完成工具・Armorが得られ、TFC鍛造ボーナスと金属の加工条件が反映される。CE元Dragonsteel Ingotを要求する旧作業台レシピによる迂回経路は残らない。

### F-012: TFC金属製ドラゴン防具

#### Description

Steel、Black Steel、Red Steel、Blue Steelの4系列について、Dragon ArmorのHead・Neck・Body・Tailを新規登録する。防御値はSteel=3.5、Black Steel=5.5、Red Steel=7.5、Blue Steel=7.5とする。既存CEのIronおよび3属性Dragonsteel Dragon Armorを維持し、強度順をIron < Steel < Black Steel < Red／Blue Steel < Dragonsteelとする。

各Armorの材料にはTFCの金属素材とDouble Sheetを使用し、CEのドラゴン用Armor装備スロット・防御力処理・見た目に接続する。旧CEのCopper／Gold／Silver／Diamond／Netherite Dragon Armorのクラフトレシピは無効化する。CE元Dragonsteel Dragon ArmorはTFC新金属から製作する。

#### Acceptance Criteria

##### AC-F012-001: TFC製ドラゴン防具の装着と防御

Preconditions:
4系列の新Dragon Armorの全4部位と、対応する飼育済みドラゴンがいる。

Action:
各部位を対応するドラゴン用スロットへ装備し、防御値を参照する。

Expected Result:
CEのドラゴンArmorとして装着され、系列別防御値が反映される。CEのIron・Dragonsteel Armorと同じ装備処理へ接続され、スロットと防御力の重複計算が生じない。

##### AC-F012-002: ドラゴン用Armorのエンティティ表示

Preconditions:
4系列の新Dragon Armorを装着したドラゴンと、CE元のDragonsteel Armorを装着したドラゴンがいる。

Action:
頭・首・胴・尾を視認できる角度からスクリーンショットを撮影する。

Expected Result:
16種類の新Armorが対象の部位に正しく描画され、テクスチャ欠損や部位ずれがない。4系列の金属色を判別できる。

### F-013: ドラゴン関連のTFC物理属性と携行分類

#### Description

Fire・Ice・Lightning DragonのTFC物理ダメージ耐性をPiercing／Slashing／Crushing各30とする。Dragon Scale ArmorにはBlack Steel相当、プレイヤー用Dragonsteel Armorと既存CE Dragonsteel Dragon ArmorにはPiercing70／Slashing60／Crushing70の耐性を設定する。新TFC金属製Dragon Armorは対応するSteel／Black Steel／Red Steel／Blue Steel相当の耐性を持つ。ドラゴン関連武器・工具の攻撃属性はTFCのPiercing／Slashing／Crushing系へ対応させ、非ドラゴン装備を一括改変しない。

CEのDragon Egg、Dragon Skull、Dragon Armor、Dragonsteel Armor、Forge設備、新Dragonsteel素材・建材へTFCのSize・Weightを設定する。Eggはvery_large／very_heavy、Skullはhuge／very_heavy、プレイヤー用Dragonsteel Armorはlarge／very_heavy、ドラゴン用Armorはvery_large／very_heavy、Dragonsteel Anvilはhuge／very_heavy、Forge Brick／Core／InputとPlated Blockはsmall／light、Plated Slabはsmall／very_light、Plated Stairsはsmall／lightを基準とする。TFCの標準分類と異なる以下のDragon関連品は旧IaFTFCの値を継承する。Dragon Boneはvery_large／medium、Dragon Scale 12色・Dragonbone部品5種・Dragonsteelの工具頭5種と未完成防具4種（各3属性）・Dragon Flute・Dragon Seeker 4種はlarge／medium、DragonboneのAxe・Sword・属性Sword 3種とDragonsteelのAxe・Sword（各3属性）はvery_large／very_heavy、Dragon Horn・Dragon Stick・Dragonbone Arrowはnormal／light、Dragon Blood 3種はnormal／medium、Dragon Meal 3種・属性Stew 3種・Dragon Flesh 3種・ドラゴン肉炒飯3種・Dragon Heart 3種・ドラゴンのSpawn Egg 3種はsmall／lightとする。その他のDragon関連品はTFCの対応カテゴリに従う。

#### Acceptance Criteria

##### AC-F013-001: ドラゴン関連の物理耐性

Preconditions:
3属性ドラゴン、Dragon Scale Armor、Dragonsteel Armor、4系列の新Dragon Armorを用意する。

Action:
TFC物理ダメージ判定に対応する攻撃を与え、設定した耐性の効果を比較する。

Expected Result:
各対象に指定したTFC物理耐性が適用される。CE元の体力・通常Armor値と同じ処理が二重実行されず、非ドラゴン装備には新しい耐性の改変を加えていない。

##### AC-F013-002: Dragon関連アイテムのサイズ

Preconditions:
指定したDragon Egg、Skull、Armor、Forge設備、Anvil、Plated Block類を所持する。

Action:
TFCのサイズ・重量制限がある収納や携行操作を実行する。

Expected Result:
上記のSize・Weightに対応するTFC収納制限が作用し、ドラゴン関連以外のCEアイテムへ不要な一括変更がない。

### F-014: CE重複資源と不要な入手経路の無効化

#### Description

TFCに同種の金属・植物などを一般化する機構がある場合はTFC系を正規の入手経路とし、CEの重複品をサバイバルで取得できなくする。CEのSilver Ingot／Nugget／Block／Raw Silver／Ore／Tool／Armor、Sapphire Ore／Gem／Block、CE元Dragonsteel Ingot／Block等の重複素材を対象とする。旧レシピだけでなく精錬・圧縮・解体・Loot・交易・特殊装置の入手経路も整理する。CE元のアイテム自体をRegistryから削除する必要はない。

CEのGold／Silver／Copper PileはF-002でTFC Oreを出力する既存装飾要素として維持する。Dragon ScaleのSilverやSapphireという色名は無効化対象の資源重複と混同しない。TFC金属から作れるCE完成Dragonsteel装備も無効化しない。旧IaFTFCの無効化Tooltip規則に従い、正規入手を停止したCE品には英語の`[DISABLED]`表示を付ける。

#### Acceptance Criteria

##### AC-F014-001: 重複品のサバイバル入手経路停止

Preconditions:
CE 2.2のレシピ・Loot・交易・生成ドロップ等の入手経路が登録されている。

Action:
無効化対象となるCE品の生成経路と、IaFTFCが置換するTFC素材の経路を調べる。

Expected Result:
重複するCE金属・宝石・Dragonsteel素材は正規サバイバルのどの対象経路からも入手できない。対応するTFC／IaFTFC素材は入手でき、Pile、Dragon Scaleの色名、CE完成Dragonsteel装備は誤って禁止されない。

##### AC-F014-002: 無効化品のTooltip

Preconditions:
無効化対象のCE素材と、引き続き使用するCE完成装備を表示できる。

Action:
アイテム説明を表示する。

Expected Result:
無効化したCE品に英語の`[DISABLED]`を示し、引き続き正規入手するCE完成装備・Pile由来の正規資源には誤表示しない。

### F-015: ドラゴン関連補助道具のTFC素材適合

#### Description

Dragon Horn、Dragon Staff、Dragon Seeker、Summoning Crystal、Dragon Mealおよびドラゴンの飼育・呼び出しに直接必要なCE既存道具のレシピについて、TFCワールドで得られる適切な材料・加工工程を使用できるようにする。ドラゴン本体の保存データ、召喚、帰還、消費等の既存機能を再実装しない。Dragon FluteのKnappingはF-009、Dragon Mealの食材適合はF-004が担当し、同じ成果物の別実装・別試験を作らない。

#### Acceptance Criteria

##### AC-F015-001: ドラゴン補助道具のTFC製作経路

Preconditions:
CEのドラゴン関連補助道具のレシピ入力と、対応するTFC材料が存在する。

Action:
元の用途に対応したTFC素材・加工工程から対象道具を製作する。

Expected Result:
CE元IDの補助道具へ到達するサバイバル製作経路が存在し、CE元の入手不能なVanilla素材を強制しない。既存CEの装備・召喚機能自体の動作確認を重複して行わない。

## Global Requirements

### 製品の境界

- 必須依存はMinecraft 1.21.1向けTerraFirmaCraftとIce and Fire: Community Editionである。設計基準はTFC 4.2.14、CE 2.2とする。CEが利用するUranusの機能はCEの依存関係を通じて扱う。
- 新たなドラゴン種、独自の成長AI、逃避AI、TFC動物を捕食対象とする標的タグ、非ドラゴンのCE生物・武器・食品の連携は実装しない。CEの非ドラゴン構造物の自然生成と非ドラゴンMobの自然スポーン・チャンク生成時スポーンは無効化する。対象はCEの生成経路に限定し、既存構造物・既存個体・アイテム・スポーンエッグ・コマンドによる召喚は削除しない。
- CE本体のドラゴンの体力、元Armor、年齢、飛行、騎乗、敵対、繁殖効果、ブレスの対生物処理、Dragon Horn等の元機能は継承する。IaFTFCが拡張する箇所以外はCEに委ねる。
- 新規に追加する植物、金属、溶融流体、Item Size、物理ダメージ属性、FoodはTFCの登録APIとデータ形式に基づく。独自に同じ仕組みを作り直さない。
- 新規ワールドのみを対象とする。既存チャンクへのレトロ生成、旧版ワールドのデータ移行、第三者ワールド生成Modとの互換保証は実装しない。
- 表示言語は英語の `en_us` のみとする。

### 既存IDと設定の扱い

- Mod IDは `iaftfc` とし、Javaパッケージは `net.claustra01.iaftfc` を使用する。CEの既存完成装備、Stew、道具のIDは変更しない。旧版のTFC名前空間に属するDragonsteelの3属性Molten Fluid IDも維持する。
- CEにConfigが存在する事項はCE設定を優先する。IaFTFCが同じ設定を別名で追加しない。CE 2.2にない構造物spacingはF-001のIaFTFC Configで扱う。
- CEのブロック破壊に関係するGriefing設定とmobGriefing、およびCEのブロック保護判定を維持する。TFC設備全体の一括保護と破壊禁止は追加しない。
- 無効化対象のCE品はレジストリから削除せず、通常サバイバルにおける入手経路だけを停止する。CE元の資源入手経路を無効化する際は、代替するTFC素材・レシピが存在する状態を保つ。

### 実装と成果物

- 新規コードベースはNeoForge公式Minecraft 1.21.1用MDKから構築する。Harnessの新規Workspace作成時は、既定のNeoForge MDKテンプレートを利用する。旧IaFTFC v0.0.1〜v0.0.3のGit履歴・アセットは調査対象であるが、Java／Mixin／JSON／PNG／Gradle設定を旧ソースツリーから一括移植しない。
- 公開API、Tag、Recipe、Worldgen JSONを優先し、MixinはCE構造物pieceの置換、ブレス対象のTFC分類、Uranusを介した給餌評価、卵孵化、Dragon Forge温度など、データ設定のみでは成立しない箇所へ限定する。旧Mixinのターゲット名を無条件に再利用しない。
- CE・TFC双方のリソースIDを扱う場合は、正しい名前空間、Recipe ID、Tag IDを使用する。CEの元JSONを直接コピーせず、現在のCE 2.2の実データから必要な差分だけを新規記述する。
- 新規に作る金属・防具のモデルやテクスチャは新たに制作する。元CEの属性別の配色およびTFCの資源形態を参照する。属性植物のテクスチャはCEの各Lilyテクスチャから作成する。Dragon Armorのエンティティ表示に使用するリソースはCEが要求する形式へ合わせる。
- サーバー側とクライアント側の処理を分離する。新しいデータ駆動の登録・同期・Worldgenは専用サーバーの実行環境でも成立する設計にする。

### 受入基準の所有と検証範囲

- 各ACは一つのFeatureが所有する。同じ結果を別のFeatureや別のACに重ねて記述しない。複数のFeatureで必要な共通条件は本節へ記述し、個別のACとして繰り返さない。
- Minecraft、NeoForge、CE、TFC、およびUranusが通常の依存関係・レジストリ・APIとして保証する基礎動作は検査対象にしない。依存Modの導入有無、Minecraftの起動自体、既存CEアイテム本来の効果、バニラのゲームルール単独の機能を再試験しない。**IaFTFCが追加・上書きした連携の境界だけ**を検証する。
- Harnessが標準で実行するCompile／Build／Code Reviewを、それ自体の成功を確かめる独立したACとして追加しない。通常のコンパイルや既存のMod ID解決で保証される自明な事項を、改めてGameTestやE2Eの目的としない。
- ゲーム状態の変化はGameTest、追加モデル・テクスチャ・Tooltipの描画はE2Eで検証する。描画を伴わないACにE2Eを重複させない。全品目の静的登録情報を一回照合すれば足りる場合、アイテムごとに同内容のテストを量産しない。
- 変更が特定の機能に限定される場合、先に成功した無関係の実装や以前の調査結果を初めから作り直さない。変更の影響があるACのみ修正・再評価し、Harness標準の工程と回帰確認の範囲を守る。
- 実装時の未確定技術事項については、対象版の公式ソースを一度調べて設計し、後続工程で同じ事実調査を重複実施しない。一次資料のGitコミットを記録して、変更があった場合だけ差分を確認する。

## Constraints

- 対象プラットフォームはMinecraft 1.21.1／NeoForge 21.1.248／Java 21である。新規ビルドで使用するTFCの基準版は4.2.14とし、4.2.5への下位互換を未確認のまま宣言しない。
- 旧版の構造物気候・Knapping・Dragonsteel・戦利品は以下の固定Gitコミットから再確認した。旧版に実装された値と、本書で新たに確定した値を混同してはならない。
  - IaFTFC旧版 v0.0.3： https://github.com/claustra01-modding/IaFTFC/tree/bc0b7d8185b968eef84106ccf0b6be77995edbda
  - CE 1.21.1／2.2： https://github.com/IAFEnvoy/IceAndFire-CE/tree/1e7855e1bca50d24f54b9ea17632b54553521e17
  - TFC 4.2.14： https://github.com/TerraFirmaCraft/TerraFirmaCraft/tree/f49fc94dfc83ff6a285d854a1dff4ddac21c8bb3
  - TFC 4.2.5： https://github.com/TerraFirmaCraft/TerraFirmaCraft/tree/c63813b94f31fef3234f1ae8d7cc8dab3c08835d
  - Uranus 1.21.1： https://github.com/IAFEnvoy/Uranus/tree/06f19778e02bbe35925255033f403ac6aa0c2ca6
- CE 2.2はUranusの配布JARを外部依存として参照する。上記UranusソースのGitコミットとCEが実際に使用するJARの内容は同一と確認していないため、給餌への介入箇所の実装時に使用JARのメソッドを確認する。この確認を一般の依存Mod起動試験と混同しない。
- IaFTFCの旧版には資源生成、戦利品、鍛冶等の生成スクリプトが存在したが、v0.0.3の作業ツリーでは削除されている。旧履歴を資料として表示する場合も、そのスクリプトを開発基盤としてコピーしない。
- F-001の構造物spacingは単なるConfig宣言では実際のWorldgen JSONへ反映されない。CE 2.2の構造物セットはデータ駆動であるため、実際に配置レジストリへ設定値を反映する方法を実装する。
- ユーザーが決定していない気候閾値や給餌換算係数を、旧版・CEの既定値という理由だけで最終仕様として固定しない。

## Open Questions

None.

