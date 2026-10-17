[![Build on master with Sonar](https://github.com/reisi007/reisishot.pictures/actions/workflows/gradle-sonar.yml/badge.svg)](https://github.com/reisi007/reisishot.pictures/actions/workflows/gradle-sonar.yml)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=reisi007_reisishot.pictures&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=reisi007_reisishot.pictures)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=reisi007_reisishot.pictures&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=reisi007_reisishot.pictures)
[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=reisi007_reisishot.pictures&metric=sqale_index)](https://sonarcloud.io/summary/new_code?id=reisi007_reisishot.pictures)
[![Code coverage](https://codecov.io/gh/reisi007/reisishot.pictures/branch/main/graph/badge.svg?token=18BXO7PXRQ)](https://codecov.io/gh/reisi007/reisishot.pictures)
[![Deployed with FTP Deploy Action](https://img.shields.io/badge/Deployed%20with%20FTP%20Deploy%20Action-2b9348)](https://github.com/SamKirkland/FTP-Deploy-Action)

# reisishot.pictures

This is the repository of my private, photography oriented website. Rendering is done in a Kotlin Backend (+ ImageMagick
for Thumbnail generation). Image loading is a custom implementation, taking account the actual displayed width / height
of the image and much more cool stuff (including an Markdown compatible Apache Velocity build)

## `main` is the main state of the project

`main` holds the **largest and most complete stage** of this implementation, dated **2022-10-30** (`85bc809`): the state
the site was built and run with — 26 Gradle modules, 111 Kotlin files, roughly 7.6k lines of Kotlin (non-blank) plus
505 lines of tests. Everything I built over the years is in here; the tag `peak-2022-10-30` pins exactly that commit,
`main` additionally carries this branch documentation.

Nothing that came later was rewritten away — it lives on its own branch:

| Ref | Commit | What it holds |
| --- | --- | --- |
| **`main`** | `85bc809` (2022-10-30) | the main state described above (plus this README update) |
| `next-js` | `f8c3ac5` (2023-01-27) | the Next.js / `next-cli` rewrite; contains `main`'s peak commit as an ancestor |
| `migration` | `414d561` (2023-01-27) | same content as `next-js`, but on the squashed merge line, so it does **not** contain the peak commits |
| `main-2022-09` | `9332078` (2022-09-28) | the previous `main` tip (two commits that are not part of the peak line, e.g. a terser bump) |
| `json` | `62fe4ae` (2022-02-13) | side experiment: a JSON based gallery generator |
| `minorFixes` | `18bdee2` (2022-04-06) | side line |
| `remove-images-testimonial` | `86b485c` (2022-04-07) | side line |
| `dependabot/*` | 2022–2025 | dependency bumps on the npm side |
| `pr/*` | 2020–2023 | refs of the pull requests |
| tag `peak-2022-10-30` | `85bc809` | the main state, pinned |
| tag `next-js-final` | `f8c3ac5` | the Next.js rewrite, pinned |

## Architecture

26 Gradle modules:

* `commons` — shared helpers
* `image-access` — image model and EXIF/metadata access (metadata-extractor)
* `backend` — the site generator
  * `backend:gallery-config`, `backend:website-config` — the Kotlin DSL that describes the site
  * `backend:html` — HTML rendering (Kotlin HTML DSL) plus forms
  * `backend:generators:gallery` (`:gallery-abstract`) — the image galleries
  * `backend:generators:page` (`:keyword`, `:minimal`, `:overview`) — pages, Markdown/YAML driven
  * `backend:generators:link`, `:sitemap`, `:testimonial`, `:multisite` — further generators
  * `backend:generators:thumbnail-abstract`, `:thumbnail-imagick` — thumbnails
  * `backend:root`, `backend:runner` — wiring and entry point (`pictures.reisishot.mise.backend.main.Main`)
* `ui:base-ui`, `ui:config-ui`, `ui:exif-ui` — JavaFX/TornadoFX desktop UIs
* `config-ui` — Compose for Desktop rewrite of the config UI (incl. LanguageTool based German spellcheck)
* `mise-utils`, `piwigo-converter` — tooling used while migrating the image data

Rendering is Kotlin HTML DSL plus Apache Velocity templates, Markdown via flexmark. Images are scaled to the width
actually needed by the browser, thumbnails are generated with ImageMagick; generation runs in parallel on
kotlinx.coroutines, configuration and JSON data use kotlinx.serialization, dependencies are managed with Gradle
version catalogs. The generated site is uploaded over FTP (`deploy/`, FTP Deploy Action in CI).

## Build

JDK 17 (CI: Temurin 17), Kotlin 1.7.20, Gradle wrapper 7.4.2:

```sh
./gradlew build --parallel   # compile + tests
./gradlew test --parallel    # tests only
```

The runner (`backend:runner`) expects the site configuration and the image sources on disk; the image sources
themselves are not part of this repository.
