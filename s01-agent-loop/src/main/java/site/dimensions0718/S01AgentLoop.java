package site.dimensions0718;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.community.model.zhipu.ZhipuAiChatModel;
import dev.langchain4j.data.message.*;
import dev.langchain4j.internal.Json;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.FinishReason;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class S01AgentLoop {
    /**
     * the function calling specification
     */
    private final ToolSpecification toolSpecification = ToolSpecification.builder()
            .name("bash")
            .description("Run a shell command.")
            .parameters(JsonObjectSchema.builder()
                    .addStringProperty("command")
                    .required("command")
                    .build())
            .build();

    private final ZhipuAiChatModel zhipuAiChatModel;

    public S01AgentLoop() {
        Dotenv dotenv = Dotenv.load();
        String apiKey = dotenv.get("spring.ai.zhipuai.api-key");
        String model = dotenv.get("spring.ai.zhipuai.chat.options.model");
        this.zhipuAiChatModel = ZhipuAiChatModel.builder()
                .apiKey(apiKey)
                .model(model)
                .build();
    }

    /**
     * `
     * run bash tool
     *
     * @param command command of need running
     * @return after running result
     */
    public String runBash(String command) {
        ProcessBuilder builder = new ProcessBuilder();
        List<String> dangerous = List.of("rm -rf /", "sudo", "shutdown", "reboot", "> /dev/");
        if (dangerous.contains(command)) {
            return "Error: Dangerous command blocked";
        }
        builder.command("bash", "-c", command);

        builder.directory(new File(System.getProperty("user.dir")));

        StringBuilder output = new StringBuilder();

        try {
            Process process = builder.start();

            // 读取 stdout 和 stderr
            BufferedReader stdout = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            BufferedReader stderr = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()));

            // 并发读取（避免阻塞）
            try (ExecutorService executor = Executors.newFixedThreadPool(2)) {

                Future<?> outFuture = executor.submit(() -> {
                    stdout.lines().forEach(line -> output.append(line).append("\n"));
                });

                Future<?> errFuture = executor.submit(() -> {
                    stderr.lines().forEach(line -> output.append(line).append("\n"));
                });

                boolean finished = process.waitFor(120, TimeUnit.SECONDS);

                if (!finished) {
                    process.destroyForcibly();
                    return "Process timed out";
                }

                outFuture.get();
                errFuture.get();
                executor.shutdown();
            }

        } catch (Exception e) {
            return e.getMessage();
        }

        String result = output.toString().trim();

        if (result.isEmpty()) {
            return "(no output)";
        }
        return result.length() > 50000 ? result.substring(0, 50000) : result;
    }

    /**
     * command object
     *
     * @param command
     */
    public record Command(String command) {

    }

    public void agentLoop(List<ChatMessage> chatMessages) {
        while (true) {
            ChatRequest chatRequest = ChatRequest.builder()
                    .messages(chatMessages)
                    .toolSpecifications(toolSpecification)
                    .maxOutputTokens(8000)
                    .build();
            ChatResponse response = this.zhipuAiChatModel.chat(chatRequest);
            AiMessage aiMessage = response.aiMessage();
            chatMessages.add(aiMessage);
            List<ChatMessage> results = new ArrayList<>();
            if (response.finishReason() != FinishReason.TOOL_EXECUTION) {
                System.out.println(aiMessage.text());
                return;
            }
            for (ToolExecutionRequest toolExecutionRequest : aiMessage.toolExecutionRequests()) {
                String command = toolExecutionRequest.arguments();
                Command commandClass = Json.fromJson(command, Command.class);
                String runBashResult = runBash(commandClass.command);
                results.add(ToolExecutionResultMessage.from(toolExecutionRequest.id(), toolExecutionRequest.name(), runBashResult));
            }
            chatMessages.add(UserMessage.from(results.toString()));
        }
    }

    public static void main(String[] args) {
        S01AgentLoop s01AgentLoop = new S01AgentLoop();
        ProcessBuilder builder = new ProcessBuilder();
        String systemPrompt = String.format("You are a coding agent at %s. Use bash to solve tasks. Act, don't explain.", builder.directory(new File(System.getProperty("user.dir"))));
        List<ChatMessage> chatMessages = new java.util.ArrayList<>(List.of(SystemMessage.from(systemPrompt)));
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String userMessage = scanner.nextLine();
            String command = userMessage.strip().toLowerCase(Locale.ROOT);
            if ("q".equals(command) || "exit".equals(command) || command.isEmpty()) {
                break;
            }
            chatMessages.add(UserMessage.from(userMessage));
            s01AgentLoop.agentLoop(chatMessages);
        }
    }
}