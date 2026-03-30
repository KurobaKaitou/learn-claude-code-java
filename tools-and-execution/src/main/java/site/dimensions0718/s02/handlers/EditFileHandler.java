package site.dimensions0718.s02.handlers;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import site.dimensions0718.s02.enums.ToolEnum;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class EditFileHandler extends AbsToolHandler {
    @Override
    public ToolEnum type() {
        return ToolEnum.EDIT_FILE;
    }

    @Override
    public String handle(Object... params) {
        String path = (String) params[0];
        String oldText = (String) params[1];
        String newText = (String) params[2];
        return runEdit(path, oldText, newText);
    }

    @Tool(value = "Replace exact text in file.", name = "edit_file")
    public String runEdit(@P("The file path.") String path, @P("The text to be replaced.") String oldText, @P("The replacement text.") String newText) {
        try {
            Path safePath = safePath(path);
            String content = Files.readString(safePath);
            if (!content.contains(oldText)) {
                return String.format("Error: Text not found in %s", path);
            }
            Files.writeString(safePath, content.replaceAll(oldText, newText));
            return String.format("Edited %s", path);
        } catch (IOException e) {
            return String.format("Error: %s", e.getMessage());
        }
    }
}
