package useful

import mu.KotlinLogging
import org.openrndr.AssetMetadata
import org.openrndr.Extension
import org.openrndr.Program
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

val logger = KotlinLogging.logger { }

class NativeGitArchiver : Extension {
    override var enabled: Boolean = true

    var commitOnRun = false
    var commitOnRequestAssets = true

    var autoCommitMessage = "auto commit"

    private val dir = File(".")

    fun commitChanges() {
        val gitStatus = "git status --porcelain".runCommand(dir)!!
        if (gitStatus.first.isNotBlank()){
            if (gitStatus.first.contains("Not a git repository")){
                logger.error { "Can't commit changes because the working directory is not a git repository" }
            } else {
                "git add .".runCommand(dir)
                "git commit -m \"${autoCommitMessage}\"".runCommand(dir)
                logger.info { "git repository is now at ${commitHash}" }
            }
        } else {
            logger.info { "no changes" }
        }
    }

//    fun tag(name: String): Boolean {
//
//    }

    val commitHash: String
        get() {
            return "git rev-parse --short HEAD".runCommand(dir)!!.first
        }

    override fun setup(program: Program) {
        val oldMetadataFunction = program.assetMetadata
        program.assetMetadata = {
            val oldMetadata = oldMetadataFunction()
            val commitHash = this.commitHash
            program.assetProperties["git-commit-hash"] = commitHash
            AssetMetadata(
                oldMetadata.programName,
                "${oldMetadata.assetBaseName}-$commitHash",
                program.assetProperties.mapValues { it.value })
        }

        program.requestAssets.listeners.add(0, {
            if (commitOnRequestAssets) {
                commitChanges()
            }
        })


        if (commitOnRun) {
            commitChanges()
        }
    }
}

fun String.runCommand(workingDir: File): Pair<String, String>? {
    try {
        val parts = this.split("\\s".toRegex())
        val proc = ProcessBuilder(*parts.toTypedArray())
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        proc.waitFor(60, TimeUnit.MINUTES)
        return Pair(proc.inputStream.bufferedReader().readText(), proc.errorStream.bufferedReader().readText())
    } catch(e: IOException) {
        e.printStackTrace()
        return null
    }
}