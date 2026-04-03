package site.dimensions0718.handler;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import site.dimensions0718.enums.ToolEnum;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class WriteFileHandler extends AbsToolHandler {
    @Override
    public ToolEnum type() {
        return ToolEnum.WRITE_FILE;
    }

    @Override
    public String handle(Object... params) {
        String path = (String) params[0];
        String content = (String) params[1];
        return runWrite(path, content);
    }

    @Tool(value = "Write content to file.", name = "write_file")
    public String runWrite(@P("The file path.") String path, @P("The content to write.") String content) {
        try {
            Path safePath = safePath(path);
            Path parent = safePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(safePath, content);
            return String.format("Wrote %d bytes to %s", content.length(), path);
        } catch (IOException e) {
            return String.format("Error: %s", e.getMessage());
        }
    }
}
