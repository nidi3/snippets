/*
 * Copyright (C) 2014 Stefan Niederhauser (nidin@gmx.ch)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package guru.nidi.snippets;

import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class Snippets {
    final Map<String, String> snippets = new HashMap<>();
    private final Pattern snippetStart;
    private final String snippetEnd;
    private final Pattern refStart;
    private final String refEnd;

    public Snippets(String snippetStart, String snippetEnd, String refStart, String refEnd) {
        this.refEnd = refEnd;
        this.snippetStart = markPattern(snippetStart);
        this.snippetEnd = snippetEnd;
        this.refStart = markPattern(refStart);
    }

    private static Pattern markPattern(String s) {
        final int pos = s.indexOf("%name");
        if (pos < 0) {
            throw new IllegalArgumentException("Start pattern must contain '%name'");
        }
        return Pattern.compile("\\Q" + s.substring(0, pos) + "\\E([A-Za-z0-9]+)\\Q" + s.substring(pos + 5));
    }

    public Snippets withFile(File file, String encoding) throws IOException {
        try (final Reader in = new InputStreamReader(new FileInputStream(file), encoding)) {
            parse(in);
        }
        return this;
    }

    public Snippets withString(String code) {
        try {
            parse(new StringReader(code));
            return this;
        } catch (IOException e) {
            throw new AssertionError("Cannot happen",e);
        }
    }

    public void replaceRefs(File file, File output, String encoding) throws IOException {
        replace(file, output, encoding, true);
    }

    public void replaceSnippets(File file, String encoding) throws IOException {
        final File temp = File.createTempFile("snippets", "txt");
        replace(file, temp, encoding, false);
        if (!file.delete()) {
            throw new IOException("Could not delete file " + file);
        }
        Files.move(temp.toPath(), file.toPath());
    }

    public String replaceRefs(String s) {
        return replace(s, true);
    }

    public String replaceSnippets(String s) {
        return replace(s, false);
    }

    private void replace(File file, File output, String encoding, boolean refs) throws IOException {
        output.getParentFile().mkdirs();
        try (final Reader in = new InputStreamReader(new FileInputStream(file), encoding);
             final Writer out = new OutputStreamWriter(new FileOutputStream(output), encoding)) {
            replace(in, out, refs);
        }
    }

    private String replace(String s, boolean refs) {
        final StringWriter sw = new StringWriter();
        try {
            replace(new StringReader(s), sw, refs);
            return sw.toString();
        } catch (IOException e) {
            throw new AssertionError("Cannot happen",e);
        }
    }

    private void parse(Reader in) throws IOException {
        final String code = IoUtils.read(in);
        final Matcher matcher = snippetStart.matcher(code);
        int endPos = -1;
        while (matcher.find(endPos + 1)) {
            endPos = code.indexOf(snippetEnd, matcher.end());
            if (endPos < 0) {
                throw new IllegalArgumentException("No snippetEnd marker found for snippetStart '" + code.substring(matcher.start(), matcher.end()) + "'");
            }
            final String name = matcher.group(1);
            if (snippets.containsKey(name)) {
                throw new IllegalArgumentException("Snippet with name '" + name + "' already existing.");
            }
            snippets.put(name, trim(code.substring(matcher.end(), endPos)));
        }
    }

    private String trim(String s) {
        int minIndent = 1000;
        final String[] lines = s.split("\n");
        for (int i = 0; i < lines.length; i++) {
            int pos = 0;
            while (pos < lines[i].length() && lines[i].charAt(pos) <= ' ') {
                pos++;
            }
            if (pos < lines[i].length() && pos < minIndent) {
                minIndent = pos;
            }
        }
        final StringBuilder sb = new StringBuilder();
        for (final String line : lines) {
            sb.append(line.length() >= minIndent ? line.substring(minIndent) : line).append('\n');
        }
        return s.endsWith("\n") ? sb.toString() : sb.substring(0, sb.length() - 1);
    }

    private void replace(Reader in, Writer out, boolean refs) throws IOException {
        final String template = IoUtils.read(in);
        final Matcher matcher = refStart.matcher(template);
        int endPos = -1;
        while (matcher.find(endPos + 1)) {
            final String name = matcher.group(1);
            if (!snippets.containsKey(name)) {
                throw new IllegalArgumentException("Snippet '" + name + "' not defined.");
            }
            if (refs) {
                out.write(template.substring(endPos + 1, matcher.start()));
                endPos = matcher.end();
            } else {
                out.write(template.substring(endPos + 1, matcher.end()));
                endPos = template.indexOf(refEnd, matcher.end());
                if (endPos < 0) {
                    throw new IllegalArgumentException("No refEnd marker found for refStart '" + template.substring(matcher.start(), matcher.end()) + "'");
                }
            }
            out.write(snippets.get(name));
        }
        out.write(template.substring(endPos));
    }
}
