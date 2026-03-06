package com.example.gestionnotes

data class Etudiant(
    var nom: String,
    var note: Double
) {
    var grade: String = calculerGrade(note)
        private set

    // Supprimez cette fonction setNote car elle est en conflit avec le setter automatique
    // La mise à jour de la note se fera directement via la propriété note
    // Et le grade sera automatiquement recalculé via le setter personnalisé ci-dessous

    private fun calculerGrade(note: Double): String {
        return when {
            note >= 97 -> "A+"
            note >= 93 -> "A"
            note >= 90 -> "A-"
            note >= 87 -> "B+"
            note >= 83 -> "B"
            note >= 80 -> "B-"
            note >= 77 -> "C+"
            note >= 73 -> "C"
            note >= 70 -> "C-"
            note >= 67 -> "D+"
            note >= 63 -> "D"
            note >= 60 -> "D-"
            else -> "F"
        }
    }

    // Setter personnalisé pour recalculer le grade quand la note change
    fun updateNote(nouvelleNote: Double) {
        note = nouvelleNote
        grade = calculerGrade(note)
    }

    fun estValide(): Boolean {
        return nom.isNotBlank() && note in 0.0..100.0
    }

    fun formaterAffichage(): String {
        val statut = when {
            note >= 60 -> "✅ Réussi"
            else -> "❌ Échoué"
        }
        return "$nom - Note: ${"%.2f".format(note)} ($grade) $statut"
    }

    fun estAdmis(): Boolean = note >= 60
}