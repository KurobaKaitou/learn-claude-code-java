package site.dimensions0718.handler;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import site.dimensions0718.enums.ToolEnum;

import java.io.IOException;
import java.nio.file.Files;

public class ReadFileHandler extends AbsToolHandler {
    @Override
    public ToolEnum type() {
        return ToolEnum.READ_FILE;
    }

    @Override
    public String handle(Object... params) {
        String path = (String) params[0];
        Integer limit = (Integer) params[1];
        return runRead(path, limit);
    }

    @Tool(value = "Read file contents.", name = "read_file")
    public String runRead(@P("The file path.") String path, @P("The maximum number of lines to read.") Integer limit) {
        try {
            String text = Files.readString(safePath(path));
            String[] lines = text.split("\\R");
            int lineCount = lines.length;
            StringBuilder result = new StringBuilder();
            for (String line : lines) {
                result.append(line).append("\n");
            }
            if (limit != null && limit < lineCount) {
                result.append("... (").append(lineCount - limit).append(" more lines)\n");
            }
            String output = String.join("\n", result.toString());
            return output.length() > 50000 ? output.substring(0, 50000) : output;
        } catch (IOException e) {
            return String.format("Error: %s", e.getMessage());
        }
    }
}
