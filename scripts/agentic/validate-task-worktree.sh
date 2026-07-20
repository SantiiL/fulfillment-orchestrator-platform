#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: $0 [worktree-path]" >&2
}

if [[ $# -gt 1 ]]; then
  usage
  exit 1
fi

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
repo_root="$(cd "${script_dir}/../.." && pwd -P)"
git_common_dir="$(git -C "${repo_root}" rev-parse --path-format=absolute --git-common-dir)"
primary_repo="$(cd "${git_common_dir}/.." && pwd -P)"
expected_worktree_root="${AGENTIC_WORKTREE_ROOT:-$HOME/agentic-worktrees/$(basename "${primary_repo}")}"
requested_path="${1:-.}"

if [[ ! -d "${requested_path}" ]]; then
  echo "Directory not found: ${requested_path}" >&2
  exit 1
fi

resolved_path="$(cd "${requested_path}" && pwd -P)"

if ! git -C "${resolved_path}" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "Path is not inside a Git worktree: ${resolved_path}" >&2
  exit 1
fi

worktree_path="$(git -C "${resolved_path}" rev-parse --show-toplevel)"

if [[ "${worktree_path}" == "${primary_repo}" ]]; then
  echo "Refusing to validate the primary checkout: ${primary_repo}" >&2
  exit 1
fi

case "${worktree_path}" in
  "${expected_worktree_root}"/*) ;;
  *)
    echo "Worktree path is outside the expected root: ${worktree_path}" >&2
    echo "Expected root: ${expected_worktree_root}" >&2
    exit 1
    ;;
esac

if ! branch_name="$(git -C "${worktree_path}" symbolic-ref --quiet --short HEAD)"; then
  echo "Worktree must be on a local branch: ${worktree_path}" >&2
  exit 1
fi

if [[ "${branch_name}" == "main" || "${branch_name}" == "develop" ]]; then
  echo "Refusing to validate protected branch worktree: ${branch_name}" >&2
  exit 1
fi

if ! git -C "${primary_repo}" worktree list --porcelain | grep -Fqx "worktree ${worktree_path}"; then
  echo "Path is not a registered linked worktree: ${worktree_path}" >&2
  exit 1
fi

echo "WORKTREE_PATH=${worktree_path}"
echo "BRANCH=${branch_name}"
git -C "${worktree_path}" status --short --branch

java_version_output="$(java -version 2>&1 | head -n 1)"
echo "JAVA_VERSION=${java_version_output}"

if [[ ! "${java_version_output}" =~ \"21([.-]|$) ]]; then
  echo "Java 21 is required." >&2
  exit 1
fi

if ! docker info >/dev/null 2>&1; then
  echo "Docker is not reachable." >&2
  exit 1
fi
echo "DOCKER_REACHABLE=yes"

if [[ ! -f "${worktree_path}/gradlew" || ! -f "${worktree_path}/gradle/wrapper/gradle-wrapper.properties" ]]; then
  echo "Gradle Wrapper is missing." >&2
  exit 1
fi
echo "GRADLE_WRAPPER_PRESENT=yes"

if [[ ! -x "${worktree_path}/gradlew" ]]; then
  echo "Gradle Wrapper must be executable." >&2
  exit 1
fi
echo "GRADLE_WRAPPER_EXECUTABLE=yes"
