package pictures.reisishot.mise.backend.main

object ProductionAll {

    @JvmStatic
    fun main(args: Array<String>) {
        Main.build(false)
        Portrait.build(false)
        Boudoir.build(false)
        Goto.build(false)
    }
}