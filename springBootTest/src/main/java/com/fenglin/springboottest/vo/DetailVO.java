package com.fenglin.springboottest.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * 魔族修行 · 内容详情（功法阁 / 历练录 / 宗门论道 / 魔典秘录 通用）
 */
public class DetailVO {

    /** 所属模块类型：art / trial / debate / codex */
    private String type;
    /** 模块中文名 */
    private String typeName;
    private Long id;
    private String title;
    /** 一句话摘要 */
    private String summary;
    /** 正文（功法取 content，其余取 summary） */
    private String content;
    /** 配图相对路径（无图为 null，前端需判空兜底） */
    private String img;
    /** 属性表：门类/品阶/境界/地域/凶险/论者/卷册 等 */
    private List<Meta> meta = new ArrayList<>();

    public String getImg() {
        return img;
    }

    public void setImg(String img) {
        this.img = img;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<Meta> getMeta() {
        return meta;
    }

    public void setMeta(List<Meta> meta) {
        this.meta = meta;
    }

    /** 详情页属性行 */
    public static class Meta {
        private String label;
        private String value;

        public Meta() {
        }

        public Meta(String label, String value) {
            this.label = label;
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
