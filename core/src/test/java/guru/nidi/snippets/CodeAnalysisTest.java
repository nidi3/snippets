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

import edu.umd.cs.findbugs.Priorities;
import guru.nidi.codeassert.config.AnalyzerConfig;
import guru.nidi.codeassert.config.In;
import guru.nidi.codeassert.dependency.*;
import guru.nidi.codeassert.findbugs.*;
import guru.nidi.codeassert.junit.CodeAssertJunit5Test;
import guru.nidi.codeassert.junit.PredefConfig;
import guru.nidi.codeassert.pmd.*;

class CodeAnalysisTest extends CodeAssertJunit5Test {

    @Override
    protected DependencyResult analyzeDependencies() {
        class GuruNidiSnippets extends DependencyRuler {
            @Override
            public void defineRules() {
                base();
            }
        }
        final DependencyRules rules = DependencyRules.denyAll()
                .withExternals("java*")
                .withRelativeRules(new GuruNidiSnippets());
        return new DependencyAnalyzer(AnalyzerConfig.maven().main()).rules(rules).analyze();
    }

    @Override
    protected FindBugsResult analyzeFindBugs() {
        return new FindBugsAnalyzer(AnalyzerConfig.maven().mainAndTest(), new BugCollector()
                .apply(PredefConfig.dependencyTestIgnore(CodeAnalysisTest.class))
                .minPriority(Priorities.NORMAL_PRIORITY)
                .because("It's ok", In.clazz(Snippets.class).ignore("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE"))
        ).analyze();
    }

    @Override
    protected PmdResult analyzePmd() {
        return new PmdAnalyzer(AnalyzerConfig.maven().mainAndTest(), new PmdViolationCollector()
                .apply(PredefConfig.dependencyTestIgnore(CodeAnalysisTest.class))
                .apply(PredefConfig.minimalPmdIgnore())
                .because("I don't agree",
                        In.clazz(Snippets.class).ignore("UseVarargs", "ConfusingTernary"),
                        In.loc("Snippets#replaceSnippets").ignore("PrematureDeclaration"))
        ).withRulesets(PredefConfig.defaultPmdRulesets()).analyze();

    }

    @Override
    protected CpdResult analyzeCpd() {
        return new CpdAnalyzer(AnalyzerConfig.maven().main(), 30, new CpdMatchCollector()).analyze();
    }
}

