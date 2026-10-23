package com.vocab.ielts.offline

/**
 * Bộ đọc JSON tối giản, chỉ phục vụ nhập gói khóa học offline. Không thêm thư viện nào và
 * chạy được trên JVM nên có thể kiểm thử bằng unit test (org.json trên unit test chỉ là stub).
 *
 * Hỗ trợ: object, array, string (kể cả escape), number, true/false/null.
 */
object SimpleJson {
    fun parse(text: String): Any? {
        val parser = Parser(text)
        val value = parser.parseValue()
        parser.skipWhitespace()
        if (!parser.atEnd()) throw IllegalArgumentException("JSON có ký tự thừa ở vị trí ${parser.position}.")
        return value
    }

    private class Parser(private val source: String) {
        var position = 0

        fun atEnd(): Boolean = position >= source.length

        fun skipWhitespace() {
            while (position < source.length && source[position].isWhitespace()) position++
        }

        fun parseValue(): Any? {
            skipWhitespace()
            if (atEnd()) fail("JSON rỗng.")
            return when (val c = source[position]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseString()
                't' -> parseLiteral("true", true)
                'f' -> parseLiteral("false", false)
                'n' -> parseLiteral("null", null)
                else -> if (c == '-' || c.isDigit()) parseNumber() else fail("Ký tự không hợp lệ '$c'.")
            }
        }

        private fun parseObject(): Map<String, Any?> {
            expect('{')
            val result = LinkedHashMap<String, Any?>()
            skipWhitespace()
            if (peek() == '}') {
                position++
                return result
            }
            while (true) {
                skipWhitespace()
                val key = parseString()
                skipWhitespace()
                expect(':')
                result[key] = parseValue()
                skipWhitespace()
                when (peek()) {
                    ',' -> position++
                    '}' -> {
                        position++
                        return result
                    }
                    else -> fail("Thiếu dấu ',' hoặc '}' trong object.")
                }
            }
        }

        private fun parseArray(): List<Any?> {
            expect('[')
            val result = mutableListOf<Any?>()
            skipWhitespace()
            if (peek() == ']') {
                position++
                return result
            }
            while (true) {
                result.add(parseValue())
                skipWhitespace()
                when (peek()) {
                    ',' -> position++
                    ']' -> {
                        position++
                        return result
                    }
                    else -> fail("Thiếu dấu ',' hoặc ']' trong array.")
                }
            }
        }

        private fun parseString(): String {
            expect('"')
            val builder = StringBuilder()
            while (!atEnd()) {
                val c = source[position++]
                when {
                    c == '"' -> return builder.toString()
                    c == '\\' -> {
                        if (atEnd()) fail("Chuỗi chưa đóng.")
                        when (val escaped = source[position++]) {
                            '"' -> builder.append('"')
                            '\\' -> builder.append('\\')
                            '/' -> builder.append('/')
                            'b' -> builder.append('\b')
                            'f' -> builder.append('\u000C')
                            'n' -> builder.append('\n')
                            'r' -> builder.append('\r')
                            't' -> builder.append('\t')
                            'u' -> {
                                if (position + 4 > source.length) fail("Mã \\u không hợp lệ.")
                                val hex = source.substring(position, position + 4)
                                position += 4
                                builder.append(hex.toInt(16).toChar())
                            }
                            else -> fail("Escape không hợp lệ '\\$escaped'.")
                        }
                    }
                    else -> builder.append(c)
                }
            }
            fail("Chuỗi chưa đóng.")
        }

        private fun parseNumber(): Any {
            val start = position
            if (peek() == '-') position++
            while (!atEnd() && (source[position].isDigit() || source[position] in ".eE+-")) position++
            val raw = source.substring(start, position)
            return if (raw.contains('.') || raw.contains('e') || raw.contains('E')) {
                raw.toDoubleOrNull() ?: fail("Số không hợp lệ '$raw'.")
            } else {
                raw.toLongOrNull() ?: fail("Số không hợp lệ '$raw'.")
            }
        }

        private fun parseLiteral(literal: String, value: Any?): Any? {
            if (!source.startsWith(literal, position)) fail("Giá trị không hợp lệ.")
            position += literal.length
            return value
        }

        private fun peek(): Char {
            if (atEnd()) fail("JSON kết thúc đột ngột.")
            return source[position]
        }

        private fun expect(c: Char) {
            if (atEnd() || source[position] != c) fail("Mong đợi '$c'.")
            position++
        }

        private fun fail(message: String): Nothing =
            throw IllegalArgumentException("$message (vị trí $position)")
    }
}
