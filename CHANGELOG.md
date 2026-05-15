# Changelog

## v1.1.2.1 — 致命バグ修正＆ビジュアル整合パッチ

### バグ修正

- **ProjectE 非導入環境でのツール採掘速度バグ修正** (PR #4 / @nekorobi-0): `data/minecraft/tags/block/mineable/pickaxe.json` が条件付き登録ブロック `advancedschematicannon:emc_schematic_cannon` を必須エントリで参照していたため、ProjectE 不在時に `minecraft:mineable/pickaxe` タグ全体が破壊され、バニラ含む全ブロックの tier 判定不能(ダイヤピッケル等が素手と同速)になっていた問題を修正。`{ "id": "...", "required": false }` でラップし、未登録時は警告ログのみで安全にスキップするように変更。
- **強化型概略図砲の手持ちテクスチャ修正**: アイテムモデルが EMC 型の `block/emc_schematic_cannon/item.json` を parent としており、texture key `#9` が EMC 型の砲身前面テクスチャを指していたため、強化型を手に持つと EMC 型の見た目になる問題を修正。子モデル側で `#9` を `cannon_barrel_front_enhanced` にオーバーライドする方式に変更(本体形状や他テクスチャは共有を維持)。

### メタ情報

- **mod_version**: 1.1.2 → **1.1.2.1** (`gradle.properties`)
- **ゲーム内 Mod 欄表示**: `neoforge.mods.toml` の `version` ハードコード値も 1.1.2.1 に同期。
- **README デザイン刷新**: SpatialAudioSystem と同一のレイアウトに統一(中央寄せロゴ、シールド行、JP/EN 切替、Spec/Detail テーブル形式)。
- **インデント整形**: PR #4 の修正でずれていた `pickaxe.json` のインデントを 2 スペースに統一(機能影響なし)。

---

## v1.1.2 — 堅牢化＆ポリッシュアップデート

### 新機能 / 改善

- **EMC概略図砲のEMCトグル復活**: `Menu#supportsEmc()` が ContainerData 同期前に false を返し、Screen の `init()` 時点で EMC ON/OFF ボタンが生成されない不具合を修正(BlockEntity 直接委譲に変更)。
- **不足アイテム名表示**: GUI の「不足ブロック」表示にアイコンの隣にローカライズされたアイテム名を併記。
- **強化型概略図砲の専用ビジュアル**: 強化型に独自 GUI 背景(EMC燃料スロットアイコン無し)と砲身前面テクスチャを追加し、ブロック種別で動的切替。EMC型は無変更。

### マルチプレイ堅牢化

- **オーナー UUID ハイジャック修正**: `createMenu` で他プレイヤーが GUI を開くだけでオーナーが上書きされる問題を修正。`IDLE/FINISHED/ERROR` 時または `ownerUUID == null` の時のみ更新。
- **パケット所有権チェック**: `CannonActionPacket` / `CannonSettingsPacket` でオーナー以外からの操作を拒否(OPは `permissions(2)` で許可)。
- **パケット入力検証**: `Action.values()[i]` の範囲チェック、`RangeBoardEditPacket` の editMode を 0..2 に制限、`WandDistancePacket` の距離値を `Mth.clamp` で正規化。

### AE2 自動クラフト

- **キャノン位置を含む追跡**: `PendingCraftKey` に `cannonPos` を追加。同一 ME 網に複数キャノンがある場合の相互干渉を解消。
- **リクエストストーム対策**: レシピ未登録時のリトライ間隔を 1 秒 → 30 秒に延長。
- **共有ステートレース修正**: シミュレーション用 Requester を per-call 化し、`AE2GridNodeManager.simulationSource` の上書き競合を排除。
- **リソースリーク対策**: AE2 側からジョブ完了通知が来ない場合でも、5 分タイムアウトで PendingCraft を強制クリーンアップ。

### GUI / UX

- **タブクリック貫通修正**: `SettingsTabWidget` / `SpeedTabWidget` / `InformationTabWidget` の mouseClicked が無条件 true を返してしまい、空白部分のクリックで下層スロットに反応しない問題を修正。
- **スピードスライダーのspam解消**: ドラッグ中は描画のみ、リリース時に1回だけパケット送信に変更。クリック/ドラッグ判定の 1px ずれも統一。
- **設定同期改善**: `syncCooldown` を 10 → 30 tick に延長し、ドラッグ中は server 値で上書きしないように修正(値が戻る UX バグ解消)。
- **燃料消費ガード**: `consumeFuelItems` を `state == RUNNING` のときのみ動作するように制限。
- **fromNetwork フォールバック改善**: client BE 未到着時のダミー BE に `setLevel` をセットし、`stillValid()` 即 false で Screen が瞬時閉じる UX バグを回避。ProjectE 不在環境では `ENHANCED_CANNON_BLOCK` を fallback。

### 内部 / 将来互換

- **NeoForge 将来互換**: `EnergyStorage.energy` のリフレクションを廃止。サブクラス `InternalEnergyStorage` の `consumeInternal(int)` で安全に内部消費。
- **巨大範囲スパイク防止**: `scanRemoveRange` に `MAX_AIR_VOLUME_PLACEMENTS` (50,000) 体積上限を追加し、サーバ tick スパイクを防止。
- **Create バージョン制約**: `neoforge.mods.toml` の Create 依存範囲を `[0.5,)` → `[6.0,)` に厳格化(`AllDataComponents` API は Create 6.x 以上必須)。

### 作者・メタ情報

- **作者**: hololocheck → **BelugaLab** に変更。
- **mod_version**: 1.2.0 → **1.1.2**。

---

## v1.1.0 — Gen2 GUI アップデート

### 新機能

- **第2世代GUI（Gen2）への全面リニューアル**
  - 新しい背景テクスチャ `GUI_2Gen.png` (256×256) を採用
  - 全ボタンをプッシュ/ノプッシュ方式に変更（ホバー時にテクスチャが切り替わる）
  - エネルギーバー、プログレスバーを専用テクスチャで描画

- **設定タブ（右側展開式）**
  - 歯車アイコンのタブをクリックで展開/折りたたみ（5tickアニメーション）
  - 2×3グリッドに6つの設定ボタンを配置:
    - 固体ブロック置換しない / 固体を固体で置換 / 固体を任意で置換
    - 固体を空気で置換 / 不足ブロックスキップ / ブロックエンティティ保護
  - 選択中のオプションに緑インジケーター表示
  - ホバー時にツールチップ表示

- **スピードタブ（右側展開式）**
  - 稲妻アイコンのタブをクリックで展開
  - テクスチャベースのスライダー（トラック + ノブ）でドラッグ操作
  - 1〜256 blocks/tick の設置速度を調整可能
  - マウスホイールでの微調整にも対応
  - 設定タブ展開時は自動的に下にシフト

- **情報タブ（左側展開式）**
  - 左側に展開するパネルでmod説明テキストを表示
  - マウスホイールでスクロール可能
  - 右側タブとは独立して動作

- **タブ相互排他制御**
  - 設定タブとスピードタブは同時に開けない（片方を開くともう片方が閉じる）
  - 情報タブは独立して開閉可能

- **EMCトグルボタン**
  - EMC使用のON/OFFを切り替え（4種テクスチャ: on_normal/on_hover/off_normal/off_hover）

- **概略図再利用ボタン**
  - 概略図を消費せずに再利用するかのトグル（4種テクスチャ）

- **ストレージモード切替ボタン**
  - AE2+チェスト / AE2のみ / チェストのみ の3モード切替
  - AE2未接続時は自動的にチェストのみモードに固定

- **モード切替ボタン（UI）**
  - 概略図モード / フィラーモードの切替（26×26、将来実装予定）

- **EMC使用済みインジケーター**
  - ブロックリスト内のEMC値を持つブロックに `EMCused.png` オーバーレイを表示

- **ブロックリストスクロール**
  - 4列×13行のブロック一覧をマウスホイールでスクロール
  - ホバー時にブロック名・個数・EMC状態のツールチップ表示

### GUI変更

- **スロット位置の調整**
  - 燃料スロット: (88,129) → (87,124)
  - 概略図スロット: (123,129) → (123,124)
  - 出力スロット: (231,129) → (231,124)
  - プレイヤーインベントリ: (88,173) → (87,174)

- **情報パネルのレイアウト変更**
  - ステータス表示を (102,38) に移動
  - EMC使用量、進捗、残りブロック数、不足ブロック表示を統合

- **ContainerData拡張（6 → 10スロット）**
  - Index 6: ReplaceMode ordinal
  - Index 7: フラグビットフィールド (skipMissing, protectBE, useEmc, reuseSchematic)
  - Index 8: BlocksPerTick
  - Index 9: StorageMode ordinal

### ネットワーク

- **CannonSettingsPacket拡張**
  - `useEmc`, `blocksPerTick`, `reuseSchematic`, `storageMode` フィールド追加
  - ReplaceMode + StorageModeを1つのintにパック
  - blocksPerTick + reuseSchematicを1つのintにパック（StreamCodec 6パラメータ制限対応）

### テクスチャ

- 45種以上のGen2 GUIテクスチャを追加 (`textures/gui/gen2/`)
  - GUI背景、エネルギーバー、プログレスバー
  - 各ボタンのnormal/hoverテクスチャペア
  - タブアイコン（設定、スピード、情報）
  - タブ展開背景テクスチャ
  - スライダートラック・ノブ
  - EMC使用済みオーバーレイ
  - モード切替アイコン

---

## v1.0.1

### Bug Fixes

- **マルチプレイでAE2 ME倉庫からのアイテム抽出が動作しない問題を修正**
  - AE2グリッドノードの`isActive()`チェック（`powered=true`を要求）を削除し、グリッドが存在すればストレージアクセスを試みるように変更
  - マルチプレイ環境ではAE2の電力源がチャンク未ロードにより`powered=false`になり、抽出が常に失敗していた
  - キャノン自身のグリッドノードが利用不可の場合、隣接するAE2ケーブル/機器のグリッドを探索するフォールバック機構を追加

- **AE2グリッドノード初期化の信頼性向上**
  - `onLoad()`で初期化失敗時に`ae2Available`フラグを永久にfalseにしていた問題を修正
  - サーバーティックで1秒間隔・最大10回のリトライロジックを追加
  - `setExposedOnSides(EnumSet.allOf(Direction.class))`を追加し、全方向からのAE2ケーブル接続を明示的に許可

- **mod説明欄のバージョン表示が`0.0none`になる問題を修正**
  - `neoforge.mods.toml`の`${file.jarVersion}`（JARマニフェスト参照）を直接バージョン指定に変更

### Diagnostics

- AE2抽出失敗時のログをINFOレベルに変更し、サーバーコンソールで状態（powered/channels）を確認可能に
