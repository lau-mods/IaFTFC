# mcmods 共通開発ガイド

このディレクトリ直下の各Modリポジトリでは、各リポジトリ内の `AGENTS.md` に加えて以下を共通規約とする。子リポジトリは単独でcheckoutされることがあるため、共通規約を変更した場合は各 `AGENTS.md` にも同じ変更を反映する。

## 共通開発ルール

### 仕様書と正本

- 仕様ファイル名は大文字の `AGENTS.md` に統一し、小文字の `agents.md` は作らない。
- `AGENTS.md` は将来案ではなく現在維持すべき仕様を記録する。未実装事項を残す場合は、実装済みと混同しないよう状態を明記する。
- このリポジトリの製品仕様は `PROJECT.md` を唯一の正本とする。`AGENTS.md` は開発規約と実装入口のみを記す。
- `README.md` は利用者向けの名称、短い概要、配布先、build入口に絞り、実装仕様、作業履歴、長い使用例を重複掲載しない。
- ライセンスと第三者表示は各リポジトリrootの `LICENCE` 一つへ統合する。`LICENSE`、template license、別noticeファイルを追加しない。
- ユーザーに見える挙動、対応バージョン、依存関係、ID、登録内容、有効化条件、データ生成規則、検証手順を変更した場合は、同じ変更で `AGENTS.md` も更新する。
- 現在値の正本は `gradle.properties`、Gradle設定、Mod metadata、実装コード、同梱dataとする。文書と食い違う場合は実物を確認し、文書も追従させる。
- バージョン番号や依存バージョンは、依頼または実装上の必要がない限り変更しない。

### 調査と互換実装

- Minecraft、loader、TFC、連携Modの仕様は、対象バージョンの公式ソース、依存ソース、または実際のJAR/dataを確認してから固定する。別バージョンの記憶だけでID、API、JSON形式を決めない。
- 連携は公開API、registry、tag、recipe・loot・worldgen等のdataを優先し、Mixin、accessor、reflectionは必要な対象へ狭く限定する。
- 必須・任意依存の区別とmetadata上のorderingを維持する。任意Modがない環境では、そのModのクラスを通常ロード経路から参照しない。
- クライアント専用クラスを共通・サーバー側から参照しない。登録、event、Mixinを変更する際は専用サーバーでのclass loadingを考慮する。
- 公開済みのregistry ID、resource namespace、config keyを変える場合は、ワールドとdata packの互換性を優先する。破壊的変更は明示された要件なしに行わない。
- 自Modのresourceは自Mod namespaceを基本とし、他Mod namespaceへ置くのはtag追加や同名上書きなど、意図した互換dataだけに限定する。

### ファイルと生成物

- 依存JAR、展開物、比較画像、解析・生成・検証スクリプトは各リポジトリ直下の `.tmp/` に置き、Git管理外にする。
- Git管理するscriptはGradle wrapper、build、CI等、clone直後の通常工程に必須なものだけに限定する。子 `AGENTS.md` が正式generatorとして明示したものを除き、補助scriptを `tools/` や `scripts/` としてcommitしない。
- 仕様と生成済みresourceを正本とし、`.tmp/` の補助scriptを唯一の仕様にしない。再生成時も同じ入力から決定的な結果になるよう検証する。
- JSONはBOMなしUTF-8で保存し、対象Minecraft版のpack layoutと形式に合わせる。
- `.gradle/`、`build/`、`run/`、`runs/`、IDE metadata、依存JAR、展開物を変更対象やcommitへ含めない。
- 各子ディレクトリは独立したGitリポジトリとして扱い、コマンドと差分確認は対象リポジトリ内で行う。既存の無関係な変更は編集しない。

### 実装と検証

- 変更前に既存の登録方法、命名、resource配置、生成scriptを確認し、同じ責務を重複実装しない。
- 変更範囲は依頼された挙動に絞り、無関係なformat変更、rename、依存更新、生成物の全面置換を混ぜない。
- 通常の途中確認は `./gradlew compileJava`、完了確認は `./gradlew build` を基本とする。wrapperに実行権限がない場合は `sh gradlew ...` を使う。
- data変更時は全JSONの構文解析を行い、tag、recipe、loot、model、texture、生成scriptには内容に応じた整合性検査を追加する。
- optional連携、Mixin、client/server境界を変更した場合は、対象Modの有無と専用サーバーでの安全性を重点的に確認する。
- Minecraftクライアントの起動は通常の完了条件にせず、ランタイムでしか確認できない変更または明示依頼がある場合に行う。
- 実行できなかった検証は成功扱いにせず、理由と未確認範囲を作業報告へ残す。

## iaftfc 固有規約

- 製品仕様の正本は `PROJECT.md`。この文書は共通開発規約と実装入口だけを示し、製品仕様を重複して定義しない。
- 固定版は Minecraft 1.21.1、NeoForge 21.1.248、TFC 4.2.14、IaF CE 2.2。版の変更は `PROJECT.md` に基づく検証を伴う。
- レシピ、Loot、model等の正式generatorは `./gradlew runData` とし、出力を `src/generated/resources` に置く。DragonsteelのPNGだけは正式generator `tools/generate_assets.py`（Python 3 + Pillow）で生成し、`src/main/resources/assets/iaftfc/textures/` に保存する。その他の補助scriptは `.tmp/` に置く。
- 元 Mod のデータを上書きする場合、`src/test/resources/iaftfc/gametest/override_manifest.json` の許可リストへ対象 ID を追加し、GameTestで実際の上書きと照合する。
- Dragon構造物の気候選択とCave鉱石選択は `src/main/java/net/claustra01/iaftfc/worldgen/`、元生成処理への限定差分は `src/main/java/net/claustra01/iaftfc/mixin/`、検証入口は `src/test/java/net/claustra01/iaftfc_test/gametest/WorldgenGameTests.java` と `tests/e2e/dragon-structures.mjs` とする。
- Dragon構造物の属性別TFCチェストと岩石適合鉱石は `worldgen/DragonChests` と `worldgen/DragonCaveOres`、9種のチェストLoot・装備profileの共有Loot・3種のPile Lootは `datagen/DragonLootData`、検証入口は `gametest/DragonStructureLootGameTests` とする。
- Dragon構造物のTFC地表Biomeタグは `DragonBiomeTags` から `runData` で生成し、元IaF構造物タグへの追加として管理する。
- 属性LilyのRegistryPlant・PlantBlock登録は `plant/IaFTFCPlant` と `plant/IaFTFCPlants`、クライアントの透過描画層登録は `plant/ElementalPlantClient`、TFC気候配置は `worldgen/ElementalPlacement`、元IaFのLily生成・Loot・Stewの限定上書きとTFC花タグは `datagen/ElementalWorldgenData`、植物モデルと表示名は `datagen/ElementalPlantAssets` と `LanguageProvider` から `runData` で生成する。TFC土壌で不足する場合だけ `mixin/ElementalFlowerSoilMixin` で補い、検証入口は `gametest/ElementalGameTests` と `tests/e2e/lilies.mjs` とする。
- GameTestは固定シード0のTFC Overworldで実行し、`src/test/java/net/claustra01/iaftfc_test/mixin/GameTestWorldPresetMixin.java` をテスト用ワールド生成の入口とする。
- Dragonsteelの指定部品・Fluid・金床登録は `metal/Dragonsteel`、金属加工データと表示素材の生成は `datagen/DragonsteelData`、GameTest入口は `gametest/DragonsteelGameTests` とする。生成と検証には `./gradlew runData`、`./gradlew runGameTestServer` を使う。
- DragonsteelのBlock Loot、採掘・金床タグ、2種のIngot Mold鋳造レシピは `DragonsteelData` から生成し、描画は `tests/e2e/dragonsteel.mjs` で確認する。
- Dragon Forgeの既存レシピ上書き、旧Dragonsteel製法の無効化、完成装備の組み立て・加熱データは `DragonsteelData` から生成する。Forge本体は `gametest/DragonForgeGameTests`、完成装備は `gametest/DragonsteelGameTests`、Forge表示は `tests/e2e/dragon-forge.mjs` で検証する。
- Dragonbone部品の登録は `knapping/DragonboneParts`、Knapping Type・レシピ・旧製法の無効化・表示素材の生成は `datagen/KnappingData` を入口とする。生成物は `./gradlew runData` で更新する。
- DragonboneとDragonscaleのメニュー処理、組み立て、旧レシピの検証入口は `gametest/KnappingGameTests`、描画確認は `tests/e2e/knapping.mjs` と `tests/e2e/scale-armor.mjs` とする。
- Dragon Armorの登録は `dragonarmor/DragonArmor`、レシピ・レシピ無効化・TFC item_size・モデルとitems JSONの生成は `datagen/DragonArmorData` とする。PNGは `src/main/resources/assets/iaftfc/textures/item/` と `src/main/resources/assets/iceandfire/textures/entity/dragon_armor/` に置く。装着と防御制限は専用Mixin、検証入口は `gametest/DragonArmorGameTests` と `tests/e2e/dragon-armor.mjs` とする。
- 生きたDragonのブレスによるTFC地形変換は `mixin/DragonBreathTerrainMixin`、Forge・Entity Damage・griefing境界の検証は `gametest/DragonBreathGameTests` を入口とする。TFCの既存カテゴリタグを参照し、IaFのWorldEventと破壊判定は維持する。
- 卵のTFC条件は `mixin/DragonEggConditionsMixin`、DragonのCalendar成長・NBT・Horn解放時の基準点更新は `dragon/CalendarDragonGrowth` と `mixin/DragonCalendar*Mixin` と `mixin/DragonHornCalendarMixin` を入口とする。`gametest/DragonEggCalendarGameTests` と `tests/e2e/dragon-egg-hatch.mjs` で検証する。
- Dragon Mealの生肉タグ・腐敗条件、Dragon Flesh/StewのFoodDataとサイズ、Seekerの材料置換は `datagen/DragonFoodAndSeekerData` から生成する。Stewを食べた際のボウル返却は `mixin/DragonStewBowlMixin`、検証は `gametest/DragonFoodAndSeekerGameTests` と `tests/e2e/dragon-food-seeker.mjs` を入口とする。
- iaftfc専用クリエイティブタブと無効化アイテムの表示除外は `IaFTFCCreativeTab` を入口とする。
- IaF属性ブロックのTFC落盤・地滑り、IaF武器・DragonのTFCダメージ種別と耐性、IaF Silver/Sapphire系レシピの無効化は `datagen/TfcCompatData` から `runData` で生成し、`gametest/TfcCompatGameTests` で検証する。
- Bestiaryの新製法ページは `src/main/resources/assets/iceandfire/lang/bestiary/<locale>_0/` で上書きし、`gametest/BestiaryGameTests` と `tests/e2e/bestiary.mjs` で検証する。
- M13の進行連結は `gametest/ProgressionGameTests`、完全停止を挟む保存検証は `PersistenceFixtureGameTests` と `PersistenceVerifyGameTests` を入口とする。`runGameTestServer` は2段階の保存検証、独立2出力のdata比較、AC対応検査を実行し、結果を `build/test-results/gametest/` に記録する。再接続表示は `tests/e2e/integration-reconnect.mjs` で確認する。

## Dragonsteel texture generator

- 再生成入口は `python3 tools/generate_assets.py`。既存iaftfcの形状、Ice and FireのIngot配色、TFC Wrought Ironの同形状のL範囲を参照する。変換方式と対象形状は同scriptに定義する。
- 対象はFire・Ice・Lightningの素材・工具頭・Block・Smooth各12形状、計36画像。未完成防具12画像は生成対象に含めない。
- 原画像は `tools/texture_sources/` に無加工で保持する。入力の出典と権利表示は `LICENCE` に集約する。ハッシュ対応表や比較画像は追加しない。
- 画像の参照先は既存 `DragonsteelData` のmodelと同じ。金属itemへの乗算着色はなく、`DragonsteelClient` は溶融流体とBucketの描画を担当する。
- 再生成のバイト一致、原形状のalpha一致、未完成防具が不変であること、modelの画像参照を検証し、`./gradlew build` で同梱を確認する。

## 依存バージョンの許容範囲

- NeoForge と依存 Mod の実行時制約は最低対応版以上の `[最低版,)` とする。完全一致指定や上限によって更新版の起動を拒否しない。
- Gradle・Harness・製品仕様に記載した固定版はビルドと再現試験の基準版。実行時の許容範囲とは分けて扱い、依存バージョンのテストも最低版以上で判定する。
