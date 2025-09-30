package com.generator;

import com.generator.model.TableConfig;
import com.generator.util.DataGenerator;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.util.List;

/**
 * テーブルデータランダム生成ツールのメインエントリーポイント。
 * Apache Commons CLI を使用してコマンドライン引数 (-x, -q, -o, -s) を解析し、データ生成とファイル出力を行います。
 */
public class RegexGeneratorApp {

    // デフォルト値
    private static final String DEFAULT_CONFIG_PATH = "config.json";
    private static final long DEFAULT_SEED = 1L;
    private static final String DEFAULT_OUTPUT_DIR = "."; // カレントディレクトリ

    public static void main(String[] args) {
        Options options = createOptions();
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        // --- 1. コマンドライン引数の解析 ---
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("コマンドライン引数の解析エラー: " + e.getMessage());
            formatter.printHelp("java -jar data-generator-app.jar", options);
            return;
        }

        // -x または -q のいずれかが必要
        if (!cmd.hasOption("x") && !cmd.hasOption("q")) {
            System.err.println("エラー: 出力形式 (-x: XLSX または -q: SQL) を指定してください。");
            formatter.printHelp("java -jar data-generator-app.jar", options);
            return;
        }

        // 出力タイプとオプションの取得
        String outputType = cmd.hasOption("x") ? "xlsx" : "sql";
        String outputDir = cmd.hasOption("o") ? cmd.getOptionValue("o") : DEFAULT_OUTPUT_DIR;
        long seed = DEFAULT_SEED;

        String configPath = cmd.getOptionValue("i", DEFAULT_CONFIG_PATH);
        if (cmd.hasOption("s")) {
            try {
                seed = Long.parseLong(cmd.getOptionValue("s"));
            } catch (NumberFormatException e) {
                System.err.println("エラー: -s オプションには有効な数値シードが必要です。");
                formatter.printHelp("java -jar data-generator-app.jar", options);
                return;
            }
        }
        
        // --- 2. データ生成と出力の実行 ---
        try {
            DataGenerator generator = new DataGenerator(seed);
            
            System.out.println("設定ファイルをロード中: " + configPath);
            // JSON設定ファイルをロード
            List<TableConfig> configs = generator.loadConfig(DEFAULT_CONFIG_PATH);

            // データ生成と指定されたファイル形式での出力
            generator.generateAndOutput(configs, outputType, outputDir);

        } catch (IOException e) {
            System.err.println("致命的なエラー: 設定ファイルの読み込みに失敗しました。詳細: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("予期せぬエラーが発生しました: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Commons CLIのOptionsオブジェクトを作成します。
     */
    private static Options createOptions() {
        Options options = new Options();

        // 必須の出力オプション
        Option xlsx = new Option("x", "xlsx", false, "XLSXファイルとして出力します。");
        options.addOption(xlsx);

        Option sql = new Option("q", "sql", false, "SQL INSERT文として出力します。");
        options.addOption(sql);

        // 任意のオプション
        Option output = new Option("o", "output", true, "出力ディレクトリを指定します (デフォルト: ./)。");
        output.setArgName("path");
        options.addOption(output);

        Option seed = new Option("s", "seed", true, "乱数シード値を指定します (デフォルト: 1)。");
        seed.setArgName("seed_value");
        options.addOption(seed);
        
        Option input = new Option("i", "inputPath", true, "入力するJSON設定ファイルのパスを指定します (デフォルト: " + DEFAULT_CONFIG_PATH + ")。");
        input.setArgName("path");
        input.setRequired(false);
        options.addOption(input);

        return options;
    }
}
