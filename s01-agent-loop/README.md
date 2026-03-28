# `S01` The Agent Loop

### Bash Is All You Need

> "One loop & Bash is all you need" -- one tool + one loop = an agent.

### Core Code

```java
/**
 * the agent loop core logic
 * @param chatMessages
 */
public void agentLoop(List<ChatMessage> chatMessages) {
    // forever loop, when model stop reason != TOOL_EXECUTION break.
    while (true) {
        // build an chatRequest, carried chatMessage, description of tool and maxOutPutTokens
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(chatMessages)
                .toolSpecifications(toolSpecification)
                .maxOutputTokens(8000)
                .build();
        // send message to model, wait fetch model thinking result
        ChatResponse response = this.zhipuAiChatModel.chat(chatRequest);
        AiMessage aiMessage = response.aiMessage();
        chatMessages.add(aiMessage);
        List<ChatMessage> results = new ArrayList<>();
        // return and print model response when model no longer execute tool
        if (response.finishReason() != FinishReason.TOOL_EXECUTION) {
            System.out.printf("agent response: %s \n", aiMessage.text());
            return;
        }
        // Iterate through toolExecutionRequests to obtain the execution command.
        for (ToolExecutionRequest toolExecutionRequest : aiMessage.toolExecutionRequests()) {
            System.out.printf("agent invoke [%s] tool,arguments [%s] \n", toolExecutionRequest.name(), toolExecutionRequest.arguments());
            String command = toolExecutionRequest.arguments();
            Command commandClass = Json.fromJson(command, Command.class);
            // invoke run bash tool
            String runBashResult = runBash(commandClass.command);
            results.add(ToolExecutionResultMessage.from(toolExecutionRequest.id(), toolExecutionRequest.name(), runBashResult));
        }
        chatMessages.add(UserMessage.from(results.toString()));
    }
}
```

> for details,please refer to: [AgentLoop](src/main/java/site/dimensions0718/AgentLoop.java)
> And [AgentLoopMain](src/main/java/site/dimensions0718/AgentLoopMain.java)

### Try it
```bash
mv .env.example .env
vim .env
# edit the ZHIPU_API_KEY to your apiKey, save .env file
run AgentLoopMain.java
```

> Try input the following prompt words
> 1. Create a file called hello.py that prints "Hello, World!"
> 2. List all Python files in this directory
> 3. What is the current git branch?
> 4. Create a directory called test_output and write 3 files in it