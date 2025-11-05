package com.project.hadeseye.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.project.hadeseye.R
import com.project.hadeseye.model.ScanHistory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportAdapter(private val reportList: MutableList<ScanHistory>) :
    RecyclerView.Adapter<ReportAdapter.ReportViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.safe_item, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val item = reportList[position]

        // 🧠 Logic: show IP if available, otherwise show URL
// 🧠 Logic: show IP if valid, else show URL, else file name
        holder.tvTitle.text = when {
            !item.ip.isNullOrBlank() && item.ip != "N/A" && item.ip != "Unknown" -> "IP: ${item.ip}"
            !item.url.isNullOrBlank() && item.url != "N/A" && item.url != "Unknown" -> "URL: ${item.url}"
            !item.fileName.isNullOrBlank() && item.fileName != "N/A" -> "File: ${item.fileName}"
            !item.domain.isNullOrBlank() && item.domain != "N/A" -> "Domain: ${item.domain}"
            else -> "Unknown Scan"
        }


        // Format timestamp to readable date
        val timestamp = item.date.toLongOrNull() ?: 0L
        val date = Date(timestamp)
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val formattedDate = sdf.format(date)
        holder.tvDate.text = "Scanned on $formattedDate"

        // Status text + color indicators
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
        }
    }

    override fun getItemCount(): Int = reportList.size

    class ReportViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvHistoryTitle)
        val tvDate: TextView = view.findViewById(R.id.tvHistoryDate)
        val tvStatus: TextView = view.findViewById(R.id.tvHistoryStatus)
        val statusDot: View = view.findViewById(R.id.statusDot)
    }
}
