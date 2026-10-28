# IaFTFC 開発ガイド

製品仕様と Acceptance Criteria の正本は `PROJECT.md`。
計画は `.harness-state/plan.json`、進捗は `.harness-state/progress.json` に保存する。
Mod ID は `iaftfc`。現時点の製品コードは TFC と IaF CE を必須依存として宣言する
IaFTFC mod であり、ゲームプレイ機能は未実装。runtime の準備を AC 合格とは扱わない。

## Harness runtime

- Minecraft 1.21.1、NeoForge 21.1.265、Java 21 を使用する。
  `PROJECT.md`、`gradle.properties`、server と client の版を揃える。
- runtime は Git 管理外の `.harness-state/runtime/` に置く。
  MC Pilot CLI 0.20.2 は `pilot/node_modules/.bin/mct`、専用サーバーは `server/`。
  CLI の `data/variants.json` の `1.21.1-neoforge` は 21.1.265 に合わせる。
- MC Pilot の環境変数は `MCT_HOME=$PWD/.harness-state/runtime/mct-home`、
  `MCT_CACHE_DIR=$PWD/.harness-state/runtime/mct-cache`、`MCT_SKILL_TARGETS=none`。
- クライアントは `iaftfc-a` と `iaftfc-b`、offline account は `iaftfc_a` と `iaftfc_b`。
  操作用 WebSocket port は順に 25586、25587。
- サーバーは `127.0.0.1:25585`。`server-ip=127.0.0.1` でローカル接続に限定し、
  offline account を受け入れる。検証ワールドは `harness-world`。
- server と両 client の `mods/` に次の配布 JAR を置く。
  TFC 4.2.12、Ice and Fire CE 2.1.4、Patchouli 1.21.1-93-NEOFORGE、
  Uranus 2.4.1-bugfix、Jupiter 2.3.7。すべて 1.21.1 / NeoForge 用。
  Uranus は必須 Architectury 13.0.8 を内包し、IaF CE は Integration 0.2 を内包する。
  内包ライブラリを別 JAR として重複導入しない。
- MC Pilot は client に操作用 mod を配置する。この構成での起動・接続確認と、
  `PROJECT.md` が要求する製品および必須依存だけの受け入れ試験は区別する。
  最小構成の AC を MC Pilot mod 入りの確認だけで合格にしない。
- macOS の server と client は arm64 Java 21 を使用する。
  client 作成時は `--java` で指定し、server は `run.sh` に指定する。
- server の `user_jvm_args.txt` と client の `instance.json` の
  `env.JAVA_TOOL_OPTIONS` に `-Dneoforge.readTimeout=120` を指定する。
  server と各 client の最大 heap は 3 GiB。
- 手動の起動確認は `level-type=tfc:overworld` で行う。
  現在の Harness は E2E 開始時に world を削除し、superflat・構造物無効へ上書きする。
  TFC 地形・自然生成を要求する AC は、この既定 E2E world だけでは検証できない。
  該当 AC の実装時に TFC world を使う検証工程を用意する。
- `npm --prefix .harness run build` で Harness をビルドする。
  `node .harness/dist/cli/main.js doctor` と `validate` で構成を確認する。
  `node .harness/dist/cli/main.js plan` は Claude を使用し、仕様・計画・初期進捗を commit する。
- Harness の画像レビューは対象 AC に関する Mod の描画を対象とし、チャット・通知・
  バニラ HUD・F3・画面内のログ文字列は対象外とする。E2E 起動時はチュートリアル、
  コマンド応答と進捗通知を抑制する。画像レビューの通過だけで起動・依存条件の AC を合格にしない。
- 起動確認後は client と server を停止する。runtime、依存 JAR、log は commit しない。
- 現行の必須依存構成は TFC world への2クライアント同時接続まで確認済み。
  server では IaF CE の任意連携レシピ5件の読み込みエラーと、client 専用クラスの
  読み込みエラーが記録される。起動継続・接続成功とエラーの解消を混同しない。
  証拠は `.harness-state/runtime/smoke/`。製品の統合検証は未実施。

## 実装と検証

- 基本パッケージは `net.claustra01.iaftfc`
- 依存 Mod の仕様は対象バージョンの公式ソースまたは実際の JAR/data で確認する。
- 解析物・補助スクリプトは Git 管理外の `.tmp/` に置く。
- 製品コードの途中確認は `./gradlew compileJava`、完了確認は `./gradlew build`。
  JSON は構文を検証し、data 変更には参照先と個数・IDの整合性確認を加える。
- 必須依存、公開ID、検証手順、実装済みの挙動を変更した場合は本書も更新する。
  仕様の採用やビルド成功だけを実ゲームの動作確認とみなさない。

## 必須依存の照合

- 配布 JAR の `META-INF/neoforge.mods.toml` と Jar-in-Jar metadata を照合する。
  TFC 4.2.12 は Patchouli 1.21.1-92 以上、IaF CE 2.1.4 は Uranus 2.3.2 以上と
  Jupiter 2.3 以上を要求する。採用版は `PROJECT.md` の表に固定する。
- Uranus 2.4.1-bugfix は Architectury 13.0.8、IaF CE 2.1.4 は Integration 0.2 を
  内包するため、それらを独立した JAR として導入しない。
- 正常構成の専用 server は既存の検証 world を退避して `level-name=harness-world`、
  `level-type=tfc:overworld`、`generate-structures=true` で新規作成する。client A/B を同時接続し、
  TFC 地形が描画された接続完了画面、server log、両 client log、`/neoforge mods` の省略されて
  いない出力を `.harness-state/runtime/smoke/tfc-world/server/` に保存する。
- 同じ JAR 集合の client A では既存の singleplayer world を使わず、world preset に TFC を選んだ
  新規 world を作成する。ロード完了後の TFC 地形、ポーズ画面、全 mod 一覧の各画面と client log を
  `.harness-state/runtime/smoke/tfc-world/singleplayer/` に保存する。専用 server への接続成功を
  singleplayer の代用にしない。
- server と singleplayer の各証拠で `minecraft`、`neoforge`、`iaftfc`、`tfc`、`iceandfire`、
  `patchouli`、`uranus`、`jupiter`、内包された `architectury`、`integration` の ID と版を照合する。
  ページ分割された画面は先頭から末尾まで順に保存し、重複 ID、余分な独立 JAR、欠落 ID がないことを
  log の `Mod List` と相互確認する。MC Pilot の client mod がある自動 E2E の一覧はこの最小構成証拠に
  含めない。
- 欠落試験では正常構成を複製し、Patchouli、TFC、Uranus、Jupiter、IaF CE の順に
  1 JAR だけを外して起動する。各回で欠落 ID または版不適合を示すロード失敗を記録し、
  screen と log を `.harness-state/runtime/smoke/missing/<mod-id>/` に保存する。テスト後に
  JAR を戻し、正常起動を確認してから次の対象を外す。内包ライブラリは個別の欠落対象に
  しない。

## M02 の公開IDと検証

- 金属素材は `iaftfc:metal/<形態>/<dragonsteel_fire|dragonsteel_ice|dragonsteel_lightning>` の42品。建材と金床は `iaftfc:metal/block/<金属>`、`<金属>_slab`、`<金属>_stairs`、`iaftfc:metal/anvil/<金属>` の12ブロックと12アイテム。
- 溶融金属は `tfc:metal/<金属>` と `tfc:metal/flowing_<金属>`。金床はTFCのTier 7として登録し、液体ブロックやバケツは登録しない。
- M02のGameTestは `./gradlew runGameTestServer` で実行し、JUnit XMLは `build/gametest-results.xml` に出力する。表示確認は `tests/e2e/dragonsteel-blocks.mjs` を使用する。ビルド・静的確認と実ゲーム合格は区別する。

- M02のドラゴン骨部品は `iaftfc:dragonbone/<axe_head|hoe_head|pickaxe_head|shovel_head|sword_blade>` の5品。ドラゴン防具は `iaftfc:dragonarmor_<steel|black_steel|red_steel|blue_steel>_<head|neck|body|tail>` の16品。完成した道具とプレイヤー防具は追加しない。
- 金属はTFCの `tfc/metal` と `tfc/fluid_heat` に融点2000℃、比熱0.00857、Tier 7を定義する。溶融金属の色は炎 `#6E211B`、氷 `#6BADC9`、雷 `#4D3A69`。
- 素材は対応する `#c:ingots`、`#c:double_ingots`、`#c:sheets`、`#c:double_sheets`、`#c:rods` の属性別・全体タグに加入する。通常ブロックはitem/block両方の `#c:storage_blocks/<金属>` と全体タグに加入し、金床は `#tfc:anvils` に加入する。溶融金属は `#c:molten_<金属>` と `#tfc:molten_metals` に加入する。タグは既存メンバーを保持する。
- 建材と金床はツルハシ採掘タグを持ち、loot tableは単体1個、二重ハーフのみ2個を落とす。M02の保存・表示についてはGameTestとE2Eの実行結果を確認するまで合格としない。

## M03 の公開IDと検証

- IaF CE の `iceandfire:dragonforge/dragonsteel_<fire|ice|lightning>_ingot` を上書きし、TFC の赤鋼・青鋼・黒鋼と対応する血から `iaftfc:metal/ingot/dragonsteel_<属性>` を製造する。進捗量は1000。
- 炉の `smeltItem` が正規インゴットを出力に入れた直後、Mixin が出力スタック全体へ温度を設定する。熱容量は3属性共通の `tfc/item_heat/dragonsteel_ingots.json` から読む。既定1800℃は `data/iaftfc/forge_heat/dragonsteel.json` の `temperature` で変更でき、レシピ見本と新規製品へ適用する。0以下・非有限値は警告して1800℃に戻す。保管済みの品は変更せず、取り出した実物はTFCの規則で冷却する。
- 炉レンガの3レシピは TFC 耐火レンガ4個と属性鱗ブロック5個から4個を得る。炉心・入力部は IaF CE の既存形態を使用する。
- GameTest の JUnit XML は `build/gametest-results.xml`、表示シナリオは `tests/e2e/dragonforge-ingots.mjs`。保存・複数クライアント・実ゲームの合格は対応する実行証拠を確認して判断する。

## M04 の公開IDと検証

- 3属性それぞれに `iaftfc:anvil/<形態>/<金属>` の11鍛造、`iaftfc:welding/<形態>/<金属>` のダブルインゴット・ダブルシート・防具4部位、`iaftfc:crafting/anvil/<金属>`、`iaftfc:crafting/tool/<金属>_<道具>`、`iaftfc:casting/<金属>_ingot`、`iaftfc:heating/<金属>/<形態>` の27加熱レシピを提供する。IaF CE の dragonsteel 完成装備の旧形状レシピ27件は同IDで無効化する。
- ダブルインゴットの溶接はTier 6、それ以外の溶接と鍛造はTier 7。鍛造1200℃、溶接1600℃、溶解2000℃を境界値を含めて適用する。道具部品からの組立と防具溶接はTFCの鍛造品質を継承する。
- M04 の GameTest は `./gradlew runGameTestServer` を使い、結果は `build/gametest-results.xml` に出力する。単体確認は `./gradlew test`、表示確認は `tests/e2e/dragonsteel-metalworking.mjs` と `tests/e2e/dragonsteel-equipment.mjs` を使う。永続化の再起動検証と E2E の溶接・防具仕上げ操作は未実施であり、各実行証拠を確認するまで合格としない。
