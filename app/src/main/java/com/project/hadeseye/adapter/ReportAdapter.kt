package com.project.hadeseye.adapter

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DatabaseReference
import com.project.hadeseye.R
import com.project.hadeseye.model.ScanHistory
import java.text.SimpleDateFormat
import java.util.*

class ReportAdapter(
    private val context: Context,
    private val databaseRef: DatabaseReference,
    private val reportList: MutableList<ScanHistory>
) : RecyclerView.Adapter<ReportAdapter.ReportViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.safe_item, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val item = reportList[position]

        holder.tvTitle.text = when {
            !item.ip.isNullOrBlank() && item.ip != "N/A" && item.ip != "Unknown" -> "IP: ${item.ip}"
            !item.url.isNullOrBlank() && item.url != "N/A" && item.url != "Unknown" -> "URL: ${item.url}"
            !item.fileName.isNullOrBlank() && item.fileName != "N/A" -> "File: ${item.fileName}"
            !item.domain.isNullOrBlank() && item.domain != "N/A" -> "Domain: ${item.domain}"
            else -> "Unknown Scan"
        }

        val timestamp = item.date.toLongOrNull() ?: 0L
        val date = Date(timestamp)
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        holder.tvDate.text = "Scanned on ${sdf.format(date)}"

        holder.tvStatus.text = item.status
        when (item.status) {
            "Safe" -> {
                holder.tvStatus.setTextColor(holder.itemView.resources.getColor(R.color.green))
                holder.statusDot.setBackgroundResource(R.drawable.green_circle)
            }
            "Threat" -> {
                holder.tvStatus.setTextColor(holder.itemView.resources.getColor(R.color.red))
                holder.statusDot.setBackgroundResource(R.drawable.red_circle)
            }
            "Malicious" -> {
                holder.tvStatus.setTextColor(holder.itemView.resources.getColor(R.color.yellow))
                holder.statusDot.setBackgroundResource(R.drawable.yellow_circle)
            }
            else -> {
                holder.tvStatus.setTextColor(holder.itemView.resources.getColor(R.color.gray))
                holder.statusDot.setBackgroundResource(R.drawable.green_circle)
            }
        }

        holder.btnDelete.setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle("Delete Report")
                .setMessage("Are you sure you want to delete this report?")
                .setPositiveButton("Delete") { _, _ ->
                    holder.btnDelete.isEnabled = false
                    deleteReport(item, position, holder)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun deleteReport(item: ScanHistory, position: Int, holder: ReportViewHolder) {
        try {
            val scanId = item.scanId
            if (scanId.isNullOrBlank()) {
                Toast.makeText(context, "Invalid scan ID.", Toast.LENGTH_SHORT).show()
                holder.btnDelete.isEnabled = true
                return
            }

            val scanRef = databaseRef.child(scanId)

            scanRef.removeValue()
                .addOnSuccessListener {
                    if (position >= 0 && position < reportList.size) {
                        reportList.removeAt(position)
                        notifyItemRemoved(position)
                        notifyItemRangeChanged(position, reportList.size)
                    }
                    Toast.makeText(context, "Report deleted successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to delete: ${e.message}", Toast.LENGTH_SHORT).show()
                    holder.btnDelete.isEnabled = true
                }

        } catch (e: Exception) {
            Log.e("ReportAdapter", "Error deleting report", e)
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            holder.btnDelete.isEnabled = true
        }
    }

    override fun getItemCount(): Int = reportList.size

    class ReportViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvHistoryTitle)
        val tvDate: TextView = view.findViewById(R.id.tvHistoryDate)
        val tvStatus: TextView = view.findViewById(R.id.tvHistoryStatus)
        val statusDot: View = view.findViewById(R.id.statusDot)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteHistory)
    }
}
