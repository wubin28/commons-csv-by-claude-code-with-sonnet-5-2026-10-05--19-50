# TC-3.2: 大小写相同，ignoreHeaderCase 取值不影响结果

## Input
Header: (auto)
RequiredHeaders: currency
IgnoreHeaderCase: false,true
CSV:
```
currency
USD
```

## Expected
Result: OK
Get: currency=USD
Note: IgnoreHeaderCase 分别取 false 和 true 两个值各运行一次本用例，两次都应成功——验证"没有大小写差异时，这个开关确实无关"（对应决策表 Step 6 把这两种组合合并成一条规则的依据）。
