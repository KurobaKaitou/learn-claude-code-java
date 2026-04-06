package site.dimensions0718.s03;

import com.fasterxml.jackson.core.JsonProcessingException;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;

import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class TodoWrite {
    public static void main(String[] args) throws JsonProcessingException {
        TodoManager todoManager = new TodoManager();
        String systemPrompt = String.format(
                "You are a coding agent at %s. Use the todo tool to plan multi-step tasks. Mark in_progress before starting, completed when done.Prefer tools over prose.",
                System.getProperty("user.dir"));
        List<ChatMessage> chatMessages = new java.util.ArrayList<>(List.of(SystemMessage.from(systemPrompt)));
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("please input your question: ");
            String userMessage = scanner.nextLine();
            String command = userMessage.strip().toLowerCase(Locale.ROOT);
            if ("q".equals(command) || "exit".equals(command) || command.isEmpty()) {
                break;
            }
            chatMessages.add(UserMessage.from(userMessage));
            todoManager.agentLoop(chatMessages);
        }
    }
}
