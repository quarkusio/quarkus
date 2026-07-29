# 重构 Issue #51207（v2）：Hibernate ORM/Reactive 兼容性测试重构

## 问题

当前 Hibernate ORM/Reactive 兼容性测试套件（`io.quarkus.hibernate.reactive.compatibility`）以去规范化方式编码了 10 个场景，导致：

1. **难以理解完整测试覆盖** - 需要查看 10 个文件才能了解所有组合
2. **测试命名混乱** - 如 `ORMReactiveCompatibilityDifferentNamedDataSourceNamedPersistenceUnitBothUnitTest`
3. **重复代码** - 每个类都重复相似的配置代码
4. **边界情况可能未测试** - 由于组合爆炸，某些组合可能遗漏

## 务实的重构方案（增量式）

由于 Quarkus 扩展测试的限制（类加载器问题），无法使用 `@ParameterizedTest`。采用增量重构：

### 阶段 1：提取公共配置模式

**目标**：创建 `CompatibilityScenario` 记录和工厂方法，使配置更清晰

**步骤**：
1. 创建 `CompatibilityScenario` 记录
2. 在每个测试类中添加场景描述注释
3. 创建场景工厂方法（可选）

### 阶段 2：合并相似测试类

**目标**：合并配置相似的测试类

**可合并的类**：
- `DefaultBothUnitTest` + `DefaultOnlyBlockingUnitTest` + `DefaultOnlyReactiveUnitTest` → `DefaultPUUnitTest`
- `NamedDataSourceBothUnitTest` + `NamedDataSourceReactiveUnitTest` → `NamedDataSourceUnitTest`

### 阶段 3：添加场景矩阵文档

**目标**：在测试类中显式列出所有场景

## 实施建议

**先做阶段 1**：
1. 创建 `CompatibilityScenario` 记录
2. 在 `CompatibilityUnitTestBase` 中添加辅助方法
3. 在 `QuarkusExtensionTest` 中支持场景参数（如果使用 JUnit 5 扩展）

**然后做阶段 2**：
1. 合并 `Default*` 类
2. 合并 `NamedDataSource*` 类

**最后做阶段 3**：
1. 添加测试矩阵表格
2. 添加场景文档

## 先决条件

此重构需在 #51206 完成后进行（如果适用）。

## 优势

- 增量式，风险低
- 保持向后兼容
- 改善可维护性
- 更清晰测试覆盖
