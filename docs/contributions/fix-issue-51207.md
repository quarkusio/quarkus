# 重构 Issue #51207：Hibernate ORM/Reactive 兼容性测试参数化

## 背景

Quarkus 的 Hibernate ORM/Reactive 兼容性测试套件（`io.quarkus.hibernate.reactive.compatibility`）当前以"去规范化"方式编码了 10 个场景，每个场景都是一个独立的测试类，导致：

1. **难以理解完整测试覆盖** - 需要查看 10 个文件才能了解所有组合
2. **测试命名混乱** - 如 `ORMReactiveCompatibilityDifferentNamedDataSourceNamedPersistenceUnitBothUnitTest`
3. **重复代码** - 每个类都重复相似的配置代码
4. **边界情况可能未测试** - 由于组合爆炸，某些组合可能遗漏

## 当前测试矩阵

| 测试类 | 数据源 | 持久化单元 | 阻塞访问 | 响应式访问 |
|--------|--------|------------|----------|------------|
| DefaultBothUnitTest | 默认 | 默认 | ✓ | ✓ |
| DefaultOnlyBlockingUnitTest | 默认 | 默认 | ✓ | ✗ |
| DefaultOnlyReactiveUnitTest | 默认 | 默认 | ✗ | ✓ |
| DefaultOnlyReactiveDisabledBlockingSessionUnitTest | 默认 | 默认 | ✗ | ✓ |
| NamedDataSourceBothUnitTest | 命名 | 默认 | ✓ | ✓ |
| NamedDataSourceReactiveUnitTest | 命名 | 默认 | ✗ | ✓ |
| NamedDataSourceNamedPersistenceUnitBothUnitTest | 命名 | 命名 | ✓ | ✓ |
| DifferentNamedDataSourceNamedPersistenceUnitBothUnitTest | 不同命名 | 命名 | ✓ | ✓ |
| NamedReactiveDefaultBlockingUnitTest | 命名 | 命名(默认阻塞) | ✓ | ✓ |
| OnlyReactiveJDBCDisabledUnitTest | 默认(JDBC禁用) | 默认 | ✗ | ✓ |

## 设计方案

### 目标

使用 JUnit 5 的 `@ParameterizedTest` 将所有场景合并到一个测试类中，使组合矩阵更加显式、易于理解。

### 参数化方案

```java
@ParameterizedTest
@MethodSource("provideCompatibilityScenarios")
void testCompatibility(CompatibilityScenario scenario) { ... }
```

每个 `CompatibilityScenario` 包含：
- 场景名称
- 配置键值对
- 是否期望响应式可用
- 是否期望阻塞访问可用

### 依赖

此任务需在 #51206 完成后进行（如果适用）。

## 实施步骤

1. 创建 `CompatibilityScenario` 记录/类
2. 创建 `ORMReactiveCompatibilityUnitTest` 参数化测试类
3. 将所有场景迁移到参数化方法源
4. 删除旧的测试类
5. 运行测试验证
