package tasks

import contributors.*

suspend fun loadContributorsProgress(
    service: GitHubService,
    req: RequestData,
    updateResults: suspend (List<User>, completed: Boolean) -> Unit
) {
    val repos = service
        .getOrgRepos(req.org)
        .also { logRepos(req, it) }
        .body() ?: listOf()

    var results = emptyList<User>()
    for ((index, repo) in repos.withIndex()) {
        val repoContributors = service
            .getRepoContributors(req.org, repo.name)
            .also { logUsers(repo, it) }
            .bodyList()
        results = (results + repoContributors).aggregate()
        updateResults(results, index == repos.lastIndex)
    }
}
