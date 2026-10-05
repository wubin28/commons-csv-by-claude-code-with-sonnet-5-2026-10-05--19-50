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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Reads a {@code *.approved.md} fixture file (the "Approved Scenarios" pattern: input and expected output combined in one
 * human-readable file) into an {@link ApprovedScenarioFixture}.
 *
 * <p>Recognized shape (see any file under {@code src/test/resources/.../approved-scenarios/table*} for real examples):</p>
 *
 * <pre>
 * # TC-x.y: &lt;title&gt;
 *
 * ## Input
 * Key: value
 * ...
 * CSV:
 * ```
 * &lt;raw CSV text, only present for CSVParser-level scenarios&gt;
 * ```
 *
 * ## Expected
 * Result: OK | FAIL
 * Key: value
 * Get: field=value   (repeatable)
 * </pre>
 *
 * This parser is intentionally dumb: it does not know what "Header" or "RequiredHeaders" mean. That knowledge lives in
 * {@link ApprovedScenarioExecutor}, which is the only place a change to the DSL's semantics should ever require a code change.
 */
final class ApprovedScenarioParser {

    private ApprovedScenarioParser() {
    }

    static ApprovedScenarioFixture parse(final Path file) throws IOException {
        final List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        int i = 0;
        while (i < lines.size() && lines.get(i).trim().isEmpty()) {
            i++;
        }
        final String heading = lines.get(i).trim();
        if (!heading.startsWith("# ")) {
            throw new IllegalStateException("Fixture " + file + " must start with a top-level '# TC-x.y: title' heading");
        }
        final String titleLine = heading.substring(2).trim();
        final String id = titleLine.contains(":") ? titleLine.substring(0, titleLine.indexOf(':')).trim() : titleLine;
        final ApprovedScenarioFixture fixture = new ApprovedScenarioFixture(id, titleLine);
        i++;

        String currentSection = null;
        while (i < lines.size()) {
            final String line = lines.get(i);
            final String trimmed = line.trim();
            if (trimmed.equals("## Input")) {
                currentSection = "Input";
                i++;
                continue;
            }
            if (trimmed.equals("## Expected")) {
                currentSection = "Expected";
                i++;
                continue;
            }
            if (trimmed.isEmpty()) {
                i++;
                continue;
            }
            if ("Input".equals(currentSection) && trimmed.equals("CSV:")) {
                i++; // consume "CSV:"
                i++; // consume opening ```` ``` ````
                final StringBuilder csv = new StringBuilder();
                while (i < lines.size() && !lines.get(i).trim().equals("```")) {
                    csv.append(lines.get(i)).append('\n');
                    i++;
                }
                i++; // consume closing ```` ``` ````
                fixture.setCsv(csv.toString());
                continue;
            }
            final int colon = trimmed.indexOf(':');
            if (colon < 0) {
                throw new IllegalStateException("Fixture " + file + " has an unparseable line: " + line);
            }
            final String key = trimmed.substring(0, colon).trim();
            final String value = trimmed.substring(colon + 1).trim();
            if ("Input".equals(currentSection)) {
                fixture.getInput().put(key, value);
            } else if ("Expected".equals(currentSection)) {
                if ("Get".equals(key)) {
                    fixture.getGetAssertions().add(value);
                } else {
                    fixture.getExpected().put(key, value);
                }
            } else {
                throw new IllegalStateException("Fixture " + file + " has content outside of '## Input'/'## Expected': " + line);
            }
            i++;
        }
        return fixture;
    }
}
