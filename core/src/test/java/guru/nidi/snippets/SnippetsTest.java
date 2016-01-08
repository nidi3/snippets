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

import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class SnippetsTest {
    @Test(expected = IllegalArgumentException.class)
    public void noNameInStart() {
        new Snippets("start", "end", "#%name", "end");
    }

    @Test(expected = IllegalArgumentException.class)
    public void noNameInRef() {
        new Snippets("#%name", "end", "ref", "end");
    }

    @Test
    public void stringParseOk() {
        final Snippets s = new Snippets("#%name#", "#", "#%name", "#")
                .withString("line\n#s1# snippet #end\nline");
        assertEquals(map("s1", "snippet "), s.snippets);
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingEnd() {
        new Snippets("#%name", "#", "#%name", "#").withString("line\n#s1 snippet \nline");
    }

    @Test(expected = IllegalArgumentException.class)
    public void duplicateKey() {
        new Snippets("#%name", "#", "#%name", "#").withString("line\n#s1 snippet ##s1 #");
    }

    @Test
    public void fileParseOk() throws IOException {
        final Snippets s = new Snippets("//*%name", "//*", "#%name", "#")
                .withFile(new File("src/test/java/guru/nidi/snippets/SnippetsCode.java"), "utf-8");
        assertEquals(map("main", "\npublic static void main(String[] args) {\n    System.exit(1);\n}\n"), s.snippets);
    }

    @Test
    public void stringReplaceRefOk() {
        final Snippets s = new Snippets("#%name", "#", "#%name", "#")
                .withString("line\n#s1 snippet #end\nline");
        assertEquals("This is code:\nsnippet \nfooter", s.replaceRefs("This is code:\n#s1\nfooter"));
    }

    @Test
    public void stringReplaceSnippetsOk() {
        final Snippets s = new Snippets("#%name", "#", "#%name", "#")
                .withString("line\n#s1\n snippet \n#end\nline");
        assertEquals("This is code:\n#s1\nsnippet \n#\nfooter", s.replaceSnippets("This is code:\n#s1\nold code#\nfooter"));
    }

    @Test
    public void fileReplaceRefOk() throws IOException {
        final Snippets s = new Snippets("#%name", "#", "#%name", "#")
                .withString("line\n#s1 snippet #end\nline");
        final File output = new File("target/out/simple.out");

        s.replaceRefs(new File("src/test/resources/guru/nidi/snippets/simple.template"), output, "utf-8");
        assertEquals("This is code:\nsnippet \nold\n#\nfooter", read(output));
    }

    @Test
    public void fileReplaceSnippetsOk() throws IOException {
        final Snippets s = new Snippets("#%name", "#", "#%name", "#")
                .withString("line\n#s1\n snippet \n#end\nline");
        final File out = new File("target/out/replace.out");
        Files.copy(new File("src/test/resources/guru/nidi/snippets/simple.template").toPath(), new FileOutputStream(out));

        s.replaceSnippets(out, "utf-8");
        assertEquals("This is code:\n#s1\nsnippet \n#\nfooter", read(out));
    }

    @Test(expected = IllegalArgumentException.class)
    public void wrongRef() {
        final Snippets s = new Snippets("#%name#", "#", "#%name", "#")
                .withString("line\n#s1# snippet #end\nline");
        assertEquals("This is code:\nsnippet \nfooter", s.replaceRefs("This is code:\n#s2\nfooter"));
    }

    private static Map<String, String> map(String... keysValues) {
        final Map<String, String> res = new HashMap<>();
        for (int i = 0; i < keysValues.length; i += 2) {
            res.put(keysValues[i], keysValues[i + 1]);
        }
        return res;
    }

    private String read(File f) throws IOException {
        try (final Reader in = new InputStreamReader(new FileInputStream(f), "utf-8")) {
            final StringBuilder s = new StringBuilder();
            char[] buf = new char[1000];
            int read;
            while ((read = in.read(buf)) > 0) {
                s.append(buf, 0, read);
            }
            return s.toString();
        }
    }

}
