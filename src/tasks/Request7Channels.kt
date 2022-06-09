package tasks

import contributors.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

suspend fun loadContributorsChannels(
    service: GitHubService,
    req: RequestData,
    updateResults: suspend (List<User>, completed: Boolean) -> Unit
) {
    coroutineScope {
        val repos = service
            .getOrgRepos(req.org)
            .also { logRepos(req, it) }
            .bodyList()

        val channel = Channel<List<User>>(repos.size)
        // producers
        for (repo in repos) {
            launch {
                log.info("${repo.name}: loading contributors...")
                val contributors = service
                    .getRepoContributors(req.org, repo.name)
                    .also { logUsers(repo, it) }
                    .bodyList()

                log.info("${repo.name}: got response, sending to channel...")
                channel.send(contributors)
                log.info("${repo.name}: sent to channel.")
            }
        }

        // receiver
        var results = emptyList<User>()
        repeat(repos.size) {
            log.info("$it: receiving...")
            val repoContributors = channel.receive()

            log.info("$it: received, updating results.")
            results = (results + repoContributors).aggregate()
            updateResults(results, it == repos.lastIndex)
        }

    }
}
