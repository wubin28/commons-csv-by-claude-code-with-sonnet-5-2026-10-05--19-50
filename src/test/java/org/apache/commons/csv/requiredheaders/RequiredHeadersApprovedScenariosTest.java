/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.commons.csv.requiredheaders;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Drives the 17 {@code required headers} acceptance tests designed in {@code 02-2-decision-table.md} ("Step 7: Convert Rules to
 * Test Cases") using the Approved Scenarios pattern: each {@code *.approved.md} file under
 * {@code src/test/resources/org/apache/commons/csv/requiredheaders/approved-scenarios/table{1,2,3}} is both the product-manager-
 * readable spec AND the only input {@link ApprovedScenarioExecutor} reads to run the test — there is no separate hand-written
 * assertion per test case to drift out of sync with the markdown.
 *
 * <p>This is a {@code @ParameterizedTest}, not a {@code @TestFactory}/{@code DynamicTest}. Both are "data-driven" in the same
 * sense, but only the former is reliably shown and individually runnable/debuggable as 17 separate child nodes in the VSCode
 * Testing panel ({@code vscjava.vscode-java-test}) — {@code DynamicTest} has long-standing, still-open limitations there
 * (microsoft/vscode-java-test issues #1282 and #1643: dynamic tests either don't appear as children until the parent is run
 * once, or error out when debugged individually from the sidebar). Per-invocation re-run/debug for
 * {@code @ParameterizedTest} was added in extension version 0.35.0; this repository was verified against 0.46.0.</p>
 *
 * <p>Each fixture file's {@link ApprovedScenarioFixture#displayName()} becomes the invocation's display name via
 * {@link Named}, so the Testing panel lists all 17 under this method, each named after its fixture title (e.g.
 * "TC-2.3a: 缺 1 个必需列"), each individually runnable/debuggable. See {@code 03-3-debugging-guide.md} for the
 * breakpoint/variable guide per test.</p>
 */
class RequiredHeadersApprovedScenariosTest {

    private static final String[] GROUPS = {"table1", "table2", "table3"};
    private static final String FIXTURES_ROOT = "src/test/resources/org/apache/commons/csv/requiredheaders/approved-scenarios";

    static Stream<Named<ApprovedScenarioFixture>> fixtures() throws IOException {
        final List<Named<ApprovedScenarioFixture>> fixtures = new ArrayList<>();
        for (final String group : GROUPS) {
            final Path dir = Paths.get(FIXTURES_ROOT, group);
            try (Stream<Path> files = Files.list(dir)) {
                final List<Path> fixtureFiles = files.filter(p -> p.getFileName().toString().endsWith(".approved.md"))
                        .sorted(Comparator.comparing(Path::getFileName)).collect(Collectors.toList());
                for (final Path fixtureFile : fixtureFiles) {
                    final ApprovedScenarioFixture fixture = ApprovedScenarioParser.parse(fixtureFile);
                    fixtures.add(Named.of(fixture.displayName(), fixture));
                }
            }
        }
        return fixtures.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fixtures")
    void requiredHeadersApprovedScenario(final ApprovedScenarioFixture fixture) throws IOException {
        new ApprovedScenarioExecutor().run(fixture);
    }
}
