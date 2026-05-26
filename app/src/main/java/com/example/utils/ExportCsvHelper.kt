package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.WeightEntry
import java.io.File
import java.io.FileWriter

object ExportCsvHelper {

    fun exportWeightDataToCsv(context: Context, entries: List<WeightEntry>) {
        if (entries.isEmpty()) {
            Toast.makeText(context, "Tidak ada data untuk diekspor", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Create a temp file in cache directory
            val csvFile = File(context.cacheDir, "Histori_Berat_Badan.csv")
            val writer = FileWriter(csvFile)

            // Write CSV Header (Indonesian for user understanding)
            writer.append("ID,Tanggal,Berat (kg),Lingkar Pinggang (cm),Konsumsi Air (ml),Durasi Olahraga (menit),Status Perasaan,Catatan\n")

            // Write Entries
            for (entry in entries) {
                val cleanNote = entry.notes.replace(",", ";").replace("\n", " ")
                writer.append("${entry.id},")
                writer.append("${entry.dateString},")
                writer.append("${entry.weight},")
                writer.append("${entry.waistCircumferenceCm},")
                writer.append("${entry.waterIntakeMl},")
                writer.append("${entry.exerciseMinutes},")
                writer.append("${entry.feelingEmoji},")
                writer.append("$cleanNote\n")
            }

            writer.flush()
            writer.close()

            // Share the CSV File using FileProvider
            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, csvFile)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "Ekspor Data Progres Berat Badan")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Ekspor Data via:").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)

        } catch (e: Exception) {
            Log.e("ExportCsvHelper", "Error exporting CSV file", e)
            Toast.makeText(context, "Gagal mengekspor CSV: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
