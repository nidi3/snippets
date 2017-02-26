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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Snippets {
    final Map<String, String> snippets;
    private final Pattern snippetStart;
    private final String snippetEnd;
    private final Pattern refStart;
    private final String refEnd;
    private final String prefix;
    private final String postfix;
    private final int tabSize;

    public Snippets(String snippetStart, String snippetEnd, String refStart, String refEnd, int tabSize) {
        this(markPattern(snippetStart), snippetEnd, markPattern(refStart), refEnd, tabSize, "", "", Collections.<String, String>emptyMap());
    }

    private Snippets(Pattern snippetStart, String snippetEnd, Pattern refStart, String refEnd, int tabSize, String prefix, String postfix, Map<String, String> snippets) {
        this.snippetStart = snippetStart;
        this.snippetEnd = snippetEnd;
        this.refStart = refStart;
        this.refEnd = refEnd;
        this.prefix = prefix;
        this.postfix = postfix;
        this.snippets = snippets;
        this.tabSize = tabSize;
    }

    private static Pattern markPattern(String s) {
        final int pos = s.indexOf("%name");
        if (pos < 0) {
            throw new IllegalArgumentException("Start pattern must contain '%name'");
        }
        return Pattern.compile("\\Q" + s.substring(0, pos) + "\\E([A-Za-z0-9]+)\\Q" + s.substring(pos + 5));
    }

    public Snippets prefix(String prefix) {
        return new Snippets(snippetStart, snippetEnd, refStart, refEnd, tabSize, prefix, postfix, snippets);
    }

    public Snippets postfix(String postfix) {
        return new Snippets(snippetStart, snippetEnd, refStart, refEnd, tabSize, prefix, postfix, snippets);
    }

    public Snippets withFile(File file, String encoding) throws IOException {
        try (final Reader in = new InputStreamReader(new FileInputStream(file), encoding)) {
            return new Snippets(snippetStart, snippetEnd, refStart, refEnd, tabSize, prefix, postfix, parse(in, new HashMap<>(snippets)));
        }
    }

    public Snippets withString(String code) {
        try {
            return new Snippets(snippetStart, snippetEnd, refStart, refEnd, tabSize, prefix, postfix, parse(new StringReader(code), new HashMap<>(snippets)));
        } catch (IOException e) {
            throw new AssertionError("Cannot happen", e);
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

    public int size() {
        return snippets.size();
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
            throw new AssertionError("Cannot happen", e);
        }
    }

    private Map<String, String> parse(Reader in, Map<String, String> snippets) throws IOException {
        final String code = IoUtils.read(in);
        final Matcher matcher = snippetStart.matcher(code);
        int endPos = 0;
        while (matcher.find(endPos)) {
            endPos = code.indexOf(snippetEnd, matcher.end());
            if (endPos < 0) {
                throw new IllegalArgumentException("No snippetEnd marker found for snippetStart '" + code.substring(matcher.start(), matcher.end()) + "'");
            }
            final String name = matcher.group(1);
            if (snippets.containsKey(name)) {
                throw new IllegalArgumentException("Snippet with name '" + name + "' already existing.");
            }
            snippets.put(name, trim(code.substring(matcher.end(), endPos)));
            endPos += snippetEnd.length();
        }
        return snippets;
    }

    private String trim(String s) {
        final String[] lines = makeLines(s);
        final int minIndent = findMinimalIndent(lines);
        final StringBuilder sb = new StringBuilder();
        for (final String line : lines) {
            sb.append(line.length() >= minIndent ? line.substring(minIndent) : line).append('\n');
        }
        return s.endsWith("\n") ? sb.toString() : sb.substring(0, sb.length() - 1);
    }

    private String[] makeLines(String s) {
        final String[] lines = s.split("\n");
        if (tabSize > 0) {
            for (int i = 0; i < lines.length; i++) {
                lines[i] = lines[i].replace("\t", tab());
            }
        }
        return lines;
    }

    private int findMinimalIndent(String[] lines) {
        int minIndent = 1000;
        for (final String line : lines) {
            int pos = 0;
            while (pos < line.length() && line.charAt(pos) <= ' ') {
                pos++;
            }
            if (pos < line.length() && pos < minIndent) {
                minIndent = pos;
            }
        }
        return minIndent;
    }

    private String tab() {
        final StringBuilder s = new StringBuilder();
        while (s.length() < tabSize) {
            s.append(' ');
        }
        return s.toString();
    }

    private void replace(Reader in, Writer out, boolean refs) throws IOException {
        final String template = IoUtils.read(in);
        final Matcher matcher = refStart.matcher(template);
        int matchPos = 0;
        int appendPos = 0;
        while (matcher.find(matchPos)) {
            final String name = matcher.group(1);
            if (!snippets.containsKey(name)) {
                throw new IllegalArgumentException("Snippet '" + name + "' not defined.");
            }
            if (refs) {
                out.write(template.substring(appendPos, matcher.start()));
                matchPos = appendPos = matcher.end();
            } else {
                out.write(template.substring(appendPos, matcher.end()));
                appendPos = template.indexOf(refEnd, matcher.end());
                if (appendPos < 0) {
                    throw new IllegalArgumentException("No refEnd marker found for refStart '" + template.substring(matcher.start(), matcher.end()) + "'");
                }
                matchPos = appendPos + refEnd.length();
            }
            out.write(prefix);
            out.write(snippets.get(name));
            out.write(postfix);
        }
        out.write(template.substring(appendPos));
    }
}
