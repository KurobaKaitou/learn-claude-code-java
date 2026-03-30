package site.dimensions0718.s02.handlers;

import site.dimensions0718.s02.enums.ToolEnum;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public abstract class AbsToolHandler {

    private static final Path WORK_DIR = Paths.get(System.getProperty("user.dir"));

    public Path safePath(String p) {
        Path path = WORK_DIR.resolve(p).normalize();
        if (!path.startsWith(WORK_DIR)) {
            throw new IllegalArgumentException("Path escapes workspace: " + p);
        }
        return path;
    }

    public abstract ToolEnum type();

    public abstract String handle(Object... params);
}
