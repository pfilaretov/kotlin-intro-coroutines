package tasks

import contributors.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

fun loadContributorsCallbacks(service: GitHubService, req: RequestData, updateResults: (List<User>) -> Unit) {
    log.info("loadContributorsCallbacks()")

    service.getOrgReposCall(req.org).onResponse { responseRepos ->
        log.info("got repos response")
        logRepos(req, responseRepos)

        val repos = responseRepos.bodyList()
        val latch = CountDownLatch(repos.size)
        val allUsers = mutableListOf<User>()
        for (repo in repos) {
            log.info("get $repo contributors...")
            service.getRepoContributorsCall(req.org, repo.name).onResponse { responseUsers ->
                logUsers(repo, responseUsers)
                val users = responseUsers.bodyList()
                allUsers += users
                latch.countDown()
            }
        }

        latch.await()
        log.info("updating results on UI")
        updateResults(allUsers.aggregate())
    }

}

inline fun <T> Call<T>.onResponse(crossinline callback: (Response<T>) -> Unit) {
    enqueue(object : Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) {
            callback(response)
        }

        override fun onFailure(call: Call<T>, t: Throwable) {
            log.error("Call failed", t)
        }
    })
}
