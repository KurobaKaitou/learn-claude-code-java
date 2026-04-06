package site.dimensions0718.s03;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.community.model.zhipu.ZhipuAiChatModel;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.FinishReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import site.dimensions0718.container.ToolHandlerContainer;
import site.dimensions0718.enums.ToolEnum;
import site.dimensions0718.handler.*;
import site.dimensions0718.handler.TodoHandler;
import site.factory.ZhiPuChatModelFactory;

import java.util.*;

public class TodoManager {
    private final Logger log = LoggerFactory.getLogger(TodoManager.class);
    private final ZhipuAiChatModel zhipuAiChatModel = ZhiPuChatModelFactory.createZhiPuChatModel();
    private final List<ToolSpecification> toolSpecifications;
    private final ToolHandlerContainer toolHandlerContainer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TodoManager() {
        List<AbsToolHandler> handlers = this.init();
        this.toolSpecifications = new ArrayList<>();
        this.toolHandlerContainer = new ToolHandlerContainer();
        handlers.forEach(handler -> this.toolSpecifications.add(ToolSpecifications.toolSpecificationsFrom(handler.getClass()).getFirst()));
    }

    private List<AbsToolHandler> init() {
        return List.of(new BashHandler(), new ReadFileHandler(), new WriteFileHandler(), new EditFileHandler(), new TodoHandler());
    }

    /**
     * <font color='red'>core logic</font>
     *
     * @param chatMessages chatMessage
     */
    public void agentLoop(List<ChatMessage> chatMessages) throws JsonProcessingException {
        int roundsSinceTodo = 0;
        while (true) {
            ChatRequest chatRequest = ChatRequest.builder().messages(chatMessages).toolSpecifications(toolSpecifications).maxOutputTokens(8000).build();
            ChatResponse response = this.zhipuAiChatModel.chat(chatRequest);
            AiMessage aiMessage = response.aiMessage();
            chatMessages.add(aiMessage);
            if (response.finishReason() != FinishReason.TOOL_EXECUTION) {
                log.info("agent response: {}", aiMessage.text());
                return;
            }
            List<ChatMessage> results = new ArrayList<>();
            boolean usedTodo = false;
            // foreach toolExecutionRequests get command argument
            for (ToolExecutionRequest toolExecutionRequest : aiMessage.toolExecutionRequests()) {
                log.info("agent invoke [{}] tool,arguments [{}] ", toolExecutionRequest.name(), toolExecutionRequest.arguments());
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
                if (toolExecutionRequest.name().equals(ToolEnum.TODO.name())) usedTodo = true;
                roundsSinceTodo = usedTodo ? 0 : roundsSinceTodo + 1;
                if (roundsSinceTodo > 3) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("type", "text");
                    map.put("text", "<reminder>Update your todos.</reminder>");
                    results.addFirst(CustomMessage.from(map));
                }
            }
            chatMessages.add(UserMessage.from(results.toString()));
        }
    }
}
