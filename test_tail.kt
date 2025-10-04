import java.io.File

fun main() {
    val file = File("C:/Users/Admin/Desktop/yhchat/app/src/main/java/com/yhchat/canary/data/model/User.kt")
    val lines = file.readLines()
    val totalLines = lines.size
    println("Total lines: $totalLines")
    
    // Print last 50 lines
    val start = kotlin.math.max(0, totalLines - 50)
    for (i in start until totalLines) {
        println("${i + 1}: ${lines[i]}")
    }
}