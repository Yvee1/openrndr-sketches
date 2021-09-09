package current

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File
import kotlin.system.measureTimeMillis

fun main() {
    println("Started")

    val elapsedTime = measureTimeMillis {
        val repo = FileRepositoryBuilder().setGitDir(File("./.git")).build()
        val git = Git(repo)
    }
    println("Finished in ${elapsedTime / 1000} seconds")
}