package com.example.gradecalculator

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.gradecalculator.ui.theme.GradeCalculatorTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Calendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GradeCalculatorTheme {
                GradeMasterApp()
            }
        }
    }
}

@Composable
fun AppLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.School,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(40.dp)
        )
    }
}

data class StudentResult(val name: String, val score: Float, val grade: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradeMasterApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    var nameInput by remember { mutableStateOf("") }
    var scoreInput by remember { mutableStateOf("") }
    var calculatedGrade by remember { mutableStateOf("") }
    
    var subjectInput by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var resultsList by remember { mutableStateOf<List<StudentResult>?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }

    val saveExcelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
        onResult = { uri -> uri?.let { scope.launch { resultsList?.let { list -> saveToExcel(context, list, it, subjectInput) } } } }
    )

    val saveWordLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
        onResult = { uri -> uri?.let { scope.launch { resultsList?.let { list -> saveToWord(context, list, it, subjectInput) } } } }
    )

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
        onResult = { uri -> uri?.let { scope.launch { resultsList?.let { list -> saveToPdf(context, list, it, subjectInput) } } } }
    )

    val pickExcelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                scope.launch {
                    isProcessing = true
                    val results = processExcelFileToResults(context, it)
                    isProcessing = false
                    if (results != null) {
                        resultsList = results
                        showExportDialog = true
                    }
                }
            }
        }
    )

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Matière : $subjectInput", fontWeight = FontWeight.ExtraBold) },
            text = { Text("Que souhaitez-vous faire des résultats ?") },
            confirmButton = {},
            dismissButton = {
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Partager directement :", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { 
                            showExportDialog = false
                            scope.launch { resultsList?.let { shareFile(context, it, subjectInput, "excel") } }
                        }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Excel")
                        }
                        FilledTonalButton(onClick = { 
                            showExportDialog = false
                            scope.launch { resultsList?.let { shareFile(context, it, subjectInput, "pdf") } }
                        }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("PDF")
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text("Enregistrer sur l'appareil :", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Button(onClick = { showExportDialog = false; saveExcelLauncher.launch("${subjectInput}.xlsx") }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Save, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Excel (.xlsx)")
                    }
                    Button(onClick = { showExportDialog = false; saveWordLauncher.launch("${subjectInput}.docx") }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Save, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Word (.docx)")
                    }
                    Button(onClick = { showExportDialog = false; savePdfLauncher.launch("${subjectInput}.pdf") }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Save, null)
                        Spacer(Modifier.width(8.dp))
                        Text("PDF (.pdf)")
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppLogo(modifier = Modifier.padding(end = 16.dp))
                        Text("GRADE MASTER PRO", fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp) 
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                )
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f), MaterialTheme.colorScheme.surface))).padding(innerPadding)) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                
                // --- SECTION CALCUL INDIVIDUEL ---
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(32.dp)) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Calcul Rapide", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, label = { Text("Nom de l'étudiant") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), singleLine = true)
                        OutlinedTextField(value = scoreInput, onValueChange = { scoreInput = it; val score = it.toFloatOrNull(); calculatedGrade = if (score != null) GradeCalculator.calculateGrade(score) else "" }, label = { Text("Note / 100") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), singleLine = true)
                        
                        AnimatedVisibility(visible = calculatedGrade.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                            GradeDisplay(name = nameInput, grade = calculatedGrade)
                        }
                    }
                }

                // --- SECTION TRAITEMENT EXCEL ---
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(32.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(8.dp))
                            Text("Mode Matière", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        
                        OutlinedTextField(
                            value = subjectInput,
                            onValueChange = { subjectInput = it },
                            label = { Text("Nom de la matière (OBLIGATOIRE)") },
                            isError = subjectInput.isBlank(),
                            leadingIcon = { Icon(Icons.Default.Bookmark, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true
                        )

                        Text("Importez un fichier Excel pour calculer et exporter les résultats automatiquement.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                        
                        Button(
                            onClick = { pickExcelLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) },
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                            enabled = subjectInput.isNotBlank() && !isProcessing,
                            shape = RoundedCornerShape(16.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                            } else {
                                Icon(Icons.Default.CloudUpload, null)
                                Spacer(Modifier.width(12.dp))
                                Text("CHARGER FICHIER EXCEL", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
                
                Text("Grade Master Pro v1.0 • ${Calendar.getInstance().get(Calendar.YEAR)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
fun GradeDisplay(name: String, grade: String) {
    val color = when(grade) { "A" -> Color(0xFF4CAF50); "F" -> Color(0xFFF44336); else -> MaterialTheme.colorScheme.primary }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 12.dp)) {
        if (name.isNotBlank()) Text(name.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.outline, letterSpacing = 2.sp)
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.size(120.dp).clip(CircleShape).background(color.copy(alpha = 0.1f)).padding(10.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
            Text(text = grade, fontSize = 52.sp, fontWeight = FontWeight.Black, color = Color.White)
        }
    }
}

suspend fun shareFile(context: Context, results: List<StudentResult>, subject: String, type: String) {
    withContext(Dispatchers.IO) {
        try {
            val sanitized = subject.replace("[^a-zA-Z0-9]".toRegex(), "_")
            val extension = if (type == "excel") "xlsx" else "pdf"
            val fileName = "${sanitized}.${extension}"
            val tempFile = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(tempFile)

            if (type == "excel") {
                val workbook = org.apache.poi.xssf.usermodel.XSSFWorkbook()
                val sheet = workbook.createSheet("Resultats")
                val header = sheet.createRow(0)
                header.createCell(0).setCellValue("Nom")
                header.createCell(1).setCellValue("Note")
                header.createCell(2).setCellValue("Grade")
                results.forEachIndexed { i, res ->
                    val row = sheet.createRow(i + 1)
                    row.createCell(0).setCellValue(res.name)
                    row.createCell(1).setCellValue(res.score.toDouble())
                    row.createCell(2).setCellValue(res.grade)
                }
                workbook.write(outputStream)
                workbook.close()
            } else {
                val pdfDocument = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                val paint = Paint()
                paint.textSize = 24f; paint.isFakeBoldText = true
                canvas.drawText("Matière : $subject", 50f, 50f, paint)
                paint.textSize = 14f; paint.isFakeBoldText = false
                var y = 100f
                results.forEach { res ->
                    canvas.drawText("${res.name} : ${res.score} -> ${res.grade}", 50f, y, paint)
                    y += 25f
                }
                pdfDocument.finishPage(page)
                pdfDocument.writeTo(outputStream)
                pdfDocument.close()
            }
            outputStream.close()

            val uri = FileProvider.getUriForFile(context, "com.example.gradecalculator.fileprovider", tempFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                this.type = if (type == "excel") "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" else "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Partager avec..."))
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { 
                Toast.makeText(context, "Erreur de partage : ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

suspend fun processExcelFileToResults(context: Context, uri: Uri): List<StudentResult>? {
    return withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val workbook = WorkbookFactory.create(inputStream)
                val sheet = workbook.getSheetAt(0)
                val results = mutableListOf<StudentResult>()
                for (i in 1..sheet.lastRowNum) {
                    val row = sheet.getRow(i) ?: continue
                    val name = row.getCell(0)?.toString() ?: "Inconnu"
                    val scoreCell = row.getCell(1)
                    val score = when (scoreCell?.cellType) {
                        org.apache.poi.ss.usermodel.CellType.NUMERIC -> scoreCell.numericCellValue.toFloat()
                        org.apache.poi.ss.usermodel.CellType.STRING -> scoreCell.stringCellValue.toFloatOrNull() ?: 0f
                        else -> 0f
                    }
                    results.add(StudentResult(name, score, GradeCalculator.calculateGrade(score)))
                }
                results
            }
        } catch (e: Exception) { null }
    }
}

suspend fun saveToExcel(context: Context, results: List<StudentResult>, uri: Uri, subject: String) {
    withContext(Dispatchers.IO) {
        try {
            val workbook = org.apache.poi.xssf.usermodel.XSSFWorkbook()
            val sheet = workbook.createSheet("Resultats")
            val subRow = sheet.createRow(0); subRow.createCell(0).setCellValue("Matière :"); subRow.createCell(1).setCellValue(subject)
            val header = sheet.createRow(2); header.createCell(0).setCellValue("Nom"); header.createCell(1).setCellValue("Note"); header.createCell(2).setCellValue("Grade")
            results.forEachIndexed { i, res ->
                val row = sheet.createRow(i + 3)
                row.createCell(0).setCellValue(res.name)
                row.createCell(1).setCellValue(res.score.toDouble())
                row.createCell(2).setCellValue(res.grade)
            }
            context.contentResolver.openOutputStream(uri)?.use { workbook.write(it) }
            workbook.close()
        } catch (e: Exception) {}
    }
}

suspend fun saveToWord(context: Context, results: List<StudentResult>, uri: Uri, subject: String) {
    withContext(Dispatchers.IO) {
        try {
            val doc = XWPFDocument()
            val title = doc.createParagraph().createRun(); title.isBold = true; title.fontSize = 20; title.setText("Résultats : $subject")
            val table = doc.createTable(results.size + 1, 3)
            val header = table.getRow(0); header.getCell(0).setText("Nom"); header.getCell(1).setText("Note"); header.getCell(2).setText("Grade")
            results.forEachIndexed { i, res ->
                val row = table.getRow(i + 1)
                row.getCell(0).setText(res.name)
                row.getCell(1).setText(res.score.toString())
                row.getCell(2).setText(res.grade)
            }
            context.contentResolver.openOutputStream(uri)?.use { doc.write(it) }
            doc.close()
        } catch (e: Exception) {}
    }
}

suspend fun saveToPdf(context: Context, results: List<StudentResult>, uri: Uri, subject: String) {
    withContext(Dispatchers.IO) {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint()
            paint.textSize = 24f; paint.isFakeBoldText = true; canvas.drawText("Résultats : $subject", 50f, 50f, paint)
            paint.textSize = 14f; paint.isFakeBoldText = false
            var y = 100f
            canvas.drawText("Nom", 50f, y, paint); canvas.drawText("Note", 300f, y, paint); canvas.drawText("Grade", 450f, y, paint)
            y += 30f
            results.forEach { res ->
                canvas.drawText(res.name, 50f, y, paint)
                canvas.drawText(res.score.toString(), 300f, y, paint)
                canvas.drawText(res.grade, 450f, y, paint)
                y += 25f
                if (y > 800f) return@forEach 
            }
            pdfDocument.finishPage(page)
            context.contentResolver.openOutputStream(uri)?.use { pdfDocument.writeTo(it) }
            pdfDocument.close()
        } catch (e: Exception) {}
    }
}
