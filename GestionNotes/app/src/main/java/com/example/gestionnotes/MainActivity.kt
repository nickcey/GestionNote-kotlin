package com.example.gestionnotes

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Classe de gestion des étudiants
class GestionEtudiants(private val etudiants: MutableList<Etudiant>) {

    fun appliquerOperation(operation: (Etudiant) -> Unit) {
        etudiants.forEach { operation(it) }
    }

    fun getEtudiantsParCritere(critere: (Etudiant) -> Boolean): List<Etudiant> {
        return etudiants.filter(critere)
    }

    fun getMoyenneClasse(): Double {
        return if (etudiants.isNotEmpty()) etudiants.map { it.note }.average() else 0.0
    }

    fun getRepartitionGrades(): Map<String, Int> {
        return etudiants.groupBy { it.grade }.mapValues { it.value.size }
    }

    fun getMeilleurEtudiant(): Etudiant? {
        return etudiants.maxByOrNull { it.note }
    }

    fun getEtudiantsOrdonnes(): List<Etudiant> {
        return etudiants.sortedByDescending { it.note }
    }
}

// Activité principale
class MainActivity : AppCompatActivity() {

    private val PICK_CSV_FILE = 1

    private lateinit var btnImporter: Button
    private lateinit var btnExporter: Button
    private lateinit var btnStats: Button
    private lateinit var txtNomFichier: TextView
    private lateinit var txtNbEtudiants: TextView
    private lateinit var txtMoyenne: TextView
    private lateinit var txtCompteur: TextView
    private lateinit var recyclerEtudiants: RecyclerView

    private val listeEtudiants = mutableListOf<Etudiant>()
    private lateinit var gestionEtudiants: GestionEtudiants
    private lateinit var adapter: EtudiantAdapter
    private var fichierUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()

        gestionEtudiants = GestionEtudiants(listeEtudiants)
    }

    private fun initViews() {
        btnImporter = findViewById(R.id.btn_importer)
        btnExporter = findViewById(R.id.btn_exporter)
        btnStats = findViewById(R.id.btn_stats)
        txtNomFichier = findViewById(R.id.txt_nom_fichier)
        recyclerEtudiants = findViewById(R.id.recycler_etudiants)

        txtNbEtudiants = findViewById(R.id.txt_nb_etudiants)
        txtMoyenne = findViewById(R.id.txt_moyenne)
        txtCompteur = findViewById(R.id.txt_compteur)

        recyclerEtudiants.layoutManager = LinearLayoutManager(this)
        adapter = EtudiantAdapter(listeEtudiants)
        recyclerEtudiants.adapter = adapter
    }

    private fun setupListeners() {
        btnImporter.setOnClickListener { ouvrirChoixFichier() }
        btnExporter.setOnClickListener { exporterFichier() }
        btnStats.setOnClickListener { afficherStatistiques() }
    }

    private fun ouvrirChoixFichier() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/csv", "text/comma-separated-values"))
        }
        startActivityForResult(Intent.createChooser(intent, "Choisir un fichier CSV"), PICK_CSV_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_CSV_FILE && resultCode == RESULT_OK && data != null) {
            fichierUri = data.data
            val fileName = getFileName(fichierUri!!)
            txtNomFichier.text = "Fichier: $fileName"
            lireFichierCSV(fichierUri!!)
        }
    }

    // Fonction getFileName corrigée
    private fun getFileName(uri: Uri): String {
        // Essayer de récupérer via ContentResolver
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        return cursor.getString(nameIndex)
                    }
                }
            }
        }

        // Sinon, essayer avec le chemin
        val path = uri.path
        return if (path != null) {
            val cut = path.lastIndexOf('/')
            if (cut != -1) {
                path.substring(cut + 1)
            } else {
                path
            }
        } else {
            "fichier_inconnu.csv"
        }
    }

    private fun lireFichierCSV(uri: Uri) {
        try {
            listeEtudiants.clear()

            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var premiereLigne = true

                    reader.forEachLine { ligne ->
                        if (premiereLigne) {
                            premiereLigne = false
                            return@forEachLine
                        }

                        if (ligne.trim().isEmpty()) return@forEachLine

                        val colonnes = ligne.split(";")
                        if (colonnes.size >= 2) {
                            val nom = colonnes[0].trim()
                            val noteStr = colonnes[1].trim().replace(",", ".")

                            try {
                                val note = noteStr.toDouble()
                                val etudiant = Etudiant(nom, note)
                                if (etudiant.estValide()) {
                                    listeEtudiants.add(etudiant)
                                }
                            } catch (e: NumberFormatException) {
                                Toast.makeText(this, "Note invalide: $noteStr", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }

            miseAJourAffichage()

            if (listeEtudiants.isNotEmpty()) {
                recyclerEtudiants.visibility = View.VISIBLE
                btnExporter.visibility = View.VISIBLE
                btnStats.visibility = View.VISIBLE
                Toast.makeText(this, "${listeEtudiants.size} étudiants chargés", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun miseAJourAffichage() {
        adapter.notifyDataSetChanged()

        txtNbEtudiants.text = listeEtudiants.size.toString()
        txtCompteur.text = listeEtudiants.size.toString()

        val moyenne = if (listeEtudiants.isNotEmpty()) {
            gestionEtudiants.getMoyenneClasse()
        } else 0.0
        txtMoyenne.text = String.format("%.1f", moyenne)
    }

    private fun afficherStatistiques() {
        if (listeEtudiants.isEmpty()) {
            Toast.makeText(this, "Aucune donnée à analyser", Toast.LENGTH_SHORT).show()
            return
        }

        val moyenne = gestionEtudiants.getMoyenneClasse()
        val meilleur = gestionEtudiants.getMeilleurEtudiant()
        val repartition = gestionEtudiants.getRepartitionGrades()
        val admis = gestionEtudiants.getEtudiantsParCritere { it.estAdmis() }
        val pourcentageAdmis = if (listeEtudiants.isNotEmpty()) {
            (admis.size.toDouble() / listeEtudiants.size) * 100
        } else 0.0

        val stats = StringBuilder()
            .append("📊 STATISTIQUES DÉTAILLÉES\n")
            .append("━━━━━━━━━━━━━━━━━━━━━━━━━\n")
            .append("👥 Total: ${listeEtudiants.size} étudiants\n")
            .append("📈 Moyenne: ${"%.2f".format(moyenne)}/100\n")
            .append("✅ Admis: ${admis.size} (${"%.1f".format(pourcentageAdmis)}%)\n")
            .append("❌ Échec: ${listeEtudiants.size - admis.size}\n\n")

        meilleur?.let {
            stats.append("🏆 Meilleur étudiant:\n")
            stats.append("   ${it.nom} - ${"%.2f".format(it.note)} (${it.grade})\n\n")
        }

        stats.append("📊 RÉPARTITION DES GRADES:\n")
        if (repartition.isNotEmpty()) {
            repartition.toSortedMap().forEach { (grade, count) ->
                stats.append("   $grade: $count étudiant${if (count > 1) "s" else ""}\n")
            }
        } else {
            stats.append("   Aucune donnée\n")
        }

        stats.append("\n📝 APERÇU (5 premiers):\n")
        gestionEtudiants.getEtudiantsOrdonnes()
            .take(5)
            .forEachIndexed { index, etudiant ->
                stats.append("   ${index + 1}. ${etudiant.nom} (${etudiant.grade})\n")
            }

        AlertDialog.Builder(this)
            .setTitle("Statistiques")
            .setMessage(stats.toString())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun exporterFichier() {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Notes_Grades_$timeStamp.csv"

            val dossier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            } else {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            }

            if (dossier != null) {
                val file = File(dossier, fileName)

                FileOutputStream(file).use { fos ->
                    OutputStreamWriter(fos).use { writer ->
                        writer.write("Nom;Note;Grade;Statut\n")
                        listeEtudiants.forEach { etudiant ->
                            val statut = if (etudiant.estAdmis()) "Admis" else "Échec"
                            writer.write("${etudiant.nom};${etudiant.note};${etudiant.grade};$statut\n")
                        }

                        writer.write("\nRÉSUMÉ STATISTIQUES\n")
                        writer.write("Moyenne;${gestionEtudiants.getMoyenneClasse()}\n")
                        writer.write("Total;${listeEtudiants.size}\n")
                        writer.write("Admis;${gestionEtudiants.getEtudiantsParCritere { it.estAdmis() }.size}\n")
                    }
                }

                Toast.makeText(this, "Fichier exporté: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Erreur: dossier inaccessible", Toast.LENGTH_LONG).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Erreur d'export: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}