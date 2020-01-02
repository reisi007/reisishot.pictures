package pictures.reisishot.mise.configui

data class FilenameData(val name: String, val digitCount: Int = 3) {
    override fun toString(): String {
        return name
    }
}