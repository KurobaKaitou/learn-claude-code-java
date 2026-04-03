package site.dimensions0718.container;

import org.reflections.Reflections;
import site.dimensions0718.enums.ToolEnum;
import site.dimensions0718.handler.AbsToolHandler;

import java.util.HashMap;
import java.util.Map;

public class ToolHandlerContainer {
    private final Map<ToolEnum, AbsToolHandler> toolHandlerMap = new HashMap<>();

    public ToolHandlerContainer() {
        Reflections reflections = new Reflections(AbsToolHandler.class.getPackageName());
        for (Class<?> clazz : reflections.getSubTypesOf(AbsToolHandler.class)) {
            try {
                AbsToolHandler handler = (AbsToolHandler) clazz.getDeclaredConstructor().newInstance();
                toolHandlerMap.put(handler.type(), handler);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public AbsToolHandler getHandler(ToolEnum toolEnum) {
        return toolHandlerMap.get(toolEnum);
    }
}
