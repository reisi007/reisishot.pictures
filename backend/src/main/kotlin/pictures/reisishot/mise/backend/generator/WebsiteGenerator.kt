package pictures.reisishot.mise.backend.generator

interface WebsiteGenerator {
    /**
     * The higher the priority, the sooner the execution.
     * The sitemap plugin, which runs last, has a defined priority of 0
     */
    val executionPriority: Int get() = 10000

    val generatorName: String

    /**
    Should return *true* if this plugin needs to regenerate some files
     */
    fun isGenerationNeeded(filename: String): Boolean

    fun generate()
}