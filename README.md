# summer-frame

> 一个从零实现的轻量级 Spring-like Java 框架，用于学习和演示目的。
>
> 本项目参考自 [廖雪峰老师的 summer-framework 教程](https://liaoxuefeng.com/books/summerframework/introduction/index.html) 与 [GitHub 开源项目](https://github.com/michaelliao/summer-framework)。

---

## 项目概述

`mini-spring` 是一个模仿 Spring Framework 核心特性的轻量级 Java 框架。它实现了基于注解的 IoC 容器、AOP 代理、JDBC 模板、声明式事务、MVC Web 框架以及内嵌 Tomcat 启动器。

项目采用多模块 Maven 结构，所有框架代码位于 `org.example` 包下。

## 技术栈

| 技术 | 版本 |
|---|---|
| Java | 21 |
| Maven | 3.9+ |
| Servlet | Jakarta EE Servlet 6.0 |
| 内嵌服务器 | Apache Tomcat 10.1.47 |
| 代理生成 | ByteBuddy 1.14.2 |
| JSON 处理 | Jackson 2.14.2 |
| 模板引擎 | FreeMarker 2.3.32 |
| 连接池 | HikariCP 5.0.1 |
| 日志 | SLF4J 2.0.7 + Logback 1.4.12 |
| 配置解析 | SnakeYAML 2.0 |
| 测试框架 | JUnit 5.9.2 |

## 模块结构

| 模块 | 说明 |
|---|---|
| `summer-parent` | 父 POM（BOM），统一管理依赖版本和插件配置 |
| `summer-context` | **核心 IoC 容器**：注解配置、组件扫描、Bean 生命周期、依赖注入、属性解析 |
| `summer-aop` | **AOP 模块**：基于 ByteBuddy 的子类代理，支持 `@Around` 等注解 |
| `summer-jdbc` | **数据访问模块**：`JdbcTemplate`、声明式事务 `@Transactional`、HikariCP 数据源配置 |
| `summer-web` | **Web MVC 模块**：`DispatcherServlet`、URL 路由、参数绑定、视图解析（FreeMarker）、静态资源 |
| `summer-boot` | **启动器模块**：内嵌 Tomcat 启动、Banner 打印、上下文初始化 |
| `summer-build` | 聚合 POM，用于一次性构建所有框架模块 |
| `hello-boot` | 示例 WAR 应用，演示如何使用框架 |

## 快速开始

### 1. 编译安装框架模块

```bash
cd summer-build
mvn install -DskipTests -Dgpg.skip=true
```

> 注意：`summer-parent` 默认绑定了 `maven-gpg-plugin`，本地构建必须加上 `-Dgpg.skip=true`。

### 2. 运行示例应用

```bash
cd hello-boot
mvn compile exec:java -Dexec.mainClass="com.example.hello.Main"
```

或者直接使用 `java -cp`：

```bash
cd hello-boot
mvn dependency:build-classpath -Dmdep.outputFile=/tmp/cp.txt -q
java -cp "target/classes:$(cat /tmp/cp.txt)" com.example.hello.Main
```

应用默认在 **http://localhost:8080** 启动。

### 3. 运行测试

```bash
cd summer-build
mvn test -Dgpg.skip=true
```

## 核心特性

### IoC 容器

- 使用 `@Configuration` + `@ComponentScan` 启动扫描
- 自动识别 `@Component` 及其元注解（`@Controller`、`@RestController`、`@Service` 等）
- 支持构造器注入、字段注入、Setter 注入
- 循环依赖检测
- `BeanPostProcessor` 扩展机制

### AOP

- 基于 ByteBuddy 动态生成子类代理
- 支持 `@Around` 注解
- `AnnotationProxyBeanPostProcessor` 基类简化自定义代理

### JDBC 与事务

- `JdbcTemplate` 模板方法
- `@Transactional` 声明式事务
- 线程级事务上下文，支持事务嵌套
- HikariCP 连接池

### Web MVC

- `DispatcherServlet` 前端控制器
- `@GetMapping` / `@PostMapping` 路由映射
- `@PathVariable`、`@RequestParam`、`@RequestBody` 参数绑定
- 自动注入 `HttpServletRequest`、`HttpServletResponse`、`HttpSession`
- `@RestController` 自动 JSON 序列化
- `@Controller` 支持 `ModelAndView`（FreeMarker 渲染）和重定向
- 静态资源支持

### Boot 启动器

- `SummerApplication.run()` 一键启动内嵌 Tomcat
- 自动读取 `application.yml` / `application.properties`
- 默认端口 `8000`，可通过 `server.port` 修改

## 配置示例

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

## 开发约定

- 框架源码统一使用 `org.example` 作为根包
- 关键逻辑和复杂流程使用中文注释
- Logger 命名基于类名
- 自定义异常继承自 `BeansException` 或 `NestedRuntimeException`

## 来源

- 教程文档：https://liaoxuefeng.com/books/summerframework/introduction/index.html
- 原始项目：https://github.com/michaelliao/summer-framework