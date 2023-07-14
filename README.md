Reproducer (SSCCE) for issue "Classpath pollution of included builds when using GradleRunner.withPluginClasspath" 

## Problem

In general, the classpath of an includED build should not be affected by the build that is includING it. 
But when using `withPluginClasspath` sometimes the classpath of the includED build is polluted with the classpath of the includING build.

`withPluginClasspath` causes this classloader to be used in the includED build:

```
MultiParentClassLoader#parent1 VisitableURLClassLoader(ClassLoaderScopeIdentifier.Id{coreAndPlugins:injected-plugin(local)})@69995ed9
```

This includes the classpath from the includING build.

## Workaround

[Publishing the plugin to the file system and loading it from there avoids this problem.](../../compare/workaround)


## Reproducer

This repo has 3 separate Gradle builds: 

1. [included-build](included-build)
2. [project-using-included-build](project-using-included-build)
3. [test-using-included-build](test-using-included-build)


[project-using-included-build](project-using-included-build) includes [included-build](included-build) and loads a settings plugin from it. 
See [project-using-included-build/settings.gradle.kts](project-using-included-build/settings.gradle.kts)

[test-using-included-build](test-using-included-build) has a GradleRunner test ([`IncludedBuildTest`](test-using-included-build/src/test/kotlin/IncludedBuildTest.kt)) 
that replicates the contents of [project-using-included-build](project-using-included-build)


## Expected behavior

The results of [`IncludedBuildTest`](test-using-included-build/src/test/kotlin/IncludedBuildTest.kt) should be the same as
running `gradle -p project-using-included-build build`

## Actual behavior

When you execute [`IncludedBuildTest`](test-using-included-build/src/test/kotlin/IncludedBuildTest.kt) it fails because the 
classpath from `withPluginClasspath` is injected into the included build ([included-build](included-build))

## Steps to reproduce

Using latest version of gradle (this was tested with Gradle `8.2.1` on JDK 17.0.7 on `macOS 13.4`)

Run `gradle -p project-using-included-build build`

You will get this output:
```
/Users/USER/git/gradle-reproducer-included-build-classpath-pollution/included-build: embeddedKotlinVersion: 1.8.20
/Users/USER/git/gradle-reproducer-included-build-classpath-pollution/included-build: getKotlinPluginVersion(logger): 1.8.20
DefaultKotlinBasePlugin classloader: InstrumentingVisitableURLClassLoader(ClassLoaderScopeIdentifier.Id{coreAndPlugins:settings[:included-build](export)})
plugin applied to : at /Users/USER/git/gradle-reproducer-included-build-classpath-pollution/project-using-included-build

> Task :buildEnvironment

------------------------------------------------------------
Root project 'project-using-included-build'
------------------------------------------------------------

classpath
No dependencies

A web-based, searchable dependency report is available by adding the --scan option.

BUILD SUCCESSFUL in 1s
11 actionable tasks: 3 executed, 8 up-to-date
```

Notice:

|                                | result |
|--------------------------------|--------|
| `embeddedKotlinVersion`          |    `1.8.20`    |
| `getKotlinPluginVersion(logger)` |  `1.8.20`      |
| `DefaultKotlinBasePlugin classloader`   |   `{coreAndPlugins:settings[:included-build](export)`     |


The classloader hierarchy used for `included-build/settings.gradle.kts` is: 
```
class Settings_gradle has classloader InstrumentingVisitableURLClassLoader(ClassLoaderScopeIdentifier.Id{coreAndPlugins:settings[:included-build]:kotlin-dsl:/Users/USER/git/gradle-reproducer-included-build-classpath-pollution/included-build/settings.gradle.kts:Settings/TopLevel/stage2(local)})@1b495b67
 - classloader of the classloader: VisitableURLClassLoader(ant-and-gradle-loader)@442d9b6e
location file:/Users/USER/.gradle/caches/8.2.1/kotlin-dsl/scripts/f9925455ee1e1f75ac9079434682a35d/classes/
with parent InstrumentingVisitableURLClassLoader(ClassLoaderScopeIdentifier.Id{coreAndPlugins:settings[:included-build](export)})@70d80790
 - classloader of the classloader: VisitableURLClassLoader(ant-and-gradle-loader)@442d9b6e
with parent CachingClassLoader(null)@31a8cf27
 - classloader of the classloader: VisitableURLClassLoader(ant-and-gradle-loader)@442d9b6e
with parent FilteringClassLoader(null)@7ef82753
 - classloader of the classloader: VisitableURLClassLoader(ant-and-gradle-loader)@442d9b6e
with parent MixInLegacyTypesClassLoader(legacy-mixin-loader)@31e5415e
 - classloader of the classloader: VisitableURLClassLoader(ant-and-gradle-loader)@442d9b6e
with parent VisitableURLClassLoader(ant-and-gradle-loader)@442d9b6e
 - classloader of the classloader: AppClassLoader(app)@251a69d7
with parent VisitableURLClassLoader(ant-loader)@726f3b58
 - classloader of the classloader: AppClassLoader(app)@251a69d7
with parent PlatformClassLoader(platform)@4380506d
 - classloader of the classloader: null
```


Next run `gradle -p test-using-included-build build` and OPEN the HTML from the failing test.

**We would expect this output to match the output above, but it does not**

From the TEST OUTPUT in the HTML: 
```
org.gradle.testkit.runner.UnexpectedBuildFailure: Unexpected build execution failure in /var/folders/zg/asdfasf/T/junit18390892208 with arguments [build]

Output:
/Users/USER/git/gradle-reproducer-included-build-classpath-pollution/included-build: embeddedKotlinVersion: 1.8.20
/Users/USER/git/gradle-reproducer-included-build-classpath-pollution/included-build: getKotlinPluginVersion(logger): 1.9.0
DefaultKotlinBasePlugin classloader: VisitableURLClassLoader(ClassLoaderScopeIdentifier.Id{coreAndPlugins:injected-plugin(local)})

> Configure project :included-build
WARNING: Unsupported Kotlin plugin version.
The `embedded-kotlin` and `kotlin-dsl` plugins rely on features of Kotlin `1.8.20` that might work differently than in the requested version `1.9.0`.

> Task :included-build:generateExternalPluginSpecBuilders UP-TO-DATE
> Task :included-build:extractPrecompiledScriptPluginPlugins UP-TO-DATE
> Task :included-build:compilePluginsBlocks UP-TO-DATE
> Task :included-build:generatePrecompiledScriptPluginAccessors UP-TO-DATE
> Task :included-build:generateScriptPluginAdapters UP-TO-DATE
> Task :included-build:pluginDescriptors UP-TO-DATE
> Task :included-build:processResources UP-TO-DATE

> Task :included-build:compileKotlin FAILED
e: file:///Users/USER/git/gradle-reproducer-included-build-classpath-pollution/included-build/src/main/kotlin/plugin-from-included-build.settings.gradle.kts:5:5 Unresolved reference. None of the following candidates is applicable because of receiver type mismatch: 
public fun Project.repositories(configuration: RepositoryHandler.() -> Unit): Unit defined in org.gradle.kotlin.dsl
public fun ScriptHandler.repositories(configuration: RepositoryHandler.() -> Unit): Unit defined in org.gradle.kotlin.dsl
e: file:///Users/USER/git/gradle-reproducer-included-build-classpath-pollution/included-build/src/main/kotlin/plugin-from-included-build.settings.gradle.kts:6:9 Unresolved reference. None of the following candidates is applicable because of receiver type mismatch: 
public inline fun RepositoryHandler.mavenCentral(vararg args: Pair<String, Any?>): MavenArtifactRepository defined in org.gradle.kotlin.dsl
e: file:///Users/USER/git/gradle-reproducer-included-build-classpath-pollution/included-build/src/main/kotlin/plugin-from-included-build.settings.gradle.kts:7:9 Unresolved reference: gradlePluginPortal

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':included-build:compileKotlin'.
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.GradleCompilerRunnerWithWorkers$GradleKotlinCompilerWorkAction
   > Compilation error. See log for more details

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights.
> Get more help at https://help.gradle.org.

BUILD FAILED in 5s
8 actionable tasks: 1 executed, 7 up-to-date
```

Notice:

|                                | result                                              |
|--------------------------------|-----------------------------------------------------|
| `embeddedKotlinVersion`          | `1.8.20`                                            |
| `getKotlinPluginVersion(logger)` | `1.9.0`                                             |
| `DefaultKotlinBasePlugin classloader`   | `coreAndPlugins:injected-plugin(local)` |

The classloader hierarchy used for `included-build/settings.gradle.kts` during `IncludedBuildTest` is:

```
class Settings_gradle has classloader VisitableURLClassLoader(ClassLoaderScopeIdentifier.Id{coreAndPlugins:settings[:included-build]:kotlin-dsl:/Users/USER/git/gradle-reproducer-included-build-classpath-pollution/included-build/settings.gradle.kts:Settings/TopLevel/stage2(local)})@31121dd6
 - classloader of the classloader: VisitableURLClassLoader(tooling-implementation-loader)@10013c
location file:/Users/USER/git/gradle-reproducer-included-build-classpath-pollution/test-using-included-build/build/tmp/test/work/.gradle-test-kit/caches/jars-9/cd2e7693f0d6f9d67378550b0d47f711/classes.jar
with parent CachingClassLoader(null)@4a19e267
 - classloader of the classloader: VisitableURLClassLoader(tooling-implementation-loader)@10013c
with parent MultiParentClassLoader(null)@b544b34

MultiParentClassLoader#parent0 CachingClassLoader(null)@6fbe80a3
 - classloader of the classloader: VisitableURLClassLoader(tooling-implementation-loader)@10013c
with parent FilteringClassLoader(null)@1aef6a05
 - classloader of the classloader: VisitableURLClassLoader(tooling-implementation-loader)@10013c
with parent MixInLegacyTypesClassLoader(legacy-mixin-loader)@6df3f402
 - classloader of the classloader: VisitableURLClassLoader(tooling-implementation-loader)@10013c
with parent VisitableURLClassLoader(tooling-implementation-loader)@10013c
 - classloader of the classloader: AppClassLoader(app)@5ffd2b27
with parent FilteringClassLoader(null)@4182d165
 - classloader of the classloader: AppClassLoader(app)@5ffd2b27
with parent AppClassLoader(app)@5ffd2b27
 - classloader of the classloader: null
with parent PlatformClassLoader(platform)@50423da2
 - classloader of the classloader: null

MultiParentClassLoader#parent1 VisitableURLClassLoader(ClassLoaderScopeIdentifier.Id{coreAndPlugins:injected-plugin(local)})@69995ed9
 - classloader of the classloader: VisitableURLClassLoader(tooling-implementation-loader)@10013c
with parent CachingClassLoader(null)@6fbe80a3
 - classloader of the classloader: VisitableURLClassLoader(tooling-implementation-loader)@10013c
with parent FilteringClassLoader(null)@1aef6a05
 - classloader of the classloader: VisitableURLClassLoader(tooling-implementation-loader)@10013c
with parent MixInLegacyTypesClassLoader(legacy-mixin-loader)@6df3f402
 - classloader of the classloader: VisitableURLClassLoader(tooling-implementation-loader)@10013c
with parent VisitableURLClassLoader(tooling-implementation-loader)@10013c
 - classloader of the classloader: AppClassLoader(app)@5ffd2b27
with parent FilteringClassLoader(null)@4182d165
 - classloader of the classloader: AppClassLoader(app)@5ffd2b27
with parent AppClassLoader(app)@5ffd2b27
 - classloader of the classloader: null
with parent PlatformClassLoader(platform)@50423da2
 - classloader of the classloader: null
```

This classloader contains kotlin `1.9.0`: 
```
MultiParentClassLoader#parent1 VisitableURLClassLoader(ClassLoaderScopeIdentifier.Id{coreAndPlugins:injected-plugin(local)})@69995ed9
```

Also Notice: 

```
The `embedded-kotlin` and `kotlin-dsl` plugins rely on features of Kotlin `1.8.20` that might work differently than in the requested version `1.9.0`.
```


The kotlin mismatch causes the compilation of `plugin-from-included-build.settings.gradle.kts` to fail, but this is NOT the main issue here, this is only a side effect.  
```
e: file:///Users/USER/git/gradle-reproducer-included-build-classpath-pollution/included-build/src/main/kotlin/plugin-from-included-build.settings.gradle.kts:5:5 Unresolved reference. None of the following candidates is applicable because of receiver type mismatch: 
public fun Project.repositories(configuration: RepositoryHandler.() -> Unit): Unit defined in org.gradle.kotlin.dsl
public fun ScriptHandler.repositories(configuration: RepositoryHandler.() -> Unit): Unit defined in org.gradle.kotlin.dsl
e: file:///Users/USER/git/gradle-reproducer-included-build-classpath-pollution/included-build/src/main/kotlin/plugin-from-included-build.settings.gradle.kts:6:9 Unresolved reference. None of the following candidates is applicable because of receiver type mismatch: 
public inline fun RepositoryHandler.mavenCentral(vararg args: Pair<String, Any?>): MavenArtifactRepository defined in org.gradle.kotlin.dsl
e: file:///Users/USER/git/gradle-reproducer-included-build-classpath-pollution/included-build/src/main/kotlin/plugin-from-included-build.settings.gradle.kts:7:9 Unresolved reference: gradlePluginPortal
```

## License

Any contributions made under this project will be governed by the
[Apache License 2.0](./LICENSE.txt).
