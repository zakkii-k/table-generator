# テーブルデータ生成ツール (table-generator)

JSON形式で定義されたテーブル構成に基づき，ランダムなテストデータを生成し，SQL INSERT文ファイルまたはXLSXファイルとして出力するJavaアプリケーション．

---

## 目次

- [はじめに](#はじめに)
- [セットアップとビルド](#セットアップとビルド)
- [ツールの使い方](#ツールの使い方)
- [JSON設定ファイル (config.json) の記述方法](#json設定ファイル-configjson-の記述方法)

---

## はじめに

結合テストやその他ダミーデータのテーブルを要する際のデータ生成のために作成した．
正規表現や集合からのランダムな選択などに加え，日本人名の生成やSpring Securityと互換性のあるパスワードハッシュ化なども可能．

- リポジトリURL: [https://github.com/zakkii-k/table-generator.git](https://github.com/zakkii-k/table-generator.git)
- 依存関係管理: Maven
- Javaバージョン: 17以上を推奨

---

## セットアップとビルド

### 1. Git Clone

リポジトリをローカルにクローンする．

```sh
git clone https://github.com/zakkii-k/table-generator.git
cd table-generator
```

### 2. Mavenの実行

pom.xml に従って依存関係の解決とビルドを行う．これにより，実行可能なJARファイルが target/ ディレクトリに生成される．

```sh
# クリーンビルドとパッケージング
mvn clean package
```

【WindowsでのMaven実行について】

Windows環境でMavenを実行するには，事前にMavenをインストールし，環境変数 M2_HOME と Path の設定が必要になる．設定が完了していれば，Linux/macOSと同様に上記コマンドが実行可能．

【Eclipseでの実行について】
Mavenコマンドの代わりに，EclipseのファイルメニューからMavenプロジェクトをインポートすることで実行も可能．その際には実行時引数を設定する必要があるので，「実行の構成」から実行する．

---

## ツールの使い方

ビルドが成功すると，target/data-generator-app-1.0-SNAPSHOT.jar というファイルが生成される．これを java -jar コマンドで実行する．

### コマンドライン引数

| オプション         | 必須性 | 説明                                                                                   |
|--------------------|--------|----------------------------------------------------------------------------------------|
| -i (--inputPath)      | 必須   | テーブル定義JSONファイルへのパスを指定．（例: src/main/resources/config.json）    |
| -q (--sql)         | 任意   | SQLのINSERT文を出力．                                                            |
| -x (--xlsx)        | 任意   | XLSXファイルを出力．（-qと-xは排他ではないため，両方指定可能）               |
| -o (--output)      | 任意   | 出力先ディレクトリを指定．（デフォルト: カレントディレクトリ .)                  |
| -s (--seed)        | 任意   | ランダムデータ生成のシード値（再現性確保のため）．（デフォルト: 1）                    |

### 実行例

1. SQLファイルとして出力（シード値100，カレントディレクトリ）

```sh
java -jar target/data-generator-app-1.0-SNAPSHOT.jar -i src/main/resources/config.json -q -s 100
```

2. XLSXファイルとして出力（シード値デフォルト，出力先ディレクトリを指定）

```sh
java -jar target/data-generator-app-1.0-SNAPSHOT.jar -i src/main/resources/config.json -x -o ./output/data
```

---

## JSON設定ファイル (config.json) の記述方法

JSONファイルは，テーブル定義のオブジェクトの配列．

### テーブル定義 (最上位オブジェクト)

| キー   | データ型 | 説明                                   |
|--------|----------|----------------------------------------|
| name   | String   | 生成するテーブル名（例: "CUSTOMER"）   |
| size   | Number   | 生成するデータ行数                     |
| data   | Array    | カラム定義のリスト                     |

例:
```json
[
    {
        "name": "CUSTOMER",
        "size": 100,
        "data": [
            （この部分は次の節で説明）
        ]
    },
    {
        "name": "ITEM",
        "size": 10,
        "data": [
            （この部分は次の節で説明）
        ]
    }
]
```


### カラム定義 (data 配列内のオブジェクト)

各カラムは type に基づいて生成方法が決定される．

| キー         | type が何の場合に必須か | データ型        | 説明                                                                                   |
|--------------|------------------------|----------------|----------------------------------------------------------------------------------------|
| columnName   | 常に必須               | String         | データベースのカラム名                                                                 |
| type         | 常に必須               | String         | データの生成方法: SERIAL, STRING, REGEX, FAKER, NUMBER, ARRAY, DATETIME のいずれか     |
| startFrom    | SERIAL の場合任意      | Number         | 連番の開始値（主キーの初期値）．(デフォルト: 1)                                        |
| format       | STRING の場合必須      | String         | 定数文字列，または主キーの値が入るプレースホルダー {i} を含む文字列．                  |
| pattern      | REGEX の場合必須       | String         | 正規表現．javafaker.regexify を使用してランダムな文字列を生成．                   |
| generator    | FAKER の場合必須       | String         | Fakerモジュールとメソッドをドット区切りで指定（例: "name.fullName", "address.zipCode"）．引数がある場合は method('arg') の形式で指定．（例: "bothify('?#?#@test.com')"） |
| min / max    | NUMBER の場合必須/任意 | Number         | 数値の生成範囲の下限/上限．                                                            |
| values       | ARRAY の場合必須       | Array (String) | 値の選択肢となるリスト．                                                               |
| isRandom     | ARRAY の場合任意       | Boolean        | true の場合ランダムに選択．false の場合シーケンシャルに選択．（デフォルト: false）      |
| minDate      | DATETIME の場合必須    | String         | 日時範囲の下限．フォーマットは yyyy-MM-dd HH:mm:ss．                                   |
| maxDate      | DATETIME の場合任意    | String         | 日時範囲の上限．（デフォルト: 実行時の現在日時）                                       |
| unique       | 任意                   | Boolean        | true の場合，生成された値がテーブル全体で一意であることを保証する．                   |
| isHashed     | STRING の場合任意      | String         | パスワードをハッシュ化し，そのハッシュ値を格納する新しいカラム名を指定．Spring Security互換のBCryptを使用． |
| fkReference | 外部キーの場合必須 | String | 外部キーの参照先を"テーブル名.カラム名"で指定する． |

### typeについて
typeは以下の7種類存在
1. **SERIAL**
主キーとなるカラムに指定．主キーは数値となる．
`startFrom`と併用することで，開始する数値を指定可能．
例: 1000から1001, 1002,,,と割り振られる．
    ```json
    {
        "columnName": "customer_id",
        "type": "SERIAL",
        "startFrom": 1000
    }
    ```
    なお，もし主キーを`C1`のように文字列としたい場合は後述のSTRINGでプレースホルダーを用いると良い．

2. **STRING**
文字列をカラムに指定．文字列は定数と，プレースホルダーの2パターン可能．
定数を指定すると，全てのセルがその文字列となる．
例:
    ```json
    {
        "columnName": "sample",
        "type": "STRING",
        "format": "hoge"
    }
    ```
    一方，プレースホルダーを指定すると，そこに数値が代入される．
    代入される数値は，`startFrom`を指定するとその値から1ずつインクリメントされ，指定しない場合はtypeが`SERIAL`のidと同じになる．type SERIALが存在しない場合は1から開始する．
    例:`user_1`, `user_2`, ...というデータが生成される．
    ```json
    {
        "columnName": "sample",
        "type": "STRING",
        "format": "user_{i}"
    }
    ```
    例: `user_10`, `user_11`, ...というデータが生成される．
    ```json
    {
        "columnName": "sample",
        "type": "STRING",
        "format": "user_{i}",
        "startFrom": 10
    }
    ```
    もし1の`SERIAL`の例の記述が同一テーブルに含まれる場合，
    例: `user_1000`, `user_1001`, ...というデータが生成される．
    ```json
    {
        "columnName": "sample",
        "type": "STRING",
        "format": "user_{i}"
    }
    ```

3. **REGEX**
正規表現を満たすランダムなデータを生成．
正規表現については Qiitaなどを参照．
例: 7桁の数値をランダム生成
    ```json
    {
        "columnName": "zip_code",
        "type": "REGEX",
        "pattern": "\\d{7}"
    }
    ```

4. **FAKER**
fakerパッケージに用意された機能のみ利用可能．
現状確認したのは`name.fullName`と`address.zipCode`のみ．メールや電話番号も可能であるが，メールは日本人氏名とは相性が悪い．電話番号はハイフン位置がランダムになるため，正規表現を用いるのが良い（郵便番号も正規表現で良い）．
例: ランダムな日本人名を生成．
    ```json
    {
        "columnName": "full_name",
        "type": "FAKER",
        "generator": "name.fullName" 
    }
    ```

5. **NUMBER**
ランダムな整数を生成する．もし範囲を指定したい場合は，`min`や`max`により制限可能．
一方のみの指定も可能で，`min`のみであればその数以上のランダムな整数が，`max`のみであればその数以下のランダムな整数が，両方与えるとその範囲内のランダムな整数がそれぞれ生成される．
例: 0〜50のランダムな整数を生成
    ```json
    {
        "columnName": "stock",
        "type": "NUMBER",
        "min": 0,
        "max": 50
    }
    ```

6. **ARRAY**
複数のデータのうちいずれかを生成させたい時に使用する．
基本は配列の先頭要素から順に選択され，末尾要素が選択された後は再度先頭要素から順に選択されていく．
配列のデータからランダムに選択させたいときは`isRandom`を`true`に指定することで実現可能．
例: S, M, L, XL, S, M, L, XL, ...と順に出力．
    ```json
    {
        "columnName": "size",
        "type": "ARRAY",
        "values": ["S", "M", "L", "XL"],
        "isRandom": false
    }
    ```

7. **DATETIME**
ランダムな日時の指定が可能．
特定の期間のうちのいずれかの日時を指定したいときは，**NUMBER**と同じように，`minDate`と`maxDate`を指定する．

    ```json
    {
        "columnName": "last_login",
        "type": "DATETIME",
        "minDate": "2024-01-01 00:00:00"
    }
    ```

サンプルの設定ファイルは[config.json](config.json)を参照．