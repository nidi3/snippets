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

import guru.nidi.codeassert.config.AnalyzerConfig;
import guru.nidi.codeassert.config.In;
import guru.nidi.codeassert.dependency.DependencyRule;
import guru.nidi.codeassert.dependency.DependencyRuler;
import guru.nidi.codeassert.model.ModelAnalyzer;
import guru.nidi.codeassert.pmd.CpdAnalyzer;
import guru.nidi.codeassert.pmd.MatchCollector;
import guru.nidi.codeassert.pmd.PmdAnalyzer;
import guru.nidi.codeassert.pmd.ViolationCollector;
import org.junit.Test;

import static guru.nidi.codeassert.config.AnalyzerConfig.mavenMainAndTestClasses;
import static guru.nidi.codeassert.config.PackageCollector.allPackages;
import static guru.nidi.codeassert.dependency.DependencyMatchers.hasNoCycles;
import static guru.nidi.codeassert.dependency.DependencyMatchers.matchesExactly;
import static guru.nidi.codeassert.dependency.DependencyRules.denyAll;
import static guru.nidi.codeassert.pmd.PmdMatchers.hasNoDuplications;
import static guru.nidi.codeassert.pmd.PmdMatchers.hasNoPmdViolations;
import static guru.nidi.codeassert.pmd.Rulesets.*;
import static org.junit.Assert.assertThat;

/**
 *
 */
public class CodeTest {
    @Test
    public void noCycles() {
        assertThat(new ModelAnalyzer(mavenMainAndTestClasses()), hasNoCycles());
    }

    @Test
    public void dependencies() {
        class GuruNidiSnippets implements DependencyRuler {
            DependencyRule $self;

            @Override
            public void defineRules() {

            }
        }
        final AnalyzerConfig config = mavenMainAndTestClasses().collecting(allPackages().including("guru.nidi.snippets*").excludingRest());
        assertThat(new ModelAnalyzer(config), matchesExactly(denyAll().withRules(new GuruNidiSnippets())));
    }

    @Test
    public void pmd() {
        final PmdAnalyzer analyzer = new PmdAnalyzer(mavenMainAndTestClasses(), new ViolationCollector()
                .because("I don't agree", In.everywhere()
                        .ignore("JUnitAssertionsShouldIncludeMessage", "MethodArgumentCouldBeFinal", "AvoidFieldNameMatchingTypeName", "UncommentedEmptyMethodBody", "AbstractNaming"))
                .because("It's in a test", In.locs("*Test", "*Test$*")
                        .ignore("AvoidDollarSigns", "TooManyStaticImports", "AvoidDuplicateLiterals")))
                .withRuleSets(basic(), braces(), design(), empty(), exceptions(), imports(), junit(),
                        naming().variableLen(1, 15), optimizations(), strings(), sunSecure(), typeResolution(), unnecessary(), unused());
        assertThat(analyzer, hasNoPmdViolations());
    }

    @Test
    public void cpd() {
        assertThat(new CpdAnalyzer(mavenMainAndTestClasses(), 20, new MatchCollector()), hasNoDuplications());
    }
}
