package io.github.mangi.eta.data.repository

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal object AutoBackupScheduler {
    private const val JOB_ID = 12031
    private const val PREFS = "eta_auto_backup"
    const val PREFIX = "eta-auto-"
    private val lock = Mutex()
    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    fun enabled(context: Context) = prefs(context).getBoolean("enabled", false)
    fun status(context: Context): String = prefs(context).getString("status", "").orEmpty()
    fun configure(context: Context, tree: Uri?) {
        check(prefs(context).edit().putBoolean("enabled", tree != null)
            .putString("tree", tree?.toString()).commit())
        schedule(context)
    }
    fun schedule(context: Context) {
        val scheduler = context.getSystemService(JobScheduler::class.java)
        if (!enabled(context)) { scheduler.cancel(JOB_ID); return }
        val result = scheduler.schedule(JobInfo.Builder(JOB_ID,
            ComponentName(context, AutoBackupJobService::class.java))
            .setPersisted(true).setRequiresCharging(true).setRequiresBatteryNotLow(true)
            .setPeriodic(24L * 60 * 60 * 1000).build())
        check(result == JobScheduler.RESULT_SUCCESS) { "BACKUP_SCHEDULING_FAILED" }
    }
    fun ownedBackup(name: String): Boolean =
        Regex("eta-auto-[0-9]+-[a-f0-9-]{36}\\.json").matches(name)

    /** Export before pruning. Failed exports are deleted; unrelated documents are never touched. */
    suspend fun run(context: Context) = lock.withLock {
        check(enabled(context)) { "BACKUP_DISABLED" }
        val raw = prefs(context).getString("tree", null) ?: error("BACKUP_FOLDER_REQUIRED")
        val tree = Uri.parse(raw)
        val resolver = context.contentResolver
        val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val name = "$PREFIX${System.currentTimeMillis()}-${UUID.randomUUID()}.json"
        val document = DocumentsContract.createDocument(resolver, parent, "application/json", name)
            ?: error("BACKUP_CREATE_FAILED")
        try {
            resolver.openOutputStream(document, "wt")?.use { EtaBackupRepository.export(context, it) }
                ?: error("BACKUP_WRITE_FAILED")
        } catch (error: Exception) {
            runCatching { DocumentsContract.deleteDocument(resolver, document) }
            throw error
        }
        // Keep the successful new backup plus six previous owned files.
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val previous = mutableListOf<Pair<String, String>>()
        resolver.query(children, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(0); val childName = cursor.getString(1).orEmpty()
                if (ownedBackup(childName) && id != DocumentsContract.getDocumentId(document)) previous += childName to id
            }
        }
        previous.sortedByDescending { it.first }.drop(6).forEach { (_, id) ->
            runCatching { DocumentsContract.deleteDocument(resolver, DocumentsContract.buildDocumentUriUsingTree(tree, id)) }
        }
        prefs(context).edit().putString("status", "OK ${System.currentTimeMillis()}").apply()
    }
    fun failed(context: Context) { prefs(context).edit().putString("status", "FAILED").apply() }
}

internal class AutoBackupJobService : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var work: Job? = null
    override fun onStartJob(params: JobParameters): Boolean {
        work = scope.launch {
            try { AutoBackupScheduler.run(applicationContext) }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { AutoBackupScheduler.failed(applicationContext) }
            jobFinished(params, false)
        }
        return true
    }
    override fun onStopJob(params: JobParameters): Boolean { work?.cancel(); return false }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}

internal class AutoBackupReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED))
            runCatching { AutoBackupScheduler.schedule(context) }
    }
}
