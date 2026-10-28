# IaFTFC 開発ガイド

製品仕様と Acceptance Criteria の正本は `PROJECT.md`。
計画は `.harness-state/plan.json`、進捗は `.harness-state/progress.json` に保存する。
Mod ID は `iaftfc`。現時点の製品コードは Example Mod テンプレートであり、
runtime の準備を IaFTFC の実装完了・AC 合格とは扱わない。

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
  この環境では `/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin/java`。
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
- 起動確認後は client と server を停止する。runtime、依存 JAR、log は commit しない。
- 現行の必須依存構成は TFC world への2クライアント同時接続まで確認済み。
  server では IaF CE の任意連携レシピ5件の読み込みエラーと、client 専用クラスの
  読み込みエラーが記録される。起動継続・接続成功とエラーの解消を混同しない。
  証拠は `.harness-state/runtime/smoke/`。製品の統合検証は未実施。

## 実装と検証

- 依存 Mod の仕様は対象バージョンの公式ソースまたは実際の JAR/data で確認する。
- 解析物・補助スクリプトは Git 管理外の `.tmp/` に置く。
- 製品コードの途中確認は `./gradlew compileJava`、完了確認は `./gradlew build`。
  JSON は構文を検証し、data 変更には参照先と個数・IDの整合性確認を加える。
- 必須依存、公開ID、検証手順、実装済みの挙動を変更した場合は本書も更新する。
  仕様の採用やビルド成功だけを実ゲームの動作確認とみなさない。
