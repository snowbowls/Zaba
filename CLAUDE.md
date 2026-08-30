# Zaba

Java Discord bot (JDA 6.3.2) built with Maven. Entry point / main class: `Bot` (`src/main/java/Bot.java`).

## Build commands

- `mvn clean package` — compiles and produces a runnable fat jar at
  `target/zaba-1.0-SNAPSHOT.jar` (`Main-Class: Bot`, built via
  `maven-shade-plugin`). Run it with `java -jar target/zaba-1.0-SNAPSHOT.jar`
  from the repo root (see runtime files below). The pre-shade thin jar is
  also left at `target/original-zaba-1.0-SNAPSHOT.jar`.
- `mvn spotless:check` — verify formatting (google-java-format via
  `spotless-maven-plugin`).
- `mvn spotless:apply` — reformat source in place. Run this before
  committing Java changes; CI-equivalent is `spotless:check`.

## JDK requirement

`maven.compiler.release` is pinned to **25**, so `mvn compile`/`package`
needs JDK 25 or newer running Maven. This machine's default JDK is
**26** (`/usr/lib/jvm/java-26-openjdk`), which satisfies this — no JDK
switching is needed for normal builds.

An older JDK 21 is also installed at `/usr/lib/jvm/java-21-openjdk`, but
it's *not* sufficient for `compile`/`package` (release 25 requires >=25).
It's not needed for anything else currently — `spotless:apply`/`check` run
fine under the default JDK 26 too, since `google-java-format` is pinned to
`1.36.1` specifically for that (see below).

## Environment quirks

- **google-java-format is pinned to 1.36.1 in the `spotless-maven-plugin`
  config.** The version spotless bundles by default reflects into javac
  internals that changed shape in JDK 25+ (`Log.DeferredDiagnosticHandler
  .getDiagnostics()` went from returning a `Queue` to a `List`), so
  `spotless:apply`/`check` crash with a `NoSuchMethodError` under this
  machine's default JDK 26 unless a newer, compatible version is pinned.
  Don't remove or downgrade that `<version>` element without re-testing
  under JDK 26.
- **Runtime config comes from a git-ignored `.env`** (loaded via
  `dotenv-kotlin`), with keys: `TOKEN`, `URI` (MongoDB), `JWT`,
  `ZABAFREEZONE`, `AIMODE`, `OAUTH` (YouTube). The bot won't start without
  one.
- **Several plain-file resources are read relative to the working
  directory at runtime, not from the classpath**: `keywords.json`,
  `badwords.txt`, `jon.txt`. Run the jar from the repo root, and make sure
  `keywords.json` exists locally — it's git-ignored (private bot content)
  and isn't checked into the repo.
- **`target/classes/*.class` files are tracked in git**, despite `target`
  being listed in `.gitignore` — they were committed before that rule was
  added, so `.gitignore` doesn't retroactively untrack them. Expect
  `git status` to show them as modified after any local build; they're
  build noise, not something to intentionally commit.
- The shade plugin has `createDependencyReducedPom` set to `false` to
  avoid leaving a stray `dependency-reduced-pom.xml` in the repo root
  after every `package`.
- The pom pulls dependencies from several non-Central repositories
  (`maven.lavalink.dev` releases/snapshots, `jitpack.io`,
  `m2.dv8tion.net`, `jcenter`, Sonatype snapshots) — needed for
  `dev.lavalink.youtube` (pinned to a specific git-commit-hash snapshot
  version, not a normal release) and `club.minnced:jdave-api`. A build
  will fail offline or if any of these repos becomes unreachable.
