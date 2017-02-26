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

import edu.umd.cs.findbugs.Priorities;
import guru.nidi.codeassert.config.AnalyzerConfig;
import guru.nidi.codeassert.config.In;
import guru.nidi.codeassert.dependency.DependencyRule;
import guru.nidi.codeassert.dependency.DependencyRuler;
import guru.nidi.codeassert.dependency.DependencyRules;
import guru.nidi.codeassert.findbugs.BugCollector;
import guru.nidi.codeassert.findbugs.FindBugsAnalyzer;
import guru.nidi.codeassert.findbugs.FindBugsResult;
import guru.nidi.codeassert.junit.CodeAssertTest;
import guru.nidi.codeassert.junit.PredefConfig;
import guru.nidi.codeassert.model.ModelAnalyzer;
import guru.nidi.codeassert.model.ModelResult;
import guru.nidi.codeassert.pmd.*;
import org.junit.Test;

import static guru.nidi.codeassert.junit.CodeAssertMatchers.packagesMatchExactly;
import static org.junit.Assert.assertThat;

public class CodeAnalysisTest extends CodeAssertTest {

    @Test
    public void dependencies() {
        class GuruNidiSnippets extends DependencyRuler {
            DependencyRule $self;

            @Override
            public void defineRules() {
            }
        }
        final DependencyRules rules = DependencyRules.denyAll()
                .withExternals("java*")
                .withRelativeRules(new GuruNidiSnippets());
        assertThat(modelResult(), packagesMatchExactly(rules));
    }

    @Override
    protected ModelResult analyzeModel() {
        return new ModelAnalyzer(AnalyzerConfig.maven().main()).analyze();
    }

    @Override
    protected FindBugsResult analyzeFindBugs() {
        return new FindBugsAnalyzer(AnalyzerConfig.maven().mainAndTest(), new BugCollector()
                .apply(PredefConfig.dependencyTestIgnore(CodeAnalysisTest.class))
                .minPriority(Priorities.NORMAL_PRIORITY)
                .because("It's a test", In.locs("*Test").ignore("RV_RETURN_VALUE_IGNORED_INFERRED"))
                .because("It's magic", In.clazz(CodeAnalysisTest.class).ignore("UUF_UNUSED_FIELD"))
                .because("It's ok", In.clazz(Snippets.class).ignore("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE"))
        ).analyze();
    }

    @Override
    protected PmdResult analyzePmd() {
        return new PmdAnalyzer(AnalyzerConfig.maven().mainAndTest(), new PmdViolationCollector()
                .apply(PredefConfig.dependencyTestIgnore(CodeAnalysisTest.class))
                .apply(PredefConfig.minimalPmdIgnore())
                .because("I don't agree", In.clazz(Snippets.class).ignore("UseVarargs"))
        ).withRulesets(PredefConfig.defaultPmdRulesets()).analyze();

    }

    @Override
    protected CpdResult analyzeCpd() {
        return new CpdAnalyzer(AnalyzerConfig.maven().main(), 30, new CpdMatchCollector()).analyze();
    }
}

