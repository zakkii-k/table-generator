# テーブルデータ生成ツール (table-generator)

このツールは、JSON形式で定義されたテーブル構成に基づき、ランダムなテストデータを生成し、SQL INSERT文ファイルまたはXLSXファイルとして出力するJavaアプリケーションです。

---

## 目次

- [はじめに](#はじめに)
- [セットアップとビルド](#セットアップとビルド)
- [ツールの使い方](#ツールの使い方)
- [JSON設定ファイル (config.json) の記述方法](#json設定ファイル-configjson-の記述方法)

---

## はじめに

ECサイトのモックアップデータ生成を目的として作成されました。  
javafakerによる日本ロケールデータ生成や、Spring Securityと互換性のあるパスワードハッシュ化機能を含みます。

- リポジトリURL: [https://github.com/zakkii-k/table-generator.git](https://github.com/zakkii-k/table-generator.git)
- 依存関係管理: Maven
- Javaバージョン: 17以上を推奨

---

## セットアップとビルド

### 1. Git Clone

まず、リポジトリをローカルにクローンします。

```sh
git clone https://github.com/zakkii-k/table-generator.git
cd table-generator
```

### 2. Mavenの実行

pom.xml に従って依存関係の解決とビルドを行います。これにより、実行可能なJARファイルが target/ ディレクトリに生成されます。

```sh
# クリーンビルドとパッケージング
mvn clean package
```

【WindowsでのMaven実行について】

Windows環境でMavenを実行するには、事前にMavenをインストールし、環境変数 M2_HOME と Path の設定が必要です。設定が完了していれば、Linux/macOSと同様に上記コマンドを実行できます。

---

## ツールの使い方

ビルドが成功すると、target/data-generator-app-1.0-SNAPSHOT.jar というファイルが生成されます。これを java -jar コマンドで実行します。

### コマンドライン引数

| オプション         | 必須性 | 説明                                                                                   |
|--------------------|--------|----------------------------------------------------------------------------------------|
| -i (--inputPath)      | 必須   | テーブル定義JSONファイルへのパスを指定します。（例: src/main/resources/config.json）    |
| -q (--sql)         | 任意   | SQLのINSERT文を出力します。                                                            |
| -x (--xlsx)        | 任意   | XLSXファイルを出力します。（-qと-xは排他ではないため、両方指定可能です）               |
| -o (--output)      | 任意   | 出力先ディレクトリを指定します。（デフォルト: カレントディレクトリ .)                  |
| -s (--seed)        | 任意   | ランダムデータ生成のシード値（再現性確保のため）。（デフォルト: 1）                    |

### 実行例

1. SQLファイルとして出力（シード値100、カレントディレクトリ）

```sh
# StackOverflowErrorを避けるため、スタックサイズ(-Xss)を増やすことを推奨
java -jar target/data-generator-app-1.0-SNAPSHOT.jar -i src/main/resources/config.json -q -s 100
```

2. XLSXファイルとして出力（シード値デフォルト、出力先ディレクトリを指定）

```sh
java -jar target/data-generator-app-1.0-SNAPSHOT.jar -i src/main/resources/config.json -x -o ./output/data
```

---

## JSON設定ファイル (config.json) の記述方法

JSONファイルは、テーブル定義のオブジェクトの配列です。

### テーブル定義 (最上位オブジェクト)

| キー   | データ型 | 説明                                   |
|--------|----------|----------------------------------------|
| name   | String   | 生成するテーブル名（例: "CUSTOMER"）   |
| size   | Number   | 生成するデータ行数                     |
| data   | Array    | カラム定義のリスト                     |

### カラム定義 (data 配列内のオブジェクト)

各カラムは type に基づいて生成方法が決定されます。

| キー         | type が何の場合に必須か | データ型        | 説明                                                                                   |
|--------------|------------------------|----------------|----------------------------------------------------------------------------------------|
| columnName   | 常に必須               | String         | データベースのカラム名                                                                 |
| type         | 常に必須               | String         | データの生成方法: SERIAL, STRING, REGEX, FAKER, NUMBER, ARRAY, DATETIME のいずれか     |
| startFrom    | SERIAL の場合任意      | Number         | 連番の開始値（主キーの初期値）。(デフォルト: 1)                                        |
| format       | STRING の場合必須      | String         | 定数文字列、または主キーの値が入るプレースホルダー {i} を含む文字列。                  |
| pattern      | REGEX の場合必須       | String         | 正規表現。javafaker.regexify を使用してランダムな文字列を生成します。                   |
| generator    | FAKER の場合必須       | String         | Fakerモジュールとメソッドをドット区切りで指定（例: "name.fullName", "address.zipCode"）。引数がある場合は method('arg') の形式で指定します。（例: "bothify('?#?#@test.com')"） |
| min / max    | NUMBER の場合必須/任意 | Number         | 数値の生成範囲の下限/上限。                                                            |
| values       | ARRAY の場合必須       | Array (String) | 値の選択肢となるリスト。                                                               |
| isRandom     | ARRAY の場合任意       | Boolean        | true の場合ランダムに選択。false の場合シーケンシャルに選択。（デフォルト: false）      |
| minDate      | DATETIME の場合必須    | String         | 日時範囲の下限。フォーマットは yyyy-MM-dd HH:mm:ss。                                   |
| maxDate      | DATETIME の場合任意    | String         | 日時範囲の上限。（デフォルト: 実行時の現在日時）                                       |
| unique       | 任意                   | Boolean        | true の場合、生成された値がテーブル全体で一意であることを保証します。                   |
| isHashed     | STRING の場合任意      | String         | パスワードをハッシュ化し、そのハッシュ値を格納する新しいカラム名を指定します。Spring Security互換のBCryptを使用します。 |

サンプルの設定ファイルは[config.json](config.json)を参照．