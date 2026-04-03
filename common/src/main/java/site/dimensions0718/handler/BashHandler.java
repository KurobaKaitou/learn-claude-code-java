package site.dimensions0718.handler;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import site.dimensions0718.enums.ToolEnum;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class BashHandler extends AbsToolHandler {

    @Override
    public ToolEnum type() {
        return ToolEnum.BASH;
    }

    @Override
    public String handle(Object... params) {
        String command = (String) params[0];
        return runBash(command);
    }


    @Tool(value = "Run a shell command.", name = "bash")
    public String runBash(@P("The shell command to run.") String command) {
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
            BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));

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
}
