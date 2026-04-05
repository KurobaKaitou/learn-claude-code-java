package site.factory;

import dev.langchain4j.community.model.zhipu.ZhipuAiChatModel;
import io.github.cdimascio.dotenv.Dotenv;

public class ZhiPuChatModelFactory {

    /**
     * create zhipu chat model
     *
     * @return zhipuChatModel
     */
    public static ZhipuAiChatModel createZhiPuChatModel() {
        Dotenv dotenv = Dotenv.load();
        String apiKey = dotenv.get("ZHIPU_API_KEY");
        String model = dotenv.get("MODEL_NAME");
        return ZhipuAiChatModel.builder().apiKey(apiKey).model(model).build();
    }
}
