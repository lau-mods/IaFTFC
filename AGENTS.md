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
- 固定版は Minecraft 1.21.1、NeoForge 21.1.248、TFC 4.2.12、IaF CE 2.1.4。版の変更は `PROJECT.md` に基づく検証を伴う。
- `./gradlew runData` をレシピ、Loot、PNGを含む生成物の唯一の正式 generator とし、出力を `src/generated/resources` に置く。補助的な `tools/` と `scripts/` は作らない。
- 元 Mod のデータを上書きする場合、`src/test/resources/iaftfc/gametest/override_manifest.json` の許可リストへ対象 ID を追加し、GameTestで実際の上書きと照合する。
