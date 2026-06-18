set shell := ["bash", "-euo", "pipefail", "-c"]

default:
  @just --list

list-versions:
  @find versions -mindepth 2 -maxdepth 2 -type f -name 'gradle.properties' -printf '%h\n' | xargs -r -n1 basename | sort -V

list-loaders version:
  @grep '^project.enabled-loaders=' "versions/{{version}}/gradle.properties" | head -n1 | cut -d= -f2- | tr ',' '\n' | sed 's/^[[:space:]]*//; s/[[:space:]]*$//' | sed '/^$/d'

list-nodes:
  @for props in versions/*/gradle.properties; do version=$(basename "$(dirname "$props")"); loaders=$(sed -nE 's/^project\.enabled-loaders=(.*)$/\1/p' "$props" | head -n1); for loader in $(printf '%s\n' "$loaders" | tr ',' '\n' | sed 's/^[[:space:]]*//; s/[[:space:]]*$//' | sed '/^$/d'); do echo "$version-$loader"; done; done | sort -V

build node:
  @if ! just list-nodes | grep -Fxq "{{node}}"; then echo "Unknown node: {{node}}"; exit 1; fi
  @version="{{node}}"; loader="${version##*-}"; version="${version%-*}"; ./gradlew --configure-on-demand ":$loader:$version:build" --console=plain

build-all:
  @./gradlew build --console=plain

publish-version version *args:
  @tasks=(":common:{{version}}:publishAllPublicationsToKafMavenRepository"); for loader in $(just list-loaders "{{version}}"); do tasks+=(":$loader:{{version}}:publishAllPublicationsToKafMavenRepository"); done; ./gradlew --configure-on-demand "${tasks[@]}" {{args}} --console=plain

compile-all:
  @tasks=(); for version in $(just list-versions); do tasks+=(":common:$version:compileJava"); for loader in $(just list-loaders "$version"); do tasks+=(":$loader:$version:compileJava"); done; done; ./gradlew --configure-on-demand "${tasks[@]}" --console=plain

run-client node:
  @if ! just list-nodes | grep -Fxq "{{node}}"; then echo "Unknown node: {{node}}"; exit 1; fi
  @version="{{node}}"; loader="${version##*-}"; version="${version%-*}"; ./gradlew --configure-on-demand ":$loader:$version:runClient" --console=plain

scenario-check node scenario="test/scenarios/liteminer.scenario.ts" timeout="240":
  @./teakitw run --node "{{node}}" --scenario "{{scenario}}" --timeout "{{timeout}}"
