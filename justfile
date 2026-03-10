set shell := ["bash", "-cu"]

default:
  @just --list

# Resolved at runtime so new version folders are picked up automatically.
version := shell("ls -1d */ | sed 's:/$::' | grep -E '^[0-9]' | sort -V | tail -n1")

list-versions:
  @ls -1d */ | sed 's:/$::' | grep -E '^[0-9]' | sort -V

versions: list-versions

latest:
  @echo {{version}}

# Check if a loader is included in a version's settings.gradle.
loader-enabled version loader:
  @if [ ! -f "{{version}}/settings.gradle" ]; then echo "false"; exit 0; fi
  @if (command -v rg >/dev/null 2>&1 && rg -q "^[[:space:]]*include\\([\"']{{loader}}[\"']\\)|^[[:space:]]*include[[:space:]]+[\"']{{loader}}[\"']" "{{version}}/settings.gradle") || \
     (! command -v rg >/dev/null 2>&1 && grep -Eq "^[[:space:]]*include\\([\"']{{loader}}[\"']\\)|^[[:space:]]*include[[:space:]]+[\"']{{loader}}[\"']" "{{version}}/settings.gradle"); then \
    echo "true"; \
  else \
    echo "false"; \
  fi

# Run Gradle inside a specific version folder, switching the JVM based on
# `java_version=` in <version>/gradle.properties.
#
# We avoid calling `sdk use` because `sdk` is a shell function and is unreliable
# in non-interactive shells; instead we set JAVA_HOME directly to SDKMAN's
# installed candidates.
with-java version *args:
  @cd "{{version}}"; \
    java_version=$(sed -nE 's/^java_version=([0-9]+).*/\1/p' gradle.properties | head -n1); \
    sdkman_path=""; \
    case "$java_version" in \
      21) sdkman_path="$HOME/.sdkman/candidates/java/21.0.9-tem" ;; \
      25) sdkman_path="$HOME/.sdkman/candidates/java/25.0.2-tem" ;; \
      *) echo "Unsupported or missing java_version=$java_version in {{version}}/gradle.properties"; exit 1 ;; \
    esac; \
    if [ -d "$sdkman_path" ]; then \
      export JAVA_HOME="$sdkman_path"; \
      export PATH="$JAVA_HOME/bin:$PATH"; \
    elif [ -n "$JAVA_HOME" ] && [ -d "$JAVA_HOME" ]; then \
      echo "SDKMAN path not found; using existing JAVA_HOME=$JAVA_HOME"; \
    else \
      echo "No valid Java installation found for java_version=$java_version"; exit 1; \
    fi; \
    ./gradlew {{args}}

# Run arbitrary Gradle tasks.
# - If the first arg is a version directory, run only there.
# - Otherwise run across all versions.
run first="" *rest:
  @if [ -z "{{first}}" ]; then echo "Usage: just run [version] <gradle args>"; exit 1; fi
  @if [ -d "{{first}}" ] && echo "{{first}}" | grep -Eq '^[0-9]'; then \
    if [ -z "{{rest}}" ]; then echo "Usage: just run [version] <gradle args>"; exit 1; fi; \
    just with-java "{{first}}" {{rest}}; \
  else \
    for v in $(ls -1d */ | sed 's:/$::' | grep -E '^[0-9]' | sort -V); do \
      echo "==> $v"; just with-java "$v" {{first}} {{rest}}; \
    done; \
  fi

build version="":
  @if [ -z "{{version}}" ]; then \
    for v in $(ls -1d */ | sed 's:/$::' | grep -E '^[0-9]' | sort -V); do \
      echo "==> $v"; \
      for loader in fabric forge neoforge; do \
        if [ "$(just loader-enabled "$v" "$loader")" = "true" ]; then \
          just with-java "$v" :$loader:build; \
        else \
          echo "Skipping $v:$loader (not included in settings.gradle)"; \
        fi; \
      done; \
    done; \
  else \
    if [ ! -d "{{version}}" ]; then echo "Version {{version}} not found."; exit 1; fi; \
    for loader in fabric forge neoforge; do \
      if [ "$(just loader-enabled "{{version}}" "$loader")" = "true" ]; then \
        just with-java "{{version}}" :$loader:build; \
      else \
        echo "Skipping {{version}}:$loader (not included in settings.gradle)"; \
      fi; \
    done; \
  fi

test version="":
  @if [ -z "{{version}}" ]; then \
    for v in $(ls -1d */ | sed 's:/$::' | grep -E '^[0-9]' | sort -V); do \
      echo "==> $v"; just with-java "$v" test; \
    done; \
  else \
    if [ ! -d "{{version}}" ]; then echo "Version {{version}} not found."; exit 1; fi; \
    just with-java "{{version}}" test; \
  fi

publish version="":
  @if [ -z "{{version}}" ]; then \
    for v in $(just list-versions); do \
      echo "==> $v"; just publish-version "$v"; \
    done; \
  else \
    if [ ! -d "{{version}}" ]; then echo "Version {{version}} not found."; exit 1; fi; \
    just publish-version "{{version}}"; \
  fi

publish-version version:
  @just publish-common "{{version}}"
  @for loader in fabric forge neoforge; do \
    if [ "$(just loader-enabled "{{version}}" "$loader")" = "true" ]; then \
      just publish-loader "{{version}}" "$loader"; \
    else \
      echo "Skipping {{version}}:$loader (not included in settings.gradle)"; \
    fi; \
  done

publish-common version *args:
  @just with-java "{{version}}" :common:publishAllPublicationsToKafMavenRepository {{args}}

publish-loader version loader *args:
  @if [ "$(just loader-enabled "{{version}}" "{{loader}}")" = "true" ]; then \
    just with-java "{{version}}" :{{loader}}:publishAllPublicationsToKafMavenRepository {{args}}; \
  else \
    echo "Skipping {{version}}:{{loader}} (not included in settings.gradle)"; \
  fi

changed base="origin/main":
  @if ! git rev-parse --verify "{{base}}" >/dev/null 2>&1; then echo "Base ref {{base}} not found."; exit 1; fi
  @changed=$(git diff --name-only "{{base}}"...HEAD | grep -oP '^[0-9]+\\.[0-9]+[^/]*' | sort -u); \
  if [ -z "$changed" ]; then echo "No changed versions."; exit 0; fi; \
  echo "$changed"

build-changed base="origin/main":
  @if ! git rev-parse --verify "{{base}}" >/dev/null 2>&1; then echo "Base ref {{base}} not found."; exit 1; fi
  @changed=$(git diff --name-only "{{base}}"...HEAD | grep -oP '^[0-9]+\\.[0-9]+[^/]*' | sort -u); \
  if [ -z "$changed" ]; then echo "No changed versions."; exit 0; fi; \
  for v in $changed; do \
    echo "==> $v"; \
    for loader in fabric forge neoforge; do \
      if [ "$(just loader-enabled "$v" "$loader")" = "true" ]; then \
        just with-java "$v" :$loader:build; \
      else \
        echo "Skipping $v:$loader (not included in settings.gradle)"; \
      fi; \
    done; \
  done

build-loader version loader *args:
  @if [ "$(just loader-enabled "{{version}}" "{{loader}}")" = "true" ]; then \
    just with-java "{{version}}" :{{loader}}:build {{args}}; \
  else \
    echo "Skipping {{version}}:{{loader}} (not included in settings.gradle)"; \
  fi

test-changed base="origin/main":
  @if ! git rev-parse --verify "{{base}}" >/dev/null 2>&1; then echo "Base ref {{base}} not found."; exit 1; fi
  @changed=$(git diff --name-only "{{base}}"...HEAD | grep -oP '^[0-9]+\\.[0-9]+[^/]*' | sort -u); \
  if [ -z "$changed" ]; then echo "No changed versions."; exit 0; fi; \
  for v in $changed; do \
    echo "==> $v"; just with-java "$v" test; \
  done

# Copy an existing version folder to create a new one.
new-version from to:
  @if [ -e "{{to}}" ]; then echo "Target {{to}} already exists."; exit 1; fi
  cp -r "{{from}}" "{{to}}"
