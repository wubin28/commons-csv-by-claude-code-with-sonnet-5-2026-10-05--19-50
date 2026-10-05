# TC-2.3a: 缺 1 个必需列

## Input
Header: (auto)
RequiredHeaders: date,amount,currency
CSV:
```
date,amount
2026-01-01,100
```

## Expected
Result: FAIL
ExceptionType: IllegalArgumentException
ExceptionMessage: Missing required header name(s): [currency]. Header names found: [date, amount]
Note: 该异常必须在 CSVParser.parse(...) 调用本身抛出，而不是等到后续 iterator().next() 才抛出——这是 01-3 §1 需求"缺列即刻失败"的核心约束。本测试通过断言 parse(...) 这一调用本身抛出异常来验证这一点（见 ApprovedScenarioExecutor#runParsingScenario）。
