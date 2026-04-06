package site.dimensions0718.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.internal.Json;
import site.dimensions0718.enums.ToolEnum;
import site.dimensions0718.vo.TodoItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class TodoHandler extends AbsToolHandler {
    private static final String PENDING = "pending";
    private static final String COMPLETED = "completed";
    private static final String IN_PROGRESS = "in_progress";
    private List<TodoItem> todoItems = new ArrayList<>();
    private final List<String> normalizedStatus = Arrays.asList(PENDING, COMPLETED, IN_PROGRESS);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ToolEnum type() {
        return ToolEnum.TODO;
    }

    @Override
    public String handle(Object... params) {
        List<?> rawList = (List<?>) params[0];
        List<TodoItem> todoItems = rawList.stream().map(item -> objectMapper.convertValue(item, TodoItem.class)).toList();
        return update(todoItems);
    }

    @Tool(name = "todo", value = "Update task list. Track progress on multi-step tasks.")
    public String update(@P("todoItems") List<TodoItem> todoItems) {
        if (todoItems.size() > 20) {
            throw new RuntimeException("Max 20 todos allowed");
        }
        List<TodoItem> validated = new ArrayList<>();
        int inProgressCount = 0;
        for (int i = 0; i < todoItems.size(); i++) {
            TodoItem todoItem = todoItems.get(i);
            String text = todoItem.getText();
            String status = todoItem.getStatus() == null ? PENDING : todoItem.getStatus().toLowerCase(Locale.ROOT);
            Integer itemId = todoItem.getId() == null ? i + 1 : todoItem.getId();
            if (text == null || text.isBlank()) {
                throw new RuntimeException(String.format("Item %d: text required", itemId));
            }
            if (!normalizedStatus.contains(status)) {
                throw new RuntimeException(String.format("Item %d: invalid status '%s'", itemId, status));
            }
            if (IN_PROGRESS.equals(status)) {
                inProgressCount++;
                validated.add(new TodoItem(itemId, text, status));
            }
            if (inProgressCount > 1) {
                throw new RuntimeException("Only one task can be in_progress at a time");
            }
            this.todoItems = validated;
        }
        return this.render();
    }

    public String render() {
        if (this.todoItems == null || this.todoItems.isEmpty()) {
            return "No todos.";
        }
        List<String> lines = new ArrayList<>();
        this.todoItems.forEach(todoItem -> {
            String market;
            switch (todoItem.getStatus()) {
                case PENDING -> market = "[ ]";
                case IN_PROGRESS -> market = "[>]";
                case COMPLETED -> market = "[x]";
                default -> market = "[?]";
            }
            lines.add(String.format("%s #%d: %s", market, todoItem.getId(), todoItem.getText()));
        });
        long done = todoItems.stream().filter(todoItem -> COMPLETED.equals(todoItem.getStatus())).count();
        lines.add(String.format("\n(%d/%d completed)", done, todoItems.size()));
        return String.join("\n", lines);
    }
}
