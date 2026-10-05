#!/usr/bin/env bash
#
# Fault injection test runner for the 17 "required headers" acceptance tests.
#
# For each row in manifest.tsv:
#   1. inject the fault (literal text replace, via the <id>.orig.txt / <id>.fault.txt pair)
#   2. mvn compile
#   3. mvn test -Dtest=RequiredHeadersApprovedScenariosTest (expected: the fault IS caught)
#   4. extract the surefire result for that test's parameterized index and check it against
#      the expected signal in manifest.tsv (mode + expected_grep)
#   5. git checkout -- <file> to remove the fault
#   6. mvn test again (expected: green again)
#
# One-time verification/learning tool (see 04-1-dialogs.md / 04-2-solutions.md). No CI wiring,
# no pom.xml changes. Safe to delete this whole fault-injection/ directory when done.
#
# Usage:
#   ./fault-injection/run-fault-injection.sh            # run all 17
#   ./fault-injection/run-fault-injection.sh TC-2.3a     # run a single test id

set -u
cd "$(dirname "$0")/.."

MANIFEST="fault-injection/manifest.tsv"
FAULTS_DIR="fault-injection/faults"
REPORT_DIR="fault-injection/report"
TEST_CLASS="RequiredHeadersApprovedScenariosTest"
XML_REPORT="target/surefire-reports/TEST-org.apache.commons.csv.requiredheaders.RequiredHeadersApprovedScenariosTest.xml"
ONLY_ID="${1:-}"

mkdir -p "$REPORT_DIR"
RESULTS_FILE="$REPORT_DIR/results.tsv"
echo -e "id\tdisplay_name\tinject_result\trevert_result" > "$RESULTS_FILE"

# Extract the surefire XML block for one parameterized testcase index.
# Prints either the self-closed "<testcase .../>" line (pass) or the whole
# "<testcase ...> ... </testcase>" block (failure/error), whichever applies.
extract_testcase() {
    local idx="$1"
    awk -v idx="$idx" '
        BEGIN { inside=0; captured="" }
        {
            if ($0 ~ ("<testcase name=\"requiredHeadersApprovedScenario\\(ApprovedScenarioFixture\\)\\[" idx "\\]\"")) {
                inside=1
            }
            if (inside) {
                captured = captured $0 "\n"
                if ($0 ~ /\/>[[:space:]]*$/) { printf "%s", captured; exit }
                if ($0 ~ /<\/testcase>/) { printf "%s", captured; exit }
            }
        }
    ' "$XML_REPORT"
}

# true (0) if the extracted testcase block represents a pass (self-closed, no failure/error child)
is_green() {
    local block="$1"
    if echo "$block" | grep -q "<failure" || echo "$block" | grep -q "<error"; then
        return 1
    fi
    return 0
}

run_mvn_test() {
    mvn -q -Drat.skip=true -Dtest="$TEST_CLASS" test > "$REPORT_DIR/last-run.log" 2>&1
}

run_mvn_compile() {
    mvn -q -Drat.skip=true compile > "$REPORT_DIR/last-compile.log" 2>&1
}

process_row() {
    local id="$1" display_name="$2" idx="$3" prod_file="$4" fault_id="$5" mode="$6" expected_grep="$7"

    echo "============================================================"
    echo "[$id] $display_name"
    echo "  production file: $prod_file (testcase index [$idx])"
    echo "  mode: $mode"

    if [[ -n "$(git status --porcelain -- "$prod_file")" ]]; then
        echo "  ABORT: $prod_file already has uncommitted changes before fault injection. Not touching it."
        echo -e "${id}\t${display_name}\tABORT_DIRTY_TREE\t-" >> "$RESULTS_FILE"
        return
    fi

    local orig_file="$FAULTS_DIR/${fault_id}.orig.txt"
    local fault_file="$FAULTS_DIR/${fault_id}.fault.txt"

    # --- 1a. safety check: the anchor text must appear exactly once (read-only) ---
    local occurrences
    occurrences="$(ORIG_FILE="$orig_file" perl -0777 -ne '
        BEGIN { local $/; open(my $fh, "<", $ENV{ORIG_FILE}) or die $!; $orig = <$fh>; }
        my $n = () = /\Q$orig\E/g;
        print $n;
    ' "$prod_file")"

    if [[ "$occurrences" != "1" ]]; then
        echo "  ABORT: expected exactly 1 occurrence of the anchor text in $prod_file, found $occurrences. Not touching the file."
        echo -e "${id}\t${display_name}\tANCHOR_NOT_FOUND\t-" >> "$RESULTS_FILE"
        return
    fi

    # --- 1b. inject fault (safe: uniqueness already confirmed above) ---
    ORIG_FILE="$orig_file" FAULT_FILE="$fault_file" perl -0777 -i -pe '
        BEGIN {
            local $/;
            open(my $fh1, "<", $ENV{ORIG_FILE}) or die $!;
            $orig = <$fh1>;
            open(my $fh2, "<", $ENV{FAULT_FILE}) or die $!;
            $fault = <$fh2>;
        }
        s/\Q$orig\E/$fault/;
    ' "$prod_file"

    if ! run_mvn_compile; then
        echo "  COMPILE FAILED after fault injection — see $REPORT_DIR/last-compile.log"
        tail -20 "$REPORT_DIR/last-compile.log"
        git checkout -- "$prod_file"
        echo -e "${id}\t${display_name}\tCOMPILE_FAILED\t-" >> "$RESULTS_FILE"
        return
    fi

    # --- 2. run tests with fault active ---
    run_mvn_test
    local block
    block="$(extract_testcase "$idx")"
    local inject_result

    if [[ "$mode" == "vacuous_control" ]]; then
        if is_green "$block"; then
            echo "  OK (control case, as expected): fault did NOT turn this test red."
            inject_result="OK_CONTROL_UNCHANGED"
        else
            echo "  UNEXPECTED: this was marked as a vacuous control case, but the test DID turn red:"
            echo "$block" | head -5
            inject_result="UNEXPECTED_RED"
        fi
    else
        if is_green "$block"; then
            echo "  FAIL: expected this test to turn RED after fault injection, but it stayed GREEN."
            inject_result="EXPECTED_RED_GOT_GREEN"
        elif echo "$block" | grep -qF "$expected_grep"; then
            echo "  PASS: test turned RED with the expected message:"
            echo "$block" | grep -oF "$expected_grep" | head -1 | sed 's/^/    -> /'
            inject_result="PASS_RED_WITH_EXPECTED_MESSAGE"
        else
            echo "  FAIL: test turned RED but WITHOUT the expected message fragment ('$expected_grep'):"
            echo "$block" | head -5
            inject_result="RED_BUT_WRONG_MESSAGE"
        fi
    fi

    # --- 3. revert fault ---
    git checkout -- "$prod_file"
    run_mvn_test
    block="$(extract_testcase "$idx")"
    local revert_result
    if is_green "$block"; then
        echo "  PASS: test is GREEN again after reverting the fault."
        revert_result="PASS_GREEN_AFTER_REVERT"
    else
        echo "  FAIL: test is still RED after reverting the fault (unexpected):"
        echo "$block" | head -5
        revert_result="STILL_RED_AFTER_REVERT"
    fi

    echo -e "${id}\t${display_name}\t${inject_result}\t${revert_result}" >> "$RESULTS_FILE"
}

tail -n +2 "$MANIFEST" | while IFS=$'\t' read -r id display_name idx prod_file fault_id mode expected_grep; do
    [[ -n "$ONLY_ID" && "$id" != "$ONLY_ID" ]] && continue
    process_row "$id" "$display_name" "$idx" "$prod_file" "$fault_id" "$mode" "$expected_grep"
done

echo "============================================================"
echo "Done. Results written to $RESULTS_FILE"
column -t -s $'\t' "$RESULTS_FILE"
echo "============================================================"
total=$(tail -n +2 "$RESULTS_FILE" | wc -l | tr -d ' ')
ok=$(tail -n +2 "$RESULTS_FILE" | awk -F'\t' '($3 ~ /^(PASS_|OK_)/) && ($4 ~ /^PASS_/)' | wc -l | tr -d ' ')
echo "Summary: $ok / $total test cases fully verified (fault correctly caught + green after revert, or confirmed as a control case)."
