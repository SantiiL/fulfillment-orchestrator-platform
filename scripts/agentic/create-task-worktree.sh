#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: $0 <task-id> <branch-name> [base-ref]" >&2
}

require_task_id() {
  local value="$1"
  if [[ ! "${value}" =~ ^[A-Za-z0-9][A-Za-z0-9._-]*$ ]]; then
    echo "Invalid task id: ${value}" >&2
    exit 1
  fi
}

if [[ $# -lt 2 || $# -gt 3 ]]; then
  usage
  exit 1
fi

task_id="$1"
branch_name="$2"
base_ref="${3:-origin/develop}"

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
repo_root="$(cd "${script_dir}/../.." && pwd -P)"
git_common_dir="$(git -C "${repo_root}" rev-parse --path-format=absolute --git-common-dir)"
primary_repo="$(cd "${git_common_dir}/.." && pwd -P)"
worktree_root="${AGENTIC_WORKTREE_ROOT:-$HOME/agentic-worktrees/$(basename "${primary_repo}")}"
task_slug="$(printf '%s' "${task_id}" | tr '[:upper:]' '[:lower:]')"
worktree_path="${worktree_root}/${task_slug}"

if [[ -z "${task_id}" || -z "${branch_name}" ]]; then
  usage
  exit 1
fi

require_task_id "${task_id}"

if ! git check-ref-format --branch "${branch_name}" >/dev/null 2>&1; then
  echo "Invalid branch name: ${branch_name}" >&2
  exit 1
fi

if [[ "${branch_name}" == "main" || "${branch_name}" == "develop" ]]; then
  echo "Refusing to create a task worktree on protected branch: ${branch_name}" >&2
  exit 1
fi

git -C "${primary_repo}" fetch origin

if ! git -C "${primary_repo}" rev-parse --verify --quiet "${base_ref}^{commit}" >/dev/null; then
  echo "Base ref does not resolve to a commit: ${base_ref}" >&2
  exit 1
fi

if git -C "${primary_repo}" show-ref --verify --quiet "refs/heads/${branch_name}"; then
  echo "Branch already exists locally: ${branch_name}" >&2
  exit 1
fi

if [[ -e "${worktree_path}" ]]; then
  echo "Refusing to create worktree because the path already exists: ${worktree_path}" >&2
  exit 1
fi

mkdir -p "${worktree_root}"

git -C "${primary_repo}" worktree add -b "${branch_name}" "${worktree_path}" "${base_ref}"

echo "WORKTREE_PATH=${worktree_path}"
echo "BRANCH=${branch_name}"
