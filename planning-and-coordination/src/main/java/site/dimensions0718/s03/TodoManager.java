package site.dimensions0718.s03;

import dev.langchain4j.agent.tool.Tool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class TodoManager {

    private List<TodoItem> todoItems = new ArrayList<>();
    private final List<String> normalizedStatus = Arrays.asList("pending", "completed", "in_progress");

    @Tool(name = "todo", value = "Update task list. Track progress on multi-step tasks.")
    public String update(List<TodoItem> todoItems) {
        if (todoItems.size() > 20) {
            throw new RuntimeException("Max 20 todos allowed");
        }
        List<TodoItem> validated = new ArrayList<>();
        int inProgressCount = 0;
        for (int i = 0; i < todoItems.size(); i++) {
            TodoItem todoItem = todoItems.get(i);
            String text = todoItem.text();
            String status = todoItem.status() == null ? "pending" : todoItem.status().toLowerCase(Locale.ROOT);
            Integer itemId = todoItem.id() == null ? i + 1 : todoItem.id();
            if (text == null || text.isBlank()) {
                throw new RuntimeException(String.format("Item %d: text required", itemId));
            }
            if (!normalizedStatus.contains(status)) {
                throw new RuntimeException(String.format("Item %d: invalid status '%s'", itemId, status));
            }
            if ("in_progress".equals(status)) {
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
            switch (todoItem.status()) {
                case "pending" -> market = "[ ]";
                case "in_progress" -> market = "[>]";
                case "completed" -> market = "[x]";
                default -> market = "[?]";
            }
            lines.add(String.format("%s #%d: %s", market, todoItem.id(), todoItem.text()));
        });
        long done = todoItems.stream().filter(todoItem -> "completed".equals(todoItem.status())).count();
        lines.add(String.format("\n(%d/%d completed)", done, todoItems.size()));
        return String.join("\n", lines);
    }
}
