# TC-3.3: ignoreHeaderCase=true 时，必需列比较忽略大小写

## Input
Header: (auto)
RequiredHeaders: Currency
IgnoreHeaderCase: true
CSV:
```
currency
USD
```

## Expected
Result: OK
Get: Currency=USD
Note: 必需列声明为 "Currency"，实际表头是 "currency"；ignoreHeaderCase=true 使必需列比较复用 headerMap 的大小写不敏感语义（ADR 决策 2）。
