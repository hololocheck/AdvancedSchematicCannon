# Advanced Schematic Cannon — Modrinth Description

**Author:** BelugaLab
**Target:** Minecraft 1.21.1 / NeoForge 21.1.168+
**License:** MIT

[English](#english) | [日本語](#日本語)

---

## English

### Overview
**Advanced Schematic Cannon** is a NeoForge add-on for Create that obliterates the bottlenecks of large-scale building. The vanilla Schematicannon is slow and forces you to shuffle items between your ME network and adjacent chests. This mod fixes both problems with a high-speed, FE-powered cannon that talks directly to Applied Energistics 2 and — when ProjectE is installed — synthesizes missing blocks on demand using EMC.

### 🚀 The Pitch

- **Up to 256 blocks/tick.** A texture-based slider lets you push the build speed from 1 to 256 blocks per tick. NBT sync is throttled, duplicate packets are eliminated, and `UPDATE_KNOWN_SHAPE` prevents cascading block updates, so even at maximum speed the server tick stays responsive.
- **Two cannon variants.** The **Enhanced Schematic Cannon** is always available. The **EMC Schematic Cannon** is auto-registered when ProjectE is installed and adds the EMC fuel slot, EMC transmutation, and EMC-conversion-on-removal. Both share the same powerful BlockEntity and GUI.
- **AE2 ME network integration.** Items are pulled directly from your ME network — no need to copy your storage into adjacent chests. A storage mode toggle lets you choose AE2 + Chest, AE2 Only, or Chest Only.
- **AE2 auto-crafting for missing blocks.** When the cannon needs a block that isn't in storage, it submits a craft request to your AE2 crafting CPUs (with per-cannon pending tracking so two cannons on the same network never block each other) and resumes once delivery arrives.
- **AE2 cable power input.** Connect an AE2 energy cable directly — the cannon converts AE → FE at up to 10,000 FE/tick into its internal buffer. No separate power line required.
- **ProjectE EMC transmutation (optional).** With ProjectE installed, toggle "EMC Mode" on the EMC variant and the cannon will synthesize missing blocks from your EMC balance on the fly.
- **AE2 cable placement & multipart auto-rotation.** Unlike the vanilla Schematicannon, it correctly places AE2 cables and rotates complex multipart blocks (ME Drives, Terminals, etc.) when the schematic is rotated.

### ✨ Tools & Building Aids

| Item / Block | Role |
|---|---|
| **Enhanced Schematic Cannon** | The core builder. Always available. 100,000 FE buffer, 500 FE per placed block, 1–256 blocks/tick. |
| **EMC Schematic Cannon** | Added only when ProjectE is installed. Same features as Enhanced, plus EMC fuel/transmutation/conversion. |
| **Air Placement Wand** | An FE-powered wand (400,000 FE) that places transparent Frame Blocks in mid-air. Adjust distance 1–15 blocks with Shift+Scroll. Shift+Right-click bulk-removes everything you placed. |
| **Range Board** | A two-point 3D-range selector with three modes (Normal / Edit Pos1 / Edit Pos2). Raycast up to 64 blocks. Used with Filler Mode and Removal Mode. |
| **Frame Block** | A transparent scaffolding block — instant break, fully light-transmitting, supports torches and redstone. Placed by the Air Placement Wand. |

### 🧰 Filler Mode (7 Patterns)

Insert a Range Board to switch the cannon into Filler Mode. Choose from:

- **Fill** — fill air blocks inside the range with the chosen block.
- **Complete Erase** — replace every block in the range with air.
- **Removal** — remove blocks and either convert them to EMC (with ProjectE) or route them to storage.
- **Wall** — only the outer walls of the range.
- **Tower** — pillars within the range.
- **Box** — outer shell (6 faces) of the range.
- **Circle Wall** — cylindrical wall inside the range.

Removal mode automatically pauses when the destination storage is full.

### 🛡️ Built for Multiplayer

- **Owner UUID lock** — once a job is running, only the player who started it can change settings or stop the cannon (OPs are allowed). Opening the GUI no longer hijacks ownership or redirects EMC/fuel costs to a different player.
- **Packet ownership & validation** — every client→server packet checks ownership, validates enum ordinals, and clamps numeric inputs. Malformed or malicious packets are dropped instead of crashing the server.
- **AE2 auto-craft hardening** — per-cannon pending tracking, a 30 s minimum retry interval to prevent calculation storms, per-call simulation requesters to avoid shared-state races, and a 5-minute force cleanup so memory is bounded even if AE2 misses a job-completion callback.

### 🛠️ Other Quality of Life

- **Top-down air replacement.** Load an air-filled schematic to turn the cannon into a precision eraser. It removes blocks from top to bottom, avoiding falling-block lag and lighting recalculation spikes.
- **Schematic reuse toggle.** Choose whether to consume the schematic upon completion or keep it for mass production.
- **Block entity protection.** Optionally skip overwriting chests, furnaces, and other block entities.
- **Skip missing blocks.** Continue placement even when a specific block is out of stock.
- **Range volume cap.** Filler removal scans respect a 50,000-block volume limit to prevent server-tick spikes on huge ranges.
- **JEI integration.** All recipes are viewable in JEI.

### 📦 Dependencies & Recommended Mods

| Mod | Version | Required |
|-----|---------|----------|
| **Create** | 6.0+ | ✅ Required |
| **NeoForge** | 21.1.168+ | ✅ Required |
| **Applied Energistics 2** | 19.0+ | ⚙️ Optional (highly recommended — needed for AE2 auto-craft & cable power) |
| **ProjectE** | 1.0+ | ⚙️ Optional (adds the EMC Schematic Cannon variant) |
| **JEI** | 19.27+ | ⚙️ Optional (recipe viewing) |
| **FE-generating tech mod** | — | 🔋 Recommended (Mekanism, Mekanism: Additions, etc. — the cannon needs FE to operate, or use an AE2 energy cable) |

### 💡 Quick Start

1. Craft an **Enhanced Schematic Cannon** (or an **EMC Schematic Cannon** if you have ProjectE).
2. Provide it with FE — either a tech-mod power cable or an AE2 energy cable.
3. Optionally connect an AE2 ME cable to its side to pull items directly from storage.
4. Drop a Create schematic into the schematic slot and press the green play button.
5. Use the right-side **Speed Tab** to push the build speed all the way to 256 blocks/tick if your server can handle it.
6. To do filler/erase/wall-building work, insert a **Range Board** instead of a schematic and pick a pattern from the Mode tab.

### 💖 Credits

Huge thanks to the developers of **Create**, **Applied Energistics 2**, and **ProjectE**. This add-on is built on top of their foundational work.

### 📜 License

Released under the **MIT License**. See [LICENSE](LICENSE) for full text.

---

## 日本語

### 概要
**Advanced Schematic Cannon** は、Create の概略図キャノンの遅さと、ME ネットワークからチェストへアイテムを移し替えるという物流ボトルネックを一掃する NeoForge アドオンです。FE 駆動の高速キャノンが Applied Energistics 2 の ME ネットワークに直接アクセスし、ProjectE 導入時には EMC によるブロック錬成にも対応します。

### 🚀 何ができるのか

- **最大 256 blocks/tick の爆速建築。** テクスチャベースのスライダーで設置速度を 1〜256 blocks/tick まで自由に調整。通信 NBT のスロットリング、重複パケット排除、`UPDATE_KNOWN_SHAPE` による連鎖更新抑止により、最高速度でもサーバ tick が破綻しません。
- **2 種類のキャノン。** **強化型概略図砲** は常時利用可能。**EMC 概略図砲** は ProjectE 導入時のみ自動登録され、EMC 燃料スロット・EMC 錬成・撤去時の EMC 変換に対応します。BlockEntity と GUI は共通設計。
- **AE2 ME ネットワーク直結。** チェストを経由せず、ME ネットワークから直接アイテムを引き出します。ストレージモードは「AE2+チェスト」「AE2 のみ」「チェストのみ」から選択可能。
- **不足ブロックの AE2 自動クラフト。** ストレージに無いブロックは AE2 のクラフト CPU にジョブを発注。キャノン位置ごとに pending を追跡するため、同一 ME 網に複数キャノンがあっても干渉しません。
- **AE2 ケーブルからの FE 給電。** AE2 エネルギーケーブルを直接接続するだけで、AE → FE 変換(最大 10,000 FE/tick)を行ってキャノン内部バッファに供給。別電源不要です。
- **ProjectE EMC 錬成(任意)。** ProjectE 導入時、EMC 概略図砲の「EMC モード」を ON にすると、不足ブロックを EMC で錬成しながら建築を続行できます。
- **AE2 ケーブル設置 ＆ 完璧な回転対応。** 本家では不可能な AE2 ケーブル設置に対応。概略図回転時、ME ターミナルやドライブなどの複雑なマルチパートブロックも自動で正しい向きに配置します。

### ✨ 追加アイテム・ブロック

| アイテム / ブロック | 役割 |
|---|---|
| **強化型概略図砲** | 中核となる建築マシン。常時利用可能。100,000 FE バッファ、設置コスト 500 FE/ブロック、1〜256 blocks/tick |
| **EMC 概略図砲** | ProjectE 導入時のみ追加。強化型の全機能 + EMC 燃料・錬成・変換 |
| **空中設置杖** | FE 駆動(400,000 FE)で透明なフレームブロックを空中に設置。Shift+スクロールで距離調整(1〜15 ブロック)。Shift+右クリックで設置済み全ブロックを一括撤去 |
| **範囲指定ボード** | 2 点指定で 3D 範囲を設定するツール。3 モード(通常 / Pos1 編集 / Pos2 編集)。最大 64 ブロックのレイキャスト対応。フィラーモード・撤去モードで使用 |
| **フレームブロック** | 透明な足場ブロック。素手で即破壊、光透過あり、松明・レッドストーン設置可能。空中設置杖で配置 |

### 🧰 フィラーモード(7 パターン)

範囲指定ボードを挿入するとフィラーモードに切替わり、以下のパターンを実行できます:

- **埋め立て** — 範囲内の空気ブロックを指定ブロックで埋める
- **完全消去** — 範囲内の全ブロックを空気に置換
- **撤去** — 範囲内のブロックを撤去し、EMC 変換(ProjectE 導入時)またはストレージへ搬入
- **壁** — 範囲の外壁のみを作成
- **タワー** — 範囲内に柱を作成
- **箱** — 範囲の外殻(6 面)を作成
- **円壁** — 範囲内に円筒形の壁を作成

撤去モードはストレージ満杯時に自動一時停止します。

### 🛡️ マルチプレイ対応の堅牢化

- **オーナー UUID ロック** — ジョブ稼働中は開始者のみが設定変更・停止操作を可能(OP は許可)。他プレイヤーが GUI を開いただけで所有権を奪う/EMC・燃料が別人から引き落とされる問題は発生しません。
- **パケット所有権 & 入力検証** — 全クライアント→サーバーパケットで所有者確認、enum ordinal の範囲チェック、数値入力の clamp を実装。改ざんパケットでクラッシュしません。
- **AE2 自動クラフトの堅牢化** — キャノン位置ごとの pending 追跡、計算ストーム防止の 30 秒最低リトライ間隔、共有 state レース防止の per-call シミュレーション requester、AE2 のジョブ完了通知欠落に備えた 5 分強制クリーンアップ。

### 🛠️ その他の便利機能

- **上からの空気置換モード** — 空気のみの概略図を読み込ませてキャノンを精密イレイザーに変身。砂の落下や光源再計算の負荷を避けるため上から下へ処理します。
- **概略図再利用トグル** — 完了時に概略図を消費するかどうかを選択可能。
- **ブロックエンティティ保護** — チェスト・かまど等の BE を上書きしない保護オプション。
- **不足ブロックスキップ** — 在庫切れブロックを飛ばして続行。
- **範囲体積上限** — フィラー撤去スキャンは 50,000 ブロックの体積上限を持ち、巨大範囲によるサーバ tick スパイクを防止。
- **JEI 対応** — 全レシピが JEI から確認可能。

### 📦 前提 Mod & 推奨環境

| Mod | バージョン | 必須 |
|-----|-----------|------|
| **Create** | 6.0+ | ✅ 必須 |
| **NeoForge** | 21.1.168+ | ✅ 必須 |
| **Applied Energistics 2** | 19.0+ | ⚙️ 任意(強く推奨 — AE2 自動クラフトとケーブル給電に必要) |
| **ProjectE** | 1.0+ | ⚙️ 任意(EMC 概略図砲を追加) |
| **JEI** | 19.27+ | ⚙️ 任意(レシピ確認) |
| **FE 生成 Mod** | — | 🔋 推奨(Mekanism / Mekanism: Additions など — キャノンの稼働に FE が必要。代替として AE2 エネルギーケーブルでも可) |

### 💡 クイックスタート

1. **強化型概略図砲**(または ProjectE 導入済みなら **EMC 概略図砲**)をクラフト。
2. FE を供給。工業 Mod の電線、または AE2 エネルギーケーブルを接続。
3. 任意で AE2 ME ケーブルを側面に繋いで ME ストレージから直接搬入。
4. Create の概略図を概略図スロットに入れて緑色の再生ボタンを押す。
5. 右側の **スピードタブ** で速度をスライダーで調整(サーバが耐えられるなら 256 blocks/tick まで可能)。
6. フィラー/消去/壁作成などを行いたい場合は、概略図の代わりに **範囲指定ボード** を挿入し、モードタブからパターンを選択。

### 💖 クレジット

素晴らしい前提 Mod である **Create**・**Applied Energistics 2**・**ProjectE** の開発チームに心から感謝を申し上げます。本 Mod は彼らの偉大な功績の上に成り立っています。

### 📜 ライセンス

**MIT ライセンス** の下で公開しています。詳細は [LICENSE](LICENSE) を参照してください。
