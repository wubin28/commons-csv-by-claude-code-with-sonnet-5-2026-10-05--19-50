# TC-3.1b: 重复列名（严格模式）抢先于必需列检查报错

## Input
Header: (auto)
RequiredHeaders: currency
DuplicateHeaderMode: DISALLOW
CSV:
```
currency,currency
1,2
```

## Expected
Result: FAIL
ExceptionType: IllegalArgumentException
ExceptionMessage: The header contains a duplicate name: "currency" in [currency, currency]. If this is valid then use CSVFormat.Builder.setDuplicateHeaderMode().
Note: commons-csv 的 CSVFormat.DEFAULT 实际上把 DuplicateHeaderMode 默认设为 ALLOW_ALL（见 CSVFormat.java 中 Builder.create() 的实现），因此本用例显式设置 DuplicateHeaderMode=DISALLOW 来复现"严格模式下重复列名被拒绝"的场景。这与决策表文档里"默认 DISALLOW"的措辞有出入，已对照源码核实：源码里真正的"不设置"状态（null）在行为上等价于 DISALLOW，但唯一公开可用的构造入口 CSVFormat.DEFAULT.builder() 会显式把它设为 ALLOW_ALL，所以要复现严格模式必须显式指定。供评审知悉。
