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
import site.dimensions0718.container.ToolHandlerContainer;
import site.dimensions0718.enums.ToolEnum;
import site.dimensions0718.handler.*;
import site.factory.ZhiPuChatModelFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ToolDispatchMap {

    private final ToolHandlerContainer toolHandlerContainer;
    private final ZhipuAiChatModel zhipuAiChatModel;
    private final List<ToolSpecification> toolSpecifications;
    private final ObjectMapper objectMapper;

    public ToolDispatchMap() {
        List<AbsToolHandler> handlers = init();
        this.toolHandlerContainer = new ToolHandlerContainer();
        this.toolSpecifications = new ArrayList<>();
        this.zhipuAiChatModel = ZhiPuChatModelFactory.createZhiPuChatModel();
        handlers.forEach(handler -> this.toolSpecifications.add(ToolSpecifications.toolSpecificationsFrom(handler.getClass()).getFirst()));
        this.objectMapper = new ObjectMapper();
    }

    private List<AbsToolHandler> init() {
        return List.of(new BashHandler(), new ReadFileHandler(), new WriteFileHandler(), new EditFileHandler());
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
                AbsToolHandler handler = this.toolHandlerContainer.getHandler(ToolEnum.get(toolExecutionRequest.name()));
                if (handler == null) {
                    result = String.format("Unknown tool: %s", toolExecutionRequest.name());
                } else {
                    result = handler.handle(params.values().toArray());
                }
                results.add(ToolExecutionResultMessage.from(toolExecutionRequest.id(), toolExecutionRequest.name(), result));
            }
            chatMessages.add(UserMessage.from(results.toString()));
        }
    }
}

