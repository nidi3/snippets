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

import guru.nidi.codeassert.config.For;
import guru.nidi.codeassert.jacoco.CoverageCollector;
import guru.nidi.codeassert.jacoco.JacocoAnalyzer;
import org.junit.Test;

import static guru.nidi.codeassert.jacoco.CoverageType.*;
import static guru.nidi.codeassert.junit.CodeAssertMatchers.hasEnoughCoverage;
import static org.junit.Assert.assertThat;

public class CodeCoverage {
    @Test
    public void coverage() {
        final JacocoAnalyzer analyzer = new JacocoAnalyzer(new CoverageCollector(BRANCH, LINE, METHOD)
                .just(For.global().setMinima(70, 90, 90))
                .just(For.allPackages().setMinima(70, 90, 90))
        );
        assertThat("enough code coverage", analyzer.analyze(), hasEnoughCoverage());
    }
}
