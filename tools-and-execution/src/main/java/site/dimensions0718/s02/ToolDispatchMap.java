package site.dimensions0718.s02;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.community.model.zhipu.ZhipuAiChatModel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.FinishReason;
import io.github.cdimascio.dotenv.Dotenv;
import site.dimensions0718.s02.enums.ToolEnum;
import site.dimensions0718.s02.handlers.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ToolDispatchMap {

    private final Map<ToolEnum, AbsToolHandler> handlerMap;
    private final ZhipuAiChatModel zhipuAiChatModel;
    private final List<ToolSpecification> toolSpecifications;
    private final ObjectMapper objectMapper;

    public ToolDispatchMap() {
        List<AbsToolHandler> handlers = init();
        this.handlerMap = new HashMap<>();
        this.toolSpecifications = new ArrayList<>();
        Dotenv dotenv = Dotenv.load();
        String apiKey = dotenv.get("ZHIPU_API_KEY");
        String model = dotenv.get("MODEL_NAME");
        this.zhipuAiChatModel = ZhipuAiChatModel.builder().apiKey(apiKey).model(model).build();
        handlers.forEach(handler -> {
            this.toolSpecifications.add(ToolSpecifications.toolSpecificationsFrom(handler.getClass()).getFirst());
            this.handlerMap.put(handler.type(), handler);
        });
        this.objectMapper = new ObjectMapper();
    }

    private List<AbsToolHandler> init() {
        return List.of(new BashHandler(), new ReadFileHandler(), new WriteFileHandler(), new EditFileHandler());
    }

    private AbsToolHandler getHandler(ToolEnum toolEnum) {
        return handlerMap.get(toolEnum);
    }

    /**
     * <font color='red'>core logic</font>
     *
     * @param chatMessages chatMessage
     */
    public void agentLoop(List<ChatMessage> chatMessages) throws JsonProcessingException {
        while (true) {
            ChatRequest chatRequest = ChatRequest.builder().messages(chatMessages).toolSpecifications(toolSpecifications).maxOutputTokens(8000).build();
            ChatResponse response = this.zhipuAiChatModel.chat(chatRequest);
            AiMessage aiMessage = response.aiMessage();
            chatMessages.add(aiMessage);
            List<ChatMessage> results = new ArrayList<>();
            if (response.finishReason() != FinishReason.TOOL_EXECUTION) {
                System.out.printf("agent response: %s \n", aiMessage.text());
                return;
            }
            // foreach toolExecutionRequests get command argument
            for (ToolExecutionRequest toolExecutionRequest : aiMessage.toolExecutionRequests()) {
                System.out.printf("agent invoke [%s] tool,arguments [%s] \n", toolExecutionRequest.name(), toolExecutionRequest.arguments());
                Map<String, Object> params = objectMapper.readValue(toolExecutionRequest.arguments(), new TypeReference<>() {
                });
                String result;
                AbsToolHandler handler = this.getHandler(ToolEnum.get(toolExecutionRequest.name()));
                if (handler == null) {
                    result = String.format("Unknown tool: %s", toolExecutionRequest.name());
                } else {
                    result = this.getHandler(ToolEnum.get(toolExecutionRequest.name())).handle(params.values().toArray());
                }
                results.add(ToolExecutionResultMessage.from(toolExecutionRequest.id(), toolExecutionRequest.name(), result));
            }
            chatMessages.add(UserMessage.from(results.toString()));
        }
    }
}

