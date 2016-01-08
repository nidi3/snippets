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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;

/**
 *
 */
@Mojo(name = "snippets", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class SnippetsMojo extends AbstractMojo {
    @Parameter(property = "snippets.snippetStart", defaultValue = "//## %name")
    private String snippetStart;
    @Parameter(property = "snippets.snippetEnd", defaultValue = "//##")
    private String snippetEnd;
    @Parameter(property = "snippets.refStart", defaultValue = "[//]: # (%name)")
    private String refStart;
    @Parameter(property = "snippets.refEnd", defaultValue = "[//]: # (end)")
    private String refEnd;
    @Parameter(property = "snippets.prefix", defaultValue = "")
    private String prefix;
    @Parameter(property = "snippets.postfix", defaultValue = "")
    private String postfix;

    /**
     * A list of files/directories containing snippets.
     */
    @Parameter(property = "snippets.inputs", required = true)
    private File[] inputs;

    /**
     * A list of files/directories containing files referencing snippets.
     */
    @Parameter(property = "snippets.outputs", required = true)
    private File[] outputs;

    @Parameter(property = "snippets.encoding", defaultValue = "UTF-8")
    private String encoding;

    /**
     * If the snippets should be replaced in the containing files or new files should be generated.
     */
    @Parameter(property = "snippets.replace", defaultValue = "true")
    private boolean replace;

    /**
     * If replace = false, the directory for the replaced files.
     */
    @Parameter(property = "snippets.target.dir", defaultValue = "target")
    private File target;

    /**
     * If replace = false, the file extension to be used.
     */
    @Parameter(property = "snippets.target.extension", defaultValue = "out")
    private String extension;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (inputs.length == 0) {
            throw new MojoFailureException("Missing parameter 'inputs'");
        }
        if (outputs.length == 0) {
            throw new MojoFailureException("Missing parameter 'outputs'");
        }
        try {
            final Snippets snippets = readInputs(new Snippets(snippetStart, snippetEnd, refStart, refEnd)
                    .prefix(unescape(prefix)).postfix(unescape(postfix)));
            getLog().info("Found " + snippets.size() + " snippets.");
            createOutputs(snippets);
        } catch (IOException e) {
            throw new MojoFailureException("Could not replace snippets.", e);
        }
    }

    private String unescape(String s) {
        return s.replaceAll("\\\\n", "\n").replaceAll("\\\\r", "\r").replaceAll("\\\\t", "\t");
    }

    private Snippets readInputs(Snippets snippets) throws IOException {
        for (File input : inputs) {
            if (input.isDirectory()) {
                for (File in : input.listFiles()) {
                    snippets = doRead(snippets, in);
                }
            } else {
                snippets = doRead(snippets, input);
            }
        }
        return snippets;
    }

    private Snippets doRead(Snippets snippets, File file) throws IOException {
        getLog().info("Reading " + file);
        return snippets.withFile(file, encoding);
    }

    private void createOutputs(Snippets snippets) throws IOException {
        for (File output : outputs) {
            if (output.isDirectory()) {
                for (File out : output.listFiles()) {
                    doReplace(snippets, out);
                }
            } else {
                doReplace(snippets, output);
            }
        }
    }

    private void doReplace(Snippets snippets, File output) throws IOException {
        getLog().info("Replacing " + output.getName());
        if (replace) {
            snippets.replaceSnippets(output, encoding);
        } else {
            snippets.replaceRefs(output, outputFor(output), encoding);
        }
    }

    private File outputFor(File file) {
        final String name = file.getName();
        final int pos = name.lastIndexOf('.');
        return new File(target, name.substring(0, pos + 1) + extension);
    }
}
