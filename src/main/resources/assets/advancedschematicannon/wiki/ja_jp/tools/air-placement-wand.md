---
title: 空中設置杖
id: tools/air-placement-wand
tags: [tool, item]
---

# 空中設置杖

```embed:item id=advancedschematicannon:air_placement_wand size=48 label=true
```

FE を消費して**フレームブロック**を空中に設置する杖です。足場や当たり判定の仮置きに使います。

[[TOC]]

## 操作

| 操作 | 動作 |
|---|---|
| **右クリック** | 視線の先にフレームブロックを設置 |
| **Shift + 右クリック** | この杖で設置した全ブロックを撤去 |
| **Shift + ホイール** | 設置距離を調整 (**1〜15 ブロック**) |
| **Shift + ホイール押し込み** | 設置距離を既定値に戻す |

杖を持っている間、設置先には**白い枠**が表示されます。設置先が空気でない場合は枠が出ません。

## エネルギー

残量はアイテムの説明 (tooltip) に `エネルギー: n / max FE` として出ます。
足りない場合は「エネルギーが不足しています」と表示され、設置できません。

## フレームブロック

```embed:item id=advancedschematicannon:frame_block size=48 label=true
```

杖が置く専用の足場ブロックです。**Shift + 右クリックで一括撤去**できるので、
建築の途中で足場として置いて、終わったらまとめて消せます。
