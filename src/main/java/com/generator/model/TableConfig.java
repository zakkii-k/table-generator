package com.generator.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * JSONの設定ファイル内の各テーブル定義に対応するPOJOクラス。
 */
public class TableConfig {
    @JsonProperty("name")
    private String name;

    @JsonProperty("size")
    private int size; // 生成する行数

    @JsonProperty("data")
    private List<ColumnConfig> data; // カラム定義

    // Getter and Setter
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public List<ColumnConfig> getData() { return data; }
    public void setData(List<ColumnConfig> data) { this.data = data; }
}
