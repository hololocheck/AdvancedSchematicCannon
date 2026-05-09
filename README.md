# Create: Advanced Schematic Cannon

<iframe width="560" height="315" src="https://www.youtube-nocookie.com/embed/ERlLhXIVe5c" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowfullscreen></iframe>
*(Click the image above to watch the showcase video on YouTube! / 画像をクリックしてYouTubeの紹介動画をご覧ください！)*

### 🚀 The Ultimate Endgame Builder
Tired of the slow building speed of the vanilla Schematicannon? Exhausted by the inefficiency of moving items from your ME network to adjacent chests?

**Create: Advanced Schematic Cannon** completely destroys the bottlenecks of large-scale building. By integrating Create and Applied Energistics 2 — with optional ProjectE support — this mod introduces a high-speed, fully optimized, and incredibly smart endgame Schematicannon powered by FE energy.

![GUI](https://cdn.modrinth.com/data/cached_images/88bda77852a13dfa31643fe90b045e1e0c2c4932.png)

### ✨ Key Features
* ⚡ **Insane Speed & Extreme Server Optimization:** Adjust building speed up to a mind-blowing **256 blocks per tick**. Network NBT sync is throttled, dual-packet sending is eliminated, and `UPDATE_KNOWN_SHAPE` prevents cascading block updates. Play at maximum speed with minimal ping impact!
* 🔧 **Two Cannon Variants:** Choose between the **Enhanced Schematic Cannon** (always available, no EMC required) and the **EMC Schematic Cannon** (registered automatically when ProjectE is installed) — both share the same powerful BlockEntity and GUI.
* 🤖 **AE2 Auto-Crafting:** When a block is missing from your ME network, the cannon automatically submits a craft request to your AE2 crafting CPUs and waits for delivery. Per-cannon request tracking prevents duplicate jobs and CPU storms.
* 🔌 **AE2 Cable Power Input:** Connect AE2 energy cables directly — the cannon converts AE→FE at up to 10,000 FE/tick into its own buffer. No need for a separate power line.
* 💾 **Direct AE2 Network Access:** Pulls items directly from your ME Network. Features a flexible storage toggle ("AE2 & Chests", "AE2 Only", or "Chests Only").
* 🌌 **Optional ProjectE Integration (On-Demand Transmutation):** With ProjectE installed, toggle "EMC Mode" on the EMC variant to automatically consume your EMC to synthesize missing blocks on the fly.
* 🏗️ **Advanced AE2 Part Placement & Auto-Rotation:** Fully supports placing AE2 cables and automatically rotates complex multipart blocks like ME Drives and Terminals when rotating the schematic.

### 🆕 v1.1.2 — Hardening & Polish Update
* 🛡️ **Multiplayer Hardening:** Owner UUID is locked while a job is running, preventing other players from hijacking the cannon by simply opening the GUI. Action and Settings packets now require ownership (OPs allowed).
* 🧰 **Packet Validation:** All client→server packets validate ordinals and clamp distance values to prevent crashes from malformed packets.
* 🔁 **AE2 Auto-Craft Robustness:** Pending-craft tracking now keys on cannon position so two cannons on the same ME network never block each other. Standalone retry interval extended from 1 s → 30 s to stop calculation storms when a recipe is permanently missing. Per-call simulation requester eliminates a shared-state race.
* 🧹 **Resource Leak Fix:** Pending craft entries are force-cleaned after 5 minutes if AE2 fails to report job completion.
* 🖱️ **GUI Polish:** Tab widgets no longer swallow clicks in dead zones. Speed slider only sends a packet on release (no more 60 pkt/s while dragging) and uses a unified X offset for click/drag (1 px snap fixed).
* ⏱️ **Settings Sync:** `syncCooldown` extended 10 → 30 ticks and dragging the speed slider no longer gets overwritten by stale server values.
* 🔧 **NeoForge-Future-Proof Energy:** Internal energy consumption no longer relies on reflecting `EnergyStorage.energy`; a small subclass exposes a safe `consumeInternal(int)` instead.
* 🎨 **Enhanced Cannon Visuals:** The Enhanced variant now has its own GUI background (no EMC fuel slot icon) and barrel front texture, switched dynamically by block type. The EMC variant is unchanged.
* 📝 **Item Name on Missing Block:** The "Missing block" indicator in the GUI now shows the item display name next to the icon.

### 🆕 v1.1.0 — Gen2 GUI Update
* 🖥️ **Gen2 GUI:** Complete GUI overhaul with expandable **Settings Tab**, **Speed Tab**, and **Information Tab**. All buttons feature dedicated textures with hover effects. Block list (4 cols × 13 rows) with mouse wheel scrolling and EMC icon overlay.
* 📐 **Filler Mode (7 Patterns):** Switch to Filler Mode by inserting a Range Board. Build with **Fill**, **Wall**, **Tower**, **Box**, **Circle Wall**, **Complete Erase**, and **Removal** patterns.
* 🪄 **Air Placement Wand:** A new FE-powered tool (400,000 FE capacity) that places transparent Frame Blocks in mid-air. Adjust distance from 1–15 blocks with Shift+Scroll. Bulk-remove all placed blocks with Shift+Right-click.
* 📋 **Range Board:** A new 2-point range selection tool with 3 modes (Normal / Edit Pos1 / Edit Pos2). Raycast up to 64 blocks. Used with Filler Mode and Removal Mode.
* 🧱 **Frame Block:** A new transparent scaffolding block — instant break, light-transmitting, supports torches and redstone placement.
* 🔄 **Enhanced Removal Mode:** EMC Conversion ON converts EMC-valued blocks to EMC and routes non-EMC items to storage. Auto-pauses when storage is full. Block list populates instantly on Range Board insertion.
* 👁️ **Preview Display:** Toggle range frame preview in both Schematic and Filler modes. Instant response with no delay.
* 🎯 **Cannon Animation:** Smooth target-tracking barrel animation using exponential decay interpolation, frame-rate independent.
* 📖 **JEI Integration:** All recipes are now viewable in JEI.

### 🛠️ Other QoL Features
* **Top-Down Air Replacement:** Load an air-filled schematic to turn the cannon into a precision eraser. It deletes blocks from top to bottom, avoiding falling block lag and lighting calculation spikes.
* **Schematic Reuse Toggle:** Choose whether to consume the schematic upon completion or keep it for mass production.
* **Block Entity Protection:** Option to skip overwriting chests, furnaces, and other block entities.
* **Skip Missing Blocks:** Continue placement even when specific blocks are out of stock.
* **Range Volume Limit:** The Filler removal scan respects a 50,000-block volume cap to prevent server-tick spikes on huge ranges.

### 📦 Dependencies & Recommendations
* **Create** 6.0+ (Required)
* **Applied Energistics 2** 19.0+ (Optional / Highly Recommended — required for AE2 auto-crafting & cable power)
* **ProjectE** 1.0+ (Optional — enables the EMC Schematic Cannon variant and on-demand EMC transmutation)
* **JEI** 19.27+ (Optional / Recommended for recipe viewing)
* **FE (Forge Energy) Generating Mods** (Recommended): Since this cannon requires FE to operate, we highly recommend installing tech mods like **Mekanism** to generate and supply power. Alternatively, connect an AE2 energy cable for direct AE→FE conversion.

### 💖 Credits & Acknowledgements
Massive thanks to the developers of **Create**, **Applied Energistics 2**, and **ProjectE**. This addon would not exist without their phenomenal mods and foundational code. All respect goes to their respective teams.

### 📜 License
This project is licensed under the **MIT License**.

---

# 日本語版 (Japanese)

### 🚀 概要
本家Createの概略図砲の遅さにウンザリしていませんか？MEネットワークからチェストへアイテムを移し替える作業に疲れていませんか？
**Create: Advanced Schematic Cannon** は、巨大建築における物流と速度のボトルネックを完全に破壊する、FEエネルギー駆動のエンドコンテンツ向け究極概略図砲を追加します。CreateとApplied Energistics 2を統合し、ProjectEは任意導入に対応しました。

### ✨ 主な機能
* ⚡ **秒間5000ブロックの爆速 ＆ 極限のサーバー最適化:** 最大 **256ブロック/tick** という異次元のスピード。通信NBTの間引き、不要なパケット送信の排除、連鎖的なブロック更新を防ぐ仕様により、最高速度でもPingやFPSへの影響を最小限に抑えます。
* 🔧 **2種類の概略図砲:** 常に利用可能な **強化型概略図砲** と、ProjectE導入時に追加される **EMC概略図砲** の2種類を選択可能。BlockEntityとGUIは共通設計です。
* 🤖 **AE2自動クラフト連携:** MEネットワークに不足ブロックがある場合、自動的にAE2のクラフトCPUへジョブを発行して完成を待ちます。キャノン位置ごとのリクエスト追跡で重複ジョブやCPU過負荷を防止。
* 🔌 **AE2ケーブルからのFE給電:** AE2エネルギーケーブルを直接接続するだけで、AE→FE変換(最大10,000 FE/tick)を行いキャノン内部バッファに供給。別の電力線は不要です。
* 💾 **AE2ネットワーク直結:** MEネットワークから直接アイテムを引き出します。「AE2+チェスト」「AE2のみ」「チェストのみ」の柔軟な切り替え機能を搭載。
* 🌌 **EMC錬成（ProjectE任意）:** ProjectE導入時、EMC概略図砲の「EMCモード」をONにすると、足りないブロックをEMCで錬成しながら建築を続行できます。
* 🏗️ **AE2ケーブル設置 ＆ 完璧な回転対応:** 本家では不可能なAE2ケーブル設置に対応。概略図の回転時、MEターミナルやドライブなどの複雑なブロックの向きも自動計算して配置します。

### 🆕 v1.1.2 — 堅牢化＆ポリッシュアップデート
* 🛡️ **マルチプレイ堅牢化:** 稼働中はオーナーUUIDをロックし、他プレイヤーがGUIを開くだけで所有権を奪われる挙動を修正。アクション/設定パケットも所有者のみ受理（OPは許可）。
* 🧰 **パケット検証:** 全クライアント→サーバーパケットで ordinal の範囲チェック、距離値の clamp を追加。不正パケットによるクラッシュを防止。
* 🔁 **AE2自動クラフトの堅牢化:** Pending Craft キーにキャノン位置を含めるよう変更。同一ME網に2台のキャノンがある場合の相互干渉を解消。レシピ未登録時のリトライ間隔を1秒→30秒に延長してCPU過負荷を防止。シミュレーション用Requesterを per-call 化して共有 state レースを排除。
* 🧹 **リソースリーク対策:** AE2側からジョブ完了通知が来ない場合でも、5分タイムアウトでPending Craft エントリを強制クリーンアップ。
* 🖱️ **GUI改善:** タブウィジェットの空白部分でクリックを吸わないよう修正。スピードスライダーはリリース時のみ送信(ドラッグ中の60pkt/s送信を解消)、クリック/ドラッグ判定の1pxずれも修正。
* ⏱️ **設定同期:** `syncCooldown` を10→30 tickに延長、スライダードラッグ中はサーバー値で上書きしないように修正(値が戻るUXバグ解消)。
* 🔧 **NeoForge将来互換:** 内部エネルギー消費の `EnergyStorage.energy` リフレクションを廃止。サブクラスで安全に `consumeInternal(int)` を提供する設計に変更。
* 🎨 **強化型砲の専用ビジュアル:** 強化型概略図砲に独自のGUI背景(EMC燃料スロットアイコン無し)と砲身前面テクスチャを追加。ブロック種別で動的切替。EMC型は無変更。
* 📝 **不足アイテム名表示:** GUIの「不足ブロック」表示にアイコンの隣にアイテム名を併記するよう改善。

### 🆕 v1.1.0 — Gen2 GUIアップデート
* 🖥️ **第2世代GUI:** GUIを全面リニューアル。展開式の**設定タブ**・**スピードタブ**・**情報タブ**を搭載。全ボタンにホバーエフェクト付きの専用テクスチャを採用。ブロックリスト（4列×13行）はマウスホイールスクロール対応、EMCアイコンオーバーレイ表示。
* 📐 **フィラーモード（7パターン）:** 範囲指定ボードを挿入してフィラーモードに切替。**埋め立て**・**壁**・**タワー**・**箱**・**円壁**・**完全消去**・**撤去**の7種類の建築パターンを実行可能。
* 🪄 **空中設置杖（新アイテム）:** FEエネルギー駆動（400,000 FE容量）の空中設置ツール。透明なフレームブロックを空中に設置可能。Shift+スクロールで設置距離を1〜15ブロックで調整。Shift+右クリックで設置した全ブロックを一括撤去。
* 📋 **範囲指定ボード（新アイテム）:** 2点を指定して3D範囲を設定するツール。通常 / Pos1編集 / Pos2編集の3モード搭載。最大64ブロック先までレイキャスト対応。フィラーモード・撤去モードで使用。
* 🧱 **フレームブロック（新ブロック）:** 透明な足場ブロック。素手で即破壊、光透過あり。松明やレッドストーンの設置が可能。
* 🔄 **撤去モード強化:** EMC変換ON時はEMC値を持つブロックをEMCに変換、持たないブロックはストレージへ自動搬入。ストレージ満杯で自動一時停止。範囲指定ボード挿入時に即座にブロックリスト表示。
* 👁️ **プレビュー表示:** 概略図モード・フィラーモード両方で範囲枠のプレビュー表示/非表示を切替可能。遅延なしの即時反映。
* 🎯 **大砲アニメーション:** ターゲットブロックへのスムーズな追従アニメーション。指数減衰補間によるフレームレート非依存の滑らかな動き。
* 📖 **JEIレシピ表示対応:** 全レシピがJEIから確認可能。

### 🛠️ その他の便利機能
* **上からの空気置換モード:** 砂の落下や光源バグによる負荷を防ぐため、現実の解体工事のように「上から下へ」処理するスマートな範囲指定イレイザー機能。
* **概略図の再利用トグル:** 建築完了時に概略図を消費するか残すかを選択可能。
* **ブロックエンティティ保護:** チェスト・かまど等のブロックエンティティを上書きしないオプション。
* **不足ブロックスキップ:** 在庫切れのブロックをスキップして続行。
* **撤去スキャン体積制限:** 50,000ブロックを超える巨大範囲のスキャンを抑止し、サーバtickスパイクを防止。

### 📦 前提Mod & 推奨環境 (Dependencies)
* **Create** 6.0+ (必須)
* **Applied Energistics 2** 19.0+ (任意 / 強く推奨 — AE2自動クラフトとケーブル給電に必要)
* **ProjectE** 1.0+ (任意 — EMC概略図砲の追加とEMC錬成機能を有効化)
* **JEI** 19.27+ (任意 / レシピ確認に推奨)
* **FEを生成できるMod** (推奨): 本Modの稼働にはFEエネルギーを使用するため、**Mekanism** などの電気を生み出せる工業Modを併せて導入することを強くお勧めします。AE2エネルギーケーブルを直接接続してAE→FE変換を行うことも可能です。

### 💖 クレジット (Credits)
素晴らしい前提Modである **Create**・**Applied Energistics 2**・**ProjectE** の開発チームに心から感謝を申し上げます。本Modは彼らの偉大な功績とコード基盤の上に成り立っています。

### 📜 ライセンス (License)
本Modは **MITライセンス** の下で公開されています。

---

### 📌 Author / 作者
**BelugaLab**

### 🔢 Current Version / 現行バージョン
**1.1.2** (Minecraft 1.21.1 / NeoForge 21.1.168+)
