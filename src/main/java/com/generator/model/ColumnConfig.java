package com.generator.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * JSONの設定ファイル内の各カラム定義に対応するPOJOクラス。
 */
public class ColumnConfig {
    @JsonProperty("columnName")
    private String columnName;

    @JsonProperty("type")
    private String type; // SERIAL, STRING, REGEX, NUMBER, ARRAY, DATETIME

    // SERIAL/STRING用
    @JsonProperty("startFrom")
    private Integer startFrom;

    // STRING用
    @JsonProperty("format")
    private String format;

    // REGEX用
    @JsonProperty("pattern")
    private String pattern;

    // NUMBER/DATETIME用
    @JsonProperty("min")
    private Long min;
    @JsonProperty("max")
    private Long max;
    @JsonProperty("minDate")
    private String minDate;
    @JsonProperty("maxDate")
    private String maxDate;

    // ARRAY用
    @JsonProperty("values")
    private List<String> values;
    @JsonProperty("isRandom")
    private Boolean isRandom;

    // 制約用
    @JsonProperty("unique")
    private Boolean unique;

    // パスワードハッシュ化用
    @JsonProperty("isHashed")
    private String isHashed; // ハッシュ化後のカラム名

    @JsonProperty("generator")
    private String generator; 

    // Getter and Setter (Jacksonが使用するため必須)
    public String getColumnName() { return columnName; }
    public void setColumnName(String columnName) { this.columnName = columnName; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Integer getStartFrom() { return startFrom; }
    public void setStartFrom(Integer startFrom) { this.startFrom = startFrom; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }

    public Long getMin() { return min; }
    public void setMin(Long min) { this.min = min; }

    public Long getMax() { return max; }
    public void setMax(Long max) { this.max = max; }

    public String getMinDate() { return minDate; }
    public void setMinDate(String minDate) { this.minDate = minDate; }

    public String getMaxDate() { return maxDate; }
    public void setMaxDate(String maxDate) { this.maxDate = maxDate; }

    public List<String> getValues() { return values; }
    public void setValues(List<String> values) { this.values = values; }

    public Boolean getIsRandom() { return isRandom; }
    public void setIsRandom(Boolean isRandom) { this.isRandom = isRandom; }

    public Boolean getUnique() { return unique; }
    public void setUnique(Boolean unique) { this.unique = unique; }

    public String getIsHashed() { return isHashed; }
    public void setIsHashed(String isHashed) { this.isHashed = isHashed; }

    public String getGenerator() { return generator; }
    public void setGenerator(String generator) { this.generator = generator; }
}
