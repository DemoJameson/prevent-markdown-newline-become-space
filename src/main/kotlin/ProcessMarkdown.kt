import com.vladsch.flexmark.ast.*
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterBlock
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension
import com.vladsch.flexmark.formatter.internal.Formatter
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.options.MutableDataSet
import java.io.File
import java.util.regex.Pattern

// markdown 文件目录
const val DOC_PATH = """D:\IdeaProjects\kotlin-web-site-cn/pages/docs"""

const val HTML_COMMENT_START = "<!--"
const val HTML_COMMENT_END = "-->"
val options = MutableDataSet().set(Parser.EXTENSIONS, listOf(TablesExtension.create(), StrikethroughExtension.create(), YamlFrontMatterExtension.create()))
val PARSER: Parser = Parser.builder(options).build()
val FORMATTER: Formatter = Formatter.builder(options).build()

fun main(args: Array<String>) {
    File(DOC_PATH).processAllMarkdownFile()
}

// 换行不分段的文本之间添加 HTML 注释标记，用于防止生成 HTML 时出现空格
fun File.addHtmlCommentTagBetweenNewLine(node: Node) {
    when {
        node is SoftLineBreak -> {
            val previousNode = node.previous
            val nextNode = node.next

            if (!needJoin(previousNode, nextNode)) return

            // 插入 HTML 注释标记
            // 正则：第一行内容 + “换行” + 第二行内容
            val regexString = "(" + Pattern.quote(previousNode.chars.toString()) + ")\\r?\\n(" + Pattern.quote(nextNode.chars.toString()) + ")"
            writeText(regexString.toPattern().matcher(readText()).replaceFirst("$1$HTML_COMMENT_START\n$HTML_COMMENT_END$2"))
        }
        node.hasChildren() -> node.children.filterNot { it is FencedCodeBlock || it is YamlFrontMatterBlock }.forEach(this::addHtmlCommentTagBetweenNewLine)
    }
}

fun Node.lastChar() = when (this) {
    is Text -> chars.lastChar()
    is Link -> firstChild.chars.lastChar()
    is Emphasis -> text.lastChar()
    is StrongEmphasis -> text.lastChar()
    else -> ' '
}

fun Node.firstChar() = when (this) {
    is Text -> chars.firstChar()
    is Link -> firstChild.chars.firstChar()
    is Emphasis -> text.firstChar()
    is StrongEmphasis -> text.firstChar()
    else -> ' '
}

// 判断转换为 HTML 后是否需要删掉两行文本间的空格
fun needJoin(previousNode: Node, nextNode: Node): Boolean {
    return previousNode.lastChar().isHanzi() && nextNode.firstChar().isHanzi()
}

// 判断汉字，排出全角空格、中文标点等
fun Char.isHanzi() = Character.isIdeographic(toInt()) && Character.isAlphabetic(toInt())

// 查看前 50 行是否包含中文字来辨别是否已经翻译
fun File.hasTranslated() = bufferedReader().lineSequence().take(50).joinToString("").chars().anyMatch(Character::isIdeographic)

// 遍历所有已翻译的 markdown 文件
fun File.processAllMarkdownFile() {
    if (isFile && name.endsWith(".md") && hasTranslated()) {
        println(absolutePath)
        val document = PARSER.parse(readText())

        addHtmlCommentTagBetweenNewLine(document)
    } else {
        this.listFiles()?.forEach { it.processAllMarkdownFile() }
    }
}

