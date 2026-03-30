package site.dimensions0718.s02.enums;

public enum ToolEnum {
    BASH,
    READ_FILE,
    WRITE_FILE,
    EDIT_FILE;

    public static ToolEnum get(String toolName) {
        for (ToolEnum tool : ToolEnum.values()) {
            if (tool.name().equalsIgnoreCase(toolName)) {
                return tool;
            }
        }
        return null;
    }
}
