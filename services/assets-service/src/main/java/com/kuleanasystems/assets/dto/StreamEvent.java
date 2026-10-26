package com.kuleanasystems.assets.dto;

public class StreamEvent {

    public static final String ASSET_DISCOVERED = "ASSET_DISCOVERED";
    public static final String TIMELINE_LOADED = "TIMELINE_LOADED";
    public static final String TIMELINE_UNAVAILABLE = "TIMELINE_UNAVAILABLE";
    public static final String STREAM_COMPLETE = "STREAM_COMPLETE";
    public static final String ERROR = "ERROR";
    public static final String NOT_FOUND = "NOT_FOUND";

    private String type;
    private Object data;

    public StreamEvent() {}

    public StreamEvent(String type, Object data) {
        this.type = type;
        this.data = data;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }

    @Override
    public String toString() {
        return "StreamEvent{type='%s', data=%s}".formatted(type, data);
    }
}
