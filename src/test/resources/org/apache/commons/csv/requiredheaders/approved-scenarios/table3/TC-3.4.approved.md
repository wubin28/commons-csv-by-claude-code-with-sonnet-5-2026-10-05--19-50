# TC-3.4: ignoreHeaderCase=false（默认）时，大小写不同 = 视为缺失

## Input
Header: (auto)
RequiredHeaders: Currency
IgnoreHeaderCase: false
CSV:
```
currency
USD
```

## Expected
Result: FAIL
ExceptionType: IllegalArgumentException
ExceptionMessage: Missing required header name(s): [Currency]. Header names found: [currency]
Note: 精确匹配模式下 "Currency" 和 "currency" 是两个不同的名字，不会被静默地当成同一列，回归既有精确匹配语义。
