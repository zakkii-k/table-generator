package com.generator.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.generator.model.ColumnConfig;
import com.generator.model.TableConfig;
import com.github.javafaker.Faker; 
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * テーブル定義に基づいてランダムなデータを生成し、SQLまたはXLSXに出力するクラス。
 */
public class DataGenerator {

    private final Random random;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final Faker faker;

    // private final Map<String, Generex> generexCache = new HashMap<>();

    public DataGenerator(long seed) {
        // シード値を持つRandomインスタンスを使用
        this.random = new Random(seed);
        this.faker = new Faker(this.random);
        System.out.println("データ生成ツールが初期化されました。シード値: " + seed);
    }

    /**
     * JSON設定ファイルを読み込み、テーブル構成のリストを返します。
     * @param jsonFilePath JSON設定ファイルのパス
     * @return テーブル構成のリスト
     * @throws IOException JSON読み込み/パースエラーが発生した場合
     */
    public List<TableConfig> loadConfig(String jsonFilePath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        File jsonFile = new File(jsonFilePath);
        // リソースフォルダからのロードを試みる
        if (!jsonFile.exists()) {
            try {
                // クラスローダーを使用してリソースからファイルをロード (Mavenのresourcesフォルダ対応)
                return mapper.readValue(
                        getClass().getClassLoader().getResourceAsStream(jsonFilePath),
                        mapper.getTypeFactory().constructCollectionType(List.class, TableConfig.class)
                );
            } catch (Exception e) {
                // ファイルが存在しない場合はエラーをスロー
                throw new IOException("設定ファイルが見つかりません: " + jsonFilePath);
            }
        }
        return mapper.readValue(jsonFile, mapper.getTypeFactory().constructCollectionType(List.class, TableConfig.class));
    }


    /**
     * 全てのテーブルのデータを生成し、指定された形式で出力します。
     * @param configs テーブル構成のリスト
     * @param outputType "sql" または "xlsx"
     * @param outputDir 出力ディレクトリ
     */
    public void generateAndOutput(List<TableConfig> configs, String outputType, String outputDir) {
        for (TableConfig config : configs) {
            System.out.println("\n--- テーブル: " + config.getName() + " (" + config.getSize() + "行) の生成を開始 ---");
            List<Map<String, Object>> generatedData = generateTableData(config);

            if (outputType.equalsIgnoreCase("sql")) {
                writeSqlFile(config.getName(), generatedData, outputDir);
            } else if (outputType.equalsIgnoreCase("xlsx")) {
                writeXlsxFile(config.getName(), generatedData, outputDir);
            }
        }
        System.out.println("\n--- 全てのデータ生成と出力が完了しました ---");
    }

    /**
     * 単一のテーブルのデータを生成します。
     * @param config テーブル構成
     * @return 生成されたデータ行のリスト (Map<カラム名, 値>)
     */
    private List<Map<String, Object>> generateTableData(TableConfig config) {
        List<Map<String, Object>> tableData = new ArrayList<>();
        
        // ユニーク制約チェック用のマップ
        Map<String, Set<Object>> uniqueValues = new ConcurrentHashMap<>();
        
        // ARRAY（配列選択）の現在のインデックス
        Map<String, Integer> arrayIndices = new HashMap<>();

        // SERIALカラムを取得し、初期値を設定
        Optional<ColumnConfig> serialColumn = config.getData().stream()
                .filter(c -> "SERIAL".equalsIgnoreCase(c.getType()))
                .findFirst();

        Integer initialSerialValue = serialColumn.map(ColumnConfig::getStartFrom).orElse(1);


        for (int i = 0; i < config.getSize(); i++) {
            Map<String, Object> row = new LinkedHashMap<>();
            int primaryKey = initialSerialValue + i; // SERIALの暫定値 (STRINGプレースホルダー用)

            for (ColumnConfig col : config.getData()) {
                Object value = null;
                boolean isUniqueRequired = col.getUnique() != null && col.getUnique();
                int attempts = 0;
                
                // SERIAL処理: primaryKeyに値を代入し、次のループのプレースホルダーに使用
                if ("SERIAL".equalsIgnoreCase(col.getType())) {
                    value = primaryKey;
                } else {
                    // ユニーク制約がある場合は、ユニークな値が得られるまでループ
                    do {
                        // SERIAL Countersはここでは不要なため削除
                        value = generateSingleValue(col, primaryKey, arrayIndices); 
                        attempts++;
                        if (!isUniqueRequired || !uniqueValues.getOrDefault(col.getColumnName(), Collections.emptySet()).contains(value)) {
                            break; // ユニーク制約がない、またはユニークな値が生成された
                        }
                        if (attempts > 100) {
                            System.err.println("エラー: カラム '" + col.getColumnName() + "' で100回試行してもユニークな値が生成できませんでした。設定を見直してください。");
                            // 処理を続行するために、最後の値を強制的に使用
                            break;
                        }
                    } while (true);
                }

                row.put(col.getColumnName(), value);

                // ユニーク制約の値を追跡
                if (isUniqueRequired) {
                    // Setへの追加は同期化されたコレクションを使用
                    uniqueValues.computeIfAbsent(col.getColumnName(), k -> Collections.synchronizedSet(new HashSet<>())).add(value);
                }

                // isHashedが指定されている場合、ハッシュカラムを追加
                if (col.getIsHashed() != null && "STRING".equalsIgnoreCase(col.getType())) {
                    String hash = Hasher.hashPassword(value.toString());
                    row.put(col.getIsHashed(), hash);
                }
            }
            tableData.add(row);
        }
        return tableData;
    }

    /**
     * 単一のカラムの値を生成します。
     * @param config カラム設定
     * @param primaryKey 現在の行の主キー値 (STRINGプレースホルダー用)
     * @param arrayIndices 配列のインデックス追跡用マップ
     */
    private Object generateSingleValue(ColumnConfig config, int primaryKey, Map<String, Integer> arrayIndices) {
        System.out.println("カラム '" + config.getColumnName() + "' の値を生成中... (タイプ: " + config.getType() + ")");
        switch (config.getType().toUpperCase()) {
            case "STRING":
                String formattedString = config.getFormat().replace("{i}", String.valueOf(primaryKey));
                return formattedString;
            
            case "REGEX":
                String pattern = config.getPattern();
                if (pattern == null || pattern.isEmpty()) {
                    return null;
                }
                return faker.regexify(pattern);

            case "NUMBER":
                long min = config.getMin() != null ? config.getMin() : 0;
                long max = config.getMax() != null ? config.getMax() : Long.MAX_VALUE;
                
                // 上限がない場合、intの最大値を使用（Long.MAX_VALUEだとXLSX出力などで問題が起きやすいため）
                if (max > Integer.MAX_VALUE) max = Integer.MAX_VALUE; 

                if (min > max) {
                    System.err.println("警告: NUMBER型のmin > maxです。min=" + min + ", max=" + max);
                    return min;
                }
                
                // 【修正】シード値を持つ this.random を使用した範囲内乱数生成
                long range = max - min + 1;
                if (range <= 0) { // オーバーフロー防止
                    return min;
                }
                // Random.nextLong() は負の値を返す可能性があるため、absとmodulo演算を使用
                return min + (Math.abs(this.random.nextLong()) % range);

            case "ARRAY":
                List<String> values = config.getValues();
                if (values == null || values.isEmpty()) return null;

                if (config.getIsRandom() != null && config.getIsRandom()) {
                    // ランダム選択
                    return values.get(random.nextInt(values.size()));
                } else {
                    // シーケンシャル選択 (先頭に戻る)
                    int index = arrayIndices.getOrDefault(config.getColumnName(), 0);
                    String value = values.get(index);
                    arrayIndices.put(config.getColumnName(), (index + 1) % values.size());
                    return value;
                }
                
            case "DATETIME":
                return generateRandomDateTime(config);

            default:
                return null;
        }
    }

    /**
     * ランダムな日時を生成します。
     */
    private String generateRandomDateTime(ColumnConfig config) {
        try {
            // 最小日時 (必須)
            LocalDateTime minDateTime = LocalDateTime.parse(config.getMinDate(), DATE_FORMAT);
            long minEpoch = minDateTime.toEpochSecond(ZoneOffset.UTC);

            // 最大日時 (オプショナル、デフォルトは現在日時)
            LocalDateTime maxDateTime;
            if (config.getMaxDate() != null) {
                maxDateTime = LocalDateTime.parse(config.getMaxDate(), DATE_FORMAT);
            } else {
                maxDateTime = LocalDateTime.now();
            }
            long maxEpoch = maxDateTime.toEpochSecond(ZoneOffset.UTC);

            if (minEpoch > maxEpoch) {
                 System.err.println("警告: DATETIME型のminDate > maxDateです。minDate=" + config.getMinDate() + ", maxDate=" + config.getMaxDate());
                 return config.getMinDate();
            }

            // 【修正】シード値を持つ this.random を使用した範囲内エポック秒生成
            long range = maxEpoch - minEpoch + 1;
            // nextLong()の絶対値を使用し、範囲内の乱数を安全に生成
            long randomEpoch = minEpoch + (Math.abs(this.random.nextLong()) % range);

            return LocalDateTime.ofEpochSecond(randomEpoch, 0, ZoneOffset.UTC).format(DATE_FORMAT);
        } catch (Exception e) {
            System.err.println("日時生成エラー: " + e.getMessage() + ". 現在の日時を返します。");
            return LocalDateTime.now().format(DATE_FORMAT);
        }
    }

    /**
     * 生成されたデータをSQL INSERT文としてファイルに書き出します。
     */
    private void writeSqlFile(String tableName, List<Map<String, Object>> data, String outputDir) {
        if (data.isEmpty()) return;

        File outFile = new File(outputDir, tableName.toLowerCase() + ".sql");
        System.out.println("SQLファイルを出力中: " + outFile.getAbsolutePath());

        try (java.io.FileWriter writer = new java.io.FileWriter(outFile)) {
            List<String> columnNames = new ArrayList<>(data.get(0).keySet());
            String columns = columnNames.stream().collect(Collectors.joining(", "));
            
            for (Map<String, Object> row : data) {
                String values = columnNames.stream()
                        .map(col -> {
                            Object value = row.get(col);
                            if (value instanceof String || value instanceof LocalDateTime) {
                                // SQLの文字列リテラルとしてシングルクォーテーションで囲む
                                return "'" + value.toString().replace("'", "''") + "'";
                            }
                            return value.toString();
                        })
                        .collect(Collectors.joining(", "));

                String insertStatement = String.format("INSERT INTO %s (%s) VALUES (%s);\n",
                        tableName, columns, values);
                writer.write(insertStatement);
            }
            System.out.println("SQLファイルの出力が完了しました。");
        } catch (IOException e) {
            System.err.println("SQLファイルへの書き込みエラー: " + e.getMessage());
        }
    }

    /**
     * 生成されたデータをXLSXファイルとして書き出します。
     */
    private void writeXlsxFile(String tableName, List<Map<String, Object>> data, String outputDir) {
        if (data.isEmpty()) return;

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(tableName);
        List<String> columnNames = new ArrayList<>(data.get(0).keySet());

        // 1. ヘッダー行の作成
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < columnNames.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columnNames.get(i));
        }

        // 2. データ行の作成
        int rowNum = 1;
        for (Map<String, Object> rowData : data) {
            Row row = sheet.createRow(rowNum++);
            for (int i = 0; i < columnNames.size(); i++) {
                String colName = columnNames.get(i);
                Object value = rowData.get(colName);
                
                Cell cell = row.createCell(i);
                if (value instanceof String) {
                    cell.setCellValue((String) value);
                } else if (value instanceof Number) {
                    cell.setCellValue(((Number) value).doubleValue());
                } else {
                    cell.setCellValue(String.valueOf(value));
                }
            }
        }

        // ファイル出力
        File outFile = new File(outputDir, tableName.toLowerCase() + ".xlsx");
        System.out.println("XLSXファイルを出力中: " + outFile.getAbsolutePath());

        try (FileOutputStream fileOut = new FileOutputStream(outFile)) {
            workbook.write(fileOut);
            System.out.println("XLSXファイルの出力が完了しました。");
        } catch (IOException e) {
            System.err.println("XLSXファイルへの書き込みエラー: " + e.getMessage());
        } finally {
            try {
                workbook.close();
            } catch (IOException e) {
                System.err.println("Workbookクローズエラー: " + e.getMessage());
            }
        }
    }
}
