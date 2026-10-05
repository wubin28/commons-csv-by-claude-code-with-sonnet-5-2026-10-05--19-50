# TC-3.1c: 重复列名在 ALLOW_ALL 模式下被允许，必需列检查正常执行并通过

## Input
Header: (auto)
RequiredHeaders: currency
DuplicateHeaderMode: ALLOW_ALL
CSV:
```
currency,currency
1,2
```

## Expected
Result: OK
Get: currency=2
Note: headerMap 中 "currency" 键最终指向最后一次出现的列（索引 1），所以 record.get("currency") 取到的是 "2"，证明 ALLOW_ALL 不会意外影响必需列判断。
