package bata.structurizr;

import com.structurizr.Workspace;
import com.structurizr.documentation.Documentable;
import com.structurizr.documentation.Documentation;
import com.structurizr.documentation.Section;
import com.structurizr.model.Element;

import com.structurizr.dsl.StructurizrDslPlugin;
import com.structurizr.dsl.StructurizrDslPluginContext;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Deflater;

/**
 * Replaces ```mermaid fenced blocks in the workspace and element documentation with an image
 * served by a Mermaid renderer we host ourselves (the mermaid.ink URL scheme).
 *
 * Parameters: url (required, e.g. http://localhost:8888), format (svg or png, default svg),
 * theme (default neutral).
 */
public class MermaidPlugin implements StructurizrDslPlugin {

    private static final Pattern BLOCK = Pattern.compile("```mermaid\\s*\\R(.*?)\\R```", Pattern.DOTALL);

    @Override
    public void run(StructurizrDslPluginContext context) {
        String url = context.getParameter("url");
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("MermaidPlugin: parameter 'url' is required");
        }
        String format = orDefault(context.getParameter("format"), "svg");
        String theme = orDefault(context.getParameter("theme"), "neutral");
        String base = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;

        Workspace workspace = context.getWorkspace();
        rewrite(workspace.getDocumentation(), base, format, theme);
        for (Element element : workspace.getModel().getElements()) {
            if (element instanceof Documentable documentable) {
                rewrite(documentable.getDocumentation(), base, format, theme);
            }
        }
    }

    private static void rewrite(Documentation documentation, String base, String format, String theme) {
        if (documentation == null) {
            return;
        }
        for (Section section : documentation.getSections()) {
            Matcher m = BLOCK.matcher(section.getContent());
            StringBuilder out = new StringBuilder();
            while (m.find()) {
                String image = "![](" + base + "/" + format + "/pako:" + encode(m.group(1), theme) + ")";
                m.appendReplacement(out, Matcher.quoteReplacement(image));
            }
            m.appendTail(out);
            section.setContent(out.toString());
        }
    }

    /** mermaid.ink payload: base64url of the zlib-compressed JSON state. */
    static String encode(String diagram, String theme) {
        String json = "{\"code\":" + quote(diagram) + ",\"mermaid\":{\"theme\":" + quote(theme) + "}}";
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        deflater.setInput(json.getBytes(StandardCharsets.UTF_8));
        deflater.finish();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            bytes.write(buffer, 0, deflater.deflate(buffer));
        }
        deflater.end();
        return Base64.getUrlEncoder().encodeToString(bytes.toByteArray());
    }

    private static String quote(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> {
                    if (c < 0x20) b.append(String.format("\\u%04x", (int) c)); else b.append(c);
                }
            }
        }
        return b.append('"').toString();
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
