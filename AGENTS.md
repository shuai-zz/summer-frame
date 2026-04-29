


# summer-frame 项目指南

> 本文件面向 AI 编程助手。阅读前请确认你了解：本项目是一个从零实现的轻量级 Spring-like Java 框架，代码注释和文档主要使用中文。

---

## 项目概述

`summer-frame`（仓库名 `mini-spring`）是一个用于学习和演示目的的轻量级 Java 框架，模仿 Spring Framework 的核心特性。它实现了基于注解的 IoC 容器、AOP 代理、JDBC 模板、声明式事务、MVC Web 框架以及内嵌 Tomcat 启动器。

项目采用多模块 Maven 结构，所有框架代码位于 `org.example` 包下。示例应用 `hello-boot` 目前引用了错误的包名（`com.itranswarp.summer.*`），无法直接编译，需要修正为 `org.example.*` 才能使用。

---

## 技术栈与运行环境

- **Java 版本**：Java 21（`maven.compiler.source/target = 21`）
- **构建工具**：Apache Maven 3.9+
- **Servlet 规范**：Jakarta EE Servlet 6.0（`jakarta.servlet` 命名空间）
- **内嵌服务器**：Apache Tomcat 10.1.47（`tomcat-embed-core`、`tomcat-embed-jasper`）
- **代理生成**：ByteBuddy 1.14.2
- **JSON 处理**：Jackson 2.14.2
- **模板引擎**：FreeMarker 2.3.32
- **连接池**：HikariCP 5.0.1
- **日志**：SLF4J 2.0.7 + Logback 1.4.12
- **配置解析**：SnakeYAML 2.0
- **测试框架**：JUnit 5.9.2（Jupiter）
- **测试辅助**：Spring Framework 的 `spring-test` 与 `spring-web`（仅用于 Mock Servlet 对象）

---

## 模块结构

项目共 8 个模块，依赖关系自上而下：

| 模块 | 说明 | 依赖 |
|---|---|---|
| `summer-parent` | 父 POM（BOM），统一管理依赖版本和插件配置 | — |
| `summer-context` | **核心 IoC 容器**：注解配置、组件扫描、Bean 生命周期、依赖注入、属性解析 | `jakarta.annotation-api`, `snakeyaml`, `slf4j`, `logback` |
| `summer-aop` | **AOP 模块**：基于 ByteBuddy 的子类代理，支持 `@Around` 等注解 | `summer-context`, `byte-buddy` |
| `summer-jdbc` | **数据访问模块**：`JdbcTemplate`、声明式事务 `@Transactional`、HikariCP 数据源配置 | `summer-aop`, `HikariCP` |
| `summer-web` | **Web MVC 模块**：`DispatcherServlet`、URL 路由、参数绑定、视图解析（FreeMarker）、静态资源 | `summer-context`, `jackson-databind`, `jakarta.servlet-api`, `freemarker` |
| `summer-boot` | **启动器模块**：内嵌 Tomcat 启动、Banner 打印、上下文初始化 | `summer-web`, `summer-jdbc`, `tomcat-embed-*` |
| `summer-build` | 聚合 POM，用于一次性构建所有框架模块 | 上述所有模块 |
| `hello-boot` | 示例 WAR 应用，演示如何使用框架 | `summer-boot`（**当前 import 包名错误，无法编译**） |

### 关键源码路径

- **IoC 容器入口**：`summer-context/src/main/java/org/example/context/AnnotationConfigApplicationContext.java`
- **Bean 定义**：`summer-context/src/main/java/org/example/context/BeanDefinition.java`
- **属性解析器**：`summer-context/src/main/java/org/example/io/PropertyResolver.java`
- **AOP 代理解析器**：`summer-aop/src/main/java/org/example/aop/ProxyResolver.java`
- **JDBC 模板**：`summer-jdbc/src/main/java/org/example/jdbc/JdbcTemplate.java`
- **事务管理器**：`summer-jdbc/src/main/java/org/example/jdbc/tx/DataSourceTransactionManager.java`
- **前端控制器**：`summer-web/src/main/java/org/example/web/DispatcherServlet.java`
- **Web 配置**：`summer-web/src/main/java/org/example/web/WebMvcConfiguration.java`
- **启动类**：`summer-boot/src/main/java/org/example/summer/boot/SummerApplication.java`

---

## 构建与测试命令

### 编译所有框架模块

```bash
cd summer-build
mvn compile -am
```

### 安装到本地 Maven 仓库（供 hello-boot 使用）

```bash
cd summer-build
mvn install -DskipTests -Dgpg.skip=true
```

> 注意：默认启用了 `maven-gpg-plugin` 和 `nexus-staging-maven-plugin`（用于发布到 Maven Central），本地构建时必须加上 `-Dgpg.skip=true`，否则会因找不到 GPG 而失败。

### 运行测试

```bash
cd summer-build
mvn test -Dgpg.skip=true
```

### 单独测试某个模块

```bash
cd summer-context
mvn test -Dgpg.skip=true
```

---

## 已知问题

1. **GPG 签名插件**：`summer-parent` 及其子模块默认绑定了 `maven-gpg-plugin` 和 `nexus-staging-maven-plugin`。本地构建和测试必须携带 `-Dgpg.skip=true`。

2. **`hello-boot` 编译失败**：示例应用中的所有 `import com.itranswarp.summer.*` 语句需要改为 `import org.example.*`，因为框架源码实际包名为 `org.example`。例如：
   - `com.itranswarp.summer.annotation.ComponentScan` → `org.example.annotation.ComponentScan`
   - `com.itranswarp.summer.web.ModelAndView` → `org.example.web.ModelAndView`
   - `com.itranswarp.summer.boot.SummerApplication` → `org.example.summer.boot.SummerApplication`

3. **`summer-context` 单元测试存在 1 个失败**：`AnnotationConfigApplicationContextTest.testAnnotationConfigApplicationContext` 第 57 行期望 `@Import(LocalDateConfiguration.class)` 生效，但测试配置类 `ScanApplication` 中的 `@Import` 已被注释掉，导致断言失败。

---

## 代码组织与核心机制

### 1. IoC 容器（`summer-context`）

- **配置类**：使用 `@Configuration` + `@ComponentScan` 启动扫描。
- **组件扫描**：自动识别 `@Component`（及其元注解，如 `@Controller`、`@RestController`、`@Service` 等）。
- **Bean 定义**：每个组件生成一个 `BeanDefinition`，包含构造器/工厂方法、`@Order` 排序、`@Primary` 优先级、`@PostConstruct`/`@PreDestroy` 生命周期方法。
- **依赖注入**：
  - 构造器注入：通过 `@Autowired` 或 `@Value` 在构造器参数中注入。
  - 字段/Setter 注入：通过 `@Autowired` 或 `@Value` 在字段或单参数方法上注入。
- **循环依赖检测**：在 `createBeanAsEarlySingleton` 过程中通过 `creatingBeanNames` 集合检测循环依赖并抛出 `UnsatisfiedDependencyException`。
- **BeanPostProcessor**：支持 `postProcessBeforeInitialization` 和 `postProcessAfterInitialization`，用于代理替换等扩展。AOP 和事务均基于此机制实现。

### 2. AOP（`summer-aop`）

- 使用 **ByteBuddy** 动态生成目标类的子类代理。
- `ProxyResolver` 负责创建代理实例，拦截所有 public 方法并委托给 `InvocationHandler`。
- 提供 `AnnotationProxyBeanPostProcessor` 基类，子类只需指定注解类型即可自动为带注解的 Bean 创建代理。
- 已有实现：
  - `AroundProxyBeanPostProcessor`（处理 `@Around`）
  - `TransactionalBeanPostProcessor`（处理 `@Transactional`）

### 3. JDBC 与事务（`summer-jdbc`）

- **`JdbcTemplate`**：提供 `queryForObject`、`queryForList`、`update`、`execute` 等模板方法，支持 `RowMapper`、`ConnectionCallback`、`PreparedStatementCallback`。
- **事务管理**：`DataSourceTransactionManager` 通过 `ThreadLocal<TransactionStatus>` 实现线程级事务上下文，支持事务嵌套（加入现有事务）。
- **`@Transactional`**：标注在类或方法上，由 `TransactionalBeanPostProcessor` 自动创建代理，在代理中开启/提交/回滚事务。
- **数据源配置**：`JdbcConfiguration` 使用 HikariCP，通过 `application.yml` 中的 `summer.datasource.*` 属性配置。

### 4. Web MVC（`summer-web`）

- **`DispatcherServlet`**：核心前端控制器，继承自 `HttpServlet`。
- **控制器识别**：自动扫描 `@Controller` 和 `@RestController`。
- **请求映射**：支持 `@GetMapping` 和 `@PostMapping`，URL 支持路径变量（如 `/api/user/{email}`）。
- **参数绑定**：
  - `@PathVariable`：路径变量
  - `@RequestParam`：查询参数 / 表单参数（支持 `defaultValue`）
  - `@RequestBody`：JSON 反序列化（基于 Jackson）
  - 无注解：自动注入 `HttpServletRequest`、`HttpServletResponse`、`HttpSession`、`ServletContext`
- **返回值处理**：
  - `@RestController` 或 `@ResponseBody`：自动序列化为 JSON（或原样输出 `String`/`byte[]`）
  - `@Controller`：支持 `ModelAndView`（FreeMarker 渲染）、`String`（`redirect:` 前缀支持重定向）、`byte[]`
- **静态资源**：默认以 `/static/` 开头或 `/favicon.ico` 的请求直接由 ServletContext 提供静态文件。
- **视图解析**：`FreeMarkerViewResolver` 解析 FreeMarker 模板，模板路径可通过 `summer.web.freemarker.template-path` 配置。

### 5. Boot 启动器（`summer-boot`）

- `SummerApplication.run(webDir, baseDir, configClass, args)` 启动内嵌 Tomcat。
- 默认读取 `application.yml` 或 `application.properties` 作为配置。
- 默认端口 `8000`，可通过 `server.port` 修改。

---

## 配置文件

框架支持 `application.yml` 或 `application.properties`，通常放在类路径根目录或 `src/main/resources` 下。

### 常用配置项示例

```yaml
server:
  port: 8080

summer:
  datasource:
    url: jdbc:sqlite:test.db
    driver-class-name: org.sqlite.JDBC
    username: sa
    password:
    maximum-pool-size: 20
    minimum-pool-size: 1
    connection-timeout: 30000
  web:
    static-path: /static/
    favicon-path: /favicon.ico
    character-encoding: UTF-8
    freemarker:
      template-path: /WEB-INF/templates
      template-encoding: UTF-8
```

### 属性解析特性

`PropertyResolver` 支持：
- `${key:defaultValue}` 占位符与嵌套解析
- 环境变量自动注入
- 多种类型自动转换：`String`、基本类型、`LocalDate`、`LocalTime`、`LocalDateTime`、`ZonedDateTime`、`Duration`、`ZoneId`

---

## 测试策略

- **单元测试**：各模块均使用 JUnit 5（Jupiter）编写测试。
- **IoC 测试**：在 `summer-context/src/test/java` 中，通过手动创建 `AnnotationConfigApplicationContext` 并传入 `PropertyResolver` 进行测试。
- **Web 测试**：在 `summer-web/src/test/java` 中，使用 Spring 提供的 `MockHttpServletRequest`、`MockHttpServletResponse`、`MockServletContext` 对 `DispatcherServlet` 进行离线测试，无需启动真实 Tomcat。
- **JDBC 测试**：在 `summer-jdbc/src/test/java` 中，使用 SQLite 内存或文件数据库进行集成测试，分别测试带事务和不带事务的场景。
- **AOP 测试**：在 `summer-aop/src/test/java` 中，通过 `AnnotationConfigApplicationContext` 启动上下文，验证代理行为。

---

## 开发约定

- **包命名**：框架源码统一使用 `org.example` 作为根包。各模块按功能细分，如 `org.example.context`、`org.example.web`、`org.example.jdbc`。
- **注解位置**：自定义注解定义在 `summer-context/src/main/java/org/example/annotation/` 或对应模块的 `annotation` 包下。
- **异常体系**：框架自定义异常均继承自 `BeansException` 或 `NestedRuntimeException`，位于各模块的 `exception` 包中。
- **日志**：使用 SLF4J + Logback，Logger 命名通常基于类名。
- **代码注释**：关键逻辑和复杂流程使用中文注释，说明设计意图和步骤顺序。

---

## 安全与注意事项

- **代理注入限制**：被 ByteBuddy 代理后的 Bean，其 **字段不会被注入**（因为代理是子类实例）。容器通过 `BeanPostProcessor.postProcessOnSetProperty()` 机制将注入操作反向代理到原始 Bean 实例上。开发自定义 `BeanPostProcessor` 时必须正确处理此接口。
- **`@Configuration` 与 `BeanPostProcessor` 限制**：`@Configuration` 类不能同时是 `BeanPostProcessor`；`BeanPostProcessor` 的构造器参数不允许使用 `@Autowired` 注入其他 Bean。
- **静态字段不可注入**：`BeanDefinitionException` 会在检测到 `@Autowired` 或 `@Value` 标注在静态字段/方法时抛出。
- **最终字段限制**：`final` 字段不可注入；`final` setter 方法注入时会输出警告，因为在代理环境下可能引发 `NullPointerException`。
- **发布安全**：`summer-parent` 配置了 `maven-gpg-plugin` 和 `nexus-staging-maven-plugin`，发布到 Maven Central 时需要进行 GPG 签名。日常开发务必跳过此步骤。
