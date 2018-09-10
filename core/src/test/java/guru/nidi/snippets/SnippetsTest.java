/*
 * Copyright © 2014 Stefan Niederhauser (nidin@gmx.ch)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package guru.nidi.snippets;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SnippetsTest {
    private final Snippets s = new Snippets("##%name", "##end", "##%name", "##end", 0)
            .withString("line\n##s1\n snippet \n##end\nline");

    @Test
    void noNameInStart() {
        assertThrows(IllegalArgumentException.class, () -> new Snippets("start", "end", "#%name", "end", 0));
    }

    @Test
    void noNameInRef() {
        assertThrows(IllegalArgumentException.class, () -> new Snippets("#%name", "end", "ref", "end", 0));
    }

    @Test
    void stringParseOk() {
        assertEquals(map("s1", "\nsnippet \n"), s.snippets);
    }

    @Test
    void missingEnd() {
        assertThrows(IllegalArgumentException.class, () ->
                new Snippets("#%name", "#", "#%name", "#", 0).withString("line\n#s1 snippet \nline"));
    }

    @Test
    void duplicateKey() {
        assertThrows(IllegalArgumentException.class, () ->
                new Snippets("#%name", "#", "#%name", "#", 0).withString("line\n#s1 snippet ##s1 #"));
    }

    @Test
    void wrongRef() {
        assertEquals("a\\n##xxx\\nblue\\n##end\\nb", s.replaceSnippets("a\\n##xxx\\nblue\\n##end\\nb"));
    }

    @Test
    void wrongRef2() {
        assertEquals("This is code:\n\nfooter", s.replaceRefs("This is code:\n##s2\nfooter"));
    }

    @Test
    void fileParseOk() throws IOException {
        final Snippets s = new Snippets("//*%name", "//*", "#%name", "#", 0)
                .withFile(new File("src/test/java/guru/nidi/snippets/SnippetsCode.java"), "utf-8");
        assertEquals(map("main", "\npublic static void main(String... args) {\n    System.exit(1);\n}\n"), s.snippets);
    }

    @Test
    void tabsize() {
        final Snippets s = new Snippets("//*%name", "//*", "#%name", "#", 2)
                .withString("//*x\n\tfirst\n\t\tsecond\n//*");
        assertEquals(map("x", "\nfirst\n  second\n"), s.snippets);
    }

    @Test
    void stringReplaceRefOk() {
        assertEquals("This is code:\n\nsnippet \n\nfooter", s.replaceRefs("This is code:\n##s1\nfooter"));
    }

    @Test
    void stringReplaceSnippetsOk() {
        assertEquals("This is code:\n##s1\nsnippet \n##end\nfooter", s.replaceSnippets("This is code:\n##s1\nold code##end\nfooter"));
    }

    @Test
    void prefixPostfix() {
        assertEquals("This is code:\npre\nsnippet \npost\nfooter", s.prefix("pre").postfix("post").replaceRefs("This is code:\n##s1\nfooter"));
    }

    @Test
    void fileReplaceRefOk() throws IOException {
        final File output = new File("target/out/simple.out");

        s.withString("##end---##end").replaceRefs(new File("src/test/resources/guru/nidi/snippets/simple.template"), output, "utf-8");
        assertEquals("This is code:\n\nsnippet \n\nold\n---\nfooter", read(output));
    }

    @Test
    void fileReplaceSnippetsOk() throws IOException {
        final File out = new File("target/out/replace.out");
        Files.copy(new File("src/test/resources/guru/nidi/snippets/simple.template").toPath(), new FileOutputStream(out));

        s.replaceSnippets(out, "utf-8");
        assertEquals("This is code:\n##s1\nsnippet \n##end\nfooter", read(out));
    }

    @Test
    void noEndRef() {
        assertThrows(IllegalArgumentException.class, () -> s.replaceSnippets("This is code:\n##s1\nold code\nfooter"));
    }

    private static Map<String, String> map(String... keysValues) {
        final Map<String, String> res = new HashMap<>();
        for (int i = 0; i < keysValues.length; i += 2) {
            res.put(keysValues[i], keysValues[i + 1]);
        }
        return res;
    }

    private String read(File f) throws IOException {
        try (final Reader in = new InputStreamReader(new FileInputStream(f), UTF_8)) {
            return IoUtils.read(in);
        }
    }

}
