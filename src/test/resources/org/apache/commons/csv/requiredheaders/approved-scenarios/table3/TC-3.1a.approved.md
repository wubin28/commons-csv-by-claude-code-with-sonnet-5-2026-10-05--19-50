# TC-3.1a: 空列名抢先于必需列检查报错

## Input
Header: (auto)
RequiredHeaders: currency
CSV:
```
date,,currency
2026-01-01,x,USD
```

## Expected
Result: FAIL
ExceptionType: IllegalArgumentException
ExceptionMessage: A header name is missing in [date, , currency]
Note: 消息中不出现 "Missing required header" 字样，用于断言必需列检查确实没有被执行到（ADR 决策 3）。
