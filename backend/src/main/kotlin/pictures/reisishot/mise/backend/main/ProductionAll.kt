package pictures.reisishot.mise.backend.main

object ProductionAll {

    @JvmStatic
    fun main(args: Array<String>) {
        println(">> Main")
        Main.build(false)
        println(">> Couples")
        Couples.build(false)
        println(">> Portrait")
        Portrait.build(false)
        println(">> Boudoir")
        Boudoir.build(false)
        println(">> Goto")
        Goto.build(false)
    }
}
