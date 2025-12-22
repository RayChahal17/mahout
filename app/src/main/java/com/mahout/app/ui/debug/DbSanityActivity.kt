package com.mahout.app.ui.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.mahout.app.core.dispatchers.DispatcherProvider
import com.mahout.app.data.local.debug.DbSanityDao
import com.mahout.app.databinding.ActivityDbSanityBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.sqlite.db.SimpleSQLiteQuery
import javax.inject.Inject

@AndroidEntryPoint
class DbSanityActivity : ComponentActivity() {

    private lateinit var binding: ActivityDbSanityBinding

    @Inject lateinit var dao: DbSanityDao
    @Inject lateinit var dispatchers: DispatcherProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDbSanityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRefresh.setOnClickListener { refresh() }
        refresh()
    }

    private fun refresh() {
        binding.progress.isVisible = true
        binding.tvOutput.text = ""

        lifecycleScope.launch {
            val text = withContext(dispatchers.io) {
                val tables = dao.listAppTables()
                val lines = buildList {
                    add("Mahout DB sanity")
                    add("Tables: ${tables.size}")
                    add("")

                    for (t in tables) {
                        val count = dao.countRows(SimpleSQLiteQuery("SELECT COUNT(*) FROM `$t`"))
                        add("$t: $count")
                    }
                }
                lines.joinToString("\n")
            }

            binding.progress.isVisible = false
            binding.tvOutput.text = text
        }
    }
}
