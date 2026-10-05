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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVFormat.Builder;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.DuplicateHeaderMode;

/**
 * Turns one {@link ApprovedScenarioFixture} into real {@code CSVFormat}/{@code CSVParser} calls and asserts the actual outcome
 * matches the fixture's {@code ## Expected} section.
 *
 * <p>This is the "single test execution logic" that the Approved Scenarios pattern says must be validated once; after that,
 * reviewing a new {@code .approved.md} file is enough. Every one of the 17 required-headers acceptance tests runs through this
 * same {@link #run(ApprovedScenarioFixture)} method — see {@code RequiredHeadersApprovedScenariosTest} for the {@code @TestFactory}
 * that wires a {@code DynamicTest} per fixture file to it.</p>
 */
final class ApprovedScenarioExecutor {

    private static final String NONE = "(none)";
    private static final String EMPTY = "(empty)";
    private static final String AUTO = "(auto)";
    private static final String DEFAULT = "(default)";
    private static final String NULL_TOKEN = "<NULL>";
    private static final String BLANK_TOKEN = "<BLANK>";

    void run(final ApprovedScenarioFixture fixture) throws IOException {
        final String ignoreHeaderCaseField = fixture.getInput().get("IgnoreHeaderCase");
        if (ignoreHeaderCaseField != null && ignoreHeaderCaseField.contains(",")) {
            // TC-3.2: one fixture, several IgnoreHeaderCase values, same Expected for each (see decision-table Step 6).
            for (final String value : ignoreHeaderCaseField.split(",")) {
                runOnce(fixture, value.trim());
            }
        } else {
            runOnce(fixture, ignoreHeaderCaseField);
        }
    }

    private void runOnce(final ApprovedScenarioFixture fixture, final String ignoreHeaderCaseOverride) throws IOException {
        final Builder builder = CSVFormat.DEFAULT.builder();
        applyHeader(builder, fixture.getInput().get("Header"));
        applyRequiredHeaders(builder, fixture.getInput().get("RequiredHeaders"));
        applyOptionalBoolean(builder::setIgnoreHeaderCase, ignoreHeaderCaseOverride);
        applyOptionalDuplicateHeaderMode(builder, fixture.getInput().get("DuplicateHeaderMode"));
        applyOptionalBoolean(builder::setAllowMissingColumnNames, fixture.getInput().get("AllowMissingColumnNames"));

        if (fixture.getCsv() == null) {
            runConstructionScenario(builder, fixture);
        } else {
            runParsingScenario(builder, fixture);
        }
    }

    // ---- CSVFormat construction-time scenarios (decision table 1) ----

    private void runConstructionScenario(final Builder builder, final ApprovedScenarioFixture fixture) {
        final Map<String, String> expected = fixture.getExpected();
        if ("OK".equals(expected.get("Result"))) {
            final CSVFormat format = builder.get();
            final String expectedRequiredHeaders = expected.get("RequiredHeaders");
            if (NONE.equals(expectedRequiredHeaders)) {
                assertNull(format.getRequiredHeaders(), fixture.displayName());
            } else if (expectedRequiredHeaders != null) {
                assertArrayEquals(parseTokens(expectedRequiredHeaders), format.getRequiredHeaders(), fixture.displayName());
            }
        } else {
            final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, builder::get, fixture.displayName());
            assertEquals(expected.get("ExceptionMessage"), thrown.getMessage(), fixture.displayName());
        }
    }

    // ---- CSVParser parsing-time scenarios (decision tables 2 and 3) ----

    private void runParsingScenario(final Builder builder, final ApprovedScenarioFixture fixture) throws IOException {
        final CSVFormat format = builder.get();
        final Map<String, String> expected = fixture.getExpected();
        if ("OK".equals(expected.get("Result"))) {
            try (CSVParser parser = CSVParser.parse(fixture.getCsv(), format)) {
                final List<CSVRecord> records = parser.getRecords();
                final String expectedRecordCount = expected.get("RecordCount");
                if (expectedRecordCount != null) {
                    assertEquals(Integer.parseInt(expectedRecordCount), records.size(), fixture.displayName());
                }
                final CSVRecord firstRecord = records.isEmpty() ? null : records.get(0);
                for (final String assertion : fixture.getGetAssertions()) {
                    final int eq = assertion.indexOf('=');
                    final String fieldName = assertion.substring(0, eq);
                    final String expectedValue = assertion.substring(eq + 1);
                    assertEquals(expectedValue, firstRecord.get(fieldName), fixture.displayName() + " / Get " + fieldName);
                }
            }
        } else {
            final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                    () -> CSVParser.parse(fixture.getCsv(), format), fixture.displayName());
            assertEquals(expected.get("ExceptionMessage"), thrown.getMessage(), fixture.displayName());
        }
    }

    // ---- Input DSL -> CSVFormat.Builder translation ----

    private static void applyHeader(final Builder builder, final String value) {
        if (value == null || NONE.equals(value)) {
            return;
        }
        if (AUTO.equals(value)) {
            builder.setHeader();
            return;
        }
        builder.setHeader(parseTokens(value));
    }

    private static void applyRequiredHeaders(final Builder builder, final String value) {
        if (value == null || NONE.equals(value)) {
            return;
        }
        if (EMPTY.equals(value)) {
            builder.setRequiredHeaders();
            return;
        }
        builder.setRequiredHeaders(parseTokens(value));
    }

    private static void applyOptionalBoolean(final Function<Boolean, Builder> setter, final String value) {
        if (value == null || DEFAULT.equals(value)) {
            return;
        }
        setter.apply(Boolean.parseBoolean(value));
    }

    private static void applyOptionalDuplicateHeaderMode(final Builder builder, final String value) {
        if (value == null || DEFAULT.equals(value)) {
            return;
        }
        builder.setDuplicateHeaderMode(DuplicateHeaderMode.valueOf(value));
    }

    private static String[] parseTokens(final String value) {
        if (value == null || NONE.equals(value)) {
            return null;
        }
        if (EMPTY.equals(value)) {
            return new String[0];
        }
        final String[] raw = value.split(",");
        final String[] result = new String[raw.length];
        for (int i = 0; i < raw.length; i++) {
            final String token = raw[i].trim();
            if (NULL_TOKEN.equals(token)) {
                result[i] = null;
            } else if (BLANK_TOKEN.equals(token)) {
                result[i] = "";
            } else {
                result[i] = token;
            }
        }
        return result;
    }
}
