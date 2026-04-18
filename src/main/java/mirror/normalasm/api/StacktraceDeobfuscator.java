package mirror.normalasm.api;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class StacktraceDeobfuscator {

    private static Map<String, String> srgMcpMethodMap = null;

    public static void init(File mappings) {
        if (srgMcpMethodMap != null) {
            return;
        }
        Map<String, String> srgMcpMethodMap = new Object2ObjectOpenHashMap<>();
        try (InputStream is = StacktraceDeobfuscator.class.getResourceAsStream("/methods.csv")) { // curseforge is stinky 🤮
            if (is == null) {
                throw new RuntimeException("methods.csv not found in classpath");
            }
            try (Scanner scanner = new Scanner(is)) {
                scanner.nextLine();
                while (scanner.hasNext()) {
                    String mappingLine = scanner.nextLine();
                    int commaIndex = mappingLine.indexOf(',');
                    String srgName = mappingLine.substring(0, commaIndex);
                    String mcpName = mappingLine.substring(commaIndex + 1, commaIndex + 1 + mappingLine.substring(commaIndex + 1).indexOf(','));
                    srgMcpMethodMap.put(srgName, mcpName);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        StacktraceDeobfuscator.srgMcpMethodMap = srgMcpMethodMap;
    }

    public static void deobfuscateThrowable(Throwable t) {
        Deque<Throwable> queue = new ArrayDeque<>();
        queue.add(t);
        while (!queue.isEmpty()) {
            t = queue.remove();
            t.setStackTrace(deobfuscateStacktrace(t.getStackTrace()));
            if (t.getCause() != null) {
                queue.add(t.getCause());
            }
            Collections.addAll(queue, t.getSuppressed());
        }
    }

    public static StackTraceElement[] deobfuscateStacktrace(StackTraceElement[] stackTrace) {
        int index = 0;
        for (StackTraceElement el : stackTrace) {
            stackTrace[index++] = new StackTraceElement(el.getClassName(), deobfuscateMethodName(el.getMethodName()), el.getFileName(), el.getLineNumber());
        }
        return stackTrace;
    }

    public static String deobfuscateMethodName(String srgName) {
        if (srgMcpMethodMap == null) {
            return srgName; // Not initialized
        }
        String mcpName = srgMcpMethodMap.get(srgName);
        // log.debug(srgName + " <=> " + mcpName != null ? mcpName : "?"); // Can't do this, it would be a recursive call to log appender
        return mcpName != null ? mcpName : srgName;
    }

    public static void main(String[] args) {
        init(new File("methods.csv"));
        for (Map.Entry<String, String> entry : srgMcpMethodMap.entrySet()) {
            System.out.println(entry.getKey() + " <=> " + entry.getValue());
        }
    }
}
