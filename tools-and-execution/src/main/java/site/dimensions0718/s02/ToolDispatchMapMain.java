package site.dimensions0718.s02;

import com.fasterxml.jackson.core.JsonProcessingException;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;

import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class ToolDispatchMapMain {
    public static void main(String[] args) throws JsonProcessingException {
        ToolDispatchMap toolDispatchMap = new ToolDispatchMap();
        String systemPrompt = String.format("You are a coding agent at %s. Use bash to solve tasks. Act, don't explain.", System.getProperty("user.dir"));
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
            toolDispatchMap.agentLoop(chatMessages);
        }
    }
}
