import com.vladsch.flexmark.ast.FencedCodeBlock
import com.vladsch.flexmark.ast.Node
import com.vladsch.flexmark.ast.SoftLineBreak
import com.vladsch.flexmark.ast.Text
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterBlock
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension
import com.vladsch.flexmark.formatter.internal.Formatter
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.options.MutableDataSet
import java.io.File
import java.util.regex.Pattern

fun Any?.println() = println(this)

// markdown 文件目录
const val DOC_PATH = """D:\docs"""

const val HTML_COMMENT_START = "<!--"
const val HTML_COMMENT_END = "-->"
val options = MutableDataSet().set(Parser.EXTENSIONS, listOf(TablesExtension.create(), StrikethroughExtension.create(),YamlFrontMatterExtension.create()))
val PARSER: Parser = Parser.builder(options).build()
val FORMATTER: Formatter = Formatter.builder(options).build()

fun main(args: Array<String>) {
    File(DOC_PATH).processAllMarkdownFile()
}

// 换行不分段的文本之间添加 HTML 注释标记，用于防止生成 HTML 时出现空格
fun File.addHtmlCommentTagBetweenNewLine(node: Node) {
    if (node is SoftLineBreak) {
        val previousNode = node.previous
        val nextNode = node.next
        if (previousNode is Text && nextNode is Text &&
                !englishEndThenStart(previousNode, nextNode) &&
                !englishAndChinese(previousNode, nextNode) &&
                !alreadyProcessed(previousNode, nextNode)) {

            // 查找插入 HTML 注释标记
            // 正则：第一行内容 + “换行” / “空白” / “>” 等可能出现的字符 + 第二行内容
            val regexString = "(" + Pattern.quote(previousNode.chars.toString()) + ")\\s*(\\r?\\n)[\\s>]*(" + Pattern.quote(nextNode.chars.toString()) + ")"
            writeText(regexString.toPattern().matcher(readText()).replaceFirst("$1$HTML_COMMENT_START$2$HTML_COMMENT_END$3"))

            // 用 formatter 的方式输出 markdown，在这里因为行数会发生变化所以不采用
            /*previousNode.chars = previousNode.chars.append(HTML_COMMENT_START)
            nextNode.chars = CharSubSequence.of(HTML_COMMENT_END).append(nextNode.chars)*/
        }
    } else if (node.hasChildren()) {
        node.children.filterNot { it is FencedCodeBlock || it is YamlFrontMatterBlock }.forEach(this::addHtmlCommentTagBetweenNewLine)
    }
}

fun alreadyProcessed(previousNode: Text, nextNode: Text): Boolean {
    return previousNode.chars.endsWith(HTML_COMMENT_START) && nextNode.chars.startsWith(HTML_COMMENT_END)
}

// 行末与行头都是英文字符或标点符号（不处理）
fun englishEndThenStart(previousNode: Text, nextNode: Text): Boolean {
    return previousNode.chars.lastChar().containEnglish() &&
            nextNode.chars.firstChar().containEnglish()
}

// 行头与行尾是中文和英文（不处理）
fun englishAndChinese(previousNode: Text, nextNode: Text): Boolean {
    return previousNode.chars.lastChar().containEnglish() && nextNode.chars.firstChar().containChinese() ||
            previousNode.chars.lastChar().containChinese() && nextNode.chars.firstChar().containEnglish()
}

fun Char.containEnglish() = toString().contains("[A-Za-z,.?!\"']".toRegex())
fun Char.containChinese() = toString().contains("\\p{sc=Han}".toRegex())

// 查看前 20 行是否包含中文字来辨别是否已经翻译
fun File.hasTranslated() = "\\p{sc=Han}".toPattern().matcher(bufferedReader().lineSequence().take(50).joinToString("")).find()

// 遍历所有已翻译的 markdown 文件
fun File.processAllMarkdownFile() {
    if (isFile && name.endsWith(".md") && hasTranslated()) {
        println("\n" + absolutePath)
        val document = PARSER.parse(readText())

        addHtmlCommentTagBetweenNewLine(document)

        // 用 formatter 格式化后的行数有变化，不适合
        /*writeText(FORMATTER.render(document))*/
    } else {
        this.listFiles()?.forEach { it.processAllMarkdownFile() }
    }
}

