#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: $0 [--force] <task-id-or-worktree-path>" >&2
}

require_task_id() {
  local value="$1"
  if [[ ! "${value}" =~ ^[A-Za-z0-9][A-Za-z0-9._-]*$ ]]; then
    echo "Invalid task id: ${value}" >&2
    exit 1
  fi
}

force_remove="false"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --force)
      force_remove="true"
      shift
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      break
      ;;
  esac
done

if [[ $# -ne 1 ]]; then
  usage
  exit 1
fi

input_value="$1"
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
repo_root="$(cd "${script_dir}/../.." && pwd -P)"
git_common_dir="$(git -C "${repo_root}" rev-parse --path-format=absolute --git-common-dir)"
primary_repo="$(cd "${git_common_dir}/.." && pwd -P)"
expected_worktree_root="${AGENTIC_WORKTREE_ROOT:-$HOME/agentic-worktrees/$(basename "${primary_repo}")}"

if [[ -d "${input_value}" ]]; then
  candidate_path="$(cd "${input_value}" && pwd -P)"
else
  require_task_id "${input_value}"
  task_slug="$(printf '%s' "${input_value}" | tr '[:upper:]' '[:lower:]')"
  candidate_path="${expected_worktree_root}/${task_slug}"
fi

if [[ ! -d "${candidate_path}" ]]; then
  echo "Worktree directory not found: ${candidate_path}" >&2
  exit 1
fi

if ! git -C "${candidate_path}" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "Path is not a Git worktree: ${candidate_path}" >&2
  exit 1
fi

worktree_path="$(git -C "${candidate_path}" rev-parse --show-toplevel)"

if [[ "${worktree_path}" == "${primary_repo}" ]]; then
  echo "Refusing to remove the primary repository worktree: ${primary_repo}" >&2
  exit 1
fi

if [[ "${worktree_path}" == "${repo_root}" ]]; then
  echo "Refusing to remove the current checkout: ${repo_root}" >&2
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
  echo "Refusing to remove protected branch worktree: ${branch_name}" >&2
  exit 1
fi

if ! git -C "${primary_repo}" worktree list --porcelain | grep -Fqx "worktree ${worktree_path}"; then
  echo "Path is not a registered linked worktree: ${worktree_path}" >&2
  exit 1
fi

if [[ "${force_remove}" != "true" ]] && [[ -n "$(git -C "${worktree_path}" status --porcelain)" ]]; then
  echo "Worktree is dirty. Re-run with --force to remove it through Git." >&2
  exit 1
fi

remove_args=()
if [[ "${force_remove}" == "true" ]]; then
  remove_args+=(--force)
fi

git -C "${primary_repo}" worktree remove "${remove_args[@]}" "${worktree_path}"
git -C "${primary_repo}" worktree prune

echo "REMOVED_WORKTREE=${worktree_path}"
